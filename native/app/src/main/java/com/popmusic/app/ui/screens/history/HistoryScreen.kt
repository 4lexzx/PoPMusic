package com.popmusic.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.popmusic.app.data.recent.RecentStore
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.components.GroupedSongList
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.components.SectionHeader

/**
 * Historial de reproducciones ("Recientes"), equivalente a la Activity
 * History de Echo Music. Se relee al entrar en la pantalla porque el
 * composable sale de la composición al navegar fuera.
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf(RecentStore.recent(context)) }
    // Corazón de favorito junto al nombre (ítem 4 de la tanda 3).
    val libraryState by (context.applicationContext as? com.popmusic.app.PoPMusicApplication)
        ?.libraryRepository?.state?.collectAsState()
        ?: remember {
            androidx.compose.runtime.mutableStateOf<com.popmusic.app.data.library.LibraryState?>(null)
        }
    val favoriteIds = remember(libraryState) {
        libraryState?.favorites?.mapTo(HashSet()) { it.id } ?: emptySet()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PopIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Recientes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (songs.isNotEmpty()) {
                    Text(
                        text = "Borrar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                RecentStore.clear(context)
                                songs = emptyList()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            if (songs.isEmpty()) {
                EmptyState(
                    title = "Sin historial todavía",
                    subtitle = "Las canciones que escuches aparecerán aquí.",
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            } else {
                Spacer(Modifier.size(8.dp))
                SectionHeader(
                    title = "Escuchado recientemente",
                    playAll = true,
                    onPlayAll = { onPlay(songs, 0) },
                )
                Spacer(Modifier.size(8.dp))
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 170.dp),
                ) {
                    item(key = "grouped") {
                        GroupedSongList(
                            songs = songs,
                            favoriteIds = favoriteIds,
                            onPlay = { index -> onPlay(songs, index) },
                        )
                    }
                }
            }
        }
    }
}
