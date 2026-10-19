package com.popmusic.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.components.TrackRow
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.LineStrong
import com.popmusic.app.ui.theme.Surface2

@Composable
fun SearchScreen(
    onPlay: (List<Song>, Int) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Buscar",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Canciones, artistas, álbumes…") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Faint,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface2,
                unfocusedContainerColor = Surface2,
                focusedBorderColor = LineStrong,
                unfocusedBorderColor = LineStrong,
            ),
        )

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Pop", "Rock", "Chill", "Latino").forEach { tag ->
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Surface2, RoundedCornerShape(50))
                        .clickable {
                            viewModel.onQueryChange(tag)
                            viewModel.search()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            viewModel.error != null -> {
                EmptyState(
                    title = "No se pudo buscar",
                    subtitle = viewModel.error ?: "",
                    modifier = Modifier.weight(1f),
                    icon = {
                        Text(text = "!", color = Danger, style = MaterialTheme.typography.headlineLarge)
                    },
                )
            }

            viewModel.results.isEmpty() -> {
                EmptyState(
                    title = "Busca en YouTube Music",
                    subtitle = "Encuentra canciones completas, sin anuncios, y ábrelas al instante.",
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Faint,
                        )
                    },
                )
            }

            else -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(viewModel.results) { index, song ->
                        TrackRow(
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.thumbnailUrl,
                            onClick = {
                                onPlay(viewModel.results, index)
                                onOpenPlayer()
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
