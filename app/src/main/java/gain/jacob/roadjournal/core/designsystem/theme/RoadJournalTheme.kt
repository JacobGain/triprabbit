package gain.jacob.roadjournal.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import gain.jacob.roadjournal.core.model.*

private val LightColors = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF315F49), secondary = androidx.compose.ui.graphics.Color(0xFF506352))
private val DarkColors = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF9BD2AF), secondary = androidx.compose.ui.graphics.Color(0xFFB5CCB8))

@Composable
fun RoadJournalTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    val context = LocalContext.current
    var colors = if (settings.useDynamicColor && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else palette(settings.accentTheme, dark)
    if (dark && settings.useAmoledBlack) colors = colors.copy(background = Color.Black, surface = Color.Black)
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}

private fun palette(accent: AccentTheme, dark: Boolean): ColorScheme {
    val primary = when (accent) {
        AccentTheme.DEFAULT -> if (dark) Color(0xFF9BD2AF) else Color(0xFF315F49)
        AccentTheme.BLUE -> if (dark) Color(0xFFA9C7FF) else Color(0xFF315DA8)
        AccentTheme.GREEN -> if (dark) Color(0xFF9CD49E) else Color(0xFF386A3A)
        AccentTheme.ORANGE -> if (dark) Color(0xFFFFB77A) else Color(0xFF8A4F00)
        AccentTheme.RED -> if (dark) Color(0xFFFFB4AB) else Color(0xFF9C423B)
        AccentTheme.PURPLE -> if (dark) Color(0xFFD4BBFF) else Color(0xFF675085)
        AccentTheme.MONOCHROME -> if (dark) Color.White else Color(0xFF333333)
    }
    return if (dark) darkColorScheme(primary = primary, secondary = primary) else lightColorScheme(primary = primary, secondary = primary)
}
