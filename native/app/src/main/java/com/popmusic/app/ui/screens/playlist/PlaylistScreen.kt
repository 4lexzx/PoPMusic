package com.popmusic.app.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.components.SectionHeader
import com.popmusic.app.ui.components.TrackRow
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Surface
import com.popmusic.app.ui.theme.Surface2

@Composable
fun PlaylistScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = viewModel(),
) {
    LaunchedEffect(playlistId) { viewModel.load(playlistId) }

    val playlist by viewModel.playlist.collectAsState()
    val songs = playlist?.songs.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Surface2, Surface, Surface),
                ),
            )
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                )
            }
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.labelLarge,
                color = Faint,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Artwork(
                url = songs.firstOrNull()?.thumbnailUrl,
                modifier = Modifier.size(196.dp),
                cornerRadius = 18.dp,
                iconSize = 64.dp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = playlist?.name ?: "Playlist",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = songCount(songs.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (songs.isEmpty()) Surface2 else MaterialTheme.colorScheme.primary,
                    )
                    .clickable(enabled = songs.isNotEmpty()) { onPlay(songs, 0) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = "Reproducir playlist",
                    tint = if (songs.isEmpty()) Faint else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            item {
                SectionHeader(title = "Canciones")
                Spacer(Modifier.height(8.dp))
            }

            if (songs.isEmpty()) {
                item {
                    EmptyState(
                        title = "Playlist vacía",
                        subtitle = "Añade canciones desde la búsqueda o desde tu biblioteca.",
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = Faint,
                            )
                        },
                    )
                }
            } else {
                items(songs, key = { it.id }) { song ->
                    val index = songs.indexOf(song)
                    TrackRow(
                        title = song.title,
                        artist = song.artist,
                        artworkUrl = song.thumbnailUrl,
                        onClick = { onPlay(songs, index) },
                        onMoreClick = { viewModel.removeSong(song.id) },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun songCount(count: Int): String = when (count) {
    0 -> "Sin canciones"
    1 -> "1 canción"
    else -> "$count canciones"
}
