package com.popmusic.app.ui.screens.library

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.data.music.MusicService
import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.ArtworkCollage
import com.popmusic.app.ui.components.BrandIcons
import com.popmusic.app.ui.components.boldName
import com.popmusic.app.ui.components.boldQuoted
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.components.GlassActionRow
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassDialogHeader
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.components.SolidMenu
import com.popmusic.app.ui.components.SolidMenuDivider
import com.popmusic.app.ui.components.SolidMenuRow
import com.popmusic.app.ui.components.copyAndOpenSpotifyExport
import com.popmusic.app.ui.components.spotifyExportText
import com.popmusic.app.ui.components.SyncTargetSelector
import com.popmusic.app.ui.components.TrackRow
import com.popmusic.app.ui.components.TopBarIcon
import com.popmusic.app.ui.components.enterOnVisible
import com.popmusic.app.ui.glass.backdrop.backdrops.LayerBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.layerBackdrop
import com.popmusic.app.ui.navigation.LocalNavCollapsed
import com.popmusic.app.ui.theme.Accent
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.pressScale
import kotlinx.coroutines.delay

private enum class LibraryDialog { ADD, CREATE, IMPORT, IMPORT_ACCOUNT }

private enum class LibraryFilter(val label: String) {
    HOME("Todo"),
    SONGS("Canciones"),
    ARTISTS("Artistas"),
    PLAYLISTS("Playlists"),
    DOWNLOADS("Descargas"),
}

