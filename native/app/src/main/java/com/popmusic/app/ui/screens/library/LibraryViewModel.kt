package com.popmusic.app.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.library.LibraryState
import com.popmusic.app.domain.Song
import kotlinx.coroutines.flow.StateFlow

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PoPMusicApplication).libraryRepository

    val state: StateFlow<LibraryState> = repository.state

    fun toggleFavorite(song: Song) = repository.toggleFavorite(song)

    fun addToPlaylist(playlistId: String, song: Song) = repository.addToPlaylist(playlistId, song)

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        repository.createPlaylist(name)
    }

    fun deletePlaylist(playlistId: String) = repository.deletePlaylist(playlistId)
}
