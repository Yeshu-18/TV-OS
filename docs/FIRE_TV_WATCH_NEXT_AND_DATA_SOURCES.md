# Fire TV Watch Next & Recently Played Data-Source Audit

**Device Under Test:** Amazon Fire TV Stick (`AFTSSS`, codename `sheldonp`), Fire OS `7.7.1.7` (Android release `9`)  
**ADB Target Observed:** `192.168.0.46:5555`  
**Scope:** Evaluating every OS-level and app-level signal on Fire OS for building an automatic `Continue Watching` / recently-played shelf in FireLite (`com.firelite.tv`), and documenting the resulting hybrid implementation in `TvWatchNextManager` and `HotstarDeepLinks`.

---

## 1. Executive Summary

Third-party Fire TV launchers **cannot** automatically read external streaming history from JioHotstar (`in.startv.hotstar`) or Amazon's private launcher integration on Fire OS `7.7.1.7` (Android `9`):

1. Android TV's public `watch_next_program` provider is empty (`No result found.`), and JioHotstar explicitly disables `watch_next` sync when running on Fire TV (`androidtv.home.watch_next = false`).
2. Amazon's private Watch Next / EPG provider (`content://amazon.media.tv/watch_next_program`) and JioHotstar's catalog/search provider (`content://in.startv.hotstar`) reject third-party queries with `SecurityException` (`Permission Denial`).
3. OS telemetry services (`NotificationManager`, `MediaSession`, `AudioFlinger`, and `ActivityRecents`) either discard content metadata after playback stops or expose only package/task and audio-track identifiers.

To track real user watch activity cleanly despite these Fire OS boundaries, FireLite implements a **10-item LRU `Recently Watched` cache** (`RecentlyWatchedLruCache`) in `TvWatchNextManager`: it queries `content://android.media.tv/watch_next_program` when available, merges genuine FireLite-recorded playback events persisted in `SharedPreferences` in strict MRU-to-LRU order (without injecting dummy items or pinned overrides), normalizes JioHotstar URIs via `HotstarDeepLinks`, and evicts the least-recently-watched entry once more than `10` distinct shows/movies are recorded.

---

## 2. Live OS & App Data-Source Audit Results

The following sources were queried directly on the live Fire TV device (`192.168.0.46:5555`):

| Data Source / Query | Observed Live Output / Behavior | Usable for External History? |
|---|---|---|
| `content://android.media.tv/watch_next_program` | `No result found.` | **No** (empty on Fire OS for tested OTT apps) |
| `content://amazon.media.tv/watch_next_program` | `java.lang.SecurityException: Permission Denial: opening provider com.amazon.providers.tv.FireTvProvider from (null) (pid=16576, uid=2000) requires com.amazon.providers.tv.permission.READ_EPG_DATA or com.amazon.providers.tv.permission.WRITE_EPG_DATA` | **No** (privileged/signature Amazon provider boundary) |
| `content://in.startv.hotstar` | `java.lang.SecurityException: Permission Denial: opening provider com.hotstar.globalsearch.SearchProvider from (null) (pid=16587, uid=2000) requires in.startv.hotstar.ACCESS_VIDEO_DATA or in.startv.hotstar.ACCESS_VIDEO_DATA` | **No** (signature-protected JioHotstar provider) |
| `dumpsys notification` | `com.netflix.ninja` defines `NotificationChannel{mId='recommendations'}` with daily `post_frequency` counts (`112`, `272`, etc.), but showed `0` active `continueWatching` notifications when idle. `in.startv.hotstar` had no active notifications or entries. Active `StatusBarNotification` entries were system services (`com.amazon.alexamediaplayer.runtime.ftv`, `com.amazon.ceviche`). | **No** |
| `dumpsys media_session` | `Sessions Stack` lists only active sessions (e.g., `com.amazon.vizzini` / `com.amazon.alexamediaplayer.runtime.ftv` when idle, or an active player session with `PlaybackState {state=3, speed=1.0, error=null}` during playback). Metadata and `PlaybackState` clear once playback stops. | **Live verification only** (not historical) |
| `dumpsys audio` | Logs active/recent `AudioTrack` instances such as `player piid:<id> uid/pid:10203/<pid> type:android.media.AudioTrack attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MOVIE flags=0x200` with `state:started` / `paused` / `stopped` timestamps. | **Live telemetry only** (no titles or content URIs) |
| `dumpsys activity recents` | Exposes task-level `TaskRecord` entries (`com.firelite.tv`, `in.startv.hotstar`, `com.firedrop.tv`, `com.amazon.firebat`, `com.netflix.ninja`, `com.amazon.firetv.youtube`, `com.amazon.venezia`, `org.videolan.vlc`, `com.zee5.amazon`, etc.) without internal fragment or content URIs. | **Package-level only** |

### Why JioHotstar Does Not Populate `watch_next_program` on Fire TV

