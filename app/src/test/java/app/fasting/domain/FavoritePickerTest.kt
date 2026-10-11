package app.fasting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FavoritePickerTest {
    private val names = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L")

    /** Every item is in exactly one place: in play, a favorite, or knocked out. */
    private fun assertAccountedFor(state: PickerState, total: Int) {
        assertEquals(total, state.inPlay + state.favorites.size + state.eliminated.size)
    }

    @Test
    fun batchSizeIsAFifthOfTheRoundKeptBetweenTwoAndFour() {
        assertEquals(2, FavoritePicker.batchSize(0))
        assertEquals(2, FavoritePicker.batchSize(6))
        assertEquals(3, FavoritePicker.batchSize(11))
        assertEquals(4, FavoritePicker.batchSize(151))
    }

    @Test
    fun startPutsTheFirstBatchOnScreenAndAccountsForEveryItem() {
        val state = FavoritePicker.start(names, Random(1))

        assertEquals(FavoritePicker.batchSize(names.size), state.batchSize)
        assertEquals(state.batchSize, state.evaluating.size)
        assertAccountedFor(state, names.size)
        assertEquals(names.toSet(), (state.current + state.evaluating).toSet())
    }

    @Test
    fun pickingKeepsTheChosenItemAndKnocksOutTheRest() {
        val start = FavoritePicker.start(names, Random(2))
        val winner = start.evaluating.first()

        val after = FavoritePicker.pick(start, setOf(winner), Random(3))

        assertTrue(winner in after.survived)
        assertEquals(start.evaluating.drop(1).toSet(), after.eliminated.map { it.id }.toSet())
        after.eliminated.forEach { assertEquals(listOf(winner), it.eliminatedBy) }
        assertAccountedFor(after, names.size)
    }

    @Test
    fun passingKeepsTheWholeBatchInPlay() {
        val start = FavoritePicker.start(names, Random(4))

        val after = FavoritePicker.pass(start, Random(5))

        assertTrue(after.eliminated.isEmpty())
        assertTrue(start.evaluating.all { it in after.survived })
        assertAccountedFor(after, names.size)
    }

    @Test
    fun aRunPickingTheFirstItemOfEachBatchFindsFavoritesAndAccountsForEveryItem() {
        val random = Random(7)
        var state = FavoritePicker.start(GEN1_POKEMON, random)
        var steps = 0

        while (!state.isFinished && steps < 5_000) {
            assertAccountedFor(state, GEN1_POKEMON.size)
            state = FavoritePicker.pick(state, setOf(state.evaluating.first()), random)
            steps++
        }

        assertTrue("the run did not finish", state.isFinished)
        assertAccountedFor(state, GEN1_POKEMON.size)
        assertTrue(state.favorites.isNotEmpty())
        assertEquals(state.favorites.size, state.favorites.toSet().size)
        assertFalse(state.favorites.any { favorite -> state.eliminated.any { it.id == favorite } })
    }

    @Test
    fun aRunWithRandomPicksFinishesAndAccountsForEveryItem() {
        val random = Random(11)
        var state = FavoritePicker.start(GEN1_POKEMON, random)
        var steps = 0

        while (!state.isFinished && steps < 5_000) {
            assertAccountedFor(state, GEN1_POKEMON.size)
            val picked = state.evaluating.filter { random.nextBoolean() }.ifEmpty { listOf(state.evaluating.first()) }
            state = FavoritePicker.pick(state, picked.toSet(), random)
            steps++
        }

        assertTrue("the run did not finish", state.isFinished)
        assertAccountedFor(state, GEN1_POKEMON.size)
        assertEquals(state.favorites.size, state.favorites.toSet().size)
    }
}
