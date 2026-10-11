package app.fasting

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import app.fasting.data.AppDatabase
import app.fasting.data.CravingEntity
import app.fasting.data.FastDao
import app.fasting.data.FastEntity
import app.fasting.data.FastRepository
import app.fasting.domain.CravingOutcome
import app.fasting.domain.FastStatus
import app.fasting.notify.FastAlarms
import app.fasting.notify.FastNotifier
import app.fasting.ui.FastingRoot
import app.fasting.ui.FastingViewModel
import app.fasting.ui.Screen
import app.fasting.ui.theme.FastingTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders each screen with fixed data and writes it to build/outputs/screenshots/*.png.
 * CI uploads that folder as an artifact so the UI can be checked without installing the APK.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var dao: FastDao

    private val hour = 3_600_000L
    private val now = System.currentTimeMillis()

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
    fun timerIdle() = render("timer-idle", Screen.TIMER, expectedFasts = 0) { }

    @Test
    fun timerActive() = render("timer-active", Screen.TIMER, expectedFasts = 1) { d ->
        d.insertFast(
            FastEntity(
                status = FastStatus.ACTIVE,
                startAt = now - 6 * hour,
                durationMs = 16 * hour,
                endedAt = null,
                startNotified = true,
                endNotified = false,
            ),
        )
    }

    @Test
    fun timerScheduled() = render("timer-scheduled", Screen.TIMER, expectedFasts = 1) { d ->
        d.insertFast(
            FastEntity(
                status = FastStatus.SCHEDULED,
                startAt = now + 2 * hour,
                durationMs = 16 * hour,
                endedAt = null,
                startNotified = false,
                endNotified = false,
            ),
        )
    }

    @Test
    fun craving() = render("craving", Screen.CRAVING, expectedFasts = 1) { d ->
        d.insertFast(
            FastEntity(
                status = FastStatus.ACTIVE,
                startAt = now - 6 * hour,
                durationMs = 16 * hour,
                endedAt = null,
                startNotified = true,
                endNotified = false,
            ),
        )
    }

    @Test
    fun history() = render("history", Screen.HISTORY, expectedFasts = 4, seed = ::seedFinished)

    @Test
    fun stats() = render("stats", Screen.STATS, expectedFasts = 4, seed = ::seedFinished)

    private suspend fun seedFinished(d: FastDao) {
        val ids = listOf(
            d.insertFast(finished(daysAgo = 1, hours = 16.0)),
            d.insertFast(finished(daysAgo = 2, hours = 14.5)),
            d.insertFast(finished(daysAgo = 3, hours = 20.0)),
            d.insertFast(finished(daysAgo = 4, hours = 12.0)),
        )
        d.insertCraving(CravingEntity(fastId = ids[0], createdAt = now, outcome = CravingOutcome.RESISTED))
        d.insertCraving(CravingEntity(fastId = ids[0], createdAt = now, outcome = CravingOutcome.RESISTED))
        d.insertCraving(CravingEntity(fastId = ids[2], createdAt = now, outcome = CravingOutcome.RESISTED))
    }

    private fun finished(daysAgo: Int, hours: Double): FastEntity {
        val start = now - (daysAgo * 24L + 2L) * hour
        return FastEntity(
            status = FastStatus.COMPLETED,
            startAt = start,
            durationMs = 16 * hour,
            endedAt = start + (hours * hour).toLong(),
            startNotified = true,
            endNotified = true,
        )
    }

    private fun render(
        name: String,
        screen: Screen,
        expectedFasts: Int,
        seed: suspend (FastDao) -> Unit,
    ) {
        runBlocking { seed(dao) }
        val context = RuntimeEnvironment.getApplication()
        val vm = FastingViewModel(FastRepository(dao, FastAlarms(context), FastNotifier(context)))
        vm.go(screen)
        composeRule.setContent {
            FastingTheme { FastingRoot(vm) }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) { vm.fasts.value.size == expectedFasts }
        composeRule.waitForIdle()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/outputs/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
