package com.popmusic.app.ui.screens.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popmusic.app.domain.LyricLine
import com.popmusic.app.ui.theme.Ink

/**
 * Línea de letra al estilo Echo (sin animación pesada, "al pie de la
 * canción"): la línea entera se muestra de golpe como un único [Text];
 * solo cambian la opacidad y la escala según la distancia a la activa.
 *
 * - **Alpha por distancia** con tween de 300 ms:
 *   activa 1f · ±1 → 0.65f · ±2 → 0.45f · resto 0.35f.
 * - **Escala** con tween de 400 ms: activa 1f, el resto al 85 %.
 * - Peso ExtraBold en la línea activa y Bold en el resto.
 *
 * (Antes cada palabra era un `Text` con barrido de brillo karaoke; el
 * usuario pidió párrafo a párrafo, sin animación pesada.)
 *
 * Lo usan la pantalla de letras completa y el panel dentro del reproductor.
 */
@Composable
fun PopLyricLine(
    line: LyricLine,
    next: LyricLine?,
    isCurrent: Boolean,
    distance: Int,
    position: State<Long>,
    typography: TextStyle,
    textColor: Color = Ink,
    translation: String? = null,
    modifier: Modifier = Modifier,
) {
    val targetAlpha = when {
        isCurrent -> 1f
        distance == 1 -> 0.65f
        distance == 2 -> 0.45f
        else -> 0.35f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(300),
        label = "lineAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.85f,
        animationSpec = tween(400),
        label = "lineScale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.Start,
    ) {
        if (line.isInstrumental || line.text.isBlank()) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = "Instrumental",
                tint = textColor.copy(alpha = if (isCurrent) 1f else 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .size(30.dp),
            )
        } else {
            // Línea completa en un solo Text (sin barrido por palabra).
            Text(
                text = line.text,
                style = typography,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!translation.isNullOrBlank()) {
            Text(
                text = translation,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textColor.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}
