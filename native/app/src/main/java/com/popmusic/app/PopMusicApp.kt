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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.popmusic.app.ui.components.MiniPlayer
import com.popmusic.app.ui.navigation.PoPBottomBar
import com.popmusic.app.ui.navigation.Routes
import com.popmusic.app.ui.screens.home.HomeScreen
import com.popmusic.app.ui.screens.library.LibraryScreen
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
    val isNowPlaying = currentRoute == Routes.NOW_PLAYING
    var isPlaying by remember { mutableStateOf(true) }

    fun openPlayer() {
        navController.navigate(Routes.NOW_PLAYING) {
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isNowPlaying) {
                Column(modifier = Modifier.background(Black)) {
                    MiniPlayer(
                        title = "Sin señal",
                        artist = "PoP Music",
                        artworkUrl = null,
                        isPlaying = isPlaying,
                        onTogglePlay = { isPlaying = !isPlaying },
                        onNext = {},
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
                SearchScreen(onOpenPlayer = { openPlayer() })
            }
            composable(Routes.RECOGNIZE) {
                RecognizeScreen()
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
                NowPlayingScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
