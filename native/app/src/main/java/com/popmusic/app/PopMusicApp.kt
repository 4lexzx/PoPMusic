package com.popmusic.app

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.popmusic.app.data.music.MusicService
import com.popmusic.app.data.prefs.BackgroundMode
import com.popmusic.app.data.ringtone.RingtoneSetter
import com.popmusic.app.data.share.SongLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import com.popmusic.app.player.PlayerOpenBus
import com.popmusic.app.player.PlayerViewModel
import com.popmusic.app.ui.components.AppBackground
import com.popmusic.app.ui.components.GlassActionRow
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassDialogHeader
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.components.MiniPlayer
import com.popmusic.app.ui.components.SyncTargetSelector
import com.popmusic.app.ui.glass.LocalAppBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.layerBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.rememberCombinedBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.rememberLayerBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.LayerBackdrop
import com.popmusic.app.ui.navigation.LocalNavCollapsed
import com.popmusic.app.ui.navigation.PoPBottomBar
import com.popmusic.app.ui.navigation.Routes
import com.popmusic.app.ui.screens.account.AccountScreen
import com.popmusic.app.ui.screens.auth.AuthScreen
import com.popmusic.app.ui.screens.auth.PasswordRecoveryDialog
import com.popmusic.app.ui.screens.flow.MusicFlowScreen
import com.popmusic.app.ui.screens.home.HomeScreen

import com.popmusic.app.ui.screens.info.SongInfoScreen
import com.popmusic.app.ui.screens.history.HistoryScreen
import com.popmusic.app.ui.screens.library.LibraryScreen
import com.popmusic.app.ui.screens.library.LibraryViewModel
import com.popmusic.app.ui.screens.lyrics.LyricsScreen
import com.popmusic.app.ui.screens.onboarding.OnboardingScreen
import com.popmusic.app.ui.screens.party.PartyScreen
import com.popmusic.app.ui.screens.tastes.TastesScreen
import com.popmusic.app.ui.screens.player.NowPlayingScreen
import com.popmusic.app.ui.screens.playlist.PlaylistScreen
import com.popmusic.app.ui.screens.recognize.RecognizeScreen
import com.popmusic.app.ui.screens.search.SearchScreen
import com.popmusic.app.ui.screens.settings.SettingsScreen
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.screens.home.HomeRefreshBus
import com.popmusic.app.ui.theme.LocalGlassOpacity
import com.popmusic.app.ui.theme.LocalLiquidGlassEnabled
import com.popmusic.app.ui.theme.LocalReduceMotion
import com.popmusic.app.ui.theme.rememberReduceMotion
import com.popmusic.app.ui.updates.UpdateDialog
import com.popmusic.app.ui.updates.UpdateViewModel

/**
 * Item 6: si la app pasó más de este tiempo en segundo plano sin música
 * sonando, al volver se abre como la primera vez (en Inicio).
 */
private const val AppResetAfterBackgroundMs = 30L * 60L * 1000L

