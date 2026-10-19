package com.popmusic.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.MediaCard
import com.popmusic.app.ui.components.SectionHeader
import com.popmusic.app.ui.components.TrackRow
import com.popmusic.app.ui.theme.Faint
import java.util.Calendar

@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val libraryState by viewModel.favorites.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.statusBarsPadding())
        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "POP MUSIC",
                        style = MaterialTheme.typography.labelMedium,
                        color = Faint,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = greeting(),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Ajustes",
                        tint = Faint,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading && state.sections.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(28.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            state.error != null && state.sections.isEmpty() -> {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = state.error ?: "No se pudo cargar",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = viewModel::load) {
                        Text("Reintentar")
                    }
                }
            }
        }

        state.sections.forEach { section ->
            Spacer(Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(title = section.title)
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(section.songs) { index, song ->
                    MediaCard(
                        title = song.title,
                        subtitle = song.artist,
                        artworkUrl = song.thumbnailUrl,
                        onClick = { onPlay(section.songs, index) },
                    )
                }
            }
        }

        if (libraryState.favorites.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(title = "Tus favoritos")
                Spacer(Modifier.height(8.dp))
                libraryState.favorites.take(6).forEach { song ->
                    TrackRow(
                        title = song.title,
                        artist = song.artist,
                        artworkUrl = song.thumbnailUrl,
                        onClick = { onPlay(libraryState.favorites, libraryState.favorites.indexOf(song)) },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 6..11 -> "Buenos días"
    in 12..19 -> "Buenas tardes"
    else -> "Buenas noches"
}
