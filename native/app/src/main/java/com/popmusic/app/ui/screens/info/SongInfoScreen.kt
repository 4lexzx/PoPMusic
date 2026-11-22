package com.popmusic.app.ui.screens.info

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.popmusic.app.data.info.SongStory
import com.popmusic.app.data.info.SongStoryRepository
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.GlassBackdrop
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.glass.backdrop.backdrops.LayerBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.layerBackdrop
import com.popmusic.app.ui.screens.lyrics.LyricsViewModel
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.Surface2
import java.util.Locale

@Composable
fun SongInfoScreen(
    song: Song?,
    onBack: () -> Unit,
    ambientBackdrop: LayerBackdrop? = null,
    modifier: Modifier = Modifier,
    viewModel: LyricsViewModel = viewModel(),
) {
    val songId = song?.id.orEmpty()
    val title = song?.title.orEmpty()
    val artist = song?.artist.orEmpty()

    LaunchedEffect(songId, title, artist, song?.durationSeconds) {
        if (title.isNotBlank()) {
            viewModel.load(
                songId = songId,
                title = title,
                artist = artist,
                durationMs = (song?.durationSeconds ?: 0L) * 1_000,
            )
        }
    }

    var youtubeAvatar by remember(songId) { mutableStateOf<String?>(null) }
    LaunchedEffect(songId) {
        if (songId.isNotBlank()) {
            val details = runCatching {
                com.popmusic.app.data.youtube.YouTubeRepository().videoDetails(songId)
            }.getOrNull()
            youtubeAvatar = details?.avatarUrl
        }
    }

    var story by remember(songId, title, artist) { mutableStateOf<SongStory?>(null) }
    var storyLoading by remember(songId, title, artist) { mutableStateOf(title.isNotBlank()) }
    LaunchedEffect(songId, title, artist) {
        if (title.isBlank()) {
            storyLoading = false
            return@LaunchedEffect
        }
        story = null
        storyLoading = true
        story = runCatching { SongStoryRepository().story(title, artist) }.getOrNull()
        storyLoading = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        GlassBackdrop(
            url = song?.thumbnailUrl,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (ambientBackdrop != null) {
                        Modifier.layerBackdrop(ambientBackdrop)
                    } else {
                        Modifier
                    },
                ),
            imageAlpha = 0.55f,
            dimTop = 0.30f,
            dimBottom = 0.97f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PopIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Información de la canción",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                song?.let {
                    LiquidGlass(
                        shape = RoundedCornerShape(30.dp),
                        elevation = 22.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Artwork(
                            url = it.thumbnailUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .padding(12.dp),
                            cornerRadius = 22.dp,
                            iconSize = 64.dp,
                        )
                    }
                }

                LiquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    elevation = 20.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    ) {
                        Text(
                            text = title.ifBlank { "Sin canción" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = artist.ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink.copy(alpha = 0.72f),
                        )
                        Spacer(Modifier.height(12.dp))
                        InfoRow("Duración", formatDuration((song?.durationSeconds ?: 0L) * 1_000))
                        viewModel.albumDisplay?.takeIf { album -> album.isNotBlank() }
                            ?.let { InfoRow("Álbum", it) }
                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sobre el artista",
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink,
                            )
                            Spacer(Modifier.weight(1f))
                            if (viewModel.artistInfoLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Faint,
                                )
                            }
                        }

                        val info = viewModel.artistInfo
                        val avatarUrl = info?.imageUrl?.takeIf { it.isNotBlank() } ?: youtubeAvatar
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = Faint,
                                    modifier = Modifier.size(30.dp),
                                )
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape),
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = info?.name?.takeIf { it.isNotBlank() }
                                        ?: artist.ifBlank { "Artista desconocido" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Ink,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (viewModel.artistInfoLoading) {
                                        "Buscando información…"
                                    } else {
                                        "Artista de la canción"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Faint,
                                )
                            }
                        }
                        val summary = info?.summary
                        if (!summary.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink.copy(alpha = 0.80f),
                                lineHeight = 18.sp,
                            )
                        } else if (!viewModel.artistInfoLoading) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Sin biografía disponible",
                                style = MaterialTheme.typography.bodySmall,
                                color = Faint,
                            )
                        }
                    }
                }

                LiquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    elevation = 20.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Music DNA",
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.weight(1f))
                            if (storyLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Faint,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val currentStory = story
                        when {
                            storyLoading -> Text(
                                text = "Buscando la historia de la canción…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Faint,
                            )

                            currentStory != null -> {
                                val extract = currentStory.extract
                                Text(
                                    text = if (extract.length > 900) {
                                        extract.take(900).trimEnd() + "…"
                                    } else {
                                        extract
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Ink.copy(alpha = 0.80f),
                                    lineHeight = 18.sp,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = if (currentStory.lang == "es") {
                                        "Basado en Wikipedia"
                                    } else {
                                        "Basado en Wikipedia (EN)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Faint,
                                )
                            }

                            else -> Text(
                                text = "Sin historia disponible",
                                style = MaterialTheme.typography.bodySmall,
                                color = Faint,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
            modifier = Modifier.width(84.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
