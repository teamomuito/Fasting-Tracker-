package app.fasting.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.domain.Durations
import app.fasting.domain.Fast
import app.fasting.domain.FastStatus
import app.fasting.domain.formatDateTime
import app.fasting.ui.FastingViewModel
import app.fasting.ui.Screen
import app.fasting.ui.UiState
import app.fasting.ui.components.DsButtonRow
import app.fasting.ui.components.DsLetterButton
import app.fasting.ui.components.DsPanel
import app.fasting.ui.components.DsPixelArt
import app.fasting.ui.components.DsScreen
import app.fasting.ui.components.DsText
import app.fasting.ui.theme.DsColors

/** A pixel clock, drawn with the same grid conventions as pictoclip's page icon. */
private val CLOCK_ART = listOf(
    "..#####..",
    ".#.....#.",
    "#...#...#",
    "#...#...#",
    "#...###.#",
    "#.......#",
    ".#.....#.",
    "..#####..",
)

/** Finished fasts, newest first. Tap one to select it, then A deletes it, as pictoclip's B does. */
@Composable
fun HistoryScreen(vm: FastingViewModel, ui: UiState, fasts: List<Fast>) {
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    val finished = fasts.filter { it.status == FastStatus.COMPLETED }
    val chosen = selected

    DsScreen(
        title = "HISTORY",
        prompt = ui.message ?: if (finished.isEmpty()) "No finished fasts yet." else "Tap a fast to select it.",
        bottom = {
            DsButtonRow {
                DsLetterButton("B", "Back", Modifier.weight(1f)) { vm.go(Screen.TIMER) }
                DsLetterButton("A", "Delete", Modifier.weight(1f), enabled = chosen != null) {
                    chosen?.let { vm.deleteFast(it) }
                    selected = null
                }
            }
        },
    ) {
        if (finished.isEmpty()) {
            DsPanel(Modifier.fillMaxWidth().padding(10.dp), fill = DsColors.Bg, padding = PaddingValues(24.dp)) {
                DsText(
                    "Start a fast and it will show up here.",
                    15.sp,
                    modifier = Modifier.fillMaxWidth(),
                    color = DsColors.Subtle,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(finished, key = { it.id }) { fast ->
                    FastRow(fast = fast, selected = chosen == fast.id) { selected = fast.id }
                }
            }
        }
    }
}

@Composable
private fun FastRow(fast: Fast, selected: Boolean, onClick: () -> Unit) {
    DsPanel(
        Modifier.fillMaxWidth(),
        fill = if (selected) DsColors.Pressed else DsColors.White,
        selected = selected,
        onClick = onClick,
        padding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DsPixelArt(CLOCK_ART, 3.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                DsText(
                    "${formatDateTime(fast.startAt)} -> ${formatDateTime(fast.endedAt ?: fast.targetEndAt)}",
                    14.sp,
                )
                val verdict = if (fast.goalMet) "goal met" else "ended early"
                DsText("${Durations.short(fast.actualMs)} ($verdict)", 13.sp, color = DsColors.Subtle)
                DsText("Cravings resisted: ${fast.cravingsResisted}", 13.sp, color = DsColors.Subtle)
            }
        }
    }
}
