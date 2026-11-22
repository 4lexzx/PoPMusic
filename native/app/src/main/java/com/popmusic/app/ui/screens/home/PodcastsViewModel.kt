package com.popmusic.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.data.podcasts.Podcast
import com.popmusic.app.data.podcasts.PodcastEpisode
import com.popmusic.app.data.podcasts.PodcastRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PodcastsUiState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val query: String = "",
    val results: List<Podcast> = emptyList(),
    val top: List<Podcast> = emptyList(),
    val subscriptions: List<Podcast> = emptyList(),
    val recent: List<PodcastEpisode> = emptyList(),
    val selected: Podcast? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
    val episodesLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Estado de la pestaña Podcast del Inicio: catálogo (iTunes), recientes,
 * suscripciones y episodios del feed seleccionado.
 */
class PodcastsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PodcastRepository(application)
    private val _state = MutableStateFlow(PodcastsUiState())
    val state: StateFlow<PodcastsUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    /** Carga inicial: recientes, suscripciones y destacados. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val (recent, subs, top) = withContext(Dispatchers.IO) {
                Triple(
                    repository.recent(),
                    repository.subscriptions(),
                    runCatching { repository.top() }.getOrDefault(emptyList()),
                )
            }
            _state.value = _state.value.copy(
                loading = false,
                loaded = true,
                recent = recent,
                subscriptions = subs,
                top = top,
            )
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), error = null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(450) // debounce mientras escribe
            _state.value = _state.value.copy(episodesLoading = true, error = null)
            val found = withContext(Dispatchers.IO) {
                runCatching { repository.search(query) }.getOrDefault(emptyList())
            }
            _state.value = _state.value.copy(
                results = found,
                episodesLoading = false,
                error = if (found.isEmpty()) "Sin resultados para «$query»" else null,
            )
        }
    }

    /** Busca una categoría concreta (chip). */
    fun searchCategory(category: String) {
        _state.value = _state.value.copy(query = category)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(episodesLoading = true, error = null)
            val found = withContext(Dispatchers.IO) {
                runCatching { repository.search(category) }.getOrDefault(emptyList())
            }
            _state.value = _state.value.copy(
                results = found,
                episodesLoading = false,
                error = if (found.isEmpty()) "Sin resultados en $category" else null,
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.value = _state.value.copy(query = "", results = emptyList(), error = null)
    }

    /** Abre un podcast y carga sus episodios del feed. */
    fun select(podcast: Podcast) {
        if (_state.value.selected?.id == podcast.id) return
        _state.value = _state.value.copy(
            selected = podcast,
            episodes = emptyList(),
            episodesLoading = true,
            error = null,
        )
        viewModelScope.launch {
            val episodes = withContext(Dispatchers.IO) {
                runCatching { repository.episodes(podcast.feedUrl, podcast) }
                    .getOrDefault(emptyList())
            }
            _state.value = _state.value.copy(
                episodes = episodes,
                episodesLoading = false,
                error = if (episodes.isEmpty()) "No se pudieron cargar los episodios." else null,
            )
        }
    }

    fun closeSelected() {
        _state.value = _state.value.copy(selected = null, episodes = emptyList(), error = null)
    }

    fun isSubscribed(id: String): Boolean = repository.isSubscribed(id)

    fun toggleSubscribe(podcast: Podcast) {
        viewModelScope.launch {
            val nowSubscribed = withContext(Dispatchers.IO) {
                repository.toggleSubscribe(podcast)
            }
            val subs = withContext(Dispatchers.IO) { repository.subscriptions() }
            _state.value = _state.value.copy(subscriptions = subs)
            // Refresca el flag del podcast seleccionado sin recargar el feed.
            _state.value.selected?.let { sel ->
                if (sel.id == podcast.id && !nowSubscribed && subs.none { it.id == sel.id }) {
                    // Sigue seleccionado; el botón se recalcula con isSubscribed().
                }
            }
        }
    }

    /** Registra el episodio como reciente ("continuar escuchando"). */
    fun markPlayed(episode: PodcastEpisode) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markPlayed(episode)
            val recent = withContext(Dispatchers.IO) { repository.recent() }
            _state.value = _state.value.copy(recent = recent)
        }
    }

    /** Episodio → canción reproducible por el motor actual (URI directa mp3). */
    fun playableSongs(): List<Song> = _state.value.episodes.map { it.toSong() }
}

/** Convierte un episodio en [Song] con la URL directa del mp3. */
fun PodcastEpisode.toSong(): Song = Song(
    id = id,
    title = title,
    artist = podcastTitle,
    durationSeconds = durationSeconds,
    thumbnailUrl = artworkUrl,
    streamUrl = audioUrl,
)
