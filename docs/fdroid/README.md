# Publishing YouFlow on F-Droid

Status: the repository is prepared; nothing has been submitted.

## What is already in place

- GPL-3.0-or-later `LICENSE`, NewPipe credited, no NewPipe name or logo.
- No Google Play services, Firebase or other proprietary libraries in the release classpath
  (checked with `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`). LeakCanary is
  debug only.
- No binary blobs in the source tree.
- NewPipeExtractor is a git submodule pinned to the commit in `gradle/libs.versions.toml`, so
  F-Droid can build it from source (`submodules: true` in the recipe).
- `dependenciesInfo` is switched off so the APK has no Google-signed metadata block.
- `fastlane/metadata/android/en-US` has the title, descriptions and changelog F-Droid shows.
- `./gradlew assembleRelease` builds an unsigned, R8-minified APK; it launches without crashes.

## Still to do (needs the owner)

1. Push the work to GitHub, including the submodule, and tag a release (`v0.1.0`).
2. Add `fastlane/metadata/android/en-US/images/icon.png` (512x512) and a few phone screenshots in
   `images/phoneScreenshots/`.
3. Submit `dev.jordanempire.youflow.yml` (this folder) as a merge request to fdroiddata, or open a
   "Request for packaging" issue. Their checklist: https://f-droid.org/docs/Inclusion_Policy/
4. Expect the `NonFreeNet` anti-feature, because the app only works with YouTube. NewPipe and other
   YouTube clients are listed with it.
5. F-Droid signs the APK with its own key. Users who install a build signed with a different key
   (for example a debug build) cannot update over it and must reinstall.

## Risks

- YouTube changes break the extractor from time to time; updating means bumping the submodule commit
  and `teamnewpipe-newpipe-extractor` in `gradle/libs.versions.toml` together.
- The Compose Material 3 alpha (`1.11.0-alpha07`) is a pre-release library; F-Droid builds it from
  Maven Central like any other dependency, but it may need pinning to a stable version later.
