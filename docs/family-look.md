# The ToolCrate family look

Stagira, Lyceum, Telos and Kairos are one family: quiet, private, bookish tools. This page is
the same in all four repositories; change it in all four together. Stagira is the reference
implementation (`ui/Theme.kt`, `ui/ToneTokens.kt`).

## Colour: Mono

Black on white, white on black. No cream, no sepia, no tinted pages.

| Token | Light | Dark | Use |
|---|---|---|---|
| page | `#FFFFFF` | `#000000` | background, surface |
| card | `#F7F7F6` | `#0E0E0E` | cards, sheets |
| raised | `#F0F0EE` | `#171717` | pressed, selected containers |
| high | `#F3F3F2` | `#121212` | surfaceContainerHigh |
| tint | `#ECECEA` | `#262626` | soft fill for chosen or kept things |
| ink | `#111111` | `#FFFFFF` | text, primary buttons, marks |
| muted | `#6E6E6E` | `#9A9A9A` | secondary text |
| line | `#CFCFCC` | `#3A3A3A` | outlines |
| hairline | `#EAEAE8` | `#1C1C1C` | dividers |
| gold | `#A8834E` | `#D6B47A` | the one accent, used sparingly: a milestone, a highlight |

- Material roles: primary = ink, secondary = ink, secondaryContainer = tint, tertiary = gold,
  outline = line, outlineVariant = hairline, surfaceContainerLow = card.
- **No alarm red.** Errors and warnings are said in words, in ink. (Telos's priority colours are
  the one exception, and always come with a text or shape cue.)
- Dark mode is true black.

## Type

- **EB Garamond** for display moments only: the app name, big figures (counts, timers), quoted
  or reflective passages, and a screen's title (`ScreenTitle`, 30/34 sp, as Stagira's top bars).
  Material `display*` roles.
- **Inter** for everything else: `headline*` Light/Normal, `title*` SemiBold, `body*` Normal,
  `label*` Medium. Inter's weights come from its variable axis.
- Big display type scales with the system font size only up to 1.25x; body text scales fully.

## Shape and space

- Cards: 24 dp corners. Bottom sheets and dialogs: 28 dp. Small tiles: 14 dp. Buttons: Material's
  pill. Other small parts keep Material's defaults (`Shapes(4, 8, 24, 24, 28)`).
- Spacing in 4 dp steps; 24 dp page margins; hairline dividers instead of shadows; no tonal
  elevation.

## Icons and store graphics

- Launcher icon: the mark in `#FAFAF8` on ink `#111111`, stroke 3 on the 108-unit canvas, one
  simple idea per app (Stagira: tally, Lyceum: open book, Telos: open ring around a point,
  Kairos: wave in a circle). No accent colour in the mark. Monochrome themed icon provided.
- Play icon (512) and feature graphic (1024x500): drawn from the launcher vector by
  `tools/family_graphics.py` (the same script in every repo), on `#0F0F0F`: mark on the left,
  name in EB Garamond `#F2F2F0`, a letter-spaced Inter label in grey `#9A9A97`
  ("THE HABIT LEDGER"), then one sentence in Inter, same grey. For example:
  `python3 tools/family_graphics.py Kairos "THE CALM KIT" "Calm tools for hard moments." app/src/main/res/font docs/play-store/graphics app/src/main/res/drawable/ic_launcher_foreground.xml`
- Store name: `Name – Descriptor` with an en dash (`Stagira – Habit Tracker`).
- Tagline: one short sentence ending in a full stop.

| App | Store name | Label | Tagline |
|---|---|---|---|
| Stagira | Stagira – Habit Tracker | THE HABIT LEDGER | Count the days you keep. |
| Lyceum | Lyceum – Daily Journal | THE DAILY JOURNAL | One quiet page a day. |
| Telos | Telos – Tasks & Goals | THE TASK BOOK | Tasks with a reason. |
| Kairos | Kairos – Calm Tools | THE CALM KIT | Calm tools for hard moments. |

## Voice

- Plain, warm, second person, short sentences. No exclamation marks.
- No guilt, no pressure, no "you missed", no streak nagging.
- Privacy said simply and the same way everywhere: "No ads, no accounts. Everything stays on
  your phone." Optional backup is "your own Google Drive, encrypted with your passphrase".
- No medical, therapeutic or productivity-miracle claims.
