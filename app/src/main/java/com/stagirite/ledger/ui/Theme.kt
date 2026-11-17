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
 * A palette: a page, cards, one soft tint for what's selected or kept, its own ink,
 * and one accent, a muted gold, kept for milestones and the Aristotle marks.
 * People choose one under Settings → Theme.
 */
enum class Tone(val label: String, val blurb: String, val light: Tokens, val dark: Tokens) {
    Paper(
        "Paper", "Warm cream and brown-black ink: the original, bookish look.",
        Tokens(0xFFF3EEE4, 0xFFFCFAF5, 0xFFE4D6BF, 0xFF2A2217, 0xFF7D7062, 0xFFA4814F, 0xFFC9B9A0, 0xFFE4DACB, 0xFFECE4D6, 0xFFEFE9DE, 0xFFFFFFFF),
        Tokens(0xFF15120E, 0xFF1F1B16, 0xFF3A3227, 0xFFF0E8DA, 0xFFA39887, 0xFFD6B47A, 0xFF5A4F42, 0xFF2C2620, 0xFF2A241D, 0xFF241F19, 0xFF0C0A08),
    ),
    Snow(
        "Snow", "Pure white and true black: the plainest of all.",
        Tokens(0xFFFFFFFF, 0xFFF6F6F5, 0xFFECECEA, 0xFF111111, 0xFF6E6E6E, 0xFFA8834E, 0xFFCFCFCC, 0xFFEAEAE8, 0xFFF0F0EE, 0xFFF3F3F2, 0xFFFFFFFF),
        Tokens(0xFF000000, 0xFF121212, 0xFF262626, 0xFFFFFFFF, 0xFF9A9A9A, 0xFFD6B47A, 0xFF4A4A4A, 0xFF1E1E1E, 0xFF1C1C1C, 0xFF171717, 0xFF000000),
    ),
    Stone(
        "Stone", "Soft greys and graphite, quiet and neutral.",
        Tokens(0xFFE9E9E6, 0xFFF5F5F3, 0xFFD6D6D2, 0xFF1C1C1E, 0xFF66666A, 0xFFA8834E, 0xFFBDBDB8, 0xFFDADAD6, 0xFFE0E0DC, 0xFFE4E4E1, 0xFFFFFFFF),
        Tokens(0xFF18181A, 0xFF222225, 0xFF35353A, 0xFFECECEE, 0xFF9A9AA0, 0xFFD2AF75, 0xFF4E4E55, 0xFF2A2A2E, 0xFF2A2A2F, 0xFF25252A, 0xFF0E0E10),
    ),
    Sage(
        "Sage", "Pale green with deep forest ink: fresh and calm.",
        Tokens(0xFFEDF2EC, 0xFFFAFCF9, 0xFFCCDDCD, 0xFF1F3326, 0xFF5D7363, 0xFFA88550, 0xFFB3C7B6, 0xFFD9E3D9, 0xFFE3EBE3, 0xFFE8EFE8, 0xFFFFFFFF),
        Tokens(0xFF0F1A14, 0xFF17241C, 0xFF254232, 0xFFE3EFE6, 0xFF8FA898, 0xFFD3B074, 0xFF3D5747, 0xFF1E2E24, 0xFF1F2F25, 0xFF1A2820, 0xFF0A120D),
    ),
    Ocean(
        "Ocean", "Sea-glass blue with navy ink: clear and modern.",
        Tokens(0xFFECF2F8, 0xFFFAFCFE, 0xFFC9DBEE, 0xFF17304D, 0xFF586E88, 0xFFAA8550, 0xFFB0C6DE, 0xFFD7E3EF, 0xFFE1EBF5, 0xFFE7EFF7, 0xFFFFFFFF),
        Tokens(0xFF0C1622, 0xFF13202F, 0xFF1F3650, 0xFFE2ECF7, 0xFF8DA2BA, 0xFFD3B074, 0xFF3B536F, 0xFF1B2A3B, 0xFF1C2B3D, 0xFF182536, 0xFF070E16),
    ),
    Rose(
        "Rose", "Blush with deep wine ink: warm and gentle.",
        Tokens(0xFFF8EDEB, 0xFFFFFAF9, 0xFFEDCFCA, 0xFF3B1F24, 0xFF86656A, 0xFFA98149, 0xFFD8BCB7, 0xFFEEDDDA, 0xFFF2E4E1, 0xFFF5E9E7, 0xFFFFFFFF),
        Tokens(0xFF1A1012, 0xFF25171A, 0xFF46262C, 0xFFF5E6E6, 0xFFAE9295, 0xFFD6B278, 0xFF5E4045, 0xFF32222A, 0xFF2F1F23, 0xFF2A1B1F, 0xFF100A0B),
    ),
    Lavender(
        "Lavender", "Soft lilac with indigo ink: dreamy but composed.",
        Tokens(0xFFF1EFF8, 0xFFFCFBFF, 0xFFD8D1EF, 0xFF29234A, 0xFF6C6690, 0xFFAC8550, 0xFFC2BBDE, 0xFFE2DEF0, 0xFFE9E6F4, 0xFFEDEBF6, 0xFFFFFFFF),
        Tokens(0xFF13111F, 0xFF1C192C, 0xFF332C55, 0xFFECE8FA, 0xFF9F98C0, 0xFFD4B077, 0xFF4A4370, 0xFF262140, 0xFF25203B, 0xFF201C33, 0xFF0B0A13),
    ),
    Dusk(
        "Dusk", "The website's evening: blue-violet into warm brown, by day and night.",
        Tokens(0xFF2A2B38, 0xFF34303A, 0xFF4A4250, 0xFFF2EDE4, 0xFFB9B0A6, 0xFFD0AE74, 0xFF5E5560, 0xFF3F3A45, 0xFF3A3540, 0xFF37323D, 0xFF1F1F29),
        Tokens(0xFF1C1C26, 0xFF26242E, 0xFF3D3644, 0xFFF2EDE4, 0xFFADA59C, 0xFFD6B47A, 0xFF524A55, 0xFF302C38, 0xFF2E2A35, 0xFF2A2631, 0xFF13131A),
    ),
    Black(
        "Black", "True black with white ink: dark always, and kindest to OLED screens at night.",
        Tokens(0xFF000000, 0xFF0E0E0E, 0xFF1F1F1F, 0xFFFFFFFF, 0xFF8E8E8E, 0xFFD6B47A, 0xFF3A3A3A, 0xFF1A1A1A, 0xFF161616, 0xFF121212, 0xFF000000),
        Tokens(0xFF000000, 0xFF0E0E0E, 0xFF1F1F1F, 0xFFFFFFFF, 0xFF8E8E8E, 0xFFD6B47A, 0xFF3A3A3A, 0xFF1A1A1A, 0xFF161616, 0xFF121212, 0xFF000000),
    ),
    ;

    /** Dusk and Black are night palettes: they stay dark whatever the light/dark setting. */
    val alwaysDark: Boolean get() = this == Dusk || this == Black

    companion object {
        /** New users start on Snow: plain white, the cleanest first look. */
        val Default = Snow

        fun of(name: String?): Tone = entries.firstOrNull { it.name == name } ?: Default
    }
}

/** The palette the app uses. */
val AppTone = Tone.Default

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
        colorScheme = animated(colorsFor(tone, dark || tone.alwaysDark)),
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
