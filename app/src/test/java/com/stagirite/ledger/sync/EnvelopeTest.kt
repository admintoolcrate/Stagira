package com.stagirite.ledger.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class EnvelopeTest {
    private val ledger = """{"version":1,"goals":[]}"""

    @Test
    fun plainPassesThrough() {
        val written = Envelope.write(ledger, key = null)
        assertEquals(Envelope.Plain(ledger), Envelope.read(written))
    }

    @Test
    fun sealedRoundTripsWithTheRightPassphrase() {
        val key = Envelope.deriveKey("correct horse".toCharArray(), iterations = 10_000)
        val written = Envelope.write(ledger, key)
        assertFalse("ciphertext must not contain the ledger", written.contains("goals"))

        val sealed = Envelope.read(written) as Envelope.Sealed
        assertTrue(sealed.matches(key))
        // A second device derives the same key from the passphrase and the stored salt.
        val again = Envelope.deriveKey("correct horse".toCharArray(), sealed.salt, sealed.iterations)
        assertEquals(ledger, sealed.open(again))
    }

    @Test(expected = AEADBadTagException::class)
    fun wrongPassphraseFails() {
        val key = Envelope.deriveKey("correct horse".toCharArray(), iterations = 10_000)
        val sealed = Envelope.read(Envelope.write(ledger, key)) as Envelope.Sealed
        sealed.open(Envelope.deriveKey("wrong".toCharArray(), sealed.salt, sealed.iterations))
    }

    @Test(expected = IllegalArgumentException::class)
    fun absurdWorkFactorIsRefused() {
        Envelope.read("""{"sealed":1,"iterations":2000000000,"salt":"AA==","iv":"AA==","data":"AA=="}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroWorkFactorIsRefused() {
        Envelope.read("""{"sealed":1,"iterations":0,"salt":"AA==","iv":"AA==","data":"AA=="}""")
    }
}
