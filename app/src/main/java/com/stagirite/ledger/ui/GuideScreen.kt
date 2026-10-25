package com.stagirite.ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagirite.ledger.data.Quotes
import kotlinx.coroutines.launch

private class GuidePage(val icon: String, val title: String, val body: String)

private val pages = listOf(
    GuidePage(
        "🏛",
        "Stagira",
        // Not "we are what we repeatedly do": that line is Will Durant's summary, often misattributed.
        "Named after Aristotle’s birthplace. He taught that character isn’t something you’re " +
            "born with but something you build, one habit at a time. Stagira keeps count of the days you put in.",
    ),
    GuidePage(
        "⚖",
        "Vices and Virtues",
        "A Vice is something you want to do less of, like drinking or late-night scrolling. A Virtue " +
            "is something you want to do more of, like reading or running. Mark each goal as one or the " +
            "other, or leave it unmarked. If you started a while ago, set the day you began.",
    ),
    GuidePage(
        "🌱",
        "When you slip",
        "It happens to everyone, and it doesn\u2019t undo the days you kept. Log it in a couple of taps " +
            "and the count starts again. Your earlier runs stay in your history, and you can jot down " +
            "what happened, for yourself.",
    ),
    GuidePage(
        "🔑",
        "Private by default",
        "Your ledger is kept on this phone. Nothing leaves it unless you turn on sync, which saves a copy " +
            "to your own Google Drive, encrypted if you add a passphrase. You can also lock the app with " +
            "your fingerprint and give sensitive goals a cover name behind a PIN.",
    ),
)

@Composable
fun GuideScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End) {
                if (!last) TextButton(onClick = onDone) { Text("Skip") }
            }
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                val page = pages[i]
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { EmojiGlyph(page.icon, 32.sp) }
                    Spacer(Modifier.height(28.dp))
                    Text(page.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    if (i == 0 && LocalUiPrefs.current.quotes) {
                        Spacer(Modifier.height(36.dp))
                        LedgerCard { QuoteBlock(Quotes.slip[2]) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == pager.currentPage) 8.dp else 6.dp)
                            .background(
                                if (i == pager.currentPage) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            ),
                    )
                }
            }
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (last) "Begin" else "Next") }
        }
    }
}
