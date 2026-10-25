package com.stagirite.ledger.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.MainActivity
import com.stagirite.ledger.R
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.Milestones
import com.stagirite.ledger.data.sortGoals
import java.time.LocalDate

private val Paper = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF161616))
private val Ink = ColorProvider(day = Color(0xFF141414), night = Color(0xFFF2F2F0))
private val Muted = ColorProvider(day = Color(0xFF7A7A76), night = Color(0xFF8E8E8A))
private val Track = ColorProvider(day = Color(0xFFE6E6E3), night = Color(0xFF2C2C2A))

/** Intent extra: the goal to open when a row is tapped. */
const val EXTRA_OPEN_GOAL = "open_goal"
private val OpenGoal = ActionParameters.Key<String>(EXTRA_OPEN_GOAL)

private val Small = DpSize(110.dp, 110.dp)
private val Wide = DpSize(250.dp, 110.dp)
private val Tall = DpSize(250.dp, 250.dp)

/**
 * Home-screen widget. Small, it's one goal and its count; wider or taller, a
 * list with each goal's progress to its next milestone. Private goals show
 * their cover names; with the app lock on, it shows nothing but "Locked".
 */
class LedgerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Small, Wide, Tall))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LedgerApplication
        val locked = app.settings.appLock
        val today = LocalDate.now()
        val plus = app.billing.plus
        val goals = if (locked || !plus) emptyList() else sortGoals(app.store.goals, app.settings.sortOrder, today).take(6)
        val icons = goals.map { it.icon }.filter { it.isNotEmpty() }.distinct().associateWith { emojiBitmap(context, it) }
        provideContent { WidgetContent(goals, icons, locked, plus, today) }
    }

    companion object {
        /** Redraws every placed widget. */
        suspend fun refresh(context: Context) = LedgerWidget().updateAll(context)
    }
}

/**
 * Glance can't use the app's fonts, so icons are drawn with Noto Emoji into a
 * white bitmap and tinted to match the theme, keeping them black and white.
 */
private fun emojiBitmap(context: Context, emoji: String): Bitmap {
    val px = (40 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.noto_emoji)
        textSize = px * 0.8f
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val metrics = paint.fontMetrics
    Canvas(bitmap).drawText(emoji, px / 2f, px / 2f - (metrics.ascent + metrics.descent) / 2f, paint)
    return bitmap
}

@Composable
private fun WidgetContent(goals: List<Goal>, icons: Map<String, Bitmap>, locked: Boolean, plus: Boolean, today: LocalDate) {
    val size = LocalSize.current
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(Paper)
            .cornerRadius(24.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        when {
            locked -> Message("Locked", "Tap to open")
            !plus -> Message("Widget", "Part of Stagira Plus. Tap to open the app.")
            goals.isEmpty() -> Message("No goals yet", "Tap to add your first.")
            size.width < Wide.width -> Single(goals.first(), icons, today)
            else -> {
                Header()
                Spacer(GlanceModifier.height(8.dp))
                val rows = if (size.height < Tall.height) 2 else 5
                goals.take(rows).forEach { GoalRow(it, icons, today) }
            }
        }
    }
}

@Composable
private fun Header() {
    Text("STAGIRA", style = TextStyle(color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
}

@Composable
private fun ColumnScope.Message(title: String, body: String) {
    Header()
    Spacer(GlanceModifier.defaultWeight())
    Text(title, style = TextStyle(color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium))
    Spacer(GlanceModifier.height(2.dp))
    Text(body, style = TextStyle(color = Muted, fontSize = 12.sp), maxLines = 2)
}

@Composable
private fun Icon(goal: Goal, icons: Map<String, Bitmap>, dp: Int) {
    val bitmap = icons[goal.icon] ?: return
    Image(
        ImageProvider(bitmap),
        contentDescription = null,
        colorFilter = ColorFilter.tint(Ink),
        modifier = GlanceModifier.size(dp.dp),
    )
}

/** The small widget: one goal, its count large. */
@Composable
private fun Single(goal: Goal, icons: Map<String, Bitmap>, today: LocalDate) {
    val days = goal.daysKept(today)
    Column(GlanceModifier.fillMaxSize().clickable(openGoal(goal))) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Header()
            Spacer(GlanceModifier.defaultWeight())
            Icon(goal, icons, 18)
        }
        Spacer(GlanceModifier.defaultWeight())
        Text("$days", style = TextStyle(color = Ink, fontSize = 40.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        Text(
            if (days == 1L) "day" else "days",
            style = TextStyle(color = Muted, fontSize = 12.sp),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(goal.title, style = TextStyle(color = Ink, fontSize = 13.sp), maxLines = 1)
    }
}

/** One line of the list: icon, title, the next milestone, and the count. */
@Composable
private fun GoalRow(goal: Goal, icons: Map<String, Bitmap>, today: LocalDate) {
    val days = goal.daysKept(today)
    val next = Milestones.next(days)
    Column(GlanceModifier.fillMaxWidth().padding(vertical = 5.dp).clickable(openGoal(goal))) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (icons[goal.icon] != null) {
                Box(GlanceModifier.size(26.dp), contentAlignment = Alignment.CenterStart) { Icon(goal, icons, 18) }
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(goal.title, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Text(
                    "Next: ${Milestones.name(next) ?: "$next days"} \u00B7 ${next - days} to go",
                    style = TextStyle(color = Muted, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Text("$days", style = TextStyle(color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
        Spacer(GlanceModifier.height(5.dp))
        LinearProgressIndicator(
            progress = Milestones.progress(days),
            modifier = GlanceModifier.fillMaxWidth().height(2.dp),
            color = Ink,
            backgroundColor = Track,
        )
    }
}

private fun openGoal(goal: Goal) = actionStartActivity<MainActivity>(actionParametersOf(OpenGoal to goal.id))

class LedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LedgerWidget()
}
