package app.fasting.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationsTest {

    @Test
    fun parsesDecimalsWithDotOrComma() {
        assertEquals(14.5, Durations.parseHours("14.5")!!, 0.0001)
        assertEquals(14.5, Durations.parseHours(" 14,5 ")!!, 0.0001)
        assertEquals(240.0, Durations.parseHours("240")!!, 0.0)
    }

    @Test
    fun rejectsTextAndOutOfRangeValues() {
        assertNull(Durations.parseHours(""))
        assertNull(Durations.parseHours("abc"))
        assertNull(Durations.parseHours("NaN"))
        assertNull(Durations.parseHours("0"))
        assertNull(Durations.parseHours("-2"))
        assertNull(Durations.parseHours("241"))
        assertNull(Durations.parseHours("0.01"))
    }

    @Test
    fun labelsHours() {
        assertEquals("16h", Durations.hoursLabel(16.0))
        assertEquals("14.5h", Durations.hoursLabel(14.5))
    }

    @Test
    fun formatsClockAndShortDurations() {
        assertEquals("01:02:05", Durations.clock(3_725_000L))
        assertEquals("26:00:00", Durations.clock(26 * Durations.HOUR_MS))
        assertEquals("00:00:00", Durations.clock(-5L))
        assertEquals("16h 05m", Durations.short(16 * Durations.HOUR_MS + 5 * Durations.MINUTE_MS))
        assertEquals("45m", Durations.short(45 * Durations.MINUTE_MS))
    }

    @Test
    fun convertsHoursToMillis() {
        assertEquals(52_200_000L, Durations.hoursToMillis(14.5))
    }

    @Test
    fun progressIsClampedToZeroAndOne() {
        assertEquals(0f, Durations.progress(1000L, 1000L, 500L), 0f)
        assertEquals(0.5f, Durations.progress(0L, 1000L, 500L), 0.0001f)
        assertEquals(1f, Durations.progress(0L, 1000L, 5000L), 0f)
    }

    @Test
    fun percentIsCappedAtOneHundred() {
        assertEquals(50, Durations.percent(8 * Durations.HOUR_MS, 16 * Durations.HOUR_MS))
        assertEquals(100, Durations.percent(20 * Durations.HOUR_MS, 16 * Durations.HOUR_MS))
    }
}
