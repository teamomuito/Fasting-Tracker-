package app.fasting.data

import app.fasting.domain.CravingOutcome
import app.fasting.domain.Fast
import app.fasting.domain.FastStatus
import app.fasting.domain.advance
import app.fasting.domain.finishEarly
import app.fasting.domain.nextTransitionAt
import app.fasting.notify.FastAlarms
import app.fasting.notify.FastNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single source of truth for fasts. Every change runs under one lock and ends by re-syncing, so
 * alerts fire once and the alarm always points at the next transition.
 */
class FastRepository(
    private val dao: FastDao,
    private val alarms: FastAlarms,
    private val notifier: FastNotifier,
) {
    private val lock = Mutex()

    fun observeFasts(): Flow<List<Fast>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun exactAlarmsAllowed(): Boolean = alarms.canScheduleExact()

    /** Brings pending fasts up to date with the clock, sends due alerts, and re-arms the alarm. */
    suspend fun sync() {
        lock.withLock { syncLocked(now()) }
    }

    /**
     * Starts a fast at [startAt], or right away when that time has already passed. Returns false when a
     * fast is already scheduled or running.
     */
    suspend fun begin(startAt: Long, durationMs: Long): Boolean {
        return lock.withLock {
            val now = now()
            if (dao.pending().isNotEmpty()) return@withLock false
            val immediate = startAt <= now
            dao.insertFast(
                FastEntity(
                    status = if (immediate) FastStatus.ACTIVE else FastStatus.SCHEDULED,
                    startAt = if (immediate) now else startAt,
                    durationMs = durationMs,
                    endedAt = null,
                    startNotified = immediate,
                    endNotified = false,
                ),
            )
            syncLocked(now)
            true
        }
    }

    /** Ends the running fast now. */
    suspend fun endEarly(id: Long) {
        lock.withLock {
            val now = now()
            syncLocked(now)
            val fast = dao.byId(id)?.toDomain()?.takeIf { it.status == FastStatus.ACTIVE } ?: return
            dao.updateFast(finishEarly(fast, now).toEntity())
            syncLocked(now)
        }
    }

    /** Cancels a scheduled fast that has not started yet. */
    suspend fun cancelScheduled(id: Long) {
        lock.withLock {
            val fast = dao.byId(id)?.toDomain() ?: return
            if (fast.status != FastStatus.SCHEDULED) return
            dao.deleteFast(id)
            syncLocked(now())
        }
    }

    /**
     * Records a craving during the running fast. Resisting keeps the fast going; giving in also ends it.
     * Does nothing when no fast is running.
     */
    suspend fun logCraving(fastId: Long, outcome: CravingOutcome) {
        lock.withLock {
            val now = now()
            syncLocked(now)
            val fast = dao.byId(fastId)?.toDomain()?.takeIf { it.status == FastStatus.ACTIVE } ?: return
            dao.insertCraving(CravingEntity(fastId = fastId, createdAt = now, outcome = outcome))
            if (outcome == CravingOutcome.GAVE_IN) {
                dao.updateFast(finishEarly(fast, now).toEntity())
                syncLocked(now)
            }
        }
    }

    /** Removes a finished fast from the history. */
    suspend fun deleteFast(id: Long) {
        lock.withLock {
            dao.deleteFast(id)
            syncLocked(now())
        }
    }

    private suspend fun syncLocked(now: Long) {
        val updated = mutableListOf<Fast>()
        for (before in dao.pending().map { it.toDomain() }) {
            val step = advance(before, now)
            if (step.fast != before) dao.updateFast(step.fast.toEntity())
            if (step.announceStart) notifier.started(step.fast)
            if (step.announceEnd) notifier.ended(step.fast)
            updated += step.fast
        }
        val pending = updated.firstOrNull { it.status != FastStatus.COMPLETED }
        alarms.arm(nextTransitionAt(pending))
    }

    private fun now(): Long = System.currentTimeMillis()
}
