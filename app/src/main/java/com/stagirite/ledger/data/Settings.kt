package com.stagirite.ledger.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

enum class SortOrder(val label: String) {
    Added("As added"),
    LongestRun("Longest current run"),
    NextMilestone("Next milestone"),
    Name("Name"),
}

enum class LockDelay(val label: String, val millis: Long) {
    Immediately("Immediately", 0),
    OneMinute("After 1 min", 60_000),
    FiveMinutes("After 5 min", 300_000),
}

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme by mutableStateOf(enumOr(prefs.getString("theme", null), ThemeMode.System))
        private set

    /** Blanks the app in the recent-apps screen and blocks screenshots. */
    var hideInRecents by mutableStateOf(prefs.getBoolean("hideInRecents", false))
        private set

    /** Ask for fingerprint, face or screen lock when the app is opened. */
    var appLock by mutableStateOf(prefs.getBoolean("appLock", false))
        private set

    var lockDelay by mutableStateOf(enumOr(prefs.getString("lockDelay", null), LockDelay.Immediately))
        private set

    var reminder by mutableStateOf(prefs.getBoolean("reminder", false))
        private set

    /** Time of the daily reminder, in minutes after midnight. */
    var reminderMinutes by mutableStateOf(prefs.getInt("reminderMinutes", 21 * 60))
        private set

    /** A weekly notification pointing to the weekly review. */
    var weekly by mutableStateOf(prefs.getBoolean("weekly", false))
        private set

    /** Day of the weekly notification, 1 (Monday) to 7 (Sunday). */
    var weeklyDay by mutableStateOf(prefs.getInt("weeklyDay", 7))
        private set

    var weeklyMinutes by mutableStateOf(prefs.getInt("weeklyMinutes", 18 * 60))
        private set

    fun updateWeekly(on: Boolean, day: Int = weeklyDay, minutes: Int = weeklyMinutes) {
        weekly = on
        weeklyDay = day.coerceIn(1, 7)
        weeklyMinutes = minutes
        prefs.edit().putBoolean("weekly", on).putInt("weeklyDay", weeklyDay).putInt("weeklyMinutes", minutes).apply()
    }

    /** The recents preview is hidden whenever the app is locked too. */
    val secureWindow: Boolean get() = hideInRecents || appLock

    fun updateTheme(mode: ThemeMode) {
        theme = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun updateHideInRecents(hide: Boolean) {
        hideInRecents = hide
        prefs.edit().putBoolean("hideInRecents", hide).apply()
    }

    fun updateAppLock(on: Boolean) {
        appLock = on
        prefs.edit().putBoolean("appLock", on).apply()
    }

    fun updateLockDelay(delay: LockDelay) {
        lockDelay = delay
        prefs.edit().putString("lockDelay", delay.name).apply()
    }

    fun updateReminder(on: Boolean, minutes: Int = reminderMinutes) {
        reminder = on
        reminderMinutes = minutes
        prefs.edit().putBoolean("reminder", on).putInt("reminderMinutes", minutes).apply()
    }

    /** Passages from Aristotle on Day 0, milestones, the empty page and the guide. */
    var quotes by mutableStateOf(prefs.getBoolean("quotes", true))
        private set

    /** The one-time screen on reaching a milestone. */
    var celebrations by mutableStateOf(prefs.getBoolean("celebrations", true))
        private set

    var haptics by mutableStateOf(prefs.getBoolean("haptics", true))
        private set

    /** The summary card at the top of the home page. */
    var homeSummary by mutableStateOf(prefs.getBoolean("homeSummary", true))
        private set

    /** "Coming up" and the combined calendar below the goals. */
    var homeExtras by mutableStateOf(prefs.getBoolean("homeExtras", true))
        private set

    fun updateQuotes(on: Boolean) { quotes = on; save("quotes", on) }
    fun updateCelebrations(on: Boolean) { celebrations = on; save("celebrations", on) }
    fun updateHaptics(on: Boolean) { haptics = on; save("haptics", on) }
    fun updateHomeSummary(on: Boolean) { homeSummary = on; save("homeSummary", on) }
    fun updateHomeExtras(on: Boolean) { homeExtras = on; save("homeExtras", on) }

    private fun save(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    var sortOrder by mutableStateOf(enumOr(prefs.getString("sortOrder", null), SortOrder.Added))
        private set

    fun updateSortOrder(order: SortOrder) {
        sortOrder = order
        prefs.edit().putString("sortOrder", order.name).apply()
    }

    /** Whether the introductory guide has been seen or skipped. */
    var guideSeen by mutableStateOf(prefs.getBoolean("guideSeen", false))
        private set

    fun updateGuideSeen(seen: Boolean) {
        guideSeen = seen
        prefs.edit().putBoolean("guideSeen", seen).apply()
    }

    /** Milestones already celebrated on this device, as "goalId:runStart:days". */
    private var celebrated by mutableStateOf(prefs.getStringSet("celebrated", emptySet())!!.toSet())

    fun isCelebrated(key: String) = key in celebrated

    fun markCelebrated(key: String) {
        celebrated = celebrated + key
        prefs.edit().putStringSet("celebrated", celebrated).apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        runCatching { enumValueOf<T>(name!!) }.getOrDefault(default)
}
