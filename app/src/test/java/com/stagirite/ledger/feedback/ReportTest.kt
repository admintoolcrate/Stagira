package com.stagirite.ledger.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class ReportTest {
    private fun param(url: String, name: String): String =
        URLDecoder.decode(url.substringAfter("$name=").substringBefore("&"), "UTF-8")

    @Test
    fun bugBodyHasOnlyFilledSections() {
        val r = Report(ReportKind.Bug, "Crash on save", "It closed", steps = "1. Edit\n2. Save", device = "App 2.0 (5)")
        assertTrue(r.body.contains("### What happened\n\nIt closed"))
        assertTrue(r.body.contains("### Steps to reproduce\n\n1. Edit\n2. Save"))
        assertFalse(r.body.contains("What you expected"))
        assertTrue(r.body.contains("### Device\n\nApp 2.0 (5)"))
        assertEquals("Bug: Crash on save", r.subject)
    }

    @Test
    fun deviceIsLeftOutWhenNotWanted() {
        assertFalse(Report(ReportKind.Feature, "Year view", "A 12-month grid").body.contains("Device"))
    }

    @Test
    fun githubLinkCarriesTemplateLabelTitleAndBody() {
        val r = Report(ReportKind.Feature, "Year view & more", "A 12-month grid, please?")
        val url = r.githubUrl()
        assertTrue(url.startsWith("https://github.com/admintoolcrate/Stagira/issues/new?"))
        assertEquals("feature_request.md", param(url, "template"))
        assertEquals("enhancement", param(url, "labels"))
        assertEquals("Year view & more", param(url, "title"))
        assertEquals(r.body, param(url, "body"))
        assertFalse("spaces are %20, not +", url.contains("+"))
    }

    @Test
    fun longBodiesAreTrimmedForTheUrl() {
        val r = Report(ReportKind.Bug, "Long", "x".repeat(20_000))
        assertTrue(param(r.githubUrl(), "body").endsWith("…(trimmed)"))
    }

    @Test
    fun mailtoGoesToTheFeedbackAddress() {
        val m = Report(ReportKind.Bug, "Hi", "There").mailtoUri()
        assertTrue(m.startsWith("mailto:admin.toolcrate@gmail.com?subject="))
        assertEquals("Bug: Hi", param(m, "subject"))
    }
}
