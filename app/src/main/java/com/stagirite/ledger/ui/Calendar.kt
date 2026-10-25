package com.stagirite.ledger.ui

import androidx.compose.foundation.BorderStroke
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
                            DayCell(day, stateOf(day), day == today, onDayClick, slipLabel)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        CalendarLegend(slipLabel)
    }
}

@Composable
private fun DayCell(day: LocalDate, state: DayState, isToday: Boolean, onClick: ((LocalDate) -> Unit)?, slipLabel: String) {
    val ink = MaterialTheme.colorScheme.onBackground
    val clickable = onClick != null && (state == DayState.Kept || state == DayState.Slip)
    val label = "${day.format(mediumDate)}: " + when (state) {
        DayState.Kept -> "kept"
        DayState.Slip -> slipLabel.lowercase()
        DayState.Untracked -> "not tracked"
        DayState.Future -> "upcoming"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp)
            .then(if (clickable) Modifier.clickable { onClick?.invoke(day) } else Modifier)
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
                "${day.dayOfMonth}",
                style = MaterialTheme.typography.labelMedium,
                color = when (state) {
                    DayState.Kept -> MaterialTheme.colorScheme.background
                    DayState.Slip -> ink
                    else -> MaterialTheme.colorScheme.outline
                },
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .width(12.dp)
                .height(2.dp)
                .background(if (isToday) ink else MaterialTheme.colorScheme.background),
        )
    }
}

@Composable
private fun CalendarLegend(slipLabel: String) {
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
    }
}
