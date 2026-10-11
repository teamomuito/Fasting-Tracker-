package app.fasting.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.fasting.R

/** Colours copied from pictoclip's `Ds.kt`, so the two apps share one look. */
object DsColors {
    val Bg = Color(0xFFF4F5F6)
    val Grid = Color(0xFFE1E4E7)
    val Border = Color(0xFF4A4F55)
    val Shadow = Color(0xFFC3C8CD)
    val Text = Color(0xFF3A3F45)
    val Subtle = Color(0xFF8A9096)
    val Pressed = Color(0xFFD5E2EE)
    val Cursor = Color(0xFF3E6DB0)
    val TopLight = Color(0xFF6F8C99)
    val TopDark = Color(0xFF4A6573)
    val Bar = Color(0xFFE3E6E9)
    val White = Color(0xFFFFFFFF)
}

/** DotGothic16, the pixel font pictoclip uses for all of its text. */
val DsFont = FontFamily(Font(R.font.dotgothic16))

@Composable
fun FastingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = DsColors.TopDark,
            onPrimary = DsColors.White,
            secondary = DsColors.Cursor,
            onSecondary = DsColors.White,
            background = DsColors.Bg,
            onBackground = DsColors.Text,
            surface = DsColors.White,
            onSurface = DsColors.Text,
            surfaceVariant = DsColors.Bar,
            onSurfaceVariant = DsColors.Subtle,
            outline = DsColors.Border,
        ),
        typography = Typography().withFont(DsFont),
        // The DS look has square corners everywhere.
        shapes = Shapes(
            extraSmall = RoundedCornerShape(0.dp),
            small = RoundedCornerShape(0.dp),
            medium = RoundedCornerShape(0.dp),
            large = RoundedCornerShape(0.dp),
            extraLarge = RoundedCornerShape(0.dp),
        ),
        content = content,
    )
}

private fun Typography.withFont(font: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = font),
    displayMedium = displayMedium.copy(fontFamily = font),
    displaySmall = displaySmall.copy(fontFamily = font),
    headlineLarge = headlineLarge.copy(fontFamily = font),
    headlineMedium = headlineMedium.copy(fontFamily = font),
    headlineSmall = headlineSmall.copy(fontFamily = font),
    titleLarge = titleLarge.copy(fontFamily = font),
    titleMedium = titleMedium.copy(fontFamily = font),
    titleSmall = titleSmall.copy(fontFamily = font),
    bodyLarge = bodyLarge.copy(fontFamily = font),
    bodyMedium = bodyMedium.copy(fontFamily = font),
    bodySmall = bodySmall.copy(fontFamily = font),
    labelLarge = labelLarge.copy(fontFamily = font),
    labelMedium = labelMedium.copy(fontFamily = font),
    labelSmall = labelSmall.copy(fontFamily = font),
)
