package com.popmusic.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.theme.Accent
import com.popmusic.app.ui.theme.Black
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Surface2

@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isShuffling by remember { mutableStateOf(false) }
    var repeatEnabled by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.28f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Surface2, Black, Black),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Cerrar",
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "REPRODUCIENDO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Faint,
                )
                Text(
                    text = "PoP Music",
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Más opciones",
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Artwork(
            url = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            cornerRadius = 22.dp,
            iconSize = 80.dp,
        )
        Spacer(Modifier.weight(1f))

        Text(
            text = "Sin señal",
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "PoP Music",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(12.dp))
        Slider(
            value = progress,
            onValueChange = { progress = it },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "1:12",
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
            Text(
                text = "3:45",
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { isShuffling = !isShuffling }) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (isShuffling) Accent else Faint,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Anterior",
                    modifier = Modifier.size(34.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Siguiente",
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(onClick = { repeatEnabled = !repeatEnabled }) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = "Repetir",
                    tint = if (repeatEnabled) Accent else Faint,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = "Me gusta",
                    tint = Faint,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.Subtitles,
                    contentDescription = "Letras",
                    tint = Faint,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = "Cola",
                    tint = Faint,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
