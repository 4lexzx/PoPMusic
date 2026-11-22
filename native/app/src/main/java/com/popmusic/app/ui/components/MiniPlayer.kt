package com.popmusic.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.GlassSurface
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.LocalLiquidGlassEnabled
import com.popmusic.app.ui.theme.Surface3
import com.popmusic.app.ui.theme.WavyShape
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mini reproductor estilo Echo Music: píldora flotante centrada (máx. 340 dp)
 * con arte circular y anillo de progreso, título con marquee, artista y
 * controles anterior / play ondulado (gira mientras suena) / siguiente.
 * Deslizar horizontalmente la píldora cambia de canción; tocarla abre el
 * reproductor completo.
 *
 * [compact] = modo inline de la barra (56 dp): sin artista y solo el botón
 * ondulado, como el accessory de Echo Music cuando la barra está plegada.
 */
@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    artworkUrl: String?,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    durationMs: Long,
    positionProvider: () -> Long,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() }
    val returnSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )

    // Sólida con "Cristal líquido" APAGADO (como la nav: Surface3 + borde);
    // lente líquida solo al activar el ajuste (la mini forma parte de la
    // barra de navegación).
    val pillShape = RoundedCornerShape(50)
    val pillModifier = Modifier
        .widthIn(max = 340.dp)
        .height(if (compact) 56.dp else 64.dp)
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragCancel = {
                    scope.launch { offsetX.animateTo(0f, returnSpec) }
                },
                onHorizontalDrag = { _, dragAmount ->
                    scope.launch {
                        offsetX.snapTo(
                            (offsetX.value + dragAmount)
                                .coerceIn(-thresholdPx * 1.6f, thresholdPx * 1.6f),
                        )
                    }
                },
                onDragEnd = {
                    val offset = offsetX.value
                    when {
                        offset <= -thresholdPx -> onNext()
                        offset >= thresholdPx -> onPrevious()
                    }
                    scope.launch { offsetX.animateTo(0f, returnSpec) }
                },
            )
        }
    val pillContent: @Composable BoxScope.() -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick,
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
                MiniProgressThumbnail(
                    artworkUrl = artworkUrl,
                    durationMs = durationMs,
                    positionProvider = positionProvider,
                    compact = compact,
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            iterations = 1,
                            initialDelayMillis = 3000,
                            velocity = 30.dp,
                        ),
                    )
                    if (!compact) {
                        Text(
                            text = artist,
                            color = Ink.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                MiniPlayerControls(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    onTogglePlay = onTogglePlay,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    compact = compact,
                )
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (LocalLiquidGlassEnabled.current) {
            GlassSurface(
                modifier = pillModifier,
                shape = pillShape,
                liquid = true,
                shine = true,
            ) {
                pillContent()
            }
        } else {
            Box(
                modifier = pillModifier
                    .background(Surface3, pillShape)
                    .border(1.dp, Faint.copy(alpha = 0.30f), pillShape),
            ) {
                pillContent()
            }
        }
    }
}

@Composable
private fun MiniProgressThumbnail(
    artworkUrl: String?,
    durationMs: Long,
    positionProvider: () -> Long,
    compact: Boolean,
) {
    val currentPosition by rememberUpdatedState(positionProvider)
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(durationMs) {
        while (true) {
            progress = if (durationMs > 0L) {
                (currentPosition().toFloat() / durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            delay(250)
        }
    }

    val ringSize = if (compact) 40.dp else 48.dp
    val innerSize = if (compact) 32.dp else 40.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ringSize)
            .drawWithContent {
                drawContent()

                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                val diameter = size.minDimension
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f,
                )
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = Ink.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                drawArc(
                    color = Ink,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .border(1.dp, Ink.copy(alpha = 0.3f), CircleShape),
        ) {
            Artwork(
                url = artworkUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = innerSize / 2,
                iconSize = if (compact) 14.dp else 18.dp,
            )
        }
    }
}

@Composable
private fun MiniPlayerControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    compact: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!compact) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = Ink,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(8.dp))
        }

        WavyMiniPlayButton(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            onTogglePlay = onTogglePlay,
            buttonSize = if (compact) 40.dp else 48.dp,
            iconSize = if (compact) 20.dp else 22.dp,
        )

        // En modo compacto también mostramos "Siguiente" (el anterior se
        // alcanza con el swipe); no queremos sólo play/pausa.
        Spacer(Modifier.width(if (compact) 4.dp else 8.dp))

        IconButton(
            onClick = onNext,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Siguiente",
                tint = Ink,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// Botón play/pausa "galleta": gira lentamente y muestra los lóbulos
// mientras la canción suena; círculo quieto en pausa.
@Composable
private fun WavyMiniPlayButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlay: () -> Unit,
    buttonSize: Dp,
    iconSize: Dp,
) {
    val wavyIndent by animateFloatAsState(
        targetValue = if (isPlaying) 0.08f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "wavyIndent",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "miniWavy")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "miniWavyRotation",
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(buttonSize)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onTogglePlay,
            )
            .graphicsLayer {
                // El contorno gira; el icono central queda quieto (Echo).
                shape = WavyShape(
                    lobes = 9,
                    indent = wavyIndent,
                    rotationDegrees = rotation,
                )
                clip = true
            }
            .background(Ink),
    ) {
        if (isBuffering && !isPlaying) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (buttonSize < 48.dp) 16.dp else 18.dp),
                strokeWidth = 2.dp,
                color = Color.Black,
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color.Black,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
