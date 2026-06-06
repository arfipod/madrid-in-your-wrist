# EMT Madrid OpenAPI

EMT support uses the MobilityLabs/OpenAPI bus arrivals API. The same client
powers the Madrid Wrist UI and the direct developer `emt` example.

## Source

```text
OpenAPI entry point: https://openapi.emtmadrid.es/
MobilityLabs portal: https://mobilitylabs.emtmadrid.es/
Swagger docs:        https://datos.emtmadrid.es/m360-swagger/docs
Login:               GET  /v1/mobilitylabs/user/login/
Arrivals:            POST /v2/transport/busemtmad/stops/{stopId}/arrives/
```

The direct EMT example targets stop `1064`, line `E3`, destination
`VALDERRIVAS`. Madrid Wrist can load multiple EMT stop targets from
`MadridTransitCatalog`.

## Configure Credentials

Do not commit credentials. Preferred local setup:

```bash
cat >> .env <<'EOF'
EMT_CLIENT_ID=YOUR_CLIENT_ID
EMT_PASS_KEY=YOUR_PASS_KEY
EMT_EMAIL=YOUR_EMAIL
EMT_PASSWORD=YOUR_PASSWORD
EOF
source ./scripts/common.sh
./gradlew --no-daemon :app:assembleDebug
```

Protected app login:

```bash
EMT_CLIENT_ID=YOUR_CLIENT_ID EMT_PASS_KEY=YOUR_PASS_KEY \
  ./gradlew --no-daemon :app:assembleDebug
```

Basic MobilityLabs login, useful when protected login returns `403`:

```bash
EMT_EMAIL=YOUR_EMAIL EMT_PASSWORD=YOUR_PASSWORD \
  ./gradlew --no-daemon :app:assembleDebug
```

Gradle properties:

```bash
./gradlew --no-daemon \
  -Pemt.clientId=YOUR_CLIENT_ID \
  -Pemt.passKey=YOUR_PASS_KEY \
  :app:assembleDebug

./gradlew --no-daemon \
  -Pemt.email=YOUR_EMAIL \
  -Pemt.password=YOUR_PASSWORD \
  :app:assembleDebug
```

When both modes are present, the app tries protected login first and falls back
to basic login. Gradle exposes values as `BuildConfig.EMT_CLIENT_ID`,
`BuildConfig.EMT_PASS_KEY`, `BuildConfig.EMT_EMAIL`, and
`BuildConfig.EMT_PASSWORD`.

## Runtime Behavior

Launch the direct example:

```bash
source ./scripts/common.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example emt
```

Runtime flow:

```text
1. Log in with X-ClientId/passKey or email/password.
2. Extract the returned accessToken.
3. Call arrivals for the configured stop.
4. Filter by line/destination.
5. Show minutes, destination, and distance when provided.
```

The endpoint is realtime. `No E3 arrival` can be valid when no matching bus is
reported for the target stop at that moment.

## API Smoke Test

This checks credentials before rebuilding the APK:

```bash
TOKEN="$(
  curl -fsS \
    -H "email: $EMT_EMAIL" \
    -H "password: $EMT_PASSWORD" \
    "https://openapi.emtmadrid.es/v1/mobilitylabs/user/login/" |
    jq -r '.data[0].accessToken // empty'
)"

curl -fsS -X POST \
  -H "accessToken: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cultureInfo": "ES",
    "Text_StopRequired_YN": "Y",
    "Text_EstimationsRequired_YN": "Y",
    "Text_IncidencesRequired_YN": "N",
    "statistics": "N"
  }' \
  "https://openapi.emtmadrid.es/v2/transport/busemtmad/stops/1064/arrives/" |
  jq -r '
    "code=\(.code) desc=\(.description)",
    "count=\((.data[0].Arrive // []) | length)",
    ((.data[0].Arrive // [])[]? |
      select(type == "object") |
      "line=\(.line // .lineArrive // .lineId) dest=\(.destination) sec=\(.estimateArrive // .busTimeLeft) meters=\(.DistanceBus // .busDistance)"
    )
  '
```

A successful response uses `code=00`.

## Verify On A Target

Build a credentialed debug APK:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

Install and launch on a selected watch or emulator:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd logcat -c
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example emt
sleep 15
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd logcat -d \
  -s WearLoop AndroidRuntime ActivityTaskManager | tail -180
```

Successful validation usually includes:

```text
WearLoop: MainActivity created. Build timestamp=...
WearLoop: EMT E3 next arrival ... min
```

Screenshots are written under `artifacts/screenshots/`.

## Troubleshooting

- Protected login with `X-ClientId` and `passKey` can return HTTP `403`; try
  basic `email`/`password` login.
- Rebuild after changing credential environment variables; use `:app:clean` if
  `BuildConfig` appears stale.
- If install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall the
  debug app from that target and reinstall.
- If an emulator says `device` too early, wait for `system_server` and
  `cmd package list packages` before installing.
- If a screenshot shows the Wear charging screen, turn virtual AC off with
  `adb emu power ac off`, wake, and relaunch.
- Parser changes should be covered by JVM tests with current nested `Arrive`
  response shapes.

## Code Map

```text
EmtMadridExample.kt       Direct Wear OS Compose example
EmtMadridArrival.kt       EMT client and arrival parser
EmtMadridArrivalTest.kt   JVM tests for token and arrival parsing
transit/MadridTransitRuntime.kt Runtime integration used by Madrid Wrist
```
