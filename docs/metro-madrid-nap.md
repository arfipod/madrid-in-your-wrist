# Metro Madrid NAP Example

This app includes Metro de Madrid schedule support published through the Spanish
National Access Point (NAP). The same helper powers the Madrid Wrist product UI
and the direct Metro development example.

## Source

The NAP dataset page for Metro de Madrid is:

```text
https://nap.transportes.gob.es/Files/Detail/933
```

The page identifies the publisher as CRTM, the transport mode as rail, and the
format as GTFS-ZIP over HTTP/HTTPS. The dataset page currently says login is
required to download it.

The API v2 dataset endpoint is:

```text
https://nap.transportes.gob.es/api/v2/conjunto-dato/933
```

As of the 2026-06-05 inspection, that endpoint reports one GTFS-ZIP file for
Metro de Madrid:

```text
dataset id:      933
file id:         1134
dataset name:    Metro de Madrid
publisher:       CRTM - Consorcio regional de transportes de Madrid
file type:       GTFS-ZIP
updated:         2025-05-30T15:25:21.09588Z
valid from:      2024-12-31
valid until:     2026-05-26 in NAP metadata
GTFS calendar:   2025-05-27 through 2026-05-27
GTFS version:    20250527
```

The downloaded ZIP contains these GTFS files:

```text
agency.txt
calendar.txt
calendar_dates.txt
fare_attributes.txt
fare_rules.txt
feed_info.txt
frequencies.txt
routes.txt
shapes.txt
stops.txt
stop_times.txt
trips.txt
```

The NAP API instructions say:

```text
1. Log in to the portal and open Edit Profile.
2. Enter an application name and request an API key.
3. Use the generated key in every API request with the ApiKey header.
```

## API Key Configuration

Do not commit NAP API keys. Provide the key at build time:

```bash
printf 'NAP_API_KEY=YOUR_KEY\n' >> .env
source ./scripts/common.sh
./gradlew --no-daemon :app:assembleDebug
```

The helper scripts automatically load `.env`, including Docker builds. You can
also provide the key for a single build:

```bash
source ./scripts/common.sh
NAP_API_KEY=YOUR_KEY ./gradlew --no-daemon :app:assembleDebug
```

Or pass it as a Gradle property:

```bash
source ./scripts/common.sh
./gradlew --no-daemon -Pnap.apiKey=YOUR_KEY :app:assembleDebug
```

The key is exposed to the debug app as:

```text
BuildConfig.NAP_API_KEY
```

## Runtime Behavior

Open the example from the app gallery or launch it directly:

```bash
source ./scripts/common.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example metro
```

The direct example targets Line 4 at `Argüelles`, showing scheduled departures
toward `Pinar de Chamartín`. The Madrid Wrist product UI can load multiple
Metro targets from its local catalog.

At runtime the direct example:

```text
1. Reads NAP API v2 dataset `933`.
2. Extracts the GTFS-ZIP file id from the dataset detail response.
3. Requests a temporary v2 download link for that file id.
4. Downloads the GTFS zip.
5. Parses stops.txt, routes.txt, trips.txt, stop_times.txt, frequencies.txt,
   calendar.txt, and calendar_dates.txt.
6. Shows the next scheduled Metro departure for the target stop.
```

NAP provides scheduled GTFS data. The example does not claim realtime arrivals.
If the NAP feed is expired but still contains a matching weekday pattern, the
example labels the result as the last GTFS date and shows the next departure from
that last-known timetable instead of presenting it as live/current service.

The strict GTFS path honors `calendar.txt` `start_date` and `end_date`. On
2026-06-05 this yields no active Metro de Madrid services because the published
Metro feed ended on 2026-05-27. The fallback path is intentionally labeled and
only ignores the date range after strict matching finds no departure; it still
uses the matching weekday pattern, route, stop, trip, stop time, and frequency
data from the feed.

For Line 4 at Argüelles, the feed contains these relevant stops:

```text
par_4_54   ARGÜELLES, parent est_4_52
par_4_126  ARGÜELLES, parent est_4_52
```

The Friday pattern is service `4_I15`. It contains both Line 4 directions:

```text
4_I15-004_2023I15_1_1_4__4___  PINAR DE CHAMARTIN
4_I15-004_2023I15_2_1_4__4___  ARGÜELLES
```

Around the 2026-06-05 18:00 local validation run, the fallback timetable
reported Line 4 Argüelles toward Pinar de Chamartín with a 4-minute headway.

## Inspection Notes

The NAP search also found related CRTM rail datasets:

```text
933   Metro de Madrid
1226  Metro Ligero y Tranvía Comunidad de Madrid
962   Cercanías Madrid
```

`Metro Ligero y Tranvía Comunidad de Madrid` is a separate dataset, not the
Metro de Madrid line 1-12/R feed. Its file id was `1427` during the inspection,
with a GTFS version of `20260507` and an active calendar range through
2027-05-07. It can produce strict current departures, but it is not used by this
Metro de Madrid example.

The `conjunto-dato/933/historico` endpoint returned historical ids, but those
ids did not download through the v2 file download endpoint during inspection.
The `fichero/1134/descarga/historico` endpoint returned an unrelated historical
EMT Madrid ZIP, so the app intentionally uses the current file id exposed by
`conjunto-dato/933`.

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

unzip -p artifacts/nap-metro-1134.zip calendar.txt
unzip -p artifacts/nap-metro-1134.zip routes.txt
unzip -p artifacts/nap-metro-1134.zip stops.txt | rg -i "argüelles|arguelles"
unzip -p artifacts/nap-metro-1134.zip frequencies.txt | rg "4_I15-004"
```

## Code Map

```text
MetroMadridExample.kt        Wear OS Compose screen
NapMetroSchedule.kt          NAP client, GTFS parser, schedule calculator
NapMetroScheduleTest.kt      JVM tests for parser and next-departure logic
```
