package com.stagirite.ledger.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.ui.EmojiGlyph
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.Space
import kotlinx.coroutines.launch

/**
 * Picks the goal a Goal widget shows. With the app lock on, it asks for the
 * lock first, since the list shows goal names (cover names for private ones).
 */
class GoalWidgetSetup : FragmentActivity() {
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // Backing out leaves no half-made widget behind.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val app = application as LedgerApplication
        if (app.settings.appLock) {
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
                    Column(Modifier.safeDrawingPadding().padding(horizontal = Space.l, vertical = Space.xl)) {
                        Text("Show which goal?", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.padding(top = Space.l))
                        val goals = app.store.goals
                        when {
                            !unlocked -> Text("Unlock to choose.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            goals.isEmpty() -> Text(
                                "Add a goal in the app first, then place the widget again.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            else -> LazyColumn {
                                items(goals, key = { it.id }) { goal ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { choose(appWidgetId, goal.id) }
                                            .padding(vertical = Space.l),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
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
                }
            }
        }
    }

    private fun choose(appWidgetId: Int, goalId: String) {
        WidgetGoals.set(this, appWidgetId, goalId)
        lifecycleScope.launch {
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@GoalWidgetSetup).getGlanceIdBy(appWidgetId)
                GoalWidget().update(this@GoalWidgetSetup, glanceId)
            }
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}
