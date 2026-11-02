package com.vibelang.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF8E4585),
    tertiaryContainer = Color(0xFFFFD7F1),
    onTertiaryContainer = Color(0xFF3A0035),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFF8F9FC),
    surfaceContainerLow = Color(0xFFF2F3F8),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EF),
    surfaceContainerHighest = Color(0xFFE9EBF3),
    surfaceVariant = Color(0xFFE2E1EC),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF293CA0),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFC4C5DD),
    secondaryContainer = Color(0xFF434659),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFFFADE8),
    tertiaryContainer = Color(0xFF722C6B),
    onTertiaryContainer = Color(0xFFFFD7F1),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF2A2C34),
    surfaceVariant = Color(0xFF45464F),
    error = Color(0xFFFFB4AB),
)

/** Colours Material 3 doesn't provide: used for "Got it" in Study and correct answers in Practice. */
@Immutable
data class ExtraColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(Color(0xFF2E7D32), Color.White, Color(0xFFC8E6C9), Color(0xFF0B2E0F))
}

@Composable
fun VibeLangTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extra = if (darkTheme) {
        ExtraColors(Color(0xFF81C784), Color(0xFF00390A), Color(0xFF1B5E20), Color(0xFFC8E6C9))
    } else {
        ExtraColors(Color(0xFF2E7D32), Color.White, Color(0xFFC8E6C9), Color(0xFF0B2E0F))
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
    }
}
