package com.popmusic.app.ui.screens.recognize

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.pressScale

@Composable
fun RecognizeScreen(
    onFound: (Song) -> Unit,
    /** true al empezar a escuchar; false si termina sin abrir una canción. */
    onListening: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecognizeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.state
    val listening = state is RecognitionUiState.Listening
    val matched = (state as? RecognitionUiState.Matched)?.song
    var permissionDenied by remember { mutableStateOf(false) }
    val notifyListening by rememberUpdatedState(onListening)

    fun begin() {
        permissionDenied = false
        notifyListening(true)
        viewModel.start()
    }

    fun stop() {
        val active = viewModel.state is RecognitionUiState.Listening ||
            viewModel.state is RecognitionUiState.Matched
        viewModel.cancel()
        if (active) notifyListening(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
        if (granted) begin()
    }

    LaunchedEffect(Unit) { viewModel.prepare() }

    LaunchedEffect(state) {
        when (state) {
            is RecognitionUiState.Found -> {
                viewModel.cancel()
                onFound(state.song)
            }
            is RecognitionUiState.Error -> notifyListening(false)
            else -> Unit
        }
    }

    // El micrófono nunca queda abierto fuera de esta pantalla.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            stop()
        }
    }

    val headline = when (state) {
        is RecognitionUiState.Listening -> "Escuchando…"
        is RecognitionUiState.Matched -> state.song.title
        is RecognitionUiState.Found -> state.song.title
        is RecognitionUiState.Error -> state.title
        RecognitionUiState.Idle -> "Toca para escuchar"
    }
    val subtitle = when (state) {
        is RecognitionUiState.Listening -> "Acerca el teléfono a la música"
        is RecognitionUiState.Matched -> state.song.artist
        is RecognitionUiState.Found -> state.song.artist
        is RecognitionUiState.Error -> state.message
        RecognitionUiState.Idle -> if (permissionDenied) {
            "Permite el micrófono para reconocer la canción."
        } else {
            "Te decimos qué canción suena en segundos."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Reconocer",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "¿Qué canción está sonando?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start),
        )

        Spacer(Modifier.weight(1f))

        Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            if (listening) ListeningRings(level = { viewModel.level })
            AnimatedContent(
                targetState = matched,
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.8f)) togetherWith
                        fadeOut(tween(160))
                },
                label = "reconocida",
            ) { song ->
                if (song != null) {
                    Artwork(
                        url = song.artworkUrl,
                        modifier = Modifier.size(220.dp),
                        cornerRadius = 24.dp,
                        iconSize = 64.dp,
                    )
                } else {
                    MicButton(
                        listening = listening,
                        level = { viewModel.level },
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            when {
                                listening -> stop()
                                granted -> begin()
                                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state is RecognitionUiState.Error) {
                Danger
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 3,
        )

        // Alto fijo: el texto no salta al aparecer el botón.
        Box(modifier = Modifier.height(66.dp), contentAlignment = Alignment.BottomCenter) {
            if (listening) {
                Text(
                    text = "Cancelar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(50),
                        )
                        .clickable(onClick = ::stop)
                        .padding(horizontal = 30.dp, vertical = 12.dp),
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun MicButton(listening: Boolean, level: () -> Float, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pulse by animateFloatAsState(
        targetValue = if (listening) 1f + level() * 0.14f else 1f,
        animationSpec = tween(140),
        label = "volumen",
    )
    Box(
        modifier = Modifier
            .size(148.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .pressScale(interaction, pressedScale = 0.92f)
            .clip(CircleShape)
            .background(Ink)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Surface2),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = if (listening) "Dejar de escuchar" else "Reconocer canción",
            tint = OnInk,
            modifier = Modifier.size(60.dp),
        )
    }
}

/** Ondas que salen del botón; más intensas cuanto más fuerte suena. */
@Composable
private fun ListeningRings(level: () -> Float) {
    val transition = rememberInfiniteTransition(label = "ondas")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_100, easing = LinearEasing), RepeatMode.Restart),
        label = "fase",
    )
    val color = Ink
    Canvas(modifier = Modifier.size(300.dp)) {
        val from = 74.dp.toPx()
        val to = size.minDimension / 2f - 2.dp.toPx()
        repeat(3) { i ->
            val t = (phase + i / 3f) % 1f
            drawCircle(
                color = color,
                radius = from + (to - from) * t,
                alpha = (1f - t) * (0.16f + 0.4f * level()),
                style = Stroke(width = (1.5f + 2.5f * (1f - t)).dp.toPx()),
            )
        }
    }
}
