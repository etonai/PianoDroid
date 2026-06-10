package com.pseddev.pianodroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PianoDroidColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = AccentColor,
    background = DarkBackground,
    surface = SurfaceColor,
    onPrimary = OnBackground,
    onSecondary = OnBackground,
    onTertiary = OnBackground,
    onBackground = OnBackground,
    onSurface = OnSurface,
)

@Composable
fun PianoDroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PianoDroidColorScheme,
        content = content,
    )
}
