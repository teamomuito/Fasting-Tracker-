package app.fasting.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.domain.Durations
import app.fasting.domain.Fast
import app.fasting.domain.FastStatus
import app.fasting.domain.computeStats
import app.fasting.ui.FastingViewModel
import app.fasting.ui.Screen
import app.fasting.ui.UiState
import app.fasting.ui.components.DsButtonRow
import app.fasting.ui.components.DsLetterButton
import app.fasting.ui.components.DsPanel
import app.fasting.ui.components.DsScreen
import app.fasting.ui.components.DsText
import app.fasting.ui.theme.DsColors
import java.time.ZoneId

private const val CHART_SLOTS = 14

/** Totals, averages, the streak, and a bar chart of the most recent fasts. */
@Composable
fun StatsScreen(vm: FastingViewModel, ui: UiState, fasts: List<Fast>) {
    val stats = remember(fasts) { computeStats(fasts, System.currentTimeMillis(), ZoneId.systemDefault()) }
    val recent = fasts
        .filter { it.status == FastStatus.COMPLETED }
        .sortedBy { it.startAt }
        .takeLast(CHART_SLOTS)
    val none = stats.totalFasts == 0

    DsScreen(
        title = "STATS",
        prompt = ui.message ?: "Your fasting so far.",
        bottom = {
            DsButtonRow {
                DsLetterButton("B", "Back", Modifier.weight(1f)) { vm.go(Screen.TIMER) }
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("TOTAL FASTS", stats.totalFasts.toString())
                StatTile("STREAK", "${stats.streakDays} days")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("AVERAGE", if (none) "--" else Durations.short(stats.averageMs))
                StatTile("LONGEST", if (none) "--" else Durations.short(stats.longestMs))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("CRAVINGS RESISTED", stats.cravingsResisted.toString())
            }
            DsText(
                "Streak counts days in a row with a fast that reached its target.",
                12.sp,
                color = DsColors.Subtle,
            )

            DsPanel(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DsText("RECENT FASTS (BLUE = TARGET MET)", 12.sp, color = DsColors.Subtle)
                    if (recent.isEmpty()) {
                        DsText("No finished fasts yet.", 14.sp, color = DsColors.Subtle)
                    } else {
                        BarChart(recent)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatTile(label: String, value: String) {
    DsPanel(Modifier.weight(1f), padding = PaddingValues(12.dp)) {
        Column {
            DsText(label, 12.sp, color = DsColors.Subtle)
            Spacer(Modifier.height(6.dp))
            DsText(value, 22.sp)
        }
    }
}

/** One bar per fast, oldest on the left. Bars are scaled to the longest fast shown. */
@Composable
private fun BarChart(fasts: List<Fast>) {
    val longest = fasts.maxOf { it.actualMs }.coerceAtLeast(1L)
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val slot = size.width / CHART_SLOTS
        val barWidth = slot * 0.6f
        val baseline = size.height
        fasts.forEachIndexed { index, fast ->
            val barHeight = size.height * (fast.actualMs.toFloat() / longest)
            drawRect(
                color = if (fast.goalMet) DsColors.Cursor else DsColors.Subtle,
                topLeft = Offset(index * slot + (slot - barWidth) / 2, baseline - barHeight),
                size = Size(barWidth, barHeight),
            )
        }
        drawLine(
            color = DsColors.Border,
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
