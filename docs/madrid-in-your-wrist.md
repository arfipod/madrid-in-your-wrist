# Madrid In Your Wrist

`Madrid Wrist` is the product surface of this repository. The app still keeps
the closed-loop Wear OS build/install/screenshot workflow, but the launcher now
opens a transport dashboard instead of the old playground home.

## Runtime Behavior

The watch app can be operated entirely from Wear OS:

- Shows a persisted list of Metro and bus favorites grouped into generic
  profiles: `Perfil 1`, `Perfil 2`, and `Perfil 3`.
- Shows a large top glance card with the soonest cached or freshly loaded
  departure/arrival for the selected profile.
- Adds Metro stations and EMT bus stops from separate pickers into the currently
  selected profile.
- Lets each favorite choose how many upcoming departures or arrivals to show, but
  keeps count/delete controls behind an explicit `EDIT` mode to make the default
  path glance-first.
- Lets each favorite opt into distance activation with a proximity radius of
  `500m`, `1km`, or `2km`. The setting is stored with the favorite and can be
  evaluated against the last known watch location.
- Refreshes only the currently selected profile from the watch. Other profiles
  keep their last successful cached snapshot until the user switches to them and
  refreshes.
- Requests location permission from the watch and sorts the local catalog by
  nearest options when a last known location is available.
- Feeds the Wear OS Tile and `SHORT_TEXT` complication from the last cached
  snapshot instead of doing network work from those glance surfaces.

Favorites, counts, selected profile, proximity trigger settings, and the latest
transit snapshot are stored in `SharedPreferences`, so the watch keeps useful
data between app launches. The first launch seeds one Metro favorite and one EMT
bus favorite in `Perfil 1`.

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
Argüelles L4 -> Pinar de Chamartín
Goya L2      -> Las Rosas
Sol L1       -> Pinar de Chamartín
```

Bus:

```text
Felipe II E3    -> Valderrivas
Daroca E3       -> Valderrivas
Valderrivas E3  -> Felipe II
```

The catalog is data-driven in `MadridTransitCatalog`, so adding more stations or
stops should not require changing the UI flow.

## Favorites And Profiles

Profiles are generic buckets for transit favorites. A favorite can represent a
Metro line/station/destination pair or an EMT line/stop/destination pair. The
same station or stop can be saved in more than one profile, with independent
counts and proximity trigger settings.

The current watch UI supports three built-in profiles. Stored legacy IDs from
earlier personal labels are still decoded and migrated to the generic profile
IDs when favorites are saved again.

## Glance Surfaces

The Activity is the only surface that refreshes Metro and EMT data. The Tile and
complication read the last cached snapshot for the selected profile and format it
for quick glances:

```text
Tile title:       Perfil 1 · Madrid
Tile body:        E3 4m
Tile footer:      Daroca E3 · 08:15
Complication:     E3 4m
```

This keeps Tile/complication rendering deterministic and avoids doing expensive
network work from surfaces that should be readable in a few seconds.

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
transit/MadridTransitModels.kt    Catalog, profiles, favorites, counts, distance sorting
transit/MadridTransitStore.kt     SharedPreferences favorites/profile persistence
transit/MadridTransitSnapshot.kt  Cached next-arrival snapshot for app, Tile, complication
transit/MadridTransitRuntime.kt   Runtime loading across Metro and bus favorites
transit/MadridLocationProvider.kt Platform last-known-location helper
WearLoopTileService.kt            ProtoLayout Tile fed by cached selected-profile snapshot
WearLoopComplicationService.kt    SHORT_TEXT complication fed by cached selected-profile snapshot
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
