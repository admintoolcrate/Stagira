package com.stagirite.ledger.data

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** A finished run of days, from [start] up to the day it ended. */
data class Streak(val start: LocalDate, val end: LocalDate) {
    val days: Long get() = maxOf(0, ChronoUnit.DAYS.between(start, end))
}

/**
 * A single habit being kept, e.g. "No more alcohol".
 *
 * Dates are calendar days (no time of day), so the count ticks over at local
 * midnight. [updatedAt] and [deleted] exist for sync: the newest edit of a goal
 * wins, and deletions are kept as tombstones so they spread to other devices.
 */
data class Goal(
    val id: String,
    val title: String,
    val startDate: LocalDate,
    val history: List<Streak> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deleted: Boolean = false,
) {
    /** Whole days kept since [startDate]; the start date itself counts as 0. */
    fun daysKept(today: LocalDate): Long = maxOf(0, ChronoUnit.DAYS.between(startDate, today))

    fun longestStreak(today: LocalDate): Long =
        maxOf(daysKept(today), history.maxOfOrNull { it.days } ?: 0)

    val freshStarts: Int get() = history.size

    /** Starts the count over from [today], recording the run that just ended. */
    fun reset(today: LocalDate, now: Long): Goal = copy(
        startDate = today,
        history = history + Streak(startDate, maxOf(startDate, today)),
        updatedAt = now,
    )

    fun edit(title: String, startDate: LocalDate, now: Long): Goal =
        copy(title = title.trim(), startDate = startDate, updatedAt = now)

    fun tombstone(now: Long): Goal =
        copy(title = "", history = emptyList(), deleted = true, updatedAt = now)
}

/**
 * Combines two copies of the ledger. For each goal the most recently changed
 * version wins; on a tie a deletion wins, so a delete is never undone by accident.
 */
fun mergeGoals(a: List<Goal>, b: List<Goal>): List<Goal> =
    (a + b).groupBy { it.id }
        .map { (_, versions) -> versions.maxWith(compareBy<Goal>({ it.updatedAt }, { it.deleted })) }
        .sortedWith(compareBy({ it.createdAt }, { it.id }))

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
    if (!today.isAfter(start)) return "Day one"
    val p = Period.between(start, today)
    val parts = buildList {
        if (p.years > 0) add(plural(p.years.toLong(), "year"))
        if (p.months > 0) add(plural(p.months.toLong(), "month"))
        if (p.days > 0) add(plural(p.days.toLong(), "day"))
    }
    return parts.joinToString(", ")
}

fun plural(n: Long, word: String): String = if (n == 1L) "1 $word" else "$n ${word}s"
