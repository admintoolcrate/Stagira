package com.stagirite.ledger.data

import kotlin.math.absoluteValue

/**
 * A passage from the Nicomachean Ethics, quoted exactly from H. Rackham's
 * translation (Loeb Classical Library, 1926) as published by the Perseus
 * Digital Library. "…" marks where a sentence is entered or left part way.
 */
data class Quote(
    val text: String,
    /** Book.chapter (roman book) and Bekker page, e.g. "VII.8, 1150b". */
    val ref: String,
    /** Our own gloss, shown apart from the quotation. */
    val note: String? = null,
) {
    val citation: String get() = "Aristotle, Nicomachean Ethics $ref · tr. H. Rackham"

    /** "Nicomachean Ethics VII.8 · 1150b" style line under the author. */
    val work: String get() = "Nicomachean Ethics ${ref.replace(", ", " · ")}"
}

object Quotes {
    private const val AKRASIA =
        "“Unrestrained” is Rackham’s word for akrasia: knowing what’s best and giving in anyway."

    /** Words for the day you begin again. */
    val slip = listOf(
        Quote(
            "…it is the profligate who cannot be cured, whereas the unrestrained man can…",
            "VII.8, 1150b",
            AKRASIA,
        ),
        Quote(
            "And those who have become unrestrained through habit are more easily cured than those who " +
                "are unrestrained by nature, since habit is easier to change than nature…",
            "VII.10, 1152a",
            AKRASIA,
        ),
        Quote(
            "Similarly we become just by doing just acts, temperate by doing temperate acts, brave by doing brave acts.",
            "II.1, 1103b",
        ),
        Quote(
            "The virtues therefore are engendered in us neither by nature nor yet in violation of nature; " +
                "nature gives us the capacity to receive them, and this capacity is brought to maturity by habit.",
            "II.1, 1103a",
        ),
        Quote(
            "…we must drag ourselves away in the opposite direction, for by steering wide of our besetting " +
                "error we shall make a middle course. This is the method adopted by carpenters to straighten warped timber.",
            "II.9, 1109b",
        ),
        Quote(
            "…virtue, like art, is constantly dealing with what is harder, since the harder the task the better is success.",
            "II.3, 1105a",
        ),
        Quote(
            "…it is easy to fail and difficult to succeed—easy to miss the target and difficult to hit it…",
            "II.6, 1106b",
        ),
    )

    /** Words for reaching a milestone. */
    val milestone = listOf(
        Quote(
            "It is correct therefore to say that a man becomes just by doing just actions and temperate " +
                "by doing temperate actions; and no one can have the remotest chance of becoming good without doing them.",
            "II.4, 1105b",
        ),
        Quote(
            "In a word, our moral dispositions are formed as a result of the corresponding activities.",
            "II.1, 1103b",
        ),
        Quote(
            "Mark me, my friend, \u2019tis long-continued training, And training in the end becomes men\u2019s nature.",
            "VII.10, 1152a",
            "Aristotle quoting the poet Evenus.",
        ),
        Quote(
            "Moreover, to be happy takes a complete lifetime; for one swallow does not make spring, nor does one fine day\u2026",
            "I.7, 1098a",
        ),
    )

    fun forMilestone(goal: Goal, days: Long): Quote =
        milestone[(goal.id.hashCode() + days.toInt()).absoluteValue % milestone.size]

    val emptyLedger = Quote(
        "…one swallow does not make spring, nor does one fine day…",
        "I.7, 1098a",
    )

    /**
     * A quote for this goal's latest fresh start; stays the same for that start.
     * Virtues skip the passages on giving in to temptation, which fit vices.
     */
    fun forSlip(goal: Goal): Quote {
        val pool = if (goal.kind == GoalKind.Virtue) slip.filter { it.note == null } else slip
        return pool[(goal.id.hashCode() + goal.freshStarts).absoluteValue % pool.size]
    }
}
