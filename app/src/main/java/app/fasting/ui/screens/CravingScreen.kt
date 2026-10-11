package app.fasting.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.domain.Craving
import app.fasting.domain.CravingOutcome
import app.fasting.domain.Durations
import app.fasting.domain.Fast
import app.fasting.ui.FastingViewModel
import app.fasting.ui.components.DsLetterButton
import app.fasting.ui.components.DsPanel
import app.fasting.ui.components.DsScreen
import app.fasting.ui.components.DsTag
import app.fasting.ui.components.DsText
import app.fasting.ui.components.rememberNow
import app.fasting.ui.theme.DsColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val GAME_MS = 15_000L
private const val BUBBLE_LIFE_MS = 2_500L

/**
 * "I wanna eat": a 10-minute countdown with a breathing circle and rotating distractions. The bottom
 * choices either log a resisted craving and go back to the timer, or end the fast.
 */
@Composable
fun CravingScreen(vm: FastingViewModel, fast: Fast) {
    val now by rememberNow(periodMs = 1_000L)
    val startedAt = rememberSaveable { System.currentTimeMillis() }
    val left = (Craving.COUNTDOWN_MS - (now - startedAt)).coerceAtLeast(0L)

    val pokemon = vm.pokemonPicker

    var card by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(card) {
        if (Craving.autoAdvances(card)) {
            delay(Craving.CARD_ROTATE_MS)
            card = Craving.nextCard(card)
        }
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
            DistractionCard(card = card, fast = fast, now = now, pokemon = pokemon) {
                card = Craving.nextCard(card)
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

/**
 * One of nine rotating cards: reminders, a game, progress, fidgets, and the favorite Pokémon picker. Cards advance
 * on their own too. The small fidgets start fresh when their card comes back around, but the Pokémon picker keeps its
 * picks because it is held by the screen.
 */
@Composable
private fun DistractionCard(card: Int, fast: Fast, now: Long, pokemon: FavoritePickerSession, onNext: () -> Unit) {
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
                3 -> {
                    val percent = Durations.percent(elapsed, fast.durationMs)
                    DsText("$percent% OF YOUR ${Durations.short(fast.durationMs)} GOAL".uppercase(), 15.sp)
                    DsText(Craving.motivation(elapsed, fast.durationMs), 14.sp, color = DsColors.Subtle)
                }
                4 -> {
                    DsText("POP THE BUBBLES", 15.sp)
                    DsText("Tap each bubble before it fades. Quick hands, quiet mind.", 14.sp, color = DsColors.Subtle)
                    BubblePop()
                }
                5 -> {
                    DsText("SQUEEZE THE BALL", 15.sp)
                    DsText("Press and hold, then let go slowly. Every squeeze counts.", 14.sp, color = DsColors.Subtle)
                    SqueezeBall()
                }
                6 -> {
                    DsText("DOODLE IT OUT", 15.sp)
                    DsText("Draw whatever comes to mind. Clear the pad when you're done.", 14.sp, color = DsColors.Subtle)
                    DoodlePad()
                }
                7 -> {
                    DsText("SPIN THE DIAL", 15.sp)
                    DsText("Drag around the dial in slow, steady circles.", 14.sp, color = DsColors.Subtle)
                    SpinDial()
                }
                else -> {
                    DsText("PICK YOUR FAVORITE POKÉMON", 15.sp)
                    DsText(
                        "Pick the ones you like best. Pass keeps a whole batch in play. The last one standing is your favorite.",
                        14.sp,
                        color = DsColors.Subtle,
                    )
                    FavoritePokemonCard(pokemon)
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

private class Bubble(val x: Float, val y: Float, val bornAt: Long)

/** Bubbles float up at random spots and fade after a few seconds. Tap one before it fades to pop it. */
@Composable
private fun BubblePop() {
    val bubbles = remember { mutableStateListOf<Bubble>() }
    var popped by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            val now = System.currentTimeMillis()
            bubbles.removeAll { now - it.bornAt > BUBBLE_LIFE_MS }
            bubbles += Bubble(Random.nextFloat() * 0.8f + 0.1f, Random.nextFloat() * 0.8f + 0.1f, now)
            if (bubbles.size > 8) bubbles.removeAt(0)
        }
    }

    val arena = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .background(Color.White)
        .border(2.dp, DsColors.Border)
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                val radius = 24.dp.toPx()
                val now = System.currentTimeMillis()
                // A bubble that has faded out is no longer there to tap, even before the loop prunes it.
                val hit = bubbles.lastOrNull { bubble ->
                    now - bubble.bornAt <= BUBBLE_LIFE_MS &&
                        (Offset(bubble.x * size.width, bubble.y * size.height) - offset).getDistance() <= radius
                }
                if (hit != null) {
                    bubbles.remove(hit)
                    popped++
                }
            }
        }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DsText("Popped: $popped", 14.sp, color = DsColors.Subtle)
        Canvas(arena) {
            val now = System.currentTimeMillis()
            val radius = 24.dp.toPx()
            bubbles.forEach { bubble ->
                val center = Offset(bubble.x * size.width, bubble.y * size.height)
                val alpha = 1f - ((now - bubble.bornAt).toFloat() / BUBBLE_LIFE_MS).coerceIn(0f, 1f)
                drawCircle(DsColors.Pressed, radius = radius, center = center, alpha = alpha)
                drawCircle(DsColors.Cursor, radius = radius, center = center, alpha = alpha, style = Stroke(2.dp.toPx()))
            }
        }
    }
}

