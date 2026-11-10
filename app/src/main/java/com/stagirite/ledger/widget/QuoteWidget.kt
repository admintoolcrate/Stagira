package com.stagirite.ledger.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.MainActivity
import com.stagirite.ledger.data.Quotes
import java.time.LocalDate

/**
 * The Aristotle widget: a passage from the Nicomachean Ethics, a different one
 * each day, set in a serif with gold marks. Nothing about your goals is shown,
 * so it stays the same with the app lock on.
 */
class QuoteWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 57.dp), DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 180.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LedgerApplication
        provideContent {
            val c = palette(app.settings.widgetStyle)
            val size = LocalSize.current
            val short = size.height < SHORT
            // Shorter passages where there's less room, so the passage is read whole.
            val quote = Quotes.forWidget(LocalDate.now(), maxChars = if (short) 90 else if (size.height < 180.dp) 160 else 240)
            Column(widgetRoot(c).clickable(actionStartActivity<MainActivity>())) {
                if (!short) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Header(c)
                        Spacer(GlanceModifier.defaultWeight())
                        Text("“", style = TextStyle(color = c.accent, fontSize = 26.sp, fontFamily = FontFamily.Serif))
                    }
                    Spacer(GlanceModifier.height(4.dp))
                }
                Text(
                    quote.text,
                    style = TextStyle(
                        color = c.ink,
                        fontSize = if (short) 13.sp else 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                    ),
                    maxLines = if (short) 3 else if (size.height < 180.dp) 4 else 8,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("—", style = TextStyle(color = c.accent, fontSize = 11.sp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        "ARISTOTLE · ${quote.ref}",
                        style = TextStyle(color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()
}
