# On-device test checklist

Install: `./gradlew :app:installDebug` (or `adb install -r app/build/outputs/apk/debug/app-debug.apk`).
The debug app id is `dev.jordanempire.youflow.debug.youtubeonly`.

## Already verified on a Pixel 8a
- Home loads Live / music / gaming lists, cards, pull to refresh
- Watch page plays, controls fit the inline player, quality, speed, captions sheet
- Comments teaser and sheet, mini player

## To verify (built, never run on a device)
1. Fullscreen: button, landscape, back exits; left half swipe = brightness, right half = volume
2. Picture in picture: press Home while a video plays on the watch page; play/pause button in the PiP window
3. Background audio: lock the screen; notification controls; next/previous
4. Resume: watch 1 minute, leave, reopen the same video, it should continue; Home "Continue watching" shelf
5. Shorts tab: swipe up/down, tap to pause, loops
6. Save to playlist (card menu or watch page), Watch later, You > Playlists, rename/remove/delete
7. Search: filter chips, scrolling for more, "Did you mean"
8. Channel: Subscribe, bell, tabs, Play all; Subscriptions tab strip and feed (pull to refresh)
9. Subscriptions menu: import Google Takeout, import NewPipe export, export
10. Sleep timer (player gear), chapters label on the seek bar, tappable timestamps in descriptions
11. Settings: theme (system / light / dark / pure black), dynamic colour, autoplay, clear history
12. Open a youtube.com / youtu.be link from another app: should open YouFlow (and plain shared text becomes a search)
13. Download button on the watch page, You > Downloads
14. If YouTube shows "Quick check needed": tap Verify, solve, it retries

## Known gaps
- Mini player shows a thumbnail, not live video
- Feed groups, backup/restore and downloads list are still the classic screens (You > Settings > Classic settings)
- App icon is still NewPipe's
