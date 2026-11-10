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

Not **Health info**: the app never asks for health data. Goals are whatever the user types, which
Play counts as user-generated content. Declaring health info would also sit oddly with the Health
apps declaration ("no health features"), the answer that got the app through review.

Everything else, including location, device or other IDs, app interactions, diagnostics,
contacts, financial info, messages, photos, audio and web history: **not collected**. There are
no ads and no purchases, and no advertising ID (answer **No** under App content → Advertising ID).

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
