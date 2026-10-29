package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ProgressTest {
    private val start = LocalDate.of(2026, 1, 1)

    /** A goal whose runs have the given lengths, in order; the last value is the current run. */
    private fun goalWithRuns(vararg lengths: Long): Pair<Goal, LocalDate> {
        var g = Goal("g", "T", start)
        var day = start
        lengths.dropLast(1).forEachIndexed { i, len ->
            day = day.plusDays(len)
            g = g.recordSlip(day, i + 1L)
        }
        return g to day.plusDays(lengths.last())
    }

    @Test
    fun medianAndTrend() {
        assertEquals(null, median(emptyList()))
        assertEquals(3.0, median(listOf(5, 1, 3))!!, 0.0)
        assertEquals(2.5, median(listOf(4, 1, 3, 2))!!, 0.0)
        val t = linearTrend(listOf(2, 4, 6, 8))!!
        assertEquals(2.0, t.slope, 1e-9)
        assertEquals(2.0, t.intercept, 1e-9)
        assertNull(linearTrend(listOf(5)))
    }

    @Test
    fun longerRunsAndFewerSlipsIsImproving() {
        val (g, today) = goalWithRuns(2, 3, 2, 3, 10, 12, 15, 16)
        val p = g.progress(today)
        assertEquals(Verdict.Improving, p.verdict)
        assertEquals(15.0, p.recentMedian!!, 0.0) // 12, 15, 16 (current counts: 16 ≥ typical)
        assertEquals(3.0, p.earlierMedian!!, 0.0) // 2, 3, 10 → median 3
        assertTrue(p.trend!!.slope > 0)
    }

    @Test
    fun shorterRunsIsHarderLately() {
        val (g, today) = goalWithRuns(30, 28, 31, 4, 3, 2, 1)
        assertEquals(Verdict.Harder, g.progress(today).verdict)
    }

    @Test
    fun dayAfterASlipDoesNotDragTheTrendDown() {
        // A long run just ended; the new current run (0 days) is left out.
        val (g, today) = goalWithRuns(5, 6, 5, 20, 22, 25, 0)
        val p = g.progress(today)
        assertEquals(6, p.series.size)
        assertEquals(22.0, p.recentMedian!!, 0.0)
    }

    @Test
    fun neverSlippedIsUnbroken() {
        val g = Goal("g", "T", start)
        assertEquals(Verdict.Unbroken, g.progress(start.plusDays(40)).verdict)
        assertEquals(Verdict.TooSoon, g.progress(start).verdict)
    }

    @Test
    fun tooFewRunsIsTooSoonUntilWindowsHelp() {
        val (g, today) = goalWithRuns(3, 4) // 7 days tracked: windows too short too
        val p = g.progress(today)
        assertEquals(Verdict.TooSoon, p.verdict)
        assertNull(p.recentMedian)
        assertNull(p.slipsRecent)
    }

    @Test
    fun slipWindowsAndBackToBackSlips() {
        // 200 days tracked → 90-day windows. Slips: days 20, 22 (close), 60, 150, 152 (close).
        var g = Goal("g", "T", start)
        listOf(20L, 22, 60, 150, 152).forEach { g = g.recordSlip(start.plusDays(it), 1) }
        val today = start.plusDays(199)
        val p = g.progress(today)
        assertEquals(90, p.windowDays)
        // Recent window: days 110..199 → slips 150, 152. Before: days 20..109 → 20, 22, 60.
        assertEquals(2, p.slipsRecent)
        assertEquals(3, p.slipsBefore)
        assertEquals(1, p.closeRecent)
        assertEquals(1, p.closeBefore)
    }

    @Test
    fun newBestsCountRunsThatBeatAllBefore() {
        val (g, today) = goalWithRuns(5, 3, 8, 8, 12, 2, 13)
        // 8 beats 5, 12 beats 8, current 13 beats 12; the second 8 only ties.
        assertEquals(3, g.progress(today).newBests)
    }

    @Test
    fun monthsCoverOnlyTrackedDays() {
        val since = LocalDate.of(2026, 3, 20)
        val g = Goal("g", "T", since).recordSlip(LocalDate.of(2026, 3, 25), 1).recordSlip(LocalDate.of(2026, 4, 2), 2)
        val months = g.progress(LocalDate.of(2026, 4, 10)).months
        assertEquals(listOf(YearMonth.of(2026, 3), YearMonth.of(2026, 4)), months.map { it.month })
        assertEquals(MonthKept(YearMonth.of(2026, 3), 11, 12), months[0]) // 20–31 March, one slip
        assertEquals(MonthKept(YearMonth.of(2026, 4), 9, 10), months[1]) // 1–10 April, one slip
        assertNotNull(months[1].rate)
    }

    @Test
    fun monthsGoBackSixAtMost() {
        val g = Goal("g", "T", LocalDate.of(2025, 1, 1))
        val months = g.progress(LocalDate.of(2026, 10, 25)).months
        assertEquals(6, months.size)
        assertEquals(YearMonth.of(2026, 5), months.first().month)
    }
}
