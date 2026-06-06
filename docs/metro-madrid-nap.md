# Metro Madrid NAP

Metro support uses the Spanish National Access Point (NAP) GTFS-ZIP dataset for
Metro de Madrid. The same helper powers the Madrid Wrist UI and the direct
developer `metro` example.

## Source

```text
Dataset page: https://nap.transportes.gob.es/Files/Detail/933
API detail:   https://nap.transportes.gob.es/api/v2/conjunto-dato/933
Dataset id:   933
Publisher:    CRTM - Consorcio regional de transportes de Madrid
Format:       GTFS-ZIP
```

As of the 2026-06-05 inspection, the API exposed file id `1134` for Metro de
Madrid. The downloaded ZIP included standard GTFS files such as `agency.txt`,
`calendar.txt`, `calendar_dates.txt`, `routes.txt`, `stops.txt`,
`stop_times.txt`, `trips.txt`, and `frequencies.txt`.

The NAP portal requires login and an API key. Use the generated key in the
`ApiKey` request header.

## Configure The API Key

Do not commit keys. Preferred local setup:

```bash
printf 'NAP_API_KEY=YOUR_KEY\n' >> .env
source ./scripts/common.sh
./gradlew --no-daemon :app:assembleDebug
```

Single build:

```bash
source ./scripts/common.sh
NAP_API_KEY=YOUR_KEY ./gradlew --no-daemon :app:assembleDebug
```

Gradle property:

```bash
source ./scripts/common.sh
./gradlew --no-daemon -Pnap.apiKey=YOUR_KEY :app:assembleDebug
```

Gradle exposes the value as `BuildConfig.NAP_API_KEY`.

## Runtime Behavior

Madrid Wrist loads Metro targets from `MadridTransitCatalog`, which is generated
from official CRTM GTFS feeds. Each favorite stores its station, line, and
destination target.

Runtime flow:

```text
1. Read NAP API dataset 933.
2. Resolve the current GTFS-ZIP file id.
3. Request a temporary download URL.
4. Download and parse the GTFS zip.
5. Match stop, route, destination, calendar, stop times, and frequencies.
6. Return the next scheduled departure.
```

NAP provides scheduled GTFS data, not realtime train arrivals. If the strict
calendar range is expired but the feed still contains a matching weekday
timetable, the app labels the result as stale and uses that last-known schedule
instead of presenting it as live/current service.

## Inspection Notes

Relevant Line 4 Argüelles stop ids seen during validation:

```text
par_4_54   ARGÜELLES, parent est_4_52
par_4_126  ARGÜELLES, parent est_4_52
```

The Friday pattern `4_I15` contained both Line 4 directions:

```text
4_I15-004_2023I15_1_1_4__4___  PINAR DE CHAMARTIN
4_I15-004_2023I15_2_1_4__4___  ARGÜELLES
```

Related CRTM datasets found during inspection:

```text
933   Metro de Madrid
1226  Metro Ligero y Tranvía Comunidad de Madrid
962   Cercanías Madrid
```

Metro Ligero is a separate dataset and is not used by the current Metro de
Madrid example.

Useful local inspection commands:

```bash
source ./scripts/common.sh
NAP_API_KEY=YOUR_KEY curl -fsS \
  -H "ApiKey: $NAP_API_KEY" \
  -H "accept: application/json" \
  https://nap.transportes.gob.es/api/v2/conjunto-dato/933 | jq .

NAP_API_KEY=YOUR_KEY curl -fsS \
  -H "ApiKey: $NAP_API_KEY" \
  -H "accept: application/json" \
  https://nap.transportes.gob.es/api/v2/fichero/1134/descarga | jq .

unzip -p artifacts/nap-metro-1134.zip stops.txt | rg -i "argüelles|arguelles"
unzip -p artifacts/nap-metro-1134.zip frequencies.txt | rg "4_I15-004"
```

## Code Map

```text
transit/MadridMetroSchedule.kt NAP client, GTFS parser, schedule calculator
NapMetroScheduleTest.kt        JVM tests for parser and next-departure logic
transit/MadridGeneratedTransitCatalog.kt Searchable Metro options generated from CRTM GTFS
transit/MadridTransitRuntime.kt Runtime integration used by Madrid Wrist
```
