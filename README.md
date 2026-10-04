# YouFlow

A free, private YouTube client for Android with a Material 3 Expressive interface. YouFlow is a
fork of [NewPipe](https://github.com/TeamNewPipe/NewPipe): the YouTube parsing comes from
[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), the screens and the player are
new.

YouFlow is not affiliated with NewPipe, TeamNewPipe or NewPipe e.V. and does not use their name or
branding.

## Features

- Home with a local "For you" feed built on the device from your history, likes and subscriptions
- Shorts feed with preloading, likes, saving and sharing
- Search, channels, playlists, comments, chapters, captions
- Subscriptions without an account, feed groups, import and export (NewPipe backups work)
- Background play, picture in picture, mini player, audio-only mode, sleep timer, queue
- Downloads
- Local history, watch later and playlists
- A reorderable bottom bar, with Shorts optional
- No Google services, no ads, no tracking, no account. Nothing leaves the device except the requests
  YouTube itself needs.

Android 12 (API 31) or newer. English only.

## Building

Needs JDK 17 or newer and the Android SDK. NewPipeExtractor is a git submodule pinned to one commit,
so clone with submodules:

```
git clone --recurse-submodules https://github.com/jordan-jakisa/NewPipe
cd NewPipe
./gradlew assembleRelease     # unsigned APK in app/build/outputs/apk/release
./gradlew testDebugUnitTest
```

Style is checked with ktlint: `./gradlew :app:runKtlint` and `./gradlew :app:formatKtlint`.

The design and scope are written up in [docs/spec/youtube-client-spec.md](docs/spec/youtube-client-spec.md).
Notes for publishing on F-Droid are in [docs/fdroid/README.md](docs/fdroid/README.md).

## Credits

- [NewPipe](https://github.com/TeamNewPipe/NewPipe) by the NewPipe contributors, which this project
  is forked from.
- [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), which does all the YouTube
  parsing.
- AndroidX, Jetpack Compose, Media3, Room, OkHttp, Coil, RxJava and the other open-source libraries
  listed in `gradle/libs.versions.toml`.

## License

Free software under the GNU General Public License, version 3 or (at your option) any later
version. See [LICENSE](LICENSE). Copyright is held by the NewPipe contributors and by the authors of
this fork, as recorded in the file headers and the git history. This program is distributed in the
hope that it will be useful, but WITHOUT ANY WARRANTY, without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
