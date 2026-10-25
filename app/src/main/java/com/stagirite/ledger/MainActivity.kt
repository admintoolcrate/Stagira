package com.stagirite.ledger

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.stagirite.ledger.ui.SupportUi
import com.stagirite.ledger.reminders.Reminder
import androidx.fragment.app.FragmentActivity
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.ui.LedgerApp
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.LocalUiPrefs
import com.stagirite.ledger.ui.LockScreen
import com.stagirite.ledger.ui.UiPrefs

// A FragmentActivity because the fingerprint prompt needs one.
class MainActivity : FragmentActivity() {
    private val app get() = application as LedgerApplication

    /** Set when opened from the weekly-review notification. */
    private var openWeekly by mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Reminder.EXTRA_OPEN_WEEKLY, false)) openWeekly = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.getBooleanExtra(Reminder.EXTRA_OPEN_WEEKLY, false)) openWeekly = true
        setContent {
            val settings = app.settings
            val lock = app.lock
            val dark = when (settings.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            LaunchedEffect(settings.secureWindow) {
                if (settings.secureWindow) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            val prefs = UiPrefs(
                quotes = settings.quotes,
                celebrations = settings.celebrations,
                haptics = settings.haptics,
                homeSummary = settings.homeSummary,
                homeExtras = settings.homeExtras,
            )
            CompositionLocalProvider(LocalUiPrefs provides prefs) {
                LedgerTheme(dark) {
                    if (lock.locked) {
                        LockScreen(onUnlock = { unlock() })
                    } else {
                        LedgerApp(
                            app.store, settings, app.sync, lock, app.privateTitles,
                            support = remember { SupportUi(app.billing, app.supportAd, lock) },
                            openWeekly = openWeekly,
                            onWeeklyOpened = { openWeekly = false },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.lock.onForeground()
        if (app.lock.locked) unlock()
        app.sync.requestSync()
        // Picks up a Plus bought on another phone, or a refund.
        app.billing.refresh()
    }

    override fun onStop() {
        super.onStop()
        // Rotating the screen also stops the activity; that isn't leaving the app.
        if (!isChangingConfigurations) {
            app.lock.onBackground()
            // Private titles are hidden again whenever the app is left.
            app.privateTitles.lock()
        }
        app.sync.requestSync()
    }

    private fun unlock() {
        app.lock.prompt(this, "Unlock the ledger") { app.lock.unlock() }
    }
}
