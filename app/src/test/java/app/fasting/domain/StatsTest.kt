package app.fasting.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class StatsTest {
    private val zone = ZoneId.of("UTC")
    private val hour = Durations.HOUR_MS
    private val day = 24 * hour

    /** A finished fast that started at [startAt] and lasted [hours]. */
    private fun done(id: Long, startAt: Long, hours: Long, targetHours: Long = 16, cravings: Int = 0) = Fast(
        id = id,
        status = FastStatus.COMPLETED,
        startAt = startAt,
        durationMs = targetHours * hour,
        endedAt = startAt + hours * hour,
        startNotified = true,
        endNotified = true,
        cravingsResisted = cravings,
    )

    @Test
    fun summarisesFinishedFasts() {
        val fasts = listOf(
            done(1, 0, 16),
            done(2, 2 * day, 12, cravings = 3),
            done(3, 4 * day, 20, cravings = 1),
        )
        val stats = computeStats(fasts, now = 5 * day, zone = zone)

        assertEquals(3, stats.totalFasts)
        assertEquals(16 * hour, stats.averageMs)
        assertEquals(20 * hour, stats.longestMs)
        assertEquals(4, stats.cravingsResisted)
    }

    @Test
    fun emptyHistoryIsAllZero() {
        val stats = computeStats(emptyList(), now = 5 * day, zone = zone)
        assertEquals(Stats(0, 0L, 0L, 0, 0), stats)
    }

    @Test
    fun streakCountsConsecutiveGoalDays() {
        val fasts = listOf(
            done(1, 0, 16),           // 1970-01-01, goal met
            done(2, day, 16),         // 1970-01-02, goal met
            done(3, 2 * day, 16),     // 1970-01-03, goal met
        )
        // Today (1970-01-03) already has a goal day, so the streak includes it.
        assertEquals(3, computeStats(fasts, now = 2 * day + 17 * hour, zone = zone).streakDays)
        // Today (1970-01-04) has none yet, so the streak still counts from yesterday.
        assertEquals(3, computeStats(fasts, now = 3 * day + hour, zone = zone).streakDays)
    }

    @Test
    fun streakBreaksOnAMissingDayAndOnShortFasts() {
        val gapped = listOf(done(1, 0, 16), done(3, 2 * day, 16))
        assertEquals(1, computeStats(gapped, now = 2 * day + 17 * hour, zone = zone).streakDays)

        val short = listOf(done(1, 0, 12))
        assertEquals(0, computeStats(short, now = day, zone = zone).streakDays)
    }
}
