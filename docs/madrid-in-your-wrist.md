# Madrid Wrist

`Madrid Wrist` is the product surface of this repository. The app still keeps
the closed-loop Wear OS build/install/screenshot workflow, but the launcher now
opens a transport dashboard instead of the developer examples gallery.

## Behavior

The watch app can be operated entirely from Wear OS:

- Shows a persisted list of Metro and bus favorites grouped into generic
  profiles: `Perfil 1`, `Perfil 2`, and `Perfil 3`.
- Shows a large top glance card with the soonest cached or freshly loaded
  departure/arrival for the selected profile.
- Adds Metro stations and EMT bus stops from separate search/picker screens into
  the currently selected profile.
- Opens the Wear OS keyboard for text search and matches the local catalog by
  station, stop, stop ID, line, destination, and aliases without requiring
  accents.
- Uses Metro de Madrid line colors for Metro labels and top glance highlights.
- Lets each favorite choose how many upcoming departures or arrivals to show, but
  keeps count/delete controls behind an explicit `EDIT` mode to make the default
  path glance-first.
- Lets each favorite opt into distance activation with a proximity radius of
  `500m`, `1km`, or `2km`. The setting is stored with the favorite and can be
  evaluated against the last known watch location.
- Refreshes only the currently selected profile from the watch. Other profiles
  keep their last successful cached snapshot until the user switches to them and
  refreshes.
- Avoids automatic API refreshes when a recent complete snapshot already covers
  the selected profile. Tapping `↻` still forces a live refresh when online.
- Uses validated internet when available. Without internet, or when an API
  refresh fails, it falls back to the selected profile's cached snapshot instead
  of clearing useful data.
- Requests location permission from the watch and sorts the local catalog by
  nearest options when a last known location is available.
- Uses only last-known location, rechecked on manual refresh, nearby flows, or at
  most every 10 minutes for proximity-triggered automatic refreshes.
- Feeds the Wear OS Tile and `SHORT_TEXT` complication from the last cached
  snapshot instead of doing network work from those glance surfaces.

Favorites, counts, selected profile, proximity trigger settings, and the latest
transit snapshot are stored in `SharedPreferences`, so the watch keeps useful
data between app launches. The first launch seeds one Metro favorite and one EMT
bus favorite in `Perfil 1`.

## Data Sources

Metro uses the NAP/GTFS implementation documented in
[`metro-madrid-nap.md`](metro-madrid-nap.md). It downloads the Metro de
Madrid GTFS-ZIP dataset with `BuildConfig.NAP_API_KEY` and computes upcoming
scheduled departures locally.

Bus uses the EMT Madrid MobilityLabs implementation documented in
[`emt-madrid-openapi.md`](emt-madrid-openapi.md). It logs in with
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

Search currently runs against this local catalog. Expanding it to literally all
Metro stations and EMT stops means feeding a complete catalog into
`MadridTransitCatalog`; the watch search UI and matcher are already prepared for
that larger list.

## Favorites And Profiles

Profiles are generic buckets for transit favorites. A favorite can represent a
Metro line/station/destination pair or an EMT line/stop/destination pair. The
same station or stop can be saved in more than one profile, with independent
counts and proximity trigger settings.

The current watch UI supports three built-in profiles. Stored legacy IDs from
earlier personal labels are still decoded and migrated to the generic profile
IDs when favorites are saved again.

## Online And Offline Policy

The Activity is the only surface that talks to Metro/EMT APIs. Before refreshing
it checks for validated internet:

- Automatic refresh: if the selected profile already has a complete snapshot
  saved within the last 3 minutes, render that cache and skip Metro/EMT calls.
- Manual refresh: tapping `↻` always attempts a live refresh when online.
- Proximity triggers: automatic refreshes only call APIs for favorites that are
  manual or inside their selected radius. Favorites outside the radius keep their
  cached data; manual refresh still requests every selected favorite.
- Location: the app never starts continuous location tracking for Madrid Wrist.
  It reads the platform's last-known location on nearby/manual/proximity flows
  and throttles automatic proximity checks to at most once every 10 minutes.
- Online: call the configured Metro and EMT helpers, then save a fresh snapshot
  for options that returned live data.
- Partial online failure: merge fresh results with the previous snapshot. A
  favorite that refreshed successfully replaces its cached item; a favorite whose
  API/config failed keeps its cached item if one exists.
- Online success with no arrivals: clear that favorite's cached item, because the
  API has explicitly reported no upcoming transport.
- Offline: skip network calls entirely and show the last cached snapshot for the
  selected profile. If no cache exists, show a clear no-connection/no-data state.

Tiles and complications always read the stored snapshot and never start network
work themselves. The snapshot stores both the display time (`HH:mm`) and a real
epoch timestamp so the Activity can decide whether automatic cache use is still
fresh.

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
network work from surfaces that should be readable in a few seconds. Their
regular update cadence is intentionally modest: 15 minutes for the Tile and 900
seconds for the complication.

## Screenshots

Captured from the installed debug APK on a real Pixel Watch 3.

![Madrid Wrist home top](images/madrid-wrist/home-top.png)

![Madrid Wrist favorites controls](images/madrid-wrist/home-favorites.png)

![Madrid Wrist add actions](images/madrid-wrist/home-actions.png)

![Madrid Wrist expanded actions](images/madrid-wrist/home-actions-expanded.png)

![Madrid Wrist nearby list](images/madrid-wrist/nearby-list.png)

## Code Map

```text
MainActivity.kt                    Launcher and direct example-route bridge
MadridInYourWristApp.kt            Wear OS Compose product UI
transit/MadridTransitModels.kt     Catalog, profiles, favorites, counts, distance sorting
transit/MadridMetroLineColors.kt   Metro, Ramal, and Metro Ligero color palette
transit/MadridTransitSearch.kt     Accent-insensitive station/stop/line matcher
transit/MadridTransitStore.kt      SharedPreferences favorites/profile persistence
transit/MadridTransitSnapshot.kt   Cached next-arrival snapshot for Activity, Tile, complication
transit/MadridTransitRefreshPolicy.kt Online/offline cache decision policy
transit/MadridTransitRuntime.kt    Runtime loading across Metro and bus favorites
transit/MadridLocationProvider.kt  Platform last-known-location helper
transit/MadridNetworkProvider.kt   Platform validated-internet helper
WearLoopTileService.kt             ProtoLayout Tile fed by cached selected-profile snapshot
WearLoopComplicationService.kt     SHORT_TEXT complication fed by cached selected-profile snapshot
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
