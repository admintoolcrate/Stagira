package com.stagirite.ledger.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.SortOrder
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.data.WidgetStyle
import com.stagirite.ledger.widget.LedgerWidget
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Settings → Appearance, on a page of its own, laid out as on the website: a live preview
 * of the app at the top, then the mode (Auto, Light, Dark or Black), the colour, and the
 * widgets' background. Every choice applies at once, and the preview shows it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    mode: ThemeMode,
    tone: Tone,
    widgetStyle: WidgetStyle,
    goals: List<Goal>,
    onMode: (ThemeMode) -> Unit,
    onTone: (Tone) -> Unit,
    onWidgetStyle: (WidgetStyle) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", style = LedgerType.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = plainTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.l),
        ) {
            val systemDark = isSystemInDarkTheme()
            val dark = when (mode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark, ThemeMode.Black -> true
            }
            Preview(goals, tone, dark, mode == ThemeMode.Black)

            Spacer(Modifier.height(Space.xl))
            SectionHeading("Mode")
            val modes = ThemeMode.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { onMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        icon = {},
                        colors = ledgerSegmentColors(),
                    ) { Text(m.label) }
                }
            }
            Spacer(Modifier.height(Space.s))
            Text(
                when (mode) {
                    ThemeMode.System -> "Light by day, dark when your phone switches to dark mode."
                    ThemeMode.Light -> "Always light."
                    ThemeMode.Dark -> "Always dark."
                    ThemeMode.Black -> "Always dark, on true black: easiest on the eyes at night, and on OLED screens."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Space.xl))
            SectionHeading("Colour")
            ColourSwatches(tone, dark, mode == ThemeMode.Black, onTone)
            Spacer(Modifier.height(Space.m))
            // As the website names them: the colour in the serif's italic, then a line about it.
            Text(
                tone.label,
                style = LedgerType.title.copy(fontSize = 24.sp, fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(tone.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(Space.xl))
            SectionHeading("Widgets")
            WidgetStyles(widgetStyle, onWidgetStyle)
            Spacer(Modifier.height(Space.s))
            Text(
                "Match app follows the colour and mode above. Each widget can also have its own: " +
                    "long-press it on your home screen and choose its settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

/**
 * The app itself, small: the Goals list and a goal's page side by side, drawn with the
 * chosen colour and mode. Only a picture: touches pass it by.
 */
@Composable
private fun Preview(goals: List<Goal>, tone: Tone, dark: Boolean, black: Boolean) {
    val today = remember { LocalDate.now() }
    val shown = remember(goals) { goals.ifEmpty { demoGoals(today) }.take(4) }
    val privacy = remember { PrivacyUi(hasPin = false, unlocked = false, requestPin = {}, open = { null }) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = Space.s)
            .clearAndSetSemantics { contentDescription = "Preview of the app in ${tone.label}" },
        horizontalArrangement = Arrangement.spacedBy(Space.m, Alignment.CenterHorizontally),
    ) {
        MockPhone(tone, dark, black) {
            GoalListScreen(
                shown, today, remember { SnackbarHostState() }, privacy, emptyList(), SortOrder.Added,
                onSort = {}, onOpen = {}, onAdd = {}, onQuickSlip = { _, _ -> }, onQuickUrge = {},
                onArchive = { _, _ -> }, onTitleLongPress = {}, onSettings = {}, onWeekly = {},
                bottomBar = { HomeNav(false) {} },
            )
        }
        MockPhone(tone, dark, black) {
            GoalDetailScreen(
                shown.first(), shown.first().title, today, privacy,
                onBack = {}, onEdit = {}, onSlip = {}, onRemoveSlip = {}, onMoveStart = {}, onArchive = {}, onDelete = {},
            )
        }
    }
}

@Composable
private fun MockPhone(tone: Tone, dark: Boolean, black: Boolean, screen: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .size(150.dp, 316.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        Box(Modifier.requiredSize(360.dp, 760.dp).graphicsLayer { scaleX = 150f / 360f; scaleY = 150f / 360f }) {
            LedgerTheme(dark, tone, black) { screen() }
        }
        // Swallows every touch, so the preview can't be scrolled or tapped.
        Box(
            Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            },
        )
    }
}

/** The colours as round swatches, as on the website: each one's page with a dot of its ink. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColourSwatches(current: Tone, dark: Boolean, black: Boolean, onPick: (Tone) -> Unit) {
    // All eight on one line on a phone; they wrap only on very narrow screens.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Tone.entries.forEach { tone ->
            val t = if (black) tone.black else if (dark) tone.dark else tone.light
            val selected = tone == current
            val ring by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant,
                label = "ring",
            )
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(if (selected) 2.dp else 1.dp, ring, CircleShape)
                    .padding(if (selected) 4.dp else 0.dp)
                    .clip(CircleShape)
                    .background(Color(t.page))
                    .clickable(role = Role.RadioButton) { onPick(tone) }
                    .semantics {
                        contentDescription = "${tone.label}. ${tone.blurb}"
                        this.selected = selected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(11.dp).background(Color(t.ink), CircleShape))
            }
        }
    }
}

/** The widgets' background, by the same names the widgets' own settings use. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetStyles(current: WidgetStyle, onPick: (WidgetStyle) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        WidgetStyle.entries.forEach { style ->
            FilterChip(
                selected = style == current,
                onClick = {
                    onPick(style)
                    // Redraw placed widgets now rather than at their next update.
                    scope.launch { runCatching { LedgerWidget.refresh(context) } }
                },
                label = { Text(style.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

/** A few made-up goals for the preview, before any of your own exist. */
private fun demoGoals(today: LocalDate): List<Goal> {
    fun ago(n: Long) = today.minusDays(n)
    return listOf(
        Goal("demo-1", "Drink less", ago(120), slips = listOf(ago(64)), icon = "🍷", kind = GoalKind.Vice),
        Goal("demo-2", "No phone in bed", ago(21), icon = "📵", kind = GoalKind.Vice),
        Goal("demo-3", "Read before bed", ago(60), slips = listOf(ago(12)), icon = "📖", kind = GoalKind.Virtue),
    )
}
