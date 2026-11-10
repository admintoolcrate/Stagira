package com.stagirite.ledger.widget

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.stagirite.ledger.data.dayState
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.DayState
import androidx.glance.text.TextDecoration
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
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
import androidx.glance.LocalContext
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
import androidx.glance.unit.ColorProvider
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
import com.stagirite.ledger.data.WidgetStyle
import com.stagirite.ledger.data.sortGoals
import java.time.LocalDate

/** Colours for one widget style; day and night follow the phone's theme. */
internal class Palette(
    val background: ColorProvider?,
    val ink: ColorProvider,
    val muted: ColorProvider,
    val track: ColorProvider,
    /** The muted gold the app and website keep for milestones. */
    val accent: ColorProvider,
    /** A drawn background (the Dusk gradient) in place of a flat colour. */
    val image: Int? = null,
)

private val Gold = ColorProvider(day = Color(0xFFA4814F), night = Color(0xFFD0AE74))

internal fun palette(style: WidgetStyle): Palette = when (style) {
    WidgetStyle.Solid -> Palette(
        // The app's warm paper and ink.
        background = ColorProvider(day = Color(0xFFFCFBF8), night = Color(0xFF1C1B1E)),
        ink = ColorProvider(day = Color(0xFF1F1D1A), night = Color(0xFFEFEBE4)),
        muted = ColorProvider(day = Color(0xFF7A756C), night = Color(0xFF9C968C)),
        track = ColorProvider(day = Color(0xFFE3DDD3), night = Color(0xFF2A2826)),
        accent = Gold,
    )
    WidgetStyle.Translucent -> Palette(
        background = ColorProvider(day = Color(0xB8FCFBF8), night = Color(0xA6121214)),
        ink = ColorProvider(day = Color(0xFF1F1D1A), night = Color(0xFFEFEBE4)),
        muted = ColorProvider(day = Color(0xFF55514A), night = Color(0xFFB8B2A8)),
        track = ColorProvider(day = Color(0x33000000), night = Color(0x40FFFFFF)),
        accent = Gold,
    )
    // No card at all: white type, like the system clock, over the wallpaper.
    WidgetStyle.Clear -> Palette(
        background = null,
        ink = ColorProvider(day = Color.White, night = Color.White),
        muted = ColorProvider(day = Color(0xD9FFFFFF), night = Color(0xD9FFFFFF)),
        track = ColorProvider(day = Color(0x4DFFFFFF), night = Color(0x4DFFFFFF)),
        accent = ColorProvider(day = Color(0xFFF0D7A6), night = Color(0xFFF0D7A6)),
    )
    // The website's dusk panel, the same by day and night.
    WidgetStyle.Dusk -> Palette(
        background = null,
        ink = ColorProvider(day = Color(0xFFF2EDE4), night = Color(0xFFF2EDE4)),
        muted = ColorProvider(day = Color(0xB3F2EDE4), night = Color(0xB3F2EDE4)),
        track = ColorProvider(day = Color(0x38F2EDE4), night = Color(0x38F2EDE4)),
        accent = ColorProvider(day = Color(0xFFD0AE74), night = Color(0xFFD0AE74)),
        image = R.drawable.widget_dusk,
    )
}

/** Intent extra: the goal to open when a row is tapped. */
const val EXTRA_OPEN_GOAL = "open_goal"
private val OpenGoal = ActionParameters.Key<String>(EXTRA_OPEN_GOAL)

private val Small = DpSize(110.dp, 110.dp)
private val Wide = DpSize(250.dp, 110.dp)
private val Tall = DpSize(250.dp, 250.dp)

/**
 * The Goals widget. One goal (or a small widget) gets the count large with
 * its next milestone; more goals share the space as a list, each with a
 * progress line. Private goals show their cover names; with the app lock on,
 * it shows nothing but "Locked".
 */
class LedgerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Small, Wide, Tall))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LedgerApplication
        // Everything is read inside the content, so a redraw (or any change to the ledger while
        // the widget is live) shows the current state rather than what was true when it started.
        provideContent {
            val today = LocalDate.now()
            val locked = app.settings.widgetsLocked
            val goals = if (locked) emptyList() else sortGoals(app.store.goals, app.settings.sortOrder, today).take(6)
            WidgetContent(goals, iconsFor(context, goals), locked, today, palette(app.settings.widgetStyle))
        }
    }

    companion object {
        /** Redraws every placed widget, of all four kinds. */
        suspend fun refresh(context: Context) {
            LedgerWidget().updateAll(context)
            GoalWidget().updateAll(context)
            OverviewWidget().updateAll(context)
            QuoteWidget().updateAll(context)
        }
    }
}

