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

The direct EMT example target is stop `1064`, line `E3`, destination
`VALDERRIVAS`. The Madrid Wrist product UI can load multiple EMT stop targets
from its local catalog.

## Credential Configuration

Do not commit EMT credentials. Build with the app credentials from MobilityLabs:

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

The helper scripts automatically load `.env`, including Docker builds. You can
also provide credentials for a single build:

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

At runtime the direct EMT example:

```text
1. Logs in with X-ClientId and passKey, or email and password.
2. Extracts the returned accessToken.
3. Calls the arrivals endpoint for stop 1064.
4. Filters arrivals whose destination contains VALDERRIVAS.
5. Shows the next bus in minutes, plus distance when provided.
```

## Replicable Emulator Validation

Use this flow to reproduce the EMT end-to-end check on a Wear OS emulator. Keep
all credentials in environment variables or Gradle properties; do not write real
values into tracked files.

### 1. Check the workstation

Start from the repository root:

```bash
./scripts/doctor.sh
rg -n "angelrubiodev|com\\.arfipod|WearLoop" app scripts docs README.md
```

Expected basics:

```text
Docker: OK when available
ADB: OK
APK path: app/build/outputs/apk/debug/app-debug.apk
ADB devices: may be empty before starting the emulator
```

The local `gradle` binary can be missing; the checked-in Gradle wrapper is used
for the build.

### 2. Export EMT credentials

Protected app login:

```bash
export EMT_CLIENT_ID="YOUR_CLIENT_ID"
export EMT_PASS_KEY="YOUR_PASS_KEY"
```

Basic MobilityLabs login, useful when protected login returns `403` code `84`:

```bash
export EMT_EMAIL="YOUR_EMAIL"
export EMT_PASSWORD="YOUR_PASSWORD"
```

When both modes are set, the app tries `X-ClientId`/`passKey` first and then
falls back to `email`/`password`.

### 3. Optional API smoke test

This confirms that MobilityLabs returns an `accessToken` before rebuilding the
APK:

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

The endpoint is real-time, so the exact arrivals change. A successful response
uses `code=00`; the app can still show `No E3 arrival` if there is no matching
E3 bus at that moment.

### 4. Build a credentialed debug APK

Use a clean build when changing EMT credentials so `BuildConfig` is regenerated:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:clean :app:testDebugUnitTest :app:assembleDebug
```

If you do not need a clean rebuild, this is the normal verification command:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

To verify that credentials reached `BuildConfig` without printing their values:

```bash
perl -ne '
  for my $name (qw(EMT_CLIENT_ID EMT_PASS_KEY EMT_EMAIL EMT_PASSWORD)) {
    if (/public static final String $name = "(.*)";/) {
      print "$name length=" . length($1) . "\n"
    }
  }
' app/build/generated/source/buildConfig/debug/com/arfipod/wearosplayground/BuildConfig.java
```

The checked-in files should never contain real credential values:

```bash
rg -n "YOUR_REAL_EMAIL|YOUR_REAL_PASSWORD|YOUR_REAL_TOKEN" app docs scripts README.md .github
```

Replace the placeholders above with short fragments that would identify the
secrets in your own shell history or notes.

### 5. Start a Wear OS emulator

List installed AVDs:

```bash
source ./scripts/common.sh
"$ANDROID_HOME/emulator/emulator" -list-avds
```

The rectangular Wear OS 6.1 AVD was the most reliable during validation:

```bash
source ./scripts/common.sh
"$ANDROID_HOME/emulator/emulator" \
  -avd Wear_OS_6_1_Rect \
  -no-window \
  -no-audio \
  -no-boot-anim \
  -gpu swiftshader_indirect
```

In another terminal, wait for the framework and package manager before
installing:

```bash
for _ in $(seq 1 180); do
  serial="$(adb devices | awk '/emulator-[0-9]+[[:space:]]+device/{print $1; exit}')"
  if [ -n "$serial" ] &&
    adb -s "$serial" shell ps -A 2>/dev/null | rg -q ' system_server$' &&
    adb -s "$serial" shell cmd package list packages >/dev/null 2>&1; then
    echo "framework/package ready on $serial"
    export ANDROID_SERIAL="$serial"
    break
  fi
  sleep 2
done
```

Do not install as soon as `adb devices` says `device`; early in boot ADB can be
ready while Android still returns `cmd: Can't find service: package`.

### 6. Install and launch the EMT example

If the emulator already has a debug APK signed by another key, uninstall first
to avoid `INSTALL_FAILED_UPDATE_INCOMPATIBLE`:

```bash
source ./scripts/common.sh
export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"

adb_cmd uninstall "$APP_ID" >/dev/null 2>&1 || true
./scripts/install-watch.sh
adb_cmd logcat -c
```

The emulator can show a Wear charging overlay above the app. Turn off virtual AC
power and wake/dismiss before launching:

```bash
adb emu power ac off || true
adb emu power capacity 78 || true
adb_cmd shell input keyevent KEYCODE_WAKEUP
adb_cmd shell input keyevent BACK || true
```

Launch the EMT screen directly:

```bash
adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example emt
```

### 7. Capture runtime evidence

Wait for the network call, then collect screenshot and logs:

```bash
sleep 15
./scripts/screenshot-watch.sh
adb_cmd logcat -d -s WearLoop AndroidRuntime ActivityTaskManager | tail -180
```

Successful validation looks like:

```text
WearLoop: MainActivity created. Build timestamp=...
WearLoop: EMT E3 next arrival 8 min
```

The screenshot should show the EMT screen with minutes, line/destination, and
meters, for example:

```text
EMT E3
E3 stop 1064 -> Valderrivas
8 min
E3 -> VALDERRIVAS
2615 m
```

Screenshot files are written under:

```text
artifacts/screenshots/
```

### 8. Shut down the emulator

```bash
adb -s "$ANDROID_SERIAL" emu kill || true
```

If the emulator was started in the foreground, wait for the command to finish so
the snapshot is saved and no session remains running.

## Troubleshooting Notes

- Protected login with `X-ClientId` and `passKey` can return HTTP `403` with
  EMT code `84`; basic `email`/`password` login can still return a usable
  `accessToken`.
- Rebuild with `:app:clean` after changing credential environment variables;
  otherwise a stale APK can still show `Set EMT login`.
- `adb devices` is not enough for install readiness. Wait for `system_server`
  and `cmd package list packages`.
- If install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall
  `com.arfipod.wearosplayground.debug` from the emulator and reinstall.
- If the screenshot shows the Wear charging screen, use `adb emu power ac off`
  and relaunch the activity.
- The EMT response contains nested objects such as `geometry`; parser changes
  should be covered by JVM tests with the current `Arrive` response shape.
- EMT arrivals are real-time. `No E3 arrival` can be a valid runtime state when
  the API returns no matching E3 bus for stop `1064`.

## Code Map

```text
EmtMadridExample.kt       Wear OS Compose screen
EmtMadridArrival.kt       EMT client and arrival parser
EmtMadridArrivalTest.kt   JVM tests for token and arrival parsing
```
