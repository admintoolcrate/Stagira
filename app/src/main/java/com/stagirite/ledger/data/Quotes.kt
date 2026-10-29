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
        Quote(
            "Yet nevertheless even in adversity nobility shines through, when a man endures repeated and severe " +
                "misfortune with patience, not owing to insensibility but from generosity and greatness of soul.",
            "I.10, 1100b",
        ),
        Quote(
            "For where we are free to act we are also free to refrain from acting, and where we are able to say No " +
                "we are also able to say Yes…",
            "III.5, 1113b",
        ),
        Quote(
            "…it consequently depends on us whether we are virtuous or vicious.",
            "III.5, 1113b",
        ),
        Quote(
            "This is why it is a hard task to be good, for it is hard to find the middle point in anything…",
            "II.9, 1109a",
        ),
        Quote(
            "However, we do not blame one who diverges a little from the right course, whether on the side of " +
                "the too much or of the too little, but one who diverges more widely…",
            "II.9, 1109b",
        ),
        Quote(
            "It is not surprising that a man should be overcome by violent and excessive pleasures or pains: " +
                "indeed it is excusable if he succumbs after a struggle…",
            "VII.7, 1150b",
            "Aristotle on giving in after a real struggle.",
        ),
        Quote(
            "…the truly good and wise man will bear all kinds of fortune in a seemly way, and will always act in " +
                "the noblest manner that the circumstances allow; even as a good general makes the most effective " +
                "use of the forces at his disposal, and a good shoemaker makes the finest shoe possible out of the " +
                "leather supplied him…",
            "I.10, 1101a",
        ),
        Quote(
            "The second rule is to notice what are the errors to which we are ourselves most prone (as different " +
                "men are inclined by nature to different faults)—and we shall discover what these are by observing " +
                "the pleasure or pain that we experience…",
            "II.9, 1109b",
        ),
        Quote(
            "Thirdly, we must in everything be most of all on our guard against what is pleasant and against " +
                "pleasure; for when pleasure is on her trial we are not impartial judges.",
            "II.9, 1109b",
            "Aristotle on why the pull of pleasure is hard to judge in the moment.",
        ),
        Quote(
            "Hence our indulgences should be moderate and few, and never opposed to principle…",
            "III.12, 1119b",
            "\u201CPrinciple\u201D is Rackham\u2019s word for reason, the plan you\u2019ve chosen.",
        ),
        Quote(
            "This is the unrestrained man: he is better than the profligate, and not absolutely bad, for in him the " +
                "highest part of man, the fundamental principle, is still preserved.",
            "VII.8, 1151a",
            AKRASIA,
        ),
        Quote(
            "…it is easier to train oneself to resist the temptations of pleasure, because these occur frequently " +
                "in life, and to practise resistance to them involves no danger…",
            "III.12, 1119a",
            "Aristotle on why temptation, met often, can be trained against.",
        ),
        Quote(
            "Thus much then is clear, that it is the middle disposition in each department of conduct that is to be " +
                "praised, but that one should lean sometimes to the side of excess and sometimes to that of " +
                "deficiency, since this is the easiest way of hitting the mean and the right course.",
            "II.9, 1109b",
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
        Quote(
            "…temperance and hardiness will not be painful when they have become habitual.",
            "X.9, 1179b",
        ),
        Quote(
            "We become temperate by abstaining from pleasures, and at the same time we are best able to abstain " +
                "from pleasures when we have become temperate.",
            "II.2, 1104a",
        ),
        Quote(
            "Hence it is incumbent on us to control the character of our activities, since on the quality of these " +
                "depends the quality of our dispositions.",
            "II.1, 1103b",
        ),
        Quote(
            "…to do these things properly is rare, praiseworthy, and noble.",
            "II.9, 1109a",
        ),
        Quote(
            "They acquire a particular quality by constantly acting in a particular way.",
            "III.5, 1114a",
        ),
        Quote(
            "…so it is those who act rightly who carry off the prizes and good things of life.",
            "I.8, 1099a",
        ),
        Quote(
            "…Restraint implies mastery, which is a different matter: victory is more glorious than the mere " +
                "avoidance of defeat.",
            "VII.7, 1150a",
        ),
        Quote(
            "And he will bear changes of fortunes most nobly, and with perfect propriety in every way, being as he " +
                "is good in very truth and four-square without reproach.",
            "I.10, 1100b",
        ),
        Quote(
            "…virtue results from the repeated performance of just and temperate actions.",
            "II.4, 1105b",
        ),
        Quote(
            "…to do noble and virtuous deeds is a thing desirable for its own sake.",
            "X.6, 1176b",
        ),
        Quote(
            "…those who work with pleasure always work with more discernment and with greater accuracy…",
            "X.5, 1175a",
        ),
        Quote(
            "…the self-restrained man stands by principle and does not change from either cause.",
            "VII.9, 1151b",
        ),
        Quote(
            "Again, everybody loves a thing more if it has cost him trouble…",
            "IX.7, 1168a",
        ),
        Quote(
            "The doer\u2019s achievement therefore remains, for nobility or beauty is long-lived…",
            "IX.7, 1168a",
        ),
        Quote(
            "Hence Courage itself is painful; and it is justly praised, because it is harder to endure pain than to " +
                "abstain from pleasure.",
            "III.9, 1117a",
        ),
        Quote(
            "…he will endure them as principle dictates, for the sake of what is noble; for that is the end at which " +
                "virtue aims.",
            "III.7, 1115b",
        ),
        Quote(
            "Acts of virtue are noble, and are performed for the sake of their nobility…",
            "IV.1, 1120a",
        ),
        Quote(
            "…in this working out of details Time seems to be a good inventor or at all events coadjutor.",
            "I.7, 1098a",
        ),
    )

    /**
     * A passage for reaching [days]. Each milestone of a run takes the next passage in turn
     * (from a starting point that differs per goal), so none repeats until all have been seen.
     */
    fun forMilestone(goal: Goal, days: Long): Quote {
        val nth = Milestones.upTo(days, goal.currentStart).size
        return milestone[(goal.id.hashCode().absoluteValue + nth) % milestone.size]
    }

    /** For the empty page: about starting, and about doing rather than talking. */
    val starting = listOf(
        Quote(
            "…one swallow does not make spring, nor does one fine day…",
            "I.7, 1098a",
        ),
        Quote(
            "…men become builders by building houses, harpers by playing on the harp.",
            "II.1, 1103a",
        ),
        Quote(
            "In so doing they act like invalids who listen carefully to what the doctor says, but entirely neglect " +
                "to carry out his prescriptions.",
            "II.4, 1105b",
            "On people who talk about virtue instead of practising it.",
        ),
        Quote(
            "And just as at the Olympic games the wreaths of victory are not bestowed upon the handsomest and " +
                "strongest persons present, but on men who enter for the competitions…",
            "I.8, 1099a",
        ),
        Quote(
            "…in the practical sciences the end is not to attain a theoretic knowledge of the various subjects, " +
                "but rather to carry out our theories in action.",
            "X.9, 1179b",
        ),
        Quote(
            "…for no doubt the proper procedure is to begin by making a rough sketch, and to fill it in afterwards.",
            "I.7, 1098a",
        ),
        Quote(
            "…as you will become a good builder from building well, so you will become a bad one from building badly.",
            "II.1, 1103b",
        ),
        Quote(
            "…moral or ethical virtue is the product of habit (ethos), and has indeed derived its name, with a slight " +
                "variation of form, from that word.",
            "II.1, 1103a",
        ),
    )

    /** A passage for the empty page; a different one each day. */
    fun forEmptyLedger(today: java.time.LocalDate): Quote =
        starting[(today.toEpochDay() % starting.size).toInt()]

    /**
     * A quote for this goal's latest fresh start; stays the same for that start.
     * Virtues skip the passages on giving in to temptation, which fit vices.
     */
    fun forSlip(goal: Goal): Quote {
        val pool = if (goal.kind == GoalKind.Virtue) slip.filter { it.note == null } else slip
        return pool[(goal.id.hashCode() + goal.freshStarts).absoluteValue % pool.size]
    }
}
