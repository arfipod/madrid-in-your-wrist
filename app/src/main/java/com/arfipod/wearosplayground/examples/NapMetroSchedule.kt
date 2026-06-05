package com.arfipod.wearosplayground.examples

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NapMetroDataset {
    const val DATASET_ID = 933
    const val API_BASE_URL = "https://nap.transportes.gob.es"
    const val DETAIL_URL = "https://nap.transportes.gob.es/Files/Detail/933"
}

data class MetroScheduleTarget(
    val label: String = "L4 Argüelles -> Pinar de Chamartín",
    val stopNameQuery: String = "Argüelles",
    val routeNameQuery: String? = "4",
    val destinationQuery: String? = "Pinar de Chamartín",
)

data class MetroDeparture(
    val stopName: String,
    val routeName: String,
    val destination: String,
    val departureTime: LocalDateTime,
    val minutesUntil: Long,
) {
    fun compactLabel(): String {
        val time = departureTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        val prefix = if (minutesUntil <= 0) "Now" else "${minutesUntil} min"
        return "$prefix | $time\n$routeName -> $destination"
    }
}

object NapMetroApiJson {
    private val urlRegex = Regex("""https?://[^"'\s\\]+""")

    fun findDownloadUrl(rawBody: String): String? = urlRegex
        .find(rawBody)
        ?.value
        ?.trimEnd('}', ']')

    fun findFileId(rawBody: String): Int? {
        val gtfsObject = Regex("""(?is)\{[^{}]*(?:GTFS|gtfs)[^{}]*\}""")
            .findAll(rawBody)
            .map { it.value }
            .firstOrNull()
            ?: rawBody

        val fieldNames = listOf("idFichero", "ficheroId", "fileId", "id")
        return fieldNames.firstNotNullOfOrNull { fieldName ->
            Regex(""""$fieldName"\s*:\s*(\d+)""")
                .find(gtfsObject)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }
    }
}

class NapMetroClient(
    private val apiKey: String,
    private val datasetId: Int = NapMetroDataset.DATASET_ID,
    private val baseUrl: String = NapMetroDataset.API_BASE_URL,
) {
    suspend fun fetchMetroGtfsZip(): ByteArray = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "NAP API key is blank." }
        val downloadUrl = resolveDownloadUrl()
        requestBytes(downloadUrl, accept = "application/octet-stream", includeApiKey = false)
    }

    private fun resolveDownloadUrl(): String {
        val detail = apiGetText("/api/v2/conjunto-dato/$datasetId")
        val fileId = NapMetroApiJson.findFileId(detail)
            ?: throw IOException("NAP dataset $datasetId did not expose a GTFS file id.")
        val linkBody = apiGetText("/api/v2/fichero/$fileId/descarga")
        return NapMetroApiJson.findDownloadUrl(linkBody)
            ?: throw IOException("NAP file $fileId did not return a download URL.")
    }

    private fun apiGetText(path: String): String {
        val body = requestBytes("$baseUrl$path", accept = "application/json", includeApiKey = true)
        return body.toString(Charsets.UTF_8)
    }

    private fun requestBytes(
        urlString: String,
        accept: String,
        includeApiKey: Boolean,
    ): ByteArray {
        val uri = URI(urlString)
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("accept", accept)
        connection.setRequestProperty("User-Agent", "wearos-playground")
        if (includeApiKey) {
            connection.setRequestProperty("ApiKey", apiKey)
        }

        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            if (responseCode !in 200..299) {
                val message = bytes.toString(Charsets.UTF_8).take(120)
                throw IOException("NAP HTTP $responseCode $message")
            }
            bytes
        } finally {
            connection.disconnect()
        }
    }
}

object GtfsCsv {
    fun parseLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index += 1
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index += 1
        }

        values += current.toString()
        return values
    }

    fun parseTable(text: String): List<Map<String, String>> {
        val lines = text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return emptyList()

        val header = parseLine(lines.first().removePrefix("\uFEFF"))
            .map { it.removePrefix("\uFEFF") }
        return lines.drop(1).map { line ->
            val values = parseLine(line)
            header.mapIndexed { index, name ->
                name to values.getOrElse(index) { "" }
            }.toMap()
        }
    }
}

class MetroGtfsSchedule(
    private val zoneId: ZoneId = ZoneId.of("Europe/Madrid"),
) {
    fun nextDeparture(
        gtfsZipBytes: ByteArray,
        target: MetroScheduleTarget = MetroScheduleTarget(),
        now: LocalDateTime = LocalDateTime.now(zoneId),
    ): MetroDeparture? {
        val feed = GtfsFeed.fromZip(gtfsZipBytes)
        return feed.nextDeparture(target = target, now = now)
    }
}

