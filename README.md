# Link Cleaner

A tiny Android app that registers itself as a share target for links. Share
a link to it (from a browser, Twitter/X, etc.) and it strips tracking
parameters (UTM tags, `fbclid`, `gclid`, and similar) then immediately
re-opens the share sheet with the cleaned link so you can pick the real
destination.

Launching the app directly shows no settings at all - just a running
count of links cleaned, tracking parameters stripped, and a breakdown of
which domains you've cleaned the most. All the cleaning options live in
the "Custom" share target instead. (Package/class names still say
"linksanitiser" — that's deliberate, so existing installs upgrade in
place rather than becoming a separate app when the display name changed.)

## Share targets

The app shows up twice in Android's share sheet:

- **Quick** — `ShareReceiverActivity`. Not configurable at all: applies the
  default tracking-parameter rules, strips surrounding text, and keeps only
  the first link if there's more than one. No UI, ever - that's the point.
- **Custom** — `CustomShareActivity`. Starts from your remembered settings
  (strip surrounding text and first-link-only, the first time) and shows a
  form letting you override any of it just for this share, with a live
  preview of the result. Shows the original shared text (scrollable, capped
  to a few lines) for reference. If the share has more than one link, it
  also shows a checkbox per link (first one pre-checked) to pick which
  survive - no separate picker screen, it's inline. The Share button
  disables itself whenever the current settings would leave nothing to
  share. Whatever you leave it at when you tap Share becomes the starting
  point next time.

## How it works

- `LinkSanitiser` (in `LinkSanitiser.kt`) is the actual link-finding and
  cleaning logic. It's plain Kotlin with no Android dependencies, so it's
  covered by JVM unit tests in `app/src/test/`. `findLinks()` locates and
  cleans each URL in a shared text without yet deciding which survive;
  `buildResult()` then keeps only the given link indices - as plain text
  cleaned in place, or (with `cleanSurroundingText`) as just those links,
  newline separated. `sanitise()` is a convenience wrapper over both for the
  simple "clean everything in place" case. `hostOf()` extracts a
  normalised domain from a URL, used for the most-cleaned-domains stats;
  `hasDomainSpecificRule()` says whether a URL's host has a curated
  site-specific rule at all, used to decide whether Custom shows that
  toggle.
- `Resharing.kt` holds the logic shared by both activities: reading the
  incoming share, recording which domains got cleaned (and how many links
  vs. how many parameters), and re-opening the share sheet with the final
  text (excluding our own share targets so you don't loop back into them).
- `MainActivity` is just the stats now, backed by `Prefs`
  (`SharedPreferences`) - no settings.
- `CustomShareActivity` owns all the actual settings UI: the
  tracking-parameter toggles (each with a description), "clean surrounding
  text", the domain-specific toggle, and the inline link picker. Changes
  made there are saved back to `Prefs` as the new remembered defaults.

## Settings (Custom share target only - Quick ignores all of these except the tracking rules)

- Strip UTM parameters (`utm_source`, `utm_campaign`, etc.) — on by default.
- Strip ad/click IDs (`fbclid`, `gclid`, `igshid`, etc.) — on by default.
- Strip referral parameters (`ref`, `source`, etc.) — off by default, since
  these occasionally affect the destination page rather than just tracking.
- Strip domain-specific parameters (e.g. The Guardian's `CMP` tag) — on by
  default, only shown at all when a link in the share actually has a
  curated rule for its domain.
- Clean surrounding text — on by default, matching Quick.
- Which links to keep, via the inline checkboxes when there's more than one.

A confirmation toast always shows after sharing - there's no toggle for it.
There's no free-text "custom parameters" field any more either - it didn't
fit once the built-in categories had their own descriptions and the
domain-specific toggle existed.

Quick always uses the current tracking-parameter rules (the first four
above) plus its own fixed "strip surrounding text, first link only" -
it doesn't read or remember the last one.

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

Builds cleanly and all 33 `LinkSanitiserTest` unit tests pass
(`./gradlew test assembleDebug`), verified with the Android SDK
(compileSdk/build-tools 35). The manifest, package name, launcher
activity, and both share targets' `ACTION_SEND` intent filters were
confirmed present in the built APK via `aapt dump`.

Not yet verified on a real device: the reworked Custom layout (shared-text
preview box, consolidated settings section, conditional domain-specific
toggle) and the split links/params counters on the main screen are all new
this round and untested on a touchscreen - only unit tests of the
underlying logic and a static check of the built APK. The shared-text
preview box nests a small fixed-height `ScrollView` inside the screen's
main `ScrollView`, which is a well-established Android pattern for this
(the inner one claims the gesture via `requestDisallowInterceptTouchEvent`
once it detects a vertical drag) but is still worth confirming feels right
in hand.
