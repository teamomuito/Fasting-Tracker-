package app.fasting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.domain.FastSpan
import app.fasting.domain.monthCells
import app.fasting.ui.theme.DsColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val WEEKDAYS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Month grids that highlight the days a fast covers. The first and last day are solid blue,
 * the days between are light blue, and today has a border.
 */
@Composable
fun DsCalendar(span: FastSpan, today: LocalDate, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        span.months.forEach { month ->
            val title = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase() + " ${month.year}"
            DsText(title, 13.sp)
            Row(Modifier.fillMaxWidth()) {
                WEEKDAYS.forEach { DsText(it, 11.sp, Modifier.weight(1f), DsColors.Subtle, TextAlign.Center) }
            }
            monthCells(month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { i -> DayCell(week.getOrNull(i), span, today, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate?, span: FastSpan, today: LocalDate, modifier: Modifier) {
    val cell = modifier.aspectRatio(1f)
    if (day == null) {
        Box(cell)
        return
    }
    val edge = day == span.first || day == span.last
    val fill = when {
        edge -> DsColors.Cursor
        day in span -> DsColors.Pressed
        else -> Color.Transparent
    }
    val ring = if (day == today) Modifier.border(2.dp, DsColors.Border) else Modifier
    Box(cell.padding(1.dp).background(fill).then(ring), contentAlignment = Alignment.Center) {
        DsText(day.dayOfMonth.toString(), 12.sp, color = if (edge) Color.White else DsColors.Text)
    }
}

