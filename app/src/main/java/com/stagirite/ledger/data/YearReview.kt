package com.stagirite.ledger.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One goal's year: days kept, its longest run within the year, and slips. */
data class YearGoalLine(val goal: Goal, val daysKept: Long, val bestRun: Long, val slips: Int)

/** A calendar year across every goal, up to today for the current year. */
data class YearReview(
    val year: Int,
    /** Days on which every goal being tracked was kept. */
    val daysAllKept: Long,
    /** Days on which at least one goal was being tracked (and due). */
    val daysTracked: Long,
    val milestones: Int,
    val urges: Int,
    /** Most days kept first. */
    val lines: List<YearGoalLine>,
) {
    /** The longest run any goal had within the year. */
    val best: YearGoalLine? get() = lines.maxByOrNull { it.bestRun }?.takeIf { it.bestRun > 0 }
}

/** Which year to show by default: last year until mid-January, so New Year's Day isn't an empty card. */
fun defaultReviewYear(today: LocalDate): Int = if (today.dayOfYear <= 14) today.year - 1 else today.year

/** The review of [year], or null when no goal was tracked in it. */
fun yearReview(goals: List<Goal>, year: Int, today: LocalDate): YearReview? {
    val from = LocalDate.of(year, 1, 1)
    val to = minOf(LocalDate.of(year, 12, 31), today)
    if (to.isBefore(from)) return null
    val inYear = goals.filter { !it.deleted && !it.since.isAfter(to) }
    if (inYear.isEmpty()) return null

    val lines = inYear.map { g ->
        val start = maxOf(g.since, from)
        val slips = g.slips.count { !it.isBefore(start) && !it.isAfter(to) }
        // A run's days within the year: a run still going on 31 December counts that day too.
        val end = if (to == today) today else to.plusDays(1)
        val bestRun = g.runs(today).maxOfOrNull { r ->
            val s = maxOf(r.start, from)
            val e = minOf(r.end, end)
            maxOf(0, ChronoUnit.DAYS.between(s, e))
        } ?: 0
        YearGoalLine(g, g.scheduledDaysBetween(start, to) - slips, bestRun, slips)
    }.sortedByDescending { it.daysKept }

    var allKept = 0L
    var tracked = 0L
    var d = from
    while (!d.isAfter(to)) {
        when (overviewDayState(inYear, d, today)) {
            DayState.Kept -> { allKept++; tracked++ }
            DayState.Slip -> tracked++
            else -> {}
        }
        d = d.plusDays(1)
    }

    // Milestones whose day fell within the year. A finished run ends on its slip, so it
    // never reached the milestone that would have fallen on that day.
    fun reachedIn(start: LocalDate, days: Long, finished: Boolean): Int =
        Milestones.upTo(days, start).count { m ->
            val day = start.plusDays(m)
            (!finished || m < days) && !day.isBefore(from) && !day.isAfter(to)
        }
    val milestones = inYear.sumOf { g ->
        g.history.sumOf { reachedIn(it.start, it.days, finished = true) } +
            reachedIn(g.currentStart, g.daysKept(today), finished = false)
    }
    val urges = inYear.sumOf { g -> g.urges.filterKeys { it.year == year && !it.isAfter(today) }.values.sum() }

    return YearReview(year, allKept, tracked, milestones, urges, lines)
}
