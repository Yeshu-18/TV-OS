# FireLite "My Favourites" Lineup — Architecture & Customization Guide

> **Scope:** Reference and step-by-step maintenance guide for editing characters, shows, cutout artwork, 16:9 scenes, blend-mode layers, one-shot animations, deep links, and D-pad focus routing in **My Favourites** (`:tv`).

---

## 1. Quick File Map (Where Everything Lives)

| Concern | File | Key Symbols / Assets |
|---|---|---|
| **Lineup data, layout, shaders, glow, leg fade & animations** | [`MyFavouritesSection.kt`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt) | `FavouriteCharacterSpec`, `buildDefaultFavouritesLineup()`, `FAV_RES_MAP`, `rememberPreloadedFavouritesAssets()`, `FavouritesSceneBackdrop()`, `MyFavouritesSection()`, `FavouritesGroupHeader()`, `FavouriteCharacterLayer()`, `drawCharacterExtraLayer()`, `drawCharacterForegroundAccent()` |
| **Screen integration, scroll offsets & D-pad focus wiring** | [`FireLiteTvScreen.kt`](../tv/src/main/kotlin/com/firelite/tv/ui/FireLiteTvScreen.kt) | `TvFocusRow`, `appRequesters`, `recentRequesters`, `firstFavouriteFocusRequester`, `ZeroVerticalBringIntoViewSpec`, `launchContent()` |
| **Character transparent cutouts & 16:9 show scenes** | `tv/src/main/res/drawable-nodpi/` | `fav_<id>.png`, `scene_fav_<id>.jpg` |
| **Poster cards for `RECENTLY WATCHED` (when `OK` clicked)** | `tv/src/main/res/drawable/` | `art_<id>.png`, `app_jiohotstar.png`, `bg_cinematic_mountain.webp` |
| **Dynamic app logo / banner loading** | [`AppIconLoader.kt`](../tv/src/main/kotlin/com/firelite/tv/util/AppIconLoader.kt) | `AppIconLoader.getAppIcon()` |
| **Streaming deep-link IDs, URIs & app package routing** | [`HotstarDeepLinks.kt`](../core/src/main/kotlin/com/firelite/core/data/HotstarDeepLinks.kt) | `PACKAGE_NAME`, `*_CONTENT_ID`, `showDetailsUri()`, `normalizeUrl()`, `packageForUrl()` |
| **10-item `Recently Watched` LRU cache** | [`TvWatchNextManager.kt`](../tv/src/main/kotlin/com/firelite/tv/util/TvWatchNextManager.kt) | `recordWatch()`, `loadWatchNextItems()`, `RecentlyWatchedLruCache` |
| **Unit tests validating lineup, z-order & D-pad neighbors** | [`MainScreenViewModelTest.kt`](../tv/src/test/kotlin/com/example/firelite/ui/main/MainScreenViewModelTest.kt) | `FireLiteRepositoryTest.myFavourites_lineupRowsZOrderAndDeepLinksAreValid` |

---

## 2. Current Two-Row Lineup Reference

The interactive stage in [`MyFavouritesSection`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt) has a maximum width of **`740.dp`** and height **`326.dp`**, centered horizontally below **RECENTLY WATCHED**. Each character's horizontal center is positioned by `xCenterFraction` (`0.0` = stage left edge, `1.0` = stage right edge), and lifted upward from the bottom edge of the stage by `bottomOffsetDp`.

