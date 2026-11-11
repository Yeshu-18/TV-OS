# FireLite — Custom Fire TV Launcher & Phone Companion

FireLite is a lightweight, Apple TV-inspired home launcher for Amazon Fire TV (`:tv`) paired with an Android phone companion remote (`:companion`) and shared Kotlin core (`:core`). Built with Kotlin, Jetpack Compose, and Kotlin Serialization (`compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`, `jvmToolchain(17)`).

---

## Project Modules

| Module | Namespace / Application ID | Version | Purpose |
|---|---|---|---|
| `:core` | `com.firelite.core` | Library | Shared models, TCP remote protocol (`port = 8765`), pin repository, and verified streaming deep links |
| `:tv` | `com.firelite.tv` | `1.0` (`versionCode = 1`) | Fire TV launcher (`HOME` / `LEANBACK_LAUNCHER`), app shelf, 10-item LRU Recently Watched, overlays, and TCP remote server |
| `:companion` | `com.firelite.companion` | `1.0` (`versionCode = 1`) | Android phone companion UI (`Remote`, `Apps`, `Pins`, `Search`, `More`, `Screen Mirror`) and TCP client |

---

## Target Hardware & Tested Devices

- **Fire TV:** Amazon Fire TV Stick (`AFTSSS`, codename `sheldonp`), Fire OS `7.7.1.7` (Android release `9`), `MT8695`, ~900 MB RAM, 4.9 GB `/data` partition.
- **Phone Companion:** Tested APK installation on `CPH2707IN` (`companion-debug.apk`).

---

## Architecture Overview

