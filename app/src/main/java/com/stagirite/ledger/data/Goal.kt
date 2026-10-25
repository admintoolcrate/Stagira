package com.stagirite.ledger.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/** A run of days kept, from [start] up to [end] (a slip, or today for the current run). */
data class Streak(val start: LocalDate, val end: LocalDate) {
    val days: Long get() = maxOf(0, ChronoUnit.DAYS.between(start, end))
}

/** Optionally, whether a goal is a vice to do less of or a virtue to do more of. */
/**
 * The two kinds of goal. Tackling a Vice means stopping or cutting down
 * (drinking less); practising a Virtue means doing something good each day
 * (one good deed a day). Either way a day counts as kept unless it's logged:
 * a slip for a Vice, a missed day for a Virtue.
 */
enum class GoalKind(
    val label: String,
    /** How it's offered when making a goal. */
    val choice: String,
    val blurb: String,
    val slip: String,
    val slips: String,
    /** The button for logging a moment of resolve (Vices only). */
    val urge: String,
    /** What it's called in stats: "Urges resisted". */
    val urges: String,
) {
    Vice(
        "Vice", "Tackle a vice",
        "Stop or cut down on something, like drinking less. Each day counts until you slip.",
        "slip", "slips", "Resisted an urge", "Urges resisted",
    ),
    Virtue(
        "Virtue", "Practise a virtue",
        "Do something good each day, like one good deed. Each day counts unless you miss it.",
        "missed day", "missed days", "Did it anyway", "Times you did it anyway",
    ),
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
    /** An emoji shown beside the title, drawn in monochrome. */
    val icon: String = "",
    val kind: GoalKind? = null,
    /**
     * For a private goal: the real title and note, encrypted with the private-titles
     * PIN. [title] is then the cover name shown to anyone else, and [reason] is empty.
     */
    val sealed: String? = null,
    /** Optional aim in days, e.g. 90. */
    val target: Long? = null,
    /** Hidden from the list, overview and widget, but kept. */
    val archived: Boolean = false,
    /**
     * Notes on slips, by the day of the slip. Empty for a private goal, whose
     * notes are sealed along with its real title.
     */
    val notes: Map<LocalDate, String> = emptyMap(),
    /** Urges resisted (or, for a Virtue, times done despite wanting to skip), counted per day. */
    val urges: Map<LocalDate, Int> = emptyMap(),
    /**
     * The weekdays a goal is due on; empty means every day. Other days are rest
     * days: they can't be missed and don't count either way.
     */
    val schedule: Set<DayOfWeek> = emptySet(),
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

    val isPrivate: Boolean get() = sealed != null

    /** "slip" for a vice or an unlabelled goal, "missed day" for a virtue. */
    val slipWord: String get() = kind?.slip ?: "slip"

    /** The urge button's wording for this goal. */
    val urgeLabel: String get() = kind?.urge ?: GoalKind.Vice.urge
    val urgesLabel: String get() = kind?.urges ?: GoalKind.Vice.urges

    fun urgesOn(day: LocalDate): Int = urges[day] ?: 0

    val hasSchedule: Boolean get() = schedule.isNotEmpty() && schedule.size < 7

    /** Whether the goal is due on [day] (always, without a schedule). */
    fun isScheduled(day: LocalDate): Boolean = !hasSchedule || day.dayOfWeek in schedule

    /** Logs one more urge resisted on [day]. */
    fun resistUrge(day: LocalDate, now: Long): Goal =
        if (day.isBefore(since)) this else copy(urges = urges + (day to urgesOn(day) + 1), updatedAt = now)

    /** Takes one urge back off [day] (for Undo). */
    fun unresistUrge(day: LocalDate, now: Long): Goal {
        val n = urgesOn(day)
        if (n == 0) return this
        return copy(urges = if (n == 1) urges - day else urges + (day to n - 1), updatedAt = now)
    }

    fun isSlip(day: LocalDate): Boolean = slips.binarySearch(day) >= 0

    /** Records a slip on [day] (today or any earlier day since the goal began). */
    fun recordSlip(day: LocalDate, now: Long): Goal {
        // A rest day can't be missed.
        if (day.isBefore(since) || isSlip(day) || !isScheduled(day)) return this
        return copy(slips = (slips + day).sorted(), updatedAt = now)
    }

    fun removeSlip(day: LocalDate, now: Long): Goal =
        if (!isSlip(day)) this else copy(slips = slips - day, notes = notes - day, updatedAt = now)

    /** Sets or (with blank [text]) clears the note on the slip on [day]. */
    fun withNote(day: LocalDate, text: String, now: Long): Goal {
        // A private goal's notes live in its sealed contents, never here in the clear.
        if (!isSlip(day) || isPrivate) return this
        val trimmed = text.trim()
        val updated = if (trimmed.isEmpty()) notes - day else notes + (day to trimmed)
        return if (updated == notes) this else copy(notes = updated, updatedAt = now)
    }

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
        val note = notes[slips.last()]
        val movedNotes = (notes - slips.last()).let { if (note != null) it + (moved to note) else it }
        return copy(slips = slips.dropLast(1) + moved, notes = movedNotes, updatedAt = now)
    }

    /** [since] can't move past the first slip. */
    fun edit(
        title: String,
        since: LocalDate,
        reason: String,
        now: Long,
        icon: String = this.icon,
        kind: GoalKind? = this.kind,
        sealed: String? = this.sealed,
        target: Long? = this.target,
        notes: Map<LocalDate, String> = this.notes,
        schedule: Set<DayOfWeek> = this.schedule,
    ): Goal = copy(
        title = title.trim(),
        since = slips.firstOrNull()?.let { minOf(it, since) } ?: since,
        reason = if (sealed != null) "" else reason.trim(),
        icon = icon,
        kind = kind,
        sealed = sealed,
        target = target?.takeIf { it > 0 },
        // Sealed goals keep their notes inside the seal.
        notes = if (sealed != null) emptyMap() else notes.filterKeys { it in slips },
        // Every day is the same as no schedule.
        schedule = if (schedule.size == 7) emptySet() else schedule,
        updatedAt = now,
    )

    fun tombstone(now: Long): Goal = copy(
        title = "", slips = emptyList(), reason = "", icon = "", kind = null, sealed = null,
        target = null, archived = false, notes = emptyMap(), urges = emptyMap(), schedule = emptySet(),
        deleted = true, updatedAt = now,
    )

    /** Progress towards [target], 0..1, or null with no target. */
    fun targetProgress(today: LocalDate): Float? =
        target?.let { (daysKept(today).toFloat() / it).coerceIn(0f, 1f) }
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
