package com.popmusic.app.ui.screens.party

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.party.PartySession
import com.popmusic.app.data.party.PartyUiState
import com.popmusic.app.data.recommendations.RecommendationsRepository
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel de la sala: la conexión y la cola viven en [PartySession]
 * (a nivel de aplicación, la sala no se cierra al salir de la pantalla);
 * aquí solo se añade el estado de búsqueda para añadir canciones.
 */
class PartyViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    val session: PartySession = app.partySession
    private val youtube = YouTubeRepository()

    private data class SearchState(
        val query: String = "",
        val searching: Boolean = false,
        val results: List<Song> = emptyList(),
    )

    private val _search = MutableStateFlow(SearchState())

    private val _state = MutableStateFlow(PartyUiState())

    /** Estado combinado (sesión + búsqueda) para la UI. */
    val state: StateFlow<PartyUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    // ————————————————— Pestaña "Mi música" —————————————————

    private val recommendations = RecommendationsRepository()

    /** Playlists locales de la sala (compartidas con la sesión). */
    val library = app.libraryRepository.state

    private val _recommended = MutableStateFlow<List<Song>>(emptyList())
    val recommended: StateFlow<List<Song>> = _recommended.asStateFlow()

    private val _recLoading = MutableStateFlow(false)
    val recLoading: StateFlow<Boolean> = _recLoading.asStateFlow()

    private var recJob: Job? = null

    // ————————————————— Sugerencias por gustos (Buscar) —————————————————

    private val _tasteSuggestions = MutableStateFlow<List<Song>>(emptyList())
    val tasteSuggestions: StateFlow<List<Song>> = _tasteSuggestions.asStateFlow()

    private var tasteJob: Job? = null

    /** Canciones sugeridas según los gustos elegidos en la app (una vez). */
    fun loadTasteSuggestions() {
        if (tasteJob?.isActive == true || _tasteSuggestions.value.isNotEmpty()) return
        tasteJob = viewModelScope.launch {
            val tastes = app.preferences.taste.value
            val songs = runCatching {
                recommendations.forTaste(
                    library = app.libraryRepository.state.value,
                    tasteGenres = tastes.genres,
                    tasteArtists = tastes.artists,
                ).songs
            }.getOrDefault(emptyList())
            _tasteSuggestions.value = songs.take(8)
        }
    }

    /** Carga recomendaciones para la pestaña "Mi música" (una vez). */
    fun loadMyMusic() {
        if (recJob?.isActive == true) return
        recJob = viewModelScope.launch {
            _recLoading.value = true
            val queueIds: Set<String> = session.state.value.snapshot?.queue
                ?.map { it.id }
                ?.toSet()
                ?: emptySet()
            val songs = runCatching {
                recommendations.radio(app.libraryRepository.state.value, excludeIds = queueIds)
            }.getOrDefault(emptyList())
            _recommended.value = songs
            _recLoading.value = false
        }
    }

    init {
        viewModelScope.launch {
            combine(session.state, _search) { s, q ->
                s.copy(
                    searchQuery = q.query,
                    searching = q.searching,
                    searchResults = q.results,
                )
            }.collect { _state.value = it }
        }
    }

    // ————————————————— Acciones de sala (delegadas) —————————————————

    fun createRoom() = session.createRoom()

    fun joinRoom(code: String) = session.joinRoom(code)

    fun leave() {
        session.leave()
        clearSearch()
    }

    fun clearError() = session.clearError()

    fun onCodeChange(code: String) = session.onCodeChange(code)

    fun togglePlay() = session.togglePlay()

    fun next() = session.next()

    fun previous() = session.previous()

    fun jumpTo(index: Int) = session.jumpTo(index)

    fun addSongs(songs: List<Song>) = session.addSongs(songs)

    // ————————————————— Aleatorio / Repetir (reproductor de la sala) —————————————————

    /** Estado del reproductor local para los botones de la sala. */
    val playerState = app.playerConnection.state

    /** Alterna el aleatorio del reproductor de la sala. */
    fun toggleShuffle() {
        val connection = app.playerConnection
        connection.setShuffle(!connection.state.value.shuffle)
    }

    /** Cicla repetir: apagado → toda la cola → una sola → apagado. */
    fun cycleRepeatMode() {
        val connection = app.playerConnection
        val next = when (connection.state.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        connection.setRepeatMode(next)
    }

    // ————————————————— Búsqueda para añadir —————————————————

    fun onSearchQueryChange(query: String) {
        _search.value = _search.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _search.value = _search.value.copy(results = emptyList(), searching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(450)
            _search.value = _search.value.copy(searching = true)
            val results = runCatching { youtube.search(query) }.getOrDefault(emptyList())
            _search.value = _search.value.copy(
                results = results.take(8),
                searching = false,
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchState()
    }
}
