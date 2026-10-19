package com.popmusic.app.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.domain.Song
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    private val connection = app.playerConnection

    val state: StateFlow<PlayerUiState> = connection.state

    fun play(songs: List<Song>, startIndex: Int = 0) = connection.play(songs, startIndex)

    fun addToQueue(songs: List<Song>) = connection.addToQueue(songs)

    fun next() = connection.next()

    fun previous() = connection.previous()

    fun togglePlayPause() = connection.togglePlayPause()

    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)

    fun setRepeatMode(mode: Int) = connection.setRepeatMode(mode)

    fun setShuffle(enabled: Boolean) = connection.setShuffle(enabled)

    fun clearError() = connection.clearError()
}
