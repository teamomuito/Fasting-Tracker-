package app.fasting.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The calendar days a fast touches, from the day it starts to the day it ends (both included). */
class FastSpan(val first: LocalDate, val last: LocalDate) {
    operator fun contains(day: LocalDate): Boolean = !day.isBefore(first) && !day.isAfter(last)

    /** Every month the span touches, oldest first. */
    val months: List<YearMonth>
        get() = generateSequence(YearMonth.from(first)) { it.plusMonths(1) }
            .takeWhile { it <= YearMonth.from(last) }
            .toList()

    /** Number of calendar days touched. */
    val days: Int get() = (last.toEpochDay() - first.toEpochDay()).toInt() + 1
}

fun fastSpan(startAt: Long, endAt: Long, zone: ZoneId = ZoneId.systemDefault()): FastSpan {
    val first = Instant.ofEpochMilli(startAt).atZone(zone).toLocalDate()
    val last = Instant.ofEpochMilli(maxOf(startAt, endAt)).atZone(zone).toLocalDate()
    return FastSpan(first, last)
}

/** Days of [month] as grid cells for a Monday-first week: null pads the cells before the 1st. */
fun monthCells(month: YearMonth): List<LocalDate?> {
    val lead = month.atDay(1).dayOfWeek.value - 1
    return List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
}
