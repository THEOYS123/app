package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanBright,
    onPrimary = Slate950,
    primaryContainer = Slate850,
    onPrimaryContainer = CyanBright,
    secondary = EmeraldBright,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = EmeraldBright,
    tertiary = PurpleNeon,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    outlineVariant = Slate700,
    error = RoseNeon,
    onError = Slate950
)

private val LightColorScheme = lightColorScheme(
    primary = CyanNeon,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF164E63),
    secondary = EmeraldNeon,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = PurpleNeon,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = RoseNeon,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark developer mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