| ID (`row`, `rowOrder`) | Character (`showTitle`) | `xCenterFraction` | `heightDp` | `aspectRatio` | `bottomOffsetDp` | `glowColorHex` | `touchType` | Cutout PNG (`drawable-nodpi`) | Scene JPG (`1280×720`) | JioHotstar `contentId` & Deep Link |
|---|---|---|---|---|---|---|---|---|---|---|
| `heart_beat` (`BACK`, `0`) | Dr. Reena (`Heart Beat`) | `0.22f` | `224f` | `477f / 480f` | `48f` | `0xFFF43F5E` | `HEART_BEAT_PULSE` | `fav_heart_beat.png` (`454×414`) | `scene_fav_heart_beat.jpg` | `1260168231` (`hotstar://in/shows/heart-beat/1260168231`) |
| `doraemon` (`BACK`, `1`) | Doraemon (`Doraemon`) | `0.51f` | `186f` | `473f / 532f` | `98f` | `0xFF00A3FF` | `DORAEMON_TAKECOPTER` | `fav_doraemon.png` (`473×532`) | `scene_fav_doraemon.jpg` | `1260021004` (`hotstar://in/shows/doraemon/1260021004`) |
| `the_court` (`BACK`, `2`) | Advocate (`The Court`) | `0.77f` | `286f` | `267f / 550f` | `12f` | `0xFFEAB308` | `COURT_GAVEL_SLAM` | `fav_the_court.png` (`261×541`) | `scene_fav_the_court.jpg` | `1271672560` (`hotstar://in/shows/the-court/1271672560`) |
| `siragadikka_aasai` (`FRONT`, `0`) | Muthu & Meena (`Siragadikka Aasai`) | `0.375f` | `176f` | `718f / 609f` | `12f` | `0xFFF59E0B` | `SIRAGADIKKA_SHIMMER` | `fav_siragadikka.png` (`718×606`) | `scene_fav_siragadikka.jpg` | `1260129356` (`hotstar://in/shows/siragadikka-aasai/1260129356`) |
| `shinchan` (`FRONT`, `1`) | Shinchan & Shiro (`Shinchan`) | `0.635f` | `182f` | `380f / 552f` | `12f` | `0xFFEF4444` | `SHINCHAN_COMIC_WIGGLE` | `fav_shinchan.png` (`380×552`) | `scene_fav_shinchan.jpg` | `1260020929` (`hotstar://in/shows/shin-chan/1260020929`) |

> **Stage layout note:** `bottomOffsetDp` keeps back-row upper bodies visible behind the front row (`48f` / `98f` vs `12f`), while keeping front-row bottoms near `0.70`–`0.94` of stage height so the `BlendMode.DstIn` gradient dissolves legs cleanly without leaving an empty gap.

---

## 3. Step-by-Step How-To Recipes

### 3.1 Replace an Existing Character Cutout or Scene Image

1. **Character cutout (`fav_<id>.png`)** in `tv/src/main/res/drawable-nodpi/`:
   - Use an **RGBA transparent PNG** (`8-bit/color RGBA`), sized around `300–750 px` on the long edge (placed in `drawable-nodpi/` so Android does not density-scale it unexpectedly).
   - Check edges against a black background to avoid colored matte fringes.
   - Update `aspectRatio = <pixelWidth>f / <pixelHeight>f` on that item in `buildDefaultFavouritesLineup()` ([`MyFavouritesSection.kt`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt)), and tune `heightDp`, `xCenterFraction`, and `bottomOffsetDp` if the framing changed.
2. **16:9 Scene backdrop (`scene_fav_<id>.jpg`)** in `tv/src/main/res/drawable-nodpi/`:
   - Use a **`1280×720` baseline JPEG** (a place/environment from the show with no UI text or network watermarks).
   - Do **not** pre-desaturate or darken it permanently unless desired — [`FavouritesSceneBackdrop`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt) automatically applies `BW_DARK_SCENE_FILTER` (0 saturation), `0.42f` image alpha, and the vertical edge scrim at runtime, and decodes it in `RGB_565` to save ~50% bitmap heap on Fire TV.

### 3.2 Swap a Show or Add a Character

Follow all **five** steps so visuals, resource loading, deep linking, and tests stay in sync:

1. **Add drawables:**
   - `tv/src/main/res/drawable-nodpi/fav_<key>.png` (transparent cutout).
   - `tv/src/main/res/drawable-nodpi/scene_fav_<key>.jpg` (`1280×720` scene).
   - `tv/src/main/res/drawable/art_<key>.png` (poster card thumbnail used by `TvContentCard` when selecting the character adds the show to **Recently Watched** via `launchContent`).
   - If using a new streaming app without an installed Android banner, add its fallback icon in `tv/src/main/res/drawable/app_<name>.png`.
