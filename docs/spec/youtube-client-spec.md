# Spec: Personal YouTube client on Material 3 Expressive

| | |
|---|---|
| Status | Draft v0.1, for Jordan's review |
| Date | 2026-10-03 |
| Base | NewPipe `dev` at `7e5df38aa` (v0.29.1, versionCode 1015) |
| Scope | Strip NewPipe to YouTube only, then rewrite the whole UI in Jetpack Compose with Material 3 Expressive |

---

## 0. Summary

NewPipe is about 78k lines of Android code (80% Java), built on Views, Fragments, RxJava and
ExoPlayer 2.19. That ExoPlayer line is end of life. It supports five services (YouTube, SoundCloud,
PeerTube, media.ccc.de, Bandcamp) through NewPipeExtractor. A small Compose Multiplatform module
(`shared/`) already exists, with Material 3 Expressive turned on, but it only renders About and two
settings screens.

The plan:

1. **Phase 0, strip.** Remove the other four services, the multi-platform targets, and the features
   that only make sense for a public F-Droid app. Rename the app so it installs next to NewPipe.
2. **Phase 1, foundation.** Build a new Android-only Compose app with one activity: an M3 Expressive
   design system, Navigation 3, Koin, and coroutine repositories over NewPipeExtractor and the
   existing Room database.
3. **Phase 2, player.** Port the player from ExoPlayer 2 to Media3. Use a `MediaLibraryService` and
   Compose player UI, plus a YouTube-style mini player, PiP and background play.
4. **Phase 3, screens.** Home, Shorts, Subscriptions, You, Search, Watch, Channel, Playlist,
   Comments and Settings, following the current YouTube information architecture.
5. **Phase 4, cutover.** Delete the legacy `app/` and `shared/` code. Import existing NewPipe data
   through the backup zip.

What stays: NewPipeExtractor (all YouTube parsing), the Room schema, the PoToken WebView provider,
the YouTube DataSource, the DASH manifest creators, feed loading, subscription import, and backup
import.

What gets rewritten: every screen, the player host and UI, the state layer (RxJava to Flow), and
the DAOs.

---

## 1. Goals, non-goals, guardrails

### Goals
- A YouTube client for Jordan's own use: no account, no ads, background play, PiP, local
  subscriptions, local history, local playlists.
- Look and feel on par with the 2025/2026 YouTube Android app (layout, gestures, mini player,
  Shorts), drawn in Material 3 Expressive instead of YouTube's own design language.
- A codebase that is modern and easy for one person to maintain: Kotlin only, Compose only,
  coroutines and Flow only, Media3.
- Keep YouTube working through NewPipeExtractor updates without touching the UI.

### Non-goals
- Signing in to a Google account, real likes, real comments, uploads, or live chat. The extractor
  is read-only and anonymous.
- Other services, iOS, desktop, Android TV, Wear.
- Publishing to Google Play. Play does not allow YouTube downloaders or ad-free YouTube clients,
  and a fork cannot use the NewPipe trademark.

### Guardrails
- **This is personal use, not a product launch.** That is why the demand-validation and
  monetization rules in Jordan's global instructions don't apply. If this ever ships publicly, both
  apply first, and Play Store distribution is ruled out (see the non-goals).
- **License.** The fork stays GPL-3.0-or-later. Keep `LICENSE` and the SPDX headers. Keep an About
  screen with open-source licences (the existing aboutlibraries setup covers this).
- **Trademark.** "NewPipe" is a registered word mark. Change the app name, icon and
  `applicationId` in Phase 0.
- **Zero budget.** Every library and tool named here is free and open source. SponsorBlock and
  Return YouTube Dislike (section 8, "Later") are free public APIs.

---

## 2. What exists today (synthesis)

### 2.1 Modules and stack

| Module | What it is | Fate |
|---|---|---|
| `:app` | The real app. Package `org.schabi.newpipe`, 281 Java files (~62.8k lines) + 150 Kotlin files (~15.2k lines). Views, Fragments, ViewBinding, RecyclerView, Groupie. | Mined for logic, then deleted at cutover |
| `shared` | Compose Multiplatform (Android, iOS, JVM). Package `net.newpipe.app`. Material3 1.11.0-alpha07 (JetBrains), Expressive opt-in, Navigation 3, Koin 4.2.2, multiplatform-settings. Has About, Settings home, Appearance settings, theme. | Ideas and composables move to the new design system; module deleted |
| `:desktopApp` | Compose window that hosts `shared` About | Delete |
| `iosApp` | Xcode wrapper around `shared` | Delete |
| `buildSrc` | SDK and version constants, TOML sort check | Keep, update constants |

Key versions today: AGP 9.3.1, Kotlin 2.4.10, JDK 21, compileSdk 37, targetSdk 35, minSdk 23,
ExoPlayer 2.19.1, Room 2.8.4 (rxjava3 artifact), RxJava 3.1.12, WorkManager 2.11.2,
OkHttp 5.5.0, Coil 3.5.0, NewPipeExtractor at JitPack commit `13a655fe`.

### 2.2 Subsystems

**UI shell.** `MainActivity` is a DrawerLayout with a toolbar, a fragment holder, and a
`BottomSheetBehavior` holder for `VideoDetailFragment`, which doubles as the mini player.
`MainFragment` holds a ViewPager of tabs you can customise (`settings/tabs/Tab.java`). Navigation is
manual fragment transactions in `util/NavigationHelper.java` (774 lines). `RouterActivity` handles
share intents and deep links, and shows an "open with" chooser (info, video, popup, background,
enqueue, download, add to playlist).

**Watch page.** `VideoDetailFragment.java` is 2,527 lines. It covers the player placeholder, title,
uploader row, primary actions (playlist, background, popup, download), secondary actions (share,
browser, Kodi), and bottom tabs: comments, related (with an autoplay switch), description with
linkified timestamps and hashtags, and metadata.

**Lists.** `BaseListFragment` and `BaseListInfoFragment` hold Search (suggestions, YouTube and
YouTube Music filters, "did you mean"), Channel (banner, subscribe, tabs: videos, shorts, live,
playlists, releases, podcasts, courses, about), Kiosk, Playlist, and Comments with replies.
Long-press on a stream opens `InfoItemDialog` (enqueue, enqueue next, background, popup, download,
add to playlist, share, browser, Kodi, mark watched, channel).

**Player.** One `PlayerService` (a `MediaBrowserServiceCompat`) and one `Player.java` (2,488 lines)
with pluggable UIs: `MainPlayerUi`, `PopupPlayerUi` (a WindowManager overlay) and
`BackgroundPlayerUi`. Media session, notification (5 configurable slots) and Android Auto browsing
are separate plug-ins. The player view tree is inflated inside the Service and moved into the
fragment with `addView`. Lifecycle is passed around by broadcast intents. The play queue is
serialized into a static cache and handed over by key. YouTube playback uses manifest creators from
the extractor (progressive to DASH, OTF, post-live DVR) and a forked `YoutubeHttpDataSource` that
sends `&range=`/`&rn=` and POST requests. There is no PiP, no sleep timer and no SponsorBlock.

**PoToken.** `util/potoken/` (684 lines of Kotlin) runs BotGuard in a hidden WebView to mint
visitor and player tokens. It is registered once with `YoutubeStreamExtractor.setPoTokenProvider`.
The extractor will not play YouTube without it.

**Data.** Room DB v9 (`newpipe.db`) has 12 tables. `service_id` appears only in `subscriptions`,
`streams`, `remote_playlists` and `search_history`. Everything else hangs off those through
cascading foreign keys. Features: subscriptions with feed groups, a feed with a fast RSS mode and
new-video notifications, watch history and stats, resume positions, search history, local
playlists (reorder, auto thumbnail, dedupe, remove watched), bookmarked remote playlists, Takeout
import, JSON export, and a zip backup (DB + `preferences.json`).

**Settings.** Nine categories in XML preference screens (about 6.9k lines) with fuzzy settings
search. Preference migrations are at version 9.

