package app.fasting.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Stats(
    val totalFasts: Int,
    val averageMs: Long,
    val longestMs: Long,
    val streakDays: Int,
    val cravingsResisted: Int,
)

/**
 * Summary of finished fasts. A streak day is a local calendar day that holds at least one fast which
 * reached its target. Until midnight the streak is still alive, so when today has no such fast yet the
 * count starts from yesterday.
 */
fun computeStats(fasts: List<Fast>, now: Long, zone: ZoneId): Stats {
    val finished = fasts.filter { it.status == FastStatus.COMPLETED }
    val lengths = finished.map { it.actualMs }
    val goalDays = finished
        .filter { it.goalMet }
        .mapNotNull { fast -> fast.endedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } }
        .toSet()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    return Stats(
        totalFasts = finished.size,
        averageMs = if (lengths.isEmpty()) 0L else lengths.sum() / lengths.size,
        longestMs = lengths.maxOrNull() ?: 0L,
        streakDays = streak(goalDays, today),
        cravingsResisted = fasts.sumOf { it.cravingsResisted },
    )
}

private fun streak(goalDays: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in goalDays) today else today.minusDays(1)
    var count = 0
    while (day in goalDays) {
        count++
        day = day.minusDays(1)
    }
    return count
}
