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

    val emptyLedger = Quote(
        "…one swallow does not make spring, nor does one fine day…",
        "I.7, 1098a",
    )

    /** A quote for this goal's latest fresh start; stays the same for that start. */
    fun forSlip(goal: Goal): Quote =
        slip[(goal.id.hashCode() + goal.freshStarts).absoluteValue % slip.size]
}
