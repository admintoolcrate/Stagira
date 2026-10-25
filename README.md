# The Stagirite's Ledger

A simple Android app that does one thing: you name a habit you're keeping —
*no more alcohol*, *no smoking*, *daily run* — and it counts the days.

> "We become just by doing just acts, temperate by doing temperate acts."
> — Aristotle (the Stagirite), *Nicomachean Ethics*

## Features

- Add any number of goals, starting today or on a past date
- Day count for each goal, rolling over automatically at midnight
- Span shown in years, months and days, plus progress to the next milestone
  (1, 3, 7, 14, 21, 30, 60, 90, 180, 270, 365 days, then every year)
- "Begin again" if you slip — the count restarts, your longest streak is kept
- Everything stays on the device; no account, no network
- Light and dark themes

## Getting the app

**Download a build:** every push runs the *Build APK* GitHub Action. Open the
latest run under the repo's **Actions** tab and download the
`stagirites-ledger-debug` artifact, unzip it, and install `app-debug.apk` on
your phone (you'll need to allow installs from unknown sources).

**Build it yourself:** open the project in Android Studio, or run

```sh
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests for the day-counting logic
```

## Project layout

```
app/src/main/java/com/stagirite/ledger/
  Goal.kt          day counting, resets, milestones (pure Kotlin, unit tested)
  GoalStore.kt     saves goals to SharedPreferences as JSON
  MainActivity.kt  entry point
  ui/LedgerApp.kt  list, detail and "new goal" screens (Jetpack Compose)
  ui/Theme.kt      parchment-and-ink colours, serif type
```

Kotlin, Jetpack Compose, Material 3. Min Android 8.0 (API 26).
