# Complications baseline

The app includes a Wear OS complication data source for Madrid Wrist. It exposes
raw `SHORT_TEXT` data to compatible watch faces; the watch face remains
responsible for rendering that data. The Activity is the only surface that
refreshes Metro and EMT data, and the complication reads the last cached snapshot
for the selected context.

## Runtime behavior

When cached data is available, the complication data source returns:

- Short text: a compact headline such as `E3 4m`.
- Short title: the context, such as `Casa`.
- Content description: a fuller Madrid Wrist transit description with the cached
  update time.

Without cached data, it returns `MAD` plus the selected context when available,
and a build-timestamp fallback in the content description.

The update period is 300 seconds, which is the minimum regular update interval
recommended by the platform for battery-friendly complication polling.

## Code map

```text
WearLoopComplicationService.kt        Data source service
WearLoopComplicationContent.kt        Testable strings and update interval
WearLoopComplicationContentTest.kt    JVM tests for cached and fallback text
transit/MadridTransitSnapshot.kt      Cached next-arrival snapshot model and codec
ic_complication_wear_loop.xml         Monochrome picker icon
```

## Dependencies

The data source uses AndroidX Watch Face complication APIs:

```kotlin
implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")
```

This app is not implementing a watch face. It only provides data that compatible
watch faces can select.

## Build verification

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

## Device verification

Install the app on the watch:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
```

Then choose a watch face that supports `SHORT_TEXT` complications and select the
`Madrid Wrist` data source from the complication picker.

Expected data source metadata:

```text
Label: Madrid Wrist
Supported type: SHORT_TEXT
Update period: 300 seconds
Service: com.arfipod.wearosplayground.WearLoopComplicationService
```

Complication data sources are not launched like activities. They are requested
by the Wear OS system when a watch face slot uses them.
