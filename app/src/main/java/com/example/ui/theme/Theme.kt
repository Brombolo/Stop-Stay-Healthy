package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HealthyEmerald,
    onPrimary = Color.Black,
    primaryContainer = DeepEmerald,
    onPrimaryContainer = Color.White,
    secondary = SkyBlue,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF21618C),
    onSecondaryContainer = Color.White,
    tertiary = WarmAmber,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = OnSurfaceDark,
    surface = DarkSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF34495E),
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = HealthyEmerald,
    onPrimary = Color.White,
    primaryContainer = SoftEmerald,
    onPrimaryContainer = DeepEmerald,
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBF5FB),
    onSecondaryContainer = Color(0xFF2874A6),
    tertiary = VividCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDEDEC),
    onTertiaryContainer = Color(0xFF943126),
    background = LightBackground,
    onBackground = OnSurfaceLight,
    surface = LightSurface,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF2F4F4),
    onSurfaceVariant = OnSurfaceVariantLight
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
