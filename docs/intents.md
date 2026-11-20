# What other apps can ask Stagira

Stagira talks to its sibling apps (Lyceum, Kairos) only through Android intents on the phone:
no server, no shared storage, no account. Every request either needs the person's tap in
Stagira or only opens Stagira; nothing is written or returned silently. All three respect
the app lock. Code: `app/src/main/java/com/stagirite/ledger/intake/`.

| Action (package `cc.toolcrate.stagira`) | From | Extras in | What happens | Result |
|---|---|---|---|---|
| `cc.toolcrate.stagira.action.RECORD_URGE` | Kairos | `source` (String), `at` (epoch ms, optional) | Asks to note an urge resisted today or yesterday; the person picks the goal and confirms | `RESULT_OK` if recorded, else `RESULT_CANCELED`; no data |
| `cc.toolcrate.stagira.action.PICK_GOAL` | Lyceum | none | The person chooses one of their goals | `RESULT_OK` with `goal_id` (≤ 128) and `goal_title` (≤ 120, the title as shown: a private goal's cover name); else `RESULT_CANCELED` |
| `cc.toolcrate.stagira.action.VIEW_GOAL` | Lyceum | `goal_id` | Opens that goal's page (or the ledger, if the id is unknown) | none |

Extras from other apps are checked: blank, over-long or control-character ids are ignored.
