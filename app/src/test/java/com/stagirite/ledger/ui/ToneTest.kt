package com.stagirite.ledger.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tone must be usable as the first thing loaded: in 3.16 its colours were built by
 * functions in Theme.kt, whose top-level values read Tone back mid-load, and the Play
 * build crashed on start. Nothing else in the ordinary tests touches the ui package,
 * so here Tone is loaded cold, as it was on the phone.
 */
class ToneTest {
    @Test
    fun loadsColdAndEveryAccentHasBothModes() {
        assertEquals(Tone.Mono, Tone.of(null))
        Tone.entries.forEach {
            assertNotNull(it.light)
            assertNotNull(it.dark)
            assertEquals(0xFFFFFFFF, it.light.page)
            assertEquals(0xFF000000, it.dark.page)
        }
    }

    @Test
    fun olderPaletteNamesMapToTheNearestAccent() {
        assertEquals(Tone.Mono, Tone.of("Snow"))
        assertEquals(Tone.Mono, Tone.of("Black"))
        assertEquals(Tone.Gold, Tone.of("Paper"))
        assertEquals(Tone.Lavender, Tone.of("Dusk"))
        assertEquals(Tone.Sage, Tone.of("Sage"))
    }
}
