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

// Ink on warm paper, as on the website: a soft, bookish off-white by day and a
// deep near-black by night, with one quiet accent, a muted gold, kept for
// milestones and the Aristotle marks.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F1D1A),
    onPrimary = Color(0xFFF3F0EA),
    primaryContainer = Color(0xFFEAE5DC),
    onPrimaryContainer = Color(0xFF1F1D1A),
    secondary = Color(0xFF7A756C),
    secondaryContainer = Color(0xFFEAE5DC),
    onSecondaryContainer = Color(0xFF1F1D1A),
    tertiary = Color(0xFFA4814F),
    onTertiary = Color(0xFFFCFBF8),
    background = Color(0xFFF3F0EA),
    onBackground = Color(0xFF1F1D1A),
    surface = Color(0xFFF3F0EA),
    onSurface = Color(0xFF1F1D1A),
    surfaceVariant = Color(0xFFEAE5DC),
    onSurfaceVariant = Color(0xFF7A756C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFBF8),
    surfaceContainer = Color(0xFFF3F0EA),
    surfaceContainerHigh = Color(0xFFEFEBE4),
    surfaceContainerHighest = Color(0xFFEAE5DC),
    outline = Color(0xFFC8BEB0),
    outlineVariant = Color(0xFFE3DDD3),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEFEBE4),
    onPrimary = Color(0xFF121214),
    primaryContainer = Color(0xFF26252A),
    onPrimaryContainer = Color(0xFFEFEBE4),
    secondary = Color(0xFF9C968C),
    secondaryContainer = Color(0xFF26252A),
    onSecondaryContainer = Color(0xFFEFEBE4),
    tertiary = Color(0xFFD0AE74),
    onTertiary = Color(0xFF121214),
    background = Color(0xFF121214),
    onBackground = Color(0xFFEFEBE4),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFEFEBE4),
    surfaceVariant = Color(0xFF26252A),
    onSurfaceVariant = Color(0xFF9C968C),
    surfaceContainerLowest = Color(0xFF0C0C0E),
    surfaceContainerLow = Color(0xFF1C1B1E),
    surfaceContainer = Color(0xFF18181B),
    surfaceContainerHigh = Color(0xFF212024),
    surfaceContainerHighest = Color(0xFF26252A),
    outline = Color(0xFF57524B),
    outlineVariant = Color(0xFF2A2826),
    error = Color(0xFFF2B8B5),
)

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
fun LedgerTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = LedgerTypography,
        content = content,
    )
}
