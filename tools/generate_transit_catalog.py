#!/usr/bin/env python3
"""Generate Madrid Wrist's searchable transit catalog from official GTFS feeds."""

from __future__ import annotations

import argparse
import csv
import dataclasses
import io
import re
import textwrap
import time
import urllib.request
import zipfile
from pathlib import Path
from typing import Dict, Iterable, Iterator, List, Optional, Set, Tuple


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = (
    ROOT
    / "app/src/main/java/com/arfipod/madridinyourwrist/transit/MadridGeneratedTransitCatalog.kt"
)
DEFAULT_CACHE_DIR = ROOT / "artifacts/gtfs"
ARCGIS_DATA_URL = "https://www.arcgis.com/sharing/rest/content/items/{item_id}/data"
CHUNK_SIZE = 55_000


@dataclasses.dataclass(frozen=True)
class Feed:
    key: str
    label: str
    item_id: str
    kind: str
    source: str
    output_prefix: str
    stop_word: str

    @property
    def url(self) -> str:
        return ARCGIS_DATA_URL.format(item_id=self.item_id)


FEEDS: Tuple[Feed, ...] = (
    Feed(
        key="metro",
        label="CRTM GTFS Red de Metro",
        item_id="5c7f2951962540d69ffe8f640d94c246",
        kind="METRO",
        source="METRO_NAP",
        output_prefix="metro",
        stop_word="Estación",
    ),
    Feed(
        key="light_rail",
        label="CRTM GTFS Red de Metro Ligero",
        item_id="aaed26cc0ff64b0c947ac0bc3e033196",
        kind="METRO",
        source="CRTM_STATIC_GTFS",
        output_prefix="metro_ligero",
        stop_word="Estación",
    ),
    Feed(
        key="emt",
        label="CRTM GTFS Red de EMT",
        item_id="868df0e58fca47e79b942902dffd7da0",
        kind="BUS",
        source="EMT_OPENAPI",
        output_prefix="bus_emt",
        stop_word="Parada",
    ),
    Feed(
        key="interurban",
        label="CRTM GTFS Red de Autobuses Interurbanos",
        item_id="885399f83408473c8d815e40c5e702b7",
        kind="BUS",
        source="CRTM_STATIC_GTFS",
        output_prefix="bus_interurbano",
        stop_word="Parada",
    ),
)


@dataclasses.dataclass(frozen=True)
class Route:
    route_id: str
    short_name: str
    long_name: str


@dataclasses.dataclass(frozen=True)
class Stop:
    stop_id: str
    stop_code: str
    name: str
    description: str
    latitude: str
    longitude: str


@dataclasses.dataclass(frozen=True)
class Trip:
    trip_id: str
    route_id: str
    headsign: str
    direction_id: str


@dataclasses.dataclass(frozen=True)
class CatalogRow:
    kind: str
    source: str
    option_id: str
    label: str
    detail: str
    latitude: str
    longitude: str
    stop_name: str
    stop_id: str
    line_id: str
    destination: str
    aliases: Tuple[str, ...]

    def encoded(self) -> str:
        return "\t".join(
            clean_field(value)
            for value in (
                self.kind,
                self.source,
                self.option_id,
                self.label,
                self.detail,
                self.latitude,
                self.longitude,
                self.stop_name,
                self.stop_id,
                self.line_id,
                self.destination,
                "|".join(clean_alias(alias) for alias in self.aliases if alias.strip()),
            )
        )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--cache-dir", type=Path, default=DEFAULT_CACHE_DIR)
    parser.add_argument("--no-download", action="store_true")
    args = parser.parse_args()

    rows: List[CatalogRow] = []
    summaries: List[str] = []
    for feed in FEEDS:
        zip_path = args.cache_dir / f"{feed.key}.zip"
        if not args.no_download or not zip_path.exists():
            download(feed.url, zip_path)
        feed_rows = list(rows_from_feed(feed, zip_path))
        rows.extend(feed_rows)
        summaries.append(f"{feed.label}: {len(feed_rows)} opciones")

    rows = sorted(
        dedupe_rows(rows),
        key=lambda row: (row.kind, row.source, natural_key(row.line_id), normalized(row.stop_name), normalized(row.destination)),
    )
    write_kotlin(args.output, rows, summaries)
    print(f"Wrote {len(rows)} generated options to {args.output}")
    for summary in summaries:
        print(f"- {summary}")


