# The Stagirite's Ledger

A minimal Android app that does one thing: name a habit you're keeping —
*no more alcohol*, *no smoking*, *daily run* — and it counts the days.

> "…one swallow does not make spring, nor does one fine day…"
> — Aristotle, *Nicomachean Ethics* I.7, 1098a (tr. H. Rackham)

## Features

- **Goals** — any number, counted from today or a past date, with an optional note on why each
  one matters. The count ticks over at midnight.
- **Begin again** when you slip: the count restarts, the run is kept, and you get a short passage
  from Aristotle for the day (see *Quotations*).
- **Backdated entries** — change when a goal (or the current run) started by tapping its "since"
  date, and record a slip on any past day (or remove one), from the goal page or by
  tapping the day in its calendar. Runs, stats and the count all follow.
- **Calendar** — a month view for each goal: filled discs for kept days, rings for slips.
- **Stats** — current, longest and average run; share of days kept; fresh starts; slips in the
  last 30 days; a chart of every run's length (tap a bar for its dates).
- **Overview** — all goals at a glance: totals, clean days this month, milestones coming up, a
  combined calendar, and a line per goal.
- Milestones at 1, 3, 7, 14, 21, 30, 60, 90, 180, 270, 365 days, then yearly, marked on the day.
- **App lock** with fingerprint, face or screen lock, locking immediately or after 1 or 5 minutes.
- **Daily reminder** (optional) at a time you choose. It never names your goals.
- Undo after deleting a goal; edit a goal's name, note or start date.
- Light, dark, or follow the system. Optional Google sync. Export / import as JSON.

## Privacy

- Works fully offline. Sync is **off** until you connect an account.
- No ads, no analytics, no server of ours. The only network traffic is to Google Drive, and only
  when sync is on.
- Sync uses Drive's hidden **app data folder** with the `drive.appdata` scope: the app can't see
  any of your other Drive files, and the folder doesn't show up in Drive. (This is the same kind of
  storage Play Games uses for saved games; Play Games itself is reserved for games.)
- **End-to-end encryption** (optional, recommended): with a passphrase, the ledger is encrypted on
  the phone with AES-256-GCM, using a key derived by PBKDF2-SHA256 (210,000 rounds). Google only ever
  stores ciphertext. The derived key is kept on each device under a hardware-backed Android
  Keystore key; the passphrase itself is never stored. Lose the passphrase and the synced copy can't
  be recovered (your phone's copy is unaffected).
- **Disconnect** revokes the app's access to your Google account and can delete the synced copy.
- **App lock** keeps the ledger behind your fingerprint, face or screen lock; while it's on, the
  app is also blanked in recent apps.
- **Hide in recent apps** blanks the app in the app switcher and blocks screenshots.
- The daily reminder's notification is generic ("A moment for your ledger."), so nothing about
  your goals appears on the lock screen.
- Android auto-backup is turned off, so nothing is copied off the device unless you turn sync on.

### How sync works

All your devices need this version or later: the ledger format changed to store slip dates
(older data is converted automatically).

Every goal carries a last-changed time. On sync the app downloads the copy in Drive, keeps the newer
version of each goal, and uploads the result. Deletions are synced as tombstones so they don't come
back. Sync runs when you open or leave the app, a couple of seconds after any change, and on
**Settings → Sync now**.

## Quotations

All quotations are from Aristotle's *Nicomachean Ethics*, translated by H. Rackham (Loeb Classical
Library, 1926), using the [Perseus Digital Library text](https://github.com/PerseusDL/canonical-greekLit)
(CC BY-SA 4.0). Each is cited by book and chapter plus the standard Bekker page, and is quoted
word for word; "…" marks where a sentence is entered or left part way. They live in
`data/Quotes.kt`, and

```sh
python3 tools/verify_quotes.py
```

checks every one against the source text, chapter and page included. CI runs it on every push.
Several passages come from Book VII, on *akrasia*, which Rackham translates "Unrestraint":
acting against your own better judgement. Aristotle's view is that this, unlike settled vice,
can be cured.

## Setting up Google sign-in (one time)

Google only lets an app use Drive once it's registered. This is free and takes about ten minutes:

1. Go to [Google Cloud Console](https://console.cloud.google.com/) and create a project.
2. **APIs & Services → Library**: enable the **Google Drive API**.
3. **APIs & Services → OAuth consent screen** (Google Auth Platform):
   - App name "The Stagirite's Ledger", user type **External**.
   - Under **Data access**, add the scope `.../auth/drive.appdata`.
   - Under **Audience**, leave it in **Testing** and add your Google account as a test user.
4. **Credentials → Create credentials → OAuth client ID → Android**:
   - Package name: `com.stagirite.ledger`
   - SHA-1: `CF:C6:60:65:8C:C7:E4:35:59:14:89:72:32:38:0D:D0:44:C0:9D:04`
     (the fixed debug key in `app/debug.keystore`, used by every debug build including the CI APK)

No keys or IDs go into the code: Google recognises the app by its package name and signature.
If you see "Google sign-in isn't configured for this build", one of these steps is missing.

For a Play Store release, sign with your own release key, add its SHA-1 as a second Android
client, and publish the consent screen.

## Getting the app

**Download a build:** every push runs the *Build APK* GitHub Action. Open the latest run under
the repo's **Actions** tab, download the `stagirites-ledger-debug` artifact, unzip it and install
`app-debug.apk` (allow installs from unknown sources when asked).

**Build it yourself:** open the project in Android Studio, or run

```sh
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests: counting, slips, stats, merging, encryption
```

## Project layout

```
app/src/main/java/com/stagirite/ledger/
  data/Goal.kt            start date + slip dates; runs, merge, milestones
  data/Stats.kt           per-goal stats, calendar day states, overview figures
  data/Quotes.kt          the Aristotle passages (checked by tools/verify_quotes.py)
  data/LedgerCodec.kt     the JSON format (storage, sync, export)
  data/GoalStore.kt       the ledger on this device
  data/Settings.kt        theme and privacy preferences
  sync/Envelope.kt        optional passphrase encryption of the synced file
  sync/KeyVault.kt        keeps the sync key under the Android Keystore
  sync/DriveClient.kt     tiny Google Drive app-data-folder client
  sync/SyncManager.kt     sign-in, sync and encryption state
  security/AppLock.kt     fingerprint / screen-lock gate
  reminders/Reminder.kt   optional daily notification
  ui/                     Compose screens: goals, goal page, overview, settings, calendar, chart
```

Kotlin, Jetpack Compose, Material 3. Android 8.0 (API 26) and up.
