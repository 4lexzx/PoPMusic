package com.popmusic.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Loader musical: un audífono que se "llena" de abajo hacia arriba, con una
 * pausa breve al terminar cada vuelta. Sustituye al círculo de progreso
 * genérico mientras se buffering la canción.
 *
 * Se dibuja en dos capas: el contorno atenuado de base y encima el mismo
 * icono recortado por arriba ([clipRect]) según el progreso del ciclo.
 */
@Composable
fun MusicLoader(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Color.White,
) {
    val transition = rememberInfiniteTransition(label = "musicLoader")
    val fill by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            // Pausa breve entre vueltas (se reinicia vacío y vuelve a llenarse).
            initialStartOffset = StartOffset(350),
        ),
        label = "musicLoaderFill",
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = null,
            tint = color.copy(alpha = 0.22f),
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    // `this.size` = lienzo (el parámetro `size: Dp` de la
                    // función sombrearía el `size` del DrawScope).
                    clipRect(top = this.size.height * (1f - fill)) {
                        this@drawWithContent.drawContent()
                    }
                },
        )
    }
}
