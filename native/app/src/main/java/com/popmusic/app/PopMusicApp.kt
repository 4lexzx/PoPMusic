package com.popmusic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import com.popmusic.app.player.PlayerViewModel
import com.popmusic.app.ui.components.MiniPlayer
import com.popmusic.app.ui.navigation.PoPBottomBar
import com.popmusic.app.ui.navigation.Routes
import com.popmusic.app.ui.screens.home.HomeScreen
import com.popmusic.app.ui.screens.library.LibraryScreen
import com.popmusic.app.ui.screens.library.LibraryViewModel
import com.popmusic.app.ui.screens.lyrics.LyricsScreen
import com.popmusic.app.ui.screens.player.NowPlayingScreen
import com.popmusic.app.ui.screens.playlist.PlaylistScreen
import com.popmusic.app.ui.screens.recognize.RecognizeScreen
import com.popmusic.app.ui.screens.search.SearchScreen
import com.popmusic.app.ui.screens.settings.SettingsScreen
import com.popmusic.app.ui.theme.Black

@Composable
fun PopMusicApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isFullScreen = currentRoute == Routes.NOW_PLAYING || currentRoute == Routes.LYRICS

    val playerViewModel: PlayerViewModel = viewModel()
    val playerState by playerViewModel.state.collectAsState()

    val libraryViewModel: LibraryViewModel = viewModel()
    val libraryState by libraryViewModel.state.collectAsState()

    val currentSong = remember(playerState.mediaId, playerState.title, playerState.artist) {
        if (playerState.mediaId.isBlank()) {
            null
        } else {
            Song(
                id = playerState.mediaId,
                title = playerState.title,
                artist = playerState.artist,
                thumbnailUrl = playerState.artworkUrl,
            )
        }
    }
    val isFavorite = currentSong != null && libraryState.favorites.any { it.id == currentSong.id }

    var showAddToPlaylist by remember { mutableStateOf(false) }

    fun openPlayer() {
        navController.navigate(Routes.NOW_PLAYING) {
            launchSingleTop = true
        }
    }

    fun cycleRepeatMode() {
        val next = when (playerState.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        playerViewModel.setRepeatMode(next)
    }

    Scaffold(
        containerColor = Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isFullScreen) {
                Column(modifier = Modifier.background(Black)) {
                    MiniPlayer(
                        title = playerState.title.ifBlank { "Sin reproducción" },
                        artist = playerState.artist.ifBlank { "Busca una canción para empezar" },
                        artworkUrl = playerState.artworkUrl,
                        isPlaying = playerState.isPlaying,
                        onTogglePlay = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.next() },
                        onClick = { openPlayer() },
                    )
                    Spacer(Modifier.height(6.dp))
                    PoPBottomBar(currentRoute = currentRoute) { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenPlayer = { openPlayer() },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onPlay = { songs, startIndex ->
                        playerViewModel.play(songs, startIndex)
                        openPlayer()
                    },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onPlay = { songs, startIndex ->
                        playerViewModel.play(songs, startIndex)
                    },
                    onOpenPlayer = { openPlayer() },
                )
            }
            composable(Routes.RECOGNIZE) {
                RecognizeScreen(
                    onFound = { song ->
                        playerViewModel.play(listOf(song), 0)
                        openPlayer()
                    },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenPlaylist = { id ->
                        navController.navigate(Routes.playlist(id))
                    },
                )
            }
            composable(
                route = Routes.PLAYLIST,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
            ) { entry ->
                PlaylistScreen(
                    playlistId = entry.arguments?.getString("playlistId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onPlay = { songs, startIndex ->
                        playerViewModel.play(songs, startIndex)
                        openPlayer()
                    },
                )
            }
            composable(Routes.NOW_PLAYING) {
                NowPlayingScreen(
                    state = playerState,
                    isFavorite = isFavorite,
                    onToggleFavorite = { currentSong?.let(libraryViewModel::toggleFavorite) },
                    onBack = { navController.popBackStack() },
                    onTogglePlayPause = { playerViewModel.togglePlayPause() },
                    onNext = { playerViewModel.next() },
                    onPrevious = { playerViewModel.previous() },
                    onSeek = { playerViewModel.seekTo(it) },
                    onToggleShuffle = { playerViewModel.setShuffle(!playerState.shuffle) },
                    onCycleRepeat = { cycleRepeatMode() },
                    onOpenLyrics = { navController.navigate(Routes.LYRICS) },
                    onAddToPlaylist = { showAddToPlaylist = true },
                )
            }
            composable(Routes.LYRICS) {
                LyricsScreen(
                    title = playerState.title,
                    artist = playerState.artist,
                    positionMs = playerState.positionMs,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            playlists = libraryState.playlists,
            onDismiss = { showAddToPlaylist = false },
            onSelect = { playlistId ->
                currentSong?.let { libraryViewModel.addToPlaylist(playlistId, it) }
                showAddToPlaylist = false
            },
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a playlist") },
        text = {
            Column {
                PlaylistOption(
                    name = "Canciones favoritas",
                    onClick = { onSelect(LibraryRepository.FAVORITES_ID) },
                )
                playlists.forEach { playlist ->
                    PlaylistOption(
                        name = playlist.name,
                        onClick = { onSelect(playlist.id) },
                    )
                }
                if (playlists.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Crea playlists en la Biblioteca.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
private fun PlaylistOption(name: String, onClick: () -> Unit) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
