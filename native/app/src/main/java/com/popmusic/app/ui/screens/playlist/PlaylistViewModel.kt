package com.popmusic.app.ui.screens.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PoPMusicApplication).libraryRepository

    private val playlistId = MutableStateFlow("")

    val playlist: StateFlow<Playlist?> = combine(playlistId, repository.state) { id, _ ->
        repository.playlist(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Playlists como destino para "Añadir a otra playlist" (sin la actual). */
    val playlists: StateFlow<List<Playlist>> =
        combine(playlistId, repository.state) { id, state ->
            state.playlists.filter { it.id != id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(id: String) {
        playlistId.value = id
    }

    fun removeSong(songId: String) {
        repository.removeFromPlaylist(playlistId.value, songId)
    }

    /** Reordenar: mueve la canción una posición arriba/abajo. */
    fun moveSong(songId: String, up: Boolean) {
        repository.moveSong(playlistId.value, songId, up)
    }

    /** Copia la canción a otra playlist (no la quita de esta). */
    fun copyToPlaylist(targetId: String, song: Song) {
        repository.addToPlaylist(targetId, song)
    }
}
