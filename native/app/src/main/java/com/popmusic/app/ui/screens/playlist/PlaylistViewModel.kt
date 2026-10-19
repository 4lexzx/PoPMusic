package com.popmusic.app.ui.screens.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.domain.Playlist
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

    fun load(id: String) {
        playlistId.value = id
    }

    fun removeSong(songId: String) {
        repository.removeFromPlaylist(playlistId.value, songId)
    }
}
