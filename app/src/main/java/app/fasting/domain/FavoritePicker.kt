package app.fasting.domain

import kotlin.random.Random

/**
 * Knockout favorite picker. This is a Kotlin port of the algorithm in antialiasis/favorite-picker, which is
 * MIT licensed (Copyright (c) 2018 Butterfree/Dragonfree/antialiasis; the notice is in
 * THIRD_PARTY/favorite-picker-LICENSE.txt).
 *
 * Items are shown in small batches. Picking some items keeps them in play and knocks out the rest, and each
 * knocked-out item remembers which picks beat it. When a round runs out, the survivors are shuffled back in.
 * A lone survivor with nothing left to show becomes the next favorite, and the items that favorite knocked out
 * return to play unless something else still holds them.
 */

/** An item knocked out by the picks in [eliminatedBy]. It returns to play once none of those picks is still in play. */
data class Elimination(val id: String, val eliminatedBy: List<String>)

/**
 * Where every item is at this moment. Each item sits in exactly one place: [current], [survived], [evaluating],
 * [eliminated], or [favorites].
 */
data class PickerState(
    val current: List<String> = emptyList(),
    val survived: List<String> = emptyList(),
    val eliminated: List<Elimination> = emptyList(),
    val evaluating: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val batchSize: Int = FavoritePicker.MIN_BATCH,
) {
    /** Items still in play this round or waiting for the next one. */
    val inPlay: Int get() = current.size + survived.size + evaluating.size

    /** True once nothing is left to show. */
    val isFinished: Boolean get() = evaluating.isEmpty() && current.isEmpty() && survived.isEmpty()
}

object FavoritePicker {
    const val MIN_BATCH = 2
    const val MAX_BATCH = 4

    /** How many items to show at once in a round of [roundSize] items: about a fifth of them, kept within the limits. */
    fun batchSize(roundSize: Int): Int = maxOf(MIN_BATCH, minOf(MAX_BATCH, (roundSize + 4) / 5))

    /** A fresh picker over [items], with the first batch already on screen. */
    fun start(items: List<String>, random: Random = Random.Default): PickerState =
        nextBatch(PickerState(current = items.shuffled(random), batchSize = batchSize(items.size)), random)

    /**
     * Picks the items in [picked] from the batch on screen. Everything on screen that is not picked is knocked out
     * by the picks. An empty set passes, which keeps the whole batch in play.
     */
    fun pick(state: PickerState, picked: Set<String>, random: Random = Random.Default): PickerState {
        val survived = state.survived.toMutableList()
        val eliminated = state.eliminated.toMutableList()
        val pickedList = picked.toList()
        for (item in state.evaluating) {
            if (picked.isEmpty() || item in picked) {
                survived.add(item)
            } else {
                eliminated.add(Elimination(item, pickedList))
            }
        }
        return nextBatch(
            state.copy(survived = survived, eliminated = eliminated, evaluating = emptyList()),
            random,
        )
    }

    /** Passes on the batch on screen, which keeps every item in it in play. */
    fun pass(state: PickerState, random: Random = Random.Default): PickerState = pick(state, emptySet(), random)

    private fun nextBatch(state: PickerState, random: Random): PickerState {
        if (state.current.size < state.batchSize && state.survived.isNotEmpty()) {
            return nextRound(state, random)
        }
        val take = minOf(state.batchSize, state.current.size)
        return state.copy(evaluating = state.current.take(take), current = state.current.drop(take))
    }

    private fun nextRound(state: PickerState, random: Random): PickerState {
        // A lone survivor with nothing left to show is the next favorite. Its knocked-out items may come back.
        if (state.current.isEmpty() && state.survived.size == 1) {
            val winner = state.survived.last()
            return nextRound(addToFavorites(state.copy(survived = state.survived.dropLast(1)), winner), random)
        }
        val current = state.current + state.survived.shuffled(random)
        return nextBatch(
            state.copy(current = current, survived = emptyList(), batchSize = batchSize(current.size)),
            random,
        )
    }

    private fun addToFavorites(state: PickerState, item: String): PickerState {
        val survived = state.survived.toMutableList()
        val eliminated = state.eliminated.toMutableList()
        removeFromEliminated(item, eliminated, survived)
        return state.copy(favorites = state.favorites + item, survived = survived, eliminated = eliminated)
    }

    /** Takes [item] out of every knock-out it caused. Any item left with no knock-outs goes back into [survived]. */
    private fun removeFromEliminated(item: String, eliminated: MutableList<Elimination>, survived: MutableList<String>) {
        for (i in eliminated.indices.reversed()) {
            val entry = eliminated[i]
            if (item in entry.eliminatedBy) {
                val remaining = entry.eliminatedBy.toMutableList().apply { remove(item) }
                if (remaining.isEmpty()) {
                    survived.add(entry.id)
                    eliminated.removeAt(i)
                } else {
                    eliminated[i] = entry.copy(eliminatedBy = remaining)
                }
            }
        }
    }
}
