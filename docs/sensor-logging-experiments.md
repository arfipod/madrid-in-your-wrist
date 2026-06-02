# Sensor logging experiments

The first sensor experiment is deliberately small: the app can log accelerometer
samples to `logcat` without adding new dependencies or background services.

## Runtime behavior

The Compose UI includes a `SENSORS OFF` / `SENSORS ON` button.

When enabled, the app:

- Registers a listener for `Sensor.TYPE_ACCELEROMETER`.
- Logs one formatted sample per second with the `WearLoop` tag.
- Logs sensor start, stop, availability, and accuracy changes.
- Stops listening when the Activity is destroyed.

Accelerometer logging does not require a runtime permission. Keep future sensor
experiments equally explicit about their permissions and power cost.

## Verification

Build, install, and launch:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:assembleDebug
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
```

Tap `SENSORS OFF` on the watch, then inspect logs:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/logcat-watch.sh
```

Expected log lines include:

```text
WearLoop: Sensor experiment started: ...
WearLoop: Accelerometer t=...ms x=... y=... z=... |g|=...
```

Tap `SENSORS ON` again to stop logging:

```text
WearLoop: Sensor experiment stopped
```

## Code map

- `SensorExperimentLogger.kt`: Android `SensorEventListener` wrapper.
- `SensorSampleFormatter.kt`: pure formatter used by the logger.
- `SensorSampleFormatterTest.kt`: JVM tests for stable formatting.

Keep formatting and throttling testable before adding more sensors.