### `:core` (Shared Data & Network Protocol)
- [`HotstarDeepLinks`](core/src/main/kotlin/com/firelite/core/data/HotstarDeepLinks.kt): Canonical JioHotstar (`in.startv.hotstar`) IDs and URIs (`showDetailsUri`, `showWatchUri`), URI normalization (`normalizeUrl`, `normalizeMediaItem`), and explicit target package resolution (`packageForUrl`, including `zee5.com` -> `com.graymatrix.did`).
- [`FireLiteRepository`](core/src/main/kotlin/com/firelite/core/data/FireLiteRepository.kt): Persists categorized pins in `SharedPreferences` (`firelite_pins_store` using `pins_kids_v1` and `pins_series_v1`), migrates stored pin URLs on `loadSavedPins`, and provides initial curated defaults (6 Kids, 7 Series, 4 What's New, and 8 Dock items).
- [`RemoteProtocol`](core/src/main/kotlin/com/firelite/core/model/RemoteProtocol.kt) & [`CompanionBridge`](core/src/main/kotlin/com/firelite/core/network/CompanionBridge.kt): Defines `RemoteKey` (`UP`, `DOWN`, `LEFT`, `RIGHT`, `SELECT`, `BACK`, `HOME`, `PLAY_PAUSE`, `VOLUME_UP`, `VOLUME_DOWN`, `MUTE`) and `RemoteCommand` (`PressKey`, `SendText`, `AddPin`, `RemovePin`, `MoveCategory`, `ReorderPins`, `RequestSync`, `SyncState`) exchanged as newline-delimited JSON over TCP port `8765` between `TvRemoteServer` and `CompanionRemoteClient`.

### `:tv` (Fire TV Launcher)
- [`AndroidManifest.xml`](tv/src/main/AndroidManifest.xml) & [`MainActivity`](tv/src/main/kotlin/com/firelite/tv/MainActivity.kt): Declares `MAIN`, `LAUNCHER`, `LEANBACK_LAUNCHER`, `HOME`, and `DEFAULT` intent filters on a `singleTask` landscape `MainActivity`, initializes `FireLiteRepository`, starts `TvRemoteServer(port = 8765)`, and dispatches pin updates (`AddPin`, `RemovePin`, `MoveCategory`, `ReorderPins`) plus `PressKey` / `SendText` events.
- [`FireLiteTvScreen`](tv/src/main/kotlin/com/firelite/tv/ui/FireLiteTvScreen.kt) & [`MyFavouritesSection`](tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt): Renders the top bar (`"FIRELITE • YOUR SHOWS • YOUR WAY"`), `APPLICATIONS`, `RECENTLY WATCHED` (when populated), the two-row interactive `MY FAVOURITES` character lineup with instant darkened B&W scene crossfade (`FavouritesSceneBackdrop`), `ScreenMirrorOverlay`, and `SettingsOverlay`, refreshing Watch Next / LRU entries on resume (`ON_RESUME`).
  - `launchContent` normalizes target deep links via `HotstarDeepLinks.normalizeUrl(item.contentUrl, preferWatch = false)`, logs the watch into the 10-item LRU cache via `TvWatchNextManager.recordWatch(context, item)`, and opens `Intent.ACTION_VIEW` (`FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`) with Leanback fallback.
  - `openTvSettings` invokes Fire TV settings (`MainSettingsActivity`, `tv.settings.v2.tv.HomeActivity`, or `ACTION_SETTINGS`).
- [`TvAppManager`](tv/src/main/kotlin/com/firelite/tv/util/TvAppManager.kt): Resolves `CATEGORY_LEANBACK_LAUNCHER` apps on `Dispatchers.IO`, pre-caches icons in parallel via `AppIconLoader.getAppIcon` (`async`/`awaitAll`), provides `getInstantFallbackApps()`, applies `BRAND_CONFIGS` (`hotstar`, `netflix`, `firebat`, `youtube`, `zee5`, `sunnxt`, `ondemand`, `jiotvplus`), and filters `BLOCKED_PACKAGES` (12 Amazon ad/promo/bloat packages).
- [`TvWatchNextManager`](tv/src/main/kotlin/com/firelite/tv/util/TvWatchNextManager.kt): Manages a thread-safe 10-item [`RecentlyWatchedLruCache`](tv/src/main/kotlin/com/firelite/tv/util/TvWatchNextManager.kt) persisted in `SharedPreferences` (`firelite_continue_watching` / `recently_watched_lru_v1`) merged chronologically with any available `content://android.media.tv/watch_next_program` rows, without injecting dummy items or pinned overrides.

### `:companion` (Phone Remote App)
- [`MainActivity`](companion/src/main/kotlin/com/firelite/companion/MainActivity.kt) & [`CompanionScreens`](companion/src/main/kotlin/com/firelite/companion/ui/CompanionScreens.kt): Implements the `Remote`, `Apps`, `Pins`, `Search`, `More`, and `Screen Mirror` (`selectedTab == 5`) views along with `PairingDialog` and `LiveKeyboardDialog`.
- **Known Limitation:** `MainActivity` currently calls `client.connect("127.0.0.1")` at startup and `PairingDialog` uses a hardcoded default code (`"8294"`) without a TV IP input field. Connecting over LAN from a phone requires adding configurable TV IP entry in the companion UI before calling `CompanionRemoteClient.connect(...)`.

---

## Build, Test & Deploy

### Build Debug APKs
```bash
./gradlew :tv:assembleDebug :companion:assembleDebug
```
Generated APKs:
- `tv/build/outputs/apk/debug/tv-debug.apk`
- `companion/build/outputs/apk/debug/companion-debug.apk`

### Run Unit Tests
```bash
./gradlew :tv:testDebugUnitTest
```
Unit tests live in [`MainScreenViewModelTest.kt`](tv/src/test/kotlin/com/example/firelite/ui/main/MainScreenViewModelTest.kt) (`FireLiteRepositoryTest`). Note that `:core` and `:companion` currently do not contain unit test source files.

### Install & Launch via ADB
```bash
# Fire TV (e.g., 192.168.0.46:5555)
adb -s <TV_IP>:5555 install -r tv/build/outputs/apk/debug/tv-debug.apk
adb -s <TV_IP>:5555 shell am start -n com.firelite.tv/.MainActivity

# Phone Companion
adb -s <PHONE_SERIAL> install -r companion/build/outputs/apk/debug/companion-debug.apk
```

### Reversion & Safety
All FireLite installations and Fire TV debloat steps are non-destructive and reversible:
- Uninstalling `com.firelite.tv` restores stock Amazon launcher routing without touching system partitions.
- Package optimizations use reversible user-0 disablement (`pm disable-user --user 0`) and AppOps restrictions rather than irreversible root/system deletions. See [`FIRE_TV_DEBLOAT_AND_OPTIMIZATION.md`](docs/FIRE_TV_DEBLOAT_AND_OPTIMIZATION.md) for the complete rollback command list.

---

## Documentation Index

- **[Fire TV Debloat & Optimization](docs/FIRE_TV_DEBLOAT_AND_OPTIMIZATION.md)** — Amazon telemetry/ad/bloat disablement on `AFTSSS` (`sheldonp`), AppOps restrictions, uninstalled third-party apps, critical whitelisted system components, and ADB rollback instructions.
- **[Fire TV Watch Next & Data-Source Audit](docs/FIRE_TV_WATCH_NEXT_AND_DATA_SOURCES.md)** — Investigation of `content://android.media.tv/watch_next_program`, `content://amazon.media.tv/watch_next_program`, JioHotstar's provider restrictions (`androidtv.home.watch_next = false`), OS dump telemetry (`notification`, `media_session`, `audio`, `recents`), and FireLite's 10-item LRU `Recently Watched` design in `TvWatchNextManager`.
- **[JioHotstar Deep Linking & Playback Guide](docs/JIOHOTSTAR_DEEP_LINKING_AND_PLAYBACK.md)** — Canonical `hotstar.com` sitemap discovery, `hotstar://in/...` URI taxonomy, live ADB playback verification under profile `Nanda kumar`, `Kids` profile maturity restriction (`ERR_PB_1426`), FireLite `Continue Watching` end-to-end launch verification, and Phone Companion install notes.
- **[My Favourites Lineup & Customization Guide](docs/MY_FAVOURITES_GUIDE.md)** — Two-row character lineup specification, cutout & 16:9 scene asset rules, `FAV_RES_MAP` & deep-link playbooks, one-shot animation/extra-layer customization, D-pad focus & `BlendMode.DstIn` invariants, and verification checklist.
