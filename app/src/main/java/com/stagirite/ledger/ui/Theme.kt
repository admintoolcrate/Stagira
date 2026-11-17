package com.stagirite.ledger.ui

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.stagirite.ledger.R
import androidx.compose.ui.unit.sp

/** The handful of colours a palette is built from. */
class Tokens(
    val page: Long, val card: Long, val tint: Long, val ink: Long, val muted: Long,
    val accent: Long, val line: Long, val hairline: Long, val raised: Long, val high: Long, val lowest: Long,
)

/**
 * A palette: ink on a page, white-ish cards, one soft tint for what's selected or
 * kept, and one accent, a muted gold, kept for milestones and the Aristotle marks.
 */
enum class Tone(val label: String, val light: Tokens, val dark: Tokens) {
    /** Warm paper, as on the website: soft and bookish. */
    Paper(
        "Paper",
        Tokens(0xFFF3F0EA, 0xFFFCFBF8, 0xFFE6DDCF, 0xFF1F1D1A, 0xFF7A756C, 0xFFA4814F, 0xFFC8BEB0, 0xFFE3DDD3, 0xFFEAE5DC, 0xFFEFEBE4, 0xFFFFFFFF),
        Tokens(0xFF121214, 0xFF1C1B1E, 0xFF34312D, 0xFFEFEBE4, 0xFF9C968C, 0xFFD0AE74, 0xFF57524B, 0xFF2A2826, 0xFF26252A, 0xFF212024, 0xFF0C0C0E),
    ),
    /** Brighter linen: nearly white, still warm. */
    Linen(
        "Linen",
        Tokens(0xFFFAF8F4, 0xFFFFFFFF, 0xFFF0EAE0, 0xFF1C1B19, 0xFF77726A, 0xFFAD8750, 0xFFD3CBBF, 0xFFECE7DF, 0xFFF2EEE7, 0xFFF5F2EC, 0xFFFFFFFF),
        Tokens(0xFF101012, 0xFF1A1A1D, 0xFF2E2B27, 0xFFF2EFEA, 0xFF9D978E, 0xFFD6B47A, 0xFF4F4A44, 0xFF26252A, 0xFF242327, 0xFF1F1E22, 0xFF0A0A0C),
    ),
    /** Fresh sage: a breath of green in the tint, crisp white cards. */
    Sage(
        "Sage",
        Tokens(0xFFF6F8F5, 0xFFFFFFFF, 0xFFE1EBE2, 0xFF1A1D1A, 0xFF6C756D, 0xFFAE8650, 0xFFC5D0C6, 0xFFE4EAE4, 0xFFEDF2ED, 0xFFF1F5F1, 0xFFFFFFFF),
        Tokens(0xFF0F1210, 0xFF181C19, 0xFF243028, 0xFFE8EEE9, 0xFF919B93, 0xFFD3B074, 0xFF46514A, 0xFF222925, 0xFF212823, 0xFF1C221E, 0xFF0A0C0B),
    ),
    /** Cool mist: clean blue-grey, the most modern of the four. */
    Mist(
        "Mist",
        Tokens(0xFFF6F7F9, 0xFFFFFFFF, 0xFFE2E8F0, 0xFF1A1C20, 0xFF6A7180, 0xFFB0874C, 0xFFC7CED9, 0xFFE5E9EF, 0xFFEDF0F5, 0xFFF1F3F7, 0xFFFFFFFF),
        Tokens(0xFF0F1115, 0xFF181B21, 0xFF252C38, 0xFFE9ECF1, 0xFF959CAA, 0xFFD4B077, 0xFF474F5E, 0xFF222731, 0xFF20252E, 0xFF1B1F27, 0xFF0A0B0E),
    ),
}

/** The palette the app uses. */
val AppTone = Tone.Paper

internal fun colorsFor(tone: Tone, dark: Boolean) = (if (dark) tone.dark else tone.light).let { t ->
    val c = { v: Long -> Color(v) }
    val base = if (dark) darkColorScheme() else lightColorScheme()
    base.copy(
        primary = c(t.ink), onPrimary = c(t.page),
        primaryContainer = c(t.raised), onPrimaryContainer = c(t.ink),
        secondary = c(t.muted),
        // The soft tint for selected and kept, a quieter stand-in for solid ink.
        secondaryContainer = c(t.tint), onSecondaryContainer = c(t.ink),
        tertiary = c(t.accent), onTertiary = c(t.card),
        background = c(t.page), onBackground = c(t.ink),
        surface = c(t.page), onSurface = c(t.ink),
        surfaceVariant = c(t.raised), onSurfaceVariant = c(t.muted),
        surfaceContainerLowest = c(t.lowest), surfaceContainerLow = c(t.card),
        surfaceContainer = c(t.page), surfaceContainerHigh = c(t.high), surfaceContainerHighest = c(t.raised),
        outline = c(t.line), outlineVariant = c(t.hairline),
        error = if (dark) Color(0xFFF2B8B5) else Color(0xFFB3261E),
    )
}

