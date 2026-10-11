package app.fasting.domain

/** The result of bringing a fast up to date with the clock, and which alerts are now due. */
data class Step(val fast: Fast, val announceStart: Boolean, val announceEnd: Boolean)

/**
 * Brings [fast] up to date with [now]. A scheduled fast whose start has passed becomes active. An active
 * fast whose target has passed completes at that target time, even if the app was closed in the meantime.
 * Each alert is announced once per fast, guarded by the notified flags.
 */
fun advance(fast: Fast, now: Long): Step {
    var current = fast
    var announceStart = false
    var announceEnd = false

    if (current.status == FastStatus.SCHEDULED && now >= current.startAt) {
        current = current.copy(status = FastStatus.ACTIVE)
        if (!current.startNotified) {
            announceStart = true
            current = current.copy(startNotified = true)
        }
    }
    if (current.status == FastStatus.ACTIVE && now >= current.targetEndAt) {
        current = current.copy(status = FastStatus.COMPLETED, endedAt = current.targetEndAt)
        if (!current.endNotified) {
            announceEnd = true
            current = current.copy(endNotified = true)
        }
    }
    return Step(current, announceStart, announceEnd)
}

/** Ends an active fast at [now], capped at its target time. A manual end never sends an end alert. */
fun finishEarly(fast: Fast, now: Long): Fast {
    if (fast.status != FastStatus.ACTIVE) return fast
    return fast.copy(
        status = FastStatus.COMPLETED,
        endedAt = minOf(now, fast.targetEndAt),
        endNotified = true,
    )
}

/** The next moment this fast needs attention, or null when nothing is pending. */
fun nextTransitionAt(fast: Fast?): Long? = when {
    fast == null -> null
    fast.status == FastStatus.SCHEDULED -> fast.startAt
    fast.status == FastStatus.ACTIVE -> fast.targetEndAt
    else -> null
}
