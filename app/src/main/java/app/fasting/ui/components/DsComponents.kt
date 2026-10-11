package app.fasting.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.ui.theme.DsColors
import app.fasting.ui.theme.DsFont
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/** Full-screen layout: grid paper, top status bar, prompt panel, content, and a bottom button bar. */
@Composable
fun DsScreen(
    title: String,
    prompt: String,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().dsGrid()) {
        DsTopBar(title)
        DsPrompt(prompt)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(DsColors.Bar)
                .navigationBarsPadding()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = bottom,
        )
    }
}

/** Pictoclip's grid paper: 16dp cells with hairline lines on the background colour. */
fun Modifier.dsGrid(): Modifier = drawBehind {
    drawRect(DsColors.Bg)
    val step = 16.dp.toPx()
    val line = (1.dp.toPx() / 2f).coerceAtLeast(1f)
    var x = 0f
    while (x < size.width) {
        drawRect(DsColors.Grid, Offset(x, 0f), Size(line, size.height))
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawRect(DsColors.Grid, Offset(0f, y), Size(size.width, line))
        y += step
    }
}

/** Gradient status bar with a title on the left and the clock on the right. */
@Composable
fun DsTopBar(title: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DsColors.TopLight, DsColors.TopDark)))
            .statusBarsPadding()
            .height(32.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DsText(title, 14.sp, color = Color.White, modifier = Modifier.weight(1f))
        DsClock(color = Color.White)
    }
}

/** Clock in the top bar, in the same "HH:mm  MM/dd" form as pictoclip. */
@Composable
fun DsClock(color: Color = DsColors.Text) {
    val text by produceState(initialValue = clockText()) {
        while (true) {
            value = clockText()
            delay(10_000)
        }
    }
    DsText(text, 14.sp, color = color)
}

private fun clockText(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm  MM/dd"))

/** The white prompt panel under the top bar, used for hints and short flash messages. */
@Composable
fun DsPrompt(text: String) {
    DsPanel(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 4.dp),
        padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        DsText(text, 15.sp)
    }
}

/**
 * A white panel with the DS notched border and an inner bottom/right shadow. Pressing it swaps the
 * fill for the pressed blue and drops the shadow. [selected] adds pictoclip's blue corner brackets.
 */
@Composable
fun DsPanel(
    modifier: Modifier = Modifier,
    fill: Color = DsColors.White,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val border = with(LocalDensity.current) { 2.dp.toPx() }
    val tap = if (onClick != null && enabled) {
        Modifier.clickable(
            interactionSource = source,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .then(tap)
            .drawBehind { drawPanel(if (pressed) DsColors.Pressed else fill, pressed, border) }
            .dsBrackets(selected)
            .padding(padding),
        content = content,
    )
}

private fun DrawScope.drawPanel(fill: Color, pressed: Boolean, px: Float) {
    drawPath(notchedPath(size.width, size.height, px), DsColors.Border)
    val left = px
    val top = px
    val right = size.width - px
    val bottom = size.height - px
    if (pressed) {
        drawRect(fill, Offset(left, top), Size(right - left, bottom - top))
        return
    }
    drawRect(DsColors.Shadow, Offset(left, top), Size(right - left, bottom - top))
    drawRect(fill, Offset(left, top), Size(right - left - px, bottom - top - px))
}

/** A rectangle with each corner cut diagonally by [c], as in pictoclip's PanelDrawable. */
private fun notchedPath(w: Float, h: Float, c: Float) = Path().apply {
    moveTo(c, 0f)
    lineTo(w - c, 0f)
    lineTo(w, c)
    lineTo(w, h - c)
    lineTo(w - c, h)
    lineTo(c, h)
    lineTo(0f, h - c)
    lineTo(0f, c)
    close()
}

/** Blue corner brackets drawn over the content, the way pictoclip marks the selected clip. */
private fun Modifier.dsBrackets(enabled: Boolean): Modifier {
    if (!enabled) return this
    return drawWithContent {
        drawContent()
        val stroke = 3.dp.toPx()
        val inset = 3.dp.toPx() + stroke / 2
        val l = inset
        val t = inset - 1.dp.toPx()
        val r = size.width - inset
        val b = size.height - inset + 1.dp.toPx()
        val arm = 14.dp.toPx()
        val path = Path().apply {
            moveTo(l, t + arm); lineTo(l, t); lineTo(l + arm, t)
            moveTo(r - arm, t); lineTo(r, t); lineTo(r, t + arm)
            moveTo(r, b - arm); lineTo(r, b); lineTo(r - arm, b)
            moveTo(l + arm, b); lineTo(l, b); lineTo(l, b - arm)
        }
        drawPath(path, DsColors.Cursor, style = Stroke(width = stroke))
    }
}

/** Text in the DS pixel font. */
@Composable
fun DsText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = DsColors.Text,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = DsFont,
        fontSize = size,
        textAlign = textAlign,
    )
}

