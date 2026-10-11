package app.fasting.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.domain.Craving
import app.fasting.domain.CravingOutcome
import app.fasting.domain.Durations
import app.fasting.domain.Fast
import app.fasting.ui.FastingViewModel
import app.fasting.ui.components.DsButtonRow
import app.fasting.ui.components.DsLetterButton
import app.fasting.ui.components.DsPanel
import app.fasting.ui.components.DsScreen
import app.fasting.ui.components.DsTag
import app.fasting.ui.components.DsText
import app.fasting.ui.components.rememberNow
import app.fasting.ui.theme.DsColors
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val GAME_MS = 15_000L

/**
 * "I wanna eat": a 10-minute countdown with a breathing circle and rotating distractions. The bottom
 * choices either log a resisted craving and go back to the timer, or end the fast.
 */
@Composable
fun CravingScreen(vm: FastingViewModel, fast: Fast) {
    val now by rememberNow(periodMs = 1_000L)
    val startedAt = rememberSaveable { System.currentTimeMillis() }
    val left = (Craving.COUNTDOWN_MS - (now - startedAt)).coerceAtLeast(0L)

    var card by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(card) {
        delay(Craving.CARD_ROTATE_MS)
        card = (card + 1) % Craving.CARD_COUNT
    }

    val breath = rememberInfiniteTransition(label = "breath")
    val phase by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = Craving.BREATH_CYCLE_MS.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    DsScreen(
        title = "CRAVING",
        prompt = if (left > 0L) "Hang on. ${Durations.clock(left)} to go." else "10 minutes done. Choose below.",
        bottom = {
            DsLetterButton(
                letter = "A",
                label = "I'M STILL FASTING",
                modifier = Modifier.fillMaxWidth(),
                fill = DsColors.TopDark,
                textColor = Color.White,
            ) { vm.logCraving(fast.id, CravingOutcome.RESISTED) }
            DsLetterButton(
                letter = "B",
                label = "I'M EATING",
                modifier = Modifier.fillMaxWidth(),
            ) { vm.logCraving(fast.id, CravingOutcome.GAVE_IN) }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BreathingCircle(phase = phase, modifier = Modifier.size(200.dp))
            DsText(Durations.clock(left), 26.sp)
            DistractionCard(card = card, fast = fast, now = now) {
                card = (card + 1) % Craving.CARD_COUNT
            }
        }
    }
}

@Composable
private fun BreathingCircle(phase: Float, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f - 4.dp.toPx()
            val radius = maxRadius * (0.45f + 0.55f * Craving.breathScale(phase))
            drawCircle(DsColors.Pressed, radius = radius)
            drawCircle(DsColors.Cursor, radius = radius, style = Stroke(3.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DsText("BREATHE", 11.sp, color = DsColors.Subtle)
            DsText(if (Craving.isInhale(phase)) "IN" else "OUT", 24.sp)
        }
    }
}

/** One of four rotating cards: water, a walk, a tap game, or progress. Cards advance on their own too. */
@Composable
private fun DistractionCard(card: Int, fast: Fast, now: Long, onNext: () -> Unit) {
    val elapsed = now - fast.startAt
    DsPanel(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (card) {
                0 -> {
                    DsText("DRINK A GLASS OF WATER", 15.sp)
                    DsText(
                        "Fill a glass now and sip it slowly. Cravings usually peak and then pass.",
                        14.sp,
                        color = DsColors.Subtle,
                    )
                }
                1 -> {
                    DsText("TAKE A 5-MINUTE WALK", 15.sp)
                    DsText("Step outside or pace the room. Movement quiets the urge.", 14.sp, color = DsColors.Subtle)
                }
                2 -> {
                    DsText("QUICK GAME: TAP THE LIT SQUARE", 15.sp)
                    TapGame()
                }
                else -> {
                    val percent = Durations.percent(elapsed, fast.durationMs)
                    DsText("$percent% OF YOUR ${Durations.short(fast.durationMs)} GOAL".uppercase(), 15.sp)
                    DsText(Craving.motivation(elapsed, fast.durationMs), 14.sp, color = DsColors.Subtle)
                }
            }
            DsTag("NEXT", modifier = Modifier.align(Alignment.End), onClick = onNext)
        }
    }
}

/** A 15-second reaction game on a 3x3 grid. One square lights up at a time; tap it to score. */
@Composable
private fun TapGame() {
    var round by rememberSaveable { mutableIntStateOf(0) }
    var lit by remember(round) { mutableIntStateOf(-1) }
    var score by remember(round) { mutableIntStateOf(0) }
    var over by remember(round) { mutableStateOf(false) }

    LaunchedEffect(round) {
        val end = System.currentTimeMillis() + GAME_MS
        while (System.currentTimeMillis() < end) {
            lit = Random.nextInt(9)
            delay(700)
        }
        lit = -1
        over = true
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DsText("Score: $score", 14.sp, color = DsColors.Subtle)
        if (over) {
            DsTag("PLAY AGAIN") { round++ }
        } else {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { col ->
                        val index = row * 3 + col
                        DsPanel(
                            Modifier.size(72.dp),
                            fill = if (index == lit) DsColors.Cursor else DsColors.White,
                            onClick = {
                                if (index == lit) {
                                    score++
                                    lit = -1
                                }
                            },
                        ) {}
                    }
                }
            }
        }
    }
}
