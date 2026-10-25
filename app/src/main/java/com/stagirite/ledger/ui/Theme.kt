package com.stagirite.ledger.ui

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

private val LedgerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Inter, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2.5).sp),
        displayMedium = displayMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp),
        displaySmall = displaySmall.copy(fontFamily = Inter, fontWeight = FontWeight.Light, letterSpacing = (-0.8).sp),
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