@Composable
fun LibraryScreen(
    onOpenPlaylist: (String) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenParty: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    contentBackdrop: LayerBackdrop? = null,
    miniVisible: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val context = LocalContext.current

    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }
    var newName by remember { mutableStateOf("") }
    var importLink by remember { mutableStateOf("") }
    // Importar: primero se elige la plataforma y después se pega un solo enlace.
    var importPlatform by remember { mutableStateOf<MusicService?>(null) }
    // Menú ⋯ por playlist (ítem 10) y confirmación de borrado en rojo (ítem 12).
    var confirmDelete by remember { mutableStateOf<Playlist?>(null) }
    // Ítem 1: cambiar nombre de la playlist (con propagación a las plataformas).
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }
    // Exportar a Spotify sin OAuth: se copia la lista y se abre TuneMyMusic.
    var spotifyExportTarget by remember { mutableStateOf<Playlist?>(null) }
    var fabVisible by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.HOME) }

    // Ítem 3: elección de sincronización al crear una playlist (chips del
    // diálogo CREATE). "NONE" = solo en el teléfono; null = ambas plataformas.
    var newSyncTarget by remember { mutableStateOf<String?>("NONE") }
    // Tanda 12: "Sincronizar…" desde el menú ⋯ de una playlist no vinculada.
    var syncPlaylistTarget by remember { mutableStateOf<Playlist?>(null) }
    var syncPlaylistChoice by remember { mutableStateOf<String?>(null) }
    val syncState by viewModel.syncState.collectAsState()
    // Spotify ya no se conecta por cuenta: aunque quede una sesión antigua en
    // preferencias, no se ofrece sincronizar con ella.
    val connectedServices = syncState.filterValues { it.connected }.keys
        .filter { it != MusicService.SPOTIFY }
        .toList()

    // Ítem 6: el FAB no tiene su propio hide-on-scroll; baja junto con la
    // barra de navegación cuando esta se colapsa por scroll real.
    val navCollapsed = LocalNavCollapsed.current

    // Avisos de sincronización (exportar/importar a YouTube o Spotify).
    val syncEvents by viewModel.syncEvents.collectAsState()
    LaunchedEffect(syncEvents) {
        val message = syncEvents
        if (message != null) {
            Toast.makeText(context, message.boldQuoted(), Toast.LENGTH_SHORT).show()
            viewModel.clearSyncEvent()
        }
    }

    // Estados de scroll por pestaña (compartidos con las listas de abajo).
    val homeGridState = rememberLazyGridState()
    val songsListState = rememberLazyListState()
    val downloadsListState = rememberLazyListState()
    val artistsGridState = rememberLazyGridState()
    val playlistsGridState = rememberLazyGridState()

    LaunchedEffect(Unit) { fabVisible = true }

    val allSongs = remember(state.favorites, state.downloads, state.playlists) {
        (state.favorites + state.downloads + state.playlists.flatMap { it.songs })
            .distinctBy { it.id }
    }
    // Corazón de favorito junto al nombre (ítem 4).
    val favoriteIds = remember(state.favorites) {
        state.favorites.mapTo(HashSet()) { it.id }
    }
    val artists = remember(allSongs) {
        // "Maná - Topic" y "Maná" son el mismo artista.
        allSongs.filter { it.artist.isNotBlank() }
            .groupBy { it.artist.trim().removeSuffix(" - Topic").trim().lowercase() }
            .map { (_, songs) ->
                songs.first().artist.trim().removeSuffix(" - Topic").trim() to songs
            }
            .sortedBy { it.first.lowercase() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .then(
                    if (contentBackdrop != null) {
                        Modifier.layerBackdrop(contentBackdrop)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biblioteca",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = librarySummary(
                            favorites = state.favorites.size,
                            playlists = state.playlists.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Faint,
                    )
                }
                TopBarIcon(
                    icon = Icons.Rounded.History,
                    contentDescription = "Recientes",
                    onClick = onOpenHistory,
                )
                // La lupa se quitó: la barra inferior ya tiene Buscar.
                Spacer(Modifier.width(2.dp))
                TopBarIcon(
                    icon = Icons.Rounded.Groups,
                    contentDescription = "Salas",
                    onClick = onOpenParty,
                )
                Spacer(Modifier.width(2.dp))
                TopBarIcon(
                    icon = Icons.Rounded.Person,
                    contentDescription = "Cuenta",
                    onClick = onOpenAccount,
                )
                Spacer(Modifier.width(2.dp))
                TopBarIcon(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Ajustes",
                    onClick = onOpenSettings,
                )
            }
            Spacer(Modifier.height(12.dp))
            LibraryFilterChips(selected = filter, onSelect = { filter = it })
            Spacer(Modifier.height(14.dp))

            when (filter) {
                LibraryFilter.HOME -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = homeGridState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 170.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "favorites") {
                        QuickAccessCard(
                            label = "Favoritos",
                            icon = Icons.Rounded.Favorite,
                            onClick = { onOpenPlaylist(LibraryRepository.FAVORITES_ID) },
                        )
                    }
                    item(key = "downloads") {
                        QuickAccessCard(
                            label = "Descargas",
                            icon = Icons.Rounded.Download,
                            onClick = { onOpenPlaylist(LibraryRepository.DOWNLOADS_ID) },
                        )
                    }
                    item(key = "songs") {
                        QuickAccessCard(
                            label = "Canciones",
                            icon = Icons.Rounded.MusicNote,
                            onClick = { filter = LibraryFilter.SONGS },
                        )
                    }
                    item(key = "artists") {
                        QuickAccessCard(
                            label = "Artistas",
                            icon = Icons.Rounded.Person,
                            onClick = { filter = LibraryFilter.ARTISTS },
                        )
                    }
                    item(key = "playlists") {
                        QuickAccessCard(
                            label = "Playlists",
                            icon = Icons.Rounded.QueueMusic,
                            onClick = { filter = LibraryFilter.PLAYLISTS },
                        )
                    }
                    item(key = "import") {
                        QuickAccessCard(
                            label = "Importar",
                            icon = Icons.Rounded.Link,
                            onClick = {
                                importLink = ""
                                importPlatform = null
                                viewModel.resetImportState()
                                dialog = LibraryDialog.IMPORT
                            },
                        )
                    }

                    item(key = "playlists-header", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Playlists",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = state.playlists.size.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Faint,
                            )
                        }
                    }

                    if (state.playlists.isEmpty()) {
                        item(key = "empty-playlists", span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                title = "Aún no tienes playlists",
                                subtitle = "Crea una o impórtala desde un enlace.",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryMusic,
                                        contentDescription = null,
                                        tint = Faint,
                                    )
                                },
                            )
                        }
                    } else {
                        items(state.playlists, key = { it.id }) { playlist ->
                            LibraryCard(
                                covers = playlist.coverCovers(),
                                title = playlist.name,
                                subtitle = songCount(playlist.songs.size),
                                onClick = { onOpenPlaylist(playlist.id) },
                                menu = { dismiss ->
                                    // Tanda 12: solo en playlists que todavía
                                    // no están vinculadas a ninguna plataforma.
                                    if (playlist.external.isEmpty() && connectedServices.isNotEmpty()) {
                                        SolidMenuRow(
                                            icon = Icons.Rounded.Sync,
                                            label = "Sincronizar…",
                                            onClick = {
                                                syncPlaylistChoice = playlist.syncTarget
                                                    ?.takeIf { it != "NONE" }
                                                    ?: if (connectedServices.size >= 2) null
                                                    else connectedServices.firstOrNull()?.name
                                                syncPlaylistTarget = playlist
                                                dismiss()
                                            },
                                        )
                                    }
                                    SolidMenuRow(
                                        icon = BrandIcons.YouTube, iconTint = Color.Unspecified,
                                        label = "Exportar a YouTube",
                                        onClick = {
                                            viewModel.exportPlaylist(
                                                playlist.id,
                                                MusicService.YOUTUBE,
                                            )
                                            dismiss()
                                        },
                                    )
                                    SolidMenuRow(
                                        icon = BrandIcons.Spotify, iconTint = Color.Unspecified,
                                        label = "Exportar a Spotify",
                                        onClick = {
                                            spotifyExportTarget = playlist
                                            dismiss()
                                        },
                                    )
                                    SolidMenuRow(
                                        icon = Icons.Rounded.Edit,
                                        label = "Cambiar nombre",
                                        onClick = {
                                            renameTarget = playlist
                                            renameText = playlist.name
                                            dismiss()
                                        },
                                    )
                                    SolidMenuDivider()
                                    SolidMenuRow(
                                        icon = Icons.Rounded.Delete,
                                        label = "Eliminar playlist",
                                        tint = Danger,
                                        onClick = {
                                            confirmDelete = playlist
                                            dismiss()
                                        },
                                    )
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }

                LibraryFilter.SONGS -> LibrarySongList(
                    songs = allSongs,
                    favoriteIds = favoriteIds,
                    onPlay = onPlay,
                    emptyTitle = "Aún no tienes canciones",
                    emptySubtitle = "Marca favoritos o descarga música para verla aquí.",
                    listState = songsListState,
                    modifier = Modifier.weight(1f),
                )

                LibraryFilter.DOWNLOADS -> LibrarySongList(
                    songs = state.downloads,
                    favoriteIds = favoriteIds,
                    onPlay = onPlay,
                    emptyTitle = "Sin descargas",
                    emptySubtitle = "Descarga canciones desde el reproductor para escucharlas sin conexión.",
                    listState = downloadsListState,
                    modifier = Modifier.weight(1f),
                )

                LibraryFilter.ARTISTS -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = artistsGridState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 170.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (artists.isEmpty()) {
                        item(key = "empty-artists", span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                title = "Sin artistas todavía",
                                subtitle = "Los artistas de tus canciones aparecerán aquí.",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = Faint,
                                    )
                                },
                            )
                        }
                    } else {
                        items(artists, key = { it.first }) { (artist, songs) ->
                            LibraryArtist(
                                name = artist,
                                fallback = songs.firstNotNullOfOrNull { it.thumbnailUrl },
                                subtitle = songCount(songs.size),
                                onClick = { onPlay(songs, 0) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }

                LibraryFilter.PLAYLISTS -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = playlistsGridState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 170.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.playlists.isEmpty()) {
                        item(key = "empty-playlists-tab", span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                title = "Aún no tienes playlists",
                                subtitle = "Crea una o impórtala desde un enlace.",
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
                        items(state.playlists, key = { it.id }) { playlist ->
                            LibraryCard(
                                covers = playlist.coverCovers(),
                                title = playlist.name,
                                subtitle = songCount(playlist.songs.size),
                                onClick = { onOpenPlaylist(playlist.id) },
                                menu = { dismiss ->
                                    // Tanda 12: solo en playlists que todavía
                                    // no están vinculadas a ninguna plataforma.
                                    if (playlist.external.isEmpty() && connectedServices.isNotEmpty()) {
                                        SolidMenuRow(
                                            icon = Icons.Rounded.Sync,
                                            label = "Sincronizar…",
                                            onClick = {
                                                syncPlaylistChoice = playlist.syncTarget
                                                    ?.takeIf { it != "NONE" }
                                                    ?: if (connectedServices.size >= 2) null
                                                    else connectedServices.firstOrNull()?.name
                                                syncPlaylistTarget = playlist
                                                dismiss()
                                            },
                                        )
                                    }
                                    SolidMenuRow(
                                        icon = BrandIcons.YouTube, iconTint = Color.Unspecified,
                                        label = "Exportar a YouTube",
                                        onClick = {
                                            viewModel.exportPlaylist(
                                                playlist.id,
                                                MusicService.YOUTUBE,
                                            )
                                            dismiss()
                                        },
                                    )
                                    SolidMenuRow(
                                        icon = BrandIcons.Spotify, iconTint = Color.Unspecified,
                                        label = "Exportar a Spotify",
                                        onClick = {
                                            spotifyExportTarget = playlist
                                            dismiss()
                                        },
                                    )
                                    SolidMenuRow(
                                        icon = Icons.Rounded.Edit,
                                        label = "Cambiar nombre",
                                        onClick = {
                                            renameTarget = playlist
                                            renameText = playlist.name
                                            dismiss()
                                        },
                                    )
                                    SolidMenuDivider()
                                    SolidMenuRow(
                                        icon = Icons.Rounded.Delete,
                                        label = "Eliminar playlist",
                                        tint = Danger,
                                        onClick = {
                                            confirmDelete = playlist
                                            dismiss()
                                        },
                                    )
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            // Ítem 6: el FAB NO se esconde con la nav (antes lo hacía con
            // `!navCollapsed` y desaparecía al hacer scroll); solo se mueve.
            visible = fabVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // Responsive: justo encima de la barra de nav (6+56+6 = 68 dp);
                // con mini reproductor sube por encima (+64 dp). En modo inline
                // la barra es una sola fila de 56 dp (el mini va dentro), así
                // que basta con el 80 base.
                .navigationBarsPadding()
                .padding(
                    end = 20.dp,
                    bottom = (if (miniVisible && !navCollapsed) 144 else 80).dp,
                ),
        ) {
            AddFab(onClick = { dialog = LibraryDialog.ADD })
        }
    }

    if (dialog == LibraryDialog.ADD) {
        GlassDialog(onDismiss = { dialog = null }) {
            // Ítem 5 (tanda 3): diálogo "+" estilo Echo, con Crear / Importar
            // desde enlace / Importar desde una cuenta vinculada.
            GlassDialogHeader(
                title = "Añadir a tu biblioteca",
                subtitle = "Crea una lista nueva o impórtala desde Spotify o YouTube.",
                onClose = { dialog = null },
            )
            Spacer(Modifier.height(18.dp))
            GlassActionRow(
                icon = Icons.Rounded.Add,
                title = "Crear playlist",
                subtitle = "Empieza una lista desde cero",
                onClick = {
                    newName = ""
                    newSyncTarget = "NONE"
                    dialog = LibraryDialog.CREATE
                },
            )
            Spacer(Modifier.height(10.dp))
            GlassActionRow(
                icon = Icons.Rounded.Link,
                title = "Importar desde enlace",
                subtitle = "Spotify y YouTube: pega el enlace de la playlist",
                onClick = {
                    importLink = ""
                    importPlatform = null
                    viewModel.resetImportState()
                    dialog = LibraryDialog.IMPORT
                },
            )
            Spacer(Modifier.height(10.dp))
            GlassActionRow(
                icon = Icons.Rounded.Sync,
                title = "Importar desde tu cuenta",
                subtitle = "YouTube o Spotify (vincúlalas en Ajustes)",
                onClick = { dialog = LibraryDialog.IMPORT_ACCOUNT },
            )
        }
    }

    if (dialog == LibraryDialog.IMPORT_ACCOUNT) {
        GlassDialog(onDismiss = { dialog = null }) {
            GlassDialogHeader(
                title = "Importar desde tu cuenta",
                subtitle = "Elige el servicio que tiene tus playlists.",
                onClose = { dialog = null },
            )
            Spacer(Modifier.height(18.dp))
            GlassActionRow(
                icon = BrandIcons.YouTube, iconTint = Color.Unspecified,
                title = MusicService.YOUTUBE.displayName(),
                subtitle = "Trae tus playlists de YouTube",
                onClick = {
                    viewModel.importFromService(MusicService.YOUTUBE)
                    dialog = null
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Las playlists de Spotify se traen desde Importar, pegando el enlace.",
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
            )
        }
    }

    if (dialog == LibraryDialog.CREATE) {
        GlassDialog(onDismiss = { dialog = null }) {
            GlassDialogHeader(
                title = "Nueva playlist",
                subtitle = "Dale un nombre a tu lista.",
                onClose = { dialog = null },
            )
            Spacer(Modifier.height(18.dp))
            GlassTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = "Nombre de la playlist",
            )
            // Ítem 3: si hay cuentas de música vinculadas, elegir si la
            // playlist se sube a YouTube/Spotify o se queda solo en el
            // teléfono. Sin cuentas no se muestra nada (todo local).
            if (connectedServices.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SyncTargetSelector(
                    services = connectedServices,
                    selected = newSyncTarget,
                    onSelect = { newSyncTarget = it },
                )
            }
            Spacer(Modifier.height(20.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { dialog = null },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Crear",
                    onClick = {
                        viewModel.createPlaylist(
                            newName,
                            if (connectedServices.isEmpty()) null else newSyncTarget,
                        )
                        dialog = null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = newName.isNotBlank(),
                )
            }
        }
    }

    // Tanda 12: "Sincronizar…" — elegir plataforma o ambas para una
    // playlist que todavía no está vinculada.
    syncPlaylistTarget?.let { targetPlaylist ->
        GlassDialog(onDismiss = { syncPlaylistTarget = null }) {
            GlassDialogHeader(
                title = AnnotatedString("Sincronizar"),
                subtitle = boldName("¿A qué plataforma subir ", targetPlaylist.name, "?"),
                onClose = { syncPlaylistTarget = null },
            )
            Spacer(Modifier.height(18.dp))
            SyncTargetSelector(
                services = connectedServices,
                selected = syncPlaylistChoice,
                onSelect = { syncPlaylistChoice = it },
                showNone = false,
            )
            Spacer(Modifier.height(20.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { syncPlaylistTarget = null },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Sincronizar",
                    onClick = {
                        viewModel.syncPlaylist(targetPlaylist.id, syncPlaylistChoice)
                        syncPlaylistTarget = null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Exportar a Spotify sin cuenta conectada: TuneMyMusic (el servicio que
    // usa el propio Spotify para importar) crea la playlist a partir de una
    // lista de texto. No hace falta Premium ni añadir usuarios a una app.
    spotifyExportTarget?.let { target ->
        GlassDialog(onDismiss = { spotifyExportTarget = null }) {
            GlassDialogHeader(
                title = AnnotatedString("Exportar a Spotify"),
                subtitle = boldName("Se copiará la lista de ", target.name, "."),
                onClose = { spotifyExportTarget = null },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Se abrirá TuneMyMusic. Pega la lista allí, elige Spotify " +
                    "y acepta para crear la playlist en tu cuenta.",
                style = MaterialTheme.typography.bodyMedium,
                color = Faint,
            )
            Spacer(Modifier.height(20.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { spotifyExportTarget = null },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Copiar y abrir",
                    onClick = {
                        copyAndOpenSpotifyExport(context, spotifyExportText(target))
                        spotifyExportTarget = null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (dialog == LibraryDialog.IMPORT) {
        val loading = importState is ImportState.Loading
        val success = importState as? ImportState.Success

        LaunchedEffect(success) {
            if (success != null) {
                delay(700)
                dialog = null
                viewModel.resetImportState()
            }
        }

        GlassDialog(
            onDismiss = {
                if (!loading) {
                    dialog = null
                    viewModel.resetImportState()
                }
            },
            dismissEnabled = !loading,
        ) {
            GlassDialogHeader(
                title = "Importar playlist",
                subtitle = "Elige la plataforma y pega su enlace.",
                onClose = if (loading || success != null) null else {
                    {
                        dialog = null
                        viewModel.resetImportState()
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            val platform = importPlatform
            if (platform == null) {
                Text(
                    text = "¿Desde dónde quieres importar?",
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                )
                Spacer(Modifier.height(12.dp))
                ImportPlatformChoice(
                    icon = BrandIcons.YouTube,
                    name = "YouTube",
                    enabled = !loading && success == null,
                    onClick = {
                        importLink = ""
                        importPlatform = MusicService.YOUTUBE
                    },
                )
                Spacer(Modifier.height(10.dp))
                ImportPlatformChoice(
                    icon = BrandIcons.Spotify,
                    name = "Spotify",
                    enabled = !loading && success == null,
                    onClick = {
                        importLink = ""
                        importPlatform = MusicService.SPOTIFY
                    },
                )
            } else {
                val platformIcon = if (platform == MusicService.YOUTUBE) {
                    BrandIcons.YouTube
                } else {
                    BrandIcons.Spotify
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = platformIcon,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = platform.displayName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Ink,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Cambiar",
                        style = MaterialTheme.typography.labelLarge,
                        color = Accent,
                        modifier = Modifier
                            .clickable(
                                enabled = !loading && success == null,
                                onClick = {
                                    importPlatform = null
                                    importLink = ""
                                },
                            )
                            .padding(8.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                GlassTextField(
                    value = importLink,
                    onValueChange = { importLink = it },
                    placeholder = "Pegar url",
                    enabled = !loading && success == null,
                )
            }
            when (val current = importState) {
                ImportState.Loading -> {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Ink,
                        trackColor = Surface2,
                    )
                }

                is ImportState.Error -> {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Danger,
                    )
                }

                is ImportState.Success -> {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = boldName("Añadida ", current.name, " con ${songCount(current.count)}."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Accent,
                    )
                }

                ImportState.Idle -> Unit
            }
            Spacer(Modifier.height(20.dp))
            if (success != null) {
                GlassPrimaryButton(
                    text = "Hecho",
                    onClick = {
                        dialog = null
                        viewModel.resetImportState()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                GlassDialogActions {
                    GlassTextButton(
                        text = if (platform == null) "Cancelar" else "Atrás",
                        onClick = {
                            if (platform == null) {
                                dialog = null
                                viewModel.resetImportState()
                                importLink = ""
                            } else {
                                importPlatform = null
                                importLink = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading,
                    )
                    if (platform != null) {
                        GlassPrimaryButton(
                            text = if (loading) "Importando…" else "Importar",
                            onClick = { viewModel.importPlaylist(importLink.trim(), platform) },
                            modifier = Modifier.weight(1f),
                            enabled = importLink.isNotBlank() && !loading,
                        )
                    }
                }
            }
        }
    }

    // Ítem 12: eliminar en ROJO con confirmación previa.
    confirmDelete?.let { playlist ->
        GlassDialog(onDismiss = { confirmDelete = null }) {
            GlassDialogHeader(
                title = boldName("¿Eliminar ", playlist.name, "?"),
                subtitle = AnnotatedString("Se quitará de tu biblioteca. Esta acción no se puede deshacer."),
                onClose = { confirmDelete = null },
            )
            Spacer(Modifier.height(20.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { confirmDelete = null },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Eliminar",
                    danger = true,
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        confirmDelete = null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Ítem 1: cambiar nombre de la playlist (con propagación a las plataformas).
    renameTarget?.let { playlist ->
        GlassDialog(onDismiss = { renameTarget = null }) {
            GlassDialogHeader(
                title = "Cambiar nombre",
                subtitle = playlist.name,
                onClose = { renameTarget = null },
            )
            Spacer(Modifier.height(16.dp))
            GlassTextField(
                value = renameText,
                onValueChange = { renameText = it },
                placeholder = "Nuevo nombre",
            )
            Spacer(Modifier.height(20.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { renameTarget = null },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Guardar",
                    onClick = {
                        viewModel.renamePlaylist(playlist.id, renameText)
                        renameTarget = null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = renameText.isNotBlank(),
                )
            }
        }
    }
}

/** Tarjeta plana de acceso rápido: icono + etiqueta, como en Echo. */
@Composable
private fun QuickAccessCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryFilterChips(
    selected: LibraryFilter,
    onSelect: (LibraryFilter) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            LibraryFilter.values().toList(),
            key = { it.name },
        ) { item ->
            val active = item == selected
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                    )
                    .clickable { onSelect(item) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LibrarySongList(
    songs: List<Song>,
    onPlay: (List<Song>, Int) -> Unit,
    emptyTitle: String,
    emptySubtitle: String,
    modifier: Modifier = Modifier,
    favoriteIds: Set<String> = emptySet(),
    listState: LazyListState = rememberLazyListState(),
) {
    if (songs.isEmpty()) {
        EmptyState(
            title = emptyTitle,
            subtitle = emptySubtitle,
            modifier = modifier,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = Faint,
                )
            },
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 170.dp),
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                TrackRow(
                    title = song.title,
                    artist = song.artist,
                    artworkUrl = song.thumbnailUrl,
                    liked = song.id in favoriteIds,
                    songId = song.id,
                    modifier = Modifier.animateItem(),
                    onClick = { onPlay(songs, index) },
                )
            }
        }
    }
}

/** Chip circular con ⋯ en la esquina de la tarjeta de playlist (ítem 10). */
@Composable
private fun MoreChip(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    // Zona táctil de 40 dp con un botón visible pequeño y discreto.
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.88f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 24.dp)
                .clip(CircleShape)
                .background(Color(0xD9101012)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "Opciones de la playlist",
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun LibraryCard(
    covers: List<String?>,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    /** Menú compacto sólido anclado al chip ⋯; recibe el cierre. */
    menu: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .enterOnVisible()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f),
    ) {
        Box {
            ArtworkCollage(
                urls = covers,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                cornerRadius = 20.dp,
                iconSize = 38.dp,
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                ) {
                    badge()
                }
            }
            if (onMenu != null || menu != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp),
                ) {
                    var menuOpen by remember { mutableStateOf(false) }
                    MoreChip(
                        onClick = {
                            if (menu != null) {
                                menuOpen = true
                            } else {
                                onMenu?.invoke()
                            }
                        },
                    )
                    if (menu != null) {
                        SolidMenu(
                            expanded = menuOpen,
                            onDismiss = { menuOpen = false },
                        ) { dismiss ->
                            menu(dismiss)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun AddFab(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    // Estilo Echo: FAB circular sólido (fondo onSurface = Ink, icono surface).
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.90f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Crear o importar playlist",
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun Playlist.coverCovers(): List<String?> =
    songs.map { it.thumbnailUrl }

@Composable
private fun LibraryArtist(
    name: String,
    fallback: String?,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photo = com.popmusic.app.ui.components.rememberArtistPhoto(name)
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .enterOnVisible()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .pressScale(interaction, pressedScale = 0.94f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.popmusic.app.ui.components.Artwork(
            // Mientras se busca la foto no se enseña la carátula: no parpadea.
            url = photo.url ?: fallback.takeIf { !photo.loading },
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape),
            cornerRadius = 200.dp,
            iconSize = 34.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Faint,
            maxLines = 1,
        )
    }
}

private fun librarySummary(favorites: Int, playlists: Int): String {
    val parts = buildList {
        if (playlists > 0) add(if (playlists == 1) "1 playlist" else "$playlists playlists")
        if (favorites > 0) add(if (favorites == 1) "1 favorita" else "$favorites favoritas")
    }
    return parts.joinToString(" · ").ifBlank { "Tu música aparecerá aquí" }
}

private fun songCount(count: Int): String = when (count) {
    0 -> "Sin canciones"
    1 -> "1 canción"
    else -> "$count canciones"
}

/**
 * Opción de plataforma en el primer paso del import: logo oficial + nombre.
 * Al elegirla se muestra un único campo «Pegar url» para esa plataforma.
 */
@Composable
private fun ImportPlatformChoice(
    icon: ImageVector,
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface2.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            // Sin tinte: los iconos de marca conservan sus colores oficiales.
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (enabled) Ink else Faint,
        )
    }
}
