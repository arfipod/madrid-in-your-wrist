# Sensor Logging Experiments

The repository keeps a small accelerometer logging helper for future opt-in
experiments. The current Madrid Wrist launcher does not expose a sensor toggle.

## Behavior

When a future screen wires the helper in, it should:

- Register `Sensor.TYPE_ACCELEROMETER`.
- Log one formatted sample per second with the `WearLoop` tag.
- Log start, stop, unavailable sensor, and accuracy changes.
- Stop listening when the owning screen or feature is disposed.

Accelerometer logging does not require a runtime permission. Future sensors
should document permissions and power cost before being exposed.

Expected log shape:

```text
WearLoop: Sensor experiment started: ...
WearLoop: Accelerometer t=...ms x=... y=... z=... |g|=...
WearLoop: Sensor experiment stopped
```

## Code Map

```text
SensorExperimentLogger.kt      Android SensorEventListener wrapper
SensorSampleFormatter.kt       Pure formatter used by the logger
SensorSampleFormatterTest.kt   JVM tests for stable formatting
```

## Verify

The helper is covered by JVM tests:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest
```

Runtime verification requires a screen or explicit route that starts
`SensorExperimentLogger`; Madrid Wrist does not currently start it.