def download(url: str, output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "MadridWristCatalogGenerator/1.0"})
    with urllib.request.urlopen(request, timeout=120) as response:
        output.write_bytes(response.read())


def rows_from_feed(feed: Feed, zip_path: Path) -> Iterator[CatalogRow]:
    with zipfile.ZipFile(zip_path) as zf:
        routes = {row["route_id"]: parse_route(row) for row in read_gtfs_table(zf, "routes.txt")}
        stops = {row["stop_id"]: parse_stop(row) for row in read_gtfs_table(zf, "stops.txt")}
        trips = {row["trip_id"]: parse_trip(row) for row in read_gtfs_table(zf, "trips.txt")}

        seen: Set[Tuple[str, str, str]] = set()
        for stop_time in read_gtfs_table(zf, "stop_times.txt"):
            trip = trips.get(stop_time.get("trip_id", ""))
            if trip is None:
                continue
            route = routes.get(trip.route_id)
            stop = stops.get(stop_time.get("stop_id", ""))
            if route is None or stop is None:
                continue
            if stop.name == "" or route.short_name == "":
                continue

            destination = trip.headsign.strip() or route.long_name.strip() or route.short_name
            key = (route.route_id, stop.stop_id, normalized(destination))
            if key in seen:
                continue
            seen.add(key)

            yield catalog_row(feed, route, stop, destination)


def read_gtfs_table(zf: zipfile.ZipFile, name: str) -> Iterator[Dict[str, str]]:
    with zf.open(name) as raw:
        text = io.TextIOWrapper(raw, encoding="utf-8-sig", newline="")
        yield from csv.DictReader(text)


def parse_route(row: Dict[str, str]) -> Route:
    return Route(
        route_id=row.get("route_id", "").strip(),
        short_name=row.get("route_short_name", "").strip(),
        long_name=row.get("route_long_name", "").strip(),
    )


def parse_stop(row: Dict[str, str]) -> Stop:
    return Stop(
        stop_id=row.get("stop_id", "").strip(),
        stop_code=row.get("stop_code", "").strip(),
        name=row.get("stop_name", "").strip(),
        description=row.get("stop_desc", "").strip(),
        latitude=row.get("stop_lat", "").strip(),
        longitude=row.get("stop_lon", "").strip(),
    )


def parse_trip(row: Dict[str, str]) -> Trip:
    return Trip(
        trip_id=row.get("trip_id", "").strip(),
        route_id=row.get("route_id", "").strip(),
        headsign=row.get("trip_headsign", "").strip(),
        direction_id=row.get("direction_id", "").strip(),
    )


def catalog_row(feed: Feed, route: Route, stop: Stop, destination: str) -> CatalogRow:
    line = route.short_name.strip()
    destination = destination.strip()
    option_id = unique_option_id(feed.output_prefix, line, stop.stop_code or stop.stop_id, destination)
    line_label = metro_line_label(line) if feed.kind == "METRO" else line.upper()
    stop_label = title_keep_acronyms(stop.name)
    destination_label = title_keep_acronyms(destination)
    label = f"{stop_label} {line_label}".strip()
    detail = destination_label
    public_stop_id = stop.stop_code or stop.stop_id
    aliases = (
        stop.description,
        public_stop_id,
        f"{feed.stop_word} {public_stop_id}",
        f"{feed.stop_word} {stop.name}",
        route.long_name,
        f"Línea {line}",
        f"Linea {line}",
        metro_line_label(line),
        destination,
    )
    return CatalogRow(
        kind=feed.kind,
        source=feed.source,
        option_id=option_id,
        label=label,
        detail=detail,
        latitude=stop.latitude,
        longitude=stop.longitude,
        stop_name=stop.name,
        stop_id=public_stop_id,
        line_id=line,
        destination=destination,
        aliases=aliases,
    )


def dedupe_rows(rows: Iterable[CatalogRow]) -> List[CatalogRow]:
    by_id: Dict[str, CatalogRow] = {}
    for row in rows:
        option_id = row.option_id
        suffix = 2
        while option_id in by_id and by_id[option_id] != row:
            option_id = f"{row.option_id}_{suffix}"
            suffix += 1
        by_id[option_id] = dataclasses.replace(row, option_id=option_id)
    return list(by_id.values())


