# Complications baseline

The app includes a minimal Wear OS complication data source. It exposes raw
`SHORT_TEXT` data to compatible watch faces; the watch face remains responsible
for rendering that data.

## Runtime behavior

The complication data source returns:

- Short text: `Loop`
- Short title: `Ready`
- Content description: `Wear Loop ready. Build ...`

The update period is 300 seconds, which is the minimum regular update interval
recommended by the platform for battery-friendly complication polling.

## Code map

```text
WearLoopComplicationService.kt        Data source service
WearLoopComplicationContent.kt        Testable strings and update interval
WearLoopComplicationContentTest.kt    JVM tests for short text and metadata
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
`Wear Loop Status` data source from the complication picker.

Expected data source metadata:

```text
Label: Wear Loop Status
Supported type: SHORT_TEXT
Update period: 300 seconds
Service: com.arfipod.wearosplayground.WearLoopComplicationService
```

Complication data sources are not launched like activities. They are requested
by the Wear OS system when a watch face slot uses them.

