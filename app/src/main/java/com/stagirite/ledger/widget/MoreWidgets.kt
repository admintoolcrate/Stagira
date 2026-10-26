package com.stagirite.ledger.widget

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.stagirite.ledger.data.DayState
import com.stagirite.ledger.data.overviewDayState
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

    // The chosen goal lives in the widget's own state, so choosing one redraws it straight away.
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LedgerApplication
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val choose = Intent(context, GoalWidgetSetup::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        provideContent {
            val colours = palette(app.settings.widgetStyle)
            val goalId = currentState(ChosenGoal) ?: WidgetGoals.goalFor(context, appWidgetId)
            val goal = goalId?.let(app.store::find)?.takeIf { !it.archived }
            val roomy = LocalSize.current.width >= 250.dp
            Column(widgetRoot(colours).clickable(actionStartActivity<MainActivity>())) {
                when {
                    app.settings.appLock -> Message("Locked", "Tap to open", colours)
                    goal == null -> Column(GlanceModifier.clickable(actionStartActivity(choose))) {
                        Message("Choose a goal", "Tap to pick the goal to show here.", colours)
                    }
                    else -> Hero(goal, iconsFor(context, listOf(goal)), LocalDate.now(), colours, roomy)
                }
            }
        }
    }

    companion object {
        val ChosenGoal = stringPreferencesKey("goal")
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
        provideContent {
            val colours = palette(app.settings.widgetStyle)
            val locked = app.settings.appLock
            val goals = if (locked) emptyList() else app.store.goals
            Column(widgetRoot(colours).clickable(actionStartActivity<MainActivity>())) {
                when {
                    locked -> Message("Locked", "Tap to open", colours)
                    goals.isEmpty() -> Message("No goals yet", "Tap to add your first.", colours)
                    else -> OverviewContent(goals, LocalDate.now(), colours, roomy = LocalSize.current.width >= 250.dp)
                }
            }
        }
    }
}

/**
 * Header, then the week as seven marks, the figures, and the next milestone
 * with its progress line along the bottom: the same rhythm as the Goal widget.
 */
@Composable
private fun OverviewContent(goals: List<Goal>, today: LocalDate, c: Palette, roomy: Boolean) {
    val week = weekReview(goals, today)
    val kept = goals.sumOf { it.stats(today).daysKept }
    val best = goals.maxOf { it.daysKept(today) }
    val next = upcomingMilestones(goals, today).firstOrNull()
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Header(c)
            Spacer(GlanceModifier.defaultWeight())
            if (roomy) WeekMarks(goals, today, c)
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Figure("${week.cleanDays}/${week.activeDays}", "kept this week", c, GlanceModifier.defaultWeight())
            if (roomy) {
                Figure("$best", "best run now", c, GlanceModifier.defaultWeight())
                Figure("$kept", "days kept in all", c, GlanceModifier.defaultWeight())
            }
        }
        if (next != null) {
            Spacer(GlanceModifier.height(10.dp))
            Text(
                "Next: ${next.goal.title}, ${Milestones.name(next.milestone, next.goal.currentStart) ?: days(next.milestone)} " +
                    if (next.inDays == 1L) "tomorrow" else "in ${days(next.inDays)}",
                style = TextStyle(color = c.muted, fontSize = 11.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(6.dp))
            Progress(next.goal.daysKept(today), next.goal.currentStart, c)
        }
    }
}

/** The last seven days, oldest first: a filled mark for a kept day, an open one for a slip. */
@Composable
private fun WeekMarks(goals: List<Goal>, today: LocalDate, c: Palette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        (6 downTo 0).forEach { back ->
            val day = today.minusDays(back.toLong())
            val mark = when (overviewDayState(goals, day, today)) {
                DayState.Kept -> "\u25CF"
                DayState.Slip -> "\u25CB"
                else -> "\u00B7"
            }
            Text(
                mark,
                style = TextStyle(color = if (back == 0) c.ink else c.muted, fontSize = 10.sp),
                modifier = GlanceModifier.padding(start = 4.dp),
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