2. **Register drawables in `FAV_RES_MAP` ([`MyFavouritesSection.kt`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt)):**
   > **Important:** [`rememberPreloadedFavouritesAssets`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt) and [`FavouritesGroupHeader`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt) look up resources through `FAV_RES_MAP` (unlike `TvComponents.getDrawableResId`, which uses `Resources.getIdentifier`). **If you forget to add new `fav_*`, `scene_fav_*`, or `app_*` keys to `FAV_RES_MAP`, the cutout or scene will not appear.**
3. **Add/verify the deep link (`HotstarDeepLinks` or direct URI):**
   - For JioHotstar shows, add `<SHOW>_CONTENT_ID` in [`HotstarDeepLinks.kt`](../core/src/main/kotlin/com/firelite/core/data/HotstarDeepLinks.kt) and pass `HotstarDeepLinks.showDetailsUri("<slug>", HotstarDeepLinks.<SHOW>_CONTENT_ID)` as `contentUrl`.
   - For another app (e.g., Netflix, Prime Video, YouTube, SunNXT, ZEE5), verify its scheme/host is handled in `HotstarDeepLinks.packageForUrl()` and set `streamingApp`, `streamingAppResName`, `deepLinkPackage`, and `contentUrl` on `FavouriteCharacterSpec`. (For JioHotstar show slug/ID verification steps, see [JioHotstar Deep Linking & Playback Guide](JIOHOTSTAR_DEEP_LINKING_AND_PLAYBACK.md).)
4. **Update [`buildDefaultFavouritesLineup()`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt):**
   - Set `row = FavouriteRow.BACK` or `FavouriteRow.FRONT`, consecutive `rowOrder` (`0, 1, ...` left-to-right), `xCenterFraction` spaced cleanly across `0.0..1.0`, `glowColor` / `glowColorHex`, and `touchType`.
5. **Update unit tests ([`MainScreenViewModelTest.kt`](../tv/src/test/kotlin/com/example/firelite/ui/main/MainScreenViewModelTest.kt)):**
   - `myFavourites_lineupRowsZOrderAndDeepLinksAreValid` asserts exact item count (`5`), `back`/`front` ID lists, `"JioHotstar"` package constraints, and D-pad neighbor IDs (`doraemonNav`, `siragadikkaNav`). Update those assertions to match any lineup change.

### 3.3 Customize One-Shot Extra Layers & Signature Animations

Every character touch is **strictly one-shot (plays once per selection, never loops)**. Three places in [`MyFavouritesSection.kt`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt) control each touch:

1. **`CharacterTouchType` enum** — defines the identifier for each character behavior.
2. **`LaunchedEffect(isSelected)` inside `FavouriteCharacterLayer`** — snaps `touchTranslateY`, `touchRotation`, `touchScaleBoost`, and `extraLayerProgress` back to `0f`, increments `selectionTriggerCount`, re-seeds `comicSeed`, and runs a finite `coroutineScope { launch { ... } }` sequence only while `isSelected` becomes `true`.
3. **Canvas overlays:**
   - `drawCharacterExtraLayer` draws **behind** the character cutout `Image` (only while `isSelected`).
   - `drawCharacterForegroundAccent` draws **over** the character cutout `Image` (currently enabled when `touchType` is `DORAEMON_TAKECOPTER`, `SHINCHAN_COMIC_WIGGLE`, or `COURT_GAVEL_SLAM`).

Current touches:

