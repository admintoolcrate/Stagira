package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class UrgesTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val goal = Goal("g", "T", start)

    @Test
    fun countsAccumulateAndUndo() {
        val day = start.plusDays(3)
        val g = goal.resistUrge(day, 1).resistUrge(day, 2)
        assertEquals(2, g.urgesOn(day))
        assertEquals(2, g.updatedAt)
        assertEquals(1, g.unresistUrge(day, 3).urgesOn(day))
        assertEquals(emptyMap<LocalDate, Int>(), g.unresistUrge(day, 3).unresistUrge(day, 4).urges)
        // Nothing to undo, or before the goal began: unchanged.
        assertSame(goal, goal.unresistUrge(day, 5))
        assertSame(goal, goal.resistUrge(start.minusDays(1), 5))
    }

    @Test
    fun wordingFollowsKind() {
        assertEquals("Resisted an urge", goal.urgeLabel)
        assertEquals("Did it anyway", goal.copy(kind = GoalKind.Virtue).urgeLabel)
        assertEquals("Urges resisted", goal.copy(kind = GoalKind.Vice).urgesLabel)
    }

    @Test
    fun codecKeepsUrges() {
        val g = goal.resistUrge(start.plusDays(2), 1).resistUrge(start.plusDays(2), 2).resistUrge(start.plusDays(9), 3).copy(createdAt = 1)
        assertEquals(listOf(g), LedgerCodec.decode(LedgerCodec.encode(listOf(g))))
    }

    @Test
    fun statsCountTotalThisRunAndLast30() {
        val today = start.plusDays(59)
        var g = goal
        listOf(5L, 5, 20, 45, 58).forEach { g = g.resistUrge(start.plusDays(it), 1) }
        g = g.recordSlip(start.plusDays(40), 2)
        val s = g.stats(today)
        assertEquals(5, s.urgesTotal)
        assertEquals(2, s.urgesThisRun) // days 45 and 58, after the slip on day 40
        assertEquals(2, s.urgesLast30) // last 30 days: days 30..59
    }

    @Test
    fun progressComparesUrgeWindowsWithoutChangingTheVerdict() {
        val today = start.plusDays(199)
        var g = goal
        listOf(150L, 160, 170).forEach { g = g.resistUrge(start.plusDays(it), 1) }
        val p = g.progress(today)
        assertEquals(3, p.urgesRecent)
        assertEquals(0, p.urgesBefore)
        assertEquals(Verdict.Unbroken, p.verdict)
    }

    @Test
    fun tombstoneDropsUrges() {
        assertEquals(emptyMap<LocalDate, Int>(), goal.resistUrge(start, 1).tombstone(2).urges)
    }
}
