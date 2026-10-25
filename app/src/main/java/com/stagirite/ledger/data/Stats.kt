package com.stagirite.ledger.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Figures for one goal, as of [today]. */
data class GoalStats(
    val current: Long,
    val longest: Long,
    /** Mean length of all runs, including the current one. */
    val average: Double,
    val freshStarts: Int,
    /** Days from [Goal.since] through today, inclusive. */
    val daysTracked: Long,
    /** Tracked days that weren't slips. */
    val daysKept: Long,
    val slipsLast30: Int,
    val runs: List<Streak>,
    val urgesTotal: Int,
    val urgesLast30: Int,
    /** Urges resisted since the current run began. */
    val urgesThisRun: Int,
) {
    /** Share of tracked days kept, 0..1. */
    val keptRate: Double get() = if (daysTracked == 0L) 1.0 else daysKept.toDouble() / daysTracked
}

fun Goal.stats(today: LocalDate): GoalStats {
    val runs = runs(today)
    val tracked = maxOf(0, ChronoUnit.DAYS.between(since, today) + 1)
    val slipsSoFar = slips.count { !it.isAfter(today) }
    val monthAgo = today.minusDays(29)
    return GoalStats(
        current = daysKept(today),
        longest = runs.maxOf { it.days },
        average = runs.map { it.days }.average(),
        freshStarts = freshStarts,
        daysTracked = tracked,
        daysKept = tracked - slipsSoFar,
        slipsLast30 = slips.count { !it.isBefore(monthAgo) && !it.isAfter(today) },
        runs = runs,
        urgesTotal = urges.filterKeys { !it.isAfter(today) }.values.sum(),
        urgesLast30 = urges.filterKeys { !it.isBefore(monthAgo) && !it.isAfter(today) }.values.sum(),
        urgesThisRun = urges.filterKeys { !it.isBefore(currentStart) && !it.isAfter(today) }.values.sum(),
    )
}

enum class DayState { Untracked, Kept, Slip, Future }

fun Goal.dayState(day: LocalDate, today: LocalDate): DayState = when {
    day.isAfter(today) -> DayState.Future
    day.isBefore(since) -> DayState.Untracked
    isSlip(day) -> DayState.Slip
    else -> DayState.Kept
}

/** Across every goal: how a day went for all the goals being tracked then. */
fun overviewDayState(goals: List<Goal>, day: LocalDate, today: LocalDate): DayState {
    if (day.isAfter(today)) return DayState.Future
    val states = goals.map { it.dayState(day, today) }.filter { it != DayState.Untracked }
    return when {
        states.isEmpty() -> DayState.Untracked
        DayState.Slip in states -> DayState.Slip
        else -> DayState.Kept
    }
}

data class UpcomingMilestone(val goal: Goal, val milestone: Long, val inDays: Long)

/** Each goal's next milestone, soonest first. */
fun upcomingMilestones(goals: List<Goal>, today: LocalDate): List<UpcomingMilestone> =
    goals.map {
        val days = it.daysKept(today)
        val next = Milestones.next(days)
        UpcomingMilestone(it, next, next - days)
    }.sortedBy { it.inDays }

/** Orders goals for the list. Names compare as shown, so cover names sort as covers. */
fun sortGoals(goals: List<Goal>, order: SortOrder, today: LocalDate): List<Goal> = when (order) {
    SortOrder.Added -> goals
    SortOrder.LongestRun -> goals.sortedByDescending { it.daysKept(today) }
    SortOrder.NextMilestone -> goals.sortedBy { Milestones.next(it.daysKept(today)) - it.daysKept(today) }
    SortOrder.Name -> goals.sortedBy { it.title.lowercase() }
}
