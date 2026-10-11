package app.fasting.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** "MM/dd HH:mm" in the device time zone, like the clock in the top bar. */
fun formatDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofPattern("MM/dd HH:mm").format(Instant.ofEpochMilli(epochMillis).atZone(zone))
