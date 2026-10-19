package com.popmusic.app.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    private val connection = app.playerConnection
    private val repository = YouTubeRepository()

    val state: StateFlow<PlayerUiState> = connection.state

    private val queue = mutableListOf<Song>()
    private var index = -1
    private var loadJob: Job? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun play(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        queue.clear()
        queue.addAll(songs)
        index = startIndex.coerceIn(0, songs.lastIndex)
        loadAndPlay()
    }

    fun next() {
        if (queue.isEmpty()) return
        index = (index + 1) % queue.size
        loadAndPlay()
    }

    fun previous() {
        if (queue.isEmpty()) return
        index = if (index - 1 < 0) queue.lastIndex else index - 1
        loadAndPlay()
    }

    fun togglePlayPause() = connection.togglePlayPause()

    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)

    fun setRepeatMode(mode: Int) = connection.setRepeatMode(mode)

    fun setShuffle(enabled: Boolean) = connection.setShuffle(enabled)

    fun clearError() {
        _error.value = null
    }

    private fun loadAndPlay() {
        val song = queue.getOrNull(index) ?: return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val url = runCatching { repository.resolveAudioUrl(song.id) }
                .onFailure { _error.value = it.message ?: "No se pudo obtener el audio" }
                .getOrNull()

            if (url != null) {
                connection.play(song, url)
            } else if (_error.value == null) {
                _error.value = "No se encontró una fuente de audio"
            }

            _isLoading.value = false
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
    }
}
