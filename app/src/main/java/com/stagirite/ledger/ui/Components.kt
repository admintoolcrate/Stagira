package com.stagirite.ledger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import com.stagirite.ledger.R
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.Quote
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val mediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
val longDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun plainTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
)

@Composable
fun ThinProgress(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
        color = MaterialTheme.colorScheme.onBackground,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** A small, spaced-out capital label above a group. */
@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 4.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

/** A number that rolls up or down to its new value. */
@Composable
fun RollingNumber(value: Long, style: TextStyle, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically(tween(300)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(300))) togetherWith
                (slideOutVertically(tween(300)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(200)))
        },
        label = "count",
        modifier = modifier,
    ) { n -> Text("$n", style = style) }
}

/** A flat, rounded panel that groups related content. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val color = MaterialTheme.colorScheme.surfaceContainerLow
    if (onClick != null && onLongClick != null) {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            Column(
                Modifier
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(padding),
                content = content,
            )
        }
    } else if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

/** A thin ring filling clockwise from the top, with [content] in the middle. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 4.dp,
    /** What a screen reader says for the ring and what's in it. */
    description: String? = null,
    content: @Composable () -> Unit,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onBackground
    // Eases to its new value, e.g. after a slip or as a day ticks over.
    val shown by animateFloatAsState(progress, animationSpec = tween(700), label = "ring")
    Box(
        modifier.then(if (description != null) Modifier.clearAndSetSemantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = Size(size.width - w, size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            if (shown > 0f) {
                drawArc(ink, -90f, 360f * shown, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** A headline figure with a label underneath. */
@Composable
fun Figure(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 10.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The monochrome emoji font (Noto Emoji, SIL Open Font License). */
val EmojiFont = FontFamily(Font(R.font.noto_emoji))

/** Draws [icon] in the monochrome emoji font, in the current ink colour. */
@Composable
fun EmojiGlyph(icon: String, size: TextUnit, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onBackground) {
    Text(
        icon,
        modifier = modifier,
        color = color,
        style = TextStyle(
            fontFamily = EmojiFont,
            fontSize = size,
            // Don't let the system swap in its colour emoji.
            platformStyle = PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.None),
        ),
    )
}

/** A goal's icon in a soft round tile. Nothing is drawn when there's no icon. */
@Composable
fun GoalIcon(icon: String, modifier: Modifier = Modifier, tile: Dp = 44.dp) {
    if (icon.isEmpty()) return
    Box(
        modifier
            .size(tile)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        EmojiGlyph(icon, (tile.value * 0.5f).sp)
    }
}

/** "VICE" / "VIRTUE", small and spaced. */
@Composable
fun KindLabel(kind: GoalKind?, modifier: Modifier = Modifier) {
    if (kind == null) return
    Text(
        kind.label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * A quotation set apart: a large thin opening mark, the passage in light type
 * with generous leading, then a short rule and the attribution in small capitals.
 */
@Composable
fun QuoteBlock(quote: Quote, modifier: Modifier = Modifier) {
    // Turned off under Settings → Personalise.
    if (!LocalUiPrefs.current.quotes) return
    Column(modifier.fillMaxWidth()) {
        // The passage and both marks are set in the bundled serif, so they look the same on
        // every phone: the opening mark at the top left, its twin at the bottom right.
        QuoteMark("\u201C", TextAlign.Start)
        Text(quote.text, style = LedgerType.passage)
        QuoteMark("\u201D", TextAlign.End)
        Spacer(Modifier.height(Space.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .width(20.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onBackground),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "ARISTOTLE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp, fontWeight = FontWeight.Medium),
                )
                Text(
                    "${quote.work} \u00B7 tr. H. Rackham",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        quote.note?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Garamond, fontStyle = FontStyle.Italic, fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuoteMark(mark: String, align: TextAlign) {
    Text(
        mark,
        style = LedgerType.mark.copy(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        color = MaterialTheme.colorScheme.outline,
        textAlign = align,
        // The glyph sits high in its line; a short box trims the space beneath it.
        modifier = Modifier.fillMaxWidth().height(24.dp).clearAndSetSemantics {},
    )
}

/** Asks for a numeric PIN, optionally twice. */
@Composable
fun PinDialog(
    title: String,
    message: String,
    confirm: String,
    repeat: Boolean,
    busy: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    minLength: Int = 4,
) {
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val valid = pin.length in minLength..8 && (!repeat || pin == again)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= 8 && v.all(Char::isDigit)) pin = v },
                    label = { Text("PIN") },
                    supportingText = { if (repeat) Text("$minLength to 8 digits") },
                    isError = error != null,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (repeat) {
                    OutlinedTextField(
                        value = again,
                        onValueChange = { v -> if (v.length <= 8 && v.all(Char::isDigit)) again = v },
                        label = { Text("Repeat PIN") },
                        isError = again.isNotEmpty() && again != pin,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = valid && !busy) { Text(if (busy) "Checking\u2026" else confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

/** "Mon · Wed · Fri" in the user's language, Monday first. */
fun scheduleText(days: Set<java.time.DayOfWeek>): String =
    java.time.DayOfWeek.entries.filter { it in days }
        .joinToString(" \u00B7 ") { it.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) }

/** The line under the name. */
const val APP_SUBTITLE = "The Habit Ledger"

/**
 * The name set as a wordmark: "Stagira", with the subtitle in small spaced
 * capitals beneath. [subtitleAlpha] lets a collapsing bar fade the subtitle out.
 */
@Composable
fun Wordmark(modifier: Modifier = Modifier, large: Boolean = true, subtitleAlpha: Float = 1f) {
    Column(modifier) {
        Text(
            "Stagira",
            style = (if (large) LedgerType.name else LedgerType.name.copy(fontSize = 26.sp, lineHeight = 30.sp)).capped(),
            maxLines = 1,
        )
        if (subtitleAlpha > 0.01f) {
            Text(
                APP_SUBTITLE.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.2.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = subtitleAlpha),
                maxLines = 1,
            )
        }
    }
}

/** Text tabs with a short rule under the chosen one, over a hairline. */
@Composable
fun TextTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                val on = i == selected
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                        .padding(top = Space.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.m))
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .background(if (on) MaterialTheme.colorScheme.onBackground else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Space.l))
    }
}

/**
 * An outlined button that also answers a press-and-hold: the fill sweeps
 * across while held, and [onHeld] runs when it reaches the end. Letting go
 * early cancels; a quick tap runs [onTap]. With [onHeld] null it's a plain button.
 */
@Composable
fun HoldButton(text: String, onTap: () -> Unit, onHeld: (() -> Unit)?, modifier: Modifier = Modifier) {
    val fill = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val buzz = rememberBuzz()
    val shape = RoundedCornerShape(26.dp)
    val ink = MaterialTheme.colorScheme.onBackground
    val outline = MaterialTheme.colorScheme.outline
    val held by rememberUpdatedState(onHeld)
    val tap by rememberUpdatedState(onTap)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, outline, shape)
            .drawBehind { drawRect(ink.copy(alpha = 0.14f), size = size.copy(width = size.width * fill.value)) }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = text) { tap(); true }
                held?.let { onLongClick(label = "Log for today") { it(); true } }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val action = held
                    val sweep = if (action == null) null else scope.launch {
                        fill.animateTo(1f, tween(durationMillis = 700, easing = LinearEasing))
                        buzz()
                        action()
                        fill.snapTo(0f)
                    }
                    val released = tryAwaitRelease()
                    if (sweep == null) {
                        if (released) tap()
                    } else if (sweep.isActive) {
                        val quick = fill.value < 0.2f
                        sweep.cancel()
                        scope.launch { fill.animateTo(0f, tween(150)) }
                        if (released && quick) tap()
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
