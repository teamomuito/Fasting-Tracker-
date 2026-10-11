package app.fasting.domain

import kotlin.math.roundToLong

/** Duration parsing and formatting. All lengths are kept in milliseconds. */
object Durations {
    const val MINUTE_MS = 60_000L
    const val HOUR_MS = 3_600_000L
    const val MAX_HOURS = 240.0

    fun hoursToMillis(hours: Double): Long = (hours * HOUR_MS).roundToLong()

    /** Accepts "14.5" or "14,5". Returns null unless the value is between one minute and [MAX_HOURS]. */
    fun parseHours(input: String): Double? {
        val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
        val minHours = MINUTE_MS.toDouble() / HOUR_MS
        return value.takeIf { it >= minHours && it <= MAX_HOURS }
    }

    /** 14.5 -> "14.5h", 16.0 -> "16h". */
    fun hoursLabel(hours: Double): String {
        val number = if (hours % 1.0 == 0.0) {
            hours.toLong().toString()
        } else {
            "%.2f".format(hours).trimEnd('0').trimEnd('.')
        }
        return "${number}h"
    }

    /** H:MM:SS, for example 03:05:09. Hours can go past 24. Negative values show as zero. */
    fun clock(ms: Long): String {
        val total = ms.coerceAtLeast(0L) / 1000
        return "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
    }

    /** "16h 05m", or "45m" under an hour. */
    fun short(ms: Long): String {
        val minutes = ms.coerceAtLeast(0L) / MINUTE_MS
        val hours = minutes / 60
        val rest = minutes % 60
        return if (hours > 0) "%dh %02dm".format(hours, rest) else "${rest}m"
    }

    /** The share of [durationMs] that has passed since [startAt], from 0 to 1. */
    fun progress(startAt: Long, durationMs: Long, now: Long): Float {
        if (durationMs <= 0L) return 1f
        return ((now - startAt).toDouble() / durationMs).coerceIn(0.0, 1.0).toFloat()
    }

    /** Whole percent of [targetMs] covered by [elapsedMs], capped at 100. */
    fun percent(elapsedMs: Long, targetMs: Long): Int {
        if (targetMs <= 0L) return 100
        return ((elapsedMs.coerceAtLeast(0L) * 100) / targetMs).coerceAtMost(100L).toInt()
    }
}