/** A squishy ball: press and hold to squeeze it, and release to let it spring back. Each release counts. */
@Composable
private fun SqueezeBall() {
    var squeezes by rememberSaveable { mutableIntStateOf(0) }
    var squeezing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (squeezing) 0.72f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "squeeze",
    )

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            // The touch target is the ball itself, not the whole strip around it.
            Canvas(
                Modifier
                    .size(120.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { start ->
                                // Only a press on the ball itself counts; the square around it does not.
                                val centre = Offset(size.width / 2f, size.height / 2f)
                                val onBall = (start - centre).getDistance() <= size.width / 2f
                                if (onBall) squeezing = true
                                val released = tryAwaitRelease()
                                squeezing = false
                                if (released && onBall) squeezes++
                            },
                        )
                    }
                    .graphicsLayer { scaleX = scale; scaleY = scale },
            ) {
                drawCircle(DsColors.TopLight, radius = size.minDimension / 2f)
                drawCircle(DsColors.Border, radius = size.minDimension / 2f, style = Stroke(3.dp.toPx()))
            }
        }
        DsText("Squeezes: $squeezes  (press and hold)", 13.sp, color = DsColors.Subtle)
    }
}

/** A finger doodle pad. Draw freely; CLEAR wipes it. */
@Composable
private fun DoodlePad() {
    val finished = remember { mutableStateListOf<Path>() }
    val live = remember { mutableStateListOf<Offset>() }
    val pad = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .background(Color.White)
        .border(2.dp, DsColors.Border)
        .touchMoves(
            onDown = { start, _ ->
                live.clear()
                live.add(start)
            },
            onMove = { point, _ -> live.add(point) },
            onUp = {
                if (live.size > 1) finished.add(strokePath(live.toList()))
                live.clear()
            },
        )

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(pad) {
            val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            finished.forEach { drawPath(it, DsColors.Cursor, style = stroke) }
            if (live.size > 1) drawPath(strokePath(live.toList()), DsColors.Cursor, style = stroke)
        }
        DsTag("CLEAR") {
            finished.clear()
            live.clear()
        }
    }
}

/** A path through the given points, so a finished stroke is not walked point by point on every frame. */
private fun strokePath(points: List<Offset>): Path = Path().apply {
    points.firstOrNull()?.let { moveTo(it.x, it.y) }
    points.drop(1).forEach { lineTo(it.x, it.y) }
}

/**
 * Reports every move of one touch, from the moment it lands until it lifts or is cancelled. Unlike the drag
 * detectors, this does not wait for touch slop, so a stroke or an arc starts exactly where the finger first touched.
 */
private fun Modifier.touchMoves(
    onDown: (Offset, IntSize) -> Unit,
    onMove: (Offset, IntSize) -> Unit,
    onUp: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        onDown(down.position, size)
        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.positionChanged()) {
                onMove(change.position, size)
                change.consume()
            }
            pressed = change.pressed
        }
        onUp()
    }
}

/** The angle, in radians, of [point] around the centre of a box of [size]. */
private fun angleAround(point: Offset, size: IntSize): Float {
    val centre = Offset(size.width / 2f, size.height / 2f)
    return atan2(point.y - centre.y, point.x - centre.x)
}

/** A dial you turn by dragging around its centre. Shows the degrees turned and the full turns. */
@Composable
private fun SpinDial() {
    var degrees by rememberSaveable { mutableStateOf(0f) }
    val lastAngle = remember { FloatArray(1) }
    val dial = Modifier
        .size(160.dp)
        .touchMoves(
            onDown = { point, size -> lastAngle[0] = angleAround(point, size) },
            onMove = { point, size ->
                val angle = angleAround(point, size)
                var delta = angle - lastAngle[0]
                if (delta > PI.toFloat()) delta -= 2f * PI.toFloat()
                if (delta < -PI.toFloat()) delta += 2f * PI.toFloat()
                degrees += Math.toDegrees(delta.toDouble()).toFloat()
                lastAngle[0] = angle
            },
            onUp = {},
        )

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(dial) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 6.dp.toPx()
            drawCircle(DsColors.White, radius = radius, center = centre)
            drawCircle(DsColors.Border, radius = radius, center = centre, style = Stroke(3.dp.toPx()))
            val angle = Math.toRadians(degrees.toDouble() - 90.0)
            val tip = Offset(
                centre.x + (cos(angle) * radius * 0.8).toFloat(),
                centre.y + (sin(angle) * radius * 0.8).toFloat(),
            )
            drawLine(DsColors.Cursor, centre, tip, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(DsColors.Border, radius = 6.dp.toPx(), center = centre)
        }
        val total = degrees.toInt()
        val normalised = ((total % 360) + 360) % 360
        DsText("$normalised deg  |  ${total / 360} full turns", 13.sp, color = DsColors.Subtle)
    }
}
