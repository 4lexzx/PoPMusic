package com.popmusic.app.ui.screens.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.components.Artwork
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Carátulas en carrusel: la anterior y la siguiente asoman al arrastrar y el
 * cambio sigue la velocidad del dedo. Si no hay canción a ese lado, hace tope
 * elástico y vuelve.
 */
@Composable
internal fun ArtworkCarousel(
    mediaId: String,
    previousArtwork: String?,
    nextArtwork: String?,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    /** Carátula actual; [override] es la que se mantiene mientras cambia la pista. */
    current: @Composable (override: String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    // Los gestos rápidos no pueden perderse entre corutinas: se acumulan aquí.
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf(false) }
    // Tras el gesto se enseña YA la carátula nueva hasta que el reproductor
    // confirma el cambio: sin esto reaparecía la anterior un instante.
    var pinned by remember { mutableStateOf<String?>(null) }
    var pinnedFrom by remember { mutableStateOf("") }
    LaunchedEffect(mediaId) { if (mediaId != pinnedFrom) pinned = null }
    LaunchedEffect(pinned) {
        if (pinned != null) {
            delay(1_500)
            pinned = null
        }
    }

    val page = with(LocalDensity.current) { (size + 26.dp).toPx() }
    val canPrevious by rememberUpdatedState(hasPrevious)
    val canNext by rememberUpdatedState(hasNext)
    val previousUrl by rememberUpdatedState(previousArtwork)
    val nextUrl by rememberUpdatedState(nextArtwork)
    val goPrevious by rememberUpdatedState(onPrevious)
    val goNext by rememberUpdatedState(onNext)
    val currentId by rememberUpdatedState(mediaId)

    Box(
        modifier = modifier
            .size(size)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    if (settling) return@rememberDraggableState
                    val blocked = (dragOffset + delta > 0f && !canPrevious) ||
                        (dragOffset + delta < 0f && !canNext)
                    dragOffset = (dragOffset + delta * if (blocked) 0.28f else 1f)
                        .coerceIn(-page, page)
                    scope.launch { offset.snapTo(dragOffset) }
                },
                onDragStarted = { if (!settling) dragOffset = offset.value },
                onDragStopped = { velocity -> if (!settling) scope.launch {
                    val x = dragOffset
                    val flick = abs(velocity) > 700f
                    val target = when {
                        canNext && x < 0f && (x < -page * 0.28f || (flick && velocity < 0f)) -> -page
                        canPrevious && x > 0f && (x > page * 0.28f || (flick && velocity > 0f)) -> page
                        else -> 0f
                    }
                    settling = true
                    offset.animateTo(
                        target,
                        spring(
                            dampingRatio = if (target == 0f) 0.78f else Spring.DampingRatioNoBouncy,
                            stiffness = 420f,
                        ),
                        initialVelocity = velocity,
                    )
                    if (target != 0f) {
                        pinnedFrom = currentId
                        pinned = if (target < 0f) nextUrl else previousUrl
                        if (target < 0f) goNext() else goPrevious()
                        offset.snapTo(0f)
                    }
                    dragOffset = 0f
                    settling = false
                } },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (hasPrevious) Neighbour(previousArtwork, -page, page) { offset.value }
        if (hasNext) Neighbour(nextArtwork, page, page) { offset.value }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = (abs(offset.value) / page).coerceIn(0f, 1f)
                    translationX = offset.value
                    scaleX = 1f - 0.12f * t
                    scaleY = scaleX
                    alpha = 1f - 0.45f * t
                },
        ) { current(pinned) }
    }
}

@Composable
private fun Neighbour(url: String?, base: Float, page: Float, offset: () -> Float) {
    Artwork(
        url = url,
        cornerRadius = 24.dp,
        iconSize = 84.dp,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val x = base + offset()
                val t = (abs(x) / page).coerceIn(0f, 1f)
                translationX = x
                scaleX = 1f - 0.12f * t
                scaleY = scaleX
                // En reposo no se dibuja: no asoma por los lados.
                alpha = if (offset() == 0f) 0f else 1f - 0.45f * t
            },
    )
}
