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
        "The Stagirite’s Ledger",
        // Not "we are what we repeatedly do": that line is Will Durant's summary, often misattributed.
        "A quiet record of the days you keep. Aristotle, the Stagirite, held that character is " +
            "built by habit, through doing. This ledger counts the doing.",
    ),
    GuidePage(
        "⚖",
        "Vices and virtues",
        "Name a goal and, if you like, mark it a vice to do less of or a virtue to do more of. " +
            "Give it a black-and-white icon. Started earlier? Pick the day you began.",
    ),
    GuidePage(
        "🌱",
        "Slips are part of it",
        "If you slip, log it in two taps: today, yesterday or any earlier day. The count starts " +
            "again and the run you had stays in the history. Milestones mark the way.",
    ),
    GuidePage(
        "🔑",
        "Yours alone",
        "Everything stays on this phone. Lock the app with your fingerprint, give sensitive goals a " +
            "cover name behind a PIN, and sync to your own Google account only if you choose.",
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
                            .size(96.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { EmojiGlyph(page.icon, 44.sp) }
                    Spacer(Modifier.height(32.dp))
                    Text(page.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    if (i == 0) {
                        Spacer(Modifier.height(32.dp))
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
