package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// The wording itself is checked against the source text by tools/verify_quotes.py.
class QuotesTest {
    private val day = LocalDate.of(2026, 1, 1)

    @Test
    fun everyQuoteIsCited() {
        (Quotes.slip + Quotes.milestone + Quotes.starting).forEach {
            assertTrue(it.text.isNotBlank())
            assertTrue(it.ref, Regex("""[IVX]+\.\d+, 1\d{3}[ab]""").matches(it.ref))
            assertTrue(it.citation.contains("tr. H. Rackham"))
        }
    }

    @Test
    fun virtuesGetNoPassagesAboutGivingIn() {
        (0 until 20).forEach { n ->
            val g = Goal("g$n", "Run", day, kind = GoalKind.Virtue).recordSlip(day.plusDays(n + 1L), 1)
            assertEquals(null, Quotes.forSlip(g).note)
        }
    }

    @Test
    fun quoteStaysTheSameForOneFreshStartAndChangesForTheNext() {
        val goal = Goal("g", "No more alcohol", day).recordSlip(day.plusDays(9), 1)
        assertEquals(Quotes.forSlip(goal), Quotes.forSlip(goal.copy(title = "renamed")))
        assertNotEquals(Quotes.forSlip(goal), Quotes.forSlip(goal.recordSlip(day.plusDays(12), 2)))
    }
}
