package com.popmusic.app.ui.screens.playlist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.data.library.LibraryRepository.PlaylistSort
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.ArtworkCollage
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.components.GlassActionRow
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassDialogHeader
import com.popmusic.app.ui.components.GlassIconButton
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.components.SectionHeader
import com.popmusic.app.ui.components.SolidMenu
import com.popmusic.app.ui.components.SolidMenuRow
import com.popmusic.app.ui.components.TrackRow
import com.popmusic.app.ui.theme.Accent
import com.popmusic.app.ui.theme.Black
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.pressScale

@Composable
fun PlaylistScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = viewModel(),
) {
    LaunchedEffect(playlistId) { viewModel.load(playlistId) }

    val context = LocalContext.current
    val app = context.applicationContext as? PoPMusicApplication
    val playlist by viewModel.playlist.collectAsState()
    val downloads by app?.libraryRepository?.state?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf<com.popmusic.app.data.library.LibraryState?>(null) }
    val downloadProgress by app?.downloadRepository?.progress?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf<Map<String, Float>>(emptyMap()) }
    val songs = playlist?.songs.orEmpty()
    // En Favoritos/Descargas no hay orden propio: no se ofrece "Mover".
    val canReorder = playlistId != "favoritos" && playlistId != "descargas"

    // Menú ⋯ de cada canción: elegir acción (antes borraba directo).
    var confirmSong by remember { mutableStateOf<Song?>(null) }
    var copyTarget by remember { mutableStateOf<Song?>(null) }
    // Menú "Ordenar" de la cabecera de canciones (ítem 1).
    var sortOpen by remember { mutableStateOf(false) }
    val otherPlaylists by viewModel.playlists.collectAsState()

    val downloadedCount = downloads?.let { state ->
        songs.count { song -> state.downloads.any { it.id == song.id } }
    } ?: 0
    val activeDownloads = downloadProgress.count { (id, _) -> songs.any { it.id == id } }
    val allDownloaded = songs.isNotEmpty() && downloadedCount == songs.size
    val wifiBlocked = app?.preferences?.playback?.value?.downloadWifiOnly == true &&
        app?.downloadRepository?.isOnWifi() == false

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Volver",
                onClick = onBack,
            )
            Text(
                text = if (playlistId == "favoritos" || playlistId == "descargas") {
                    "Colección"
                } else {
                    "Playlist"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 160.dp),
        ) {
            // Hero (foto/nombre/botones) DENTRO del scroll: al bajar, se
            // mueve con la lista en vez de quedarse estático (tanda 5).
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(10.dp))
            ArtworkCollage(
                urls = songs.map { it.thumbnailUrl },
                modifier = Modifier
                    .size(214.dp)
                    .shadow(
                        elevation = 26.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black,
                        spotColor = Color.Black,
                    ),
                cornerRadius = 24.dp,
                iconSize = 68.dp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = playlist?.name ?: "Playlist",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(songCount(songs.size))
                    val total = totalDurationLabel(songs)
                    if (total != null) append(" · $total")
                    if (albumArtist(songs) != null) append(" · ${albumArtist(songs)}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                val interaction = remember { MutableInteractionSource() }
                val enabled = songs.isNotEmpty()

                // Orden de botones (tanda 6, punto 4): aleatorio, play, descargar.
                GlassIconButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = "Aleatorio",
                    onClick = { if (songs.isNotEmpty()) onPlay(songs.shuffled(), 0) },
                    enabled = songs.isNotEmpty(),
                    size = 56.dp,
                    iconSize = 24.dp,
                )

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(
                            elevation = if (enabled) 18.dp else 0.dp,
                            shape = CircleShape,
                            ambientColor = Accent.copy(alpha = 0.35f),
                            spotColor = Accent.copy(alpha = 0.35f),
                        )
                        .clip(CircleShape)
                        .background(
                            if (enabled) {
                                Brush.linearGradient(listOf(Ink, Accent))
                            } else {
                                Brush.linearGradient(listOf(Surface2, Surface2))
                            },
                        )
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(bounded = false, radius = 34.dp, color = Black),
                            enabled = enabled,
                        ) { onPlay(songs, 0) }
                        .pressScale(interaction, pressedScale = 0.88f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Rounded.PlayArrow else Icons.Rounded.QueueMusic,
                        contentDescription = "Reproducir playlist",
                        tint = if (enabled) Black else Faint,
                        modifier = Modifier.size(38.dp),
                    )
                }

                GlassIconButton(
                    icon = if (allDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                    contentDescription = if (allDownloaded) "Descargada" else "Descargar playlist",
                    onClick = {
                        if (wifiBlocked) {
                            Toast.makeText(
                                context,
                                "Conecta a Wi-Fi o desactiva «Solo Wi-Fi» en Ajustes",
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else if (!allDownloaded) {
                            songs.forEach { song -> app?.downloadRepository?.download(song) }
                        }
                    },
                    enabled = songs.isNotEmpty() && !allDownloaded,
                    size = 56.dp,
                    iconSize = 24.dp,
                    tint = if (allDownloaded) Accent else Ink,
                )
            }
            if (activeDownloads > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Descargando $downloadedCount de ${songs.size}…",
                    style = MaterialTheme.typography.labelMedium,
                    color = Faint,
                )
            }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Box {
                    SectionHeader(
                        title = "Canciones",
                        action = "Ordenar",
                        onActionClick = { sortOpen = true },
                    )
                    SolidMenu(
                        expanded = sortOpen,
                        onDismiss = { sortOpen = false },
                    ) { dismiss ->
                        SolidMenuRow(
                            icon = Icons.Rounded.SortByAlpha,
                            label = "Título (A-Z)",
                            onClick = {
                                viewModel.sort(PlaylistSort.TITLE)
                                dismiss()
                            },
                        )
                        SolidMenuRow(
                            icon = Icons.Rounded.Person,
                            label = "Artista (A-Z)",
                            onClick = {
                                viewModel.sort(PlaylistSort.ARTIST)
                                dismiss()
                            },
                        )
                        SolidMenuRow(
                            icon = Icons.Rounded.Schedule,
                            label = "Duración (mayor primero)",
                            onClick = {
                                viewModel.sort(PlaylistSort.DURATION)
                                dismiss()
                            },
                        )
                    }
                }
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
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    TrackRow(
                        title = song.title,
                        artist = song.artist,
                        artworkUrl = song.thumbnailUrl,
                        liked = downloads?.favorites?.any { it.id == song.id } == true,
                        modifier = Modifier.animateItem(),
                        onClick = { onPlay(songs, index) },
                        moreMenu = { dismiss ->
                            if (canReorder && index > 0) {
                                SolidMenuRow(
                                    icon = Icons.Rounded.ArrowUpward,
                                    label = "Mover arriba",
                                    onClick = {
                                        viewModel.moveSong(song.id, up = true)
                                        dismiss()
                                    },
                                )
                            }
                            if (canReorder && index < songs.lastIndex) {
                                SolidMenuRow(
                                    icon = Icons.Rounded.ArrowDownward,
                                    label = "Mover abajo",
                                    onClick = {
                                        viewModel.moveSong(song.id, up = false)
                                        dismiss()
                                    },
                                )
                            }
                            SolidMenuRow(
                                icon = Icons.Rounded.PlaylistAdd,
                                label = "Añadir a otra playlist",
                                onClick = {
                                    copyTarget = song
                                    dismiss()
                                },
                            )
                            SolidMenuRow(
                                icon = Icons.Rounded.Delete,
                                label = "Eliminar",
                                tint = Danger,
                                onClick = {
                                    confirmSong = song
                                    dismiss()
                                },
                            )
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Copiar la canción a otra playlist (menú ⋯ → "Añadir a otra playlist").
    copyTarget?.let { song ->
        GlassDialog(onDismiss = { copyTarget = null }) {
            GlassDialogHeader(
                title = "Añadir a otra playlist",
                subtitle = song.title,
                onClose = { copyTarget = null },
            )
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (otherPlaylists.isEmpty()) {
                    Text(
                        text = "Aún no tienes otras playlists.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Faint,
                    )
                }
                otherPlaylists.forEach { target ->
                    GlassActionRow(
                        icon = Icons.Rounded.QueueMusic,
                        title = target.name,
                        subtitle = when (target.songs.size) {
                            0 -> "Playlist vacía"
                            1 -> "1 canción"
                            else -> "${target.songs.size} canciones"
                        },
                        onClick = {
                            viewModel.copyToPlaylist(target.id, song)
                            copyTarget = null
                        },
                    )
                }
            }
        }
    }

    // Confirmación en rojo antes de quitar la canción de la playlist.
    confirmSong?.let { song ->
        GlassDialog(onDismiss = { confirmSong = null }) {
            GlassDialogHeader(
                title = "¿Eliminar esta canción?",
                subtitle = song.title,
                onClose = { confirmSong = null },
            )
            Spacer(Modifier.height(16.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { confirmSong = null },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Eliminar",
                    danger = true,
                    onClick = {
                        viewModel.removeSong(song.id)
                        confirmSong = null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun songCount(count: Int): String = when (count) {
    0 -> "Sin canciones"
    1 -> "1 canción"
    else -> "$count canciones"
}

private fun totalDurationLabel(songs: List<Song>): String? {
    val seconds = songs.sumOf { it.durationSeconds }
    if (seconds <= 0L) return null
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "$hours h $minutes min"
        minutes > 0 -> "$minutes min"
        else -> "$seconds s"
    }
}

private fun albumArtist(songs: List<Song>): String? {
    if (songs.isEmpty()) return null
    val first = songs.first().artist.trim()
    if (first.isBlank()) return null
    return if (songs.all { it.artist.trim() == first }) first else null
}
