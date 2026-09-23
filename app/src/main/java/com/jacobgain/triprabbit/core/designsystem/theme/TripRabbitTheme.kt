package com.jacobgain.triprabbit.core.designsystem.theme

import android.os.Build
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jacobgain.triprabbit.core.model.*

private val LightColors = lightColorScheme(
    primary = Color(0xFF236B53), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EEDD), onPrimaryContainer = Color(0xFF244C35),
    secondary = Color(0xFF626F65), secondaryContainer = Color(0xFFEBEFE8),
    onSecondaryContainer = Color(0xFF293D30),
    background = Color(0xFFF6F7F3), surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A2B25), onSurface = Color(0xFF1A2B25),
    onSurfaceVariant = Color(0xFF657169), surfaceVariant = Color(0xFFEEF1EA),
    surfaceContainer = Color(0xFFF0F2EC), surfaceContainerLow = Color(0xFFF3F5EF),
    surfaceContainerHigh = Color(0xFFE8ECE4), outline = Color(0xFF7A877C), outlineVariant = Color(0xFFE1E6DC),
    surfaceContainerHighest = Color(0xFFE0E6DA), surfaceContainerLowest = Color.White,
    surfaceTint = Color(0xFF236B53),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D5B1), onPrimary = Color(0xFF133A27),
    primaryContainer = Color(0xFF2C4434), onPrimaryContainer = Color(0xFFD1EBC7),
    secondary = Color(0xFFBAC7B9), secondaryContainer = Color(0xFF303C31),
    onSecondaryContainer = Color(0xFFDDE8D8),
    background = Color(0xFF111814), surface = Color(0xFF1A231D),
    onBackground = Color(0xFFEEF2E9), onSurface = Color(0xFFEEF2E9),
    onSurfaceVariant = Color(0xFFB0BDB0), surfaceVariant = Color(0xFF2B372D),
    surfaceContainer = Color(0xFF202C23), surfaceContainerLow = Color(0xFF222D24),
    surfaceContainerHigh = Color(0xFF303D32), outline = Color(0xFF7D8D7F), outlineVariant = Color(0xFF344238),
    surfaceContainerHighest = Color(0xFF3B483D), surfaceContainerLowest = Color(0xFF0C120F),
    surfaceTint = Color(0xFFA8D5B1),
)

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
    val context = LocalContext.current
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    var colors = if (settings.useDynamicColor && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else palette(settings.accentTheme, dark)
    if (dark && settings.useAmoledBlack) colors = colors.copy(background = Color.Black, surface = Color.Black)
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = Shapes(
        small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
    ), content = content)
}

private fun palette(accent: AccentTheme, dark: Boolean): ColorScheme {
    val base = if (dark) DarkColors else LightColors
    val (primary, container, onContainer) = when (accent) {
        AccentTheme.DEFAULT -> return base
        AccentTheme.BLUE -> if (dark) Triple(Color(0xFFA9C7FF), Color(0xFF273F62), Color(0xFFD8E7FF))
            else Triple(Color(0xFF315DA8), Color(0xFFDDE8FA), Color(0xFF173A6B))
        AccentTheme.GREEN -> if (dark) Triple(Color(0xFF9CD49E), Color(0xFF2A4930), Color(0xFFD3F0D0))
            else Triple(Color(0xFF386A3A), Color(0xFFE2F0DE), Color(0xFF284E2A))
        AccentTheme.ORANGE -> if (dark) Triple(Color(0xFFFFB77A), Color(0xFF5A3B1C), Color(0xFFFFE1C4))
            else Triple(Color(0xFF8A4F00), Color(0xFFFFE8C7), Color(0xFF603600))
        AccentTheme.RED -> if (dark) Triple(Color(0xFFFFB4AB), Color(0xFF5B302D), Color(0xFFFFDAD5))
            else Triple(Color(0xFF9C423B), Color(0xFFF8DFDB), Color(0xFF6D302A))
        AccentTheme.PURPLE -> if (dark) Triple(Color(0xFFD4BBFF), Color(0xFF443059), Color(0xFFEBDDFF))
            else Triple(Color(0xFF675085), Color(0xFFEBE2F7), Color(0xFF48335F))
        AccentTheme.MONOCHROME -> if (dark) Triple(Color.White, Color(0xFF343434), Color.White)
            else Triple(Color(0xFF333333), Color(0xFFE5E5E5), Color(0xFF242424))
    }
    return base.copy(primary = primary, primaryContainer = container, onPrimaryContainer = onContainer, surfaceTint = primary)
}
