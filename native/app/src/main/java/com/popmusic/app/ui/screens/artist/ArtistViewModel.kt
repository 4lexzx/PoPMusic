package com.popmusic.app.ui.screens.artist

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.recommendations.DiscoveryPool
import com.popmusic.app.data.recommendations.RecommendationText
import com.popmusic.app.data.recommendations.RecommendationsRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Grandes éxitos de un artista, alimentados por el mismo catálogo que la radio. */
class ArtistViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    private val recommendations = RecommendationsRepository()
    private var loadJob: Job? = null

    var artist by mutableStateOf("")
        private set
    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun open(name: String) {
        val clean = name.trim()
        if (clean.isBlank() || clean == artist) return
        loadJob?.cancel()
        artist = clean
        songs = emptyList()
        error = null
        isLoading = true
        val key = RecommendationText.key(clean)

        loadJob = viewModelScope.launch {
            val found = runCatching {
                recommendations.discoverSongs(
                    queries = listOf("$clean exitos", "$clean canciones", "$clean songs"),
                    excludeIds = emptySet(),
                    limit = 40,
                )
            }.onFailure { Log.w(TAG, "No se pudo cargar los exitos de $clean", it) }
                .getOrDefault(emptyList())
                .filter { song ->
                    RecommendationText.artistMatchesStrict(song.artist, key) ||
                        RecommendationText.key(song.title).startsWith("$key - ")
                }

            val library = app.libraryRepository.state.value
            val local = (library.favorites + library.downloads +
                library.playlists.flatMap { it.songs })
                .filter { RecommendationText.artistMatchesStrict(it.artist, key) }

            songs = (local + found).distinctBy { it.id }
            DiscoveryPool.add(songs, listOf(clean))
            isLoading = false
            error = if (songs.isEmpty()) "No se encontraron canciones de $clean" else null
        }
    }

    private companion object {
        const val TAG = "ArtistPlaylist"
    }
}
