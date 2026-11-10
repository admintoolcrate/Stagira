package com.stagirite.ledger.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.YearGoalLine
import com.stagirite.ledger.data.YearReview
import com.stagirite.ledger.data.defaultReviewYear
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.data.yearReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** How many goals the card lists by name before "and 3 more". */
private const val CARD_GOALS = 5

/**
 * The year in review: a card of the year's figures that can be shared as an
 * image. Nothing leaves the phone unless the share button is pressed, private
 * goals never show their real names, and names can be left off altogether.
 */
@Composable
fun YearReviewDialog(goals: List<Goal>, today: LocalDate, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        YearReviewPage(goals, today, onDismiss)
    }
}

/** The page inside the dialog: the year picker, the card, and sharing. */
@Composable
internal fun YearReviewPage(goals: List<Goal>, today: LocalDate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firstYear = goals.minOfOrNull { it.since.year } ?: today.year
    var year by rememberSaveable { mutableIntStateOf(maxOf(firstYear, defaultReviewYear(today))) }
    var showNames by rememberSaveable { mutableStateOf(true) }
    var sharing by remember { mutableStateOf(false) }
    val review = remember(goals, year, today) { yearReview(goals, year, today) }
    val layer = rememberGraphicsLayer()

    run {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                    Text("Your year", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { year-- }, enabled = year > firstYear) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
                    }
                    Text("$year", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { year++ }, enabled = year < today.year) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next year")
                    }
                }
                Spacer(Modifier.height(Space.m))
                if (review == null) {
                    Text(
                        "No goals were being tracked in $year.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Space.s),
                    )
                } else {
                    // Recorded as it's drawn, so the share button can turn it into an image.
                    Box(
                        Modifier.drawWithContent {
                            layer.record { this@drawWithContent.drawContent() }
                            drawLayer(layer)
                        },
                    ) {
                        YearCard(review, showNames, inProgress = year == today.year)
                    }
                    Spacer(Modifier.height(Space.l))
                    SwitchRow(
                        title = "Show goal names",
                        detail = "Private goals are never named on the card.",
                        checked = showNames,
                        onChange = { showNames = it },
                    )
                    Spacer(Modifier.height(Space.m))
                    Button(
                        onClick = {
                            sharing = true
                            scope.launch {
                                val bitmap = layer.toImageBitmap().asAndroidBitmap()
                                runCatching { shareImage(context, bitmap, year) }
                                sharing = false
                            }
                        },
                        enabled = !sharing,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = Space.s))
                        Text("Share as an image")
                    }
                    Text(
                        "Only the card is shared, and only when you choose.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Space.s),
                    )
                }
            }
        }
    }
}

@Composable
internal fun YearCard(review: YearReview, showNames: Boolean, inProgress: Boolean) {
    // The website's dusk panel: the same card in light and dark, so a shared image always looks right.
    val ink = OnDusk
    val faint = OnDusk.copy(alpha = 0.7f)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(DuskGradient), RoundedCornerShape(28.dp))
            .padding(horizontal = 28.dp, vertical = 32.dp),
    ) {
        Text(
            "STAGIRA · ${review.year}" + if (inProgress) " SO FAR" else "",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
            color = faint,
        )
        Spacer(Modifier.height(Space.l))
        Text("${review.daysAllKept}", style = LedgerType.hero.copy(fontSize = 80.sp, lineHeight = 84.sp), color = ink)
        Text(
            if (review.lines.size > 1) "days kept, with nothing slipped" else if (review.daysAllKept == 1L) "day kept" else "days kept",
            style = MaterialTheme.typography.labelLarge,
            color = faint,
        )
        Text(
            "of ${plural(review.daysTracked, "day")} tracked",
            style = MaterialTheme.typography.bodySmall,
            color = faint,
        )
        Spacer(Modifier.height(Space.l))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xl)) {
            review.best?.let { DuskFigure(plural(it.bestRun, "day"), "longest run") }
            DuskFigure("${review.milestones}", if (review.milestones == 1) "milestone" else "milestones", gold = true)
            if (review.urges > 0) DuskFigure("${review.urges}", "urges resisted")
        }
        Spacer(Modifier.height(Space.m))
        HorizontalDivider(color = OnDusk.copy(alpha = 0.22f))
        Spacer(Modifier.height(Space.m))
        review.lines.take(CARD_GOALS).forEachIndexed { i, line ->
            CardGoalRow(line, i, showNames, ink, faint)
        }
        if (review.lines.size > CARD_GOALS) {
            Text(
                "and ${plural((review.lines.size - CARD_GOALS).toLong(), "more goal")}",
                style = MaterialTheme.typography.bodySmall,
                color = faint,
                modifier = Modifier.padding(top = Space.xs),
            )
        }
        Spacer(Modifier.height(Space.xl))
        Text(
            "Count the days you keep · stagira.toolcrate.cc",
            style = MaterialTheme.typography.bodySmall,
            color = faint,
        )
    }
}

/** A figure on the dusk card, with its label beneath; milestones in gold. */
@Composable
private fun DuskFigure(value: String, label: String, gold: Boolean = false) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = if (gold) DuskGold else OnDusk)
        Text(label, style = MaterialTheme.typography.labelMedium, color = OnDusk.copy(alpha = 0.7f))
    }
}

@Composable
private fun CardGoalRow(line: YearGoalLine, index: Int, showNames: Boolean, ink: Color, faint: Color) {
    val goal = line.goal
    val named = showNames && !goal.isPrivate
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (named && goal.icon.isNotEmpty()) {
            EmojiGlyph(goal.icon, 16.sp, color = ink)
            Spacer(Modifier.width(Space.s))
        }
        Text(
            when {
                named -> goal.title
                goal.isPrivate && showNames -> "A private goal"
                goal.kind == GoalKind.Vice -> "Vice ${index + 1}"
                goal.kind == GoalKind.Virtue -> "Virtue ${index + 1}"
                else -> "Goal ${index + 1}"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            plural(line.daysKept, "day"),
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            color = faint,
        )
    }
}

/** Writes the card to the app's cache and opens the share sheet. */
private suspend fun shareImage(context: Context, bitmap: Bitmap, year: Int) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "stagira-$year.png").also { f ->
            // Hardware bitmaps can't be compressed directly.
            val soft = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            f.outputStream().use { soft.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    (context.applicationContext as LedgerApplication).lock.expectReturn()
    context.startActivity(Intent.createChooser(send, "Share your year"))
}
