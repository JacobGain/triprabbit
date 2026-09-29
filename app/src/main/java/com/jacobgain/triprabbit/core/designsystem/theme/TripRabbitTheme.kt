package com.jacobgain.triprabbit.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jacobgain.triprabbit.core.model.*

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B58), onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EEE9), onPrimaryContainer = Color(0xFF003E33),
    secondary = Color(0xFF52656D), secondaryContainer = Color(0xFFE0EBF0),
    onSecondaryContainer = Color(0xFF243F4A),
    background = Color(0xFFF2F6F7), surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF142E38), onSurface = Color(0xFF142E38),
    onSurfaceVariant = Color(0xFF526770), surfaceVariant = Color(0xFFE6EFF1),
    surfaceContainer = Color(0xFFEAF1F3), surfaceContainerLow = Color(0xFFEDF4F5),
    surfaceContainerHigh = Color(0xFFDFEAED), outline = Color(0xFF71878E), outlineVariant = Color(0xFFD3E1E5),
    surfaceContainerHighest = Color(0xFFD4E3E7), surfaceContainerLowest = Color.White,
    surfaceTint = Color(0xFF006B58),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF00A080), onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF154C42), onPrimaryContainer = Color(0xFFB8F5DF),
    secondary = Color(0xFFB2CAD4), secondaryContainer = Color(0xFF253E49),
    onSecondaryContainer = Color(0xFFD6EAF2),
    background = Color(0xFF0C1920), surface = Color(0xFF142832),
    onBackground = Color(0xFFE8F2F5), onSurface = Color(0xFFE8F2F5),
    onSurfaceVariant = Color(0xFFADC3CD), surfaceVariant = Color(0xFF293F49),
    surfaceContainer = Color(0xFF1A303A), surfaceContainerLow = Color(0xFF182F39),
    surfaceContainerHigh = Color(0xFF263F4A), outline = Color(0xFF7F9BA8), outlineVariant = Color(0xFF304B57),
    surfaceContainerHighest = Color(0xFF35505D), surfaceContainerLowest = Color(0xFF081319),
    surfaceTint = Color(0xFF00A080),
)

private fun retint(color: Color, accent: Color): Color {
    val sourceHsv = FloatArray(3)
    val accentHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), sourceHsv)
    android.graphics.Color.colorToHSV(accent.toArgb(), accentHsv)
    sourceHsv[0] = accentHsv[0]
    sourceHsv[1] *= accentHsv[1]
    return Color(android.graphics.Color.HSVToColor(sourceHsv))
}

private val AppTypography = Typography(
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 46.sp, lineHeight = 52.sp, letterSpacing = (-1.8).sp),
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-1.2).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-.8).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 34.sp, letterSpacing = (-.6).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-.4).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-.2).sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = .2.sp),
)

@Composable
fun TripRabbitTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val base = if (dark) DarkColors else LightColors
    val colors = settings.accentColor?.let { rgb ->
        val accent = Color(rgb or (0xFF shl 24))
        // Primary is also used for text on neutral surfaces, so constrain its contrast.
        var primary = accent
        val background = retint(base.background, accent)
        val surface = retint(base.surface, accent)
        fun contrast(a: Color, b: Color): Float = (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
        repeat(24) {
            if (minOf(contrast(primary, background), contrast(primary, surface)) < 4.5f)
                primary = lerp(primary, if (dark) Color.White else Color.Black, .08f)
        }
        val container = if (dark) lerp(primary, Color.Black, .72f) else retint(Color(0xFFE5EEE9), accent)
        val foreground = if (contrast(primary, Color.Black) > contrast(primary, Color.White)) Color.Black else Color.White
        base.copy(primary = primary, onPrimary = foreground, primaryContainer = container,
            onPrimaryContainer = if (dark) Color.White else Color.Black, surfaceTint = accent,
            secondary = primary, secondaryContainer = container, onSecondaryContainer = if (dark) Color.White else Color.Black,
            background = retint(base.background, accent), surface = retint(base.surface, accent),
            onBackground = retint(base.onBackground, accent), onSurface = retint(base.onSurface, accent),
            onSurfaceVariant = retint(base.onSurfaceVariant, accent), surfaceVariant = retint(base.surfaceVariant, accent),
            surfaceContainer = retint(base.surfaceContainer, accent), surfaceContainerLow = retint(base.surfaceContainerLow, accent),
            surfaceContainerHigh = retint(base.surfaceContainerHigh, accent), surfaceContainerHighest = retint(base.surfaceContainerHighest, accent),
            outline = retint(base.outline, accent), outlineVariant = retint(base.outlineVariant, accent))
    } ?: base
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = Shapes(
        small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp), extraLarge = RoundedCornerShape(20.dp),
    ), content = content)
}
