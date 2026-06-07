# Madrid Wrist Complication

The app includes a Wear OS `SHORT_TEXT` complication data source for Madrid
Wrist. The Activity is the only surface that refreshes Metro and EMT data; the
complication reads the last cached snapshot for the selected profile.

## Behavior

With cached data:

```text
Short text:        E3 4m
Short title:       brief selected-profile icon, for example heart/home/briefcase
Description:       Madrid Wrist transit summary with cached update time
Update interval:   900 seconds
```

Without cached data, it returns `MAD`, the selected profile when available, and
a build-timestamp fallback in the content description.

The app only provides raw complication data. Watch faces decide how to render it.

## Code Map

```text
WearLoopComplicationService.kt        Data source service
WearLoopComplicationContent.kt        Testable strings and update interval
WearLoopComplicationContentTest.kt    JVM tests
transit/MadridTransitSnapshot.kt      Cached next-arrival snapshot model and codec
ic_complication_wear_loop.xml         Monochrome picker icon
```

## Dependency

```kotlin
implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")
```

## Verify

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
```

Then choose a watch face that supports `SHORT_TEXT` complications and select
`Madrid Wrist` from the complication picker.

Expected provider metadata:

```text
Label: Madrid Wrist
Supported type: SHORT_TEXT
Update period: 900 seconds
Service: com.arfipod.madridinyourwrist.WearLoopComplicationService
```
