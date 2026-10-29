# Getting Stagira onto Google Play

A checklist, in order. Items marked **(you)** need your accounts or decisions; everything else is
already set up in this repo.

Current Google rules this is based on (October 2026):

- New **personal** developer accounts (created after 13 November 2023) must run a **closed test
  with at least 12 testers opted in for the last 14 days** before production access can be
  requested. Organisation accounts are exempt.
  ([Play Console Help](https://support.google.com/googleplay/android-developer/answer/14151465))
- From 31 August 2026, new apps and updates must **target Android 16 (API 36)**. Stagira
  already does (`targetSdk = 36`).
  ([Android developers](https://developer.android.com/google/play/requirements/target-sdk))
- Every app must complete the **Health apps declaration**, even if it has no health features.
  ([Play Console Help](https://support.google.com/googleplay/android-developer/answer/14738291))
- The Drive scope the app uses, `drive.appdata`, is **non-sensitive**, so publishing the Google
  sign-in screen needs only basic verification.
  ([Google Drive API](https://developers.google.com/workspace/drive/api/guides/api-specific-auth))

---

## 1. Developer account (you)

1. Go to [play.google.com/console](https://play.google.com/console) and sign up.
2. Choose **Personal** (one-time US$25, ID verification; the 12-tester rule applies) or
   **Organisation** (needs a D-U-N-S number; no tester rule).
3. Complete identity verification. This can take a few days.

## 2. Your upload key (you, once — keep it safe)

The upload key proves future updates come from you. Make it on your own computer:

```sh
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

- Store `upload-keystore.jks` and its passwords in a password manager. **Never commit them**
  (`.gitignore` already blocks `*.jks` and `keystore.properties`).
- With Play App Signing (the default), Google holds the real app-signing key; if you lose the
  upload key, Google can reset it, but it's slow.

Then either:

- **Build locally:** copy `keystore.properties.example` to `keystore.properties`, fill it in,
  and run `./gradlew bundleRelease`. The bundle is at
  `app/build/outputs/bundle/release/app-release.aab`.
- **Or let GitHub build it:** in the repo, **Settings → Secrets and variables → Actions**, add
  - `UPLOAD_KEYSTORE_BASE64` — the output of `base64 -w0 upload-keystore.jks`
  - `UPLOAD_STORE_PASSWORD`, `UPLOAD_KEY_ALIAS` (`upload`), `UPLOAD_KEY_PASSWORD`

  Then, in **Actions → Build APK → Run workflow**, tick **release** (or push a tag such as
  `v2.1`). That run produces a `stagirite-play-bundle` artifact, signed and ready to upload.

## 3. Create the app in Play Console (you)

1. **Create app** → name *Stagira*, default language, **App**, **Free**.
2. The package name is `com.stagirite.ledger`. It's fixed forever once used.
3. Accept Play App Signing when you upload the first bundle.

## 4. App content (Policy → App content) (you, ~20 minutes)

Suggested answers are in [`data-safety.md`](data-safety.md). In short:

| Section | Answer |
|---|---|
| Privacy policy | A public URL to [`privacy-policy.md`](privacy-policy.md) (see step 6) |
| Ads | **Yes, the app contains ads** (only the optional "Watch an ad to support us") |
| App access | All functionality is available without special access |
| Content rating | Questionnaire: references to alcohol, tobacco and gambling as things to cut down on; no violence, no user-to-user content |
| Target audience | **18 and over** (its examples include alcohol and gambling; this also keeps it out of the Families programme) |
| Data safety | See `data-safety.md` |
| Health apps | Tick what matches how you describe it. If the listing talks about quitting drinking, smoking or gambling, *Mental and behavioral health* is the honest fit; if it's pitched as a general habit tracker, review the list and decide. |
| Government app, financial features, news | No |

## 4b. Plus, tips and the optional ad (you)

**In-app products** (Monetize → Products → In-app products, after the first bundle is uploaded).
Create these *one-time products* with exactly these ids:

| Product id | What | Suggested price |
|---|---|---|
| `plus` | Stagira Plus | 4.99 |
| `tip_small_2` | Small tip | 1.99 |
| `tip_medium` | Tip | 4.99 |
| `tip_large_2` | Large tip | 9.99 |

Then add your Google account as a **licence tester** (Settings → License testing) so you can buy
them free while testing. Debug builds from GitHub have Plus unlocked anyway; only the Play
version charges.

**AdMob** (only if you want the "watch an ad" option to earn):

1. Create an account at [admob.google.com](https://admob.google.com), add the app, and create
   a **Rewarded** ad unit.
2. The ids are in `app/build.gradle.kts`: app `ca-app-pub-8879201576445737~6754347629` in every
   build, and the rewarded unit `ca-app-pub-8879201576445737/2133626084` in release (Play) builds
   only. Test builds use Google's sample ad unit, so nobody taps a real ad on a test copy.
3. **Blocking controls → Sensitive categories:** block *Alcohol*, *Gambling & betting* and
   *Dating*, and consider *Drugs & supplements*. For a recovery app this matters.
4. **Privacy & messaging:** create a **European regulations (GDPR)** message for the app; the
   app shows it automatically to EEA/UK users before an ad.
5. Link the AdMob app to the Play listing once it's live.

A brand-new AdMob account usually serves few or no ads for its first days while Google reviews it.

## 5. Store listing (Grow → Store presence → Main store listing) (you)

- Text: copy from [`store-listing.md`](store-listing.md).
- **App icon:** `graphics/icon-512.png` (512 × 512).
- **Feature graphic:** `graphics/feature-graphic-1024x500.png`.
- **Phone screenshots:** at least 2 (up to 8). Take them on your phone from the release build,
  using made-up goals, not your own. A good set: home page, a goal's ring and stats, the year
  view, the progress card, the weekly review, the Day 0 screen with a quote, dark mode.
- Category: **Health & Fitness** (or Productivity). Contact email: `admin.toolcrate@gmail.com`.

## 6. Host the privacy policy (you)

Play needs a public web page. The repo has a small site in `docs/` for this:

1. On GitHub: **Settings → Pages → Build and deployment**, Source **Deploy from a branch**,
   Branch `main`, folder **/docs** → **Save**.
2. A minute later the site is at `https://admintoolcrate.github.io/Stagira/`, and the
   policy at `https://admintoolcrate.github.io/Stagira/play-store/privacy-policy.html`.
   That second link goes in Play Console → App content → Privacy policy.

(Or paste `privacy-policy.md` into a free Google Sites page.)

## 7. Google sign-in for the Play version (you)

Play re-signs the app with Google's key, so sign-in needs that key registered too:

1. Play Console → **Test and release → Setup → App signing** (often still labelled *App integrity*): copy the
   **SHA-1** of the *App signing key* and of the *Upload key*.
2. Google Cloud Console → **Credentials** → add an **Android OAuth client** for each SHA-1, package
   `com.stagirite.ledger` (keep the debug one too).
3. **OAuth consent screen → Publish app** (move from Testing to In production). With only the
   non-sensitive `drive.appdata` scope this is basic verification: app name, support email,
   privacy policy URL, and a homepage on a domain you've verified.

Until this is done, sync only works for the test users you've added.

## 8. Testing tracks (you)

1. **Internal testing** first: upload the `.aab`, add yourself, install from the Play link, and
   check sign-in, sync, lock, reminders and the widget.
2. **Closed testing** (personal accounts): create a track, add **at least 12 testers** (aim for
   15–20 in case some drop out) by email list or Google Group, and keep them opted in for
   **14 days in a row**. Ask them to open the app a few times and send feedback from
   *Settings → Help & feedback*.
3. After 14 days, **apply for production access** from the dashboard. Google asks a few questions
   about the test.

## 9. Production

1. Create a production release with the same bundle (or a newer one), write release notes, and
   roll out (a staged rollout, e.g. 20%, is a good start).
2. Review usually takes from a few hours to a few days.

## Every update after that

- Raise `versionCode` (and `versionName`) in `app/build.gradle.kts`; Play rejects a repeated
  `versionCode`.
- Build the signed bundle (locally or the CI artifact), upload to a track, promote to production.

## Uploads from GitHub (one-time setup)

Release builds (Actions → Build APK → Run workflow → tick "release") upload the signed bundle
straight to **Internal testing**. To switch that on:

1. **Google Cloud** (the project used for sign-in): *APIs & Services → Library* → enable
   **Google Play Android Developer API**.
2. *IAM & Admin → Service accounts → Create service account*, e.g. `github-uploader`. Skip the
   optional role and user steps. Open it → *Keys → Add key → Create new key → JSON*; a file
   downloads. Treat it like a password: it can publish to your Play account.
3. **Play Console → Users and permissions → Invite new users**: the service account's email
   (ends in `iam.gserviceaccount.com`). Under *App permissions* add Stagira with **Release to
   testing tracks** (and *View app information*). Invite.
4. **GitHub → Settings → Secrets and variables → Actions → New repository secret**:
   `PLAY_SERVICE_ACCOUNT_JSON`, the whole contents of the JSON file. Then delete the file.

Version codes come from the build number (1000 + run number), so they always go up. To get a
build to testers or users, promote it in Play Console: *Internal testing → Promote release*.
