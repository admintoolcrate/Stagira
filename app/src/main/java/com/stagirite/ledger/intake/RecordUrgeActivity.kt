package com.stagirite.ledger.intake

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.Tone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lets Kairos (or another app on this phone) ask Stagira to record "an urge
 * resisted", with no server in between.
 *
 * Nothing is ever written silently: the person sees the request, picks which goal
 * it belongs to, and confirms. Stagira's own app lock is respected, private goals
 * show only their cover names, and nothing about the ledger is returned to the caller
 * beyond RESULT_OK or RESULT_CANCELED.
 *
 * Contract:
 *   action  cc.toolcrate.stagira.action.RECORD_URGE
 *   extras  "source" (String, e.g. "kairos"), "at" (Long epoch millis, optional)
 * Only today or yesterday can be recorded; anything else is treated as today.
 */
class RecordUrgeActivity : FragmentActivity() {
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LedgerApplication
        if (app.startupError != null) { setResult(Activity.RESULT_CANCELED); finish(); return }
        setResult(Activity.RESULT_CANCELED)

        val day = dayFor(intent.getLongExtra(EXTRA_AT, -1L))
        val from = intent.getStringExtra(EXTRA_SOURCE)?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Another app"
        val last = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST, null)
        unlocked = !(app.settings.appLock && app.lock.locked)
        if (!unlocked) app.lock.prompt(this, "Unlock Stagira") { app.lock.unlock(); unlocked = true }

        setContent {
            val settings = app.settings
            val dark = when (settings.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            )
            LedgerTheme(dark, Tone.of(settings.palette)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Spacer(Modifier.height(24.dp))
                        Text("Record an urge resisted?", style = MaterialTheme.typography.headlineMedium)
                        val whenText = if (day == LocalDate.now()) "today" else "yesterday"
                        Text(
                            "$from asks to note an urge you rode out $whenText. Choose the goal it belongs to, or leave it.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!unlocked) {
                            Text("Stagira is locked.", style = MaterialTheme.typography.bodyLarge)
                            OutlinedButton({ app.lock.prompt(this@RecordUrgeActivity, "Unlock Stagira") { app.lock.unlock(); unlocked = true } },
                                Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Unlock") }
                        } else {
                            val goals = ordered(app.store.goals, last)
                            if (goals.isEmpty()) {
                                Text("You don't have a goal in Stagira yet.", style = MaterialTheme.typography.bodyLarge)
                            }
                            goals.forEach { g ->
                                OutlinedButton(
                                    onClick = { record(app, g, day) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                ) {
                                    Text(
                                        listOf(g.icon, g.title).filter { it.isNotBlank() }.joinToString(" "),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                        TextButton({ finish() }, Modifier.heightIn(min = 48.dp)) { Text("Not now") }
                    }
                }
            }
        }
    }

    private fun record(app: LedgerApplication, goal: Goal, day: LocalDate) {
        app.store.resistUrge(goal.id, day)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST, goal.id).apply()
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val ACTION = "cc.toolcrate.stagira.action.RECORD_URGE"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_AT = "at"
        private const val PREFS = "intake"
        private const val KEY_LAST = "lastUrgeGoal"

        /** Today or yesterday only; anything else (future, older, missing) becomes today. */
        fun dayFor(atMillis: Long, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): LocalDate {
            if (atMillis <= 0) return today
            val d = Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDate()
            return if (d == today || d == today.minusDays(1)) d else today
        }

        /** The last goal chosen first, then Vices (where urges belong), then the rest. */
        fun ordered(goals: List<Goal>, lastId: String?): List<Goal> =
            goals.sortedWith(compareBy({ it.id != lastId }, { it.kind != GoalKind.Vice }))
    }
}
