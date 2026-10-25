package com.stagirite.ledger

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.ui.LedgerApp
import com.stagirite.ledger.ui.LedgerTheme

class MainActivity : ComponentActivity() {
    private val app get() = application as LedgerApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings = app.settings
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
            LaunchedEffect(settings.hideInRecents) {
                if (settings.hideInRecents) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            LedgerTheme(dark) {
                LedgerApp(app.store, settings, app.sync)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.sync.requestSync()
    }

    override fun onStop() {
        super.onStop()
        app.sync.requestSync()
    }
}
