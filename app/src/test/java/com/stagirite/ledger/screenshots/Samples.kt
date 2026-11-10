package com.stagirite.ledger.screenshots

import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.ui.PrivacyUi
import java.time.DayOfWeek
import java.time.LocalDate

/** Made-up goals for the store and review screenshots. */
object Samples {
    val today = LocalDate.of(2026, 11, 14)
    fun ago(n: Long) = today.minusDays(n)

    val drink = Goal(
        "drink", "Drink less", ago(200),
        slips = listOf(ago(151), ago(97), ago(64)),
        reason = "Clearer mornings, and more of them.",
        icon = "🍷", kind = GoalKind.Vice,
        urges = mapOf(ago(1) to 1, ago(3) to 2, ago(6) to 1, ago(10) to 1, ago(17) to 1),
    )
    val phone = Goal("phone", "No phone in bed", ago(21), icon = "📵", kind = GoalKind.Vice)
    val read = Goal(
        "read", "Read before bed", ago(120),
        slips = listOf(ago(58), ago(12)),
        icon = "📖", kind = GoalKind.Virtue,
    )
    val walk = Goal(
        "walk", "Morning walk", ago(90),
        slips = listOf(ago(40), ago(10)),
        icon = "🚶", kind = GoalKind.Virtue,
        schedule = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
    )
    val goals = listOf(drink, phone, read, walk)
    val privacy = PrivacyUi(hasPin = false, unlocked = false, requestPin = {}, open = { null })
}