private val iconCache = HashMap<String, Bitmap>()

/** Icon bitmaps for [goals], drawn once per emoji and reused across redraws. */
internal fun iconsFor(context: Context, goals: List<Goal>): Map<String, Bitmap> = synchronized(iconCache) {
    goals.map { it.icon }.filter { it.isNotEmpty() }.distinct()
        .associateWith { iconCache.getOrPut(it) { emojiBitmap(context, it) } }
}

/**
 * Glance can't use the app's fonts, so icons are drawn with Noto Emoji into a
 * white bitmap and tinted to match, keeping them black and white.
 */
internal fun emojiBitmap(context: Context, emoji: String): Bitmap {
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

internal fun days(n: Long) = if (n == 1L) "1 day" else "$n days"

/** "Next: 1 week, in 2 days", or how the first day reads. */
internal fun nextLine(days: Long, start: LocalDate): String {
    if (days == 0L) return "Day one is tomorrow"
    val next = Milestones.next(days, start)
    return "Next: ${Milestones.name(next, start) ?: days(next)}, in ${days(next - days)}"
}

@Composable
private fun WidgetContent(
    goals: List<Goal>,
    icons: Map<String, Bitmap>,
    locked: Boolean,
    today: LocalDate,
    c: Palette,
) {
    val size = LocalSize.current
    Column(widgetRoot(c).clickable(actionStartActivity<MainActivity>())) {
        when {
            locked -> Message("Locked", "Tap to open", c)
            goals.isEmpty() -> Message("No goals yet", "Tap to add your first.", c)
            goals.size == 1 || size.width < Wide.width -> Hero(goals.first(), icons, today, c, roomy = size.width >= Wide.width)
            else -> {
                Header(c)
                Spacer(GlanceModifier.height(6.dp))
                // Rows share the height evenly, so two goals fill the widget as well as five.
                val fits = if (size.height < Tall.height) 2 else 5
                goals.take(fits).forEach { goal ->
                    Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.CenterStart) {
                        GoalRow(goal, icons, today, c)
                    }
                }
            }
        }
    }
}

/** The card (or none, for Clear) every widget sits on; tighter at one row high. */
@Composable
internal fun widgetRoot(c: Palette): GlanceModifier {
    val short = LocalSize.current.height < SHORT
    var root = GlanceModifier.fillMaxSize()
    if (c.image != null) root = root.background(ImageProvider(c.image)).cornerRadius(if (short) 18.dp else 24.dp)
    else if (c.background != null) root = root.background(c.background).cornerRadius(if (short) 18.dp else 24.dp)
    return if (short) root.padding(horizontal = 12.dp, vertical = 8.dp) else root.padding(horizontal = 18.dp, vertical = 16.dp)
}

/** Below this height a widget is one row of the home screen, and lays out in a line. */
internal val SHORT = 100.dp

@Composable
internal fun Header(c: Palette) {
    // As on the website: a small gold mark before the spaced capitals.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("\u25CF", style = TextStyle(color = c.accent, fontSize = 7.sp))
        Spacer(GlanceModifier.width(5.dp))
        Text("STAGIRA", style = TextStyle(color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
    }
}

private val serifCache = HashMap<String, Bitmap>()

/** [text] drawn in EB Garamond, white, to be tinted; Glance itself can't use the app's fonts. */
internal fun serifBitmap(context: Context, text: String, px: Float): Bitmap = synchronized(serifCache) {
    serifCache.getOrPut("$text|${px.toInt()}") {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.eb_garamond_regular)
            textSize = px
            color = android.graphics.Color.WHITE
            fontFeatureSettings = "tnum"
        }
        // Cropped to the ink itself, so a count lines up with the words beside it.
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val pad = 2
        val w = (bounds.width() + pad * 2).coerceAtLeast(1)
        val h = (bounds.height() + pad * 2).coerceAtLeast(1)
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawText(text, pad - bounds.left.toFloat(), pad - bounds.top.toFloat(), paint)
        }
    }
}

