package app.fasting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecycleTest {
    private val hour = Durations.HOUR_MS

    private fun scheduled() = Fast(id = 1, status = FastStatus.SCHEDULED, startAt = 10 * hour, durationMs = 16 * hour)

    @Test
    fun scheduledFastWaitsUntilItsStart() {
        val step = advance(scheduled(), now = 5 * hour)
        assertEquals(FastStatus.SCHEDULED, step.fast.status)
        assertFalse(step.announceStart)
    }

    @Test
    fun startAnnouncesOnlyOnce() {
        val started = advance(scheduled(), now = 10 * hour)
        assertEquals(FastStatus.ACTIVE, started.fast.status)
        assertTrue(started.announceStart)
        assertTrue(started.fast.startNotified)

        val later = advance(started.fast, now = 11 * hour)
        assertFalse(later.announceStart)
    }

    @Test
    fun activeFastCompletesAtItsTargetEvenIfLate() {
        val active = advance(scheduled(), now = 10 * hour).fast
        val step = advance(active, now = 40 * hour)

        assertEquals(FastStatus.COMPLETED, step.fast.status)
        assertEquals(26 * hour, step.fast.endedAt)
        assertTrue(step.announceEnd)
        assertTrue(step.fast.goalMet)
    }

    @Test
    fun missedStartAndEndAreBothAnnounced() {
        val step = advance(scheduled(), now = 40 * hour)
        assertTrue(step.announceStart)
        assertTrue(step.announceEnd)
        assertEquals(FastStatus.COMPLETED, step.fast.status)
    }

    @Test
    fun endingEarlyRecordsTheRealLengthWithoutAnEndAlert() {
        val active = Fast(id = 2, status = FastStatus.ACTIVE, startAt = 0L, durationMs = 16 * hour, startNotified = true)
        val done = finishEarly(active, now = 9 * hour)

        assertEquals(FastStatus.COMPLETED, done.status)
        assertEquals(9 * hour, done.actualMs)
        assertFalse(done.goalMet)
        assertTrue(done.endNotified)
    }

    @Test
    fun endingEarlyIsCappedAtTheTarget() {
        val active = Fast(id = 3, status = FastStatus.ACTIVE, startAt = 0L, durationMs = 16 * hour, startNotified = true)
        assertEquals(16 * hour, finishEarly(active, now = 30 * hour).endedAt)
    }

    @Test
    fun finishingAFastThatIsNotActiveChangesNothing() {
        assertEquals(scheduled(), finishEarly(scheduled(), now = 11 * hour))
    }

    @Test
    fun nextTransitionFollowsTheStatus() {
        val active = Fast(id = 4, status = FastStatus.ACTIVE, startAt = 10 * hour, durationMs = 16 * hour)
        val completed = active.copy(status = FastStatus.COMPLETED, endedAt = 26 * hour)

        assertEquals(10 * hour, nextTransitionAt(scheduled()))
        assertEquals(26 * hour, nextTransitionAt(active))
        assertNull(nextTransitionAt(completed))
        assertNull(nextTransitionAt(null))
    }
}
