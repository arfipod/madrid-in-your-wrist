# Metro Madrid NAP Example

This app includes a Wear OS example for Metro de Madrid schedule data published
through the Spanish National Access Point (NAP).

## Source

The NAP dataset page for Metro de Madrid is:

```text
https://nap.transportes.gob.es/Files/Detail/933
```

The page identifies the publisher as CRTM, the transport mode as rail, and the
format as GTFS-ZIP over HTTP/HTTPS. The dataset page currently says login is
required to download it.

The NAP API instructions say:

```text
1. Log in to the portal and open Edit Profile.
2. Enter an application name and request an API key.
3. Use the generated key in every API request with the ApiKey header.
```

## API Key Configuration

Do not commit NAP API keys. Provide the key at build time:

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

The example targets `Goya / Felipe II`, using `Goya` as the Metro stop name in
GTFS because Plaza de Felipe II is served by the Goya Metro station.

At runtime the app:

```text
1. Requests a NAP download link for dataset/file 933.
2. Falls back to reading dataset detail JSON and extracting a GTFS file id.
3. Downloads the GTFS zip.
4. Parses stops.txt, routes.txt, trips.txt, stop_times.txt, calendar.txt, and
   calendar_dates.txt.
5. Shows the next scheduled Metro departure for the target stop.
```

NAP provides scheduled GTFS data. The example does not claim realtime arrivals.

## Code Map

```text
MetroMadridExample.kt        Wear OS Compose screen
NapMetroSchedule.kt          NAP client, GTFS parser, schedule calculator
NapMetroScheduleTest.kt      JVM tests for parser and next-departure logic
```
