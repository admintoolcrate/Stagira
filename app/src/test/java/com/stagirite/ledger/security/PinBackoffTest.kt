package com.stagirite.ledger.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinBackoffTest {
    @Test
    fun firstFiveAreFreeThenItDoublesToAnHour() {
        (0..5).forEach { assertEquals(0L, pinBackoffMillis(it)) }
        assertEquals(30_000L, pinBackoffMillis(6))
        assertEquals(60_000L, pinBackoffMillis(7))
        assertEquals(120_000L, pinBackoffMillis(8))
        assertEquals(3_600_000L, pinBackoffMillis(20))
        assertEquals(3_600_000L, pinBackoffMillis(Int.MAX_VALUE))
    }
}
