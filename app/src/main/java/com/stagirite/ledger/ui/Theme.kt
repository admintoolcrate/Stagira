package com.stagirite.ledger.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val Parchment = Color(0xFFF4ECD8)
private val Vellum = Color(0xFFFBF6EA)
private val Ink = Color(0xFF1C1A17)
private val Bronze = Color(0xFF8C5A2B)
private val Gilt = Color(0xFFD9A56A)

private val LightColors = lightColorScheme(
    primary = Bronze,
    onPrimary = Vellum,
    primaryContainer = Color(0xFFEBD9BC),
    onPrimaryContainer = Color(0xFF3B240E),
    secondary = Color(0xFF6B5D4F),
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceContainerLow = Vellum,
    surfaceContainer = Vellum,
    surfaceContainerHigh = Color(0xFFF0E5CD),
    surfaceVariant = Color(0xFFE6D9BE),
    onSurfaceVariant = Color(0xFF5A4E42),
    outline = Color(0xFFA8977F),
)

private val DarkColors = darkColorScheme(
    primary = Gilt,
    onPrimary = Color(0xFF3B240E),
    primaryContainer = Color(0xFF5A3A1A),
    onPrimaryContainer = Color(0xFFF6DDBB),
    secondary = Color(0xFFCDBFAF),
    background = Ink,
    onBackground = Color(0xFFEDE4D3),
    surface = Ink,
    onSurface = Color(0xFFEDE4D3),
    surfaceContainerLow = Color(0xFF24211D),
    surfaceContainer = Color(0xFF24211D),
    surfaceContainerHigh = Color(0xFF2E2A25),
    surfaceVariant = Color(0xFF3A342D),
    onSurfaceVariant = Color(0xFFCDBFAF),
    outline = Color(0xFF8A7D6E),
)

private val Serif = FontFamily.Serif

private val LedgerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Serif),
        displayMedium = displayMedium.copy(fontFamily = Serif),
        displaySmall = displaySmall.copy(fontFamily = Serif),
        headlineLarge = headlineLarge.copy(fontFamily = Serif),
        headlineMedium = headlineMedium.copy(fontFamily = Serif),
        headlineSmall = headlineSmall.copy(fontFamily = Serif),
        titleLarge = titleLarge.copy(fontFamily = Serif),
        titleMedium = titleMedium.copy(fontFamily = Serif),
        bodyLarge = bodyLarge.copy(fontFamily = Serif),
    )
}

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = LedgerTypography,
        content = content,
    )
}
