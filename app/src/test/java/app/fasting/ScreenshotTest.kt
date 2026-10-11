package app.fasting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.fasting.data.CravingEntity
import app.fasting.data.FastDao
import app.fasting.data.FastEntity
import app.fasting.data.FastRepository
import app.fasting.data.FastWithCravings
import app.fasting.domain.CravingOutcome
import app.fasting.domain.FastStatus
import app.fasting.notify.FastAlarms
import app.fasting.notify.FastNotifier
import app.fasting.ui.FastingRoot
import app.fasting.ui.FastingViewModel
import app.fasting.ui.Screen
import app.fasting.ui.theme.FastingTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
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
 * Renders each screen with fixed data and writes PNG files into the build/outputs/screenshots folder.
 * CI uploads that folder as an artifact so the UI can be checked without installing the APK.
 * Data comes from an in-memory fake DAO, so emissions happen on the test thread and need no database threads.
 * (Kotlin block comments nest, so this text must not contain a slash followed by an asterisk.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
// NATIVE rasterises through the Robolectric native runtime, which runs on Linux x86_64, macOS and Windows x86_64 only.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dao = FakeFastDao()

    private val hour = 3_600_000L
    private val now = System.currentTimeMillis()

    @Test
    fun timerIdle() = render("timer-idle", Screen.TIMER, expectedFasts = 0) { }

    @Test
    fun timerActive() = render("timer-active", Screen.TIMER, expectedFasts = 1) { d ->
        d.insertFast(active(startedHoursAgo = 6))
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
        d.insertFast(active(startedHoursAgo = 6))
    }

    @Test
    fun history() = render("history", Screen.HISTORY, expectedFasts = 4, seed = ::seedFinished)

    @Test
    fun stats() = render("stats", Screen.STATS, expectedFasts = 4, seed = ::seedFinished)

    private fun active(startedHoursAgo: Int) = FastEntity(
        status = FastStatus.ACTIVE,
        startAt = now - startedHoursAgo * hour,
        durationMs = 16 * hour,
        endedAt = null,
        startNotified = true,
        endNotified = false,
    )

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
        composeRule.waitForIdle()
        check(vm.fasts.value.size == expectedFasts) {
            "$name: expected $expectedFasts fasts, saw ${vm.fasts.value.size}"
        }

        // Draw the window's view tree into a bitmap with the platform canvas. This avoids Compose's
        // captureToImage path, which timed out under Robolectric.
        val view = composeRule.activity.window.decorView
        val width = view.resources.displayMetrics.widthPixels
        val height = view.resources.displayMetrics.heightPixels
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        composeRule.waitForIdle()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/outputs/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

/** In-memory stand-in for the Room DAO. Each change bumps [version], which re-emits [observeAll]. */
private class FakeFastDao : FastDao {
    private val fasts = mutableListOf<FastEntity>()
    private val cravings = mutableListOf<CravingEntity>()
    private val version = MutableStateFlow(0)
    private var nextId = 1L

    override fun observeAll(): Flow<List<FastWithCravings>> = version.map {
        fasts.sortedByDescending { it.startAt }.map { f ->
            FastWithCravings(
                id = f.id,
                status = f.status,
                startAt = f.startAt,
                durationMs = f.durationMs,
                endedAt = f.endedAt,
                startNotified = f.startNotified,
                endNotified = f.endNotified,
                cravingsResisted = cravings.count { it.fastId == f.id && it.outcome == CravingOutcome.RESISTED },
            )
        }
    }

    override suspend fun pending(): List<FastEntity> = fasts.filter { it.status != FastStatus.COMPLETED }

    override suspend fun byId(id: Long): FastEntity? = fasts.firstOrNull { it.id == id }

    override suspend fun insertFast(fast: FastEntity): Long {
        val id = nextId++
        fasts += fast.copy(id = id)
        version.value++
        return id
    }

    override suspend fun updateFast(fast: FastEntity) {
        fasts.replaceAll { if (it.id == fast.id) fast else it }
        version.value++
    }

    override suspend fun deleteFast(id: Long) {
        fasts.removeAll { it.id == id }
        cravings.removeAll { it.fastId == id }
        version.value++
    }

    override suspend fun insertCraving(craving: CravingEntity) {
        cravings += craving.copy(id = nextId++)
        version.value++
    }

    override suspend fun cravingCount(): Int = cravings.size
}
