package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class NotesTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val slip = start.plusDays(10)
    private val goal = Goal("g", "T", start).recordSlip(slip, 1)

    @Test
    fun notesAttachToSlips() {
        val g = goal.withNote(slip, "  Wedding, open bar  ", 2)
        assertEquals("Wedding, open bar", g.notes[slip])
        assertEquals(2, g.updatedAt)
        // Only slip days can carry a note.
        assertSame(g, g.withNote(start.plusDays(3), "x", 3))
        // A blank note clears it.
        assertNull(g.withNote(slip, " ", 4).notes[slip])
    }

    @Test
    fun removingASlipDropsItsNote() {
        val g = goal.withNote(slip, "note", 2).removeSlip(slip, 3)
        assertEquals(emptyMap<LocalDate, String>(), g.notes)
    }

    @Test
    fun movingTheLatestSlipCarriesItsNote() {
        val g = goal.withNote(slip, "note", 2).moveCurrentStart(start.plusDays(7), 3)
        assertEquals(mapOf(start.plusDays(7) to "note"), g.notes)
    }

    @Test
    fun privateGoalsNeverHoldPlainNotes() {
        val private = goal.copy(sealed = "sealed")
        assertSame(private, private.withNote(slip, "secret", 2))
        val madePrivate = goal.withNote(slip, "secret", 2).edit("Cover", start, "", 3, sealed = "sealed")
        assertEquals(emptyMap<LocalDate, String>(), madePrivate.notes)
    }

    @Test
    fun codecKeepsNotes() {
        val g = goal.withNote(slip, "Felt low; called a friend", 2).copy(createdAt = 1)
        assertEquals(listOf(g), LedgerCodec.decode(LedgerCodec.encode(listOf(g))))
        assertEquals(mapOf(slip to "x"), LedgerCodec.decodeNotes(LedgerCodec.encodeNotes(mapOf(slip to "x"))))
    }

    @Test
    fun tombstoneDropsNotes() {
        assertEquals(emptyMap<LocalDate, String>(), goal.withNote(slip, "n", 2).tombstone(3).notes)
    }
}
