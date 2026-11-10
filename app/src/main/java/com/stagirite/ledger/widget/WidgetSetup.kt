package com.stagirite.ledger.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.data.WidgetStyle
import com.stagirite.ledger.ui.DuskGradient
import com.stagirite.ledger.ui.EmojiGlyph
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.SectionHeading
import com.stagirite.ledger.ui.Space
import kotlinx.coroutines.launch

/**
 * A widget's own settings, opened when it's placed (the Goal widget) or from the
 * widget itself (long-press → settings): its background, and for the Goal widget,
 * which goal it shows. With the app lock on, the goal list asks for the lock first,
 * since it shows goal names (cover names for private ones).
 */
class WidgetSetup : FragmentActivity() {
    private var unlocked by mutableStateOf(false)

    /** The widget's own style, or null for "as the app's setting". */
    private var style by mutableStateOf<WidgetStyle?>(null)
    private var goalId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className.orEmpty()
        val isGoal = provider.endsWith("GoalWidgetReceiver")
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // A Goal widget can't be placed without a goal, so backing out removes it. The others
        // work as they are, so backing out keeps them.
        setResult(if (isGoal) Activity.RESULT_CANCELED else Activity.RESULT_OK, result)

        val app = application as LedgerApplication
        goalId = WidgetGoals.goalFor(this, appWidgetId)
        lifecycleScope.launch {
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@WidgetSetup).getGlanceIdBy(appWidgetId)
                val prefs = getAppWidgetState(this@WidgetSetup, PreferencesGlanceStateDefinition, glanceId)
                style = widgetStyle(prefs[WidgetStyleKey], WidgetStyle.Solid).takeIf { prefs[WidgetStyleKey] != null }
                if (isGoal) prefs[GoalWidget.ChosenGoal]?.let { goalId = it }
            }
        }
        if (isGoal && app.settings.appLock) {
            app.lock.prompt(this, "Choose a goal for the widget") { unlocked = true }
        } else {
            unlocked = true
        }

        setContent {
            val dark = when (app.settings.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            LedgerTheme(dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier
                            .safeDrawingPadding()
                            .padding(horizontal = Space.l, vertical = Space.xl),
                    ) {
                        Text(
                            when {
                                isGoal -> "Goal widget"
                                provider.endsWith("OverviewWidgetReceiver") -> "Overview widget"
                                provider.endsWith("QuoteWidgetReceiver") -> "Aristotle widget"
                                else -> "Goals widget"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(Space.l))
                            SectionHeading("Background")
                            StyleRow(null, "Same as the app (${app.settings.widgetStyle.label})", style == null) { style = null }
                            WidgetStyle.entries.forEach { s -> StyleRow(s, s.label, style == s) { style = s } }

                            if (isGoal) {
                                Spacer(Modifier.height(Space.xl))
                                SectionHeading("Show which goal?")
                                val goals = app.store.goals
                                when {
                                    !unlocked -> Text("Unlock to choose.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    goals.isEmpty() -> Text(
                                        "Add a goal in the app first, then place the widget again.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    else -> goals.forEach { goal ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { goalId = goal.id }
                                                .padding(vertical = Space.s),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            RadioButton(selected = goalId == goal.id, onClick = { goalId = goal.id })
                                            if (goal.icon.isNotEmpty()) {
                                                EmojiGlyph(goal.icon, 20.sp)
                                                Spacer(Modifier.width(Space.m))
                                            }
                                            Text(goal.title, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(Space.m))
                        Button(
                            onClick = { save(appWidgetId, provider, isGoal, result) },
                            enabled = !isGoal || (goalId != null && app.store.find(goalId!!) != null),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) { Text("Save") }
                    }
                }
            }
        }
    }

    private fun save(appWidgetId: Int, provider: String, isGoal: Boolean, result: Intent) {
        val chosenGoal = goalId
        if (isGoal && chosenGoal != null) WidgetGoals.set(this, appWidgetId, chosenGoal)
        lifecycleScope.launch {
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@WidgetSetup).getGlanceIdBy(appWidgetId)
                // Writing the widget's own state is what makes a running widget redraw.
                updateAppWidgetState(this@WidgetSetup, glanceId) { prefs ->
                    val s = style
                    if (s == null) prefs.remove(WidgetStyleKey) else prefs[WidgetStyleKey] = s.name
                    if (isGoal && chosenGoal != null) prefs[GoalWidget.ChosenGoal] = chosenGoal
                }
                glanceFor(provider).update(this@WidgetSetup, glanceId)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun glanceFor(provider: String): GlanceAppWidget = when {
        provider.endsWith("GoalWidgetReceiver") -> GoalWidget()
        provider.endsWith("OverviewWidgetReceiver") -> OverviewWidget()
        provider.endsWith("QuoteWidgetReceiver") -> QuoteWidget()
        else -> LedgerWidget()
    }
}

@Composable
private fun StyleRow(style: WidgetStyle?, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Swatch(style)
        Spacer(Modifier.width(Space.m))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A small preview of each background. */
@Composable
private fun Swatch(style: WidgetStyle?) {
    val shape = RoundedCornerShape(10.dp)
    val edge = MaterialTheme.colorScheme.outlineVariant
    val base = Modifier.size(width = 40.dp, height = 28.dp)
    val modifier = when (style) {
        null -> base.border(1.dp, MaterialTheme.colorScheme.outline, shape)
        WidgetStyle.Solid -> base.background(Color(0xFFFCFBF8), shape).border(1.dp, edge, shape)
        WidgetStyle.Translucent -> base.background(Color(0x99FCFBF8), shape).border(1.dp, edge, shape)
        WidgetStyle.Clear -> base.background(Color(0xFF6B6F78), shape)
        WidgetStyle.Dusk -> base.background(Brush.linearGradient(DuskGradient), shape)
        WidgetStyle.Dawn -> base.background(Brush.linearGradient(listOf(Color(0xFFF7E1CC), Color(0xFFF3D9D8), Color(0xFFE6E1F0))), shape)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (style == WidgetStyle.Clear) Text("Aa", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
