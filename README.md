# Link Cleaner

A tiny Android app that registers itself as a share target for links. Share
a link to it (from a browser, Twitter/X, etc.) and it strips tracking
parameters (UTM tags, `fbclid`, `gclid`, and similar) then immediately
re-opens the share sheet with the cleaned link so you can pick the real
destination.

Launching the app directly shows no settings at all - just a running
count of links cleaned and a breakdown of which domains you've cleaned
the most. All the cleaning options live in the "Custom" share target
instead. (Package/class names still say "linksanitiser" — that's
deliberate, so existing installs upgrade in place rather than becoming a
separate app when the display name changed.)

## Share targets

The app shows up twice in Android's share sheet:

- **Quick** — `ShareReceiverActivity`. Not configurable at all: applies the
  default tracking-parameter rules, strips surrounding text, and keeps only
  the first link if there's more than one. No UI, ever - that's the point.
- **Custom** — `CustomShareActivity`. Starts from your remembered settings
  (strip surrounding text and first-link-only, the first time) and shows a
  form letting you override any of it just for this share, with a live
  preview of the result. If the share has more than one link, it also shows
  a checkbox per link (first one pre-checked) to pick which survive - no
  separate picker screen, it's inline. Whatever you leave it at when you tap
  Share becomes the starting point next time.

## How it works

- `LinkSanitiser` (in `LinkSanitiser.kt`) is the actual link-finding and
  cleaning logic. It's plain Kotlin with no Android dependencies, so it's
  covered by JVM unit tests in `app/src/test/`. `findLinks()` locates and
  cleans each URL in a shared text without yet deciding which survive;
  `buildResult()` then keeps only the given link indices - as plain text
  cleaned in place, or (with `cleanSurroundingText`) as just those links,
  newline separated. `sanitise()` is a convenience wrapper over both for the
  simple "clean everything in place" case. `hostOf()` extracts a
  normalised domain from a URL, used for the most-cleaned-domains stats.
- `Resharing.kt` holds the logic shared by both activities: reading the
  incoming share, recording which domains got cleaned, and re-opening the
  share sheet with the final text (excluding our own share targets so you
  don't loop back into them).
- `MainActivity` is just the counter and domain stats now, backed by
  `Prefs` (`SharedPreferences`) - no settings.
- `CustomShareActivity` owns all the actual settings UI: the
  tracking-parameter toggles, custom params field, toast toggle, "clean
  surrounding text", and the inline link picker. Changes made there are
  saved back to `Prefs` as the new remembered defaults.

## Settings (Custom share target only - Quick ignores all of these except the tracking rules)

- Strip UTM parameters (`utm_source`, `utm_campaign`, etc.) — on by default.
- Strip ad/click IDs (`fbclid`, `gclid`, `igshid`, etc.) — on by default.
- Strip referral parameters (`ref`, `source`, etc.) — off by default, since
  these occasionally affect the destination page rather than just tracking.
- Custom comma-separated parameter names to always strip.
- Clean surrounding text — on by default, matching Quick.
- Which links to keep, via the inline checkboxes when there's more than one.
  The Share button is disabled whenever the current settings would leave
  nothing to share (e.g. surrounding text stripped and every link
  deselected).

A confirmation toast always shows after sharing - there's no toggle for it.

Quick always uses the current tracking-parameter rules (the first four
above) plus its own fixed "strip surrounding text, first link only" -
it doesn't read or remember the last two.

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

Builds cleanly and all 32 `LinkSanitiserTest` unit tests pass
(`./gradlew test assembleDebug`), verified with the Android SDK
(compileSdk/build-tools 35). The manifest, package name, launcher
activity, and both share targets' `ACTION_SEND` intent filters were
confirmed present in the built APK via `aapt dump`.

The previous round's Custom-target crash was a real bug, root-caused by
reading the layout: `MaterialCardView`'s `app:strokeColor` was pointed at
`?android:attr/listDivider`, which is a *drawable* attribute, not a
colour - an inflate-time type mismatch that would throw as soon as the
layout loaded. Fixed by giving the preview card a plain background
colour instead of a stroke.

Not yet verified on a real device: the Share-button disable logic, the
removal of the toast toggle, and the domain-list styling (monospace
domain, no capitalisation, moved reset button) are all new this round
and untested on a touchscreen - only unit tests of the underlying logic
and a static check of the built APK.
