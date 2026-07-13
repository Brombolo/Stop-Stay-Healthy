package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC0C4EA),
    onPrimary = Color(0xFF141936),
    primaryContainer = PrimaryLightIndigo,
    onPrimaryContainer = Color(0xFF969ABE),
    secondary = Color(0xFFADCFAF),
    onSecondary = Color(0xFF03210C),
    secondaryContainer = Color(0xFF304D35),
    onSecondaryContainer = Color(0xFFC8EBCA),
    tertiary = Color(0xFFFFA295),
    onTertiary = Color(0xFF420900),
    tertiaryContainer = Color(0xFF641906),
    onTertiaryContainer = Color(0xFFEB7E63),
    background = DarkBackground,
    onBackground = OnSurfaceDark,
    surface = DarkSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2E3132),
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF141936),
    secondary = SecondaryMoss,
    onSecondary = Color.White,
    secondaryContainer = SecondaryLightMoss,
    onSecondaryContainer = Color(0xFF03210C),
    tertiary = TertiaryCoral,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryLightCoral,
    onTertiaryContainer = Color(0xFF420900),
    background = LightBackground,
    onBackground = OnSurfaceLight,
    surface = LightSurface,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE1E3E4),
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
