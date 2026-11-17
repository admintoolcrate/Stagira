package com.stagirite.ledger.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagirite.ledger.data.SortOrder
import com.stagirite.ledger.ui.GoalDetailScreen
import com.stagirite.ledger.ui.GoalListScreen
import com.stagirite.ledger.ui.HomeNav
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.LedgerType
import com.stagirite.ledger.ui.Tone
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
 * One image per palette: the Goals list and a goal page by day, and the Goals list
 * by night, side by side, to choose between palettes. Saved to app/build/review-screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w820dp-h1000dp-xhdpi")
class ToneScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun onlyWhenAsked() = assumeTrue(System.getProperty("screenshots") == "true")

    @Test fun allByDay() = grid(dark = false, name = "tones-day") { List() }
    @Test fun allByNight() = grid(dark = true, name = "tones-night") { List() }
    @Test fun goalsByDay() = grid(dark = false, name = "tones-goal") { Goal() }

    private val s = Samples

    @Composable
    private fun List() = GoalListScreen(
        s.goals, s.today, SnackbarHostState(), s.privacy, emptyList(), SortOrder.Added,
        onSort = {}, onOpen = {}, onAdd = {}, onQuickSlip = { _, _ -> }, onQuickUrge = {},
        onArchive = { _, _ -> }, onTitleLongPress = {}, onSettings = {}, onWeekly = {},
        bottomBar = { HomeNav(false) {} },
    )

    @Composable
    private fun Goal() = GoalDetailScreen(
        s.drink, s.drink.title, s.today, s.privacy,
        onBack = {}, onEdit = {}, onSlip = {}, onRemoveSlip = {}, onMoveStart = {}, onArchive = {}, onDelete = {},
    )

    @Composable
    private fun Phone(tone: Tone, dark: Boolean, screen: @Composable () -> Unit) {
        Box(
            Modifier
                .size(180.dp, 380.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color(0x22000000), RoundedCornerShape(18.dp)),
        ) {
            Box(Modifier.requiredSize(360.dp, 760.dp).graphicsLayer { scaleX = 0.5f; scaleY = 0.5f }) {
                LedgerTheme(dark, tone) { screen() }
            }
        }
    }

    /** All eight palettes, four to a row, each labelled. */
    private fun grid(dark: Boolean, name: String, screen: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFFE9E9E7)).padding(20.dp)) {
                Tone.entries.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { tone ->
                            Column {
                                Text(tone.label, style = LedgerType.title.copy(fontSize = 22.sp), color = Color(0xFF1C1C1C))
                                Spacer(Modifier.height(6.dp))
                                Phone(tone, dark, screen)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
        compose.mainClock.advanceTimeBy(3_000)
        shadowOf(Looper.getMainLooper()).idle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/review-screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
