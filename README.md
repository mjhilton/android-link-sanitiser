# Link Sanitiser

A tiny Android app that registers itself as a share target for links. Share
a link to it (from a browser, Twitter/X, etc.) and it strips tracking
parameters (UTM tags, `fbclid`, `gclid`, and similar) then immediately
re-opens the share sheet with the cleaned link so you can pick the real
destination.

The app itself has almost no UI: a launcher screen shows a running count of
links cleaned and a handful of settings.

## How it works

- `ShareReceiverActivity` is an invisible activity registered for
  `ACTION_SEND` / `text/plain`. It finds URLs in the shared text, strips
  known tracking query parameters, and calls `Intent.createChooser` again
  with the cleaned text — excluding itself from the resulting chooser so
  you don't loop back into it.
- `LinkSanitiser` (in `LinkSanitiser.kt`) is the actual stripping logic. It's
  plain Kotlin with no Android dependencies, so it's covered by JVM unit
  tests in `app/src/test/`.
- `MainActivity` shows the cleaned-link counter and settings, backed by
  `Prefs` (`SharedPreferences`).

## Settings

- Strip UTM parameters (`utm_source`, `utm_campaign`, etc.) — on by default.
- Strip ad/click IDs (`fbclid`, `gclid`, `igshid`, etc.) — on by default.
- Strip referral parameters (`ref`, `source`, etc.) — off by default, since
  these occasionally affect the destination page rather than just tracking.
- Custom comma-separated parameter names to always strip.
- Toggle for a confirmation toast on each share.

## Building

This project has no Android SDK bundled, and needs `compileSdk`/`targetSdk`
35 with build-tools 35.0.0 (see `app/build.gradle.kts`).

**Dev container (recommended if you don't have the SDK installed locally):**
Open the repo in VS Code with the Dev Containers extension (or GitHub
Codespaces) and it will build a container with the JDK and Android SDK
already installed — see `.devcontainer/`. First build takes a few minutes
while it downloads the SDK components.

**Local machine with Android Studio:** open the project as-is; Android
Studio will offer to install any missing SDK components.

**Command line, with `ANDROID_HOME` already set:**

```
./gradlew assembleDebug
./gradlew test          # unit tests for LinkSanitiser
```

## Installing

```
./gradlew installDebug
```

or sideload `app/build/outputs/apk/debug/app-debug.apk`, then set it (or
just leave it available) as an option in Android's share sheet.

## Status

Builds cleanly and all 15 `LinkSanitiserTest` unit tests pass
(`./gradlew test assembleDebug`), verified with the Android SDK
(compileSdk/build-tools 35). The manifest, package name, launcher
activity, and the `ShareReceiverActivity` `ACTION_SEND` intent filter
were all confirmed present in the built APK via `aapt dump`.

Not yet verified: an actual on-device/emulator run through the Android
share sheet — install the APK and try sharing a link from a browser or
another app to confirm the end-to-end flow (chooser reopens, link is
cleaned, counter increments) before relying on it day to day.
