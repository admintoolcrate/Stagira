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
        (Quotes.slip + Quotes.emptyLedger).forEach {
            assertTrue(it.text.isNotBlank())
            assertTrue(it.ref, Regex("""[IVX]+\.\d+, 1\d{3}[ab]""").matches(it.ref))
            assertTrue(it.citation.contains("tr. H. Rackham"))
        }
    }

    @Test
    fun quoteStaysTheSameForOneFreshStartAndChangesForTheNext() {
        val goal = Goal("g", "No more alcohol", day).reset(day.plusDays(9), 1)
        assertEquals(Quotes.forSlip(goal), Quotes.forSlip(goal.copy(title = "renamed")))
        assertNotEquals(Quotes.forSlip(goal), Quotes.forSlip(goal.reset(day.plusDays(12), 2)))
    }
}
