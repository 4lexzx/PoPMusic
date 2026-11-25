package com.popmusic.app.ui.screens.flow

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.recommendations.DiscoveryPool
import com.popmusic.app.data.recommendations.RecommendationsRepository
import com.popmusic.app.domain.Lyrics
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class MusicFlowState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Feed del Music Flow (descubrimiento vertical estilo Resso/Echo).
 *
 * Arranca con lo que ya hay en la biblioteca local para que el primer swipe
 * tenga contenido inmediato y, en paralelo, pide recomendaciones a YouTube.
 */
class MusicFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    private val recommendations = RecommendationsRepository()
    private val lyricsRepository = app.lyricsRepository

    private var loadedOnce = false
    private var lyricsJob: Job? = null
    private var inFlightSongId: String? = null
    private val lyricsCache = HashMap<String, Lyrics?>()

    var state by mutableStateOf(MusicFlowState())
        private set

    var lyrics by mutableStateOf<Lyrics?>(null)
        private set

    var offsetMs by mutableIntStateOf(0)
        private set

    var lyricsSongId by mutableStateOf<String?>(null)
        private set

    fun ensureLoaded() {
        if (loadedOnce) return
        loadedOnce = true
        val library = app.libraryRepository.state.value
        val local = (library.favorites + library.playlists.flatMap { it.songs })
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }

        state = state.copy(
            songs = local,
            isLoading = true,
            error = null,
        )

        viewModelScope.launch {
            val taste = app.preferences.taste.value
            val fresh = runCatching {
                recommendations.forTaste(
                    library = library,
                    tasteGenres = taste.genres,
                    tasteArtists = taste.artists,
                )
            }.onFailure {
                Log.w(TAG, "No se pudo cargar el feed del flow", it)
            }.getOrNull()?.songs.orEmpty()

            DiscoveryPool.add(fresh)
            val merged = (local + fresh).distinctBy { it.id }
            state = MusicFlowState(
                songs = merged,
                isLoading = false,
                error = if (merged.isEmpty()) {
                    "Necesitamos algo de música para armar tu flow"
                } else {
                    null
                },
            )
        }
    }

    fun refresh() {
        loadedOnce = false
        ensureLoaded()
    }

    /** Carga (con caché) la letra de la canción visible en el pager. */
    fun loadLyrics(song: Song?, durationMs: Long) {
        if (song == null || song.id.isBlank() || song.title.isBlank()) {
            lyrics = null
            lyricsSongId = null
            offsetMs = 0
            return
        }
        if (lyricsSongId == song.id && lyricsCache.containsKey(song.id)) {
            lyrics = lyricsCache[song.id]
            return
        }
        lyricsSongId = song.id
        lyrics = lyricsCache[song.id]
        val duration = durationMs.takeIf { it > 0L } ?: (song.durationSeconds * 1000L)
        offsetMs = runCatching {
            lyricsRepository.offsetMs(song.id, song.title, song.artist, duration)
        }.getOrDefault(0)

        if (lyricsCache.containsKey(song.id)) return

        // Si ya hay una carga en vuelo para esta misma canción, no la reiniciamos
        // (p. ej. cuando llega la duración real desde el reproductor).
        if (inFlightSongId == song.id && lyricsJob?.isActive == true) return

        lyricsJob?.cancel()
        inFlightSongId = song.id
        lyricsJob = viewModelScope.launch {
            val loaded = runCatching {
                lyricsRepository.load(song.id, song.title, song.artist, duration)
            }.onFailure {
                if (it !is kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "Sin letra para ${song.title}", it)
                }
            }.getOrNull()
            lyricsCache[song.id] = loaded
            if (lyricsSongId == song.id) {
                lyrics = loaded
            }
        }
    }

    private companion object {
        const val TAG = "MusicFlow"
    }
}