/** A count set in the app's serif, as in the app itself. Scales with the text size up to 1.25×. */
@Composable
internal fun SerifText(text: String, size: Float, color: ColorProvider) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val scale = context.resources.configuration.fontScale.coerceIn(1f, 1.25f)
    val bitmap = serifBitmap(context, text, size * density * scale)
    Image(
        ImageProvider(bitmap),
        contentDescription = text,
        colorFilter = ColorFilter.tint(color),
        modifier = GlanceModifier.size((bitmap.width / density).dp, (bitmap.height / density).dp),
    )
}

/** A goal's count, or "Today" on a new goal's first day. */
@Composable
internal fun Count(goal: Goal, days: Long, size: Float, c: Palette) {
    val fresh = days == 0L && goal.slips.isEmpty()
    SerifText(if (fresh) "Today" else "$days", if (fresh) size * 0.6f else size, c.ink)
}

@Composable
internal fun ColumnScope.Message(title: String, body: String, c: Palette) {
    Header(c)
    Spacer(GlanceModifier.defaultWeight())
    Text(title, style = TextStyle(color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Medium))
    Spacer(GlanceModifier.height(2.dp))
    Text(body, style = TextStyle(color = c.muted, fontSize = 12.sp), maxLines = 2)
}

@Composable
internal fun Icon(goal: Goal, icons: Map<String, Bitmap>, c: Palette, dp: Int) {
    val bitmap = icons[goal.icon] ?: return
    Image(
        ImageProvider(bitmap),
        contentDescription = null,
        colorFilter = ColorFilter.tint(c.ink),
        modifier = GlanceModifier.size(dp.dp),
    )
}

