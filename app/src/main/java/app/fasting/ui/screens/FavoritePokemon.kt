package app.fasting.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.domain.FavoritePicker
import app.fasting.domain.PickerState
import app.fasting.ui.components.DsActionButton
import app.fasting.ui.components.DsChoice
import app.fasting.ui.components.DsTag
import app.fasting.ui.components.DsText
import app.fasting.ui.theme.DsColors
import kotlin.random.Random

/**
 * Owns the favorite picker for the craving screen. It lives above the rotating cards, so the picks survive the card
 * rotating away and coming back. UNDO steps back through earlier states.
 */
class FavoritePickerSession(private val items: List<String>) {
    private val random = Random.Default
    private val history = mutableStateListOf<PickerState>()

    var state by mutableStateOf(FavoritePicker.start(items, random))
        private set

    val canUndo: Boolean get() = history.isNotEmpty()

    fun pick(picked: Set<String>) {
        push()
        state = FavoritePicker.pick(state, picked, random)
    }

    fun pass() {
        push()
        state = FavoritePicker.pass(state, random)
    }

    fun undo() {
        if (history.isNotEmpty()) {
            state = history.removeAt(history.lastIndex)
        }
    }

    fun reset() {
        history.clear()
        state = FavoritePicker.start(items, random)
    }

    private fun push() {
        history.add(state)
        if (history.size > MAX_UNDO) {
            history.removeAt(0)
        }
    }

    private companion object {
        const val MAX_UNDO = 20
    }
}

/**
 * The favorite Pokémon card. Tap the ones you like best, then PICK them, or PASS to keep the whole batch in play.
 * The last one standing becomes your favorite, and the list of favorites grows from there.
 */
@Composable
fun FavoritePokemonCard(session: FavoritePickerSession) {
    val state = session.state
    // A new batch clears the selection, so an old tap never carries over to the next batch.
    var selected by remember(state.evaluating) { mutableStateOf(emptySet<String>()) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DsText("Found ${state.favorites.size}  |  Still in play ${state.inPlay}", 13.sp, color = DsColors.Subtle)

        if (state.isFinished) {
            DsText("Everything is ordered. Your favorites are listed below.", 14.sp, color = DsColors.Subtle)
        } else {
            state.evaluating.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { name ->
                        DsChoice(
                            label = name,
                            selected = name in selected,
                            modifier = Modifier.weight(1f),
                        ) {
                            selected = if (name in selected) selected - name else selected + name
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DsActionButton(
                    label = "PICK",
                    modifier = Modifier.weight(1f),
                    fill = DsColors.TopDark,
                    textColor = Color.White,
                    enabled = selected.isNotEmpty(),
                ) { session.pick(selected) }
                DsActionButton(label = "PASS", modifier = Modifier.weight(1f)) { session.pass() }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DsTag("UNDO", enabled = session.canUndo) { session.undo() }
            DsTag("RESET") { session.reset() }
        }

        if (state.favorites.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.favorites.forEachIndexed { index, name ->
                    DsText("${index + 1}. $name", 14.sp)
                }
            }
        }
    }
}
