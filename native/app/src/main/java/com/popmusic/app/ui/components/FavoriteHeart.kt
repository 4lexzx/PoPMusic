package com.popmusic.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Corazón de "me gusta" con degradado de marca, resplandor y animación al
 * marcar. Se usa en las listas ([FavoriteBadge]) y en el reproductor y el
 * Music Flow ([AnimatedFavoriteHeart]); antes era el icono rojo plano de
 * Material, que no transmitía el cambio de estado.
 */
private val HeartTop = Color(0xFFFF5A78)
private val HeartBottom = Color(0xFFE11D48)

private val heartBrush: Brush
    get() = Brush.verticalGradient(listOf(HeartTop, HeartBottom))

private val glowBrush: Brush
    get() = Brush.radialGradient(
        listOf(HeartTop.copy(alpha = 0.40f), Color.Transparent),
    )

/** Icono relleno con el degradado en vez de un color plano. */
@Composable
private fun GradientHeart(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(brush = heartBrush, blendMode = BlendMode.SrcIn)
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * Corazón pequeño que acompaña al título de una canción cuando es favorita.
 * Entra con un "pop" elástico una sola vez y lleva un halo suave: sigue
 * siendo legible a 15 dp pero deja de ser un icono estático.
 */
@Composable
fun FavoriteBadge(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!active) return
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }
    // Sin halo: nítido y del mismo tamaño que el texto.
    Box(
        modifier = modifier
            .size(20.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
        contentAlignment = Alignment.Center,
    ) {
        GradientHeart(18.dp)
    }
}

/**
 * Corazón grande del reproductor y del doble toque del Music Flow: escala
 * elástica al marcar, resplandor con latido suave y seis partículas que
 * salen disparadas. [trigger] permite relanzar la ráfaga en cada doble toque.
 */
@Composable
fun AnimatedFavoriteHeart(
    active: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    trigger: Int = 0,
    inactiveTint: Color = Color.White,
    popOnActivate: Boolean = true,
    pulse: Boolean = false,
    contentDescription: String? = null,
) {
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    val glow = remember { Animatable(0.22f) }

    LaunchedEffect(active) {
        if (active && popOnActivate) {
            pop.snapTo(0.5f)
            pop.animateTo(
                1.3f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(active, pulse) {
        if (active && pulse) {
            glow.animateTo(
                0.34f,
                infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            )
        } else {
            glow.animateTo(if (active) 0.26f else 0f, tween(220))
        }
    }

    val label = contentDescription
    Box(
        modifier = modifier
            .size(iconSize + 4.dp)
            .let { base -> if (label != null) base.semantics { this.contentDescription = label } else base },
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(iconSize * 2)
                    .graphicsLayer {
                        alpha = glow.value * (1f - burst.value * 0.45f)
                        scaleX = 0.85f + burst.value * 0.45f
                        scaleY = 0.85f + burst.value * 0.45f
                    }
                    .background(glowBrush, CircleShape),
            )
            repeat(6) { index ->
                val angle = Math.toRadians((index * 60).toDouble())
                val travel = (iconSize.value * 0.85f) * burst.value
                Box(
                    modifier = Modifier
                        .offset(
                            x = (cos(angle) * travel).toFloat().dp,
                            y = (sin(angle) * travel).toFloat().dp,
                        )
                        .size((4.5f - burst.value * 2f).coerceAtLeast(1.5f).dp)
                        .graphicsLayer { alpha = (1f - burst.value) * 0.95f }
                        .background(if (index % 2 == 0) HeartTop else HeartBottom, CircleShape),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = if (active) pop.value else 1f
                    scaleY = if (active) pop.value else 1f
                },
            contentAlignment = Alignment.Center,
        ) {
            if (active) {
                GradientHeart(iconSize)
            } else {
                Icon(
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = inactiveTint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}