/** One goal, its count large, with the next milestone and a progress line. */
@Composable
internal fun Hero(goal: Goal, icons: Map<String, Bitmap>, today: LocalDate, c: Palette, roomy: Boolean) {
    val days = goal.daysKept(today)
    val size = LocalSize.current
    // One row high: a single line. One cell: just the icon and the count.
    if (size.height < SHORT) {
        if (size.width < 100.dp) TinyGoal(goal, icons, days, c) else StripGoal(goal, icons, days, c, wide = size.width >= 200.dp)
        return
    }
    Column(GlanceModifier.fillMaxSize().clickable(openGoal(goal))) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (icons[goal.icon] != null) {
                Icon(goal, icons, c, 22)
                Spacer(GlanceModifier.width(8.dp))
            }
            Text(
                goal.title,
                style = TextStyle(color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            // A Vice's moment of resolve, logged from the home screen; a Virtue has nothing to log on a good day.
            if (roomy && goal.kind != GoalKind.Virtue) ResistButton(goal, c) else if (roomy) Header(c)
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(verticalAlignment = Alignment.Bottom) {
            Count(goal, days, if (roomy) 60f else 50f, c)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                when (days) {
                    0L -> if (goal.slips.isEmpty()) "started" else "day one tomorrow"
                    1L -> "day"
                    else -> "days"
                },
                style = TextStyle(color = c.muted, fontSize = 13.sp),
                modifier = GlanceModifier.padding(bottom = 1.dp),
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        val urges = goal.urgesOn(today)
        Text(
            buildString {
                append(statusLine(goal, days))
                if (roomy && urges > 0) append(" \u00B7 resisted ${if (urges == 1) "once" else "${urges}\u00D7"} today")
            },
            style = TextStyle(
                color = if (Milestones.isMilestone(days, goal.currentStart)) c.accent else c.muted,
                fontSize = 11.sp,
                fontWeight = if (Milestones.isMilestone(days, goal.currentStart)) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = 1,
        )
        // With height to spare, the goal's own week, as on the Overview widget.
        if (size.height >= 170.dp) {
            Spacer(GlanceModifier.height(8.dp))
            WeekLetters(today, c) { goal.dayState(it, today) }
        }
        Spacer(GlanceModifier.height(6.dp))
        Progress(days, goal.currentStart, c)
    }
}

/** "Milestone today: 1 week" on the day, otherwise the next one coming. */
internal fun statusLine(goal: Goal, days: Long): String =
    if (Milestones.isMilestone(days, goal.currentStart)) {
        "Milestone today: ${Milestones.name(days, goal.currentStart) ?: days(days)}"
    } else {
        nextLine(days, goal.currentStart)
    }

/** "Resisted +1": logs an urge resisted today for this goal, without opening the app. */
@Composable
private fun ResistButton(goal: Goal, c: Palette) {
    Text(
        "Resisted +1",
        style = TextStyle(color = c.ink, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        modifier = GlanceModifier
            .cornerRadius(12.dp)
            .background(c.track)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clickable(actionRunCallback<ResistUrgeAction>(actionParametersOf(UrgeGoal to goal.id))),
    )
}

private val UrgeGoal = ActionParameters.Key<String>("urge_goal")

/** Runs the "Resisted +1" tap: logs it, which redraws every widget, and the app shows it too. */
class ResistUrgeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[UrgeGoal] ?: return
        val app = context.applicationContext as LedgerApplication
        withContext(Dispatchers.Main) { app.store.resistUrge(id, LocalDate.now()) }
    }
}

/**
 * The last seven days as weekday letters, oldest first: a kept day in ink, a
 * day with a slip struck through, a day before tracking (or a rest day) faint.
 * (Dots read as page markers, as if there were more widgets to swipe through.)
 */
@Composable
internal fun WeekLetters(today: LocalDate, c: Palette, stateOf: (LocalDate) -> DayState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        (6 downTo 0).forEach { back ->
            val day = today.minusDays(back.toLong())
            val state = stateOf(day)
            Text(
                day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                style = TextStyle(
                    color = if (state == DayState.Kept || state == DayState.Slip) c.ink else c.muted,
                    fontSize = 11.sp,
                    fontWeight = if (back == 0) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (state == DayState.Slip) TextDecoration.LineThrough else null,
                ),
                modifier = GlanceModifier.padding(end = 5.dp),
            )
        }
    }
}

/** One cell: the icon over the count. */
@Composable
private fun TinyGoal(goal: Goal, icons: Map<String, Bitmap>, days: Long, c: Palette) {
    Column(
        GlanceModifier.fillMaxSize().clickable(openGoal(goal)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(goal, icons, c, 18)
        Spacer(GlanceModifier.height(2.dp))
        Count(goal, days, 26f, c)
    }
}

/** One row high: icon, name (and next milestone when there's room), then the count. */
@Composable
private fun StripGoal(goal: Goal, icons: Map<String, Bitmap>, days: Long, c: Palette, wide: Boolean) {
    Row(GlanceModifier.fillMaxSize().clickable(openGoal(goal)), verticalAlignment = Alignment.CenterVertically) {
        if (icons[goal.icon] != null) {
            Icon(goal, icons, c, 22)
            Spacer(GlanceModifier.width(10.dp))
        }
        Column(GlanceModifier.defaultWeight()) {
            Text(goal.title, style = TextStyle(color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            if (wide) Text(nextLine(days, goal.currentStart), style = TextStyle(color = c.muted, fontSize = 11.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.width(8.dp))
        Count(goal, days, 30f, c)
    }
}

/** One line of the list: icon, title, next milestone, the count, and a progress line. */
@Composable
private fun GoalRow(goal: Goal, icons: Map<String, Bitmap>, today: LocalDate, c: Palette) {
    val days = goal.daysKept(today)
    Column(GlanceModifier.fillMaxWidth().clickable(openGoal(goal))) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (icons[goal.icon] != null) {
                Box(GlanceModifier.size(28.dp), contentAlignment = Alignment.CenterStart) { Icon(goal, icons, c, 18) }
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(goal.title, style = TextStyle(color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                val milestone = Milestones.isMilestone(days, goal.currentStart)
                Text(statusLine(goal, days), style = TextStyle(color = if (milestone) c.accent else c.muted, fontSize = 11.sp), maxLines = 1)
            }
            Spacer(GlanceModifier.width(10.dp))
            SerifText("$days", 30f, c.ink)
        }
        Spacer(GlanceModifier.height(6.dp))
        Progress(days, goal.currentStart, c)
    }
}

@Composable
internal fun Progress(days: Long, start: LocalDate, c: Palette) {
    LinearProgressIndicator(
        progress = Milestones.progress(days, start),
        modifier = GlanceModifier.fillMaxWidth().height(3.dp).cornerRadius(2.dp),
        color = c.ink,
        backgroundColor = c.track,
    )
}

internal fun openGoal(goal: Goal) = actionStartActivity<MainActivity>(actionParametersOf(OpenGoal to goal.id))

class LedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LedgerWidget()
}
