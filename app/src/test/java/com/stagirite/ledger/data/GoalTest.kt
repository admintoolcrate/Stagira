package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val goal = Goal(id = "g", title = "No more alcohol", startDate = start)

    @Test
    fun countsWholeDaysSinceStart() {
        assertEquals(0, goal.daysKept(start))
        assertEquals(1, goal.daysKept(start.plusDays(1)))
        assertEquals(365, goal.daysKept(LocalDate.of(2027, 1, 1)))
        assertEquals(0, goal.daysKept(start.minusDays(3)))
    }

    @Test
    fun resetRecordsHistoryAndKeepsLongestStreak() {
        val day40 = start.plusDays(40)
        val reset = goal.reset(day40, now = 5)
        assertEquals(day40, reset.startDate)
        assertEquals(listOf(Streak(start, day40)), reset.history)
        assertEquals(1, reset.freshStarts)
        assertEquals(5, reset.updatedAt)
        assertEquals(0, reset.daysKept(day40))
        assertEquals(40, reset.reset(day40.plusDays(5), now = 6).longestStreak(day40.plusDays(5)))
        assertEquals(50, reset.longestStreak(day40.plusDays(50)))
    }

    @Test
    fun tombstoneDropsContent() {
        val t = goal.reset(start.plusDays(3), 1).tombstone(now = 9)
        assertTrue(t.deleted)
        assertEquals("", t.title)
        assertEquals(emptyList<Streak>(), t.history)
        assertEquals(9, t.updatedAt)
    }

    @Test
    fun mergeKeepsNewestVersionOfEachGoal() {
        val a = Goal("a", "A", start, createdAt = 1, updatedAt = 10)
        val b = Goal("b", "B", start, createdAt = 2, updatedAt = 10)
        val aRenamed = a.edit("A2", start, now = 20)
        val bDeleted = b.tombstone(now = 30)
        val c = Goal("c", "C", start, createdAt = 3, updatedAt = 3)

        val merged = mergeGoals(listOf(aRenamed, b), listOf(a, bDeleted, c))
        assertEquals(listOf(aRenamed, bDeleted, c), merged)
        // Order of arguments doesn't matter.
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
        assertEquals(730, Milestones.next(365))
        assertEquals(1095, Milestones.next(800))
        assertEquals(0f, Milestones.progress(0), 0f)
        assertEquals(0.5f, Milestones.progress(45), 0.001f)
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
            goal.reset(start.plusDays(12), 4).copy(createdAt = 2),
            Goal("x", "", start, createdAt = 3, updatedAt = 8, deleted = true),
        )
        assertEquals(goals, LedgerCodec.decode(LedgerCodec.encode(goals)))
    }
}