**Downloads.** `us/shandian/giga` (~6.8k lines) plus muxers in `streams/` (~5.5k lines). It is a
multithreaded resumable engine with SAF, DASH-to-MP4/WebM/OGG muxing, TTML to SRT, URL recovery,
and its own SQLite DB.

**Infra.** `App.kt` initialises ACRA, prefs migrations, the extractor, notification channels and
the PoToken provider. There is no DI in `:app`. `DownloaderImpl` is an OkHttp singleton that holds
the reCAPTCHA and restricted-mode cookies. Errors go through `ErrorActivity`, `ErrorInfo` and
`ReCaptchaActivity`. There are 22 JVM tests and 7 instrumented tests. CI builds the `continuous`
variant.

### 2.3 Multi-service footprint
- About 704 `serviceId` references across 113 files. Most of it is plumbing (NavigationHelper,
  ExtractorHelper, Tab JSON, play queue items).
- Service-only code: the PeerTube instance UI and helper, the drawer service switcher, SoundCloud,
  MediaCCC and Bandcamp empty states and error mappings, 12 service theme styles, 16 service
  colours, about 70 manifest intent filters for non-YouTube hosts, and about 1,421 translated
  strings across 82 locale folders.
- The extractor dependency still bundles every service. Removal is app-side work only.

### 2.4 Keep, port or drop

| Area | Decision | Reason |
|---|---|---|
| NewPipeExtractor dependency | **Keep** as is | All YouTube parsing. Upstream fixes arrive by bumping the commit. |
| `util/potoken/*` | **Keep**, light port (Rx to coroutines) | Already Kotlin. Needed for playback. |
| `YoutubeHttpDataSource`, manifest creator wiring, `ListHelper` itag logic | **Port** to Media3 + Kotlin | Core of YouTube playback. Behaviour must not change. |
| Play queue model and `MediaSourceManager` | **Replace** with a Media3 playlist plus a lazy resolver | Media3 handles windowing. The custom Rx engine is the main tech debt. |
| Room entities and schema | **Keep** | Same schema, so NewPipe backups import directly. |
| DAOs | **Rewrite** with `Flow`/`suspend` | Drop room-rxjava3. |
| Feed loading, notification worker, subscription import/export | **Port** (mostly Kotlin already) | Proven logic. Swap Rx for coroutines. |
| Backup import/export | **Keep format**, port | Lets Jordan bring over existing NewPipe data. |
| `DownloaderImpl`, cookies, reCAPTCHA flow | **Port** | Needed by the extractor and for bot checks. |
| All Fragments, Activities, layouts, adapters | **Drop** | Replaced by Compose. |
| `shared/` module | **Drop**, salvage theme and preference composables | Becomes Android-only. |
| Downloads engine | **Decision D2** | Biggest optional block. |
| Popup (WindowManager) player | **Decision D3**, recommend drop in favour of PiP | PiP is the platform standard and costs far less. |

---

## 3. Target product

### 3.1 Information architecture

The 2025/2026 YouTube Android app has five bottom destinations: Home, Shorts, Create,
Subscriptions, You. Create makes no sense without an account, so this app has four.

```
App
├── Home                (bottom nav 1)
├── Shorts              (bottom nav 2)
├── Subscriptions       (bottom nav 3)
├── You                 (bottom nav 4)
│   ├── History
│   ├── Playlists  →  Playlist (local) / Playlist (saved remote)
│   ├── Watch later  (local playlist)
│   ├── Liked videos (local playlist)
│   ├── Downloads    (if D2 keeps downloads)
│   └── Settings  →  categories
├── Search              (top bar action, full screen)
├── Notifications       (top bar action: new uploads from belled channels)
├── Channel             (pushed)
├── Playlist            (pushed)
├── Watch               (global layer: expanded / mini / fullscreen / PiP)
│   ├── Description sheet
│   ├── Comments sheet → Replies
│   ├── Chapters sheet
│   ├── Queue sheet
│   └── Player settings sheet
└── Router (share / deep links, no UI unless "always ask")
```

Watch is a **global layer** that sits above the navigation stack, the way YouTube does it. Opening
a video never pushes a destination. It expands the player layer, and collapsing it shows the mini
player docked above the bottom bar while you keep browsing.

### 3.2 Global chrome

**Top app bar (Home, Subscriptions, You).** App logo/wordmark on the left. On the right: Search and
Notifications (with a badge count of unseen items). Built from `TopAppBar` with
`enterAlwaysScrollBehavior`, so it hides on scroll down and comes back on scroll up, like YouTube.

**Bottom navigation.** `ShortNavigationBar` with four items on compact widths. On medium and
expanded widths it becomes a `WideNavigationRail` (collapsed rail on medium, expanded on expanded).
Selected icons are filled, unselected are outlined. Labels are always shown. Reselecting a tab
scrolls it to the top, and reselecting at the top refreshes.

**Mini player.** Docked above the nav bar, 64dp tall, full width, with a `surfaceContainerHigh`
background. It shows the live video surface (16:9, not a thumbnail), title and channel on two
lines, play/pause and close. A 2dp progress line runs along the top edge. Drag up or tap to expand.
Swipe sideways to dismiss (stop and clear the queue).

**Snackbars** for undoable actions (removed from playlist, unsubscribed, cleared history).

### 3.3 Screens

Every screen has these states: Loading (Expressive `LoadingIndicator`, or skeleton placeholders
for lists), Content, Empty (illustration from `MaterialShapes` + one sentence + one action), and
Error (a message that says what to do, plus Retry; reCAPTCHA errors open the solve flow;
403/IP-ban errors link to the explanation).

#### Home
- **Purpose:** what to watch now, with no account.
- **Chip row** (`FilterChip`, scrolls sideways, sticky under the top bar): All, Subscriptions,
  Live, Music, Gaming, Movies & shows, Podcasts. The last five map to YouTube kiosks
  (`live`, `trending_music`, `trending_gaming`, `trending_movies_and_shows`,
  `trending_podcasts_episodes`).
- **"All" feed** is assembled locally, in this order:
  1. "Continue watching" shelf: streams in `stream_state` that were started but not finished,
     newest first, in a horizontal carousel.
  2. New uploads from subscriptions (the feed table), as large 16:9 cards.
  3. A Shorts shelf every ~8 cards: Shorts from subscribed channels, in a
     `HorizontalMultiBrowseCarousel` of 9:16 tiles.
  4. **Local recommendations** (version 1.1, see risk R4): related videos of the last N watched
     videos, ranked by how often they show up, minus watched videos and hidden channels.
- **Video card:** full-bleed 16:9 thumbnail with a duration badge (or LIVE / UPCOMING / SHORTS
  badge), a red resume bar along the bottom if partly watched, a channel avatar, two lines of
  title, and `channel · views · age`. A ⋮ button opens the item actions sheet.
- **Pull to refresh** with `PullToRefreshBox` and the Expressive indicator.

#### Shorts
- **Purpose:** a vertical swipe player for Shorts.
- **Source:** Shorts from subscribed channels (fetch the `shorts` channel tab during feed refresh),
  plus any Short opened from search, related, or a channel. Unwatched first. See risk R5: the
  extractor has no global Shorts feed.
- **Layout:** `VerticalPager`, one Short per page, full screen, edge to edge. The right edge has a
  column of actions: Like (local), Comments, Share, Save, More. The bottom left has the channel
  avatar, name and Subscribe, then the title (two lines, tap to expand). A thin progress bar sits
  at the bottom.
- **Playback:** a pool of three Media3 players (previous, current, next) that preload the next
  page. Tap to pause. Long-press for 2x speed. Shorts loop.
- **Empty state:** "Subscribe to channels to see their Shorts here", with a Search action.

#### Subscriptions
- **Channel strip** (top): a horizontal row of circular avatars with names, most recently
  uploaded first, and an "All" button at the end that opens the full channel list. Tapping an
  avatar filters the feed to that channel.
- **Chip row:** All, Today, Unwatched, Continue watching, Live, then one chip per **feed group**.
  Feed groups are the existing `feed_group` table. Long-press a group chip to edit it.
