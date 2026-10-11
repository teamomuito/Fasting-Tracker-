package app.fasting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.fasting.data.FastRepository
import app.fasting.domain.CravingOutcome
import app.fasting.domain.Durations
import app.fasting.domain.Fast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { TIMER, CRAVING, HISTORY, STATS }

/** Screen-level state. Fasts themselves come from the repository through [FastingViewModel.fasts]. */
data class UiState(
    val screen: Screen = Screen.TIMER,
    val lengthHours: Double = 16.0,
    val startInHours: Double = 0.0,
    val message: String? = null,
)

class FastingViewModel(private val repository: FastRepository) : ViewModel() {

    val fasts: StateFlow<List<Fast>> = repository.observeFasts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var messageJob: Job? = null

    init {
        sync()
    }

    fun sync() {
        viewModelScope.launch { repository.sync() }
    }

    fun exactAlarmsAllowed(): Boolean = repository.exactAlarmsAllowed()

    fun go(screen: Screen) {
        _ui.update { it.copy(screen = screen) }
    }

    fun setLength(hours: Double) {
        _ui.update { it.copy(lengthHours = hours) }
    }

    fun setStartIn(hours: Double) {
        _ui.update { it.copy(startInHours = hours) }
    }

    /** Starts the chosen fast now, or schedules it for after the chosen delay. */
    fun beginFast() {
        val state = _ui.value
        val startAt = System.currentTimeMillis() + Durations.hoursToMillis(state.startInHours)
        val durationMs = Durations.hoursToMillis(state.lengthHours)
        viewModelScope.launch {
            val started = repository.begin(startAt, durationMs)
            flash(
                when {
                    !started -> "Finish or cancel your current fast first."
                    state.startInHours == 0.0 -> "Fast started!"
                    else -> "Fast scheduled!"
                },
            )
        }
    }

    fun endFast(id: Long) {
        viewModelScope.launch {
            repository.endEarly(id)
            flash("Fast ended.")
        }
    }

    fun cancelScheduled(id: Long) {
        viewModelScope.launch {
            repository.cancelScheduled(id)
            flash("Schedule cancelled.")
        }
    }

    /** Logs a craving and returns to the timer. Giving in also ends the fast. */
    fun logCraving(fastId: Long, outcome: CravingOutcome) {
        viewModelScope.launch {
            repository.logCraving(fastId, outcome)
            _ui.update { it.copy(screen = Screen.TIMER) }
            flash(if (outcome == CravingOutcome.RESISTED) "Craving beaten!" else "Fast ended. Enjoy your meal.")
        }
    }

    fun deleteFast(id: Long) {
        viewModelScope.launch {
            repository.deleteFast(id)
            flash("Deleted!")
        }
    }

    /** Shows [text] in the prompt panel for 1.5 seconds, like pictoclip's "Copied!" flash. */
    private fun flash(text: String) {
        messageJob?.cancel()
        _ui.update { it.copy(message = text) }
        messageJob = viewModelScope.launch {
            delay(1500)
            _ui.update { it.copy(message = null) }
        }
    }

    companion object {
        fun factory(repository: FastRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { FastingViewModel(repository) }
        }
    }
}
