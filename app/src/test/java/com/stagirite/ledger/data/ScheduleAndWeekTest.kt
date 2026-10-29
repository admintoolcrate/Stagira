package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class ScheduleAndWeekTest {
    // 5 January 2026 is a Monday.
    private val monday = LocalDate.of(2026, 1, 5)
    private val mwf = Goal("g", "Exercise", monday, kind = GoalKind.Virtue, schedule = setOf(MONDAY, WEDNESDAY, FRIDAY))

    @Test
    fun restDaysCantBeMissed() {
        val tuesday = monday.plusDays(1)
        assertTrue(mwf.isScheduled(monday))
        assertFalse(mwf.isScheduled(tuesday))
        assertSame(mwf, mwf.recordSlip(tuesday, 1))
        assertEquals(listOf(monday.plusDays(2)), mwf.recordSlip(monday.plusDays(2), 1).slips)
    }

    @Test
    fun restDaysShowAsRestAndDontCount() {
        val sunday = monday.plusDays(6)
        assertEquals(DayState.Rest, mwf.dayState(monday.plusDays(1), sunday))
        assertEquals(DayState.Kept, mwf.dayState(monday, sunday))
        // Two weeks: 6 sessions due, one missed.
        val g = mwf.recordSlip(monday.plusDays(4), 1)
        val s = g.stats(monday.plusDays(13))
        assertEquals(6, s.daysTracked)
        assertEquals(5, s.daysKept)
    }

    @Test
    fun everyDayIsTheSameAsNoSchedule() {
        val all = java.time.DayOfWeek.entries.toSet()
        assertEquals(emptySet<java.time.DayOfWeek>(), mwf.edit("x", monday, "", 1, schedule = all).schedule)
        assertTrue(Goal("h", "T", monday).isScheduled(monday.plusDays(1)))
    }

    @Test
    fun codecKeepsSchedule() {
        val g = mwf.copy(createdAt = 1)
        assertEquals(listOf(g), LedgerCodec.decode(LedgerCodec.encode(listOf(g))))
    }

    @Test
    fun overviewIgnoresRestDays() {
        val daily = Goal("d", "No sugar", monday)
        val tuesday = monday.plusDays(1)
        assertEquals(DayState.Kept, overviewDayState(listOf(mwf, daily), tuesday, monday.plusDays(6)))
        assertEquals(DayState.Rest, overviewDayState(listOf(mwf), tuesday, monday.plusDays(6)))
    }

    @Test
    fun weekReviewCountsTheSevenDaysAndTheWeekBefore() {
        val start = monday.minusDays(14)
        var daily = Goal("d", "No sugar", start)
        daily = daily.recordSlip(start.plusDays(9), 1) // the week before
        daily = daily.recordSlip(monday.plusDays(2), 2) // this week
        daily = daily.resistUrge(monday.plusDays(1), 3).resistUrge(monday.plusDays(3), 4)
        val end = monday.plusDays(6)
        val w = weekReview(listOf(daily, mwf), end)

        assertEquals(monday, w.start)
        assertEquals(1, w.slips)
        assertEquals(1, w.slipsBefore)
        assertEquals(2, w.urges)
        assertEquals(6, w.cleanDays)
        assertEquals(7, w.activeDays)

        val d = w.goals.first { it.goal.id == "d" }
        assertEquals(7, d.due)
        assertEquals(6, d.kept)
        val e = w.goals.first { it.goal.id == "g" }
        assertEquals(3, e.due) // Mon, Wed, Fri
        assertEquals(3, e.kept)
        // The exercise goal began this Monday: 1, 2, 3 and 5 days fall this week.
        assertEquals(listOf(1L, 2L, 3L, 5L), e.milestones)
    }
}
