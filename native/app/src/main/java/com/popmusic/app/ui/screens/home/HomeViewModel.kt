package com.popmusic.app.ui.screens.home

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
import com.popmusic.app.data.recommendations.DiscoverTheme
import com.popmusic.app.data.recommendations.Mood
import com.popmusic.app.data.recommendations.PODCAST_TOPICS
import com.popmusic.app.data.recommendations.RecommendationsRepository
import com.popmusic.app.data.recommendations.TASTE_GENRES
import com.popmusic.app.data.recommendations.TasteRecommendations
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.json.JSONArray
import org.json.JSONObject

data class HomeSection(
    val title: String,
    val songs: List<Song>,
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val sections: List<HomeSection> = emptyList(),
    val taste: TasteRecommendations = TasteRecommendations(),
    val error: String? = null,
    val discoverSections: List<HomeSection> = emptyList(),
    val isLoadingMore: Boolean = false,
    val discoverEnded: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = YouTubeRepository()
    private val charts = ChartsRepository(application)
    private val recommendations = RecommendationsRepository()
    private val library = (application as PoPMusicApplication).libraryRepository
    private val tastePrefs = (application as PoPMusicApplication).preferences
    private val cache = application.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
    private var loadJob: Job? = null
    private var moreJob: Job? = null
    private val seenIds: MutableSet<String> =
        java.util.Collections.synchronizedSet(mutableSetOf())
    private val usedThemeQueries: MutableSet<String> =
        java.util.Collections.synchronizedSet(mutableSetOf())
    private val themeQueue = ArrayDeque<DiscoverTheme>()
    private var themeRefills = 0
    private var emptyRounds = 0

    @Volatile
    private var cacheSavedAt = 0L

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val favorites: StateFlow<com.popmusic.app.data.library.LibraryState> = library.state

    /** Secciones "Descubre más": el usuario puede desactivarlas en Ajustes. */
    val discoverEnabled: StateFlow<Boolean> = tastePrefs.playback
        .map { it.showDiscoverSections }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            true,
        )

    /** Preferencias de reproducción (filtros de contenido, miniatura…). */
    val playback = tastePrefs.playback

    /** Tendencias del chart (misma fuente que la pestaña Buscar, región por defecto). */
    var chart by mutableStateOf<List<Song>>(emptyList())
        private set

    /** Etiqueta del estado de ánimo que se está cargando (null = ninguno). */
    var moodLoading by mutableStateOf<String?>(null)
        private set

    private data class EditorialQuery(
        val title: String,
        val query: String,
        val podcasts: Boolean = false,
    )

    init {
        android.util.Log.d("StartupPerf", "HomeViewModel init START")
        _state.value = HomeUiState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            restoreCache()
            if (!isCacheFresh()) {
                load()
            } else {
                android.util.Log.d("StartupPerf", "Home cache fresh, load skipped")
                loadMore()
            }
        }
        val lastHandled = HomeRefreshBus.resumedAt.value
        viewModelScope.launch {
            HomeRefreshBus.resumedAt.collect { timestamp ->
                if (timestamp > lastHandled && !isCacheFresh()) load()
            }
        }
        viewModelScope.launch {
            var last = tastePrefs.taste.value
            tastePrefs.taste.collect { taste ->
                if (taste != last) {
                    last = taste
                    load()
                }
            }
        }
        android.util.Log.d("StartupPerf", "HomeViewModel init END")
        viewModelScope.launch {
            // Tanda 6 (punto 1): tendencias = listas diarias de Spotify Charts
            // (Top 50 Global + Top 50 del país) resueltas a YouTube y con
            // filtro estricto de música. Si Spotify falla, fallback al
            // kiosk de tendencias de YouTube.
            val chartSongs = runCatching { charts.topSongs() }
                .getOrDefault(emptyList())
                .ifEmpty {
                    runCatching { repository.trendingMusic() }.getOrDefault(emptyList())
                }
            chart = ContentFilters.musicOnly(chartSongs)
        }
    }

    /** Busca la mezcla de un mood y entrega las canciones para reproducirlas. */
    fun playMood(mood: Mood, onReady: (List<Song>) -> Unit) {
        if (moodLoading != null) return
        moodLoading = mood.label
        viewModelScope.launch {
            val songs = runCatching { repository.search(mood.query) }
                .getOrDefault(emptyList())
            moodLoading = null
            if (songs.isNotEmpty()) onReady(songs)
        }
    }

    private fun isCacheFresh(): Boolean {
        val savedAt = cacheSavedAt
        return savedAt > 0L && System.currentTimeMillis() - savedAt < CACHE_TTL_MS
    }

    fun loadMore() {
        if (moreJob?.isActive == true) return
        if (_state.value.discoverEnded) return
        if (!tastePrefs.playback.value.showDiscoverSections) {
            _state.value = _state.value.copy(isLoadingMore = false, discoverEnded = true)
            return
        }
        val theme = nextTheme()
        if (theme == null) {
            _state.value = _state.value.copy(discoverEnded = true, isLoadingMore = false)
            return
        }
        moreJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            val songs = runCatching {
                recommendations.discoverSongs(theme.queries, seenIds, DISCOVER_CARDS)
            }.getOrDefault(emptyList())
            _state.value = _state.value.copy(isLoadingMore = false)
            if (songs.isEmpty()) {
                emptyRounds++
                if (emptyRounds >= 2) {
                    _state.value = _state.value.copy(discoverEnded = true)
                }
                return@launch
            }
            emptyRounds = 0
            usedThemeQueries.addAll(theme.queries)
            seenIds.addAll(songs.map { it.id })
            _state.value = _state.value.copy(
                discoverSections = _state.value.discoverSections +
                    HomeSection(theme.title, songs),
            )
        }
    }

    private fun nextTheme(): DiscoverTheme? {
        if (themeQueue.isEmpty()) {
            if (themeRefills >= MAX_THEME_REFILLS) return null
            val fresh = recommendations.discoverThemes(
                library.state.value,
                tastePrefs.taste.value.genres,
                tastePrefs.taste.value.artists,
            )
                .filter { theme -> theme.queries.none { it in usedThemeQueries } }
                .shuffled()
            if (fresh.isEmpty()) return null
            themeRefills++
            themeQueue.addAll(fresh)
        }
        return themeQueue.removeFirstOrNull()
    }

    fun load() {
        if (loadJob?.isActive == true) return

        val startedAt = android.os.SystemClock.uptimeMillis()
        loadJob = viewModelScope.launch {
            val hadContent = _state.value.sections.isNotEmpty() || _state.value.taste.songs.isNotEmpty()
            _state.value = _state.value.copy(isLoading = true, error = null)

            val sections = runCatching {
                supervisorScope {
                    val editorial = async {
                        editorialQueries().map { editorialQuery ->
                            async {
                                val songs = runCatching {
                                    if (editorialQuery.podcasts) {
                                        repository.searchVideos(editorialQuery.query)
                                    } else {
                                        repository.search(editorialQuery.query)
                                    }
                                }.getOrDefault(emptyList())
                                editorialQuery.title to songs.take(CARDS_PER_SECTION)
                            }
                        }.awaitAll()
                    }
                    val taste = async {
                        runCatching {
                            recommendations.forTaste(
                                library.state.value,
                                tastePrefs.taste.value.genres,
                                tastePrefs.taste.value.artists,
                            )
                        }.getOrDefault(TasteRecommendations())
                    }
                    editorial.await() to taste.await()
                }
            }

            sections
                .onSuccess { (results, taste) ->
                    android.util.Log.d(
                        "StartupPerf",
                        "HomeViewModel.load done in ${android.os.SystemClock.uptimeMillis() - startedAt}ms",
                    )
                    val loaded = results
                        .filter { it.second.isNotEmpty() }
                        .map { HomeSection(it.first, it.second) }

                    seenIds.addAll(loaded.flatMap { section -> section.songs.map { it.id } })
                    seenIds.addAll(taste.songs.map { it.id })
                    val previous = _state.value
                    _state.value = HomeUiState(
                        isLoading = false,
                        sections = loaded,
                        taste = taste,
                        error = if (loaded.isEmpty() && taste.songs.isEmpty()) {
                            "Sin conexión. Comprueba tu red."
                        } else {
                            null
                        },
                        discoverSections = previous.discoverSections,
                        isLoadingMore = previous.isLoadingMore,
                        discoverEnded = previous.discoverEnded,
                    )
                    if (loaded.isNotEmpty() || taste.songs.isNotEmpty()) {
                        viewModelScope.launch(Dispatchers.IO) {
                            saveCache(loaded, taste)
                        }
                    }
                    loadMore()
                }
                .onFailure {
                    _state.value = if (hadContent) {
                        _state.value.copy(isLoading = false, error = null)
                    } else {
                        HomeUiState(
                            isLoading = false,
                            error = "No se pudo cargar la música. Comprueba tu conexión.",
                        )
                    }
                }
        }
    }

    private fun restoreCache() {
        val start = android.os.SystemClock.uptimeMillis()
        val raw = cache.getString(CACHE_KEY, null) ?: return
        val restored = runCatching {
            val root = JSONObject(raw)
            cacheSavedAt = root.optLong(KEY_SAVED_AT, 0L)
            val sectionsJson = root.optJSONArray("sections") ?: JSONArray()
            val sections = buildList {
                for (i in 0 until sectionsJson.length()) {
                    val section = sectionsJson.optJSONObject(i) ?: continue
                    val title = section.optString("title")
                    val songs = decodeSongs(section.optJSONArray("songs") ?: JSONArray())
                    if (title.isNotBlank() && songs.isNotEmpty()) {
                        add(HomeSection(title, songs))
                    }
                }
            }
            val taste = TasteRecommendations(decodeSongs(root.optJSONArray("taste") ?: JSONArray()))
            sections to taste
        }.getOrNull() ?: return

        if (restored.first.isNotEmpty() || restored.second.songs.isNotEmpty()) {
            seenIds.addAll(restored.first.flatMap { section -> section.songs.map { it.id } })
            seenIds.addAll(restored.second.songs.map { it.id })
            _state.value = HomeUiState(
                isLoading = true,
                sections = restored.first,
                taste = restored.second,
            )
        }
        android.util.Log.d("StartupPerf", "HomeViewModel.restoreCache ${android.os.SystemClock.uptimeMillis() - start}ms")
    }

    private fun saveCache(sections: List<HomeSection>, taste: TasteRecommendations) {
        val root = JSONObject()
        root.put(KEY_SAVED_AT, System.currentTimeMillis())
        root.put("sections", JSONArray().apply {
            sections.forEach { section ->
                put(JSONObject().apply {
                    put("title", section.title)
                    put("songs", encodeSongs(section.songs))
                })
            }
        })
        root.put("taste", encodeSongs(taste.songs))
        cache.edit().putString(CACHE_KEY, root.toString()).apply()
    }

    private fun encodeSongs(songs: List<Song>): JSONArray = JSONArray().apply {
        songs.forEach { song ->
            put(JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("durationSeconds", song.durationSeconds)
                if (song.thumbnailUrl != null) put("thumbnail", song.thumbnailUrl)
            })
        }
    }

    private fun decodeSongs(array: JSONArray): List<Song> = buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val title = item.optString("title")
            if (id.isBlank() || title.isBlank()) continue
            add(
                Song(
                    id = id,
                    title = title,
                    artist = item.optString("artist"),
                    durationSeconds = item.optLong("durationSeconds"),
                    thumbnailUrl = if (item.has("thumbnail")) item.optString("thumbnail") else null,
                ),
            )
        }
    }

    /** Secciones editoriales: si el usuario definió gustos, se arman con ellos; si no, fallback. */
    private fun editorialQueries(): List<EditorialQuery> {
        val taste = tastePrefs.taste.value
        val queries = if (!taste.completed) {
            QUERIES
        } else {
            buildList {
                val genreLabels = taste.genres.toList()
                val genres = TASTE_GENRES.filter { it.label in genreLabels }
                genres.shuffled().take(3).forEach { genre ->
                    add(EditorialQuery(genre.label, "${genre.query} exitos"))
                }
                taste.artists.shuffled().take(2).forEach { artist ->
                    add(EditorialQuery("Tu artista: $artist", "$artist songs"))
                }
                if (taste.podcast) {
                    val topics = PODCAST_TOPICS.filter { it.label in taste.podcastTopics }
                    if (topics.isEmpty()) {
                        add(EditorialQuery("Podcasts", "podcast en espanol", podcasts = true))
                    } else {
                        topics.shuffled().take(2).forEach { topic ->
                            add(EditorialQuery("Podcasts · ${topic.label}", topic.query, podcasts = true))
                        }
                    }
                }
                if (isEmpty()) {
                    addAll(QUERIES)
                }
            }
        }
        // Echo siempre muestra una sección de podcasts: si el perfil no la aportó, va el genérico.
        return if (queries.any { it.podcasts }) {
            queries
        } else {
            queries + EditorialQuery("Podcasts", "podcast en espanol", podcasts = true)
        }
    }

    private companion object {
        const val CACHE_NAME = "home_cache"
        const val CACHE_KEY = "last_home"
        const val KEY_SAVED_AT = "savedAt"
        const val CARDS_PER_SECTION = 8
        const val CACHE_TTL_MS = 30L * 60L * 1000L
        const val DISCOVER_CARDS = 8
        const val MAX_THEME_REFILLS = 3

        val QUERIES = listOf(
            EditorialQuery("Pop en español", "pop en español exitos"),
            EditorialQuery("Rock en español", "rock en español exitos"),
            EditorialQuery("Reggaetón", "reggaeton exitos"),
            EditorialQuery("Ritmos latinos", "latin pop exitos"),
        )
    }
}
