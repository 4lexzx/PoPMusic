package com.popmusic.app.ui.screens.recognize

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.theme.Accent
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.Surface3

@Composable
fun RecognizeScreen(
    onFound: (Song) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecognizeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.state

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
        if (granted) viewModel.start(context)
    }

    LaunchedEffect(state) {
        val found = state as? RecognitionUiState.Found
        if (found != null) {
            onFound(found.song)
            viewModel.reset()
        }
    }

    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (state is RecognitionUiState.Recording) 1.1f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state is RecognitionUiState.Recording) 700 else 1600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    val busy = state is RecognitionUiState.Recording || state is RecognitionUiState.Recognizing

    val headline = when (state) {
        is RecognitionUiState.Recording -> "Escuchando…"
        is RecognitionUiState.Recognizing -> "Analizando…"
        is RecognitionUiState.Found -> state.song.title
        is RecognitionUiState.Error -> "No se pudo reconocer"
        else -> "Toca para escuchar"
    }

    val subtitle = when (state) {
        is RecognitionUiState.Recording -> "Acerca el teléfono al altavoz"
        is RecognitionUiState.Recognizing -> "Buscando coincidencias"
        is RecognitionUiState.Found -> state.song.artist
        is RecognitionUiState.Error -> state.message
        else -> if (permissionDenied) {
            "Necesitamos el micrófono para reconocer la canción."
        } else {
            "Analizamos unos segundos de audio y buscamos la canción en YouTube Music."
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

        Box(
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Accent.copy(alpha = 0.22f), Surface3.copy(alpha = 0.4f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Ink)
                    .clickable(enabled = !busy) {
                        permissionDenied = false
                        when {
                            !hasPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            state is RecognitionUiState.Error -> {
                                viewModel.reset()
                                viewModel.start(context)
                            }
                            else -> viewModel.start(context)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state is RecognitionUiState.Recognizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp,
                        color = Surface2,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Reconocer canción",
                        tint = Surface2,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state is RecognitionUiState.Error) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(32.dp))
    }
}
