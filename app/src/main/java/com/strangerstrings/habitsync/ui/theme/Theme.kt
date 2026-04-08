package com.strangerstrings.habitsync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    surface = Surface,
    surfaceContainerHigh = SurfaceContainerHigh,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryContainer,
    onPrimary = OnPrimaryContainer,
    secondary = Secondary,
)

@Composable
fun HabitSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content,
    )
}
