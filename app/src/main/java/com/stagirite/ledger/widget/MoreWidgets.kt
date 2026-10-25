package com.stagirite.ledger.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.MainActivity
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.Milestones
import com.stagirite.ledger.data.stats
import com.stagirite.ledger.data.upcomingMilestones
import com.stagirite.ledger.data.weekReview
import java.time.LocalDate

/** Which goal each Goal widget shows, by app-widget id. */
object WidgetGoals {
    private fun prefs(context: Context) = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    fun goalFor(context: Context, appWidgetId: Int): String? = prefs(context).getString("goal_$appWidgetId", null)

    fun set(context: Context, appWidgetId: Int, goalId: String) =
        prefs(context).edit().putString("goal_$appWidgetId", goalId).apply()

    fun forget(context: Context, appWidgetIds: IntArray) =
        prefs(context).edit().apply { appWidgetIds.forEach { remove("goal_$it") } }.apply()
}

/**
 * The Goal widget (free): one goal of your choosing, its count large, with
 * the next milestone and a progress line. Choose the goal when placing it, or
 * change it from the widget's settings.
 */
class GoalWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LedgerApplication
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val locked = app.settings.appLock
        val colours = palette(app.settings.widgetStyle)
        val goal = WidgetGoals.goalFor(context, appWidgetId)?.let(app.store::find)?.takeIf { !it.archived }
        val icons = goal?.icon?.takeIf { it.isNotEmpty() }?.let { mapOf(it to emojiBitmap(context, it)) } ?: emptyMap()
        val choose = Intent(context, GoalWidgetSetup::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        provideContent {
            val roomy = LocalSize.current.width >= 250.dp
            Column(widgetRoot(colours).clickable(actionStartActivity<MainActivity>())) {
                when {
                    locked -> Message("Locked", "Tap to open", colours)
                    goal == null -> Column(GlanceModifier.clickable(actionStartActivity(choose))) {
                        Message("Choose a goal", "Tap to pick the goal to show here.", colours)
                    }
                    else -> Hero(goal, icons, LocalDate.now(), colours, roomy)
                }
            }
        }
    }
}

class GoalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetGoals.forget(context, appWidgetIds)
    }
}

/**
 * The Overview widget (free): how all goals are going together. This week's
 * kept days, all days kept, and the next milestone coming up.
 */
class OverviewWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LedgerApplication
        val locked = app.settings.appLock
        val colours = palette(app.settings.widgetStyle)
        val goals = if (locked) emptyList() else app.store.goals
        val today = LocalDate.now()
        provideContent {
            Column(widgetRoot(colours).clickable(actionStartActivity<MainActivity>())) {
                when {
                    locked -> Message("Locked", "Tap to open", colours)
                    goals.isEmpty() -> Message("No goals yet", "Tap to add your first.", colours)
                    else -> OverviewContent(goals, today, colours, roomy = LocalSize.current.width >= 250.dp)
                }
            }
        }
    }
}

@Composable
private fun OverviewContent(goals: List<Goal>, today: LocalDate, c: Palette, roomy: Boolean) {
    val week = weekReview(goals, today)
    val kept = goals.sumOf { it.stats(today).daysKept }
    val next = upcomingMilestones(goals, today).firstOrNull()
    Column {
        Header(c)
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth()) {
            Figure("${week.cleanDays}/${week.activeDays}", "this week", c, GlanceModifier.defaultWeight())
            if (roomy) {
                Figure("$kept", "days kept", c, GlanceModifier.defaultWeight())
                Figure("${goals.size}", if (goals.size == 1) "goal" else "goals", c, GlanceModifier.defaultWeight())
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        if (next != null) {
            Text(
                "Next: ${next.goal.title}, ${Milestones.name(next.milestone) ?: days(next.milestone)} " +
                    if (next.inDays == 1L) "tomorrow" else "in ${days(next.inDays)}",
                style = TextStyle(color = c.muted, fontSize = 11.sp),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun Figure(value: String, label: String, c: Palette, modifier: GlanceModifier) {
    Column(modifier) {
        Text(value, style = TextStyle(color = c.ink, fontSize = 30.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        Text(label, style = TextStyle(color = c.muted, fontSize = 11.sp), maxLines = 1)
    }
}

class OverviewWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OverviewWidget()
}
