# EMT Madrid OpenAPI Example

This app includes a Wear OS example for the real-time EMT Madrid bus API.

## Source

The OpenAPI entry point lists the MobilityLabs registration portal and bus
methods:

```text
https://openapi.emtmadrid.es/
https://mobilitylabs.emtmadrid.es/
https://datos.emtmadrid.es/m360-swagger/docs
```

The Swagger docs include:

```text
GET  /v1/mobilitylabs/user/login/
POST /v2/transport/busemtmad/stops/{stopId}/arrives/
```

The current app target is stop `1064`, line `E3`, destination `VALDERRIVAS`.

## Credential Configuration

Do not commit EMT credentials. Build with the app credentials from MobilityLabs:

```bash
EMT_CLIENT_ID=YOUR_CLIENT_ID EMT_PASS_KEY=YOUR_PASS_KEY \
  ./gradlew --no-daemon :app:assembleDebug
```

If protected login returns `403`, use the basic MobilityLabs login that returns
an `accessToken` from `email` and `password`:

```bash
EMT_EMAIL=YOUR_EMAIL EMT_PASSWORD=YOUR_PASSWORD \
  ./gradlew --no-daemon :app:assembleDebug
```

Or pass either mode as Gradle properties:

```bash
./gradlew --no-daemon \
  -Pemt.clientId=YOUR_CLIENT_ID \
  -Pemt.passKey=YOUR_PASS_KEY \
  :app:assembleDebug
```

```bash
./gradlew --no-daemon \
  -Pemt.email=YOUR_EMAIL \
  -Pemt.password=YOUR_PASSWORD \
  :app:assembleDebug
```

When both modes are present, the app tries protected login first and falls back
to basic login.

The values are exposed to the debug app as:

```text
BuildConfig.EMT_CLIENT_ID
BuildConfig.EMT_PASS_KEY
BuildConfig.EMT_EMAIL
BuildConfig.EMT_PASSWORD
```

## Runtime Behavior

Open the example from the app gallery or launch it directly:

```bash
source ./scripts/common.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example emt
```

At runtime the app:

```text
1. Logs in with X-ClientId and passKey, or email and password.
2. Extracts the returned accessToken.
3. Calls the arrivals endpoint for stop 1064.
4. Filters arrivals whose destination contains VALDERRIVAS.
5. Shows the next bus in minutes, plus distance when provided.
```

## Code Map

```text
EmtMadridExample.kt       Wear OS Compose screen
EmtMadridArrival.kt       EMT client and arrival parser
EmtMadridArrivalTest.kt   JVM tests for token and arrival parsing
```
