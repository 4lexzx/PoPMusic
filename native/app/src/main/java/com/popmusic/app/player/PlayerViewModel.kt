package com.popmusic.app.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.recommendations.RecommendationsRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    private val connection = app.playerConnection
    private val recommendations = RecommendationsRepository()
    private var autoExtend = false
    private var queueIds: Set<String> = emptySet()
    private var radioJob: Job? = null
    private var prefetchedKey: String? = null
    /** Artista de la canción elegida, por si el estado aún no lo trae. */
    private var lastArtist: String? = null

    val state: StateFlow<PlayerUiState> = connection.state
    val positionMs: StateFlow<Long> = connection.positionMs

    init {
        connection.onNearQueueEnd = {
            if (autoExtend && radioJob?.isActive != true) {
                radioJob = viewModelScope.launch {
                    // Semilla = lo que está sonando ahora (no la primera canción
                    // de la cola), para que la radio siga al mismo artista.
                    val current = connection.state.value
                    val seed = current.artist.ifBlank { lastArtist.orEmpty() }
                    val batch = runCatching {
                        recommendations.radio(
                            library = app.libraryRepository.state.value,
                            excludeIds = queueIds,
                            seedArtist = seed,
                        )
                    }.onFailure {
                        Log.w(TAG, "No se pudo ampliar la cola", it)
                    }.getOrDefault(emptyList())
                    if (batch.isNotEmpty()) {
                        queueIds = queueIds + batch.map { it.id }
                        connection.addToQueue(batch)
                        Log.d(TAG, "Radio añadió ${batch.size} canciones a la cola")
                    }
                }
            }
        }

        viewModelScope.launch {
            connection.state.collect { state ->
                if (state.mediaId.isBlank() || state.durationMs <= 0L) return@collect
                val key = "${state.mediaId}|${state.durationMs / 1000}"
                if (key == prefetchedKey) return@collect
                prefetchedKey = key
                app.lyricsRepository.prefetch(
                    songId = state.mediaId,
                    title = state.title,
                    artist = state.artist,
                    durationMs = state.durationMs,
                )
            }
        }
    }

    fun play(songs: List<Song>, startIndex: Int = 0, autoExtend: Boolean = false) {
        this.autoExtend = autoExtend
        lastArtist = songs.getOrNull(startIndex)?.artist
        queueIds = songs.mapTo(HashSet()) { it.id }
        radioJob?.cancel()
        connection.play(songs, startIndex)
    }

    fun addToQueue(songs: List<Song>) {
        queueIds = queueIds + songs.map { it.id }
        connection.addToQueue(songs)
    }

    fun next() = connection.next()

    fun previous() = connection.previous()

    fun togglePlayPause() = connection.togglePlayPause()

    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)

    fun currentPositionMs(): Long = connection.currentPositionMs()

    fun setRepeatMode(mode: Int) = connection.setRepeatMode(mode)

    fun setShuffle(enabled: Boolean) = connection.setShuffle(enabled)

    fun prefetchAllQueue() = connection.prefetchAllQueue()

    fun setInstantPlay(enabled: Boolean) {
        app.preferences.updatePlayback { it.copy(instantPlay = enabled) }
    }

    val instantPlay: Flow<Boolean> = app.preferences.playback.map { it.instantPlay }

    fun clearError() = connection.clearError()

    override fun onCleared() {
        connection.onNearQueueEnd = null
        super.onCleared()
    }

    private companion object {
        const val TAG = "PlayerViewModel"
    }
}
