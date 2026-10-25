package com.stagirite.ledger

import org.junit.Assert.assertEquals
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
    }

    @Test
    fun neverNegative() {
        assertEquals(0, goal.daysKept(start.minusDays(3)))
    }

    @Test
    fun resetKeepsLongestStreak() {
        val day40 = start.plusDays(40)
        val reset = goal.reset(day40)
        assertEquals(day40, reset.startDate)
        assertEquals(40, reset.bestStreak)
        assertEquals(1, reset.resets)
        assertEquals(0, reset.daysKept(day40))
        // A shorter later streak doesn't replace the best one.
        assertEquals(40, reset.reset(day40.plusDays(5)).bestStreak)
        assertEquals(50, reset.longestStreak(day40.plusDays(50)))
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
        assertEquals("Today is the first day", describeSpan(start, start))
        assertEquals("1 day", describeSpan(start, start.plusDays(1)))
        assertEquals("1 year, 2 months, 3 days", describeSpan(start, LocalDate.of(2027, 3, 4)))
    }
}
