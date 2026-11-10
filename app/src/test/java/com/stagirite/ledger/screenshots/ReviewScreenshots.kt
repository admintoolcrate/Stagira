package com.stagirite.ledger.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.data.SortOrder
import com.stagirite.ledger.ui.GoalDetailScreen
import com.stagirite.ledger.ui.GoalListScreen
import com.stagirite.ledger.ui.GuideScreen
import com.stagirite.ledger.ui.HomeNav
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.OverviewDashboard
import com.stagirite.ledger.ui.PrivateTitlesUi
import com.stagirite.ledger.ui.SettingsScreen
import com.stagirite.ledger.ui.WeeklyReviewScreen
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Whole screens, drawn tall so nothing is cut off, for reviewing the design.
 * Not for the store. Drawn with the store screenshots, into app/build/review-screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h1700dp-xhdpi")
class ReviewScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun onlyWhenAsked() = assumeTrue(System.getProperty("screenshots") == "true")

    private val s = Samples

    private fun list(dark: Boolean, name: String) = draw(name, dark) {
        GoalListScreen(
            s.goals, s.today, SnackbarHostState(), s.privacy, emptyList(), SortOrder.Added,
            onSort = {}, onOpen = {}, onAdd = {}, onQuickSlip = { _, _ -> }, onQuickUrge = {},
            onArchive = { _, _ -> }, onTitleLongPress = {}, onSettings = {}, onWeekly = {},
            bottomBar = { HomeNav(false) {} },
        )
    }

    private fun goal(dark: Boolean, name: String) = draw(name, dark) {
        GoalDetailScreen(
            s.drink, s.drink.title, s.today, s.privacy,
            onBack = {}, onEdit = {}, onSlip = {}, onRemoveSlip = {}, onMoveStart = {}, onArchive = {}, onDelete = {},
        )
    }

    @Test fun r1_list() = list(false, "r1-list")
    @Test fun r2_goal() = goal(false, "r2-goal")
    @Test fun r3_overview() = draw("r3-overview") {
        OverviewDashboard(
            s.goals, s.today, onOpen = {}, onWeekly = {}, onSettings = {}, bottomBar = { HomeNav(true) {} },
            showBackupPrompt = false, onSetUpBackup = {}, onDismissBackup = {},
        )
    }
    @Test fun r4_week() = draw("r4-week") { WeeklyReviewScreen(s.goals, s.today, onOpen = {}, onBack = {}) }
    @Test fun r5_settings() = draw("r5-settings") {
        val app = ApplicationProvider.getApplicationContext<LedgerApplication>()
        SettingsScreen(
            app.store, app.settings, app.sync, app.lock,
            PrivateTitlesUi(hasPin = false, unlocked = false, onSetUp = {}, onChange = {}, onForget = {}),
            onShowGuide = {}, onBack = {},
        )
    }
    @Test fun r6_guide() = draw("r6-guide") { GuideScreen(onDone = {}, onCreate = {}) }
    @Test fun r7_list_dark() = list(true, "r7-list-dark")
    @Test fun r8_goal_dark() = goal(true, "r8-goal-dark")

    private fun draw(name: String, dark: Boolean = false, screen: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { LedgerTheme(dark) { screen() } }
        compose.mainClock.advanceTimeBy(3_000)
        shadowOf(Looper.getMainLooper()).idle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/review-screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
