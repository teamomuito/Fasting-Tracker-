package app.fasting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CravingTest {
    private val hour = Durations.HOUR_MS

    @Test
    fun breathingCircleGrowsThenShrinks() {
        assertEquals(0f, Craving.breathScale(0f), 0.0001f)
        assertEquals(1f, Craving.breathScale(0.5f), 0.0001f)
        assertEquals(0f, Craving.breathScale(1f), 0.0001f)
        assertTrue(Craving.isInhale(0.25f))
        assertFalse(Craving.isInhale(0.75f))
    }

    @Test
    fun countdownIsTenMinutes() {
        assertEquals(600_000L, Craving.COUNTDOWN_MS)
    }

    @Test
    fun interactiveCardsWaitForNextWhileTheRestRotate() {
        assertTrue(Craving.autoAdvances(0))
        assertTrue(Craving.autoAdvances(1))
        assertTrue(Craving.autoAdvances(3))
        assertFalse(Craving.autoAdvances(2))
        assertFalse(Craving.autoAdvances(8))
    }

    @Test
    fun cardsRotateThroughAllOfThemAndWrap() {
        assertEquals(9, Craving.CARD_COUNT)
        var card = 0
        val seen = mutableSetOf<Int>()
        repeat(Craving.CARD_COUNT) {
            seen += card
            card = Craving.nextCard(card)
        }
        assertEquals((0 until Craving.CARD_COUNT).toSet(), seen)
        assertEquals(0, card)
    }

    @Test
    fun motivationReflectsProgress() {
        assertTrue(Craving.motivation(4 * hour, 16 * hour).startsWith("4h 00m in."))
        assertTrue(Craving.motivation(10 * hour, 16 * hour).contains("past halfway"))
        assertTrue(Craving.motivation(17 * hour, 16 * hour).contains("Goal reached"))
    }
}
