# YT Client (working title)

A personal, YouTube-only Android client. It is a fork of [NewPipe](https://github.com/TeamNewPipe/NewPipe)
that is being stripped down to YouTube and then given a new Jetpack Compose UI built on Material 3
Expressive.

This is a personal project. It is not affiliated with NewPipe, TeamNewPipe or NewPipe e.V., it is not
published anywhere, and it does not use the NewPipe name or branding. "NewPipe" is a registered word
mark of its owners, so the working title above is a placeholder until a final name is chosen.

## What it is

- A lightweight YouTube front-end: search, watch, background play, subscriptions without an account,
  local history and playlists.
- Built on the legacy NewPipe app (Views, Fragments, RxJava, ExoPlayer 2) with a small Compose module
  in `shared/`. The plan is to rewrite the UI and player in phases.
- YouTube only. SoundCloud, PeerTube, media.ccc.de and Bandcamp support is being removed.
- No update checker, no crash reporting service, no donation links, no external reporting. Errors are
  shown locally and can be copied or shared by hand.

The full plan, scope and phases are in [docs/spec/youtube-client-spec.md](docs/spec/youtube-client-spec.md).

## Status

Phase 0 (strip to YouTube) is in progress on the `youtube-only` branch. Expect an app that does not
build or run correctly at every commit while the removal work is going on.

## Building

Requires JDK 21 and the Android SDK.

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Code style is checked with ktlint on demand: `./gradlew :app:runKtlint` and `./gradlew :app:formatKtlint`.

## Credits

- [NewPipe](https://github.com/TeamNewPipe/NewPipe) by the NewPipe contributors, which this project
  is forked from.
- [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), which does all the YouTube
  parsing.
- The other open-source libraries used are listed on the About screen inside the app.

## License

This project is free software, licensed under the GNU General Public License, version 3 or (at your
option) any later version. See [LICENSE](LICENSE).

Copyright is held by the NewPipe contributors and by the authors of this fork, as recorded in the
file headers and the git history. This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.

Source: https://github.com/jordan-jakisa/NewPipe
