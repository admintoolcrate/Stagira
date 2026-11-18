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

/** The handful of colours the app is drawn with, in one mode. */
data class Tokens(
    val page: Long, val card: Long, val tint: Long, val ink: Long, val muted: Long,
    val accent: Long, val line: Long, val hairline: Long, val raised: Long, val high: Long, val lowest: Long,
    /** The accent colour itself: progress lines, tabs, the chosen things. */
    val brand: Long = ink,
)

/**
 * The accent: the one colour on a white or black page. It tints what's chosen or kept,
 * and draws the progress lines and tabs; the type stays black or white, and milestones
 * keep their muted gold. Chosen under Settings → Appearance, with the mode (Auto, White
 * or Black).
 */
enum class Tone(val label: String, val blurb: String, val light: Tokens, val dark: Tokens) {
    Mono(
        "Mono", "No colour at all: black on white, white on black.",
        white(brand = 0xFF111111, tint = 0xFFECECEA),
        black(brand = 0xFFFFFFFF, tint = 0xFF262626),
    ),
    Gold(
        "Gold", "The muted gold of the milestones, everywhere.",
        white(brand = 0xFF8F6E3C, tint = 0xFFF1E6D3),
        black(brand = 0xFFD6B47A, tint = 0xFF3A3022),
    ),
    Sage(
        "Sage", "A calm forest green.",
        white(brand = 0xFF3F6B4E, tint = 0xFFDCE9DE),
        black(brand = 0xFF9CC5A8, tint = 0xFF22362A),
    ),
    Ocean(
        "Ocean", "A clear sea blue.",
        white(brand = 0xFF2E5C8A, tint = 0xFFDCE8F4),
        black(brand = 0xFF98BCE3, tint = 0xFF1D3248),
    ),
    Rose(
        "Rose", "A warm, deep rose.",
        white(brand = 0xFF9B4A57, tint = 0xFFF4DEE0),
        black(brand = 0xFFE3A5AE, tint = 0xFF3F2328),
    ),
    Lavender(
        "Lavender", "A soft violet.",
        white(brand = 0xFF5E4FA2, tint = 0xFFE5E0F5),
        black(brand = 0xFFB6A9EC, tint = 0xFF2E2848),
    ),
    Clay(
        "Clay", "An earthy terracotta.",
        white(brand = 0xFFA65733, tint = 0xFFF6E1D6),
        black(brand = 0xFFE8A383, tint = 0xFF3E281E),
    ),
    ;

    /** Black mode is the dark one; pages are always true black. */
    val black: Tokens get() = dark

    companion object {
        /** New users start with no colour: black on white, or white on black. */
        val Default = Mono

        /** The accent saved under [name]; older palette names map to the nearest accent. */
        fun of(name: String?): Tone = when (name) {
            "Snow", "Stone", "Black" -> Mono
            "Paper" -> Gold
            "Dusk" -> Lavender
            else -> entries.firstOrNull { it.name == name } ?: Default
        }
    }
}

internal fun colorsFor(tone: Tone, dark: Boolean, black: Boolean = false) = (if (black) tone.black else if (dark) tone.dark else tone.light).let { t ->
    val c = { v: Long -> Color(v) }
    val base = if (dark || black) darkColorScheme() else lightColorScheme()
    base.copy(
        primary = c(t.ink), onPrimary = c(t.page),
        primaryContainer = c(t.raised), onPrimaryContainer = c(t.ink),
        // The accent: progress lines, tabs and the chosen things.
        secondary = c(t.brand), onSecondary = c(t.page),
        // The soft tint for selected and kept, a quieter stand-in for solid ink.
        secondaryContainer = c(t.tint), onSecondaryContainer = c(t.ink),
        tertiary = c(t.accent), onTertiary = c(t.card),
        background = c(t.page), onBackground = c(t.ink),
        surface = c(t.page), onSurface = c(t.ink),
        surfaceVariant = c(t.raised), onSurfaceVariant = c(t.muted),
        surfaceContainerLowest = c(t.lowest), surfaceContainerLow = c(t.card),
        surfaceContainer = c(t.page), surfaceContainerHigh = c(t.high), surfaceContainerHighest = c(t.raised),
        outline = c(t.line), outlineVariant = c(t.hairline),
        error = if (dark || black) Color(0xFFF2B8B5) else Color(0xFFB3261E),
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
fun LedgerTheme(dark: Boolean, tone: Tone = Tone.Default, black: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = animated(colorsFor(tone, dark, black)),
        typography = LedgerTypography,
        content = content,
    )
}

/** Changing palette (or day to night) fades the colours across rather than snapping. */
@Composable
private fun animated(target: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme {
    @Composable
    fun Color.eased(): Color = androidx.compose.animation.animateColorAsState(
        this, androidx.compose.animation.core.tween(450), label = "theme",
    ).value
    return target.copy(
        primary = target.primary.eased(), onPrimary = target.onPrimary.eased(),
        secondary = target.secondary.eased(),
        secondaryContainer = target.secondaryContainer.eased(), onSecondaryContainer = target.onSecondaryContainer.eased(),
        tertiary = target.tertiary.eased(),
        background = target.background.eased(), onBackground = target.onBackground.eased(),
        surface = target.surface.eased(), onSurface = target.onSurface.eased(),
        surfaceVariant = target.surfaceVariant.eased(), onSurfaceVariant = target.onSurfaceVariant.eased(),
        surfaceContainerLow = target.surfaceContainerLow.eased(), surfaceContainer = target.surfaceContainer.eased(),
        surfaceContainerHigh = target.surfaceContainerHigh.eased(), surfaceContainerHighest = target.surfaceContainerHighest.eased(),
        outline = target.outline.eased(), outlineVariant = target.outlineVariant.eased(),
    )
}
