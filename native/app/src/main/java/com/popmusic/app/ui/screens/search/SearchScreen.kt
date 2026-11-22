package com.popmusic.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.components.GroupedSongList
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.components.SectionHeader
import com.popmusic.app.ui.components.TrackRow
import com.popmusic.app.ui.components.enterOnVisible
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.pressScale

@Composable
fun SearchScreen(
    onPlay: (List<Song>, Int) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    /** Abre la playlist de grandes éxitos del artista tocado. */
    onOpenArtist: (String) -> Unit = {},
    /** Flecha de retroceso (tanda 5, ítem 7). */
    onBack: () -> Unit = {},
    viewModel: SearchViewModel = viewModel(),
) {
    val query = viewModel.query.trim()
    val searching = query.isNotEmpty()
    val results = viewModel.filtered(viewModel.results)
    // Corazón de favorito junto al nombre (ítem 4 de la tanda 3).
    val library by viewModel.library.collectAsState()
    val favoriteIds = remember(library) { library.favorites.mapTo(HashSet()) { it.id } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Volver",
                onClick = onBack,
            )
            SearchPill(viewModel = viewModel, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))

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
                        Text(
                            text = "!",
                            color = Danger,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    },
                )
            }

            searching && results.isEmpty() -> {
                EmptyState(
                    title = "Sin resultados",
                    subtitle = if (viewModel.source == SearchSource.LOCAL) {
                        "No hay coincidencias en tu biblioteca."
                    } else {
                        "Prueba con otro título, artista o álbum."
                    },
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

            searching -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 40.dp),
                ) {
                    if (viewModel.artistMatches.isNotEmpty()) {
                        item(key = "artist-cards") {
                            Text(
                                text = "Artistas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(
                                    viewModel.artistMatches,
                                    key = { (name, _) -> name },
                                ) { (name, song) ->
                                    ArtistResultCard(
                                        name = name,
                                        artworkUrl = song.thumbnailUrl,
                                        onClick = { onOpenArtist(name) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    itemsIndexed(results, key = { _, song -> song.id }) { index, song ->
                        TrackRow(
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.thumbnailUrl,
                            liked = song.id in favoriteIds,
                            songId = song.id,
                            modifier = Modifier.animateItem(),
                            onClick = {
                                onPlay(results, index)
                                onOpenPlayer()
                            },
                        )
                    }
                }
            }

            viewModel.source == SearchSource.LOCAL -> {
                // Sin query en modo biblioteca: no tiene sentido mostrar el chart.
                EmptyState(
                    title = "Busca en tu biblioteca",
                    subtitle = "Escribe un título, artista o álbum de tus favoritos, descargas o playlists.",
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = Faint,
                        )
                    },
                )
            }

            else -> {
                BrowseContent(
                    viewModel = viewModel,
                    onPlay = onPlay,
                    onOpenPlayer = onOpenPlayer,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Barra de búsqueda en píldora con lupa a la izquierda y toggle de fuente
 * a la derecha (globo = "Global", nota = biblioteca), igual que Echo
 * (`SearchSource`).
 */
@Composable
private fun SearchPill(viewModel: SearchViewModel, modifier: Modifier = Modifier) {
    val online = viewModel.source == SearchSource.ONLINE

    OutlinedTextField(
        value = viewModel.query,
        onValueChange = viewModel::onQueryChange,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        placeholder = {
            Text(if (online) "Buscar…" else "Buscar en tu biblioteca…")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Faint,
            )
        },
        trailingIcon = {
            IconButton(onClick = viewModel::toggleSource) {
                Icon(
                    imageVector = if (online) Icons.Rounded.Public else Icons.Rounded.LibraryMusic,
                    contentDescription = if (online) {
                        "Fuente: Global. Toca para buscar en tu biblioteca"
                    } else {
                        "Fuente: Biblioteca. Toca para buscar global"
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
        ),
    )
}

/** Contenido "sin búsqueda": chart paginado + artistas + recomendados. */
@Composable
private fun BrowseContent(
    viewModel: SearchViewModel,
    onPlay: (List<Song>, Int) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chartSongs = viewModel.chartSlice()
    val chartTotal = viewModel.chartPageCount
    val page = viewModel.chartPage
    val artists = viewModel.trendingArtists
    val chartList = viewModel.chartList
    val forYou = viewModel.filtered(viewModel.forYou)
    // Corazón de favorito junto al nombre (ítem 4 de la tanda 3).
    val library by viewModel.library.collectAsState()
    val favoriteIds = remember(library) { library.favorites.mapTo(HashSet()) { it.id } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        if (viewModel.isChartLoading && viewModel.trending.isEmpty()) {
            item(key = "chart-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        } else if (chartSongs.isEmpty()) {
            item(key = "chart-empty") {
                Text(
                    text = "No se pudieron cargar las tendencias.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                )
            }
        } else {
            item(key = "chart-header") {
                Column(modifier = Modifier.enterOnVisible()) {
                    Text(
                        text = "Tendencias",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            item(key = "chart-list") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                ) {
                    chartSongs.forEachIndexed { index, song ->
                        ChartRow(
                            rank = page * CHART_PAGE_SIZE + index + 1,
                            song = song,
                            modifier = Modifier.enterOnVisible(
                                delayMillis = (index * 18).coerceAtMost(144),
                            ),
                            onClick = {
                                onPlay(chartList, page * CHART_PAGE_SIZE + index)
                                onOpenPlayer()
                            },
                        )
                        if (index != chartSongs.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 76.dp, end = 14.dp)
                                    .height(1.dp)
                                    .background(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    ),
                            )
                        }
                    }
                }
            }

            if (chartTotal > 1) {
                item(key = "chart-pagination") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = { viewModel.goToChartPage(page - 1) },
                                    enabled = page > 0,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronLeft,
                                        contentDescription = "Página anterior",
                                        tint = if (page > 0) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            Faint
                                        },
                                    )
                                }
                                Text(
                                    text = "${page + 1} de $chartTotal",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                                IconButton(
                                    onClick = { viewModel.goToChartPage(page + 1) },
                                    enabled = page < chartTotal - 1,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = "Página siguiente",
                                        tint = if (page < chartTotal - 1) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            Faint
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (artists.isNotEmpty()) {
                item(key = "artists-header") {
                    Column(modifier = Modifier.enterOnVisible()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Artistas en tendencia",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
                item(key = "artists-row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(artists, key = { it.first }) { (artist, song) ->
                            ArtistTrendCard(
                                rank = artists.indexOfFirst { it.first == artist } + 1,
                                artist = artist,
                                artworkUrl = song.thumbnailUrl,
                                onClick = {
                                    onPlay(listOf(song), 0)
                                    onOpenPlayer()
                                },
                            )
                        }
                    }
                }
            }

            if (forYou.isNotEmpty()) {
                item(key = "foryou-header") {
                    Column(
                        modifier = Modifier
                            .padding(top = 26.dp)
                            .enterOnVisible(),
                    ) {
                        SectionHeader(
                            title = "Recomendado para ti",
                            playAll = true,
                            onPlayAll = {
                                onPlay(forYou, 0)
                                onOpenPlayer()
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Canciones elegidas según tus favoritos y playlists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Faint,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                item(key = "foryou-list") {
                    GroupedSongList(
                        songs = forYou,
                        favoriteIds = favoriteIds,
                        onPlay = { index ->
                            onPlay(forYou, index)
                            onOpenPlayer()
                        },
                    )
                }
            }
        }
    }
}

/** Fila numerada del chart: rango, carátula y "artista · duración". */
@Composable
private fun ChartRow(
    rank: Int,
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Artwork(
            url = song.thumbnailUrl,
            modifier = Modifier.size(52.dp),
            cornerRadius = 12.dp,
            iconSize = 22.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = buildString {
                append(song.artist)
                val duration = formatDuration(song.durationSeconds)
                if (duration.isNotEmpty()) append("  ·  ").append(duration)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Tarjeta de artista con insignia de ranking, como en Echo. */
@Composable
private fun ArtistTrendCard(
    rank: Int,
    artist: String,
    artworkUrl: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .enterOnVisible()
            .clickable(onClick = onClick),
    ) {
        Box {
            Artwork(
                url = artworkUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                cornerRadius = 18.dp,
                iconSize = 40.dp,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = artist,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Tarjeta de artista en los resultados de búsqueda. */
@Composable
private fun ArtistResultCard(
    name: String,
    artworkUrl: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(
            url = artworkUrl,
            modifier = Modifier.size(72.dp),
            cornerRadius = 36.dp,
            iconSize = 26.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Grandes éxitos",
            style = MaterialTheme.typography.labelSmall,
            color = Faint,
            maxLines = 1,
        )
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return ""
    val minutes = seconds / 60
    val rest = seconds % 60
    return "$minutes:${rest.toString().padStart(2, '0')}"
}

private const val CHART_PAGE_SIZE = 5
