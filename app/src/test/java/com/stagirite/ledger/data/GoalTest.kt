package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val goal = Goal(id = "g", title = "No more alcohol", since = start)

    @Test
    fun countsWholeDaysSinceStart() {
        assertEquals(0, goal.daysKept(start))
        assertEquals(1, goal.daysKept(start.plusDays(1)))
        assertEquals(365, goal.daysKept(LocalDate.of(2027, 1, 1)))
        assertEquals(0, goal.daysKept(start.minusDays(3)))
    }

    @Test
    fun slipTodayStartsTheCountAgainAndKeepsTheRun() {
        val day40 = start.plusDays(40)
        val g = goal.recordSlip(day40, now = 5)
        assertEquals(day40, g.currentStart)
        assertEquals(listOf(Streak(start, day40)), g.history)
        assertEquals(1, g.freshStarts)
        assertEquals(5, g.updatedAt)
        assertEquals(0, g.daysKept(day40))
        assertEquals(40, g.longestStreak(day40.plusDays(5)))
        assertEquals(50, g.longestStreak(day40.plusDays(50)))
    }

    @Test
    fun backdatedSlipSplitsTheRunItFallsIn() {
        val today = start.plusDays(30)
        val g = goal.recordSlip(start.plusDays(20), 1).recordSlip(start.plusDays(10), 2)
        assertEquals(listOf(start.plusDays(10), start.plusDays(20)), g.slips)
        assertEquals(
            listOf(Streak(start, start.plusDays(10)), Streak(start.plusDays(10), start.plusDays(20)), Streak(start.plusDays(20), today)),
            g.runs(today),
        )
        assertEquals(10, g.daysKept(today))
    }

    @Test
    fun slipsBeforeTheStartOrTwiceOnADayAreIgnored() {
        assertEquals(goal, goal.recordSlip(start.minusDays(1), 9))
        val once = goal.recordSlip(start.plusDays(3), 1)
        assertEquals(once, once.recordSlip(start.plusDays(3), 2))
    }

    @Test
    fun removingASlipJoinsTheRunsBack() {
        val day = start.plusDays(7)
        val g = goal.recordSlip(day, 1).removeSlip(day, 2)
        assertEquals(emptyList<LocalDate>(), g.slips)
        assertEquals(14, g.daysKept(start.plusDays(14)))
        assertEquals(2, g.updatedAt)
    }

    @Test
    fun startCannotMovePastTheFirstSlip() {
        val g = goal.recordSlip(start.plusDays(5), 1).edit("x", start.plusDays(9), "", 2)
        assertEquals(start.plusDays(5), g.since)
        assertEquals(start.minusDays(4), g.edit("x", start.minusDays(4), "", 3).since)
    }

    @Test
    fun movingTheStartWithNoSlipsChangesTheStartDate() {
        val g = goal.moveCurrentStart(start.minusDays(30), now = 7)
        assertEquals(start.minusDays(30), g.since)
        assertEquals(30, g.daysKept(start))
        assertEquals(7, g.updatedAt)
        assertEquals(null, g.currentStartEarliest)
    }

    @Test
    fun movingTheStartAfterASlipMovesTheLatestSlip() {
        val g = goal.recordSlip(start.plusDays(10), 1).recordSlip(start.plusDays(20), 2)
        assertEquals(start.plusDays(11), g.currentStartEarliest)

        val moved = g.moveCurrentStart(start.plusDays(15), now = 3)
        assertEquals(listOf(start.plusDays(10), start.plusDays(15)), moved.slips)
        assertEquals(start, moved.since)
        assertEquals(15, moved.daysKept(start.plusDays(30)))

        // Can't go back past the slip before it.
        assertEquals(listOf(start.plusDays(10), start.plusDays(11)), g.moveCurrentStart(start, 4).slips)
    }

    @Test
    fun tombstoneDropsContent() {
        val t = goal.copy(reason = "health").recordSlip(start.plusDays(3), 1).tombstone(now = 9)
        assertTrue(t.deleted)
        assertEquals("", t.title)
        assertEquals("", t.reason)
        assertEquals(emptyList<LocalDate>(), t.slips)
        assertEquals(9, t.updatedAt)
    }

    @Test
    fun mergeKeepsNewestVersionOfEachGoal() {
        val a = Goal("a", "A", start, createdAt = 1, updatedAt = 10)
        val b = Goal("b", "B", start, createdAt = 2, updatedAt = 10)
        val aRenamed = a.edit("A2", start, "", now = 20)
        val bDeleted = b.tombstone(now = 30)
        val c = Goal("c", "C", start, createdAt = 3, updatedAt = 3)

        val merged = mergeGoals(listOf(aRenamed, b), listOf(a, bDeleted, c))
        assertEquals(listOf(aRenamed, bDeleted, c), merged)
        assertEquals(merged, mergeGoals(listOf(a, bDeleted, c), listOf(aRenamed, b)))
    }

    @Test
    fun mergeTiePrefersDeletion() {
        val a = Goal("a", "A", start, updatedAt = 10)
        assertTrue(mergeGoals(listOf(a), listOf(a.tombstone(10))).single().deleted)
        assertTrue(mergeGoals(listOf(a.tombstone(10)), listOf(a)).single().deleted)
    }

    @Test
    fun milestones() {
        assertEquals(1, Milestones.next(0))
        assertEquals(3, Milestones.next(1))
        assertEquals(30, Milestones.next(21))
        assertEquals(100, Milestones.next(90))
        assertEquals(500, Milestones.next(365))
        assertEquals(730, Milestones.next(500))
        assertEquals(1000, Milestones.next(800))
        assertEquals(1095, Milestones.next(1000))
        assertEquals(0f, Milestones.progress(0), 0f)
        assertEquals(0.5f, Milestones.progress(45), 0.001f)
        assertTrue(Milestones.isMilestone(30))
        assertTrue(Milestones.isMilestone(730))
        assertTrue(Milestones.isMilestone(1000))
        assertFalse(Milestones.isMilestone(31))
        assertFalse(Milestones.isMilestone(0))
        assertEquals(listOf(1L, 3, 7, 14, 21, 30, 60, 90, 100), Milestones.upTo(100))
    }

    @Test
    fun milestoneNames() {
        assertEquals("1 week", Milestones.name(7))
        assertEquals("3 weeks", Milestones.name(21))
        assertEquals("1 year", Milestones.name(365))
        assertEquals("2 years", Milestones.name(730))
        assertEquals(null, Milestones.name(100))
    }

    @Test
    fun describesSpan() {
        assertEquals("Day one", describeSpan(start, start))
        assertEquals("1 day", describeSpan(start, start.plusDays(1)))
        assertEquals("1 year, 2 months, 3 days", describeSpan(start, LocalDate.of(2027, 3, 4)))
    }

    @Test
    fun codecRoundTrips() {
        val goals = listOf(
            goal.copy(reason = "for my kids", createdAt = 2).recordSlip(start.plusDays(12), 4),
            Goal("x", "", start, createdAt = 3, updatedAt = 8, deleted = true),
        )
        assertEquals(goals, LedgerCodec.decode(LedgerCodec.encode(goals)))
    }

    @Test
    fun codecReadsVersionOne() {
        // Started Jan 1, slipped Jan 11 and Jan 21; current run from Jan 21.
        val v1 = """{"version":1,"goals":[{"id":"g","title":"T","start":${start.plusDays(20).toEpochDay()},
            "history":[[${start.toEpochDay()},${start.plusDays(10).toEpochDay()}],
                       [${start.plusDays(10).toEpochDay()},${start.plusDays(20).toEpochDay()}]],
            "created":1,"updated":2,"deleted":false}]}"""
        val g = LedgerCodec.decode(v1).single()
        assertEquals(start, g.since)
        assertEquals(listOf(start.plusDays(10), start.plusDays(20)), g.slips)
        assertEquals(start.plusDays(20), g.currentStart)
        assertEquals("", g.reason)
    }
}