@Composable
fun PopMusicApp() {
    remember { android.util.Log.d("StartupPerf", "PopMusicApp composition START") }
    val app = LocalContext.current.applicationContext as PoPMusicApplication
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    val backgroundSettings by app.preferences.settings.collectAsState()
    val reduceMotion = rememberReduceMotion()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isFullScreen = currentRoute == Routes.NOW_PLAYING ||
        currentRoute == Routes.MUSIC_FLOW ||
        currentRoute == Routes.LYRICS ||
        currentRoute == Routes.SONG_INFO ||
        currentRoute == Routes.AUTH ||
        currentRoute == Routes.TASTES

    val playerViewModel: PlayerViewModel = viewModel()
    val playerState by playerViewModel.state.collectAsState()

    val libraryViewModel: LibraryViewModel = viewModel()
    val libraryState by libraryViewModel.state.collectAsState()
    // Ítem 3: cuentas de música vinculadas para el selector de sync al
    // crear una playlist desde el diálogo "Añadir a playlist".
    val musicSyncState by libraryViewModel.syncState.collectAsState()
    val connectedMusicServices = musicSyncState.filterValues { it.connected }.keys.toList()

    val updateViewModel: UpdateViewModel = viewModel()
    val updateState by updateViewModel.state.collectAsState()
    val authSession by app.authRepository.session.collectAsState()
    val tasteSettings by app.preferences.taste.collectAsState()
    val playbackSettings by app.preferences.playback.collectAsState()

    val currentSong = remember(playerState.mediaId, playerState.title, playerState.artist, playerState.durationMs) {
        if (playerState.mediaId.isBlank()) {
            null
        } else {
            Song(
                id = playerState.mediaId,
                title = playerState.title,
                artist = playerState.artist,
                durationSeconds = playerState.durationMs / 1_000,
                thumbnailUrl = playerState.artworkUrl,
            )
        }
    }
    val isFavorite = currentSong != null && libraryState.favorites.any { it.id == currentSong.id }

    val downloadProgress by app.downloadRepository.progress.collectAsState()
    val isDownloaded = currentSong != null && libraryState.downloads.any { it.id == currentSong.id }
    val currentDownloadProgress = currentSong?.let { downloadProgress[it.id] }

    var showAddToPlaylist by remember { mutableStateOf(false) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var navInline by remember { mutableStateOf(false) }
    val supportsLiquidGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val appBackdrop = rememberLayerBackdrop(
        cacheContent = backgroundSettings.mode == BackgroundMode.PRESET,
    ) { drawContent() }
    LaunchedEffect(backgroundSettings) { appBackdrop.invalidateContent() }

    // Arranca el motor de sincronización tras el primer frame (no afecta al arranque).
    LaunchedEffect(Unit) { app.musicSync }

    // Fuentes adicionales de backdrop: ambiente del reproductor y contenido real
    // de Biblioteca (para que los cristales desenfocuen lo que hay de verdad detrás).
    val playerBackdrop = rememberLayerBackdrop { drawContent() }
    val libraryBackdrop = rememberLayerBackdrop { drawContent() }
    // Captura del contenido de navegación (listas reales). SOLO la consume la
    // capa superpuesta (mini + barra): los cristales dentro del NavHost NO
    // pueden consumir esta captura o el árbol de render entra en ciclo.
    val navContentBackdrop = rememberLayerBackdrop { drawContent() }
    val overlayBackdrop = rememberCombinedBackdrop(appBackdrop, navContentBackdrop)
    val surfaceBackdrop = when {
        currentRoute == Routes.NOW_PLAYING ||
            currentRoute == Routes.MUSIC_FLOW ||
            currentRoute == Routes.LYRICS ||
            currentRoute == Routes.SONG_INFO -> rememberCombinedBackdrop(appBackdrop, playerBackdrop)
        currentRoute == Routes.LIBRARY -> rememberCombinedBackdrop(appBackdrop, libraryBackdrop)
        else -> rememberCombinedBackdrop(appBackdrop)
    }

    var scrollAccumulator by remember { mutableFloatStateOf(0f) }

    val navScrollConnection = remember {
        object : NestedScrollConnection {
            // Ítem 5: SOLO el scroll real oculta la barra. onPreScroll se
            // disparaba con cualquier arrastre (aunque no hubiera nada que
            // bajar) y escondía barra + mini; onPostScroll con consumed
            // solo llega cuando el contenido de verdad se movió.
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // consumed PRIMERO (firma oficial de NestedScrollConnection);
                // con los parámetros invertidos se leía "available" (≈0) y la
                // barra casi nunca se ocultaba al hacer scroll.
                val dy = consumed.y
                if (dy != 0f) {
                    if (scrollAccumulator * dy < 0f) scrollAccumulator = 0f
                    scrollAccumulator += dy
                    if (scrollAccumulator < -90f && !navInline) {
                        navInline = true
                        scrollAccumulator = 0f
                    } else if (scrollAccumulator > 60f && navInline) {
                        navInline = false
                        scrollAccumulator = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Ítem 1: fotograma de la pantalla que había ANTES de abrir el
    // reproductor (PixelCopy al navegar). Se dibuja detrás del Scaffold
    // para que, al arrastrar hacia abajo o cerrar el reproductor, se vea
    // el inicio al instante en vez de un fondo oscuro.
    var playerBehind by remember { mutableStateOf<Bitmap?>(null) }

    fun capturePlayerBehind() {
        var ctx: android.content.Context = context
        while (ctx is ContextWrapper && ctx !is Activity) {
            ctx = ctx.baseContext
        }
        val activity = ctx as? Activity ?: return
        val window = activity.window ?: return
        val decor = window.decorView
        val w = decor.width
        val h = decor.height
        if (w <= 0 || h <= 0) return
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(
                window,
                bmp,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        playerBehind = bmp
                    } else {
                        bmp.recycle()
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        } catch (e: RuntimeException) {
            bmp.recycle()
        }
    }

    fun openPlayer() {
        capturePlayerBehind()
        navController.navigate(Routes.NOW_PLAYING) {
            launchSingleTop = true
        }
    }

    // Se descarta el fotograma a los 700 ms de salir del reproductor
    // (las transiciones duran ~300 ms) para que ninguna otra pantalla
    // quede con una imagen vieja detrás. Dentro del reproductor/letras/
    // información se conserva (esas pantallas tienen fondo propio).
    LaunchedEffect(currentRoute) {
        val inPlayer = currentRoute == Routes.NOW_PLAYING ||
            currentRoute == Routes.LYRICS ||
            currentRoute == Routes.SONG_INFO
        if (!inPlayer) {
            delay(700)
            playerBehind = null
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

    // Item 5: la notificación de media session (tocarla) pide abrir el
    // reproductor; el bus llega desde MainActivity (onCreate/onNewIntent).
    val openPlayerPending by PlayerOpenBus.pending.collectAsState()
    LaunchedEffect(openPlayerPending) {
        if (openPlayerPending) {
            PlayerOpenBus.consume()
            openPlayer()
        }
    }

    // Item 6: si la app estuvo >30 min en segundo plano SIN música sonando
    // (y no se abrió desde la notificación), al volver se limpia la navegación
    // y queda como la primera vez: en Inicio.
    val resumedAt by HomeRefreshBus.resumedAt.collectAsState()
    LaunchedEffect(resumedAt) {
        if (resumedAt == 0L) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val openedFromNotification = (now - PlayerOpenBus.requestedAtMs) < 5_000L
        if (openedFromNotification) return@LaunchedEffect
        val backgroundAt = app.preferences.lastBackgroundedAt()
        if (backgroundAt <= 0L) return@LaunchedEffect
        val awayMs = now - backgroundAt
        if (awayMs > AppResetAfterBackgroundMs && !playerState.isPlaying) {
            navController.popBackStack(Routes.HOME, false)
        }
    }

    CompositionLocalProvider(
        LocalGlassOpacity provides backgroundSettings.glassOpacity,
        LocalLiquidGlassEnabled provides backgroundSettings.liquidGlass,
        LocalReduceMotion provides reduceMotion,
        LocalAppBackdrop provides surfaceBackdrop,
        // Ítem 6: el FAB de Biblioteca baja junto con la barra colapsada.
        LocalNavCollapsed provides navInline,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { android.util.Log.d("StartupPerf", "root layout pass done") },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (supportsLiquidGlass) {
                            Modifier.layerBackdrop(appBackdrop)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                AppBackground(
                    settings = backgroundSettings,
                    modifier = Modifier.fillMaxSize(),
                )
                // Fotograma de la pantalla anterior: queda entre el
                // fondo y el Scaffold, de modo que se ve mientras el
                // reproductor baja (arrastre o transición de cierre).
                playerBehind?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = Ink,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .nestedScroll(navScrollConnection)
                            .then(
                                if (supportsLiquidGlass) {
                                    Modifier.layerBackdrop(navContentBackdrop)
                                } else {
                                    Modifier
                                },
                            ),
                        enterTransition = {
                            if (reduceMotion) {
                                fadeIn(tween(150))
                            } else {
                                // Empuja hacia adelante: entra desde abajo
                                // con desvanecimiento; el que sale se va hacia
                                // arriba (dirección coherente al volver).
                                fadeIn(tween(260)) +
                                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) {
                                        it / 12
                                    }
                            }
                        },
                        exitTransition = {
                            if (reduceMotion) {
                                fadeOut(tween(120))
                            } else {
                                fadeOut(tween(180)) +
                                    slideOutVertically(tween(280, easing = FastOutSlowInEasing)) {
                                        -it / 28
                                    }
                            }
                        },
                        popEnterTransition = {
                            if (reduceMotion) {
                                fadeIn(tween(150))
                            } else {
                                fadeIn(tween(260)) +
                                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) {
                                        -it / 28
                                    }
                            }
                        },
                        popExitTransition = {
                            if (reduceMotion) {
                                fadeOut(tween(120))
                            } else {
                                fadeOut(tween(200)) +
                                    slideOutVertically(tween(300, easing = FastOutSlowInEasing)) {
                                        it / 12
                                    }
                            }
                        },
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                onOpenPlayer = { openPlayer() },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                onOpenFlow = { navController.navigate(Routes.MUSIC_FLOW) },
                                onOpenLibrary = {
                                    navController.navigate(Routes.LIBRARY) {
                                        popUpTo(Routes.HOME) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                },
                                onOpenDownloads = {
                                    navController.navigate(
                                        Routes.playlist(LibraryRepository.DOWNLOADS_ID),
                                    )
                                },
                                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                                onOpenSearch = {
                                    navController.navigate(Routes.SEARCH) {
                                        popUpTo(Routes.HOME) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                },
                                onOpenParty = { navController.navigate(Routes.PARTY) },
                                onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
                                onPlay = { songs, startIndex, autoExtend ->
                                    playerViewModel.play(songs, startIndex, autoExtend)
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
                                onBack = { navController.popBackStack() },
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
                                onPlay = { songs, startIndex ->
                                    playerViewModel.play(songs, startIndex)
                                    openPlayer()
                                },
                                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                                onOpenParty = { navController.navigate(Routes.PARTY) },
                                onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                contentBackdrop = libraryBackdrop,
                                miniVisible = playerState.mediaId.isNotBlank(),
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
                        composable(
                            route = Routes.NOW_PLAYING,
                            enterTransition = {
                                slideInVertically(
                                    animationSpec = tween(360, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it },
                                ) + fadeIn(tween(200))
                            },
                            exitTransition = {
                                slideOutVertically(tween(300)) { it } + fadeOut(tween(200))
                            },
                            popEnterTransition = {
                                slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it } +
                                    fadeIn(tween(200))
                            },
                            popExitTransition = {
                                slideOutVertically(tween(300)) { it } + fadeOut(tween(200))
                            },
                        ) {
                            NowPlayingScreen(
                                state = playerState,
                                isFavorite = isFavorite,
                                hideArtwork = playbackSettings.hideThumbnail,
                                ambientMotion = playbackSettings.ambientMotion,
                                onToggleFavorite = { currentSong?.let(libraryViewModel::toggleFavorite) },
                                onBack = { navController.popBackStack() },
                                onTogglePlayPause = { playerViewModel.togglePlayPause() },
                                onNext = { playerViewModel.next() },
                                onPrevious = { playerViewModel.previous() },
                                onSeek = { playerViewModel.seekTo(it) },
                                onToggleShuffle = { playerViewModel.setShuffle(!playerState.shuffle) },
                                onCycleRepeat = { cycleRepeatMode() },
                                onOpenLyrics = { navController.navigate(Routes.LYRICS) },
                                onAddToPlaylist = {
                                    addToPlaylistSong = currentSong
                                    showAddToPlaylist = true
                                },
                                onOpenInfo = { navController.navigate(Routes.SONG_INFO) },
                                isDownloaded = isDownloaded,
                                downloadProgress = currentDownloadProgress,
                                onDownload = {
                                    val song = currentSong
                                    if (song != null && !app.downloadRepository.download(song)) {
                                        Toast.makeText(
                                            context,
                                            "Activa Wi-Fi o desactiva «Solo Wi-Fi» en Ajustes",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                                onShare = {
                                    currentSong?.let { song ->
                                        shareScope.launch {
                                            // Enlace universal de song.link (Odesli) con
                                            // fallback al de YouTube si la API falla.
                                            val link = SongLink.resolve(song.id)
                                                ?: "https://youtu.be/${song.id}"
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, link)
                                            }
                                            runCatching {
                                                context.startActivity(
                                                    Intent.createChooser(send, "Compartir canción"),
                                                )
                                            }
                                        }
                                    }
                                },
                                onViewOnYouTube = {
                                    currentSong?.let { song ->
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://youtu.be/${song.id}"),
                                                ),
                                            )
                                        }
                                    }
                                },
                                onSetRingtone = {
                                    val song = currentSong
                                    if (song != null) {
                                        if (!RingtoneSetter.canWrite(context)) {
                                            Toast.makeText(
                                                context,
                                                "Activa «Modificar ajustes del sistema» " +
                                                    "para PoP Music y repite el paso",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                            RingtoneSetter.requestWriteAccess(context)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Estableciendo tono…",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            shareScope.launch {
                                                val result =
                                                    RingtoneSetter.setAsRingtone(context, song)
                                                Toast.makeText(
                                                    context,
                                                    result.fold(
                                                        onSuccess = { message -> message },
                                                        onFailure = { error ->
                                                            error.message
                                                                ?: "No se pudo establecer el tono"
                                                        },
                                                    ),
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                positionProvider = playerViewModel::currentPositionMs,
                                ambientBackdrop = playerBackdrop,
                            )
                        }
                        composable(Routes.MUSIC_FLOW) {
                            MusicFlowScreen(
                                playerState = playerState,
                                favoriteIds = libraryState.favorites.mapTo(HashSet()) { it.id },
                                downloadedIds = libraryState.downloads.mapTo(HashSet()) { it.id },
                                downloadProgress = downloadProgress,
                                onToggleFavorite = { song -> libraryViewModel.toggleFavorite(song) },
                                onPlayAt = { songs, index ->
                                    playerViewModel.play(songs, index, autoExtend = true)
                                },
                                onTogglePlayPause = { playerViewModel.togglePlayPause() },
                                onOpenPlayer = { openPlayer() },
                                onOpenLyrics = { navController.navigate(Routes.LYRICS) },
                                onOpenInfo = { navController.navigate(Routes.SONG_INFO) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                onAddToPlaylist = { song ->
                                    addToPlaylistSong = song
                                    showAddToPlaylist = true
                                },
                                onShare = { song ->
                                    shareScope.launch {
                                        val link = SongLink.resolve(song.id)
                                            ?: "https://youtu.be/${song.id}"
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, link)
                                        }
                                        runCatching {
                                            context.startActivity(
                                                Intent.createChooser(send, "Compartir canción"),
                                            )
                                        }
                                    }
                                },
                                onDownload = { song ->
                                    if (!app.downloadRepository.download(song)) {
                                        Toast.makeText(
                                            context,
                                            "Activa Wi-Fi o desactiva «Solo Wi-Fi» en Ajustes",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                                onViewOnYouTube = { song ->
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://youtu.be/${song.id}"),
                                            ),
                                        )
                                    }
                                },
                                onSeek = { playerViewModel.seekTo(it) },
                                onBack = { navController.popBackStack() },
                                positionProvider = playerViewModel::currentPositionMs,
                            )
                        }
                        composable(Routes.LYRICS) {
                            LyricsScreen(
                                songId = playerState.mediaId,
                                title = playerState.title,
                                artist = playerState.artist,
                                artworkUrl = playerState.artworkUrl,
                                positionMs = playerState.positionMs,
                                durationMs = playerState.durationMs,
                                isPlaying = playerState.isPlaying,
                                positionProvider = playerViewModel::currentPositionMs,
                                onBack = { navController.popBackStack() },
                                ambientBackdrop = playerBackdrop,
                                lyricsStyle = playbackSettings.lyricsStyle,
                                onLyricsStyleChange = { style ->
                                    app.preferences.updatePlayback { it.copy(lyricsStyle = style) }
                                },
                            )
                        }
                        composable(Routes.SONG_INFO) {
                            SongInfoScreen(
                                song = currentSong,
                                onBack = { navController.popBackStack() },
                                ambientBackdrop = playerBackdrop,
                            )
                        }
                        composable(Routes.HISTORY) {
                            HistoryScreen(
                                onBack = { navController.popBackStack() },
                                onPlay = { songs, startIndex ->
                                    playerViewModel.play(songs, startIndex)
                                    openPlayer()
                                },
                            )
                        }
                        composable(Routes.ACCOUNT) {
                            AccountScreen(
                                onBack = { navController.popBackStack() },
                                onOpenPlaylist = { id ->
                                    navController.navigate(Routes.playlist(id))
                                },
                                onPlay = { songs, startIndex ->
                                    playerViewModel.play(songs, startIndex)
                                    openPlayer()
                                },
                                onOpenAuth = { navController.navigate(Routes.AUTH) },
                                onSignOut = {
                                    shareScope.launch { app.authRepository.signOut() }
                                },
                                session = authSession,
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                preferences = app.preferences,
                                authRepository = app.authRepository,
                                libraryRepository = app.libraryRepository,
                                musicSync = app.musicSync,
                                onOpenAuth = { navController.navigate(Routes.AUTH) },
                                onOpenTastes = { navController.navigate(Routes.TASTES) },
                                onOpenDownloads = {
                                    navController.navigate(
                                        Routes.playlist(LibraryRepository.DOWNLOADS_ID),
                                    )
                                },
                                onCheckUpdates = { onResult ->
                                    updateViewModel.checkAgain { info -> onResult(info != null) }
                                },
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.AUTH) {
                            AuthScreen(
                                onBack = { navController.popBackStack() },
                                onAuthenticated = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.TASTES) {
                            TastesScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.PARTY) {
                            PartyScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }

            if (!isFullScreen) {
                CompositionLocalProvider(
                    LocalAppBackdrop provides overlayBackdrop,
                ) {
                    PoPBottomBar(
                        currentRoute = currentRoute,
                        inline = navInline,
                        onExpand = { navInline = false },
                        miniVisible = playerState.mediaId.isNotBlank(),
                        miniContent = { miniModifier ->
                            MiniPlayer(
                                title = playerState.title.ifBlank { "Sin reproducción" },
                                artist = playerState.artist.ifBlank { "Busca una canción para empezar" },
                                artworkUrl = playerState.artworkUrl,
                                isPlaying = playerState.isPlaying,
                                isBuffering = playerState.isBuffering,
                                durationMs = playerState.durationMs,
                                positionProvider = playerViewModel::currentPositionMs,
                                onTogglePlay = { playerViewModel.togglePlayPause() },
                                onPrevious = { playerViewModel.previous() },
                                onNext = { playerViewModel.next() },
                                onClick = { openPlayer() },
                                compact = navInline,
                                modifier = miniModifier,
                            )
                        },
                        onNavigate = { route ->
                            navInline = false
                            // Ítem 1: cada botón SIEMPRE lleva a su
                            // pestaña. Sin saveState/restoreState: la
                            // restauración podía revivir la pila anterior
                            // (entrabas a Ajustes y "Inicio" te devolvía
                            // a Ajustes en vez de Inicio).
                            if (navController.currentBackStackEntry?.destination?.route != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 6.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }

    PasswordRecoveryDialog(authRepository = app.authRepository)

    if (showAddToPlaylist) {
        val dialogSong = addToPlaylistSong ?: currentSong
        AddToPlaylistDialog(
            playlists = libraryState.playlists,
            onDismiss = {
                showAddToPlaylist = false
                addToPlaylistSong = null
            },
            onSelect = { playlistId ->
                dialogSong?.let { libraryViewModel.addToPlaylist(playlistId, it) }
                showAddToPlaylist = false
                addToPlaylistSong = null
            },
            onCreate = { name, syncTarget ->
                val playlist = libraryViewModel.createPlaylist(name, syncTarget)
                if (playlist != null) {
                    dialogSong?.let { libraryViewModel.addToPlaylist(playlist.id, it) }
                    showAddToPlaylist = false
                    addToPlaylistSong = null
                }
            },
            syncServices = connectedMusicServices,
        )
    }

    if (updateState.info != null) {
        UpdateDialog(
            state = updateState,
            onDismiss = updateViewModel::dismiss,
            onDownload = updateViewModel::download,
            onInstall = updateViewModel::install,
            onPermissionResult = updateViewModel::onReturnedFromSettings,
        )
    }

    if (!tasteSettings.completed) {
        OnboardingScreen(
            isEditMode = false,
            onDone = { },
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (String, String?) -> Unit,
    syncServices: List<MusicService> = emptyList(),
) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    // Ítem 3: elección de sync al crear (NONE = solo en el teléfono).
    var syncTarget by remember { mutableStateOf<String?>("NONE") }

    GlassDialog(onDismiss = onDismiss) {
        GlassDialogHeader(
            title = "Añadir a playlist",
            subtitle = "Elige una lista o crea una nueva al instante.",
            onClose = onDismiss,
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassActionRow(
                icon = Icons.Rounded.Favorite,
                title = "Canciones favoritas",
                subtitle = "Tus me gusta",
                onClick = { onSelect(LibraryRepository.FAVORITES_ID) },
            )
            playlists.forEach { playlist ->
                GlassActionRow(
                    icon = Icons.Rounded.QueueMusic,
                    title = playlist.name,
                    subtitle = when (playlist.songs.size) {
                        0 -> "Playlist vacía"
                        1 -> "1 canción"
                        else -> "${playlist.songs.size} canciones"
                    },
                    onClick = { onSelect(playlist.id) },
                )
            }
            if (playlists.isEmpty()) {
                Text(
                    text = "Aún no tienes playlists propias.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Faint,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (creating) {
            GlassTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Nombre de la playlist",
            )
            // Ítem 3: opción de subirla a YouTube/Spotify (si hay cuentas).
            if (syncServices.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SyncTargetSelector(
                    services = syncServices,
                    selected = syncTarget,
                    onSelect = { syncTarget = it },
                )
            }
            Spacer(Modifier.height(12.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { creating = false },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Crear y añadir",
                    onClick = {
                        onCreate(name, if (syncServices.isEmpty()) null else syncTarget)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank(),
                )
            }
        } else {
            GlassTextButton(
                text = "+ Nueva playlist",
                onClick = {
                    name = ""
                    syncTarget = "NONE"
                    creating = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
