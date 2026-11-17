package com.stagirite.ledger.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The palettes as a row of small previews: each card is the palette by day, a count in its
 * own ink over its page with the tint and the gold, and beneath, a strip of how it looks by
 * night. The row scrolls sideways; the chosen card has an ink edge and a tick.
 */
@Composable
internal fun PalettePicker(current: Tone, onPick: (Tone) -> Unit) {
    val state = rememberLazyListState()
    // Open with the chosen palette in view.
    LaunchedEffect(Unit) { state.scrollToItem((Tone.entries.indexOf(current) - 1).coerceAtLeast(0)) }
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
    ) {
        items(Tone.entries, key = { it.name }) { tone -> PaletteCard(tone, tone == current) { onPick(tone) } }
    }
}

@Composable
private fun PaletteCard(tone: Tone, selected: Boolean, onClick: () -> Unit) {
    val day = if (tone.alwaysDark) tone.dark else tone.light
    val night = tone.dark
    val shape = RoundedCornerShape(18.dp)
    val edge by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant,
        label = "edge",
    )
    val edgeWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "edgeWidth")
    Column(
        Modifier
            .width(92.dp)
            .clip(shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = "${tone.label}. ${tone.blurb}"
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(92.dp, 120.dp).clip(shape).border(edgeWidth, edge, shape)) {
            Column(Modifier.fillMaxSize()) {
                // By day: a count in the palette's ink, a progress line, a milestone line in gold.
                Box(Modifier.weight(1f).fillMaxWidth().background(Color(day.page)).padding(10.dp)) {
                    Column {
                        Text("64", style = LedgerType.count.copy(fontSize = 28.sp, lineHeight = 28.sp), color = Color(day.ink))
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.width(40.dp).height(3.dp).background(Color(day.ink), RoundedCornerShape(2.dp)))
                        Spacer(Modifier.height(5.dp))
                        Box(Modifier.width(58.dp).height(3.dp).background(Color(day.accent), RoundedCornerShape(2.dp)))
                    }
                    Box(Modifier.align(Alignment.TopEnd).size(16.dp).background(Color(day.tint), CircleShape))
                }
                // By night.
                Row(
                    Modifier.height(30.dp).fillMaxWidth().background(Color(night.page)).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(24.dp).height(3.dp).background(Color(night.ink), RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(7.dp).background(Color(night.accent), CircleShape))
                }
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.onBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tone.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
    }
}
