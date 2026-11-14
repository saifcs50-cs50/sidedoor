package com.sidedoor.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ember = Color(0xFFE2553F)
private val Plum = Color(0xFF1F1A24)

private val Light = lightColorScheme(
    primary = Ember,
    onPrimary = Color.White,
    background = Color(0xFFF7F4EF),
    onBackground = Plum,
    surface = Color.White,
    onSurface = Plum,
    surfaceVariant = Color(0xFFEDE7DF),
    onSurfaceVariant = Color(0xFF5E5663),
    secondaryContainer = Plum,
    onSecondaryContainer = Color.White,
    errorContainer = Color(0xFFFFE1DA),
    onErrorContainer = Color(0xFF5A1406),
)

private val Dark = darkColorScheme(
    primary = Ember,
    onPrimary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2EEE9),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFF2EEE9),
    surfaceVariant = Color(0xFF26232A),
    onSurfaceVariant = Color(0xFFB9B1BD),
    secondaryContainer = Color(0xFF2E2833),
    onSecondaryContainer = Color.White,
    errorContainer = Color(0xFF4A1A10),
    onErrorContainer = Color(0xFFFFDAD2),
)

@Composable
fun SidedoorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
