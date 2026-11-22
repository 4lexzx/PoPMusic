package com.popmusic.app.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.popmusic.app.data.filter.ContentFilters
import com.popmusic.app.data.recommendations.MOODS
import com.popmusic.app.data.recommendations.Mood
import com.popmusic.app.data.recent.RecentStore
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.GroupedSongList
import com.popmusic.app.ui.components.MusicLoader
import com.popmusic.app.ui.components.PopLogo
import com.popmusic.app.ui.components.SectionHeader
import com.popmusic.app.ui.components.TrackRow
import com.popmusic.app.ui.components.enterOnVisible
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.pressScale
import kotlin.random.Random

private enum class HomeTab(val label: String) {
    ALL("Todo"),
    FOR_YOU("Para ti"),
    FAVORITES("Favoritos"),
    DISCOVER("Descubrir"),
    PODCAST("Podcasts"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFlow: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenParty: () -> Unit,
    onOpenAccount: () -> Unit,
    onPlay: (List<Song>, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    remember { android.util.Log.d("StartupPerf", "HomeScreen composition START") }
    val state by viewModel.state.collectAsState()
    val libraryState by viewModel.favorites.collectAsState()
    // Corazón de favorito junto al nombre (ítem 4 de la tanda 3).
    val favoriteIds = remember(libraryState.favorites) {
        libraryState.favorites.mapTo(HashSet()) { it.id }
    }
    val discoverEnabled by viewModel.discoverEnabled.collectAsState()
    val playbackPrefs by viewModel.playback.collectAsState()
    val listState = rememberLazyListState()
    var tab by rememberSaveable { mutableStateOf(HomeTab.ALL) }

    // Pestaña Podcasts: catálogo real (iTunes + RSS), se carga bajo demanda.
    val podcastsViewModel: PodcastsViewModel = viewModel()
    val podcasts by podcastsViewModel.state.collectAsState()
    LaunchedEffect(tab) {
        if (tab == HomeTab.PODCAST && !podcasts.loaded && !podcasts.loading) {
            podcastsViewModel.refresh()
        }
    }

    LaunchedEffect(
        listState,
        state.discoverSections.size,
        state.isLoadingMore,
        state.discoverEnded,
        discoverEnabled,
        tab,
    ) {
        if (!discoverEnabled || tab != HomeTab.ALL) return@LaunchedEffect
        val layout = listState.layoutInfo
        val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (
            !state.isLoadingMore &&
            !state.discoverEnded &&
            lastVisible >= layout.totalItemsCount - 6
        ) {
            viewModel.loadMore()
        }
    }

    val hideVideos = playbackPrefs.hideVideos
    val hideShorts = playbackPrefs.hideShorts

    val tasteSongs = remember(state.taste.songs, hideVideos, hideShorts) {
        ContentFilters.apply(state.taste.songs, hideVideos, hideShorts)
    }
    val feedSections = remember(state.sections, hideVideos, hideShorts) {
        state.sections
            .map { section ->
                section.copy(songs = ContentFilters.apply(section.songs, hideVideos, hideShorts))
            }
            .filter { it.songs.isNotEmpty() }
    }
    val discoverSections = remember(state.discoverSections, hideVideos, hideShorts) {
        state.discoverSections
            .map { section ->
                section.copy(songs = ContentFilters.apply(section.songs, hideVideos, hideShorts))
            }
            .filter { it.songs.isNotEmpty() }
    }

    val randomPool = remember(tasteSongs, libraryState.favorites) {
        (tasteSongs + libraryState.favorites).distinctBy { it.id }
    }

    // Tarjetas grandes ("Todo"): lo que de verdad escucha el usuario
    // (historial + favoritos + gusto editorial), rotado por hora para que
    // no sean siempre las mismas.
    val appContext = LocalContext.current
    val heroEpoch = System.currentTimeMillis() / HERO_ROTATION_MS
    val heroSongs = remember(
        tasteSongs,
        libraryState.favorites,
        heroEpoch,
        appContext,
    ) {
        buildHeroSongs(
            taste = tasteSongs,
            favorites = libraryState.favorites,
            recent = runCatching { RecentStore.recent(appContext) }.getOrDefault(emptyList()),
        )
    }

    val chartSongs = remember(viewModel.chart, hideVideos, hideShorts) {
        ContentFilters.apply(viewModel.chart, hideVideos, hideShorts)
    }

    // Pull-to-refresh: el refresh manual ignora la TTL de la caché.
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) refreshing = false
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            viewModel.load()
        },
        modifier = modifier.fillMaxSize(),
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header ESTÁTICO: título + chips fuera de la lista, no se va con el scroll.
        Spacer(Modifier.statusBarsPadding())
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ítem 4: el logo del usuario donde decía "PoP Music"; PopLogo
            // dibuja con filtrado bicúbico (a 24 dp se veía pixelado).
            PopLogo(
                contentDescription = "PoP Music",
                modifier = Modifier.height(28.dp),
            )
            Spacer(Modifier.weight(1f))
            HomeTopIcon(
                icon = Icons.Rounded.History,
                description = "Recientes",
                onClick = onOpenHistory,
            )
            // La lupa se quitó de la cabecera: la barra inferior ya tiene Buscar.
            Spacer(Modifier.width(4.dp))
            HomeTopIcon(
                icon = Icons.Rounded.Groups,
                description = "Salas",
                onClick = onOpenParty,
            )
            Spacer(Modifier.width(4.dp))
            HomeTopIcon(
                icon = Icons.Rounded.Person,
                description = "Cuenta",
                onClick = onOpenAccount,
            )
            Spacer(Modifier.width(4.dp))
            HomeTopIcon(
                icon = Icons.Rounded.Settings,
                description = "Ajustes",
                onClick = onOpenSettings,
            )
        }
        Spacer(Modifier.height(16.dp))
        HomeChips(selected = tab, onSelect = { tab = it })
        // Pequeño espacio para que la píldora activa (blanca) no quede
        // pegada/recortada contra el borde de la lista.
        Spacer(Modifier.height(8.dp))

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {

        if (state.isLoading && state.sections.isEmpty()) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MusicLoader(
                        size = 28.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (state.error != null && state.sections.isEmpty()) {
            item(key = "error") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = state.error ?: "No se pudo cargar",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = viewModel::load) {
                        Text("Reintentar")
                    }
                }
            }
        }

        if (tab == HomeTab.ALL) {
            val heroes = heroSongs
            if (heroes.isNotEmpty()) {
                item(key = "hero") {
                    Spacer(Modifier.height(16.dp))
                    // Carrusel hero estilo Echo (material3): recorta cada tarjeta
                    // a una ventana que se abre/cierra al pasar (sin escala ni fade).
                    val configuration = LocalConfiguration.current
                    val heroWidth = configuration.screenWidthDp.dp - 32.dp
                    val carouselState = rememberCarouselState { heroes.size }
                    // Autoavance cada 5 s mientras nadie esté deslizando, en
                    // ping-pong: al llegar al final regresa hacia atrás página a
                    // página (nunca salta a la primera). El tween va con escala
                    // 1x forzada: si el sistema tiene la escala de animación en
                    // 0, Compose corta cualquier animación a cero y el paso se
                    // veía como un cambio de foto.
                    LaunchedEffect(carouselState, heroes.size) {
                        if (heroes.size < 2) return@LaunchedEffect
                        val oneX = object : MotionDurationScale {
                            override val scaleFactor = 1f
                        }
                        var forward = true
                        while (true) {
                            delay(5000)
                            if (carouselState.isScrollInProgress) continue
                            val current = carouselState.currentItem
                            val target = when {
                                current <= 0 -> {
                                    forward = true
                                    1
                                }
                                current >= heroes.size - 1 -> {
                                    forward = false
                                    heroes.size - 2
                                }
                                else -> if (forward) current + 1 else current - 1
                            }
                            withContext(oneX) {
                                carouselState.animateScrollToItem(
                                    item = target,
                                    animationSpec = tween(
                                        durationMillis = 700,
                                        easing = FastOutSlowInEasing,
                                    ),
                                )
                            }
                        }
                    }
                    HorizontalCenteredHeroCarousel(
                        state = carouselState,
                        maxItemWidth = heroWidth,
                        itemSpacing = 10.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(214.dp),
                    ) { page ->
                        val hero = heroes[page % heroes.size]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .maskClip(RoundedCornerShape(28.dp)),
                        ) {
                            DailyDiscoveryCard(
                                song = hero,
                                onPlay = { onPlay(heroes, page % heroes.size, true) },
                            )
                        }
                    }
                }
            }

            item(key = "quick-row") {
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(title = "Acceso rápido")
                }
                Spacer(Modifier.height(10.dp))
                // Una sola fila horizontal (antes ocupaba 2 filas y mucho espacio).
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        QuickTile(
                            label = "Favoritos",
                            icon = Icons.Rounded.Favorite,
                            modifier = Modifier.width(132.dp),
                            onClick = onOpenLibrary,
                        )
                    }
                    item {
                        QuickTile(
                            label = "Descargas",
                            icon = Icons.Rounded.Download,
                            modifier = Modifier.width(132.dp),
                            onClick = onOpenDownloads,
                        )
                    }
                    item {
                        QuickTile(
                            label = "Music Flow",
                            icon = Icons.Rounded.GraphicEq,
                            modifier = Modifier.width(132.dp),
                            onClick = onOpenFlow,
                        )
                    }
                    item {
                        QuickTile(
                            label = "Aleatorio",
                            icon = Icons.Rounded.Shuffle,
                            modifier = Modifier.width(132.dp),
                            onClick = {
                                if (randomPool.isNotEmpty()) {
                                    onPlay(randomPool, Random.nextInt(randomPool.size), false)
                                }
                            },
                        )
                    }
                    item {
                        QuickTile(
                            label = "Playlists",
                            icon = Icons.Rounded.QueueMusic,
                            modifier = Modifier.width(132.dp),
                            onClick = onOpenLibrary,
                        )
                    }
                    item {
                        QuickTile(
                            label = "Para ti",
                            icon = Icons.Rounded.AutoAwesome,
                            modifier = Modifier.width(132.dp),
                            onClick = { tab = HomeTab.FOR_YOU },
                        )
                    }
                }
            }

            item(key = "moods") {
                Spacer(Modifier.height(26.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(title = "Estados de ánimo")
                    Spacer(Modifier.height(10.dp))
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(MOODS) { _, mood ->
                        MoodTile(
                            mood = mood,
                            loading = viewModel.moodLoading == mood.label,
                            onClick = {
                                viewModel.playMood(mood) { songs ->
                                    val filtered =
                                        ContentFilters.apply(songs, hideVideos, hideShorts)
                                    if (filtered.isNotEmpty()) {
                                        onPlay(filtered, 0, true)
                                    }
                                }
                            },
                        )
                    }
                }
            }

            item(key = "chart") {
                Spacer(Modifier.height(26.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(
                        title = "Tendencias",
                        action = "Ver todo",
                        onActionClick = onOpenSearch,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (chartSongs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(chartSongs) { index, song ->
                            ChartTile(
                                rank = index + 1,
                                song = song,
                                onClick = { onPlay(chartSongs, index, true) },
                            )
                        }
                    }
                }
            }
        }

        if (tab == HomeTab.ALL || tab == HomeTab.FOR_YOU) {
            val songs =
                if (tab == HomeTab.FOR_YOU) tasteSongs
                else tasteSongs.take(6)
            if (songs.isNotEmpty()) {
                item(key = "taste-header") {
                    Spacer(Modifier.height(26.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(
                            title = "Para ti",
                            playAll = true,
                            onPlayAll = { onPlay(songs, 0, true) },
                            action = if (tab == HomeTab.ALL && tasteSongs.size > 6) {
                                "Ver todo"
                            } else {
                                null
                            },
                            onActionClick = if (tab == HomeTab.ALL) {
                                { tab = HomeTab.FOR_YOU }
                            } else {
                                null
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Recomendado según tus favoritos y playlists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Faint,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item(key = "taste-list") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GroupedSongList(
                            songs = songs,
                            favoriteIds = favoriteIds,
                            onPlay = { index -> onPlay(songs, index, true) },
                        )
                    }
                }
            }
        }

        if (tab == HomeTab.ALL) {
            items(
                feedSections,
                key = { section -> "sec-${section.title}" },
            ) { section ->
                SongShelf(
                    title = section.title,
                    songs = section.songs,
                    onPlay = onPlay,
                    favoriteIds = favoriteIds,
                )
            }
        }

        if (tab == HomeTab.ALL || tab == HomeTab.FAVORITES) {
            val favorites =
                if (tab == HomeTab.FAVORITES) libraryState.favorites
                else libraryState.favorites.take(6)
            if (favorites.isNotEmpty()) {
                item(key = "fav-header") {
                    Spacer(Modifier.height(26.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(
                            title = "Canciones favoritas",
                            playAll = true,
                            onPlayAll = { onPlay(favorites, 0, false) },
                            action = if (tab == HomeTab.ALL && libraryState.favorites.size > 6) {
                                "Ver todo"
                            } else {
                                null
                            },
                            onActionClick = if (tab == HomeTab.ALL) {
                                { tab = HomeTab.FAVORITES }
                            } else {
                                null
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item(key = "fav-list") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GroupedSongList(
                            songs = favorites,
                            favoriteIds = favoriteIds,
                            onPlay = { index -> onPlay(favorites, index, false) },
                        )
                    }
                }
            }
        }

        if (tab == HomeTab.DISCOVER ||
            (tab == HomeTab.ALL && discoverEnabled && discoverSections.isNotEmpty())
        ) {
            item(key = "disc-header") {
                Spacer(Modifier.height(26.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(title = "Descubre más")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Nuevas secciones según exploras",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Faint,
                    )
                }
            }
            itemsIndexed(
                discoverSections,
                key = { index, _ -> "disc-$index" },
            ) { _, section ->
                SongShelf(
                    title = section.title,
                    songs = section.songs,
                    onPlay = onPlay,
                    favoriteIds = favoriteIds,
                )
            }
            if (discoverSections.isEmpty()) {
                item(key = "disc-empty") {
                    Text(
                        text = "Sigue escuchando para descubrir más secciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Faint,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }

        if (tab == HomeTab.ALL && discoverEnabled && state.isLoadingMore) {
            item(key = "disc-loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MusicLoader(
                        size = 22.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (tab == HomeTab.PODCAST) {
            item(key = "pod-search") {
                Column {
                    Spacer(Modifier.height(4.dp))
                    com.popmusic.app.ui.components.GlassTextField(
                        value = podcasts.query,
                        onValueChange = podcastsViewModel::onQueryChange,
                        placeholder = "Busca un podcast…",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        // Tanda 9: "X" para limpiar lo escrito de un toque.
                        trailingIcon = if (podcasts.query.isNotEmpty()) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = Faint,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .clickable { podcastsViewModel.onQueryChange("") }
                                        .padding(3.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    PodcastCategoryRow(
                        categories = com.popmusic.app.data.podcasts.PODCAST_CATEGORIES,
                        onSelect = podcastsViewModel::searchCategory,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            val selectedPodcast = podcasts.selected
            if (selectedPodcast != null) {
                item(key = "pod-detail") {
                    PodcastDetailHeader(
                        podcast = selectedPodcast,
                        subscribed = podcasts.subscriptions.any { it.id == selectedPodcast.id },
                        onBack = podcastsViewModel::closeSelected,
                        onToggleSubscribe = { podcastsViewModel.toggleSubscribe(selectedPodcast) },
                    )
                }
                if (podcasts.episodesLoading) {
                    item(key = "pod-eps-loading") { PodcastLoading() }
                } else {
                    items(
                        podcasts.episodes.size,
                        key = { "ep-${podcasts.episodes[it].id}" },
                    ) { index ->
                        val episode = podcasts.episodes[index]
                        PodcastEpisodeRow(
                            episode = episode,
                            onClick = {
                                podcastsViewModel.markPlayed(episode)
                                onPlay(podcasts.episodes.map { it.toSong() }, index, true)
                            },
                        )
                    }
                    if (podcasts.episodes.isEmpty()) {
                        item(key = "pod-eps-empty") {
                            Text(
                                text = podcasts.error ?: "Sin episodios disponibles.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Faint,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            } else if (podcasts.query.isNotBlank()) {
                item(key = "pod-results-header") { PodcastHeader("Resultados") }
                if (podcasts.episodesLoading) {
                    item(key = "pod-results-loading") { PodcastLoading() }
                } else {
                    items(
                        podcasts.results.size,
                        key = { "res-${podcasts.results[it].id}" },
                    ) { index ->
                        PodcastTile(
                            podcast = podcasts.results[index],
                            onClick = { podcastsViewModel.select(podcasts.results[index]) },
                        )
                    }
                    if (podcasts.results.isEmpty()) {
                        item(key = "pod-results-empty") {
                            Text(
                                text = podcasts.error ?: "Sin resultados.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Faint,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            } else {
                if (podcasts.recent.isNotEmpty()) {
                    item(key = "pod-recent-header") { PodcastHeader("Continuar escuchando") }
                    val recent = podcasts.recent.take(4)
                    items(recent.size, key = { "rec-${recent[it].id}" }) { index ->
                        val episode = recent[index]
                        PodcastEpisodeRow(
                            episode = episode,
                            onClick = {
                                podcastsViewModel.markPlayed(episode)
                                onPlay(recent.map { it.toSong() }, index, true)
                            },
                        )
                    }
                    item(key = "pod-recent-gap") { Spacer(Modifier.height(4.dp)) }
                }

                if (podcasts.subscriptions.isNotEmpty()) {
                    item(key = "pod-subs-header") { PodcastHeader("Tus suscripciones") }
                    item(key = "pod-subs-row") {
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                podcasts.subscriptions.size,
                                key = { "sub-${podcasts.subscriptions[it].id}" },
                            ) { index ->
                                PodcastGridTile(
                                    podcast = podcasts.subscriptions[index],
                                    onClick = { podcastsViewModel.select(podcasts.subscriptions[index]) },
                                )
                            }
                        }
                    }
                }

                item(key = "pod-top-header") { PodcastHeader("Destacados") }
                if (podcasts.loading) {
                    item(key = "pod-top-loading") { PodcastLoading() }
                } else {
                    items(
                        podcasts.top.size,
                        key = { "top-${podcasts.top[it].id}" },
                    ) { index ->
                        PodcastTile(
                            podcast = podcasts.top[index],
                            onClick = { podcastsViewModel.select(podcasts.top[index]) },
                        )
                    }
                    if (podcasts.top.isEmpty()) {
                        item(key = "pod-top-empty") {
                            Text(
                                text = "No se pudo cargar el catálogo. Revisa tu conexión.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Faint,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        item(key = "end-spacer") {
            Spacer(Modifier.height(24.dp))
        }
    }
    }
    }
    android.util.Log.d("StartupPerf", "HomeScreen body END")
}

@Composable
private fun HomeTopIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun HomeChips(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(HomeTab.values().toList(), key = { _, it -> it.name }) { index, item ->
            val active = item == selected
            val interaction = remember { MutableInteractionSource() }
            // Cambio de color animado al tocar (antes saltaba de golpe).
            val background by animateColorAsState(
                targetValue = if (active) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "chipBackground",
            )
            val foreground by animateColorAsState(
                targetValue = if (active) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "chipForeground",
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                color = foreground,
                modifier = Modifier
                    .enterOnVisible(delayMillis = index * 40)
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(bounded = true),
                        onClick = { onSelect(item) },
                    )
                    .pressScale(interaction, pressedScale = 0.93f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** Cada cuánto rotan las tarjetas grandes del tab "Todo". */
private const val HERO_ROTATION_MS = 60 * 60 * 1000L

/**
 * Tarjetas grandes del Home: prioriza lo que el usuario escucha de verdad
 * (historial reciente y favoritos) y completa con el gusto editorial,
 * intercalando orígenes para que no salgan siempre las mismas ni seguidas
 * del mismo tipo. La rotación por hora hace que varíe a lo largo del día.
 */
private fun buildHeroSongs(
    taste: List<Song>,
    favorites: List<Song>,
    recent: List<Song>,
): List<Song> {
    val picked = LinkedHashMap<String, Song>()
    fun offer(song: Song) {
        if (picked.size >= HERO_COUNT) return
        if (song.id.isNotBlank()) picked.putIfAbsent(song.id, song)
    }

    val hourIndex = ((System.currentTimeMillis() / HERO_ROTATION_MS) % 24).toInt()

    // 1) Historial: lo más reciente primero, pero con rotación horaria para
    //    que no sea siempre la misma cola.
    val recentRing = if (recent.isEmpty()) {
        recent
    } else {
        val offset = hourIndex % recent.size
        recent.drop(offset) + recent.take(offset)
    }
    recentRing.forEach(::offer)

    // 2) Favoritos alternados con gusto editorial: ambos giran por hora.
    fun <T> rotated(items: List<T>): List<T> {
        if (items.size < 2) return items
        val offset = hourIndex % items.size
        return items.drop(offset) + items.take(offset)
    }
    val favs = rotated(favorites)
    val tastes = rotated(taste)
    val maxRows = maxOf(favs.size, tastes.size)
    for (i in 0 until maxRows) {
        if (picked.size >= HERO_COUNT) break
        if (i < favs.size) offer(favs[i])
        if (i < tastes.size) offer(tastes[i])
    }

    // 3) Relleno con lo que quede del pool, en el mismo orden rotado.
    rotated((taste + favorites).distinctBy { it.id }).forEach(::offer)

    return picked.values.toList()
}

private const val HERO_COUNT = 12

@Composable
private fun DailyDiscoveryCard(
    song: Song,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(214.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onPlay,
            )
            .pressScale(interaction, pressedScale = 0.98f),
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.90f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .height(92.dp)
            .enterOnVisible()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.95f)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SongShelf(
    title: String,
    songs: List<Song>,
    onPlay: (List<Song>, Int, Boolean) -> Unit,
    favoriteIds: Set<String> = emptySet(),
) {
    if (songs.isEmpty()) return
    Spacer(Modifier.height(24.dp))
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(
            title = title,
            playAll = true,
            onPlayAll = { onPlay(songs, 0, false) },
        )
        Spacer(Modifier.height(10.dp))
        GroupedSongList(
            songs = songs,
            favoriteIds = favoriteIds,
            onPlay = { index -> onPlay(songs, index, false) },
        )
    }
}

/** Chip de estado de ánimo estilo Echo: píldora gris compacta de 40 dp. */
@Composable
private fun MoodTile(
    mood: Mood,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(40.dp)
            .enterOnVisible()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.95f)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            MusicLoader(
                size = 16.dp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = mood.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Tarjeta del chart con el puesto en gran letra sobre la carátula. */
@Composable
private fun ChartTile(
    rank: Int,
    song: Song,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(140.dp)
            .enterOnVisible()
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.95f),
    ) {
        Box {
            Artwork(
                url = song.thumbnailUrl,
                cornerRadius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.labelSmall,
            color = Faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