- **Feed list:** the same card as Home, sorted by upload date. Infinite scroll over the up to 500
  stored items.
- **Refresh:** pull to refresh, plus the periodic worker. During a refresh a `LinearWavyProgressIndicator`
  under the chips shows "Updating 37/120". Failed channels show up in a dismissible banner that
  opens a list with Retry or Unsubscribe.
- **Manage** (from the overflow menu): a channel list with search, per-channel bell toggle,
  unsubscribe, group assignment, **Import** (Google Takeout zip/CSV, previous NewPipe export) and
  **Export**.

#### You
YouTube's "You" page, without an account:
- **Header:** app name and a settings shortcut. No avatar.
- **History** shelf: a horizontal carousel of recent videos, with "View all" opening History
  (searchable, grouped by day, swipe to remove, "Clear all", pause history toggle).
- **Playlists** shelf: local and saved playlists, plus the two system playlists below, with
  "View all" and a "New playlist" item.
- **Watch later:** a local playlist created on first run and pinned first. Every item's actions
  sheet has "Save to Watch later".
- **Liked videos:** a local playlist. The Like button on Watch and Shorts adds to it. Dislike is
  stored locally and hides the video from local recommendations.
- **Downloads** row (if D2 keeps downloads), **Stats** row (most watched, from
  `StreamHistoryDAO.getStatistics`), **Settings** row.

#### Search
- A full-screen search (`SearchBar` expanding into `ExpandedFullScreenSearchBar`) that opens from
  the top bar search icon.
- **Suggestions:** local history (clock icon, swipe or long-press to delete) and remote
  suggestions (search icon). An arrow-up-left button copies a suggestion into the field. A voice
  search mic uses `RecognizerIntent` (free, built into the system).
- **Results:** a chip row of content filters the extractor offers for YouTube (All, Videos,
  Channels, Playlists, then the YouTube Music filters: Songs, Videos, Albums, Playlists, Artists).
  Mixed results: video cards, channel rows (large avatar + Subscribe), playlist cards (stacked
  thumbnail), and a Shorts shelf when results include Short-form items
  (`StreamInfoItem.isShortFormContent`). A "Showing results for X / Search instead for Y"
  correction bar.
- **Pasting a YouTube URL** routes straight to Watch, Channel or Playlist.

