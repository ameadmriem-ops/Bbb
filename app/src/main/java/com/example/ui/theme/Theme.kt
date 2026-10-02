package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AiPrimaryCyan,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F54),
    onPrimaryContainer = Color(0xFF70F5FF),
    secondary = AiSecondaryPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A148C),
    onSecondaryContainer = Color(0xFFE1BEE7),
    tertiary = AiTertiaryEmerald,
    onTertiary = Color(0xFF00382E),
    background = AiDarkBackground,
    onBackground = AiTextPrimaryDark,
    surface = AiDarkSurface,
    onSurface = AiTextPrimaryDark,
    surfaceVariant = AiDarkCard,
    onSurfaceVariant = AiTextSecondaryDark,
    outline = AiDarkBorder,
    error = AiError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007A8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7F4F7),
    onPrimaryContainer = Color(0xFF002024),
    secondary = Color(0xFF7B2CBF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9D8FD),
    onSecondaryContainer = Color(0xFF2E0854),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    background = AiLightBackground,
    onBackground = AiTextPrimaryLight,
    surface = AiLightSurface,
    onSurface = AiTextPrimaryLight,
    surfaceVariant = AiLightCard,
    onSurfaceVariant = AiTextSecondaryLight,
    outline = AiLightBorder,
    error = AiError,
    onError = Color.White
)

val LocalThemeIsDark = staticCompositionLocalOf { true }

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to Dark Mode as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
