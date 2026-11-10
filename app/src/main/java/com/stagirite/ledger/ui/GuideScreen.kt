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
import com.stagirite.ledger.data.GoalTemplate
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.goalTemplates
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import java.time.LocalDate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
        "Tackle a vice: stop or cut down on something, like drinking less. Practise a virtue: do something " +
            "good each day, like one good deed. Either way, good days count on their own: there\u2019s nothing " +
            "to log unless something goes wrong.",
    ),
    GuidePage(
        "🌱",
        "When you slip",
        "It happens to everyone, and it doesn\u2019t undo the days you kept. Log it in a couple of taps " +
            "and begin again. The run you had stays in your history, with a note on what happened if you " +
            "want one.",
    ),
    GuidePage(
        "🔑",
        "Private by default",
        "Your ledger is kept on this phone. Nothing leaves it unless you turn on sync, which saves a copy " +
            "to your own Google Drive, encrypted if you add a passphrase. You can also lock the app with " +
            "your fingerprint and give sensitive goals a cover name behind a PIN.",
    ),
)

/**
 * The introductory guide. With [onCreate], it ends on a page that asks what
 * you're starting, so the first visit lands on a goal already counting.
 */
@Composable
fun GuideScreen(onDone: () -> Unit, onCreate: ((GoalDraft) -> Unit)? = null) {
    val count = pages.size + if (onCreate != null) 1 else 0
    val pager = rememberPagerState { count }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == count - 1
    var title by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(GoalKind.Vice) }
    var sinceEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val finish = {
        val name = title.trim()
        if (onCreate != null && name.isNotEmpty()) {
            onCreate(GoalDraft(name, LocalDate.ofEpochDay(sinceEpochDay), "", icon, kind, false, null))
        }
        onDone()
    }

    Surface(Modifier.readableWidth(), color = MaterialTheme.colorScheme.background) {
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
                if (i == pages.size) {
                    FirstGoalPage(
                        title, icon, kind, LocalDate.ofEpochDay(sinceEpochDay),
                        onTitle = { title = it },
                        onIcon = { icon = it },
                        onKind = { kind = it; title = ""; icon = "" },
                        onTemplate = { title = it.title; icon = it.icon },
                        onSince = { sinceEpochDay = it.toEpochDay() },
                    )
                    return@HorizontalPager
                }
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
                    Text(page.title, style = LedgerType.name)
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
                onClick = { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    when {
                        !last -> "Next"
                        onCreate != null && title.isNotBlank() -> "Start counting"
                        onCreate != null -> "Skip for now"
                        else -> "Begin"
                    },
                )
            }
        }
    }
}

/**
 * The last page of a first visit: the same choices as the full goal editor (the
 * kind, an icon, the name and the start date), with common goals to pick from.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FirstGoalPage(
    title: String,
    icon: String,
    kind: GoalKind,
    since: LocalDate,
    onTitle: (String) -> Unit,
    onIcon: (String) -> Unit,
    onKind: (GoalKind) -> Unit,
    onTemplate: (GoalTemplate) -> Unit,
    onSince: (LocalDate) -> Unit,
) {
    var pickingIcon by rememberSaveable { mutableStateOf(false) }
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(Space.l))
        Text("What are you starting?", style = LedgerType.name.copy(fontSize = 32.sp, lineHeight = 36.sp))
        Spacer(Modifier.height(Space.s))
        Text(
            "Pick the kind of goal, give it a name, and the count begins.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.l))
        GoalKind.entries.forEach { k ->
            KindChoice(k, selected = kind == k) { onKind(k) }
            Spacer(Modifier.height(Space.s))
        }
        Spacer(Modifier.height(Space.s))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            goalTemplates.filter { it.kind == kind }.forEach { t ->
                FilterChip(
                    selected = title == t.title,
                    onClick = { onTemplate(t) },
                    label = { Text(t.title) },
                    leadingIcon = { EmojiGlyph(t.icon, 14.sp) },
                )
            }
        }
        Spacer(Modifier.height(Space.l))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                    .clickable { pickingIcon = true },
                contentAlignment = Alignment.Center,
            ) {
                if (icon.isEmpty()) {
                    Icon(Icons.Filled.Add, contentDescription = "Choose an icon", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    EmojiGlyph(icon, 26.sp)
                }
            }
            Spacer(Modifier.width(Space.m))
            OutlinedTextField(
                value = title,
                onValueChange = onTitle,
                label = { Text("Goal") },
                placeholder = { Text(if (kind == GoalKind.Virtue) "e.g. One good deed a day" else "e.g. Drink less") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Space.s))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Started", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { pickingDay = true }) {
                Text(if (since == today) "Today" else since.format(mediumDate))
            }
        }
        Spacer(Modifier.height(Space.l))
    }

    if (pickingIcon) {
        IconPickerDialog(current = icon, onDismiss = { pickingIcon = false }, onPick = { onIcon(it); pickingIcon = false })
    }
    if (pickingDay) {
        DayPickerDialog(
            initial = since,
            earliest = null,
            latest = today,
            onDismiss = { pickingDay = false },
            onPick = { onSince(it); pickingDay = false },
        )
    }
}
