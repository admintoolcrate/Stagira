package com.stagirite.ledger.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.MainActivity
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.sortGoals
import java.time.LocalDate

private val Paper = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF191919))
private val Ink = ColorProvider(day = Color(0xFF141414), night = Color(0xFFF2F2F0))
private val Muted = ColorProvider(day = Color(0xFF6B6B68), night = Color(0xFF9A9A97))

/**
 * Home-screen widget: up to four goals and their counts. Private goals show
 * their cover names; with the app lock on, it shows nothing but "Locked".
 */
class LedgerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LedgerApplication
        val locked = app.settings.appLock
        val today = LocalDate.now()
        val goals = if (locked) emptyList() else sortGoals(app.store.goals, app.settings.sortOrder, today).take(4)
        provideContent { WidgetContent(goals, locked, today) }
    }

    companion object {
        /** Redraws every placed widget. */
        suspend fun refresh(context: Context) = LedgerWidget().updateAll(context)
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(goals: List<Goal>, locked: Boolean, today: LocalDate) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(Paper)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text("Stagirite", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(8.dp))
        when {
            locked -> Text("Locked", style = TextStyle(color = Ink, fontSize = 16.sp))
            goals.isEmpty() -> Text("Add a goal to start counting.", style = TextStyle(color = Ink, fontSize = 14.sp))
            else -> goals.forEach { goal ->
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        goal.title,
                        style = TextStyle(color = Ink, fontSize = 14.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        "${goal.daysKept(today)}",
                        style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Normal),
                    )
                }
            }
        }
    }
}

class LedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LedgerWidget()
}
