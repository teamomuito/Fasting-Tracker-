package app.fasting.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FastCalendarTest {
    private val zone = ZoneId.of("UTC")

    private fun at(date: String, hour: Int) =
        LocalDate.parse(date).atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()

    @Test
    fun overnightFastTouchesTwoDays() {
        val span = fastSpan(at("2026-10-17", 20), at("2026-10-18", 12), zone)
        assertEquals(2, span.days)
        assertTrue(LocalDate.parse("2026-10-18") in span)
        assertFalse(LocalDate.parse("2026-10-19") in span)
    }

    @Test
    fun fastAcrossMonthEndListsBothMonths() {
        val span = fastSpan(at("2026-10-30", 20), at("2026-11-02", 8), zone)
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 11)), span.months)
    }

    @Test
    fun monthCellsPadToMonday() {
        // 1 Oct 2026 is a Thursday, so three blanks come first.
        val cells = monthCells(YearMonth.of(2026, 10))
        assertEquals(3, cells.takeWhile { it == null }.size)
        assertEquals(34, cells.size)
        assertNull(cells[0])
        assertEquals(LocalDate.of(2026, 10, 1), cells[3])
    }
}
