package app.fasting.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.fasting.domain.Durations
import app.fasting.domain.Fast
import app.fasting.domain.FastStatus
import app.fasting.domain.fastSpan
import app.fasting.domain.formatDateTime
import app.fasting.ui.FastingViewModel
import app.fasting.ui.Screen
import app.fasting.ui.UiState
import app.fasting.ui.components.DsActionButton
import app.fasting.ui.components.DsButtonRow
import app.fasting.ui.components.DsCalendar
import app.fasting.ui.components.DsChoice
import app.fasting.ui.components.DsLetterButton
import app.fasting.ui.components.DsPanel
import app.fasting.ui.components.DsRing
import app.fasting.ui.components.DsScreen
import app.fasting.ui.components.DsTag
import app.fasting.ui.components.DsText
import app.fasting.ui.components.rememberNow
import app.fasting.ui.theme.DsColors

private val LENGTH_PRESETS = listOf(12.0, 16.0, 18.0, 20.0, 24.0)
private val START_PRESETS = listOf(0.0, 1.0, 2.0, 4.0, 8.0)

/** Start, schedule, watch, and end a fast. The ring shows the countdown or the elapsed progress. */
@Composable
fun TimerScreen(vm: FastingViewModel, ui: UiState, active: Fast?) {
    val context = LocalContext.current
    val now by rememberNow(periodMs = if (active == null) 60_000L else 1_000L)

    var exactOk by remember { mutableStateOf(vm.exactAlarmsAllowed()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { exactOk = vm.exactAlarmsAllowed() }

    var editLength by rememberSaveable { mutableStateOf(false) }
    var editStartIn by rememberSaveable { mutableStateOf(false) }
    var confirmEnd by rememberSaveable { mutableStateOf(false) }

    val notificationAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun startFast() {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsAsk) notificationAsk.launch(Manifest.permission.POST_NOTIFICATIONS)
        vm.beginFast()
    }

    DsScreen(
        title = "FASTING",
        prompt = ui.message ?: idlePrompt(active, now),
        bottom = {
            DsButtonRow {
                DsLetterButton("X", "History", Modifier.weight(1f)) { vm.go(Screen.HISTORY) }
                DsLetterButton("Y", "Stats", Modifier.weight(1f)) { vm.go(Screen.STATS) }
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!exactOk) {
                DsPanel(Modifier.fillMaxWidth(), fill = DsColors.Bg) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DsText(
                            "Exact alarms are off, so start and end alerts may arrive a few minutes late.",
                            13.sp,
                        )
                        DsTag("ALLOW EXACT ALARMS") { openExactAlarmSettings(context) }
                    }
                }
            }

            val face = ringFace(ui, active, now)
            DsPanel(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DsRing(progress = face.progress, modifier = Modifier.size(240.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DsText(face.label, 12.sp, color = DsColors.Subtle)
                            DsText(face.value, 28.sp)
                            DsText(face.footer, 12.sp, color = DsColors.Subtle)
                        }
                    }
                }
            }

            if (active == null) {
                SectionTitle("FAST LENGTH")
                OptionGrid(
                    presets = LENGTH_PRESETS,
                    selectedHours = ui.lengthHours,
                    label = { Durations.hoursLabel(it).uppercase() },
                    onPreset = { vm.setLength(it) },
                    onCustom = { editLength = true },
                )

                SectionTitle("START IN")
                OptionGrid(
                    presets = START_PRESETS,
                    selectedHours = ui.startInHours,
                    label = { if (it == 0.0) "NOW" else Durations.hoursLabel(it).uppercase() },
                    onPreset = { vm.setStartIn(it) },
                    onCustom = { editStartIn = true },
                )
                PlanLine(ui, now)
            }

            val plannedStart = active?.startAt ?: (now + Durations.hoursToMillis(ui.startInHours))
            val plannedEnd = active?.targetEndAt ?: (plannedStart + Durations.hoursToMillis(ui.lengthHours))
            DsPanel(Modifier.fillMaxWidth()) {
                DsCalendar(fastSpan(plannedStart, plannedEnd), Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate())
            }

            when {
                active == null -> DsActionButton(
                    label = if (ui.startInHours == 0.0) "START FAST" else "SCHEDULE FAST",
                    modifier = Modifier.fillMaxWidth(),
                    fill = DsColors.TopDark,
                    textColor = Color.White,
                ) { startFast() }

                active.status == FastStatus.SCHEDULED -> DsActionButton(
                    label = "CANCEL SCHEDULE",
                    modifier = Modifier.fillMaxWidth(),
                ) { vm.cancelScheduled(active.id) }

                else -> DsActionButton(
                    label = "END FAST",
                    modifier = Modifier.fillMaxWidth(),
                ) { confirmEnd = true }
            }

            if (active?.status == FastStatus.ACTIVE) {
                DsActionButton(
                    label = "I WANNA EAT",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    fill = DsColors.Cursor,
                    textColor = Color.White,
                ) { vm.go(Screen.CRAVING) }
            }
        }
    }

    if (editLength) {
        HoursDialog(
            title = "Fast length (hours)",
            onConfirm = {
                vm.setLength(it)
                editLength = false
            },
            onDismiss = { editLength = false },
        )
    }
    if (editStartIn) {
        HoursDialog(
            title = "Start in (hours)",
            onConfirm = {
                vm.setStartIn(it)
                editStartIn = false
            },
            onDismiss = { editStartIn = false },
        )
    }
    if (confirmEnd && active != null) {
        ConfirmDialog(
            text = "End this fast now? It will be saved with its real length.",
            confirmLabel = "END",
            onConfirm = {
                vm.endFast(active.id)
                confirmEnd = false
            },
            onDismiss = { confirmEnd = false },
        )
    }
}

