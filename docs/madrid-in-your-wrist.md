# Madrid In Your Wrist

`Madrid Wrist` is the product surface of this repository. The app still keeps
the closed-loop Wear OS build/install/screenshot workflow, but the launcher now
opens a transport dashboard instead of the old playground home.

## Runtime Behavior

The watch app can be operated entirely from Wear OS:

- Shows a persisted list of Metro and bus favorites.
- Adds Metro stations and EMT bus stops from separate pickers.
- Lets each favorite choose how many upcoming departures or arrivals to show.
- Refreshes all favorites from the watch.
- Requests location permission from the watch and sorts the local catalog by
  nearest options when a last known location is available.

Favorites are stored in `SharedPreferences`, so the watch keeps the list between
app launches. The first launch seeds one Metro favorite and one EMT bus favorite.

## Data Sources

Metro uses the NAP/GTFS implementation documented in
[`docs/metro-madrid-nap.md`](metro-madrid-nap.md). It downloads the Metro de
Madrid GTFS-ZIP dataset with `BuildConfig.NAP_API_KEY` and computes upcoming
scheduled departures locally.

Bus uses the EMT Madrid MobilityLabs implementation documented in
[`docs/emt-madrid-openapi.md`](emt-madrid-openapi.md). It logs in with
`BuildConfig.EMT_CLIENT_ID`/`BuildConfig.EMT_PASS_KEY` or
`BuildConfig.EMT_EMAIL`/`BuildConfig.EMT_PASSWORD`, then calls the stop arrivals
endpoint.

The initial EMT bus catalog is intentionally small and curated from EMT's public
E3 line page:

```text
https://www.emtmadrid.es/EMTBUS/Mi-linea?linea=E3
```

That page lists E3 stops including `FELIPE II - 755`,
`AVENIDA DE DAROCA-CASALARREINA - 1064`, and `VALDERRIVAS - 5116`.

## Current Catalog

Metro:

```text
Arguelles L4 -> Pinar de Chamartin
Goya L2      -> Las Rosas
Sol L1       -> Pinar de Chamartin
```

Bus:

```text
Felipe II E3    -> Valderrivas
Daroca E3       -> Valderrivas
Valderrivas E3  -> Felipe II
```

The catalog is data-driven in `MadridTransitCatalog`, so adding more stations or
stops should not require changing the UI flow.

## Screenshots

Captured from the installed debug APK on a real Pixel Watch 3.

![Madrid Wrist home top](images/madrid-wrist/home-top.png)

![Madrid Wrist favorites controls](images/madrid-wrist/home-favorites.png)

![Madrid Wrist add actions](images/madrid-wrist/home-actions.png)

![Madrid Wrist expanded actions](images/madrid-wrist/home-actions-expanded.png)

![Madrid Wrist nearby list](images/madrid-wrist/nearby-list.png)

## Code Map

```text
MainActivity.kt                 Launcher and direct example-route bridge
MadridInYourWristApp.kt         Wear OS Compose product UI
transit/MadridTransitModels.kt  Catalog, favorites, counts, distance sorting
transit/MadridTransitStore.kt   SharedPreferences persistence and codec
transit/MadridTransitRuntime.kt Runtime loading across Metro and bus favorites
transit/MadridLocationProvider.kt Platform last-known-location helper
```

## Verification

Preferred local verification:

```bash
source ./scripts/common.sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

Runtime validation when a watch or emulator is connected:

```bash
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/install-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/launch-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/screenshot-watch.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT ./scripts/logcat-watch.sh
```

Direct example routes still work for development:

```bash
source ./scripts/common.sh
ANDROID_SERIAL=WATCH_IP:ADB_PORT adb_cmd shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --es example metro
```