private data class GtfsFeed(
    val stops: List<Map<String, String>>,
    val routesById: Map<String, Map<String, String>>,
    val tripsById: Map<String, Map<String, String>>,
    val stopTimes: List<Map<String, String>>,
    val frequenciesByTripId: Map<String, List<Map<String, String>>>,
    val calendar: List<Map<String, String>>,
    val calendarDates: List<Map<String, String>>,
) {
    fun nextDeparture(
        target: MetroScheduleTarget,
        now: LocalDateTime,
    ): MetroDeparture? {
        val targetStopIds = stops
            .filter { row ->
                row["stop_name"].orEmpty().normalized().contains(target.stopNameQuery.normalized())
            }
            .associateBy { it["stop_id"].orEmpty() }
        if (targetStopIds.isEmpty()) return null

        return sequenceOf(0L, 1L)
            .flatMap { dayOffset ->
                departuresForDate(
                    target = target,
                    targetStopIds = targetStopIds,
                    serviceDate = now.toLocalDate().plusDays(dayOffset),
                    now = now,
                )
            }
            .minByOrNull { it.departureTime }
    }

    private fun departuresForDate(
        target: MetroScheduleTarget,
        targetStopIds: Map<String, Map<String, String>>,
        serviceDate: LocalDate,
        now: LocalDateTime,
    ): Sequence<MetroDeparture> {
        val activeServices = activeServices(serviceDate)
        if (activeServices.isEmpty()) return emptySequence()
        val serviceStart = serviceDate.atStartOfDay()

        return stopTimes.asSequence().mapNotNull { stopTime ->
            val stopId = stopTime["stop_id"].orEmpty()
            val stop = targetStopIds[stopId] ?: return@mapNotNull null
            val trip = tripsById[stopTime["trip_id"].orEmpty()] ?: return@mapNotNull null
            if (trip["service_id"].orEmpty() !in activeServices) return@mapNotNull null

            val route = routesById[trip["route_id"].orEmpty()] ?: emptyMap()
            val routeName = routeName(route)
            if (!target.routeNameQuery.isNullOrBlank() &&
                !routeName.normalized().contains(target.routeNameQuery.normalized())
            ) {
                return@mapNotNull null
            }

            val destination = trip["trip_headsign"].orEmpty().ifBlank { "Metro" }
            if (!target.destinationQuery.isNullOrBlank() &&
                !destination.normalized().contains(target.destinationQuery.normalized())
            ) {
                return@mapNotNull null
            }

            val stopOffsetSeconds = parseGtfsSeconds(stopTime["departure_time"].orEmpty())
                ?: parseGtfsSeconds(stopTime["arrival_time"].orEmpty())
                ?: return@mapNotNull null

            val tripId = stopTime["trip_id"].orEmpty()
            val departureTime = nextDepartureTime(
                serviceStart = serviceStart,
                tripId = tripId,
                stopOffsetSeconds = stopOffsetSeconds,
                now = now,
            ) ?: return@mapNotNull null

            departure(
                stopName = stop["stop_name"].orEmpty().ifBlank { target.label },
                routeName = routeName,
                destination = destination,
                departureTime = departureTime,
                now = now,
            )
        }
    }

    private fun nextDepartureTime(
        serviceStart: LocalDateTime,
        tripId: String,
        stopOffsetSeconds: Int,
        now: LocalDateTime,
    ): LocalDateTime? {
        val frequencyDepartures = frequenciesByTripId[tripId].orEmpty()
            .asSequence()
            .mapNotNull { frequency ->
                nextFrequencyDepartureTime(
                    serviceStart = serviceStart,
                    stopOffsetSeconds = stopOffsetSeconds,
                    frequency = frequency,
                    now = now,
                )
            }
            .minOrNull()
        if (frequencyDepartures != null) return frequencyDepartures

        val departureTime = serviceStart.plusSeconds(stopOffsetSeconds.toLong())
        return departureTime.takeIf { it.isAfter(now) }
    }

    private fun nextFrequencyDepartureTime(
        serviceStart: LocalDateTime,
        stopOffsetSeconds: Int,
        frequency: Map<String, String>,
        now: LocalDateTime,
    ): LocalDateTime? {
        val startSeconds = parseGtfsSeconds(frequency["start_time"].orEmpty()) ?: return null
        val endSeconds = parseGtfsSeconds(frequency["end_time"].orEmpty()) ?: return null
        val headwaySeconds = frequency["headway_secs"].orEmpty().toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return null

        val firstDepartureSeconds = startSeconds + stopOffsetSeconds
        val lastDepartureSeconds = endSeconds + stopOffsetSeconds
        val nowSeconds = Duration.between(serviceStart, now).seconds
        if (nowSeconds > lastDepartureSeconds) return null

        val intervalsAfterStart = ((nowSeconds - firstDepartureSeconds).coerceAtLeast(0) +
            headwaySeconds - 1) / headwaySeconds
        val departureSeconds = firstDepartureSeconds + intervalsAfterStart * headwaySeconds
        if (departureSeconds > lastDepartureSeconds) return null

        return serviceStart.plusSeconds(departureSeconds)
            .takeIf { it.isAfter(now) }
    }

    private fun departure(
        stopName: String,
        routeName: String,
        destination: String,
        departureTime: LocalDateTime,
        now: LocalDateTime,
    ): MetroDeparture {
        val secondsUntil = Duration.between(now, departureTime).seconds.coerceAtLeast(0)
        return MetroDeparture(
            stopName = stopName,
            routeName = routeName,
            destination = destination,
            departureTime = departureTime,
            minutesUntil = (secondsUntil + 59) / 60,
        )
    }

    private fun activeServices(date: LocalDate): Set<String> {
        val dayField = when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "monday"
            java.time.DayOfWeek.TUESDAY -> "tuesday"
            java.time.DayOfWeek.WEDNESDAY -> "wednesday"
            java.time.DayOfWeek.THURSDAY -> "thursday"
            java.time.DayOfWeek.FRIDAY -> "friday"
            java.time.DayOfWeek.SATURDAY -> "saturday"
            java.time.DayOfWeek.SUNDAY -> "sunday"
        }
        val gtfsDate = date.format(DateTimeFormatter.BASIC_ISO_DATE)
        val active = calendar
            .filter { row ->
                row[dayField] == "1" &&
                    gtfsDate >= row["start_date"].orEmpty() &&
                    gtfsDate <= row["end_date"].orEmpty()
            }
            .mapNotNullTo(mutableSetOf()) { it["service_id"] }

        calendarDates
            .filter { it["date"] == gtfsDate }
            .forEach { row ->
                val serviceId = row["service_id"].orEmpty()
                when (row["exception_type"]) {
                    "1" -> active += serviceId
                    "2" -> active -= serviceId
                }
            }

        return active
    }

    companion object {
        fun fromZip(zipBytes: ByteArray): GtfsFeed {
            val files = readZipTextFiles(zipBytes)
            return GtfsFeed(
                stops = GtfsCsv.parseTable(files.getValue("stops.txt")),
                routesById = GtfsCsv.parseTable(files.getValue("routes.txt"))
                    .associateBy { it["route_id"].orEmpty() },
                tripsById = GtfsCsv.parseTable(files.getValue("trips.txt"))
                    .associateBy { it["trip_id"].orEmpty() },
                stopTimes = GtfsCsv.parseTable(files.getValue("stop_times.txt")),
                frequenciesByTripId = GtfsCsv.parseTable(files["frequencies.txt"].orEmpty())
                    .groupBy { it["trip_id"].orEmpty() },
                calendar = GtfsCsv.parseTable(files["calendar.txt"].orEmpty()),
                calendarDates = GtfsCsv.parseTable(files["calendar_dates.txt"].orEmpty()),
            )
        }

        private fun readZipTextFiles(zipBytes: ByteArray): Map<String, String> {
            val files = mutableMapOf<String, String>()
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        val fileName = entry.name.substringAfterLast("/")
                        files[fileName] = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                }
            }
            return files
        }
    }
}

private fun String.normalized(): String = lowercase(Locale.ROOT)
    .replace("á", "a")
    .replace("é", "e")
    .replace("í", "i")
    .replace("ó", "o")
    .replace("ú", "u")
    .replace("ü", "u")
    .replace("ñ", "n")

private fun routeName(route: Map<String, String>): String {
    val shortName = route["route_short_name"].orEmpty()
    val longName = route["route_long_name"].orEmpty()
    return listOf(shortName, longName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Metro" }
}

private fun parseGtfsSeconds(raw: String): Int? {
    val parts = raw.split(":")
    if (parts.size != 3) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    val seconds = parts[2].toIntOrNull() ?: return null
    return hours * 3600 + minutes * 60 + seconds
}