#### Watch (expanded)
Top to bottom, on a phone in portrait:
1. **Player**, 16:9 (or the video's aspect ratio, capped at 1:1 for vertical videos), with
   **ambient mode**: a soft glow behind the player in the dominant colours of the current frame or
   thumbnail, the way YouTube does it.
2. **Title** (`titleLarge` emphasized, two lines, tap to open the description sheet), then
   `views · age · ...more`.
3. **Channel row:** avatar, name, subscriber count, and a **Subscribe** button. Subscribe is a
   filled tonal button that morphs shape on press and turns into a bell toggle once subscribed.
4. **Action row** (a horizontally scrolling `ButtonGroup`):
   - Like and Dislike as one connected pill. The like count comes from the extractor and is read
     only; pressing Like saves to the local Liked playlist.
   - Share, Save (playlist picker sheet), Download (if D2), Background, then a ⋮ overflow for
     "Open in browser", "Copy link at current time", "Mark as watched".
5. **Comments teaser card:** comment count and the top comment. Tap to open the Comments sheet.
6. **Related videos** as cards, with an **Autoplay** switch in the player's top bar (not here).

Sheets (`ModalBottomSheet`, partial height, drag to expand):
- **Description:** title, a stats row (likes, views, date), the linkified description (timestamps
  seek, hashtags search, links open in-app or in the browser), a chapters list, tags as chips, and
  metadata (category, licence, language, visibility).
- **Comments:** sort is "Top" only (what the extractor gives). Each comment shows avatar, author,
  age, text (5 lines, expandable), likes, a heart if the creator hearted it, a pinned label, and
  "N replies", which opens a replies sub-page inside the sheet.
- **Chapters:** thumbnail, title and start time per chapter. The current one is highlighted. Tap to
  seek.
- **Queue:** the current queue with drag to reorder, swipe to remove, Loop and Shuffle toggles, and
  "Save queue as playlist".

**Expanded layout (tablet, foldable open, landscape on large screens):** two panes. The left pane
has the player and info. The right pane has related videos, with comments as a side sheet. On a
foldable in tabletop posture, the video sits on the top half and the controls plus info on the
bottom half.

#### Player controls (inline and fullscreen)
Modelled on the 2025 YouTube player: translucent, minimal, with as little covering the video as
possible.
- **Tap** shows or hides the controls (auto-hide after 3s while playing).
- **Top bar:** a collapse chevron (to the mini player), Autoplay toggle, Captions toggle, and a
  Settings gear.
- **Centre:** previous, a large play/pause that **morphs shape** (circle to rounded square, via
  `MaterialShapes` + `Morph`), next. These are translucent circular buttons.
- **Bottom:** current time / duration, a chapter title chip (tap to open the Chapters sheet),
  fullscreen.
- **Seekbar:** split into chapter segments. While scrubbing it grows taller, shows the storyboard
  thumbnail and timestamp above the thumb, and gives haptic ticks at chapter edges. Live videos
  show a red LIVE pill that jumps to the live edge.
- **Gestures:**
  - Double-tap left or right third: seek back or forward (default 10s), chained taps add up, with
    the existing ripple-and-seconds overlay redrawn in Compose.
  - Long-press: 2x speed while held, with a "2x ▸▸" pill.
  - Swipe down on the inline player: collapse to the mini player.
  - Swipe up in fullscreen: show a related-videos panel. Swipe down: leave fullscreen.
  - Fullscreen only: vertical swipe on the left half for brightness, right half for volume
    (configurable, as now).
  - Pinch: switch between fit and zoom-to-fill.
- **Settings sheet** (from the gear): Quality (Auto / list), Playback speed (slider 0.25x to 4x
  plus preset chips; the advanced part has pitch, semitones, and unlinking pitch from tempo,
  ported from `PlaybackParameterDialog`), Captions (language list, style follows the system
  caption settings), Audio track (original, dubbed, descriptive), Sleep timer (new: 10, 15, 30, 45,
  60 minutes, end of video), Loop video, Skip silence.
- **End of video:** the next autoplay item shows as a countdown card (5s), with Cancel.

#### Background, PiP, notification
- **Background play** works with the screen off or the app in the background. Video and subtitle
  tracks are turned off when nothing is showing them, as today (`useVideoAndSubtitles(false)`).
- **PiP:** auto-enter when going home while a video plays (`setAutoEnterEnabled`, Android 12+),
  with play/pause, previous and next as PiP actions. This replaces the popup player (decision D3).
- **Media notification and lock screen:** Media3's default media notification, plus custom
  commands for Loop, Shuffle and Close. The five-slot customisation screen is dropped (decision
  D6, recommend drop).
- **Android Auto:** browse Watch later, Liked, Playlists and History, and search by voice
  (decision D7, recommend keep; it costs little with `MediaLibraryService`).

#### Channel
- **Header:** a banner (16:6, fading into the background), a large avatar, the name in
  `headlineMedium` emphasized, then `@handle · N subscribers · N videos`, the description in one
  line (tap for the About sheet), and Subscribe + bell.
- **Tabs** (`PrimaryScrollableTabRow`, sticky): Videos, Shorts, Live, Releases, Playlists,
  Podcasts, Courses. Only tabs that the extractor returns for this channel are shown.
- **Videos/Live/Podcasts:** a list of video cards. **Shorts:** a 3-column grid of 9:16 tiles with
  view counts. **Playlists/Releases/Courses:** playlist cards.
- **About sheet:** description, links, join date, total views, country (whatever the extractor
  provides).
- **Overflow:** Share, Open in browser, Play all (queue the Videos tab), Add to a feed group.

#### Playlist (remote and local)
- **Hero:** the first thumbnail, blurred and tinted with a scheme made from that thumbnail
  (content-based dynamic colour), behind the title, owner, `N videos · total duration`, and the
  description in two lines.
- **Actions:** a `ButtonGroup` with **Play all** (filled) and **Shuffle** (tonal), then
  Save/bookmark, Share and ⋮.
- **Local playlists also have:** rename, delete, choose thumbnail, remove watched (optionally
  partly watched too), remove duplicates, drag to reorder, swipe to remove, and share as a list of
  URLs, as titles plus URLs, or as a YouTube temporary playlist (first 50 items, existing
  `ExportPlaylist.kt`).
- **Multi-select** (long-press to start): a `HorizontalFloatingToolbar` at the bottom with Play,
  Add to queue, Move to playlist, Remove.

#### Notifications
- A list of new uploads from channels whose bell is on (`notification_mode = 1`), newest first,
  with unseen ones marked. Built from the feed table. This page needs no extra network calls.
- Settings for the background check (interval, Wi-Fi only) live in Settings, as now.

#### Item actions sheet (replaces InfoItemDialog)
For a video: Play next, Add to queue, Save to Watch later, Save to playlist, Download (D2), Share,
Play in background, Play in PiP, Mark as watched / unwatched, Go to channel, Not interested (hides
it from local recommendations), Don't recommend this channel. For a channel: Subscribe/Unsubscribe,
Share, Open in browser. For a playlist: Save, Share, Play all, Shuffle.

#### Router (share and deep links)
- Keep the YouTube intent filters: `youtube.com`, `m.`, `www.`, `music.youtube.com`, `youtu.be`,
  `youtube-nocookie.com/embed/`, `vnd.youtube:` and `vnd.youtube.launch:` schemes, `y2u.be`,
  plus the Invidious and Piped hosts (and fix the `inv.nadeko.net/` trailing slash bug).
- Default action: open Watch (or Channel or Playlist) in-app and start playing. The "always ask"
  chooser becomes a bottom sheet with Watch, Background, Add to queue, Save to Watch later,
  Download (D2), and a "Remember my choice" toggle.
- Plain shared text with no URL opens Search with that text.

#### Settings
Compose settings with a search field at the top (port the fuzzy search idea; a list of entries
generated in code replaces XML parsing). Categories after the strip:

| Category | Contents |
|---|---|
| Appearance | Theme (system, light, dark), pure black in dark, dynamic colour on/off, accent colour when dynamic colour is off, list density, app language |
| Playback | Default quality (Wi-Fi, mobile), default video and audio format, prefer original audio, prefer descriptive audio, autoplay (always, Wi-Fi only, never), resume playback, seek step, inexact seek, gestures (left/right side), start in fullscreen, PiP on home, minimise behaviour, skip silence, captions style link |
| Content | Content country, content language, restricted mode, show age-restricted content, image quality, search suggestions (local, remote), show comments, show related |
| Feed and notifications | Feed refresh threshold, fast mode (RSS), tabs to fetch (videos, shorts, live), new-upload notifications on/off, check interval, Wi-Fi only, per-channel bells |
| History and privacy | Watch history, search history, resume positions, show watched indicators, clear watch history, clear search history, clear playback states, clear cache, clear reCAPTCHA cookies |
| Downloads (D2) | Folders, thread count, retries, pause on mobile data, one at a time |
| Backup | Export (zip), Import (zip, including NewPipe backups), import subscriptions, export subscriptions, reset settings |
| Advanced | ExoPlayer/Media3 workarounds (decoder fallback, disable tunnelling, setOutputSurface workaround), progressive load interval |
| About | Version, licence, open-source libraries, source link (Jordan's fork) |

Debug-only entries (crash the player, show error snackbar, check new streams now) go into a
"Developer" category visible only in debug builds.

---

## 4. Design system: Material 3 Expressive

### 4.1 Library baseline
- Move from JetBrains Compose Multiplatform artifacts to **AndroidX Compose** (BOM) because the app
  becomes Android-only.
- `androidx.compose.material3:material3` on the **1.5.0 alpha line**. The Expressive components are
  there, behind `@ExperimentalMaterial3ExpressiveApi`. Stable 1.4.0 lacks some of them. Pin an exact
  alpha at kickoff and bump on purpose (risk R6).
- `androidx.compose.material3:material3-adaptive-navigation-suite` and
  `material3-window-size-class` for adaptive layout.
- `androidx.graphics:graphics-shapes` for shape morphing (comes with Material3 for
  `MaterialShapes`).

### 4.2 Theme
`MaterialExpressiveTheme(colorScheme, motionScheme = MotionScheme.expressive(), shapes, typography)`
wraps the app.

**Colour**
- Dynamic colour (Material You) on Android 12+ by default.
- Fallback and opt-out: a scheme generated from a YouTube-adjacent red seed (`#E53935`, the current
  YouTube light primary), with the "expressive" scheme variant so containers carry more colour.
- Dark theme follows the system by default. "Pure black" sets `surface`, `background` and the
  `surfaceContainer*` roles to near-black for OLED, as the existing `blackScheme` does.
- **Content-based colour** on Watch (ambient mode) and Playlist heroes: generate a scheme from the
  thumbnail on a background thread. Only the hero area uses it; the rest of the screen keeps the
  app scheme so contrast stays predictable.
- YouTube red is reserved for meaning: LIVE badges, the resume bar on thumbnails, and the
  notification badge. It is never decoration.

**Typography**
- Roboto Flex (variable, free) via downloadable Google Fonts or bundled. It suits Expressive's
  emphasized styles.
- Use the **emphasized** type styles (`*Emphasized`) for screen titles, the Watch title, channel
  names in headers, and chips that are selected. Body text uses the regular scale.
- Numbers (durations, counts, timestamps) use tabular figures.

**Shape**
- Default `Shapes` from Expressive, with these assignments:

| Element | Shape |
|---|---|
| Video thumbnails in cards | `large` (16dp) on Home/feeds; `medium` (12dp) in dense lists |
| Shorts tiles | `extraLarge` |
| Bottom sheets, mini player top corners | `extraLarge` top corners |
| Channel avatars | circle; in the channel header, a `MaterialShapes.Cookie9Sided` frame that morphs to circle on load |
| Primary buttons | fully rounded; morph to a squarer shape on press (Expressive button shape morph) |
| Empty-state art | a large `MaterialShapes` shape (Clover, Sunny, Pill) filled with `primaryContainer` and an icon |

**Motion**
- `MotionScheme.expressive()` (spring based) for spatial movement: the mini player expanding,
  sheets, chip selection, button shape morphs.
- Effects motion (colour, alpha) uses the scheme's effects specs, so nothing bounces.
- If the system animator scale is 0 or "remove animations" is on, switch to
  `MotionScheme.standard()` and skip non-essential motion.
- Shared element transitions (`SharedTransitionLayout`) for thumbnail → Watch player and
  avatar → Channel header.

### 4.3 Component map

| Need | Component |
|---|---|
| Bottom navigation | `ShortNavigationBar` + `ShortNavigationBarItem` |
| Tablet/foldable navigation | `WideNavigationRail` (collapsed / expanded) |
| Adaptive switch | `NavigationSuiteScaffold` or a small switch on the window size class |
| Loading | `LoadingIndicator`; `ContainedLoadingIndicator` on top of content and in pull to refresh |
| Progress (feed refresh, downloads, buffering in mini player) | `LinearWavyProgressIndicator` / `CircularWavyProgressIndicator` |
| Watch action row, Like/Dislike pill, playlist Play all/Shuffle | `ButtonGroup`, connected buttons |
| Split actions ("Play all" + ▾ for background/queue) | `SplitButtonLayout` |
| Multi-select actions | `HorizontalFloatingToolbar` |
| "New" actions in You and Playlists (new playlist, import) | `FloatingActionButtonMenu` |
| Search | `SearchBar` / `ExpandedFullScreenSearchBar` |
| Filters | `FilterChip` rows |
| Shelves (Continue watching, Shorts) | `HorizontalMultiBrowseCarousel` / `HorizontalUncontainedCarousel` |
| Toggles (Autoplay, Loop, Shuffle, bell) | `ToggleButton` / `IconToggleButton` |
| Sheets | `ModalBottomSheet` |
| Lists in settings and sheets | `ListItem` with Expressive grouping (segmented, rounded group corners) |
| Top bars | `TopAppBar`, `MediumFlexibleTopAppBar` for Channel/Playlist titles |

### 4.4 Adaptive layout
- **Compact** (phones): bottom bar, single pane, Watch as a full-screen layer.
- **Medium** (small tablets, foldables open in portrait): collapsed rail, 2-column video grid,
  Watch stays single-pane with a wider info column.
- **Expanded** (tablets, landscape foldables): expanded rail, 3 to 4 column grid, Watch in two
  panes (section 3.3).
- **Foldable postures:** tabletop puts the video above the fold. Book posture uses two panes.
- **Landscape on a phone:** rotating while a video is expanded enters fullscreen. Otherwise lists
  just reflow.

### 4.5 Accessibility
- Every icon button has a content description. The player announces state changes ("Paused",
  "Seeked forward 10 seconds").
- Touch targets are at least 48dp, including the mini player controls.
- The layout holds at 200% font scale. Card titles wrap; nothing gets cut off mid-word in a fixed
  box.
- Colour contrast passes WCAG AA for text in both themes and on content-based hero colours (check
  the generated scheme and fall back to the app scheme if it fails).
- Captions are on whenever the system caption preference is on.

---

## 5. Architecture

### 5.1 Module graph

```
:app                     single Activity, nav host, DI root, Router entry
:core:designsystem       theme, tokens, shared composables (cards, chips, shelves, empty/error states)
:core:model              plain Kotlin domain models (VideoItem, VideoDetails, Channel, Playlist, Comment, ...)
:core:extractor          NewPipe init, DownloaderImpl, cookies, PoToken provider, mappers extractor → model
:core:database           Room DB (schema v9 → v10), DAOs with Flow/suspend
:core:data               repositories (Video, Channel, Playlist, Search, Feed, Subscription, History, Library, Settings)
:core:player             Media3 MediaLibraryService, resolvers, YouTube DataSource, queue, PlayerController
:core:work               WorkManager workers (feed refresh, new-upload notifications, import/export)
:core:download           (only if D2 keeps downloads) existing engine behind a Kotlin interface
:feature:home  :feature:shorts  :feature:subscriptions  :feature:library (You)
:feature:search  :feature:watch  :feature:channel  :feature:playlist
:feature:settings  :feature:notifications
```

Rules: features depend on `core:*` only, never on each other. Navigation between features goes
through `NavKey`s defined in `:app` (or a small `:core:navigation`). Only `:core:extractor` sees
NewPipeExtractor types. Everything above it sees `:core:model`.

### 5.2 Patterns
- **UI:** a stateless screen composable taking `UiState` and `onEvent`, plus a stateful wrapper
  that gets the ViewModel from Koin. Previews use fake states.
- **State:** `ViewModel` exposes one `StateFlow<UiState>` (sealed: Loading, Content, Error), built
  with `stateIn(viewModelScope, WhileSubscribed(5_000), Loading)`. One-off effects (snackbar,
  navigate) go through a `Channel`.
- **Paging:** extractor pages are `Page` tokens. Use a small in-house pager (load more when 5 items
  from the end) instead of Paging 3. The extractor's model doesn't fit PagingSource well and the
  lists are not huge.
- **Threading:** extractor and DB calls are `suspend` on `Dispatchers.IO`. No RxJava anywhere.
  `kotlinx-coroutines-rx3` stays only while legacy code still exists, and goes at cutover.
- **DI:** Koin (already in the repo, with annotations and the compiler plugin). One module per Gradle
  module.
- **Navigation:** AndroidX Navigation 3 with `@Serializable` `NavKey`s per tab back stack. Each
  bottom tab keeps its own back stack, the way YouTube does. The Watch layer is not a nav entry; it
  is state in a `PlayerController` held at Activity scope.
- **Images:** Coil 3 sharing the OkHttp client, with the existing `ImageStrategy` quality
  preference ported.
- **Settings storage:** keep `SharedPreferences` with the **same keys** as NewPipe, behind a typed
  `SettingsRepository` that exposes `Flow`s. This keeps NewPipe `preferences.json` imports
  working. DataStore buys nothing here.
- **Serialization:** kotlinx.serialization for nav keys, export JSON and tab config.

### 5.3 Extractor layer (`:core:extractor`)
- `NewPipe.init(DownloaderImpl, localization, country)` at app start, as now.
- `YoutubeStreamExtractor.setPoTokenProvider(PoTokenProviderImpl)`, as now.
- `DownloaderImpl` ported to Kotlin. It keeps the user agent, the Brotli/gzip interceptor,
  reCAPTCHA cookie and restricted-mode cookie. Debug builds add an `HttpLoggingInterceptor`
  (replaces Stetho).
- An `InfoCache` (in-memory LRU) with the same expiry as today (1 hour for YouTube).
- Mappers from extractor types (`StreamInfo`, `StreamInfoItem`, `ChannelInfo`, `ChannelTabInfo`,
  `PlaylistInfo`, `CommentsInfoItem`, `SearchInfo`, `KioskInfo`) to `:core:model` types.
- `serviceId` is the constant `YouTube.serviceId` (0) in this module only.
- **Extractor updates:** pinned by commit in `libs.versions.toml`. When YouTube breaks, bump to the
  latest `TeamNewPipe/NewPipeExtractor` commit that fixes it. Watch upstream NewPipe releases for
  app-side fixes to PoToken and `YoutubeHttpDataSource`, which do **not** come with the extractor
  and must be ported by hand (risk R1).

### 5.4 Database (`:core:database`)
- Same 12 tables. Same file name `newpipe.db` so a NewPipe backup restores into it.
- **Migration 9 → 10:** delete non-YouTube rows (section 7.1).
- DAOs rewritten to `Flow<List<...>>` for observed queries and `suspend` for one-shot reads and
  writes. Drop `room-rxjava3`.
- Keep the `service_id` columns, always written as 0. Dropping them would need a table rebuild and
  would break importing NewPipe backups, for no real gain.
- Seed the two system playlists (Watch later, Liked videos) on first run, identified by a stored
  playlist id in settings (no schema change).
- Keep `DatabaseMigrationTest` and add 9 → 10 cases.

### 5.5 Player (`:core:player`)
**Shape:** a `MediaLibraryService` (Media3 `session`) owns one `ExoPlayer`. The UI talks to it
through a `MediaController` wrapped in an Activity-scoped `PlayerController` that exposes
`StateFlow<PlayerUiState>` (current item, position, buffered position, isPlaying, chapters,
qualities, audio tracks, captions, speed, repeat, shuffle, sleep timer, error).

**Pipeline**
1. The queue is a Media3 playlist of `MediaItem`s that carry only a YouTube URL and display
   metadata (title, channel, thumbnail) in `MediaMetadata`.
2. A custom `MediaSource.Factory` resolves each item lazily when the player needs it: fetch
   `StreamInfo` (through the cache), pick streams with the ported `ListHelper` logic (itag
   allow-list, preferred resolution, format, audio track, original/descriptive audio), then build
   the source with the ported `PlaybackResolver.createYoutubeMediaSource` rules:
   - progressive video-only or audio → a DASH manifest from `YoutubeProgressiveDashManifestCreator`,
     falling back to progressive
   - muxed progressive → `ProgressiveMediaSource`
   - OTF → `YoutubeOtfDashManifestCreator`
   - post-live → `YoutubePostLiveStreamDvrDashManifestCreator`
   - live → DASH first, then HLS, with the `availabilityStartTime = 0` fix
   - video + separate audio + subtitles → `MergingMediaSource`
3. `YoutubeHttpDataSource` is ported to the Media3 `DataSource` API with identical behaviour
   (`&range=`, `&rn=`, POST body, headers, per-client user agent). Write a golden test that records
   the request it builds for each stream type before and after the port.
4. Media3 `SimpleCache` replaces the ExoPlayer 2 cache, in the same cache dir, with an LRU cap.
5. **URL expiry:** when a resolved URL returns 403 or "resource gone", re-resolve once through the
   extractor (bypassing the cache) and resume at the same position.

**Features carried over:** resume position (save on pause, seek, item change, and every 5s while
playing; same thresholds: save after 5s or past 1/4, count as finished within 60s of the end and
past 3/4), watch history, speed and pitch with semitones, skip silence, repeat, shuffle, autoplay
next (related) with the Wi-Fi rule, quality and audio track switching without losing position,
captions, seek previews (storyboards from `getPreviewFrames()`), chapters, live edge.

**New:** sleep timer, PiP actions, long-press 2x, Shorts player pool, per-item "Not interested".

**Video surface:** `media3-ui-compose` `PlayerSurface` (SurfaceView type) inside
`movableContentOf`, so the same surface moves between the inline player, the mini player and
fullscreen without a black flash. That replaces today's `addView` re-parenting hack.

**Dropped:** `PlayerUi` plug-in system, broadcast-intent event bus, `SerializedCache` hand-off,
`PlayerHolder`, `PlayQueueActivity`, the WindowManager popup, external players, Kodi,
`Runtime.halt(0)` on task removal.

### 5.6 Background work (`:core:work`)
- `FeedRefreshWorker` (CoroutineWorker, foreground with type dataSync): port `FeedLoadManager`.
  Keep 3 channels in parallel, the YouTube throttle (random sleep every 50 extractions), the
  refresh threshold, and fast mode via the RSS `FeedExtractor`. Add the Shorts tab to what gets
  fetched by default so the Shorts tab has content.
- `NewUploadsWorker` (periodic): port `NotificationWorker` from RxWorker to CoroutineWorker. Same
  grouped notifications.
- `SubscriptionImportWorker` / `SubscriptionExportWorker`: already coroutine-based, move as is.
- Replace `FeedLoadService` (a foreground Service) with the worker above.

### 5.7 Errors
- One `AppError` model ported from `ErrorInfo`: network, reCAPTCHA, age restricted, unavailable,
  private, geo-blocked, 403/IP ban (links to the existing FAQ URL), extractor parse error.
- **reCAPTCHA:** a Compose screen hosting the WebView, ported from `ReCaptchaActivity`. Saves the
  cookie into `DownloaderImpl`.
- **No crash reporting service.** Uncaught exceptions write a local log. A debug-visible
  "Last crash" screen in Developer settings shows and shares it. ACRA, the email sender and GitHub
  issue links are dropped.

### 5.8 Build and tooling
- minSdk: decision D4 (recommend 29). targetSdk and compileSdk: the latest stable API at kickoff.
- Build types: `debug` (suffix `.debug`) and `release` (minified, signed with Jordan's own key).
  Drop `continuous` and the branch-name suffixes.
- Keep ktlint, but run it in CI and as a manual task, not on every debug build (remove the
  `preDebugBuild` hook). Drop checkstyle (no Java left) and Sonar.
- Keep `checkDependenciesOrder`.
- Add a **baseline profile** module (`androidx.baselineprofile`) for start-up and feed scrolling.
- CI: one GitHub Actions workflow running build, unit tests and lint on push. Drop the backport,
  labeler, no-response and image-minimizer workflows.

---

## 6. Phase 0: strip to YouTube (removal list)

Done inside the legacy app so there's still a working app at every step. Each bullet is one
commit. Paths are relative to `app/src/main/java/org/schabi/newpipe/` unless shown in full.

### 6.1 Identity
- New app name and launcher icon (decision D1).
- New `applicationId` and namespace in `buildSrc/src/main/kotlin/ProjectConfig.kt` so it installs
  next to NewPipe. Leave the Java package `org.schabi.newpipe` alone in Phase 0. The new modules get
  new packages anyway.
- Reset `versionCode` and `versionName` (for example 1 / 0.1.0).
- Rewrite `README.md` for the fork: what it is, GPL notice, credit to NewPipe, no NewPipe branding.

### 6.2 Other services
- **Delete:** `util/PeertubeHelper.kt`, `settings/PeertubeInstanceListFragment.java`, layouts
  `fragment_instance_list.xml`, `item_instance.xml`, `instance_spinner_item.xml`,
  `instance_spinner_layout.xml`, drawables `ic_placeholder_peertube.xml`,
  `ic_placeholder_media_ccc.xml`, `ic_placeholder_bandcamp.xml`,
  `progress_soundcloud_horizontal_{dark,light}.xml`.
- **Manifest:** remove the SoundCloud, media.ccc.de, PeerTube (15 hosts) and Bandcamp intent
  filters from `RouterActivity`. Fix the `inv.nadeko.net/` host. Remove the stale
  `.about.AboutActivity` entry.
- **MainActivity:** remove the drawer service switcher (`showServices`, `enhancePeertubeMenu`,
  `toggleServices`, `changeService`, the `menu_services_group` handling, the header service
  icon/name) and the drawer Donate item.
- **Service branches:** `SearchFragment` (PeerTube Sepia search), `KioskFragment` (MediaCCC empty
  state), `BaseListInfoFragment` (SoundCloud empty state), `VideoDetailFragment` (Bandcamp fan-page
  error), `error/ErrorInfo.kt` (SoundCloud Go+), `util/text/TimestampLongPressClickableSpan.kt`,
  `util/external_communication/KoreUtils.java`, `util/ThemeHelper.java` service styles.
- **`util/ServiceHelper.kt`:** keep only YouTube: icon, import instructions, filter strings,
  1-hour cache. Pin the selected service to YouTube and drop the `current_service_key` preference.
- **`settings/migration/SettingMigrations.java`:** make MIGRATION_6_7 a no-op body, but keep the
  entry so version numbers stay intact. Add MIGRATION_9_10 (section 7.2).
- **Resources:** keep only the YouTube colours and styles in `colors_services.xml` and
  `styles_services.xml`. Remove the PeerTube preference screen from `xml/content_settings.xml`,
  the PeerTube keys, `peertube_instance_list_url`, SoundCloud import strings, `soundcloud_go_plus_content`,
  `migration_info_6_7_*`, the tracks/users/events/conferences filter strings, `channel_tab_tracks`,
  and the TRACKS and LIKES channel tab keys.
- **Selecting a kiosk** (`SelectKioskFragment`, `ChooseTabsFragment`): YouTube kiosks only, no
  service names.
- **Subscriptions import menu:** one entry, YouTube (Google Takeout), plus "Previous export".
- **`shared/.../theme/ServiceTheme.kt`** and `ServiceThemeTest.kt`: YouTube only. (Deleted later
  anyway.)
- **Tests:** in `DatabaseMigrationTest.kt` replace `ServiceList.SoundCloud.serviceId` with the
  literal `1`.

### 6.3 Platform targets
- Delete `desktopApp/`, `iosApp/`, the `jvm()` and iOS targets plus `iosMain`/`jvmMain` in
  `shared/build.gradle.kts`, and `include(":desktopApp")` in `settings.gradle.kts`.
- Remove the Leanback launcher category and TV banner from the manifest (decision D8).

### 6.4 Features a personal build doesn't need
- **Update checker:** `NewVersionWorker.kt`, `util/ReleaseVersionUtil.kt`,
  `settings/UpdateSettingsFragment.java`, `xml/update_settings.xml`, the app-update notification
  channel, the consent dialog in `MainActivity`, `NewVersionManagerTest.kt`.
- **ACRA:** dependency, `error/AcraReportSender*.java`, `initACRA` in `App.kt`, the report-by-email
  and GitHub buttons in `ErrorActivity`.
- **Donate / NewPipe links:** drawer item, `donottranslate.xml` URLs, `shared` About link cards,
  `.github/FUNDING.yml`, the QR codes in `assets/`.
- **Kodi/Kore**, **external players**, **PanicResponderActivity**, the "Keep Android Open" and
  API-23 deprecation dialogs.
- **Stetho** (abandoned). LeakCanary stays in debug.
- **fastlane/** (21 MB), **doc/** (translated READMEs), the `assets/` design SVGs, Weblate badge.
- **Translations:** decision D5 (recommend English only). Delete `values-*` language folders in
  `app/src/main/res` and `shared/src/commonMain/composeResources`. Keep `values-night`,
  `values-land`, `values-sw600dp*`, `values-v*`, `values-w820dp`.
- **Sonar** plugin, config and CI job; **checkstyle** (keep until the Java is gone, then delete).
- `.github`: keep a trimmed `ci.yml`. Drop the rest.

### 6.5 Phase 0 acceptance
- `./gradlew assembleDebug testDebugUnitTest` passes.
- The app installs next to NewPipe, plays a YouTube video, a live stream and a Short, refreshes the
  feed, and imports a NewPipe backup zip that contains SoundCloud rows without crashing (they get
  removed by migration 9 → 10).
- `git grep -i -E "soundcloud|peertube|bandcamp|media\.?ccc"` returns nothing outside the extractor
  dependency, migration history and tests.

---

## 7. Data migration

### 7.1 Room 9 → 10
In one transaction:
1. `DELETE FROM subscriptions WHERE service_id != 0` (cascades to `feed`,
   `feed_group_subscription_join`, `feed_last_updated`).
2. `DELETE FROM remote_playlists WHERE service_id != 0`.
3. `DELETE FROM search_history WHERE service_id != 0`.
4. `UPDATE playlists SET thumbnail_stream_id = -1, is_thumbnail_permanent = 0 WHERE
   thumbnail_stream_id IN (SELECT uid FROM streams WHERE service_id != 0)`.
5. `DELETE FROM streams WHERE service_id != 0` (cascades to `stream_history`, `stream_state`,
   `playlist_stream_join`).
6. Re-number `playlist_stream_join.join_index` per playlist so there are no gaps (rebuild via a
   temp table ordered by the old `join_index`).

Tests: a v9 fixture with mixed-service rows in every table, migrate, check that only service 0
rows remain, playlists are gap-free, thumbnails point at existing streams, and foreign keys pass
`PRAGMA foreign_key_check`.

### 7.2 Preferences 9 → 10
- Rewrite `saved_tabs_key`: drop KIOSK, CHANNEL and PLAYLIST tabs with a non-zero service id.
  (Main tabs disappear entirely in the new UI, but the migration keeps Phase 0 working.)
- Remove `current_service_key` and the PeerTube keys.
- Remove update-checker keys.

### 7.3 Bringing over Jordan's NewPipe data
- In NewPipe: Settings → Backup and restore → Export database → zip.
- In the new app: Settings → Backup → Import → pick the zip. The existing import path restores
  `newpipe.db` and `preferences.json`, restarts, and Room runs 9 → 10.
- Add a guard: refuse a zip whose DB version is **newer** than the app's (today there is no version
  check).
- Drop reading the legacy Java-serialized `newpipe.settings` (marked deprecated and unsafe in the
  code); only `preferences.json` is read.

---

## 8. Feature matrix

| Feature | Today | New app |
|---|---|---|
| Five services | yes | **YouTube only** |
| Drawer + customisable main tabs | yes | **Replaced** by bottom nav (Home, Shorts, Subscriptions, You) |
| Kiosks (Live, Music, Gaming, Movies, Podcasts) | drawer/tabs | **Home chips** |
| Subscriptions + feed groups + fast feed | yes | Keep, new UI (channel strip, group chips) |
| New-upload notifications | yes | Keep + **in-app Notifications page** |
| Watch history, stats, resume | yes | Keep |
| Local playlists, bookmarks | yes | Keep + **Watch later, Liked videos** |
| Search with history, suggestions, YT Music filters | yes | Keep + voice search |
| Comments + replies | yes | Keep (sheet) |
| Chapters | panel in player | Keep (seekbar segments + sheet) |
| Main / background player | yes | Keep (Media3) |
| Popup player | yes | **Drop** for PiP (D3) |
| PiP | no | **New** |
| Mini player | bottom sheet fragment | Rebuilt, YouTube style |
| Shorts | channel tab only | **New** vertical player + Shorts tab |
| Speed, pitch, skip silence | yes | Keep |
| Long-press 2x | no | **New** |
| Sleep timer | no | **New** |
| Ambient mode | no | **New** |
| Queue | panel + PlayQueueActivity | Sheet |
| Android Auto | yes | Keep (D7) |
| Notification slot customisation | yes | **Drop** (D6) |
| Downloads | yes | D2 |
| Kodi, external players | yes | Drop |
| Update checker, ACRA, donate | yes | Drop |
| TV / Leanback | partial | Drop (D8) |
| Translations | ~110 languages | English (D5) |
| iOS, desktop | About screen only | Drop |
| Later (not in v1) | | SponsorBlock segments (skip/mark), Return YouTube Dislike counts, local recommendations tuning, DeArrow titles |

---

## 9. Delivery plan

Each phase ends with something installable on Jordan's phone. The phases are ordered so that the
riskiest work (the player port) happens before most of the screen work.

### Phase 0: strip (section 6)
**Done when:** section 6.5 passes.

### Phase 1: foundation
- New Gradle modules from section 5.1, empty features with placeholder screens.
- `:app` with one `ComponentActivity`, edge to edge, `MaterialExpressiveTheme`, the adaptive
  scaffold (bottom bar / rail), per-tab back stacks with Navigation 3.
- `:core:designsystem`: theme (dynamic colour, fallback scheme, pure black, typography, shapes,
  motion), video card, shelf, chip row, empty, error and loading states, with previews and
  screenshot tests.
- `:core:extractor`, `:core:database` (v10 migration + Flow DAOs), `:core:data` repositories with
  unit tests.
- The legacy app keeps running unchanged as a separate entry while this happens (the new Activity
  is the launcher only in debug at first).

**Done when:** the new shell runs, the Home "Subscriptions" chip shows feed items from the real DB,
and repository tests pass.

### Phase 2: player
- `:core:player` per section 5.5, with the `YoutubeHttpDataSource` golden test.
- Watch layer: inline player, controls, gestures, fullscreen, mini player with the movable surface,
  background play, PiP, media notification, sleep timer, settings sheet.
- Resume positions and history writes.

**Done when** all of these play correctly on Jordan's phone, with no black flash when moving
between inline, mini, fullscreen and PiP:
- a normal video (720p and 1080p)
- a video with separate audio tracks (dubbed)
- a live stream, including going to the live edge
- a post-live DVR stream
- a Short
- a video with captions
- a playlist queue of 20 items with shuffle

### Phase 3: screens
Order: Watch details (description, comments, chapters, queue, related) → Subscriptions → Search →
Channel → Playlist → You (history, playlists, Watch later, Liked) → Home → Shorts → Notifications →
Settings → Router.

**Done when:** every row in section 8 marked Keep or New works in the new app, and the legacy app is
no longer needed day to day.

### Phase 4: cutover
- Delete legacy `app/` code, `shared/`, RxJava, ExoPlayer 2, Groupie, Markwon (if descriptions use
  the new linkifier), evernote state-saver, Bridge, checkstyle, kapt.
- Rename modules so `:app` is the new app.
- Migrate the debug and release `applicationId` so data carries over (or do one last backup/import).

**Done when:** `./gradlew build` has no Java sources, no `io.reactivex` and no `com.google.android.exoplayer2`
imports, and a release build runs on Jordan's phone.

### Phase 5: polish
Baseline profile, tablet/foldable layouts, ambient mode, content-based heroes, local
recommendations (Home "All"), and any of the "Later" items Jordan picks.

---

## 10. Testing and quality
- **Unit (JVM):** mappers (extractor fixtures to models), repositories (fake extractor and in-memory
  Room), player stream selection (`ListHelper` rules), resume-position thresholds, feed throttle,
  migration SQL, settings migrations, backup import with old and new zips.
- **Instrumented:** Room migration tests (v2 to v10), DAO tests (ported), one Media3 playback test
  per stream type behind a flag (needs network, run on demand, not in CI).
- **Compose UI tests** for Search, Subscriptions, Watch controls and the mini player gestures.
- **Screenshot tests** (Roborazzi, free, runs on the JVM) for design-system components in light,
  dark, pure black, 200% font scale.
- **Performance:** cold start to first Home frame under 800ms on Jordan's phone with the baseline
  profile. Feed scroll has no dropped frames in a macrobenchmark at 100 items. Player start (tap to
  first frame) under 1.5s on Wi-Fi for a cached `StreamInfo`.
- **Manual check before each phase sign-off:** TalkBack pass on the screens touched, rotation,
  process death (state comes back), airplane mode (errors make sense).

---

## 11. Risks

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| R1 | Once rewritten, upstream NewPipe app fixes can't be merged. YouTube changes often need app-side fixes too (PoToken, `YoutubeHttpDataSource`, as in commit `b4003d641`). | Playback breaks until Jordan ports the fix by hand | Keep those files thin and close to upstream. Watch NewPipe's `dev` branch for changes to `util/potoken/` and `player/datasource/`. Extractor fixes still come free by bumping the commit. |
| R2 | YouTube rate limits or blocks the IP (403s, "sign in to confirm you're not a bot"). More requests (Home recommendations, Shorts tab fetch, refreshing related) make it more likely. | Playback and feed fail for hours | Keep the feed throttle, cache `StreamInfo`, fetch recommendations lazily and rarely, show a clear 403 message, keep the reCAPTCHA flow. |
| R3 | Media3 port changes playback behaviour (the custom DataSource, manifest creators, live edge). | Videos stall or fail | Golden request tests, the Phase 2 stream-type checklist, and the legacy app kept installed until Phase 2 passes. |
| R4 | Local recommendations are unproven: quality is unknown, and they cost extra requests. | Home "All" feels empty or repetitive | Ship v1 without them (subscriptions + continue watching + Shorts). Add them in Phase 5 behind a toggle, and judge them by use. |
| R5 | The extractor has no global Shorts feed, so the Shorts tab only has Shorts from subscribed channels. | Shorts tab is thin with few subscriptions | Say so in the empty state. Include Shorts from search and related. Accept the limit for v1. |
| R6 | Expressive APIs are experimental (`@ExperimentalMaterial3ExpressiveApi`) on the 1.5 alpha line and may change. | Build breaks when bumping | Pin the alpha. Wrap Expressive components in `:core:designsystem` so changes land in one module. |
| R7 | A rewrite this size stalls halfway, leaving two apps. | Wasted work | Phase order puts the player (riskiest) first. Each phase ships. The legacy app stays usable until cutover. |
| R8 | Effort. A single developer, with AI help, should expect roughly: Phase 0 about 1 week, Phase 1 about 2 weeks, Phase 2 about 3 to 4 weeks, Phase 3 about 5 to 6 weeks, Phase 4 about 1 week, Phase 5 open-ended. These are estimates, not measured. | Plans slip | Re-plan after Phase 2 using the real pace. |

---

## 12. Decisions needed from Jordan

**Resolved 2026-10-03:** D2 keep downloads (existing engine stays), D5 English only. Applied by
choosing the recommendation without asking: D3 popup removed (PiP later), D6 slot customisation
kept for now, D7 Android Auto kept, D8 TV not pursued. Still open: D1 name and `applicationId`,
D4 minSdk, D9, D10.

| # | Decision | Recommendation |
|---|---|---|
| D1 | App name and `applicationId` | Pick a name with no "Pipe" in it. `applicationId` under a domain or handle you control. |
| D2 | Downloads: (a) drop, (b) keep the existing engine behind a Kotlin interface with a new Compose UI, (c) rewrite on WorkManager + Media3 Transformer | **(b)** if you download at all. It works, and it's 12k lines you don't have to write. (a) if you only stream: biggest single cut. |
| D3 | Popup (floating window) player | **Drop**, use PiP. PiP is the platform standard, needs no overlay permission, and is a fraction of the code. |
| D4 | minSdk | **29** (Android 10). Simpler storage, system dark theme, gesture nav. Use 31 if your devices are all Android 12+, which gives dynamic colour everywhere. |
| D5 | Translations | **English only.** Cuts ~110 folders and most of the service strings. |
| D6 | Notification action slot customisation | **Drop.** Use Media3's standard layout plus Loop, Shuffle, Close. |
| D7 | Android Auto | **Keep.** Close to free with `MediaLibraryService`. |
| D8 | Android TV / Leanback | **Drop.** A real TV UI is a separate project. |
| D9 | Local Like / Watch later / Liked videos | **Yes.** Fills the action row with something useful without an account. |
| D10 | SponsorBlock and Return YouTube Dislike | **Later** (Phase 5). Both are free public APIs that send video IDs to third parties; decide then. |

---

## Appendix A: where today's logic lives (for porting)

| Logic | Current file(s) |
|---|---|
| App start, extractor init, PoToken registration | `App.kt` |
| HTTP for the extractor, cookies | `DownloaderImpl.java` |
| PoToken | `util/potoken/*` |
| Stream selection | `util/ListHelper.java` |
| YouTube media sources | `player/resolver/PlaybackResolver.java`, `VideoPlaybackResolver.java`, `AudioPlaybackResolver.java`, `player/helper/PlayerDataSource.java`, `player/helper/YoutubeDashLiveManifestParser.java` |
| YouTube HTTP data source | `player/datasource/YoutubeHttpDataSource.java` |
| Seek previews | `player/seekbarpreview/*` |
| Speed and pitch | `player/helper/PlaybackParameterDialog.java`, `PlayerSemitoneHelper` |
| Double-tap overlay | `views/player/*`, `player/gesture/*` |
| Resume and history | `local/history/HistoryRecordManager.java`, `Player.java` progress loop |
| Feed loading | `local/feed/service/FeedLoadManager.kt`, `local/feed/FeedDatabaseManager.kt` |
| New-upload notifications | `local/feed/notifications/*` |
| Subscriptions | `local/subscription/SubscriptionManager.kt`, `workers/*`, `services/ImportExportJsonHelper.kt` |
| Playlists | `local/playlist/LocalPlaylistManager.java`, `RemotePlaylistManager.kt`, `ExportPlaylist.kt` |
| Backup | `settings/export/ImportExportManager.kt`, `BackupFileLocator.kt`, `util/ZipHelper.java`, `streams/io/*` |
| Text links, timestamps, hashtags | `util/text/*` |
| Errors, reCAPTCHA | `error/ErrorInfo.kt`, `error/ReCaptchaActivity.java` |
| Kiosk names | `util/KioskTranslator.kt` |
| Channel tabs | `util/ChannelTabHelper.java` |
| Downloads | `us/shandian/giga/*`, `download/*`, `streams/*` |
| Settings keys | `res/values/settings_keys.xml`, `res/xml/*_settings.xml` |
| Theme ideas to salvage | `shared/src/commonMain/kotlin/net/newpipe/app/theme/*`, `composable/*Preference.kt` |

## Appendix B: target libraries (verify exact versions at kickoff)

| Library | Version line | Note |
|---|---|---|
| Kotlin / AGP / JDK | 2.4.x / 9.3.x / 21 | as today |
| Compose BOM + material3 | material3 1.5.0 alpha | Expressive APIs |
| material3-adaptive, navigation-suite | matching | adaptive layout |
| AndroidX Navigation 3 | 1.1.x | replaces the JetBrains artifact |
| Media3 (exoplayer, dash, hls, session, ui-compose, datasource-okhttp) | 1.10.x | replaces ExoPlayer 2.19.1 |
| Room | 2.8.x | drop room-rxjava3 |
| WorkManager | 2.11.x | drop work-rxjava3 |
| Koin (+ annotations, compiler plugin) | 4.2.x | as in `shared` today |
| Coil 3 | 3.5.x | as today |
| OkHttp | 5.x | as today |
| kotlinx.serialization, coroutines | current | |
| NewPipeExtractor | pinned commit | bump when YouTube breaks |
| Roborazzi, baseline profile | current | testing and performance |
| Removed | | RxJava, RxAndroid, RxBinding, ExoPlayer 2, Groupie, evernote state-saver, Bridge, ACRA, Stetho, checkstyle, Sonar, Markwon (if replaced), NoNonsense FilePicker (SAF only) |