def unique_option_id(prefix: str, line: str, stop_id: str, destination: str) -> str:
    return "_".join(
        part
        for part in (
            prefix,
            slug(line),
            slug(stop_id),
            slug(destination),
        )
        if part
    )


def slug(value: str) -> str:
    value = normalized(value)
    value = re.sub(r"[^a-z0-9]+", "_", value)
    return value.strip("_") or "x"


def normalized(value: str) -> str:
    replacements = str.maketrans(
        {
            "á": "a",
            "é": "e",
            "í": "i",
            "ó": "o",
            "ú": "u",
            "ü": "u",
            "ñ": "n",
            "Á": "a",
            "É": "e",
            "Í": "i",
            "Ó": "o",
            "Ú": "u",
            "Ü": "u",
            "Ñ": "n",
        }
    )
    return value.translate(replacements).lower()


def natural_key(value: str) -> Tuple[int, str]:
    match = re.search(r"\d+", value)
    if match:
        return int(match.group(0)), value
    return 10_000, value


def title_keep_acronyms(value: str) -> str:
    words = re.split(r"(\s+|-)", value.strip())
    return "".join(word if word.isspace() or word == "-" else title_word(word) for word in words)


def title_word(value: str) -> str:
    if value.isupper() and len(value) <= 3:
        return value
    if any(char.isdigit() for char in value):
        return value.upper()
    return value[:1].upper() + value[1:].lower()


def metro_line_label(line: str) -> str:
    line = line.strip().upper()
    if line.startswith("ML") or line == "R":
        return line
    if line.startswith("L"):
        return line
    return f"L{line}"


def clean_field(value: object) -> str:
    return str(value).replace("\t", " ").replace("\n", " ").replace("\r", " ").strip()


def clean_alias(value: object) -> str:
    return clean_field(value).replace("|", " ")