App analysis (`UpdatePlayNextWork.java` in `in.startv.hotstar`) shows that Android TV `watch_next` synchronization is gated behind the remote feature flag `"androidtv.home.watch_next"` (defaulting to `Boolean.FALSE`) combined with `hasSystemFeature("amazon.hardware.fire_tv")`.

Even though FireLite declares `READ_TV_LISTINGS` and `READ_EPG_DATA` in `tv/src/main/AndroidManifest.xml`, Fire OS restricts `com.amazon.providers.tv.FireTvProvider` to Amazon-signed/system callers, and JioHotstar does not write rows into `content://android.media.tv/watch_next_program`.

---

## 3. FireLite LRU `Recently Watched` Architecture (`MAX_RECENT_ITEMS = 10`)

Because Fire OS blocks third-party access to external OTT history, FireLite bridges the gap in [`TvWatchNextManager`](../tv/src/main/kotlin/com/firelite/tv/util/TvWatchNextManager.kt) (and [`RecentlyWatchedLruCache`](../tv/src/main/kotlin/com/firelite/tv/util/TvWatchNextManager.kt)), [`HotstarDeepLinks`](../core/src/main/kotlin/com/firelite/core/data/HotstarDeepLinks.kt), and [`FireLiteTvScreen`](../tv/src/main/kotlin/com/firelite/tv/ui/FireLiteTvScreen.kt):

### 3.1 Storage, LRU Eviction & Data Flow
1. **Access-Ordered LRU Cache (`RecentlyWatchedLruCache`, capacity `10`)**:
   - Backed by an access-ordered `LinkedHashMap` (`accessOrder = true`) that moves re-watched items to the front (`snapshotRecentFirst()[0]`) and automatically evicts the least-recently-watched item when the 11th distinct show/movie is recorded.
   - Deduplicates across ID prefixes (`cw_` vs raw pin ID vs `wn_`), content IDs, normalized URIs, and `(title, platform)` pairs while preserving metadata such as `artworkResName`.
2. **Local History Store (`loadLocalHistoryEntries` / `saveLocalHistoryEntries`)**:
   - Reads and writes JSON-serialized `RecentlyWatchedLruCache.Entry` items (`item` + `lastWatchedAtMillis`) in `SharedPreferences` (`PREFS_NAME = "firelite_continue_watching"`, key `KEY_RECENT_LRU_ITEMS = "recently_watched_lru_v1"`).
   - Passes items through `HotstarDeepLinks.normalizeMediaItem(..., preferWatch = true)` on read/write.
3. **OS Check & Chronological Merge (`loadWatchNextItems`)**:
   - Attempts to read `WATCH_NEXT_URI` (`content://android.media.tv/watch_next_program`) alongside `last_engagement_time_utc_millis` and merges any OS entries with local LRU entries chronologically, capping at `MAX_RECENT_ITEMS = 10`.
   - Does **not** auto-inject synthetic defaults or force-pin any specific title to index `0`; before any playback is recorded (when OS Watch Next is also empty), the `RECENTLY WATCHED` shelf stays hidden cleanly.
4. **Recording, Launching & Resume Refresh (`launchContent` -> `recordWatch`)**:
   - In `FireLiteTvScreen.launchContent`, FireLite normalizes the target URL (`HotstarDeepLinks.normalizeUrl(item.contentUrl, preferWatch = false)`), records the watch into `TvWatchNextManager.recordWatch(context, item)` (promoting it to slot `0` with a `/watch` URI), refreshes `watchNextItems = TvWatchNextManager.getRecentlyWatchedSnapshot()`, and dispatches `Intent.ACTION_VIEW` (`FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`) with Leanback fallback.
   - `FireLiteTvScreen` also observes `Lifecycle.Event.ON_RESUME` to re-run `TvWatchNextManager.loadWatchNextItems(context)` whenever returning to the launcher.

### 3.2 Package Routing (`packageForUrl`)

`HotstarDeepLinks.packageForUrl` maps known streaming URLs directly to their Fire TV target packages (such as `hotstar://` / `hotstar.com` -> `in.startv.hotstar` and `zee5.com` -> `com.graymatrix.did`), avoiding ambiguous OS chooser dialogs.

---

## 4. Known Constraints & Operational Notes

- **In-App Watches Outside FireLite:** Titles started by browsing directly inside JioHotstar, Netflix, or the stock Amazon launcher (without launching from FireLite) cannot be discovered retroactively via `watch_next_program` or OS telemetry due to Fire OS permission boundaries.
- **Profile State Dependency in JioHotstar:** `Recently Watched` deep links into mature shows (e.g., *The Court*, *Heart Beat*, *I'm Game*) require an adult profile (`Nanda kumar`) to be active in `in.startv.hotstar`. Under the `Kids` profile, JioHotstar blocks playback of `U/A 16+` content with `ERR_PB_1426`. See [JioHotstar Deep Linking & Playback](JIOHOTSTAR_DEEP_LINKING_AND_PLAYBACK.md) for details.

