package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SoftLavender,
    secondary = SoftLilac,
    tertiary = WarmAccent,
    background = DeepCharcoalBg,
    surface = ElegantSurface,
    onPrimary = DeepGrape,
    onSecondary = DeepGrape,
    onBackground = SilverText,
    onSurface = SilverText,
    surfaceVariant = DarkBarBg,
    onSurfaceVariant = MutedSlate,
    outline = CharcoalOutline
)

private val LightColorScheme = darkColorScheme( // Force dark theme for cohesive "Elegant Dark" experience as requested
    primary = SoftLavender,
    secondary = SoftLilac,
    tertiary = WarmAccent,
    background = DeepCharcoalBg,
    surface = ElegantSurface,
    onPrimary = DeepGrape,
    onSecondary = DeepGrape,
    onBackground = SilverText,
    onSurface = SilverText,
    surfaceVariant = DarkBarBg,
    onSurfaceVariant = MutedSlate,
    outline = CharcoalOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