| `CharacterTouchType` | Body Motion (`Animatable`) | Behind (`drawCharacterExtraLayer`) | In Front (`drawCharacterForegroundAccent`) |
|---|---|---|---|
| `HEART_BEAT_PULSE` | Two-beat scale pulse (`+0.055f` / `+0.072f` -> `0f`) | Crimson ECG wave (`BlendMode.Screen`, `900ms`) | — |
| `DORAEMON_TAKECOPTER` | Levitation (`translationY -28f` -> `-8f`) | Cyan 4D pocket radial aura (`BlendMode.Screen`) | Spinning Take-copter arc (`540°`, `900ms`) |
| `COURT_GAVEL_SLAM` | Vertical slam (`+18f` -> bouncy spring `0f`) | Gold shockwave ring + Justice Scales beam (`Darken` + `Screen`, `480ms`) | Verdict medallion ring near shoulder (`Darken` + stroke) |
| `SIRAGADIKKA_SHIMMER` | Gentle tilt (`-3.0°` -> `+2.4°` -> `0°`) | 6 marigold sunbeam rays (`BlendMode.Screen`, `700ms`) | — |
| `SHINCHAN_COMIC_WIGGLE` | Hop (`-20f` -> `0f`) + wiggle (`±5.5°` -> `0°`) | Red pop circle (`BlendMode.Screen`) | 7 randomized comic burst lines around head (`comicSeed`, `720ms`) |

### 3.4 Adjust Glow, Header, Row Overlap (`zIndex`), and Bottom Leg Fade

All visual constants sit inside [`MyFavouritesSection`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt) and [`FavouritesGroupHeader`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt):

- **Z-order overlap (`resolveCharacterZIndex`):**
  - `BACK` unselected = `1f`, `BACK` selected = `2f` (grows in front of unselected back peers, still behind front row).
  - `FRONT` unselected = `10f`, `FRONT` selected = `20f` (steps in front of everyone when selected).
- **Selected scale & B&W transition (`FavouriteCharacterLayer`):**
  - `saturation` transitions `0f` <-> `1f` (`tween(200)`).
  - `focusScale` transitions `1.0f` <-> `1.09f` (`spring(dampingRatio = 0.86f, stiffness = 460f)`).
- **Behind-group colored spotlight:**
  - `Canvas(zIndex = 0f)` draws a radial gradient centered horizontally at `glowCenterFraction` (spring-animated toward `selectedSpec.xCenterFraction`), vertically at `size.height * 0.48f`, radius `size.width * 0.48f`, peak alpha `0.70f * glowAlpha` (`0.94f`).
- **Transparent leg fade (`BlendMode.DstIn`):**
  - The character wrapper `Box` uses `CompositingStrategy.Offscreen` + `.drawWithContent` drawing a `BlendMode.DstIn` vertical gradient:
    - `0.0f..0.70f`: `Color.Black` (100% opaque character upper body)
    - `0.86f`: `Color(0x66000000)` (~40% alpha feather)
    - `0.94f..1.0f`: `Color.Transparent` (0% alpha melt)
  - **Do not replace this with a solid dark `Box` overlay at the bottom of the stage** — an opaque overlay box paints a visible black bar across the background scene/wallpaper.
- **Header & Streaming App Logo (`FavouritesGroupHeader`):**
  - Row height is `64.dp` (accommodates title + subtitle cleanly without vertical clipping).
  - Streaming badge dynamically sizes width (`(30f * (w/h)).coerceIn(30f, 64f).dp` × `30.dp`) with `ContentScale.Fit` so wide Leanback TV banners (e.g., JioHotstar) are never cropped.

---

## 4. Critical Architecture Invariants & TV Pitfalls (Do Not Regress)

Several subtle Fire TV (`AFTSSS`, Android 9 / ~900 MB RAM) and Compose TV focus issues were fixed while building this section. Preserve these patterns when refactoring:

### 4.1 Two-Stage Off-Main-Thread Decoding (Cold-Start ANR Prevention)
[`rememberPreloadedFavouritesAssets`](../tv/src/main/kotlin/com/firelite/tv/ui/MyFavouritesSection.kt) decodes bitmaps on `Dispatchers.IO`:
1. **Stage 1:** `bg_cinematic_mountain` (`RGB_565`) + `fav_*` cutouts (`ARGB_8888`, yielding between bitmaps).
2. `delay(120)` so initial Activity draw and frame-0 D-pad focus complete cleanly.
3. **Stage 2:** `scene_fav_*` 16:9 backdrops in `RGB_565` (~50% less RAM than `ARGB_8888`).

