package com.stagirite.ledger.ui

// The builders for Tone's colours live in a file of their own, with no state, on purpose: when
// they sat in Theme.kt, building Tone loaded Theme.kt, whose top-level values read Tone back
// before it was ready, and the Play build crashed on start (3.16).

/** White mode: a pure white page and black ink; [brand] and [tint] carry the accent. */
internal fun white(brand: Long, tint: Long) = Tokens(
    page = 0xFFFFFFFF, card = 0xFFF7F7F6, tint = tint, ink = 0xFF111111, muted = 0xFF6E6E6E,
    accent = 0xFFA8834E, line = 0xFFCFCFCC, hairline = 0xFFEAEAE8, raised = 0xFFF0F0EE, high = 0xFFF3F3F2,
    lowest = 0xFFFFFFFF, brand = brand,
)

/** Black mode: a true-black page and white ink, with the accent lightened to read on black. */
internal fun black(brand: Long, tint: Long) = Tokens(
    page = 0xFF000000, card = 0xFF0E0E0E, tint = tint, ink = 0xFFFFFFFF, muted = 0xFF9A9A9A,
    accent = 0xFFD6B47A, line = 0xFF3A3A3A, hairline = 0xFF1C1C1C, raised = 0xFF171717, high = 0xFF121212,
    lowest = 0xFF000000, brand = brand,
)
