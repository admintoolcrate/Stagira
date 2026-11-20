package com.stagirite.ledger.intake

import android.app.Activity

import android.content.Intent
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
import com.stagirite.ledger.MainActivity
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.ui.LedgerTheme
import com.stagirite.ledger.ui.Tone
import com.stagirite.ledger.widget.EXTRA_OPEN_GOAL

/**
 * Lyceum's links to Stagira (Lyceum's docs/stagira-intent.md), on the phone only:
 *
 *  - PICK_GOAL, started for a result: the person picks one of their goals. Returns `goal_id`
 *    and `goal_title` (the title as shown in the list: a private goal's cover name, never its
 *    real one). Respects the app lock. Backing out returns RESULT_CANCELED.
 *  - VIEW_GOAL with `goal_id`: opens that goal's page in Stagira, behind Stagira's own lock.
 */
object GoalLinks {
    const val ACTION_PICK = "cc.toolcrate.stagira.action.PICK_GOAL"
    const val ACTION_VIEW = "cc.toolcrate.stagira.action.VIEW_GOAL"
    const val EXTRA_GOAL_ID = "goal_id"
    const val EXTRA_GOAL_TITLE = "goal_title"
    const val MAX_ID = 128
    const val MAX_TITLE = 120

    /** An id another app sent, if it is usable: non-blank, short, no control characters. */
    fun cleanId(id: String?): String? =
        id?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_ID && it.none(Char::isISOControl) }

    /** What PICK_GOAL hands back for [goal]: its id and its shown title, trimmed to fit. */
    fun result(goal: Goal): Pair<String, String> =
        goal.id to goal.title.filterNot(Char::isISOControl).trim().take(MAX_TITLE)
}

class PickGoalActivity : FragmentActivity() {
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as LedgerApplication
        setResult(Activity.RESULT_CANCELED)
        if (app.startupError != null) { finish(); return }
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
                        Text("Choose a goal", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Another app on this phone asks which goal to link. It will see only the goal's name as shown here.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!unlocked) {
                            Text("Stagira is locked.", style = MaterialTheme.typography.bodyLarge)
                            OutlinedButton(
                                { app.lock.prompt(this@PickGoalActivity, "Unlock Stagira") { app.lock.unlock(); unlocked = true } },
                                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            ) { Text("Unlock") }
                        } else {
                            val goals = app.store.goals
                            if (goals.isEmpty()) Text("You don't have a goal in Stagira yet.", style = MaterialTheme.typography.bodyLarge)
                            goals.forEach { g ->
                                OutlinedButton({ pick(g) }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                    Text(listOf(g.icon, g.title).filter { it.isNotBlank() }.joinToString(" "), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        TextButton({ finish() }, Modifier.heightIn(min = 48.dp)) { Text("Not now") }
                    }
                }
            }
        }
    }

    private fun pick(goal: Goal) {
        val (id, title) = GoalLinks.result(goal)
        setResult(Activity.RESULT_OK, Intent().putExtra(GoalLinks.EXTRA_GOAL_ID, id).putExtra(GoalLinks.EXTRA_GOAL_TITLE, title))
        finish()
    }
}

/** VIEW_GOAL: hands the id to MainActivity (which applies the app lock) and gets out of the way. */
class ViewGoalActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = GoalLinks.cleanId(intent.getStringExtra(GoalLinks.EXTRA_GOAL_ID))
        val open = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        // An unknown or deleted goal just opens the ledger.
        if (id != null && (application as? LedgerApplication)?.let { a -> a.startupError == null && a.store.find(id) != null } == true) {
            open.putExtra(EXTRA_OPEN_GOAL, id)
        }
        startActivity(open)
        finish()
    }

}
