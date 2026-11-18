package com.stagirite.ledger.screenshots

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.stagirite.ledger.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Opens the real app with settings left behind by earlier versions, so a crash on
 * start shows here with its stack trace. Runs with the screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class StartupCheck {
    @Before
    fun onlyWhenAsked() = assumeTrue(System.getProperty("screenshots") == "true")

    private fun open(vararg prefs: Pair<String, String>) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().apply {
            putBoolean("guideSeen", true)
            prefs.forEach { (k, v) -> putString(k, v) }
        }.commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        controller.pause().stop().destroy()
    }

    @Test fun fresh() = open()
    @Test fun fromBlackMode() = open("theme" to "Black", "palette" to "Sage")
    @Test fun fromBlackPalette() = open("palette" to "Black")
    @Test fun fromDark() = open("theme" to "Dark", "palette" to "Dusk", "widgetStyle" to "Black")
    @Test fun fromLight() = open("theme" to "Light", "palette" to "Paper")
}
