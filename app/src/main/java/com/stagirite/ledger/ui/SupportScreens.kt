package com.stagirite.ledger.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stagirite.ledger.BuildConfig
import com.stagirite.ledger.security.AppLock

/** The developer's Ko-fi page. Empty until there is one; the button then stays hidden. */
const val KOFI_URL = ""

/**
 * Settings → Support Stagira. The app is free, with no ads and nothing to buy.
 * Copies installed from GitHub may link to Ko-fi; the Google Play copy doesn't,
 * since Play requires support for a developer inside an app to go through Play
 * billing. The website carries the link for everyone.
 */
@Composable
fun SupportNote(lock: AppLock) {
    val context = LocalContext.current
    Text(
        "Stagira is free, with no ads and nothing to buy. If it helps you, the kindest support is telling " +
            "someone else about it, or sending feedback below.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (BuildConfig.BETA_UPDATES && KOFI_URL.isNotEmpty()) {
        Spacer(Modifier.height(Space.m))
        OutlinedButton(onClick = {
            lock.expectReturn()
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(KOFI_URL))) }
        }) { Text("Buy me a coffee on Ko-fi") }
    }
}
