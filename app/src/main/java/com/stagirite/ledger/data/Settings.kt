package com.stagirite.ledger.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

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
