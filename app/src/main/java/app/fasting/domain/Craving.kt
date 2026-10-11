package app.fasting.domain

import kotlin.math.PI
import kotlin.math.cos

/** The "I wanna eat" helper: a 10-minute countdown, a breathing guide, and rotating distractions. */
object Craving {
    const val COUNTDOWN_MS = 10 * Durations.MINUTE_MS
    const val CARD_ROTATE_MS = 150_000L
    const val CARD_COUNT = 9
    const val BREATH_CYCLE_MS = 8_000L

    /** The card after [card], wrapping back to the first one after the last. */
    fun nextCard(card: Int): Int = (card + 1) % CARD_COUNT

    /** Cards that wait for NEXT instead of moving on by themselves, so a game or fidget is never cut off mid-use. */
    val INTERACTIVE_CARDS: Set<Int> = setOf(2, 4, 5, 6, 7, 8)

    /** True when the timer should move on from [card] by itself. */
    fun autoAdvances(card: Int): Boolean = card !in INTERACTIVE_CARDS

    /** Size of the breathing circle for [phase] (0..1 of one cycle). Eases in and out, 0 to 1. */
    fun breathScale(phase: Float): Float = ((1 - cos(2 * PI * phase)) / 2).toFloat()

    /** True during the first half of a breathing cycle. */
    fun isInhale(phase: Float): Boolean = phase < 0.5f

    /** Encouragement that reflects how far into the fast the user already is. */
    fun motivation(elapsedMs: Long, targetMs: Long): String {
        val done = Durations.short(elapsedMs)
        val percent = Durations.percent(elapsedMs, targetMs)
        return when {
            percent >= 100 -> "$done in. Goal reached. Eating now still counts as a good fast."
            percent >= 50 -> "$done in and past halfway. Keep going."
            else -> "$done in. Each minute you hold makes the next one easier."
        }
    }
}
