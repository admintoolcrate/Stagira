package com.stagirite.ledger.feedback

import java.net.URLEncoder

/** Where reports go. The app has no server; these open in the reporter's own browser or email app. */
object FeedbackDestinations {
    const val EMAIL = "admin.toolcrate@gmail.com"
    const val GITHUB_REPO = "admintoolcrate/The-Stagirite-s-Ledger"
}

enum class ReportKind(val label: String, val template: String, val githubLabel: String, val subjectPrefix: String) {
    Bug("Bug report", "bug_report.md", "bug", "Bug"),
    Feature("Feature request", "feature_request.md", "enhancement", "Feature request"),
}

/**
 * A bug report or feature request, written by the user. It never contains any
 * goal data; [device] is only included if the user leaves it ticked, and the
 * form shows them exactly what it says.
 */
data class Report(
    val kind: ReportKind,
    val title: String,
    /** What happened (bug) or what they'd like (feature). */
    val details: String,
    /** Bug only: how to make it happen. */
    val steps: String = "",
    /** Bug only: what they expected instead. */
    val expected: String = "",
    /** e.g. "App 2.0 (5) · Android 15 (API 35) · Google Pixel 8"; null to leave out. */
    val device: String? = null,
) {
    val subject: String get() = "${kind.subjectPrefix}: ${title.trim().ifEmpty { "(no title)" }}"

    /** Markdown, matching the GitHub issue templates. */
    val body: String
        get() = buildString {
            when (kind) {
                ReportKind.Bug -> {
                    section("What happened", details)
                    section("Steps to reproduce", steps)
                    section("What you expected", expected)
                }
                ReportKind.Feature -> section("What you'd like", details)
            }
            if (device != null) section("Device", device)
            append("_Sent from the app. No goal data is included._")
        }

    private fun StringBuilder.section(heading: String, text: String) {
        if (text.isBlank()) return
        append("### ").append(heading).append("\n\n").append(text.trim()).append("\n\n")
    }

    /** A new-issue link with everything filled in. Long bodies are trimmed to stay under URL limits. */
    fun githubUrl(): String {
        val body = if (this.body.length > MAX_URL_BODY) this.body.take(MAX_URL_BODY) + "\n\n…(trimmed)" else this.body
        return "https://github.com/${FeedbackDestinations.GITHUB_REPO}/issues/new" +
            "?template=" + enc(kind.template) +
            "&labels=" + enc(kind.githubLabel) +
            "&title=" + enc(title.trim()) +
            "&body=" + enc(body)
    }

    /** A mailto: link with subject and body, for email apps that ignore intent extras. */
    fun mailtoUri(): String =
        "mailto:${FeedbackDestinations.EMAIL}?subject=" + enc(subject) + "&body=" + enc(body)

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    companion object {
        private const val MAX_URL_BODY = 6_000
    }
}
