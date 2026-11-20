package com.stagirite.ledger.intake

import com.stagirite.ledger.data.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GoalLinksTest {
    @Test fun `usable ids pass, anything odd is ignored`() {
        assertEquals("abc-123", GoalLinks.cleanId("  abc-123 "))
        assertNull(GoalLinks.cleanId(null))
        assertNull(GoalLinks.cleanId("   "))
        assertNull(GoalLinks.cleanId("a\u0000b"))
        assertNull(GoalLinks.cleanId("x".repeat(129)))
        assertEquals(128, GoalLinks.cleanId("x".repeat(128))!!.length)
    }

    @Test fun `a pick returns the id and the title as shown, never a private goal's real name`() {
        val since = LocalDate.of(2026, 1, 1)
        val private = Goal("p1", "Evening walk", since, sealed = "ENCRYPTED-REAL-TITLE")
        assertEquals("p1" to "Evening walk", GoalLinks.result(private))
        val long = Goal("g2", "  " + "y".repeat(200) + "\n", since)
        val (_, title) = GoalLinks.result(long)
        assertEquals(120, title.length)
    }
}