private class RingFace(val progress: Float, val label: String, val value: String, val footer: String)

private fun ringFace(ui: UiState, active: Fast?, now: Long): RingFace = when {
    active == null -> RingFace(0f, "READY", Durations.hoursLabel(ui.lengthHours), "fast length")
    active.status == FastStatus.SCHEDULED -> RingFace(
        progress = 0f,
        label = "STARTS IN",
        value = Durations.clock(active.startAt - now),
        footer = "then ${Durations.short(active.durationMs)}",
    )
    else -> RingFace(
        progress = Durations.progress(active.startAt, active.durationMs, now),
        label = "ELAPSED",
        value = Durations.clock(now - active.startAt),
        footer = "LEFT ${Durations.clock(active.targetEndAt - now)}",
    )
}

private fun idlePrompt(active: Fast?, now: Long): String = when {
    active == null -> "Choose a length, then start."
    active.status == FastStatus.SCHEDULED -> "Scheduled for ${formatDateTime(active.startAt)}."
    else -> "Fasting. ${Durations.short(active.targetEndAt - now)} to go."
}

@Composable
private fun SectionTitle(text: String) {
    DsText(text, 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = DsColors.Subtle)
}

private class Option(val label: String, val selected: Boolean, val onClick: () -> Unit)

/** Preset choices in rows of three, plus a CUSTOM cell that shows the typed value once one is set. */
@Composable
private fun OptionGrid(
    presets: List<Double>,
    selectedHours: Double,
    label: (Double) -> String,
    onPreset: (Double) -> Unit,
    onCustom: () -> Unit,
) {
    val isCustom = selectedHours !in presets
    val options = presets.map { hours ->
        Option(label(hours), hours == selectedHours) { onPreset(hours) }
    } + Option(if (isCustom) label(selectedHours) else "CUSTOM", isCustom, onCustom)

    options.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { option ->
                DsChoice(option.label, option.selected, Modifier.weight(1f), option.onClick)
            }
        }
    }
}

@Composable
private fun PlanLine(ui: UiState, now: Long) {
    val start = now + Durations.hoursToMillis(ui.startInHours)
    val end = start + Durations.hoursToMillis(ui.lengthHours)
    Column(Modifier.fillMaxWidth()) {
        if (ui.startInHours > 0.0) {
            DsText("Starts ${formatDateTime(start)}", 13.sp, color = DsColors.Subtle)
        }
        DsText("Ends ${formatDateTime(end)}", 13.sp, color = DsColors.Subtle)
    }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    context.startActivity(
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
    )
}
