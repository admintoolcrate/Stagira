package com.stagirite.ledger.data

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** A run of days kept, from [start] up to [end] (a slip, or today for the current run). */
data class Streak(val start: LocalDate, val end: LocalDate) {
    val days: Long get() = maxOf(0, ChronoUnit.DAYS.between(start, end))
}

/**
 * A single habit being kept, e.g. "No more alcohol".
 *
 * A goal is its [since] date plus the days it slipped. Everything else (the
 * current count, past runs, stats, the calendar) is worked out from those, so a
 * slip can be recorded or removed for any past day.
 *
 * [updatedAt] and [deleted] exist for sync: the newest edit of a goal wins, and
 * deletions are kept as tombstones so they spread to other devices.
 */
data class Goal(
    val id: String,
    val title: String,
    val since: LocalDate,
    /** Sorted, distinct, none before [since]. */
    val slips: List<LocalDate> = emptyList(),
    /** Optional note on why this goal matters, shown on its page. */
    val reason: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deleted: Boolean = false,
) {
    /** The day the current run began: the latest slip, or [since]. */
    val currentStart: LocalDate get() = slips.lastOrNull() ?: since

    /** Whole days since the current run began; that day itself counts as 0. */
    fun daysKept(today: LocalDate): Long = maxOf(0, ChronoUnit.DAYS.between(currentStart, today))

    /** Finished runs, oldest first. */
    val history: List<Streak>
        get() = (listOf(since) + slips).zipWithNext { a, b -> Streak(a, b) }

    fun runs(today: LocalDate): List<Streak> = history + Streak(currentStart, maxOf(currentStart, today))

    fun longestStreak(today: LocalDate): Long = runs(today).maxOf { it.days }

    val freshStarts: Int get() = slips.size

    fun isSlip(day: LocalDate): Boolean = slips.binarySearch(day) >= 0

    /** Records a slip on [day] (today or any earlier day since the goal began). */
    fun recordSlip(day: LocalDate, now: Long): Goal {
        if (day.isBefore(since) || isSlip(day)) return this
        return copy(slips = (slips + day).sorted(), updatedAt = now)
    }

    fun removeSlip(day: LocalDate, now: Long): Goal =
        if (!isSlip(day)) this else copy(slips = slips - day, updatedAt = now)

    /**
     * The earliest day the current run could be moved back to: unlimited with no
     * slips, otherwise the day after the slip (or start) before the latest one.
     */
    val currentStartEarliest: LocalDate?
        get() = if (slips.isEmpty()) null else (slips.dropLast(1).lastOrNull() ?: since).plusDays(1)

    /**
     * Changes when the current run began. With no slips that's the goal's start
     * date; otherwise the latest slip moves to [day].
     */
    fun moveCurrentStart(day: LocalDate, now: Long): Goal {
        if (slips.isEmpty()) return if (day == since) this else copy(since = day, updatedAt = now)
        val earliest = currentStartEarliest!!
        val moved = maxOf(day, earliest)
        if (moved == slips.last()) return this
        return copy(slips = slips.dropLast(1) + moved, updatedAt = now)
    }

    /** [since] can't move past the first slip. */
    fun edit(title: String, since: LocalDate, reason: String, now: Long): Goal = copy(
        title = title.trim(),
        since = slips.firstOrNull()?.let { minOf(it, since) } ?: since,
        reason = reason.trim(),
        updatedAt = now,
    )

    fun tombstone(now: Long): Goal =
        copy(title = "", slips = emptyList(), reason = "", deleted = true, updatedAt = now)
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
    private val fixed = listOf(1L, 3, 7, 14, 21, 30, 60, 90, 100, 180, 270, 365)

    /** Every milestone up to [limit]: the fixed ladder, then each year, 500 days and every 1,000. */
    fun upTo(limit: Long): List<Long> {
        val years = (2..(limit / 365 + 1)).map { it * 365L }
        val thousands = (1..(limit / 1000 + 1)).map { it * 1000L }
        return (fixed + years + 500L + thousands).filter { it <= limit }.distinct().sorted()
    }

    /** The next milestone strictly greater than [days]. */
    fun next(days: Long): Long = upTo(days + 1000).first { it > days }

    /** The most recent milestone reached, or 0 if none yet. */
    fun previous(days: Long): Long = upTo(days).lastOrNull { it <= days } ?: 0

    fun isMilestone(days: Long): Boolean = days > 0 && previous(days) == days

    /** Fraction of the way from the previous milestone to the next one, 0..1. */
    fun progress(days: Long): Float {
        val prev = previous(days)
        val next = next(days)
        return ((days - prev).toFloat() / (next - prev).toFloat()).coerceIn(0f, 1f)
    }

    /** A friendlier name where there is one: "1 week", "2 years". */
    fun name(days: Long): String? = when {
        days == 7L -> "1 week"
        days == 14L || days == 21L -> "${days / 7} weeks"
        days % 365 == 0L -> plural(days / 365, "year")
        else -> null
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