Avoid calling synchronous `painterResource(...)` on large full-bleed wallpapers or scenes inside `FavouritesSceneBackdrop`.

### 4.2 D-Pad Row Navigation (`DPAD_UP` / `DPAD_DOWN` / `DPAD_LEFT` / `DPAD_RIGHT`)
- **Row-locked horizontal movement + nearest-neighbor vertical hop (`computeFavouritesNeighbors`):**
  - `Left`/`Right` return `FocusRequester.Cancel` at the ends of a row so D-pad presses never wrap or jump rows unexpectedly.
  - `FRONT` row `up` uses `lastBackFocusedId == "doraemon"` or `neighbors.upId` (nearest `xCenterFraction` in `BACK`).
  - `BACK` row `up` invokes `upFocusRequesterProvider()`, wired in [`FireLiteTvScreen`](../tv/src/main/kotlin/com/firelite/tv/ui/FireLiteTvScreen.kt) as `{ activeRecentRequester() ?: activeAppRequester() }`.
- **Single focus node per character (`FavouriteCharacterLayer`):**
  - Attach `focusModifier` alongside **only** `.clickable(interactionSource = interactionSource, ...)`. Do **not** add an extra `.focusable(interactionSource)` on top — duplicate focusable nodes can intercept D-pad directions and drop `up` navigation.
- **Non-lazy `APPLICATIONS` and `RECENTLY WATCHED` rows (`FireLiteTvScreen`):**
  - Those two horizontal shelves intentionally use `Row(Modifier.horizontalScroll(...))` + persistent ID-keyed `HashMap<String, FocusRequester>` (`appRequesters`, `recentRequesters`) rather than `LazyRow`. With `LazyRow`, scrolling off index `0` detaches offscreen `FocusRequester` nodes and breaks `DPAD_UP` return from **My Favourites**.
- **Smooth vertical row alignment without horizontal bobbing:**
  - `FireLiteTvScreen` wraps the outer `verticalScroll` column in `ZeroVerticalBringIntoViewSpec` (preventing Left/Right key presses from jigging vertical scroll), scrolls programmatically per `focusedRow` using `onGloballyPositioned` section Y positions (`recentSectionTopYPx`, `myFavouritesTopYPx`), and keeps a trailing `Spacer(140.dp)` so scrolling to **My Favourites** cleanly clears **Recently Watched** above the viewport.

---

## 5. Build, Test & Live Fire TV Verification Checklist

After any edit to **My Favourites** assets, lineup specs, animations, or focus routing:

1. **Run unit tests and assemble debug APK:**
   ```bash
   ./gradlew :tv:testDebugUnitTest :tv:assembleDebug --rerun-tasks --console=plain
   ```
2. **Install & launch on Fire TV:**
   ```bash
   adb -s <TV_IP>:5555 install -r tv/build/outputs/apk/debug/tv-debug.apk
   adb -s <TV_IP>:5555 shell am start -n com.firelite.tv/.MainActivity
   ```
   *(Wait ~12–15s on debug cold boot for ART startup and initial `APPS` focus to settle before sending synthetic ADB keyevents.)*
3. **Smoke-test all 4 directions + `OK` deep links:**
   - Move `DOWN` through `APPLICATIONS` -> `RECENTLY WATCHED` -> `MY FAVOURITES` Back Row -> Front Row.
   - Confirm `LEFT`/`RIGHT` stays in the active row, only the selected character turns to color + glows + fades in their darkened B&W scene, and there is no black bar under legs.
   - Press `UP` from Front Row -> Back Row -> `RECENTLY WATCHED` -> `APPLICATIONS` (including after scrolling horizontally past index `0`) -> `TvTopBar` Search/Settings, confirming the default mountain wallpaper returns when leaving `MY FAVOURITES`.
   - Press `OK` (`KEYCODE_DPAD_CENTER`) on every character and confirm it opens the right show in its target app (e.g., `in.startv.hotstar`).

