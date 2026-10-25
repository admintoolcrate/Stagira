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
