# The Stagirite's Ledger

A minimal Android app (**Stagirite** on your home screen) that does one thing: name a habit you're keeping —
*no more alcohol*, *no smoking*, *daily run* — and it counts the days.

> "…one swallow does not make spring, nor does one fine day…"
> — Aristotle, *Nicomachean Ethics* I.7, 1098a (tr. H. Rackham)

## Features

- **Goals** — any number, counted from today or a past date, with an optional note on why each
  one matters. The count ticks over at midnight.
- **Vices and virtues** — optionally mark a goal a vice (something to do less of) or a virtue
  (something to do more of). Virtues log "missed days" instead of slips.
- **Icons** — a black-and-white emoji beside each goal, drawn with the open-source Noto Emoji
  font (see *Third-party material*).
- **Private titles** — show a cover name ("No more corn") while the real name and note stay
  encrypted behind a PIN. Long-press the app's name on the Goals screen to reveal them; they hide
  again when you leave the app.
- **Home-screen widget** — up to four goals and their counts. Private goals show their cover
  names, and while the app lock is on it shows only "Locked".
- **Targets** — an optional aim per goal (30, 90, 180 or 365 days); the ring fills towards it.
- **Archive** — hide finished or paused goals without losing them; restore any time.
- **Quick actions** — long-press a goal on the list to log a slip today or archive it, with Undo.
- **Sort** — as added, longest current run, next milestone, or name.
- **Suggestions** when adding a goal (No alcohol, Exercise, Read …) fill in the icon and type.
- **Guide** — a short optional introduction on first launch, and an About page explaining the
  name, in Settings.
- **Begin again** when you slip: the count restarts, the run is kept, and you get a short passage
  from Aristotle for the day (see *Quotations*).
- **Backdated entries** — change when a goal (or the current run) started by tapping its "since"
  date, and record a slip on any past day (or remove one), from the goal page or by
  tapping the day in its calendar. Runs, stats and the count all follow.
- **Calendar** — a month view for each goal: filled discs for kept days, rings for slips.
- **Progress** — measured by the gaps between slips: a verdict (Improving, Steady, Harder
  lately, or Unbroken), your typical run now against before (median of the last 3 runs vs the 3
  before), slips in the last 90 days against the 90 before, back-to-back slips (within 3 days),
  new personal bests, and days kept by month. Too little history says so rather than guessing.
- **Stats** — current, longest and average run; share of days kept; fresh starts; slips in the
  last 30 days; a chart of every run's length with a trend line and the median run (tap a bar
  for its dates).
- **Overview** — all goals at a glance: totals, clean days this month, milestones coming up, a
  combined calendar, and a line per goal.
- **Milestones** at 1, 3, 7, 14, 21, 30, 60, 90, 100, 180, 270 and 365 days, then 500, every
  1,000 and every year. Each goal has a Milestones tab (reached ones with their dates, the next few
  with the dates they'll fall on), and reaching one shows a short celebration with an Aristotle
  passage.
- **Log a slip** in two taps: Today, Yesterday, or another day from a calendar, with Undo.
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
- **Private titles**: real names and notes are encrypted (AES-256-GCM, PBKDF2 key from your
  PIN) on the phone and in sync; only the cover name is stored in the clear. A 4–8 digit PIN
  keeps them from anyone using or glancing at your phone; against someone with your synced file
  and time to guess, rely on sync encryption too. Forgetting the PIN can't be undone.
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

## Third-party material

Icons use [Noto Emoji](https://github.com/google/fonts/tree/main/ofl/notoemoji) (© 2013, 2022
Google) under the SIL Open Font License 1.1, which allows bundling the font in an app; its licence
ships inside the app and is shown in Settings → About. Quotations are from the Perseus Digital
Library's Rackham text (CC BY-SA 4.0). Details in [`third_party/`](third_party/).

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

**If "Connect Google account" fails** with "Google doesn't recognise this app yet", one of the
steps above is missing — most often the Android OAuth client (step 4), or your account not being
added as a test user (step 3). Changes in Google Cloud can take a few minutes to apply.

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
  data/Progress.kt        improvement measures from the gaps between slips
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
  widget/LedgerWidget.kt  home-screen widget (Jetpack Glance)
  ui/                     Compose screens: goals, goal page, overview, settings, calendar, chart
```

Kotlin, Jetpack Compose, Material 3. Android 8.0 (API 26) and up.
