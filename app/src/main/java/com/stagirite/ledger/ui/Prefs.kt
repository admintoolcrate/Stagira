package com.stagirite.ledger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** The Personalise choices, available anywhere in the UI. */
data class UiPrefs(
    val quotes: Boolean = true,
    val celebrations: Boolean = true,
    val haptics: Boolean = true,
    val homeSummary: Boolean = true,
    val homeExtras: Boolean = true,
)

val LocalUiPrefs = staticCompositionLocalOf { UiPrefs() }

/** A short buzz, if the user wants them. */
@Composable
fun rememberBuzz(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val on = LocalUiPrefs.current.haptics
    return { if (on) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
}
