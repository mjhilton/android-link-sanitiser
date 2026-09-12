# Link Cleaner

A tiny Android app that registers itself as a share target for links. Share
a link to it (from a browser, Twitter/X, etc.) and it strips tracking
parameters (UTM tags, `fbclid`, `gclid`, and similar) then immediately
re-opens the share sheet with the cleaned link so you can pick the real
destination.

The app itself has almost no UI: a launcher screen shows a running count of
links cleaned and a handful of settings. (Package/class names still say
"linksanitiser" — that's deliberate, so existing installs upgrade in place
rather than becoming a separate app when the display name changed.)

## Share targets

The app shows up twice in Android's share sheet:

- **Quick** — `ShareReceiverActivity`. Applies your saved default settings
  immediately, no extra taps. If those defaults happen to be "Choose" links
  to keep and there's more than one link, it still needs the small picker
  below — there's no way to choose without *some* UI — but otherwise it's a
  single invisible hop back into a fresh share sheet.
- **Advanced** — `AdvancedShareActivity`. Shows a small floating form,
  pre-filled from your saved defaults, letting you override any setting for
  that one share only. Nothing you change here is saved.

Both funnel into the same cleaning logic and, if needed, the same link
picker (`LinkPickerActivity`, internal-only — not itself a share target).

## How it works

- `LinkSanitiser` (in `LinkSanitiser.kt`) is the actual link-finding and
  cleaning logic. It's plain Kotlin with no Android dependencies, so it's
  covered by JVM unit tests in `app/src/test/`. `findLinks()` locates and
  cleans each URL in a shared text without yet deciding which survive;
  `buildResult()` then applies "Links to keep" and "Clean surrounding text"
  to produce the final shared text. `sanitise()` is a convenience wrapper
  over both for the common "clean everything in place" case.
- `Resharing.kt` holds the logic shared by all three activities: reading the
  incoming share, deciding whether the link picker is needed, and re-opening
  the share sheet with the final text (excluding our own share targets so
  you don't loop back into them).
- `MainActivity` shows the cleaned-link counter and settings, backed by
  `Prefs` (`SharedPreferences`).

## Settings

- Strip UTM parameters (`utm_source`, `utm_campaign`, etc.) — on by default.
- Strip ad/click IDs (`fbclid`, `gclid`, `igshid`, etc.) — on by default.
- Strip referral parameters (`ref`, `source`, etc.) — off by default, since
  these occasionally affect the destination page rather than just tracking.
- Custom comma-separated parameter names to always strip.
- Toggle for a confirmation toast on each share.
- **Clean surrounding text** — off by default. When on, supporting prose is
  discarded and only the cleaned link(s) are shared.
- **Links to keep** — All (default), First, or Choose. First keeps only the
  earliest link in the share and drops the rest; Choose shows a small picker
  (links start unselected) and shares just the ones you pick, one per line.

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

Not yet verified on a real device: the Advanced override form and the
Choose link picker — both are new UI this session and haven't been
through an actual touchscreen/share-sheet test yet, only unit tests of
the underlying logic and a static check of the built APK's manifest.
