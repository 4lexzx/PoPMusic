package com.popmusic.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.popmusic.app.ui.theme.Black
import com.popmusic.app.ui.theme.blurIfSupported

@Composable
fun GlassBackdrop(
    url: String?,
    modifier: Modifier = Modifier,
    imageAlpha: Float = 0.65f,
    dimTop: Float = 0.35f,
    dimBottom: Float = 0.94f,
    /** Canvas/Ambient de Echo: la imagen desenfocada respira con zoom lento. */
    motion: Boolean = false,
) {
    Box(modifier = modifier) {
        if (url != null) {
            val scale = if (motion) {
                val transition = rememberInfiniteTransition(label = "backdropMotion")
                transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.16f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(MOTION_DURATION_MS, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "backdropScale",
                ).value
            } else {
                1f
            }
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = imageAlpha,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .blurIfSupported(52.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Black.copy(alpha = dimTop), Black.copy(alpha = dimBottom)),
                    ),
                ),
        )
    }
}

private const val MOTION_DURATION_MS = 16_000
