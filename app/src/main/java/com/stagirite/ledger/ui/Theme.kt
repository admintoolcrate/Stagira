package com.stagirite.ledger.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Monochrome: ink on paper, paper on ink. No accent colour.
private val LightColors = lightColorScheme(
    primary = Color(0xFF141414),
    onPrimary = Color(0xFFFAFAF8),
    primaryContainer = Color(0xFFEDEDEA),
    onPrimaryContainer = Color(0xFF141414),
    secondary = Color(0xFF6B6B68),
    secondaryContainer = Color(0xFFEDEDEA),
    onSecondaryContainer = Color(0xFF141414),
    background = Color(0xFFFAFAF8),
    onBackground = Color(0xFF141414),
    surface = Color(0xFFFAFAF8),
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFEDEDEA),
    onSurfaceVariant = Color(0xFF6B6B68),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F2),
    surfaceContainer = Color(0xFFF2F2EF),
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
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF1F1F1F),
    surfaceContainerHighest = Color(0xFF242424),
    outline = Color(0xFF4A4A48),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFF2B8B5),
)

/** Serif is used only for the name; everything else is the system sans. */
val Wordmark = FontFamily.Serif

private val LedgerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Thin),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Light),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Light),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Light),
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
