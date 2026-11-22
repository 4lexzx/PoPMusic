package com.popmusic.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import com.popmusic.app.ui.theme.LocalReduceMotion

/**
 * Fundido de entrada la primera vez que el nodo se coloca en pantalla: al
 * entrar al viewport de una lista perezosa, al insertar contenido o al
 * cambiar de tab. Como las listas perezosas vuelven a componer los nodos al
 * re-entrar, cada pasada por el scroll repite el fundido (y las filas de una
 * sección pueden entrar escalonadas con [delayMillis]).
 *
 * Si el sistema pide "reducir movimiento" ([LocalReduceMotion]) no hace nada.
 *
 * @param durationMillis duración del fundido.
 * @param delayMillis retardo escalonado, p. ej. `(index * 25).coerceAtMost(175)`.
 */
@Composable
fun Modifier.enterOnVisible(
    durationMillis: Int = 220,
    delayMillis: Int = 0,
): Modifier {
    if (LocalReduceMotion.current) return this
    var shown by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "enterOnVisible",
    )
    return this
        .onGloballyPositioned {
            if (!shown) shown = true
        }
        .graphicsLayer {
            alpha = progress
            if (progress < 1f) {
                // Micro-desplazamiento hacia arriba mientras aparece.
                translationY = (1f - progress) * 10f
            }
        }
}
