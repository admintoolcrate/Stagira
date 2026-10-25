# Data safety form — suggested answers

Play Console → Policy → App content → Data safety. These answers follow how the app actually
behaves (see `privacy-policy.md`). They're deliberately conservative: when in doubt, declare.

## Overview

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** (only when the user turns on Google Drive sync) |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (HTTPS to Google Drive) |
| Do you provide a way for users to request that their data is deleted? | **Yes** (Disconnect with "delete synced copy", or uninstall) |

## Data types

Declare these as **Collected**, **not shared**, **optional** (users can choose whether it's
collected), purpose **App functionality** only:

| Category | Type | Why |
|---|---|---|
| App activity | Other user-generated content | Goal names, notes and dates in the synced copy |
| Health and fitness | Health info | Goals may describe habits such as drinking or smoking |

### The optional rewarded ad

Google's Mobile Ads SDK is in the app (only started when the user taps *Watch an ad to support
us*). Declare what Google lists for the SDK in
[its Data safety guidance](https://developers.google.com/admob/android/privacy/play-data-disclosure)
— check that page when you fill the form, since Google keeps it current. At the time of writing
it covers, roughly:

| Category | Type | Collected / shared | Purposes |
|---|---|---|---|
| Location | Approximate location (from IP address) | Collected and shared | Advertising, analytics, fraud prevention |
| Device or other IDs | Advertising ID, app set ID | Collected and shared | Advertising, analytics, fraud prevention |
| App activity | App interactions (with the ad) | Collected and shared | Advertising, analytics |
| App info and performance | Diagnostics | Collected and shared | Analytics, fraud prevention |

Mark these **optional**, since they only happen if the user chooses to watch an ad.

### Purchases

Google Play handles payment; the app only stores whether Plus is owned, on the device. Nothing
about purchases is sent to the developer.

Everything else (contacts, financial info, messages, photos, audio, web history): **not collected**.

### Why not declare feedback reports

Bug reports and feature requests are sent by the user from their own email app or browser; the
app itself doesn't transmit them. Device details in a report are shown to the user first and
are optional.

### Why "not shared"

Synced data goes to the user's own Google Drive at their request, and the developer never
receives it.

## Security section

- **Data is encrypted in transit:** Yes.
- **You can request that data be deleted:** Yes.
