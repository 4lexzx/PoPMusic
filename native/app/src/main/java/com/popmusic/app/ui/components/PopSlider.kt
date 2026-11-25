package com.popmusic.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.Surface3
import kotlin.math.roundToInt

/**
 * Barra deslizante de PoP Music: pista fina redondeada y pulgar circular que
 * crece al tocarlo. Con [centered] se rellena desde el cero (ecualizador).
 */
@Composable
fun PopSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    /** Salto entre valores; 0 = continuo. */
    step: Float = 0f,
    centered: Boolean = false,
    label: String = "",
    onValueChangeFinished: (Float) -> Unit = {},
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var lastValue by remember { mutableFloatStateOf(value) }
    var width by remember { mutableFloatStateOf(1f) }
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val insetPx = with(LocalDensity.current) { 12.dp.toPx() }
    val thumb by animateDpAsState(if (dragging) 13.dp else 10.dp, tween(140), label = "thumb")
    val track by animateDpAsState(if (dragging) 8.dp else 6.dp, tween(140), label = "track")

    fun snap(raw: Float): Float {
        val limited = raw.coerceIn(range.start, range.endInclusive)
        if (step <= 0f) return limited
        return (range.start + ((limited - range.start) / step).roundToInt() * step)
            .coerceIn(range.start, range.endInclusive)
    }

    fun at(x: Float): Float {
        val usable = (width - insetPx * 2).coerceAtLeast(1f)
        return snap(range.start + ((x - insetPx) / usable).coerceIn(0f, 1f) * span)
    }

    fun emit(next: Float) {
        lastValue = next
        change(next)
    }

    val ink = Ink
    val hole = OnInk
    val rail = Surface3
    val railEdge = Ink.copy(alpha = 0.10f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics {
                if (label.isNotBlank()) contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, range)
                setProgress { target ->
                    val next = snap(target)
                    change(next)
                    finished(next)
                    true
                }
            }
            .pointerInput(range, step) {
                detectTapGestures { offset ->
                    width = size.width.toFloat()
                    emit(at(offset.x))
                    finished(lastValue)
                }
            }
            // Solo horizontal: no roba el desplazamiento vertical de la pantalla.
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    dragX += delta
                    emit(at(dragX))
                },
                onDragStarted = { start ->
                    dragging = true
                    dragX = start.x
                    emit(at(dragX))
                },
                onDragStopped = {
                    dragging = false
                    finished(lastValue)
                },
            ),
    ) {
        width = size.width
        val usable = (size.width - insetPx * 2).coerceAtLeast(1f)
        val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
        val x = insetPx + usable * fraction
        val y = size.height / 2f
        val h = track.toPx()
        val corner = CornerRadius(h / 2f)
        drawRoundRect(rail, Offset(insetPx, y - h / 2f), Size(usable, h), corner)
        drawRoundRect(railEdge, Offset(insetPx, y - h / 2f), Size(usable, h), corner)
        val origin = if (centered) {
            insetPx + usable * ((0f - range.start) / span).coerceIn(0f, 1f)
        } else {
            insetPx
        }
        drawRoundRect(
            ink,
            Offset(minOf(origin, x), y - h / 2f),
            Size(kotlin.math.abs(x - origin), h),
            corner,
        )
        if (centered) drawCircle(ink.copy(alpha = 0.5f), h / 2f, Offset(origin, y))
        val r = thumb.toPx()
        if (dragging) drawCircle(ink.copy(alpha = 0.14f), r * 1.7f, Offset(x, y))
        drawCircle(ink, r, Offset(x, y))
        drawCircle(hole, r * 0.36f, Offset(x, y))
    }
}
