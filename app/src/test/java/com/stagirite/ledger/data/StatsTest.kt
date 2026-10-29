package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsTest {
    private val start = LocalDate.of(2026, 3, 1)
    private val today = start.plusDays(39) // 40 days tracked, inclusive

    // Slips on day 10 and day 30.
    private val goal = Goal("g", "T", start)
        .recordSlip(start.plusDays(10), 1)
        .recordSlip(start.plusDays(30), 2)

    @Test
    fun stats() {
        val s = goal.stats(today)
        assertEquals(9, s.current)
        assertEquals(20, s.longest)
        assertEquals((10 + 20 + 9) / 3.0, s.average, 1e-9)
        assertEquals(2, s.freshStarts)
        assertEquals(40, s.daysTracked)
        assertEquals(38, s.daysKept)
        assertEquals(0.95, s.keptRate, 1e-9)
        assertEquals(2, s.slipsLast30)
    }

    @Test
    fun slipsLast30CountsTheLast30DaysIncludingToday() {
        assertEquals(1, goal.stats(start.plusDays(39 + 10)).slipsLast30)
    }

    @Test
    fun dayStates() {
        assertEquals(DayState.Untracked, goal.dayState(start.minusDays(1), today))
        assertEquals(DayState.Kept, goal.dayState(start, today))
        assertEquals(DayState.Slip, goal.dayState(start.plusDays(10), today))
        assertEquals(DayState.Future, goal.dayState(today.plusDays(1), today))
    }

    @Test
    fun overviewDayIsASlipIfAnyGoalSlipped() {
        val other = Goal("h", "U", start.plusDays(5))
        val goals = listOf(goal, other)
        assertEquals(DayState.Kept, overviewDayState(goals, start, today))
        assertEquals(DayState.Slip, overviewDayState(goals, start.plusDays(10), today))
        assertEquals(DayState.Untracked, overviewDayState(goals, start.minusDays(1), today))
    }

    @Test
    fun upcomingMilestonesSoonestFirst() {
        val a = Goal("a", "A", today.minusDays(29)) // 29 days: 30 is 1 away
        val b = Goal("b", "B", today.minusDays(8)) // 8 days: 10 is 2 away
        val up = upcomingMilestones(listOf(b, a), today)
        assertEquals(listOf("a", "b"), up.map { it.goal.id })
        assertEquals(listOf(1L, 2L), up.map { it.inDays })
        assertEquals(listOf(30L, 10L), up.map { it.milestone })
    }
}
