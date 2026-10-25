package com.stagirite.ledger.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme by mutableStateOf(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", null)!!) }.getOrDefault(ThemeMode.System)
    )
        private set

    /** Blanks the app in the recent-apps screen and blocks screenshots. */
    var hideInRecents by mutableStateOf(prefs.getBoolean("hideInRecents", false))
        private set

    fun updateTheme(mode: ThemeMode) {
        theme = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun updateHideInRecents(hide: Boolean) {
        hideInRecents = hide
        prefs.edit().putBoolean("hideInRecents", hide).apply()
    }
}
