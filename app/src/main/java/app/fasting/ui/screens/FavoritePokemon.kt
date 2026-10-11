package app.fasting.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.domain.FavoritePicker
import app.fasting.domain.PickerState
import app.fasting.domain.Pokemon
import app.fasting.ui.components.DsActionButton
import app.fasting.ui.components.DsPanel
import app.fasting.ui.components.DsTag
import app.fasting.ui.components.DsText
import app.fasting.ui.theme.DsColors
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Owns the favorite picker for the craving screen. The picker works on ids, which are the National Pokédex numbers
 * as strings, and [species] maps each id back to its Pokémon. [selected] holds the taps in the current batch. PICK,
 * PASS, UNDO and RESET all clear it, so a tap never carries over to a different batch.
 */
class FavoritePickerSession(species: List<Pokemon>) {
    private val random = Random.Default
    private val bySpecies = species.associateBy { it.dex.toString() }
    private val ids = species.map { it.dex.toString() }
    private val history = mutableStateListOf<PickerState>()

    var state by mutableStateOf(FavoritePicker.start(ids, random))
        private set

    var selected by mutableStateOf(emptySet<String>())
        private set

    val canUndo: Boolean get() = history.isNotEmpty()

    fun species(id: String): Pokemon = bySpecies.getValue(id)

    fun toggle(id: String) {
        selected = if (id in selected) selected - id else selected + id
    }

    /** Keeps the selected Pokémon and knocks out the rest of the batch. */
    fun pick() {
        val picked = selected
        push()
        selected = emptySet()
        state = FavoritePicker.pick(state, picked, random)
    }

    /** Keeps the whole batch in play. */
    fun pass() {
        push()
        selected = emptySet()
        state = FavoritePicker.pass(state, random)
    }

    fun undo() {
        if (history.isNotEmpty()) {
            state = history.removeAt(history.lastIndex)
            selected = emptySet()
        }
    }

    fun reset() {
        history.clear()
        selected = emptySet()
        state = FavoritePicker.start(ids, random)
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
 * PC sprites from the app's assets. The PNG bytes are kept after the first read, since they are small, and each tile
 * decodes its own bitmap. Nothing is evicted, so a long list of favorites never re-reads or thrashes a cache.
 */
private object PokemonSprites {
    private val pngs = ConcurrentHashMap<Int, ByteArray>()

    fun decode(context: Context, dex: Int): ImageBitmap? = runCatching {
        val png = pngs.getOrPut(dex) { context.assets.open("pokemon/pc/$dex.png").use { it.readBytes() } }
        BitmapFactory.decodeByteArray(png, 0, png.size)?.asImageBitmap()
    }.getOrNull()
}

/** A decoded sprite and the Pokédex number it belongs to. */
private class LoadedSprite(val dex: Int, val bitmap: ImageBitmap)

/**
 * A Pokémon's PC sprite inside a box of [size]. The box keeps its size while the art loads, so labels do not jump.
 * Pixel art is drawn unsmoothed so it stays crisp when scaled.
 */
@Composable
private fun PokemonSprite(pokemon: Pokemon, size: Dp) {
    val context = LocalContext.current
    val loaded by produceState<LoadedSprite?>(initialValue = null, pokemon.dex) {
        val bitmap = withContext(Dispatchers.IO) { PokemonSprites.decode(context, pokemon.dex) }
        value = bitmap?.let { LoadedSprite(pokemon.dex, it) }
    }
    Box(Modifier.size(size)) {
        // The state keeps the previous Pokémon's sprite until this one loads, so draw it only when the dex matches.
        loaded?.takeIf { it.dex == pokemon.dex }?.let { sprite ->
            Image(
                bitmap = sprite.bitmap,
                contentDescription = pokemon.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
            )
        }
    }
}

@Composable
private fun PokemonTile(pokemon: Pokemon, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    DsPanel(
        modifier.heightIn(min = 48.dp),
        fill = if (selected) DsColors.Pressed else DsColors.White,
        selected = selected,
        onClick = onClick,
        padding = PaddingValues(6.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PokemonSprite(pokemon, 56.dp)
            DsText(pokemon.name, 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

/**
 * The favorite Pokémon card. Tap the ones you like best, then PICK them, or PASS to keep the whole batch in play.
 * The last one standing becomes your favorite, and the list of favorites grows from there.
 */
@Composable
fun FavoritePokemonCard(session: FavoritePickerSession) {
    val state = session.state

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DsText("Found ${state.favorites.size}  |  Still in play ${state.inPlay}", 13.sp, color = DsColors.Subtle)

        if (state.isFinished) {
            DsText("Everything is ordered. Your favorites are listed below.", 14.sp, color = DsColors.Subtle)
        } else {
            state.evaluating.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { id ->
                        PokemonTile(
                            pokemon = session.species(id),
                            selected = id in session.selected,
                            modifier = Modifier.weight(1f),
                        ) { session.toggle(id) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DsActionButton(
                    label = "PICK",
                    modifier = Modifier.weight(1f),
                    fill = DsColors.TopDark,
                    textColor = Color.White,
                    enabled = session.selected.isNotEmpty(),
                ) { session.pick() }
                DsActionButton(label = "PASS", modifier = Modifier.weight(1f)) { session.pass() }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DsTag("UNDO", enabled = session.canUndo) { session.undo() }
            DsTag("RESET") { session.reset() }
        }

        if (state.favorites.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.favorites.forEachIndexed { index, id ->
                    val pokemon = session.species(id)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PokemonSprite(pokemon, 28.dp)
                        Spacer(Modifier.width(8.dp))
                        DsText("${index + 1}. ${pokemon.name}", 14.sp)
                    }
                }
            }
        }
    }
}
