package com.stagirite.ledger.data

import java.time.LocalDate

/** One goal's week. */
data class GoalWeek(
    val goal: Goal,
    /** Days the goal was due this week (rest days and days before it began don't count). */
    val due: Long,
    val kept: Long,
    val slips: Int,
    val urges: Int,
    /** Milestones reached during the week, in days. */
    val milestones: List<Long>,
    /** Its count at the end of the week. */
    val runAtEnd: Long,
)

/** The seven days ending [end], with the seven before for comparison. */
data class WeekReview(
    val start: LocalDate,
    val end: LocalDate,
    val goals: List<GoalWeek>,
    /** Days on which no goal slipped, out of days with any goal due. */
    val cleanDays: Int,
    val activeDays: Int,
    val slips: Int,
    val urges: Int,
    val slipsBefore: Int,
    val urgesBefore: Int,
) {
    val milestones: Int get() = goals.sumOf { it.milestones.size }
}

fun weekReview(goals: List<Goal>, end: LocalDate): WeekReview {
    val start = end.minusDays(6)
    val days = (0L..6L).map { start.plusDays(it) }
    val perGoal = goals.filter { !it.since.isAfter(end) }.map { g ->
        val inWeek = { d: LocalDate -> !d.isBefore(start) && !d.isAfter(end) }
        val from = maxOf(start, g.since)
        val due = g.scheduledDaysBetween(from, end)
        val slips = g.slips.count(inWeek)
        GoalWeek(
            goal = g,
            due = due,
            kept = due - slips,
            slips = slips,
            urges = g.urges.filterKeys(inWeek).values.sum(),
            milestones = g.runs(end).flatMap { run ->
                Milestones.upTo(run.days, run.start).filter { m -> inWeek(run.start.plusDays(m)) }
            },
            runAtEnd = g.daysKept(end),
        )
    }
    val states = days.map { overviewDayState(goals, it, end) }
    val prevStart = start.minusDays(7)
    val inPrev = { d: LocalDate -> !d.isBefore(prevStart) && d.isBefore(start) }
    return WeekReview(
        start = start,
        end = end,
        goals = perGoal,
        cleanDays = states.count { it == DayState.Kept },
        activeDays = states.count { it == DayState.Kept || it == DayState.Slip },
        slips = perGoal.sumOf { it.slips },
        urges = perGoal.sumOf { it.urges },
        slipsBefore = goals.sumOf { g -> g.slips.count(inPrev) },
        urgesBefore = goals.sumOf { g -> g.urges.filterKeys(inPrev).values.sum() },
    )
}
