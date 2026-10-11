package id.gustyx.signalsonar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SonarPrimary,
    onPrimary = SonarOnPrimary,
    primaryContainer = SonarPrimaryContainer,
    onPrimaryContainer = SonarOnPrimaryContainer,
    secondary = SonarSecondary,
    onSecondary = SonarOnSecondary,
    secondaryContainer = SonarSecondaryContainer,
    onSecondaryContainer = SonarOnSecondaryContainer,
    tertiary = SonarTertiary,
    onTertiary = SonarOnTertiary,
    background = SonarBackground,
    onBackground = SonarTextPrimary,
    surface = SonarSurface,
    onSurface = SonarTextPrimary,
    surfaceVariant = SonarSurfaceVariant,
    onSurfaceVariant = SonarTextSecondary,
    error = SonarError,
    onError = SonarOnError
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008947),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F5C4),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF006874),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF97F0FF),
    onSecondaryContainer = Color(0xFF001F24),
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDEE5DD),
    onSurfaceVariant = Color(0xFF424943),
    error = SonarError,
    onError = Color.White
)

@Composable
fun SignalSonarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Material You dynamic color support on Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
