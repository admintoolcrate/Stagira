package com.stagirite.ledger.data

/**
 * Emoji offered as goal icons. They're drawn with Noto Emoji, Google's monochrome
 * emoji font (SIL Open Font License), so they come out black and white.
 */
object GoalIcons {
    val groups: List<Pair<String, List<String>>> = listOf(
        "Habits" to listOf("🍺", "🍷", "🥃", "🚬", "🚭", "💊", "🎲", "🔞", "🍬", "🍔", "🍕", "🥤", "☕", "📱", "💻", "🎮", "📺", "🛒", "💸", "😡"),
        "Body" to listOf("🏃", "🚶", "🚴", "🏊", "🏋", "🧘", "💪", "💧", "🥗", "🍎", "🛌", "🦷"),
        "Mind" to listOf("📚", "✍", "🎸", "🎨", "🧠", "🙏", "⏰", "🎯"),
        "Life" to listOf("🌱", "☀", "🌙", "⭐", "🔥", "❤", "🤝", "👪", "🐕", "🧹", "💰", "⚖", "🏛", "🕊", "⚓", "🔑"),
    )

    val all: List<String> get() = groups.flatMap { it.second }
}

/** A ready-made goal to start from. */
data class GoalTemplate(val title: String, val icon: String, val kind: GoalKind)

val goalTemplates = listOf(
    GoalTemplate("Drink less", "🍺", GoalKind.Vice),
    GoalTemplate("No alcohol", "🍷", GoalKind.Vice),
    GoalTemplate("No smoking", "🚭", GoalKind.Vice),
    GoalTemplate("Less phone", "📱", GoalKind.Vice),
    GoalTemplate("No sugar", "🍬", GoalKind.Vice),
    GoalTemplate("No gambling", "🎲", GoalKind.Vice),
    GoalTemplate("One good deed a day", "🤝", GoalKind.Virtue),
    GoalTemplate("Exercise", "🏃", GoalKind.Virtue),
    GoalTemplate("Read", "📚", GoalKind.Virtue),
    GoalTemplate("Meditate", "🧘", GoalKind.Virtue),
    GoalTemplate("Journal", "✍", GoalKind.Virtue),
    GoalTemplate("Early to bed", "🛌", GoalKind.Virtue),
)
