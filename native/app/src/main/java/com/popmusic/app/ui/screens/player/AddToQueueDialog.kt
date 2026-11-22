package com.popmusic.app.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.popmusic.app.data.filter.ContentFilters
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassDialogHeader
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Busca una canción y la añade a la cola actual sin salir del reproductor. */
@Composable
fun AddToQueueDialog(
    onDismiss: () -> Unit,
    onAdd: (Song) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val repository = remember { YouTubeRepository() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    fun runSearch() {
        val term = query.trim()
        if (term.isBlank()) return
        scope.launch {
            searching = true
            searched = true
            results = withContext(Dispatchers.IO) {
                runCatching {
                    ContentFilters.musicOnly(repository.search(term)).take(20)
                }.getOrDefault(emptyList())
            }
            searching = false
        }
    }

    GlassDialog(onDismiss = onDismiss) {
        GlassDialogHeader(
            title = "Añadir a la cola",
            subtitle = "Busca una canción y suena a continuación.",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(16.dp))
        GlassTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Buscar canción…",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Faint,
                    modifier = Modifier.size(20.dp),
                )
            },
            imeAction = ImeAction.Search,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { runSearch() },
            ),
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp, max = 330.dp),
        ) {
            when {
                searching -> {
                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Ink)
                    }
                }

                results.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                        Text(
                            text = if (searched) {
                                "Sin resultados. Prueba con otro título o artista."
                            } else {
                                "Escribe un título o artista para buscar."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Faint,
                        )
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(results, key = { it.id }) { song ->
                            val interaction = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = ripple(bounded = true),
                                    ) {
                                        onAdd(song)
                                    }
                                    .pressScale(interaction, pressedScale = 0.97f)
                                    .padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Ink,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Faint,
                                        maxLines = 1,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Añadir a la cola",
                                    tint = Ink,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassDialogActions {
            GlassTextButton(
                text = "Cancelar",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            GlassPrimaryButton(
                text = "Buscar",
                onClick = { runSearch() },
                modifier = Modifier.weight(1f),
                enabled = query.isNotBlank() && !searching,
            )
        }
    }
}
