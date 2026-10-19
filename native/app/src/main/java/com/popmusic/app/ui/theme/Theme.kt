package com.popmusic.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PoPDarkScheme = darkColorScheme(
    primary = Ink,
    onPrimary = Black,
    primaryContainer = Surface3,
    onPrimaryContainer = Ink,
    secondary = Accent,
    onSecondary = Black,
    secondaryContainer = Surface2,
    onSecondaryContainer = Ink,
    tertiary = Muted,
    onTertiary = Black,
    background = Black,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    surfaceContainer = BgElev,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3,
    outline = LineStrong,
    outlineVariant = Line,
    error = Danger,
    onError = Black,
)

private val PoPShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun PoPMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PoPDarkScheme,
        typography = PoPTypography,
        shapes = PoPShapes,
        content = content,
    )
}
