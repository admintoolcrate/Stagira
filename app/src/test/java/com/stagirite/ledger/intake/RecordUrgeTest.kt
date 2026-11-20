package com.stagirite.ledger.intake

import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RecordUrgeTest {
    private val today = LocalDate.of(2026, 11, 19)
    private fun at(d: LocalDate) = d.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test fun `today and yesterday are kept`() {
        assertEquals(today, RecordUrgeActivity.dayFor(at(today), today, ZoneOffset.UTC))
        assertEquals(today.minusDays(1), RecordUrgeActivity.dayFor(at(today.minusDays(1)), today, ZoneOffset.UTC))
    }

    @Test fun `older, future or missing times become today`() {
        assertEquals(today, RecordUrgeActivity.dayFor(at(today.minusDays(5)), today, ZoneOffset.UTC))
        assertEquals(today, RecordUrgeActivity.dayFor(at(today.plusDays(1)), today, ZoneOffset.UTC))
        assertEquals(today, RecordUrgeActivity.dayFor(-1, today, ZoneOffset.UTC))
    }

    @Test fun `last chosen goal first, then vices`() {
        val since = LocalDate.of(2026, 1, 1)
        val virtue = Goal("a", "Read", since, kind = GoalKind.Virtue)
        val vice = Goal("b", "No alcohol", since, kind = GoalKind.Vice)
        val plain = Goal("c", "Walk", since)
        assertEquals(listOf("b", "a", "c"), RecordUrgeActivity.ordered(listOf(virtue, vice, plain), null).map { it.id })
        assertEquals(listOf("c", "b", "a"), RecordUrgeActivity.ordered(listOf(virtue, vice, plain), "c").map { it.id })
    }
}
