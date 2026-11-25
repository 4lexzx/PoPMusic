package com.popmusic.app.ui.screens.search

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.charts.ChartsRepository
import com.popmusic.app.data.filter.ContentFilters
import com.popmusic.app.data.library.LibraryState
import com.popmusic.app.data.recommendations.DiscoveryPool
import com.popmusic.app.data.recommendations.RecommendationsRepository
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Fuente de búsqueda estilo Echo: ONLINE = "Global", LOCAL = biblioteca. */
enum class SearchSource { LOCAL, ONLINE }

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    private val repository = YouTubeRepository()
    private val charts = ChartsRepository(application)
    private val recommendations = RecommendationsRepository()
    private val prefs =
        application.getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

    /** Biblioteca en vivo: alimenta el corazón de favorito junto al nombre (ítem 4). */
    val library: StateFlow<LibraryState> = app.libraryRepository.state

    var query by mutableStateOf("")
        private set

    /** Fuente activa (persistida): el toggle globo/biblioteca de la barra. */
    var source by mutableStateOf(loadSource())
        private set

    var results by mutableStateOf<List<Song>>(emptyList())
        private set

    var forYou by mutableStateOf<List<Song>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /** Chart de tendencias (estilo "Top 100" de Echo). */
    var trending by mutableStateOf<List<Song>>(emptyList())
        private set

    var chartCountry by mutableStateOf(DEFAULT_CHART_COUNTRY)
        private set

    var chartPage by mutableStateOf(0)
        private set

    var isChartLoading by mutableStateOf(true)
        private set

    /** Artistas del chart agrupados, para la fila "Trending Artists".
     *  Se mantiene el orden de aparición en el chart para que la insignia
     *  de rango coincida con la posición real. */
    val trendingArtists: List<Pair<String, Song>>
        get() = chartList
            .groupBy { it.artist }
            .filterKeys { it.isNotBlank() }
            .map { (artist, songs) -> artist to songs.first() }

    val chartPageCount: Int
        get() = (chartList.size + CHART_PAGE_SIZE - 1) / CHART_PAGE_SIZE

    /** Artistas distintos de la última búsqueda, para la tarjeta de artista. */
    var artistMatches by mutableStateOf<List<Pair<String, Song>>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            val taste = app.preferences.taste.value
            forYou = filtered(runCatching {
                recommendations.forTaste(
                    library = app.libraryRepository.state.value,
                    tasteGenres = taste.genres,
                    tasteArtists = taste.artists,
                ).songs
            }.getOrDefault(emptyList()))
            DiscoveryPool.add(forYou)
        }
        loadTrending(DEFAULT_CHART_COUNTRY)
    }

    /** Aplica los filtros "Ocultar videos"/"Ocultar Shorts" de Ajustes.
     *  Se llama también al componer para reaccionar a los cambios de ajustes. */
    fun filtered(songs: List<Song>): List<Song> {
        val playback = app.preferences.playback.value
        return ContentFilters.apply(songs, playback.hideVideos, playback.hideShorts)
    }

    /** Chart ya filtrado: base común de paginación, filas y artistas.
     *  Siempre pasa por musicOnly para que no entren IA/niños/vídeos. */
    val chartList: List<Song>
        get() = ContentFilters.musicOnly(filtered(trending))

    fun loadTrending(country: String) {
        chartCountry = country
        chartPage = 0
        isChartLoading = true
        viewModelScope.launch {
            // Tanda 6 (punto 1): chart de Spotify Charts (global + país),
            // resuelto a YouTube; `country` queda solo como etiqueta.
            // Si Spotify falla, fallback al kiosk de tendencias de YouTube.
            val songs = runCatching { charts.topSongs() }
                .getOrDefault(emptyList())
                .ifEmpty {
                    runCatching { repository.trendingMusic(country) }
                        .getOrDefault(emptyList())
                }
            trending = songs
            DiscoveryPool.add(songs)
            isChartLoading = false
        }
    }

    fun goToChartPage(page: Int) {
        if (trending.isEmpty()) return
        chartPage = page.coerceIn(0, (chartPageCount - 1).coerceAtLeast(0))
    }

    fun chartSlice(): List<Song> {
        val list = chartList
        if (list.isEmpty()) return emptyList()
        val from = chartPage * CHART_PAGE_SIZE
        return list.drop(from).take(CHART_PAGE_SIZE)
    }

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        query = value
        searchJob?.cancel()

        val term = value.trim()
        if (term.length < 2) {
            results = emptyList()
            artistMatches = emptyList()
            error = null
            isLoading = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            performSearch(term)
        }
    }

    fun search() {
        val term = query.trim()
        if (term.isEmpty()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(term) }
    }

    fun toggleSource() {
        source = if (source == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
        prefs.edit().putString(KEY_SOURCE, source.name).apply()
        // Re-ejecuta la búsqueda actual con la nueva fuente.
        searchJob?.cancel()
        val term = query.trim()
        if (term.length < 2) {
            results = emptyList()
            error = null
            isLoading = false
            return
        }
        searchJob = viewModelScope.launch { performSearch(term) }
    }

    private suspend fun performSearch(term: String) {
        if (source == SearchSource.LOCAL) {
            // Biblioteca: filtrado local instantáneo, sin red ni loader.
            isLoading = false
            error = null
            results = localMatches(term)
            artistMatches = artistCards(results, term)
            DiscoveryPool.add(results, listOf(term))
            return
        }
        isLoading = true
        error = null
        val outcome = try {
            Result.success(repository.search(term))
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Throwable) {
            Result.failure(failure)
        }

        // Descarta respuestas que ya no corresponden al texto actual.
        if (query.trim() != term) return

        outcome
            .onFailure { error = it.message ?: "Error de búsqueda" }
            .onSuccess { songs ->
                results = filtered(songs)
                artistMatches = artistCards(results, term)
                DiscoveryPool.add(results, listOf(term))
            }
        isLoading = false
    }

    /** Coincidencias en favoritos + descargas + playlists (sin duplicados). */
    private fun localMatches(term: String): List<Song> {
        val needle = normalize(term)
        val library = app.libraryRepository.state.value
        val candidates = library.favorites +
            library.downloads +
            library.playlists.flatMap { it.songs }
        val seen = HashSet<String>()
        val matches = ArrayList<Song>()
        for (song in candidates) {
            if (!seen.add(song.id)) continue
            if (normalize(song.title).contains(needle) ||
                normalize(song.artist).contains(needle)
            ) {
                matches += song
            }
        }
        return filtered(matches)
    }

    /** Artistas de los resultados, con los que concuerdan mejor primero. */
    private fun artistCards(songs: List<Song>, term: String): List<Pair<String, Song>> {
        val needle = normalize(term)
        return songs
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist.trim() }
            .map { (artist, group) ->
                val key = normalize(artist)
                val score = when {
                    key == needle -> 3
                    key.contains(needle) -> 2
                    needle.contains(key) && key.length >= 3 -> 1
                    else -> 0
                }
                val cover = group.firstOrNull { !it.thumbnailUrl.isNullOrBlank() } ?: group.first()
                Triple(artist, cover, score to group.size)
            }
            .filter { it.third.first > 0 || needle.isBlank() }
            .sortedWith(compareByDescending<Triple<String, Song, Pair<Int, Int>>> { it.third.first }
                .thenByDescending { it.third.second })
            .take(3)
            .map { it.first to it.second }
    }

    private fun loadSource(): SearchSource = runCatching {
        SearchSource.valueOf(
            prefs.getString(KEY_SOURCE, SearchSource.ONLINE.name) ?: SearchSource.ONLINE.name,
        )
    }.getOrDefault(SearchSource.ONLINE)

    /** Minúsculas sin acentos para que "canción" encuentre "Cancion". */
    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    fun clearError() {
        error = null
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
        const val CHART_PAGE_SIZE = 5
        /**
         * Tendencias GLOBALES (ítem 1 de la tanda 5): "US" actúa como
         * proxy global (música en inglés + hispano internacional, p. ej.
         * Bad Bunny) en vez de sacar el chart solo de México.
         */
        const val DEFAULT_CHART_COUNTRY = "US"
        const val KEY_SOURCE = "search_source"
    }
}