/** The website's "dusk" panel: a deep blue-violet into warm brown, for the year card. */
val DuskGradient = listOf(Color(0xFF2A2B38), Color(0xFF34303A), Color(0xFF43362F))
val OnDusk = Color(0xFFF2EDE4)
val DuskGold = Color(0xFFD0AE74)

/** Inter at one weight, picked from its variable weight axis. */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Inter (SIL Open Font License) for everything but the emoji icons. */
val Inter = FontFamily(
    inter(FontWeight.Thin),
    inter(FontWeight.ExtraLight),
    inter(FontWeight.Light),
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
)

/**
 * EB Garamond (SIL Open Font License): the classical voice, kept to three jobs:
 * the name, the day counts and Aristotle's words. Inter does everything else.
 */
val Garamond = FontFamily(
    Font(R.font.eb_garamond_regular, FontWeight.Normal),
    Font(R.font.eb_garamond_medium, FontWeight.Medium),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

/**
 * The type scale. Interface text uses Material's roles below; the serif sizes
 * live here so every screen draws from the same few.
 */
object LedgerType {
    /** Counts line up in columns and don't jiggle as they roll. */
    private const val FIGURES = "tnum"

    /** The goal page's count. */
    val hero = TextStyle(fontFamily = Garamond, fontSize = 96.sp, lineHeight = 96.sp, fontFeatureSettings = FIGURES)
    /** A count in a list row. */
    val count = TextStyle(fontFamily = Garamond, fontSize = 40.sp, lineHeight = 40.sp, fontFeatureSettings = FIGURES)
    /** The wordmark. */
    val name = TextStyle(fontFamily = Garamond, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = 0.sp)
    /** A passage from Aristotle. */
    val passage = TextStyle(fontFamily = Garamond, fontSize = 20.sp, lineHeight = 28.sp)
    /** A screen's title in the top bar, as the website sets its headings. */
    val title = TextStyle(fontFamily = Garamond, fontSize = 30.sp, lineHeight = 34.sp)
    /** A headline figure in a summary: days kept, a week's tally. */
    val figure = TextStyle(fontFamily = Garamond, fontSize = 32.sp, lineHeight = 34.sp, fontFeatureSettings = FIGURES)
    /** Quotation marks around a passage. */
    val mark = TextStyle(fontFamily = Garamond, fontSize = 44.sp, lineHeight = 44.sp)
}

/**
 * Scales big display type with the system text size only up to [max]×, so a
 * 96sp count doesn't swamp the screen at the largest settings. Body text is
 * left to scale fully.
 */
@Composable
fun TextStyle.capped(max: Float = 1.25f): TextStyle {
    val scale = LocalDensity.current.fontScale
    if (scale <= max) return this
    val k = max / scale
    return copy(
        fontSize = fontSize * k,
        lineHeight = if (lineHeight.isSp) lineHeight * k else lineHeight,
    )
}

/**
 * On tablets, foldables and phones turned sideways, screens keep a readable
 * width in the middle instead of stretching edge to edge.
 */
val MAX_CONTENT_WIDTH = 640.dp

/** Centres a screen and caps its width at [MAX_CONTENT_WIDTH], over the page colour. */
@Composable
fun Modifier.readableWidth(): Modifier = this
    .fillMaxSize()
    .background(MaterialTheme.colorScheme.background)
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = MAX_CONTENT_WIDTH)

/** Spacing in steps of 4dp; layouts use these rather than one-off values. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

private val LedgerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Garamond, fontWeight = FontWeight.Normal, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
        displayMedium = displayMedium.copy(fontFamily = Garamond, fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp, fontFeatureSettings = "tnum"),
        displaySmall = displaySmall.copy(fontFamily = Garamond, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, fontFeatureSettings = "tnum"),
        headlineLarge = headlineLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Light, letterSpacing = (-0.6).sp),
        headlineMedium = headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Light, letterSpacing = (-0.4).sp),
        headlineSmall = headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Normal, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        titleMedium = titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
        titleSmall = titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Inter, letterSpacing = 0.sp),
        bodyMedium = bodyMedium.copy(fontFamily = Inter, letterSpacing = 0.sp),
        bodySmall = bodySmall.copy(fontFamily = Inter, letterSpacing = 0.1.sp),
        labelLarge = labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
        labelMedium = labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
        labelSmall = labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    )
}

@Composable
fun LedgerTheme(dark: Boolean, tone: Tone = AppTone, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorsFor(tone, dark),
        typography = LedgerTypography,
        content = content,
    )
}
