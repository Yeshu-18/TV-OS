# JioHotstar Deep Linking & Direct Playback Findings (Fire TV)

> **Device, App & Verification Context**
> - **Device**: Amazon Fire TV Stick 3rd Generation (`AFTSSS`, `sheldonp`)
> - **Fire OS Version**: 7.7.1.7 (Android 9 Pie, API 28)
> - **Target Package**: `in.startv.hotstar` (JioHotstar)
> - **Installed TV App Version**: `26.10.26.1` (`versionCode=4727`, `lastUpdateTime=2026-11-10 09:45:06`)
> - **Statically Inspected APK**: `26.10.12.0` (`versionCode=0x126b`)
> - **Verification Scope (November 11, 2026)**:
>   - **Live-tested on Fire TV (`192.168.0.46:5555`)**: *I'm Game* (`1271720887` & erroneous `1271354531`), *Heart Beat* (`1260168231`), *The Court* (`1271672560`, series + episode `scammed/1271708931`), *Shinchan* (`1260020929`), *Doraemon* (`1260021004`), *Bittu Bahanebaaz: Ek Ka Teen* (`1271708139`), *Ninja Hattori Classic* (`1971003458`), *Motu Patlu* (`1971003164`), *Siragadikka Aasai* (`1260129356`), *Ayyanar Thunai* (`1271388570`), plus end-to-end launches from `com.firelite.tv/.MainActivity` under JioHotstar's `Nanda kumar` profile.
>   - **Pending live Fire TV checks**: Episode-level `hotstar://in/shows/<showSlug>/<showId>/<episodeSlug>/<episodeId>/watch` URIs for *Heart Beat* (`1271668632`), *Shinchan* (`1260021738`), and *Doraemon* (`1260022097`), plus negative missing-`/in/` test on `heart-beat`.

---

## 1. Executive Summary

To launch JioHotstar content directly into video playback (instead of falling back to the Home screen or opening a Details page) on Fire TV, external callers must use the `hotstar://` scheme with the `/in/` region prefix and `/watch` suffix:

- **Details page (`DetailPageFragment`)**:
  `hotstar://in/<type>/<slug>/<contentId>`
- **Direct playback / Series resume (`WatchFragment`)**:
  `hotstar://in/<type>/<slug>/<contentId>/watch`
- **Episode-specific direct playback (canonical Hotstar pattern)**:
  `hotstar://in/shows/<showSlug>/<showId>/<episodeSlug>/<episodeId>/watch`

