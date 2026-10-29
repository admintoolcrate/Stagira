package com.stagirite.ledger.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.data.DayState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val monthTitle = DateTimeFormatter.ofPattern("LLLL yyyy")

/**
 * A month of days. Kept days are filled discs, slips are hollow rings, so the
 * two read apart by shape, not only by shade. Today is underlined.
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    today: LocalDate,
    earliest: YearMonth,
    stateOf: (LocalDate) -> DayState,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: ((LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier,
    slipLabel: String = "Slip",
    /** Urges resisted on a day; days with any get a small dot. */
    urgesOf: (LocalDate) -> Int = { 0 },
    showUrgeKey: Boolean = false,
    showRestKey: Boolean = false,
) {
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val latest = YearMonth.from(today)

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                month.format(monthTitle.withLocale(locale)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }, enabled = month > earliest) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }, enabled = month < latest) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            (0 until 7).forEach { i ->
                Text(
                    firstDay.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - firstDay.value + 7) % 7
        val cells = lead + month.lengthOfMonth()
        val weeks = (cells + 6) / 7
        for (w in 0 until weeks) {
            Row(Modifier.fillMaxWidth()) {
                for (d in 0 until 7) {
                    val index = w * 7 + d - lead
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (index in 0 until month.lengthOfMonth()) {
                            val day = first.plusDays(index.toLong())
                            DayCell(day, stateOf(day), day == today, onDayClick, slipLabel, urgesOf(day))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        CalendarLegend(slipLabel, showUrgeKey, showRestKey)
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    state: DayState,
    isToday: Boolean,
    onClick: ((LocalDate) -> Unit)?,
    slipLabel: String,
    urges: Int,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val clickable = onClick != null && (state == DayState.Kept || state == DayState.Slip)
    val label = "${day.format(mediumDate)}: " + when (state) {
        DayState.Kept -> "kept"
        DayState.Slip -> slipLabel.lowercase()
        DayState.Untracked -> "not tracked"
        DayState.Rest -> "rest day"
        DayState.Future -> "upcoming"
    } + if (urges > 0) ", ${if (urges == 1) "1 urge" else "$urges urges"} resisted" else ""
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp)
            .then(
                if (clickable) {
                    Modifier.clickable(onClickLabel = if (state == DayState.Slip) "Remove ${slipLabel.lowercase()}" else "Log a ${slipLabel.lowercase()}", role = Role.Button) { onClick?.invoke(day) }
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .then(
                    when (state) {
                        DayState.Kept -> Modifier.background(ink, CircleShape)
                        DayState.Slip -> Modifier.border(BorderStroke(1.5.dp, ink), CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (state == DayState.Rest) "\u2013" else "${day.dayOfMonth}",
                style = MaterialTheme.typography.labelMedium,
                color = when (state) {
                    DayState.Kept -> MaterialTheme.colorScheme.background
                    DayState.Slip -> ink
                    else -> MaterialTheme.colorScheme.outline
                },
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(if (isToday) 12.dp else 0.dp)
                    .height(2.dp)
                    .background(ink),
            )
            if (urges > 0) {
                if (isToday) Spacer(Modifier.width(3.dp))
                Box(Modifier.size(4.dp).background(ink, CircleShape))
            }
        }
    }
}

@Composable
private fun CalendarLegend(slipLabel: String, showUrgeKey: Boolean = false, showRestKey: Boolean = false) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(ink, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text("Kept", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(10.dp).border(BorderStroke(1.5.dp, ink), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(slipLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.width(10.dp).height(2.dp).background(ink))
        Spacer(Modifier.width(6.dp))
        Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (showRestKey) {
            Spacer(Modifier.width(16.dp))
            Text("\u2013 Rest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showUrgeKey) {
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(4.dp).background(ink, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("Urge resisted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val yearMonthName = DateTimeFormatter.ofPattern("LLL")

/**
 * Twelve small months. Kept days are filled squares, slips are hollow, so they
 * read apart by shape; tap a month to open it.
 */
@Composable
fun YearCalendar(
    year: Int,
    today: LocalDate,
    earliestYear: Int,
    stateOf: (LocalDate) -> DayState,
    onYearChange: (Int) -> Unit,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val ink = MaterialTheme.colorScheme.onBackground
    val faint = MaterialTheme.colorScheme.outlineVariant

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$year", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { onYearChange(year - 1) }, enabled = year > earliestYear) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
            }
            IconButton(onClick = { onYearChange(year + 1) }, enabled = year < today.year) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next year")
            }
        }
        Spacer(Modifier.height(8.dp))
        (0 until 4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                (1..3).forEach { col ->
                    val month = YearMonth.of(year, row * 3 + col)
                    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
                    val states = days.map(stateOf)
                    val kept = states.count { it == DayState.Kept }
                    val slipped = states.count { it == DayState.Slip }
                    val rested = states.count { it == DayState.Rest }
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(enabled = !month.atDay(1).isAfter(today)) { onMonthClick(month) }
                            .semantics {
                                contentDescription = "${month.format(DateTimeFormatter.ofPattern("LLLL").withLocale(locale))}: " +
                                    "$kept kept, $slipped ${if (slipped == 1) "slip" else "slips"}" +
                                    if (rested > 0) ", $rested rest days" else ""
                            }
                            .padding(vertical = 6.dp),
                    ) {
                        Text(
                            month.format(yearMonthName.withLocale(locale)),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (month == YearMonth.from(today)) ink else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        val lead = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
                        val weeks = (lead + days.size + 6) / 7
                        Canvas(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(7f / weeks),
                        ) {
                            val cell = size.width / 7
                            val dot = cell * 0.72f
                            val stroke = (cell * 0.12f).coerceAtLeast(1f)
                            days.forEachIndexed { i, _ ->
                                val slot = lead + i
                                val x = (slot % 7) * cell + (cell - dot) / 2
                                val y = (slot / 7) * cell + (cell - dot) / 2
                                val corner = CornerRadius(dot * 0.25f)
                                when (states[i]) {
                                    DayState.Kept -> drawRoundRect(ink, Offset(x, y), Size(dot, dot), corner)
                                    DayState.Slip -> drawRoundRect(
                                        ink, Offset(x + stroke / 2, y + stroke / 2), Size(dot - stroke, dot - stroke), corner,
                                        style = Stroke(stroke),
                                    )
                                    DayState.Untracked -> drawRoundRect(faint, Offset(x, y), Size(dot, dot), corner)
                                    // Rest days: a small centred mark, so the week's rhythm shows.
                                    DayState.Rest -> drawCircle(faint, dot * 0.18f, Offset(x + dot / 2, y + dot / 2))
                                    DayState.Future -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(ink, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(6.dp))
            Text("Kept", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(9.dp).border(BorderStroke(1.5.dp, ink), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(6.dp))
            Text("Slip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(9.dp).background(faint, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(6.dp))
            Text("Not tracked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A month calendar with a Month | Year switch; tapping a month in the year opens it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerCalendar(
    today: LocalDate,
    earliest: LocalDate,
    stateOf: (LocalDate) -> DayState,
    modifier: Modifier = Modifier,
    onDayClick: ((LocalDate) -> Unit)? = null,
    slipLabel: String = "Slip",
    urgesOf: (LocalDate) -> Int = { 0 },
    showUrgeKey: Boolean = false,
    showRestKey: Boolean = false,
) {
    var yearView by rememberSaveable { mutableStateOf(false) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var year by rememberSaveable { mutableStateOf(today.year) }
    val plus = LocalPlus.current

    Column(modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            listOf(false, true).forEachIndexed { i, isYear ->
                SegmentedButton(
                    selected = yearView == isYear,
                    onClick = {
                        if (isYear && !plus.has) plus.offer("The year view is part of Plus.") else yearView = isYear
                    },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) { Text(if (isYear) "Year" else "Month") }
            }
        }
        if (yearView) {
            YearCalendar(
                year = year,
                today = today,
                earliestYear = earliest.year,
                stateOf = stateOf,
                onYearChange = { year = it },
                onMonthClick = {
                    month = it.toString()
                    yearView = false
                },
            )
        } else {
            MonthCalendar(
                month = YearMonth.parse(month),
                today = today,
                earliest = YearMonth.from(earliest),
                stateOf = stateOf,
                onMonthChange = {
                    month = it.toString()
                    year = it.year
                },
                onDayClick = onDayClick,
                slipLabel = slipLabel,
                urgesOf = urgesOf,
                showUrgeKey = showUrgeKey,
                showRestKey = showRestKey,
            )
        }
    }
}
