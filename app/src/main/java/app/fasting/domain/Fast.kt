package app.fasting.domain

/** Lifecycle of one fast. Only one fast can be scheduled or active at a time; finished fasts form the history. */
enum class FastStatus { SCHEDULED, ACTIVE, COMPLETED }

/** How a craving ended: still fasting, or gave in (which ends the fast). */
enum class CravingOutcome { RESISTED, GAVE_IN }

/**
 * One fast. [startAt] is the planned start while SCHEDULED and the real start afterwards.
 * [endedAt] is set once the fast is COMPLETED. The notified flags stop an alert from firing twice.
 */
data class Fast(
    val id: Long,
    val status: FastStatus,
    val startAt: Long,
    val durationMs: Long,
    val endedAt: Long? = null,
    val startNotified: Boolean = false,
    val endNotified: Boolean = false,
    val cravingsResisted: Int = 0,
) {
    /** The moment the target length is reached. */
    val targetEndAt: Long get() = startAt + durationMs

    /** Real length of the fast so far, or of the finished fast. */
    val actualMs: Long get() = ((endedAt ?: startAt) - startAt).coerceAtLeast(0L)

    /** True when a finished fast lasted at least its target length. */
    val goalMet: Boolean get() = endedAt != null && actualMs >= durationMs
}
