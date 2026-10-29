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

// Monochrome: ink on paper, paper on ink. No accent colour.
private val LightColors = lightColorScheme(
    primary = Color(0xFF141414),
    onPrimary = Color(0xFFFAFAF8),
    primaryContainer = Color(0xFFEDEDEA),
    onPrimaryContainer = Color(0xFF141414),
    secondary = Color(0xFF6B6B68),
    secondaryContainer = Color(0xFFEDEDEA),
    onSecondaryContainer = Color(0xFF141414),
    background = Color(0xFFF4F4F1),
    onBackground = Color(0xFF141414),
    surface = Color(0xFFF4F4F1),
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFEDEDEA),
    onSurfaceVariant = Color(0xFF6B6B68),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF4F4F1),
    surfaceContainerHigh = Color(0xFFF0F0ED),
    surfaceContainerHighest = Color(0xFFEDEDEA),
    outline = Color(0xFFBDBDB8),
    outlineVariant = Color(0xFFE6E6E2),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2F2F0),
    onPrimary = Color(0xFF0F0F0F),
    primaryContainer = Color(0xFF242424),
    onPrimaryContainer = Color(0xFFF2F2F0),
    secondary = Color(0xFF9A9A97),
    secondaryContainer = Color(0xFF242424),
    onSecondaryContainer = Color(0xFFF2F2F0),
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFF2F2F0),
    surface = Color(0xFF0F0F0F),
    onSurface = Color(0xFFF2F2F0),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFF9A9A97),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF191919),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF1F1F1F),
    surfaceContainerHighest = Color(0xFF242424),
    outline = Color(0xFF4A4A48),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFF2B8B5),
)

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
