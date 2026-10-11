package app.fasting.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fasting.domain.FastStatus
import app.fasting.ui.screens.CravingScreen
import app.fasting.ui.screens.HistoryScreen
import app.fasting.ui.screens.StatsScreen
import app.fasting.ui.screens.TimerScreen

/** Switches between the four screens. There is no nav graph: the app has one level of depth. */
@Composable
fun FastingRoot(vm: FastingViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val fasts by vm.fasts.collectAsStateWithLifecycle()
    val active = fasts.firstOrNull { it.status != FastStatus.COMPLETED }

    // Alerts can be late while the app is closed, so catch up whenever the screen comes back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.sync() }
    BackHandler(enabled = ui.screen != Screen.TIMER) { vm.go(Screen.TIMER) }

    when (ui.screen) {
        Screen.TIMER -> TimerScreen(vm, ui, active)
        Screen.CRAVING -> active?.let { CravingScreen(vm, it) } ?: TimerScreen(vm, ui, active)
        Screen.HISTORY -> HistoryScreen(vm, ui, fasts)
        Screen.STATS -> StatsScreen(vm, ui, fasts)
    }
}
