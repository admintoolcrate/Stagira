package com.stagirite.ledger.screenshots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.SortOrder
import com.stagirite.ledger.data.yearReview
import com.stagirite.ledger.ui.DuskGradient
import com.stagirite.ledger.ui.GoalDetailScreen
import com.stagirite.ledger.ui.GoalListScreen
import com.stagirite.ledger.ui.HomeNav
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.LedgerType
import com.stagirite.ledger.ui.OverviewDashboard
import com.stagirite.ledger.ui.PrivacyUi
import com.stagirite.ledger.ui.WeeklyReviewScreen
import com.stagirite.ledger.ui.YearCard
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Google Play screenshots, drawn from the app's real screens with made-up goals:
 * each one a phone-sized screen, scaled into a frame under a caption, at 1080×1920.
 *
 *     ./gradlew testDebugUnitTest --tests '*StoreScreenshots*' -Pscreenshots=true
 *
 * The images land in app/build/store-screenshots. Skipped in ordinary test runs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
class StoreScreenshots {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun onlyWhenAsked() = assumeTrue(System.getProperty("screenshots") == "true")

    private val today = LocalDate.of(2026, 11, 14)
    private fun ago(n: Long) = today.minusDays(n)

    private val drink = Goal(
        "drink", "Drink less", ago(200),
        slips = listOf(ago(151), ago(97), ago(64)),
        reason = "Clearer mornings, and more of them.",
        icon = "🍷", kind = GoalKind.Vice,
        urges = mapOf(ago(1) to 1, ago(3) to 2, ago(6) to 1, ago(10) to 1, ago(17) to 1),
    )
    private val phone = Goal("phone", "No phone in bed", ago(21), icon = "📵", kind = GoalKind.Vice)
    private val read = Goal(
        "read", "Read before bed", ago(120),
        slips = listOf(ago(58), ago(12)),
        icon = "📖", kind = GoalKind.Virtue,
    )
    private val walk = Goal(
        "walk", "Morning walk", ago(90),
        slips = listOf(ago(40), ago(10)),
        icon = "🚶", kind = GoalKind.Virtue,
        schedule = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
    )
    private val goals = listOf(drink, phone, read, walk)
    private val privacy = PrivacyUi(hasPin = false, unlocked = false, requestPin = {}, open = { null })

    @Test
    fun s1_goals() = shoot("1-goals", "Count the days", "you keep.") {
        GoalListScreen(
            goals, today, SnackbarHostState(), privacy, emptyList(), SortOrder.Added,
            onSort = {}, onOpen = {}, onAdd = {}, onQuickSlip = { _, _ -> }, onQuickUrge = {},
            onArchive = { _, _ -> }, onTitleLongPress = {}, onSettings = {}, onWeekly = {},
            bottomBar = { HomeNav(false) {} },
        )
    }

    @Test
    fun s2_goal() = shoot("2-goal", "Every day you hold to it", "adds to the count.") {
        GoalDetailScreen(
            drink, drink.title, today, privacy,
            onBack = {}, onEdit = {}, onSlip = {}, onRemoveSlip = {}, onMoveStart = {}, onArchive = {}, onDelete = {},
        )
    }

    @Test
    fun s3_overview() = shoot("3-overview", "How it’s all going,", "at a glance.") {
        OverviewDashboard(
            goals, today, onOpen = {}, onWeekly = {}, onSettings = {},
            bottomBar = { HomeNav(true) {} },
            showBackupPrompt = false, onSetUpBackup = {}, onDismissBackup = {},
        )
    }

    @Test
    fun s4_week() = shoot("4-week", "A weekly review,", "without the guilt.") {
        WeeklyReviewScreen(goals, today, onOpen = {}, onBack = {})
    }

    @Test
    fun s5_year() = shoot("5-year", "Your year,", "in one card.") {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            YearCard(yearReview(goals, today.year, today)!!, showNames = true, inProgress = true)
        }
    }

    @Test
    fun s6_dark() = shoot("6-dark", "No ads, no account.", "Just your ledger.", dark = true) {
        GoalListScreen(
            goals, today, SnackbarHostState(), privacy, emptyList(), SortOrder.LongestRun,
            onSort = {}, onOpen = {}, onAdd = {}, onQuickSlip = { _, _ -> }, onQuickUrge = {},
            onArchive = { _, _ -> }, onTitleLongPress = {}, onSettings = {}, onWeekly = {},
            bottomBar = { HomeNav(false) {} },
        )
    }

    /** Draws [screen] at phone size, framed under the caption, and saves it as a PNG. */
    private fun shoot(name: String, caption: String, accent: String, dark: Boolean = false, screen: @Composable () -> Unit) {
        compose.setContent {
            LedgerTheme(dark) { Frame(caption, accent, dark, screen) }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/store-screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

/** The website's look: warm paper (or dusk, in dark), the caption in EB Garamond with a gold italic turn. */
@Composable
private fun Frame(caption: String, accent: String, dark: Boolean, screen: @Composable () -> Unit) {
    val gold = MaterialTheme.colorScheme.tertiary
    val background = if (dark) Brush.linearGradient(DuskGradient)
    else Brush.verticalGradient(listOf(Color(0xFFF7EBDD), Color(0xFFF3F0EA), Color(0xFFE9EEE4)))
    val ink = if (dark) Color(0xFFF2EDE4) else MaterialTheme.colorScheme.onBackground
    Column(
        Modifier.fillMaxSize().background(background).padding(top = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            buildAnnotatedString {
                append(caption)
                append("\n")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = if (dark) Color(0xFFD0AE74) else gold)) { append(accent) }
            },
            style = LedgerType.name.copy(fontSize = 30.sp, lineHeight = 34.sp),
            color = ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(22.dp))
        // The screen itself at a real phone size (360 × 760 dp), shown at 70%.
        Box(
            Modifier
                .size(252.dp, 532.dp)
                .clip(RoundedCornerShape(30.dp))
                .border(1.dp, if (dark) Color(0x33F2EDE4) else Color(0x29786955), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .requiredSize(360.dp, 760.dp)
                    .graphicsLayer { scaleX = 0.7f; scaleY = 0.7f },
            ) { screen() }
        }
    }
}
