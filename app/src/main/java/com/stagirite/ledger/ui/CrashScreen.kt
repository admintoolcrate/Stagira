package com.stagirite.ledger.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shown instead of the app when the last launch crashed before drawing anything. Plain on
 * purpose: the system font and default colours, none of the app's own theme or data, so it
 * opens even when those are what's broken. It shows the error, which can be copied or sent.
 */
@Composable
fun CrashScreen(trace: String, canRetry: Boolean, onRetry: () -> Unit) {
    val context = LocalContext.current
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.safeDrawingPadding().padding(20.dp)) {
                Text("Stagira couldn’t open", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Something went wrong as it started. Your goals are safe on this phone. Please send the " +
                        "details below to the developer, so it can be fixed in the next update.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { send(context, trace) }) { Text("Send by email") }
                    OutlinedButton(onClick = { copy(context, trace) }) { Text("Copy") }
                }
                if (canRetry) {
                    OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("Try opening again") }
                }
                Spacer(Modifier.height(16.dp))
                SelectionContainer(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFF2F2F2))
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    Text(trace, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

private fun send(context: Context, trace: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf("admin.toolcrate@gmail.com"))
        .putExtra(Intent.EXTRA_SUBJECT, "Stagira couldn't open")
        .putExtra(Intent.EXTRA_TEXT, trace)
    runCatching { context.startActivity(intent) }.onFailure { copy(context, trace) }
}

private fun copy(context: Context, trace: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Stagira error", trace))
}