/** Action button with a centred label, for the main actions. */
@Composable
fun DsActionButton(
    label: String,
    modifier: Modifier = Modifier,
    fill: Color = DsColors.White,
    textColor: Color = DsColors.Text,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DsPanel(
        modifier.heightIn(min = 52.dp),
        fill = fill,
        enabled = enabled,
        onClick = onClick,
        padding = PaddingValues(12.dp),
    ) {
        DsText(
            label,
            17.sp,
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** Bottom-bar button: a lettered ring (A, B, X, Y) followed by a label, as on the DS. */
@Composable
fun DsLetterButton(
    letter: String,
    label: String,
    modifier: Modifier = Modifier,
    fill: Color = DsColors.White,
    textColor: Color = DsColors.Text,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DsPanel(
        modifier.heightIn(min = 48.dp),
        fill = fill,
        enabled = enabled,
        onClick = onClick,
        padding = PaddingValues(0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .padding(start = 12.dp)
                    .size(22.dp)
                    .border(2.dp, textColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                DsText(letter, 13.sp, color = textColor)
            }
            DsText(
                label,
                16.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp, end = 12.dp),
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Row of letter buttons that share the bottom bar. */
@Composable
fun DsButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Choice tile for a row of options. The selected one gets the pressed fill and brackets. */
@Composable
fun DsChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    DsPanel(
        modifier.heightIn(min = 48.dp),
        fill = if (selected) DsColors.Pressed else DsColors.White,
        selected = selected,
        onClick = onClick,
        padding = PaddingValues(6.dp),
    ) {
        DsText(
            label,
            16.sp,
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            textAlign = TextAlign.Center,
        )
    }
}

/** Small pill-style button, for secondary actions such as NEXT. */
@Composable
fun DsTag(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    DsPanel(
        modifier,
        fill = DsColors.Bg,
        enabled = enabled,
        onClick = onClick,
        padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    ) {
        DsText(label, 13.sp)
    }
}

/** Pixel art on a coarse grid, the way pictoclip draws its page icon. '#' border, '.' white, '=' subtle. */
@Composable
fun DsPixelArt(rows: List<String>, unit: Dp, modifier: Modifier = Modifier) {
    val width = rows.maxOf { it.length }
    Canvas(modifier.size(unit * width, unit * rows.size)) {
        val u = unit.toPx()
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = when (ch) {
                    '#' -> DsColors.Border
                    '.' -> DsColors.White
                    '=' -> DsColors.Subtle
                    else -> return@forEachIndexed
                }
                drawRect(color, Offset(x * u, y * u), Size(u, u))
            }
        }
    }
}

/**
 * Progress ring in the DS style: a thick track, a blue progress arc starting at 12 o'clock,
 * and 60 pixel-like ticks around the outside.
 */
@Composable
fun DsRing(progress: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val radius = size.minDimension / 2f - 14.dp.toPx() - stroke / 2
            val center = Offset(size.width / 2f, size.height / 2f)

            for (i in 0 until 60) {
                val angle = Math.toRadians(i * 6.0 - 90.0)
                val major = i % 5 == 0
                val inner = radius + stroke / 2 + 4.dp.toPx()
                val outer = inner + (if (major) 8.dp else 4.dp).toPx()
                drawLine(
                    color = DsColors.Border,
                    start = Offset(center.x + (cos(angle) * inner).toFloat(), center.y + (sin(angle) * inner).toFloat()),
                    end = Offset(center.x + (cos(angle) * outer).toFloat(), center.y + (sin(angle) * outer).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                )
            }

            drawCircle(DsColors.Grid, radius = radius, center = center, style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    color = DsColors.Cursor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Butt),
                )
            }
        }
        content()
    }
}

/** Current time that updates every [periodMs], so countdowns stay live without a busy loop. */
@Composable
fun rememberNow(periodMs: Long): State<Long> = produceState(System.currentTimeMillis(), periodMs) {
    while (true) {
        value = System.currentTimeMillis()
        delay(periodMs)
    }
}