For **movies** (*I'm Game*), `hotstar://in/movies/<slug>/<movieId>/watch` starts playback on both warm and cold launches.
For **series** (*Heart Beat*), `hotstar://in/shows/<slug>/<showId>/watch` automatically resumes **both** the last-watched episode and saved playback position when JioHotstar is already running/warm (and falls back to the primary season CTA episode when unwatched), while cold-start `/watch` after `am force-stop` showed intermittent timing sensitivity around `SelectProfileFragment`.

---

## 2. Verified Paths Across FireLite Titles

| Title | Category | Valid Content ID | Canonical Web / Sitemap Path (`hotstar.com`) | Recommended Fire TV Details URI | Recommended Fire TV Direct Watch / Resume URI | Status |
|---|---|---|---|---|---|---|
| ***I'm Game*** | Movie | **`1271720887`** *(previously `1271354531`)* | `https://www.hotstar.com/in/movies/i-m-game/1271720887` & `.../watch` (`MOVIE-1`, `MOVIE_WATCH_PAGES-1`) | `hotstar://in/movies/im-game/1271720887` *(or `i-m-game`)* | `hotstar://in/movies/im-game/1271720887/watch` *(live-tested; canonical slug is `i-m-game`)* | **Live Fire TV Verified** (`WatchFragment`; warm, cold, & FireLite `CONTINUE WATCHING` playback verified on `Nanda kumar`; hit `ERR_PB_1426` only when `Kids` profile was active) |
| ***Heart Beat*** | Series | **`1260168231`** *(sitemap also lists separate regional/season IDs `1271664440`, `1271669884`)* | `https://www.hotstar.com/in/shows/heart-beat/1260168231` | `hotstar://in/shows/heart-beat/1260168231` | `hotstar://in/shows/heart-beat/1260168231/watch` | **Live Fire TV Verified** (warm details, warm play & `S3 E2` auto-resume verified on `Nanda kumar`; cold start hit `LandingFragment` / `ERR_PB_1410`; hit `ERR_PB_1426` only when `Kids` profile was active) |
| ***The Court*** | Series | **`1271672560`** *(distinct from Korean drama `may-it-please-the-court`: `1260113199` / `1260112840`)* | `https://www.hotstar.com/in/shows/the-court/1271672560` | `hotstar://in/shows/the-court/1271672560` | `hotstar://in/shows/the-court/1271672560/watch` | **Live Fire TV Verified** — Details (`2026`, `U/A 16+`, `1 Season`), series `/watch` (`S1 E1 Black Coats`, including from FireLite `CONTINUE WATCHING`), and episode-level `.../scammed/1271708931/watch` (`S1 E29 Scammed`) all played on `Nanda kumar`; hit `ERR_PB_1426` only on `Kids` profile |
| ***Shinchan*** | Kids / Series | **`1260020929`** | `https://www.hotstar.com/in/shows/shin-chan/1260020929` *(canonical slug is hyphenated `shin-chan`)* | `hotstar://in/shows/shin-chan/1260020929` | `hotstar://in/shows/shin-chan/1260020929/watch` | **Live Fire TV Verified** — Details (`S15 E1`), direct watch (`shin-chan/.../watch`), and alternate slug (`shinchan/.../watch`) all started playback |
| ***Doraemon*** | Kids / Series | **`1260021004`** | `https://www.hotstar.com/in/shows/doraemon/1260021004` | `hotstar://in/shows/doraemon/1260021004` | `hotstar://in/shows/doraemon/1260021004/watch` | **Live Fire TV Verified** — Details (`U/A 7+`) and direct watch (`/watch`) verified |
| ***Bittu Bahanebaaz: Ek Ka Teen*** | Kids / Movie | **`1271708139`** | — | `hotstar://in/movies/bittu-bahanebaaz-ek-ka-teen/1271708139` | `hotstar://in/movies/bittu-bahanebaaz-ek-ka-teen/1271708139/watch` | **Live Fire TV Verified** — Direct `/watch` started playback (`WatchFragment` + audio) |
| ***Ninja Hattori Classic*** | Kids / Series | **`1971003458`** *(previously stale `1260000045`)* | — | `hotstar://in/shows/ninja-hattori-classic/1971003458` | `hotstar://in/shows/ninja-hattori-classic/1971003458/watch` | **Live Fire TV Verified** — Details and direct `/watch` (via FireLite Continue Watching) started playback |
| ***Motu Patlu*** | Kids / Series | **`1971003164`** | — | `hotstar://in/shows/motu-patlu/1971003164` | `hotstar://in/shows/motu-patlu/1971003164/watch` | **Details Live Verified** (`2026`, `U`, `16 Seasons`) |
| ***Siragadikka Aasai*** | Series | **`1260129356`** | — | `hotstar://in/shows/siragadikka-aasai/1260129356` | `hotstar://in/shows/siragadikka-aasai/1260129356/watch` | **Details Live Verified** (`Free For You`, `U/A 13+`, `Watch Latest Episode`) |
| ***Ayyanar Thunai*** | Series | **`1271388570`** | — | `hotstar://in/shows/ayyanar-thunai/1271388570` | `hotstar://in/shows/ayyanar-thunai/1271388570/watch` | **Details Live Verified** (`Free For You`, `U/A 13+`, `Watch Latest Episode`) |

### Canonical Episode-Level Watch Paths (`new-sitemap-EPISODE-1..9.xml`)

Every series episode URL published in Hotstar's production sitemaps follows `/in/shows/<showSlug>/<showId>/<episodeSlug>/<episodeId>/watch`, converting to `hotstar://in/shows/<showSlug>/<showId>/<episodeSlug>/<episodeId>/watch`:

- **Heart Beat (`1260168231`)** — 260 matched URLs across `EPISODE-1..9.xml`:
  - S3 E2 (`Starting Over`, verified playing on Fire TV): `https://www.hotstar.com/in/shows/heart-beat/1260168231/starting-over/1271668632/watch`
  - Example: `https://www.hotstar.com/in/shows/heart-beat/1260168231/the-failed-surgery/1271463813/watch`
  - Deep link form (episode ID from sitemap; series-level playback verified on Fire TV -> `S3 E2 Starting Over`): `hotstar://in/shows/heart-beat/1260168231/starting-over/1271668632/watch`
- **The Court (`1271672560`)** — 40 matched URLs across `EPISODE-1..9.xml` (including language/variant IDs such as `collateral/1271708312` and `collateral/1271708932`):
  - Examples:
    - `https://www.hotstar.com/in/shows/the-court/1271672560/the-choice/1271687155/watch`
    - `https://www.hotstar.com/in/shows/the-court/1271672560/the-face-off/1271708936/watch`
    - `https://www.hotstar.com/in/shows/the-court/1271672560/scammed/1271708931/watch` (live-verified via ADB on Fire TV under `Nanda kumar` -> plays `S1 E29 Scammed`)
    - `https://www.hotstar.com/in/shows/the-court/1271672560/d-day/1271708939/watch`
  - Live-verified episode deep link: `hotstar://in/shows/the-court/1271672560/scammed/1271708931/watch` (and series deep link `hotstar://in/shows/the-court/1271672560/watch` -> `S1 E1 Black Coats`)
- **Shinchan (`1260020929`, canonical slug `shin-chan`)**:
  - Examples:
    - `https://www.hotstar.com/in/shows/shin-chan/1260020929/family-trip-to-hawaii/1260021738/watch`
    - `https://www.hotstar.com/in/shows/shin-chan/1260020929/pure-heart-shaved-ice-catch-legendary-cicada/1260021746/watch`
  - Deep link form (pending ADB): `hotstar://in/shows/shin-chan/1260020929/family-trip-to-hawaii/1260021738/watch`
- **Doraemon (`1260021004`, canonical slug `doraemon`)**:
  - Examples:
    - `https://www.hotstar.com/in/shows/doraemon/1260021004/love-at-first-sight-shizuka-s-space-outdoor/1260022097/watch`
    - `https://www.hotstar.com/in/shows/doraemon/1260021004/river-swimming-with-sake-auto-revenge-radar/1260022024/watch`
  - Deep link form (pending ADB): `hotstar://in/shows/doraemon/1260021004/love-at-first-sight-shizuka-s-space-outdoor/1260022097/watch`

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "hotstar://in/movies/im-game/1271720887/watch" \
  -p in.startv.hotstar
```

---

## 3. Root Causes Identified

### 3.1 Incorrect Content ID for *I'm Game* (`1271354531` vs `1271720887`)
- FireLite previously stored `contentId = "1271354531"` and `contentUrl = "https://www.hotstar.com/in/movies/im-game/1271354531"` in `core/src/main/kotlin/com/firelite/core/data/FireLiteRepository.kt` and `tv/src/main/kotlin/com/firelite/tv/util/TvWatchNextManager.kt` (now migrated to `1271720887`).
- Every live test on Fire TV using `1271354531` (across `hotstar://`, `hotstar://in/`, `https://`, with or without `/watch`, and bare `hotstar://1271354531`) caused JioHotstar to fall back to Home (`LandingFragment` with `firstPageId=/v2/pages/home`).
- Resolving *I'm Game* on `hotstar.com` (and verifying via `new-sitemap-MOVIE-1.xml` / `new-sitemap-MOVIE_WATCH_PAGES-1.xml`) yielded `1271720887` (`https://www.hotstar.com/in/movies/i-m-game/1271720887/watch`), and testing **`1271720887`** with `hotstar://in/movies/im-game/1271720887/watch` opened and played *I'm Game* immediately.
- Note on slug resilience: on live Fire TV, both `hotstar://in/movies/im-game/1271720887/watch` (non-canonical unhyphenated slug for `i-m-game`) and `hotstar://in/shows/shinchan/1260020929/watch` (non-canonical unhyphenated slug for `shin-chan`) resolved identically to their canonical sitemap counterparts, confirming that backend routing prioritizes `/in/` + trailing numeric `contentId` over exact slug spelling for both movies and shows.

### 3.2 `www.hotstar.com` SPA HTTP 200 Trap When Validating IDs
- Plain `curl` / HTTP status checks against `https://www.hotstar.com/in/...` are **unreliable** for verifying JioHotstar IDs: `www.hotstar.com` returned `HTTP/2 200` and the exact same `425,609`-byte SPA HTML shell for both valid (`1271720887`) and invalid (`1271354531`) movie URLs.
- Always validate IDs and slugs against `https://www.hotstar.com/in/new-sitemap.xml` (`SHOWS-1`, `MOVIE-1`, `MOVIE_WATCH_PAGES-1`, `EPISODE-1..9`) and/or live Fire TV ADB launches.

### 3.3 Why `https://www.hotstar.com/...` Fails on Fire TV (`MainActivity.j` Bug)
Inspection of the decompiled JioHotstar APK (`in.startv.hotstar` `26.10.12.0`) shows why `https://` deep links fail on Fire TV even for known-valid content IDs:

1. `com.hotstar.MainActivity` (`launchMode="singleInstance"`) routes both `onCreate` and `onNewIntent` to `MainActivity.l(Intent, boolean)`.
2. When `action` is `android.intent.action.VIEW` or `MAIN` with non-null `intent.getData()`, `MainActivity.l()` checks `p144gc.d.e(this)` — which checks `PackageManager.hasSystemFeature("amazon.hardware.fire_tv")`.
3. On Fire TV (`true`), it calls `MainActivity.j(String.valueOf(intent.getData()), z10)`:
   ```java
   if (!kotlin.text.b.s(str, "hotstar://", false)) {
       str = "hotstar://".concat(str);
   }
   k().B0(str, z10, false);
   ```
   - `kotlin.text.b.s` checks substring containment (`contains`), not prefix, and `https://www.hotstar.com/...` does not contain `"hotstar://"`.
   - `j()` therefore prepends `"hotstar://"`, mutating `"https://www.hotstar.com/in/..."` into `"hotstar://https://www.hotstar.com/in/..."`.
4. `MainViewModel.B0()` then checks `Nm.h.r(string, "hotstar://", false)` (`startsWith("hotstar://")`) and replaces `"hotstar://"` with `"/"` — turning the URL into `"/https://www.hotstar.com/in/..."` before URL-encoding it into the `/v2/start` request (`BffStartRequest`).
5. JioHotstar's `/v2/start` backend cannot resolve `"/https://www.hotstar.com/..."` and returns `/v2/pages/home` (`LandingFragment`).

*(Live-verified with valid `Heart Beat` ID `1260168231`: `https://www.hotstar.com/in/shows/heart-beat/1260168231` landed on `LandingFragment` on warm & cold starts, whereas `hotstar://in/shows/heart-beat/1260168231` opened `DetailPageFragment` and `hotstar://in/shows/heart-beat/1260168231/watch` opened `WatchFragment` and started playback.)*

### 3.4 Why `/in/` Is Required With `hotstar://`
- Because `MainViewModel.B0()` replaces the literal `"hotstar://"` prefix with `"/"`:
  - `hotstar://movies/im-game/...` becomes `"/movies/im-game/..."` (missing region segment).
  - `hotstar://in/movies/im-game/...` becomes `"/in/movies/im-game/..."` (matches JioHotstar's India routing path on `/v2/start`).

### 3.5 Profile Gate (`SelectProfileFragment`) & Cold-Start Behavior
- On a cold start (`in.startv.hotstar` not running), JioHotstar first displays `OnboardingFragment` -> `ProfileContainerFragment` -> `SelectProfileFragment` (`"Who's watching?"`, with profile `Nanda kumar` `focused=true selected=true` alongside `Kids` and `Add`) before resolving/navigating the deep link.
- **Profile Maturity Gate (`Kids` vs `Nanda kumar`, `ERR_PB_1426`)**: When the active profile inside JioHotstar is `Kids`, deep linking to `U/A 16+` titles such as *The Court* or *Heart Beat* fails inside the player with **`ERR_PB_1426`** due to maturity restrictions. Switching the active profile to **`Nanda kumar`** in `My Space` (or `"Who's watching?"`) clears `ERR_PB_1426` and restores direct playback across shows and movies (*The Court* `S1 E1 Black Coats` & `S1 E29 Scammed`, *Heart Beat* `S3 E2 Starting Over`, and *I'm Game*).
- **Movies (*I'm Game*, `1271720887`)**: After `SelectProfileFragment`, Hotstar preserved the pending deep link, navigated directly to `WatchFragment` (`watchPageId=/v2/pages/watch`, `isDeeplink=true`), initialized `ExoPlayerImpl`, and started `AudioTrack` (`player piid:87`, `CONTENT_TYPE_MOVIE`).
- **Series (*Heart Beat*, `1260168231`)**:
  - **Warm start (`LandingFragment` active)**: Reliable for both details (`DetailPageFragment`) and watch/resume (`WatchFragment`).
  - **Cold start (`am force-stop`, no input)** was inconsistent across two runs:
    - Run 1: showed `SelectProfileFragment{740e78e}` at `t=14s`, then fell back to `LandingFragment{c482135}` (`firstPageId=/v2/pages/home`) by `t=39s`.
    - Run 2: `SelectProfileFragment` auto-transitioned to `WatchFragment{aaf0f1a}` (`watchPageId=/v2/pages/watch`, `isDeeplink=true`) by `t=20s`, where `ErrorFragment` displayed `'Content not available'` / `'The requested video is not available. Please select another content.'` (`ERR_PB_1410   26.10.26.1`). Immediate warm retry of the exact same URI succeeded (`S3 E2 Starting Over`).
- **Asleep Display Edge Case**: When the Fire TV was asleep (`mWakefulness=Asleep`, `Display Power: state=OFF`) with `WatchFragment` paused, launching a new `hotstar://in/shows/...` intent did not switch fragments until the TV was woken (`KEYCODE_WAKEUP`).

### 3.6 Series Playback & Auto-Resume Findings (*Heart Beat* Live Test)
- **Details URI (`hotstar://in/shows/heart-beat/1260168231`)**:
  - Opened `DetailPageFragment{9d14eee}` (`detailPageId=/v2/pages/detail?content_id=1260168231&filters=content_type%3Dshow_clips`, `isDeeplink=true`) + `EpisodeContentFragment{f46c0c3}`.
  - UI dump showed `3 Seasons`, `U/A 16+`, `#2 in Tamil Today`, `New Episodes Thursdays`, CTA `Watch Latest Season` / `S3 E1`, and Season 3 (`60 Eps`: S3 E1 `Home No More` `31m`, S3 E2 `Starting Over` `19m`, S3 E3 `No Room for Fear` `27m`, S3 E4 `Old Scars` `19m`).
  - Note: `DetailPageFragment` enforces `FLAG_SECURE` (`screencap` produces a blank/black image; `uiautomator dump` still reads the UI hierarchy).
- **Watch / Resume URI (`hotstar://in/shows/heart-beat/1260168231/watch`)**:
  - **Initial launch (no prior watch progress)**: opened `WatchFragment{4bbcc78}` (`watchPageId=/v2/pages/watch`, `isDeeplink=true`) and automatically played `S3 E1 Home No More` (`metadata:size=6, description=S3 E1 Home No More`, `player piid:159 state:started`).
  - **Continue Watching auto-resume**: after advancing into `S3 E2 Starting Over` (`position=47637` -> `position=93321`), backing out to `LandingFragment`, and re-firing `hotstar://in/shows/heart-beat/1260168231/watch`, JioHotstar resumed **`S3 E2 Starting Over`** directly (observed `PlaybackState {state=3, position=724, ...}` during initial load at `t=8s`, then auto-sought to `position=110016` / `~110s`).
  - This confirms `hotstar://in/shows/<slug>/<showId>/watch` handles **series Continue Watching resume** (both episode selection and saved timestamp) automatically when the app is warm.

### 3.7 Internal `android-app://` URIs Are Not Externally Supported
- `Pe.b.b()` uses internal `android-app://com.hotstar/...` templates with Jetpack Navigation only *after* `/v2/start` returns a `BffPageNavigationAction`.
- `android-app` is not declared in `in.startv.hotstar`'s `<intent-filter>` manifest entries, and `MainActivity.j()` would prepend `hotstar://` to it anyway. External callers must not use `android-app://`.

---

## 4. Verification Matrix

### 4.1 Live-Verified on Fire TV (`192.168.0.46:5555`)

| Tested URI | Launch State | Observed Result |
|---|---|---|
| `hotstar://in/movies/im-game/1271720887/watch` | Warm (`MainActivity` brought to front) | **Success** — opened `WatchFragment` (`watchPageId=/v2/pages/watch`, `isDeeplink=true`); `AudioTrack` (`player piid:71`, `CONTENT_TYPE_MOVIE`) started |
| `hotstar://in/movies/im-game/1271720887/watch` | Cold start (`in.startv.hotstar` stopped) | **Success** — `OnboardingFragment` -> `SelectProfileFragment`, then `WatchFragment` (`isDeeplink=true`) started `ExoPlayerImpl` + `AudioTrack` (`player piid:87`) |
| `hotstar://in/shows/heart-beat/1260168231` | Warm (`LandingFragment` active) | **Success (Details)** — opened `DetailPageFragment{9d14eee}` + `EpisodeContentFragment{f46c0c3}` (`3 Seasons`, CTA `S3 E1`) |
| `hotstar://in/shows/heart-beat/1260168231/watch` | Warm, no prior progress | **Success (Default Play)** — opened `WatchFragment{4bbcc78}` (`isDeeplink=true`), played `S3 E1 Home No More` (`player piid:159`) *(earlier warm test also started `player piid:647`)* |
| `hotstar://in/shows/heart-beat/1260168231/watch` | Warm, after watching `S3 E2 Starting Over` to `~93s` | **Success (Auto-Resume)** — opened `WatchFragment` on `S3 E2 Starting Over` (`position=724` at `t=8s`, then auto-sought to `position=110016`) |
| `hotstar://in/shows/heart-beat/1260168231/watch` | Cold start (`am force-stop`, no input) | **Intermittent** — Run 1 went `SelectProfileFragment{740e78e}` -> `LandingFragment{c482135}`; Run 2 went `SelectProfileFragment` -> `WatchFragment{aaf0f1a}` with `ErrorFragment` (`ERR_PB_1410`); immediate warm retry played `S3 E2` |
| `https://www.hotstar.com/in/shows/heart-beat/1260168231` | Warm & cold (`am force-stop`) | **Home fallback** — landed on `LandingFragment` (`firstPageId=/v2/pages/home`) |
| `hotstar://movies/im-game/1271354531/watch` | Warm & cold (`am force-stop`) | **Home fallback** — `LandingFragment` (both missing `/in/` and invalid ID `1271354531`) |
| `hotstar://in/movies/im-game/1271354531(/watch)` / `hotstar://1271354531` / `https://.../1271354531(/watch)` | Warm | **Home fallback** — `LandingFragment` (invalid ID `1271354531`) |
| `hotstar://in/shows/the-court/1271672560` | Warm | **Success (Details)** — opened *The Court* (`2026`, `U/A 16+`, `1 Season`, `5 Languages`, `New Episodes Fridays`, CTA `Watch Latest Season` / `S1 E1`) |
| `hotstar://in/shows/the-court/1271672560/watch` *(and clicking `Watch Latest Season` CTA)* | Warm (`Kids` profile active) | **Routed to `WatchFragment` (`ERR_PB_1426`)** — `Kids` profile (`U/A 16+` blocked) showed `'Inappropriate content — This content has a mature rating and is unavailable in the current profile'` (`ERR_PB_1426`), identical to `heart-beat/1260168231/watch` and `im-game/1271720887/watch` while `Kids` was active |
| `hotstar://in/shows/the-court/1271672560/watch` | Warm (`Nanda kumar` profile active & FireLite `CONTINUE WATCHING`) | **Success (Direct Play)** — opened `WatchFragment` and started playback of `S1 E1 Black Coats` (`player piid:55` / `piid:111 state:started`, `PlaybackState state=3`, `speed=1.0`, `error=null`) |
| `hotstar://in/shows/the-court/1271672560/scammed/1271708931/watch` | Warm (`Nanda kumar` profile active) | **Success (Episode Direct Play)** — opened `WatchFragment` and played `S1 E29 Scammed` (`player piid:63 state:started`, `PlaybackState state=3`, `error=null`) |
| `hotstar://in/shows/shin-chan/1260020929` | Warm | **Success (Details)** — opened *Shinchan* (`#1 in Tamil Today`, `2019`, `U/A 7+`, `1 Season`, CTA `Watch Latest Season` / `S15 E1`) |
| `hotstar://in/shows/shin-chan/1260020929/watch` & `hotstar://in/shows/shinchan/1260020929/watch` | Warm | **Success (Direct Play + Slug Resilience)** — both hyphenated (`shin-chan`) and unhyphenated (`shinchan`) slugs entered `WatchFragment` (`WatchOverlayFragment`, `player piid:431` / `piid:495 state:started`) |
| `hotstar://in/shows/doraemon/1260021004` | Warm | **Success (Details)** — opened *Doraemon* (`#3 in Tamil Today`, `IMDb 8.4`, `2020`, `U/A 7+`, `1 Season`, `3 Languages`) |
| `hotstar://in/shows/doraemon/1260021004/watch` | Warm | **Success (Direct Play)** — entered `WatchFragment` (`WatchOverlayFragment`, `player piid:455 state:started`) |
| `hotstar://in/movies/bittu-bahanebaaz-ek-ka-teen/1271708139/watch` | Warm | **Success (Direct Play)** — entered `WatchFragment` (`player piid:463` / `piid:503 state:started`, no error overlay) |
| `hotstar://in/shows/ninja-hattori-classic/1971003458` | Warm (ADB + FireLite `PINNED SHOWS`) | **Success (Details)** — opened *Ninja Hattori* (`2024`, `U/A 7+`, `2 Seasons`, `9 Languages`) |
| `hotstar://in/shows/ninja-hattori-classic/1971003458/watch` | Warm (FireLite `CONTINUE WATCHING`) | **Success (Direct Play)** — entered `WatchFragment` and started playback (`player piid:551 state:started`) |
| `hotstar://in/shows/motu-patlu/1971003164` | Warm | **Success (Details)** — opened *Motu Patlu* (`2026`, `U`, `16 Seasons`, `7 Languages`) |
| `hotstar://in/shows/siragadikka-aasai/1260129356` | Warm | **Success (Details)** — opened *Siragadikka Aasai* (`Free For You`, `2026`, `U/A 13+`, `Watch Latest Episode`) |
| `hotstar://in/shows/ayyanar-thunai/1271388570` | Warm | **Success (Details)** — opened *Ayyanar Thunai* (`Free For You`, `2026`, `U/A 13+`, `Watch Latest Episode`) |

### 4.2 Pending Live Fire TV Checks

- **Episode-level & negative checks**: Direct episode-level `hotstar://in/shows/<showSlug>/<showId>/<episodeSlug>/<episodeId>/watch` URIs for *Heart Beat* (`1271668632`), *Shinchan* (`1260021738`), and *Doraemon* (`1260022097`), plus missing-`/in/` negative check (`hotstar://shows/heart-beat/1260168231/watch`).

### 4.3 FireLite On-Device Verification (TV + Phone Companion, Nov 11, 2026)

- **TV App (`com.firelite.tv`)**: Installed `tv-debug.apk` on `192.168.0.46:5555`. On first launch, `TvWatchNextManager` automatically migrated all 5 existing `continue_watching_items_v4` SharedPreferences entries from legacy `https://www.hotstar.com/...` / `1271354531` to verified `hotstar://in/.../watch` URIs (`1271720887`, `1271708139`, `1260168231`, `1271672560`, `1260021004`). Under `Nanda kumar`, end-to-end UI clicks launched *I'm Game* (`CONTINUE WATCHING` -> `WatchFragment` `player piid:95`), *The Court* (`CONTINUE WATCHING` -> `S1 E1 Black Coats` `player piid:111`), *Shinchan* and *Ninja Hattori* from `PINNED SHOWS` (promoting `/watch` entries into `CONTINUE WATCHING` via `recordWatch`), and *Ninja Hattori* from `CONTINUE WATCHING` directly into playback (`player piid:551`).
- **Companion App (`com.firelite.companion`)**: Installed `companion-debug.apk` on the connected phone (`adb-3C15BV002N500000-xsWD8B._adb-tls-connect._tcp`) and launched `com.firelite.companion/.MainActivity`. Note: `TvRemoteServer` on Fire TV listens on port `8765`, while `companion/src/main/kotlin/com/firelite/companion/MainActivity.kt` currently hardcodes `client.connect("127.0.0.1")` and `PairingDialog` does not yet accept a TV IP address.

---

## 5. FireLite Registry & Launcher Implementation (Updated & Built)

FireLite now uses verified `hotstar://in/...` Fire TV deep links and content IDs across `core` and `tv`:

- `core/src/main/kotlin/com/firelite/core/data/HotstarDeepLinks.kt`:
  - Shared constants, URL/media-item migration helpers (`normalizeUrl`, `normalizeMediaItem`), and scheme + domain package resolution (`packageForUrl`).
  - Migrates legacy `https://(www.)hotstar.com/...`, missing-`/in/` `hotstar://...` paths, stale *I'm Game* ID `1271354531` -> `1271720887`, stale *Ninja Hattori* ID `1260000045` -> `1971003458` (`ninja-hattori-classic`), and legacy *Bittu Bahanebaaz* search URL -> `1271708139` (`movies/bittu-bahanebaaz-ek-ka-teen/1271708139/watch`).
- `core/src/main/kotlin/com/firelite/core/data/FireLiteRepository.kt`:
  - Pinned Kids & Series JioHotstar items use Details URIs (`hotstar://in/movies/im-game/1271720887`, `hotstar://in/shows/heart-beat/1260168231`, `hotstar://in/shows/the-court/1271672560`, `hotstar://in/shows/shin-chan/1260020929`, `hotstar://in/shows/doraemon/1260021004`, `hotstar://in/shows/ninja-hattori-classic/1971003458`, `hotstar://in/shows/motu-patlu/1971003164`, `hotstar://in/shows/siragadikka-aasai/1260129356`, `hotstar://in/shows/ayyanar-thunai/1271388570`).
  - Updated `heart_beat` `durationOrEpisodes` to `"3 Seasons"`, and migrated persisted pins (`loadSavedPins`) via `HotstarDeepLinks.normalizeMediaItem`.
- `tv/src/main/kotlin/com/firelite/tv/util/TvWatchNextManager.kt`:
  - Seeded Continue Watching items (`getInitialContinueWatchingItems`) use direct `/watch` deep links (`cw_im_game` -> `hotstar://in/movies/im-game/1271720887/watch`, `cw_bittu_bahanebaaz` -> `hotstar://in/movies/bittu-bahanebaaz-ek-ka-teen/1271708139/watch`, `cw_heart_beat` -> `hotstar://in/shows/heart-beat/1260168231/watch`, `cw_the_court` -> `hotstar://in/shows/the-court/1271672560/watch`, `cw_doraemon` -> `hotstar://in/shows/doraemon/1260021004/watch`).
  - Migrates persisted `continue_watching_items_v4` entries and TV Watch Next provider (`content://android.media.tv/watch_next_program`) Hotstar URIs on load, updates `recordWatch` to promote Continue Watching copies to `/watch`, and delegates `packageForUrl` to `HotstarDeepLinks.packageForUrl` (supporting both `hotstar://` scheme and HTTPS hosts).
- `tv/src/main/kotlin/com/firelite/tv/ui/FireLiteTvScreen.kt` (`launchContent`):
  - Normalizes the outgoing `contentUrl` before launching `Intent.ACTION_VIEW` (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`), while updating in-memory and persisted Continue Watching from `TvWatchNextManager.recordWatch`.
