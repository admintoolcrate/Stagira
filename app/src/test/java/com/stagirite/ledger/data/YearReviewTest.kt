package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class YearReviewTest {
    private val today = LocalDate.of(2026, 3, 1)

    // Began 20 Dec 2025, slipped 10 Jan 2026.
    private val goal = Goal("g", "T", LocalDate.of(2025, 12, 20))
        .recordSlip(LocalDate.of(2026, 1, 10), 1)

    @Test
    fun thisYearSoFar() {
        val r = yearReview(listOf(goal), 2026, today)!!
        val line = r.lines.single()
        assertEquals(59, line.daysKept) // 1 Jan to 1 Mar is 60 days, less the slip
        assertEquals(50, line.bestRun) // 10 Jan to 1 Mar
        assertEquals(1, line.slips)
        assertEquals(59, r.daysAllKept)
        assertEquals(60, r.daysTracked)
        // Day 14 of the first run (3 Jan), then 1, 2, 3, 5, 7, 10, 14, 21, 30 and 45 of the current one.
        assertEquals(11, r.milestones)
    }

    @Test
    fun lastYearCountsOnlyItsOwnDays() {
        val r = yearReview(listOf(goal), 2025, today)!!
        val line = r.lines.single()
        assertEquals(12, line.daysKept) // 20 to 31 Dec
        assertEquals(12, line.bestRun) // the run carried on past 31 Dec
        assertEquals(0, line.slips)
        assertEquals(12, r.daysAllKept)
        assertEquals(6, r.milestones) // days 1, 2, 3, 5, 7 and 10
    }

    @Test
    fun noReviewForAYearWithNothingTracked() {
        assertNull(yearReview(listOf(goal), 2024, today))
        assertNull(yearReview(listOf(goal), 2027, today))
        assertNull(yearReview(emptyList(), 2026, today))
    }

    @Test
    fun earlyJanuaryShowsLastYear() {
        assertEquals(2025, defaultReviewYear(LocalDate.of(2026, 1, 14)))
        assertEquals(2026, defaultReviewYear(LocalDate.of(2026, 1, 15)))
    }

    @Test
    fun urgesAreCountedForTheYear() {
        val g = goal.resistUrge(LocalDate.of(2025, 12, 28), 2)
            .resistUrge(LocalDate.of(2026, 2, 1), 3)
            .resistUrge(LocalDate.of(2026, 2, 1), 4)
        assertEquals(2, yearReview(listOf(g), 2026, today)!!.urges)
        assertEquals(1, yearReview(listOf(g), 2025, today)!!.urges)
    }
}
