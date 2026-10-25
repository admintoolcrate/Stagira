package com.stagirite.ledger

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.ui.LedgerApp
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.LockScreen

// A FragmentActivity because the fingerprint prompt needs one.
class MainActivity : FragmentActivity() {
    private val app get() = application as LedgerApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            LedgerTheme(dark) {
                if (lock.locked) {
                    LockScreen(onUnlock = { unlock() })
                } else {
                    LedgerApp(app.store, settings, app.sync, lock)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.lock.onForeground()
        if (app.lock.locked) unlock()
        app.sync.requestSync()
    }

    override fun onStop() {
        super.onStop()
        // Rotating the screen also stops the activity; that isn't leaving the app.
        if (!isChangingConfigurations) app.lock.onBackground()
        app.sync.requestSync()
    }

    private fun unlock() {
        app.lock.prompt(this, "Unlock the ledger") { app.lock.unlock() }
    }
}
