package com.stagirite.ledger.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class Verdict(val label: String) {
    Unbroken("Unbroken"),
    Improving("Improving"),
    Steady("Steady"),
    Harder("Harder lately"),
    TooSoon("Not enough history yet"),
}

/** Share of tracked days kept in one calendar month (only the tracked part of it). */
data class MonthKept(val month: YearMonth, val kept: Long, val tracked: Long) {
    val rate: Double get() = if (tracked == 0L) 0.0 else kept.toDouble() / tracked
}

/** A straight line through run lengths: length ≈ intercept + slope × run number. */
data class Trend(val intercept: Double, val slope: Double) {
    fun at(index: Int): Double = intercept + slope * index
}

/**
 * How a goal is going over time, measured by the gaps between slips.
 *
 * Runs here are the finished runs, plus the current one once it's at least as
 * long as a typical finished run. That keeps the day after a slip from looking
 * like a collapse, while a long current run still counts as progress.
 */
data class Progress(
    val verdict: Verdict,
    /** The runs the measures below are taken from, oldest first. */
    val series: List<Streak>,
    /** Median of the last [RECENT] runs, and of the [RECENT] before them. */
    val recentMedian: Double?,
    val earlierMedian: Double?,
    /** Slips in the last [windowDays] days, and in the same span before that. */
    val windowDays: Long,
    val slipsRecent: Int?,
    val slipsBefore: Int?,
    /** Of those, slips within [CLOSE_DAYS] days of the slip before. */
    val closeRecent: Int?,
    val closeBefore: Int?,
    /** Urges resisted in the recent window and the one before; never counts against the verdict. */
    val urgesRecent: Int? = null,
    val urgesBefore: Int? = null,
    /** Runs that beat every run before them (the first run doesn't count). */
    val newBests: Int,
    val medianRun: Double?,
    val trend: Trend?,
    val months: List<MonthKept>,
) {
    companion object {
        const val RECENT = 3
        const val CLOSE_DAYS = 3
        const val MAX_WINDOW = 90L
        const val MIN_WINDOW = 14L
    }
}

fun median(values: List<Long>): Double? {
    if (values.isEmpty()) return null
    val s = values.sorted()
    val mid = s.size / 2
    return if (s.size % 2 == 1) s[mid].toDouble() else (s[mid - 1] + s[mid]) / 2.0
}

/** Least-squares line through (i, values[i]); null with fewer than two points. */
fun linearTrend(values: List<Long>): Trend? {
    if (values.size < 2) return null
    val n = values.size.toDouble()
    val xs = values.indices.map { it.toDouble() }
    val meanX = xs.average()
    val meanY = values.average()
    val sxx = xs.sumOf { (it - meanX) * (it - meanX) }
    val sxy = xs.indices.sumOf { (xs[it] - meanX) * (values[it] - meanY) }
    val slope = if (sxx == 0.0) 0.0 else sxy / sxx
    return Trend(meanY - slope * meanX, slope).takeIf { n >= 2 }
}

fun Goal.progress(today: LocalDate, monthsBack: Int = 6): Progress {
    val finished = history
    val current = Streak(currentStart, maxOf(currentStart, today))
    val typical = median(finished.map { it.days })
    val series = if (typical == null || current.days >= typical) finished + current else finished
    val lengths = series.map { it.days }

    // 1. Recent runs against the ones before.
    val recent = lengths.takeLast(Progress.RECENT)
    val earlier = lengths.dropLast(Progress.RECENT).takeLast(Progress.RECENT)
    val recentMedian = if (earlier.isEmpty()) null else median(recent)
    val earlierMedian = if (earlier.isEmpty()) null else median(earlier)

    // 2 and 5. Slips, and back-to-back slips, in two equal windows.
    val tracked = ChronoUnit.DAYS.between(since, today) + 1
    val window = minOf(Progress.MAX_WINDOW, tracked / 2)
    val haveWindows = window >= Progress.MIN_WINDOW
    val recentFrom = today.minusDays(window - 1)
    val beforeFrom = recentFrom.minusDays(window)
    fun inRange(d: LocalDate, from: LocalDate, until: LocalDate) = !d.isBefore(from) && d.isBefore(until)
    val past = slips.filter { !it.isAfter(today) }
    val close = past.zipWithNext().filter { (a, b) -> ChronoUnit.DAYS.between(a, b) <= Progress.CLOSE_DAYS }.map { it.second }
    val slipsRecent = if (haveWindows) past.count { inRange(it, recentFrom, today.plusDays(1)) } else null
    val slipsBefore = if (haveWindows) past.count { inRange(it, beforeFrom, recentFrom) } else null
    val closeRecent = if (haveWindows) close.count { inRange(it, recentFrom, today.plusDays(1)) } else null
    val closeBefore = if (haveWindows) close.count { inRange(it, beforeFrom, recentFrom) } else null
    val urgesRecent = if (haveWindows) urges.filterKeys { inRange(it, recentFrom, today.plusDays(1)) }.values.sum() else null
    val urgesBefore = if (haveWindows) urges.filterKeys { inRange(it, beforeFrom, recentFrom) }.values.sum() else null

    // 4. New personal bests, counting the current run if it has already set one.
    val all = runs(today).map { it.days }
    var best = all.firstOrNull() ?: 0
    var newBests = 0
    all.drop(1).forEach { if (it > best) { newBests++; best = it } }

    // 6. Kept days per month.
    val firstMonth = maxOf(YearMonth.from(since), YearMonth.from(today).minusMonths(monthsBack - 1L))
    val months = generateSequence(firstMonth) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(YearMonth.from(today)) }
        .map { m ->
            val from = maxOf(m.atDay(1), since)
            val until = minOf(m.atEndOfMonth(), today)
            val days = ChronoUnit.DAYS.between(from, until) + 1
            MonthKept(m, days - past.count { !it.isBefore(from) && !it.isAfter(until) }, days)
        }.toList()

    // The verdict: run lengths and slip frequency, each a vote.
    val votes = buildList {
        if (recentMedian != null && earlierMedian != null) {
            val ratio = recentMedian / maxOf(earlierMedian, 1.0)
            add(if (ratio >= 1.25) 1 else if (ratio <= 0.8) -1 else 0)
        }
        if (slipsRecent != null && slipsBefore != null && slipsRecent + slipsBefore > 0) {
            add(slipsBefore.compareTo(slipsRecent).coerceIn(-1, 1))
        }
    }
    val verdict = when {
        past.isEmpty() && tracked > 1 -> Verdict.Unbroken
        votes.isEmpty() -> Verdict.TooSoon
        votes.sum() >= 1 && votes.none { it < 0 } -> Verdict.Improving
        votes.sum() <= -1 && votes.none { it > 0 } -> Verdict.Harder
        else -> Verdict.Steady
    }

    return Progress(
        verdict = verdict,
        series = series,
        recentMedian = recentMedian,
        earlierMedian = earlierMedian,
        windowDays = window,
        slipsRecent = slipsRecent,
        slipsBefore = slipsBefore,
        closeRecent = closeRecent,
        closeBefore = closeBefore,
        urgesRecent = urgesRecent,
        urgesBefore = urgesBefore,
        newBests = newBests,
        medianRun = median(finished.map { it.days }),
        trend = linearTrend(finished.map { it.days }),
        months = months,
    )
}
