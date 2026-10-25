package com.stagirite.ledger

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * A single habit being kept, e.g. "No more alcohol".
 *
 * Dates are stored as calendar days (no time of day), so the count ticks over
 * at local midnight and isn't thrown off by time zones or clock changes.
 */
data class Goal(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    val bestStreak: Long = 0,
    val resets: Int = 0,
) {
    /** Whole days kept since [startDate]. Day one is the start date itself, which counts as 0. */
    fun daysKept(today: LocalDate): Long = maxOf(0, ChronoUnit.DAYS.between(startDate, today))

    fun longestStreak(today: LocalDate): Long = maxOf(bestStreak, daysKept(today))

    /** Starts the count over from [today], remembering the streak that just ended. */
    fun reset(today: LocalDate): Goal = copy(
        startDate = today,
        bestStreak = longestStreak(today),
        resets = resets + 1,
    )
}

object Milestones {
    private val fixed = listOf(1L, 3, 7, 14, 21, 30, 60, 90, 180, 270, 365)

    /** The next milestone strictly greater than [days]; beyond a year, every further year. */
    fun next(days: Long): Long =
        fixed.firstOrNull { it > days } ?: ((days / 365) + 1) * 365

    /** The most recent milestone reached, or 0 if none yet. */
    fun previous(days: Long): Long =
        if (days < 365) fixed.lastOrNull { it <= days } ?: 0 else (days / 365) * 365

    /** Fraction of the way from the previous milestone to the next one, 0..1. */
    fun progress(days: Long): Float {
        val prev = previous(days)
        val next = next(days)
        return ((days - prev).toFloat() / (next - prev).toFloat()).coerceIn(0f, 1f)
    }
}

/** "1 year, 2 months, 3 days"; empty parts are left out. */
fun describeSpan(start: LocalDate, today: LocalDate): String {
    if (!today.isAfter(start)) return "Today is the first day"
    val p = Period.between(start, today)
    val parts = buildList {
        if (p.years > 0) add(plural(p.years.toLong(), "year"))
        if (p.months > 0) add(plural(p.months.toLong(), "month"))
        if (p.days > 0) add(plural(p.days.toLong(), "day"))
    }
    return parts.joinToString(", ")
}

fun plural(n: Long, word: String): String = if (n == 1L) "1 $word" else "$n ${word}s"
