package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DevotionalLightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = SaffronOnPrimary,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = SaffronOnContainer,
    secondary = SandalwoodSecondary,
    onSecondary = SandalwoodOnSecondary,
    secondaryContainer = SandalwoodContainer,
    onSecondaryContainer = SandalwoodOnContainer,
    tertiary = SacredVermilion,
    onTertiary = SaffronOnPrimary,
    tertiaryContainer = SacredVermilionContainer,
    background = DevotionalCreamBackground,
    onBackground = DevotionalCharcoalText,
    surface = DevotionalCardSurface,
    onSurface = DevotionalCharcoalText,
    surfaceVariant = DevotionalSurfaceVariant,
    onSurfaceVariant = DevotionalCharcoalText,
    outline = DevotionalBorder
)

private val DevotionalDarkColorScheme = darkColorScheme(
    primary = SaffronDarkPrimary,
    onPrimary = SaffronDarkOnPrimary,
    primaryContainer = SaffronDarkContainer,
    onPrimaryContainer = SaffronDarkOnContainer,
    secondary = SaffronDarkPrimary,
    onSecondary = SaffronDarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = GoldAccent,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurface,
    outline = DarkBorder
)

@Composable
fun BhajanSangrahTheme(
    themePreference: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DevotionalDarkColorScheme else DevotionalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
