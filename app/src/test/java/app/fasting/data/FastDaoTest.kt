package app.fasting.data

import androidx.room.Room
import app.fasting.domain.CravingOutcome
import app.fasting.domain.FastStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FastDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: FastDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.fastDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun countsOnlyResistedCravingsForEachFast() = runBlocking {
        val first = dao.insertFast(finished(startAt = 1_000L))
        val second = dao.insertFast(finished(startAt = 2_000_000L))
        dao.insertCraving(CravingEntity(fastId = first, createdAt = 1_500L, outcome = CravingOutcome.RESISTED))
        dao.insertCraving(CravingEntity(fastId = first, createdAt = 1_600L, outcome = CravingOutcome.RESISTED))
        dao.insertCraving(CravingEntity(fastId = first, createdAt = 1_700L, outcome = CravingOutcome.GAVE_IN))

        val rows = dao.observeAll().first().associateBy { it.id }
        assertEquals(2, rows.getValue(first).cravingsResisted)
        assertEquals(0, rows.getValue(second).cravingsResisted)
    }

    @Test
    fun deletingAFastRemovesItsCravings() = runBlocking {
        val id = dao.insertFast(finished(startAt = 1_000L))
        dao.insertCraving(CravingEntity(fastId = id, createdAt = 1_500L, outcome = CravingOutcome.RESISTED))

        dao.deleteFast(id)

        assertEquals(0, dao.cravingCount())
        assertEquals(0, dao.observeAll().first().size)
    }

    @Test
    fun pendingReturnsOnlyScheduledAndActiveFasts() = runBlocking {
        dao.insertFast(finished(startAt = 1_000L))
        val scheduled = dao.insertFast(
            finished(startAt = 9_000L).copy(status = FastStatus.SCHEDULED, endedAt = null),
        )
        val active = dao.insertFast(
            finished(startAt = 8_000L).copy(status = FastStatus.ACTIVE, endedAt = null),
        )

        assertEquals(setOf(scheduled, active), dao.pending().map { it.id }.toSet())
    }

    private fun finished(startAt: Long) = FastEntity(
        status = FastStatus.COMPLETED,
        startAt = startAt,
        durationMs = 3_600_000L,
        endedAt = startAt + 3_600_000L,
        startNotified = true,
        endNotified = true,
    )
}
