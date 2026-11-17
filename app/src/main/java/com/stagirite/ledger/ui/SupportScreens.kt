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
import com.stagirite.ledger.security.AppLock

/** The website's start page: from there, people can find the ways to support Stagira. */
private const val SITE_URL = "https://stagira.toolcrate.cc"

/**
 * Settings → Support Stagira. The app is free, with no ads and nothing to buy. The link goes
 * to the website's start page rather than straight to a donation page: Google Play doesn't
 * allow apps to point people to outside payments.
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
    Spacer(Modifier.height(Space.m))
    OutlinedButton(onClick = {
        lock.expectReturn()
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SITE_URL))) }
    }) { Text("Find out how you can support Stagira") }
}
