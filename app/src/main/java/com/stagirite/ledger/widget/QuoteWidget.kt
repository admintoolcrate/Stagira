package com.stagirite.ledger.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.MainActivity
import com.stagirite.ledger.R
import com.stagirite.ledger.data.Quote
import com.stagirite.ledger.data.Quotes
import java.time.LocalDate

/**
 * The Aristotle widget: a passage from the Nicomachean Ethics, a different one
 * each day, laid out as the app sets its quotations: EB Garamond, a gold mark
 * before and after, then a short rule and the attribution. The passage is sized
 * to fill the widget without being cut off. Nothing about your goals is shown,
 * so it stays the same with the app lock on.
 */
class QuoteWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 57.dp), DpSize(250.dp, 57.dp), DpSize(180.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 180.dp), DpSize(320.dp, 250.dp)),
    )

    // Holds the widget's own style, chosen from its settings.
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LedgerApplication
        provideContent {
            val c = palette(widgetStyle(currentState(WidgetStyleKey), app.settings.widgetStyle))
            Column(widgetRoot(c).clickable(actionStartActivity<MainActivity>())) {
                QuoteContent(LocalDate.now(), c)
            }
        }
    }
}

@Composable
private fun ColumnScope.QuoteContent(today: LocalDate, c: Palette) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val short = size.height < SHORT
    // The space inside the card (see widgetRoot's padding), less the marks and the attribution.
    val width = size.width - if (short) 24.dp else 36.dp
    val height = size.height - if (short) 16.dp else if (size.height < 150.dp) 32.dp + 22.dp else 32.dp + 66.dp
    val fit = fitPassage(context, Quotes.widgetPool(today), (width.value * density).toInt(), (height.value * density).toInt(), short)
        ?: return

    if (short) {
        // One row high: just the passage.
        Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.CenterStart) { Passage(fit, c, density) }
        return
    }
    if (size.height >= 150.dp) {
        SerifText("“", 34f, c.accent)
        Spacer(GlanceModifier.height(8.dp))
    }
    Passage(fit, c, density)
    Spacer(GlanceModifier.defaultWeight())
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.width(18.dp).height(1.dp).background(c.accent)) {}
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text("ARISTOTLE", style = TextStyle(color = c.ink, fontSize = 10.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            if (size.height >= 150.dp) {
                Text(fit.quote.work, style = TextStyle(color = c.muted, fontSize = 10.sp), maxLines = 1)
            }
        }
        if (size.height >= 150.dp) SerifText("”", 34f, c.accent)
    }
}

@Composable
private fun Passage(fit: Fitted, c: Palette, density: Float) {
    Image(
        ImageProvider(fit.bitmap),
        contentDescription = "${fit.quote.text} Aristotle, ${fit.quote.work}",
        colorFilter = ColorFilter.tint(c.ink),
        modifier = GlanceModifier.size((fit.bitmap.width / density).dp, (fit.bitmap.height / density).dp),
    )
}

private class Fitted(val quote: Quote, val bitmap: Bitmap)

private val passageCache = HashMap<String, Fitted?>()

/**
 * Picks the day's passage, or the next one that fits, at the largest size between
 * 22sp and a readable minimum that fits the space; drawn in EB Garamond, white, to tint.
 */
private fun fitPassage(context: Context, pool: List<Quote>, widthPx: Int, heightPx: Int, short: Boolean): Fitted? {
    if (widthPx <= 0 || heightPx <= 0) return null
    val scale = context.resources.configuration.fontScale.coerceIn(1f, 1.25f)
    val key = "${pool.first().text.hashCode()}|$widthPx|$heightPx|$scale"
    synchronized(passageCache) { passageCache[key]?.let { return it } }
    val density = context.resources.displayMetrics.density
    val typeface = ResourcesCompat.getFont(context, R.font.eb_garamond_regular)
    val sizes = (if (short) 15 downTo 12 else 22 downTo 14).map { it.toFloat() }
    var result: Fitted? = null
    search@ for (quote in pool) {
        val text = quote.text
        for (sp in sizes) {
            val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = sp * density * scale
                color = android.graphics.Color.WHITE
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.12f)
                .setIncludePad(false)
                .build()
            if (layout.height <= heightPx) {
                val bitmap = Bitmap.createBitmap(widthPx, layout.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                layout.draw(Canvas(bitmap))
                result = Fitted(quote, bitmap)
                break@search
            }
        }
    }
    synchronized(passageCache) {
        if (passageCache.size > 24) passageCache.clear()
        passageCache[key] = result
    }
    return result
}

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()
}
