package com.popmusic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.popmusic.app.player.PlayerViewModel
import com.popmusic.app.ui.components.MiniPlayer
import com.popmusic.app.ui.navigation.PoPBottomBar
import com.popmusic.app.ui.navigation.Routes
import com.popmusic.app.ui.screens.home.HomeScreen
import com.popmusic.app.ui.screens.library.LibraryScreen
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
    val isLoading by playerViewModel.isLoading.collectAsState()

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
                    onOpenPlaylist = {
                        navController.navigate(Routes.playlist("favoritos"))
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
                )
            }
            composable(Routes.NOW_PLAYING) {
                NowPlayingScreen(
                    state = playerState,
                    isLoading = isLoading,
                    onBack = { navController.popBackStack() },
                    onTogglePlayPause = { playerViewModel.togglePlayPause() },
                    onNext = { playerViewModel.next() },
                    onPrevious = { playerViewModel.previous() },
                    onSeek = { playerViewModel.seekTo(it) },
                    onToggleShuffle = { playerViewModel.setShuffle(!playerState.shuffle) },
                    onCycleRepeat = { cycleRepeatMode() },
                    onOpenLyrics = { navController.navigate(Routes.LYRICS) },
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
}
