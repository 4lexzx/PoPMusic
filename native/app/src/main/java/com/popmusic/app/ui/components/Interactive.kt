package com.popmusic.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.GlassTint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.LocalReduceMotion
import com.popmusic.app.ui.theme.Surface3
import com.popmusic.app.ui.theme.pressScale

/** Icono circular sólido (botón circular con fondo opaco, sin cristal). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
    tint: Color = Ink,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    // Tanda 5 (ítem 4): sólido como el resto de la interfaz (sin liquid
    // glass), igual que la barra de navegación con el switch apagado.
    Box(
        modifier = modifier
            .size(size)
            .background(Surface3, CircleShape)
            .border(1.dp, Faint.copy(alpha = 0.30f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = size / 2),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.90f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else Faint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Botón de icono limpio (sin fondo), con onda y compresión al pulsar. */
@Composable
fun PopIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = Ink,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(46.dp)
            .then(modifier)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = 23.dp),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.82f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Botón principal de reproducción: limpio, con relieve y pulsación elástica. */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val reduceMotion = LocalReduceMotion.current
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "playScale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.15f),
                        ),
                    ),
                    CircleShape,
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(bounded = false, radius = 39.dp),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlass(
                modifier = Modifier.matchParentSize(),
                shape = CircleShape,
                tint = GlassTint,
                elevated = false,
                // Item 13: lente líquida solo en la nav.
                liquid = false,
            ) {
                Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 2.dp,
                            color = Ink,
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Ink,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
            }
        }
    }
}
