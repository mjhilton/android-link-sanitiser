# Link Cleaner

A tiny Android app that registers itself as a share target for links. Share
a link to it (from a browser, Twitter/X, etc.) and it strips tracking
parameters (UTM tags, `fbclid`, `gclid`, and similar) then immediately
re-opens the share sheet with the cleaned link so you can pick the real
destination.

The app itself has almost no UI: a launcher screen shows a running count of
links cleaned and a handful of tracking-parameter settings. (Package/class
names still say "linksanitiser" — that's deliberate, so existing installs
upgrade in place rather than becoming a separate app when the display name
changed.)

## Share targets

The app shows up twice in Android's share sheet:

- **Quick** — `ShareReceiverActivity`. Not configurable at all: applies the
  default tracking-parameter rules, strips surrounding text, and keeps only
  the first link if there's more than one. No UI, ever - that's the point.
- **Custom** — `CustomShareActivity`. Starts from the same defaults as Quick
  (strip surrounding text, first link only) but shows a small form letting
  you override any of that just for this share, with a live preview of the
  result. If the share has more than one link, it also shows a checkbox per
  link (first one pre-checked) to pick which survive - no separate picker
  screen, it's inline. Nothing changed here is saved.

## How it works

- `LinkSanitiser` (in `LinkSanitiser.kt`) is the actual link-finding and
  cleaning logic. It's plain Kotlin with no Android dependencies, so it's
  covered by JVM unit tests in `app/src/test/`. `findLinks()` locates and
  cleans each URL in a shared text without yet deciding which survive;
  `buildResult()` then keeps only the given link indices - as plain text
  cleaned in place, or (with `cleanSurroundingText`) as just those links,
  newline separated. `sanitise()` is a convenience wrapper over both for the
  simple "clean everything in place" case.
- `Resharing.kt` holds the logic shared by both activities: reading the
  incoming share and re-opening the share sheet with the final text
  (excluding our own share targets so you don't loop back into them).
- `MainActivity` shows the cleaned-link counter and tracking-parameter
  settings, backed by `Prefs` (`SharedPreferences`). Which links to keep and
  whether to clean surrounding text are decided per-share in Custom, not
  configured here.

## Settings (on the main screen - apply to both share targets)

- Strip UTM parameters (`utm_source`, `utm_campaign`, etc.) — on by default.
- Strip ad/click IDs (`fbclid`, `gclid`, `igshid`, etc.) — on by default.
- Strip referral parameters (`ref`, `source`, etc.) — off by default, since
  these occasionally affect the destination page rather than just tracking.
- Custom comma-separated parameter names to always strip.
- Toggle for a confirmation toast on each share.

## Building

This project has no Android SDK bundled, and needs `compileSdk` 35 with
build-tools 35.0.0 (see `app/build.gradle.kts`). `targetSdk` is
deliberately 34, not 35 — see the comment next to it in
`app/build.gradle.kts` for why.

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

Builds cleanly and all 29 `LinkSanitiserTest` unit tests pass
(`./gradlew test assembleDebug`), verified with the Android SDK
(compileSdk/build-tools 35). The manifest, package name, launcher
activity, and both share targets' `ACTION_SEND` intent filters were
confirmed present in the built APK via `aapt dump`.

Not yet verified on a real device: the Custom form's live preview and
inline link picker are new this round and haven't been through an
actual touchscreen/share-sheet test yet, only unit tests of the
underlying logic and a static check of the built APK's manifest.