def write_kotlin(output: Path, rows: List[CatalogRow], summaries: List[str]) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    metro_rows = [row for row in rows if row.kind == "METRO"]
    bus_rows = [row for row in rows if row.kind == "BUS"]
    metro_chunks = chunk_text("\n".join(row.encoded() for row in metro_rows), CHUNK_SIZE)
    bus_chunks = chunk_text("\n".join(row.encoded() for row in bus_rows), CHUNK_SIZE)
    generated_at = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    summary_text = "\n".join(f" * - {summary}" for summary in summaries)
    metro_chunk_literals = ",\n".join(textwrap.indent(kotlin_triple_string(chunk), "        ") for chunk in metro_chunks)
    bus_chunk_literals = ",\n".join(textwrap.indent(kotlin_triple_string(chunk), "        ") for chunk in bus_chunks)

    output.write_text(
        f"""package com.arfipod.madridinyourwrist.transit

/**
 * Generated by tools/generate_transit_catalog.py from official CRTM GTFS feeds.
 *
{summary_text}
 */
object MadridGeneratedTransitCatalog {{
    const val GENERATED_AT_UTC: String = "{generated_at}"
    const val OPTION_COUNT: Int = {len(rows)}

    val metroOptions: List<MadridTransitOption> by lazy {{
        parseRows(METRO_ROW_CHUNKS)
    }}

    val busOptions: List<MadridTransitOption> by lazy {{
        parseRows(BUS_ROW_CHUNKS)
    }}

    val options: List<MadridTransitOption> by lazy {{
        metroOptions + busOptions
    }}

    private val optionByIdCache = mutableMapOf<String, MadridTransitOption>()

    fun optionById(id: String): MadridTransitOption? {{
        if (id.isBlank()) return null
        synchronized(optionByIdCache) {{
            optionByIdCache[id]?.let {{ option -> return option }}
        }}
        return findOptionById(id)?.also {{ option ->
            synchronized(optionByIdCache) {{
                optionByIdCache[id] = option
            }}
        }}
    }}

    private fun parseRows(rowChunks: Array<String>): List<MadridTransitOption> {{
        return rowChunks.asSequence()
            .flatMap {{ chunk -> chunk.lineSequence() }}
            .map {{ line -> line.trim() }}
            .filter {{ line -> line.isNotBlank() }}
            .mapNotNull {{ line -> line.toTransitOption() }}
            .toList()
    }}

    private fun findOptionById(id: String): MadridTransitOption? {{
        return when {{
            id.startsWith("metro") -> optionByIdIn(METRO_ROW_CHUNKS, id)
            id.startsWith("bus") -> optionByIdIn(BUS_ROW_CHUNKS, id)
            else -> optionByIdIn(METRO_ROW_CHUNKS, id) ?: optionByIdIn(BUS_ROW_CHUNKS, id)
        }}
    }}

    private fun optionByIdIn(rowChunks: Array<String>, id: String): MadridTransitOption? {{
        return rowChunks.asSequence()
            .flatMap {{ chunk -> chunk.lineSequence() }}
            .map {{ line -> line.trim() }}
            .firstNotNullOfOrNull {{ line ->
                if (line.isNotBlank() && line.generatedOptionId() == id) line.toTransitOption() else null
            }}
    }}

    private fun String.generatedOptionId(): String? {{
        var fieldStart = 0
        repeat(2) {{
            val nextSeparator = indexOf('\\t', startIndex = fieldStart)
            if (nextSeparator < 0) return null
            fieldStart = nextSeparator + 1
        }}
        val fieldEnd = indexOf('\\t', startIndex = fieldStart).takeIf {{ index -> index >= 0 }} ?: length
        return substring(fieldStart, fieldEnd)
    }}

    private fun String.toTransitOption(): MadridTransitOption? {{
        val fields = split('\\t')
        if (fields.size < 12) return null
        val kind = runCatching {{ MadridTransitKind.valueOf(fields[0]) }}.getOrNull() ?: return null
        val source = runCatching {{ MadridTransitSource.valueOf(fields[1]) }}.getOrNull() ?: return null
        val id = fields[2]
        val label = fields[3]
        val detail = fields[4]
        val latitude = fields[5].toDoubleOrNull()
        val longitude = fields[6].toDoubleOrNull()
        val stopName = fields[7]
        val stopId = fields[8]
        val lineId = fields[9]
        val destination = fields[10]
        val aliases = fields[11].split('|').filter {{ alias -> alias.isNotBlank() }}
        val location = if (latitude != null && longitude != null) {{
            MadridGeoPoint(latitude = latitude, longitude = longitude)
        }} else {{
            null
        }}

        return MadridTransitOption(
            id = id,
            kind = kind,
            source = source,
            label = label,
            detail = detail,
            location = location,
            searchAliases = aliases,
            metroTarget = if (kind == MadridTransitKind.METRO) {{
                MetroScheduleTarget(
                    label = "$lineId $stopName -> $destination",
                    stopNameQuery = stopName,
                    routeNameQuery = lineId,
                    destinationQuery = destination,
                    stopId = stopId,
                )
            }} else {{
                null
            }},
            busTarget = if (kind == MadridTransitKind.BUS) {{
                EmtMadridStopTarget(
                    stopId = stopId,
                    label = "$stopName $lineId -> $destination",
                    lineId = lineId,
                    destination = destination,
                    stopName = stopName,
                )
            }} else {{
                null
            }},
        )
    }}

    private val METRO_ROW_CHUNKS: Array<String> = arrayOf(
{metro_chunk_literals}
    )

    private val BUS_ROW_CHUNKS: Array<String> = arrayOf(
{bus_chunk_literals}
    )
}}
""",
        encoding="utf-8",
    )


def chunk_text(text: str, chunk_size: int) -> List[str]:
    chunks: List[str] = []
    current: List[str] = []
    current_size = 0
    for line in text.splitlines():
        line_size = len(line.encode("utf-8")) + 1
        if current and current_size + line_size > chunk_size:
            chunks.append("\n".join(current))
            current = []
            current_size = 0
        current.append(line)
        current_size += line_size
    if current:
        chunks.append("\n".join(current))
    return chunks


def kotlin_triple_string(value: str) -> str:
    return '"""\n' + value.replace('"""', '\\"\\"\\"') + '\n""".trimIndent()'


if __name__ == "__main__":
    main()
