# Examples Gallery

The app includes a compact examples gallery under:

```text
app/src/main/java/com/arfipod/wearosplayground/examples/
```

Open the app and tap one of the example buttons from the home screen, or launch
an example directly through ADB with the optional `example` intent extra.

```bash
source ./scripts/common.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example flappy
```

Supported route values:

```text
flappy
api
metro
emt
3d
audio
video
```

## Screenshots

Captured from the installed debug app on Wear OS targets.

![Wear Loop examples home](images/examples/wear-home.png)

## Included Examples

### Flappy Bird

`FlappyBirdExample.kt` renders a tiny Flappy Bird-style loop in Compose Canvas.
Tap the play area to flap. The pure game state lives in `FlappyBirdLogic` and is
covered by JVM tests.

![Flappy Bird example](images/examples/wear-flappy.png)

### API Output

`ApiExample.kt` performs a small `HttpURLConnection` GET to:

```text
https://api.github.com/zen
```

The response is compacted by `ApiOutputFormatter` before being shown on the
watch. The app manifest includes `android.permission.INTERNET` for this example.

![API output example](images/examples/wear-api.png)

### Metro Madrid

`MetroMadridExample.kt` uses the NAP API key exposed as `BuildConfig.NAP_API_KEY`
to download the Metro de Madrid GTFS-ZIP dataset and calculate the next scheduled
Line 4 departure from `Argüelles` toward `Pinar de Chamartín`.

Configure the key at build time:

```bash
NAP_API_KEY=YOUR_KEY ./gradlew --no-daemon :app:assembleDebug
```

Or:

```bash
./gradlew --no-daemon -Pnap.apiKey=YOUR_KEY :app:assembleDebug
```

Direct route:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example metro
```

See `docs/metro-madrid-nap.md` for the account/key flow and implementation
notes.

![Metro Madrid example](images/examples/wear-metro.png)

### EMT E3

`EmtMadridExample.kt` uses MobilityLabs credentials exposed as
`BuildConfig.EMT_CLIENT_ID`/`BuildConfig.EMT_PASS_KEY` or
`BuildConfig.EMT_EMAIL`/`BuildConfig.EMT_PASSWORD` to show the next E3 arrival
from EMT stop `1064` toward `Valderrivas`.

Configure credentials at build time:

```bash
EMT_CLIENT_ID=YOUR_CLIENT_ID EMT_PASS_KEY=YOUR_PASS_KEY \
  ./gradlew --no-daemon :app:assembleDebug
```

Or:

```bash
EMT_EMAIL=YOUR_EMAIL EMT_PASSWORD=YOUR_PASSWORD \
  ./gradlew --no-daemon :app:assembleDebug
```

Or:

```bash
./gradlew --no-daemon \
  -Pemt.clientId=YOUR_CLIENT_ID \
  -Pemt.passKey=YOUR_PASS_KEY \
  :app:assembleDebug
```

Or:

```bash
./gradlew --no-daemon \
  -Pemt.email=YOUR_EMAIL \
  -Pemt.password=YOUR_PASSWORD \
  :app:assembleDebug
```

Direct route:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example emt
```

See `docs/emt-madrid-openapi.md` for registration and endpoint notes.

![EMT E3 example](images/examples/wear-emt.png)

### 3D Cube

`ThreeDExample.kt` projects a rotating cube in software and renders it with
Compose Canvas. This keeps the dependency surface small while demonstrating
basic 3D math on a Wear OS screen.

![3D cube example](images/examples/wear-3d.png)

### Audio

`AudioExample.kt` plays a short tone sequence with Android's platform
`ToneGenerator`. The testable `AudioExampleMelody` object defines the sequence
duration and visual meter behavior.

![Audio example](images/examples/wear-audio.png)

### Video

`VideoExample.kt` hosts a platform `VideoView` through Compose `AndroidView` and
streams a small HTTPS MP4:

```text
https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4
```

Playback loops after preparation. Network availability on the watch or emulator
is required.

![Video example](images/examples/wear-video.png)

## Verification

Preferred build verification:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

Runtime validation on each available ADB target:

```bash
source ./scripts/common.sh
for example in flappy api metro emt 3d audio video; do
  ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
    -n "$APP_ID/$MAIN_ACTIVITY" \
    --es example "$example"
  sleep 4
  ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
done
```

Use the actual emulator or watch serial in `ANDROID_SERIAL`. If multiple ADB
targets are connected, never omit `ANDROID_SERIAL`.
