package com.popmusic.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.popmusic.app.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerConnection(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<Song>, Int>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Error de reproduccion", error)
            val mediaController = controller
            if (mediaController != null && mediaController.hasNextMediaItem()) {
                mediaController.seekToNextMediaItem()
                mediaController.prepare()
                mediaController.play()
                return
            }
            _state.value = _state.value.copy(
                error = error.message ?: "No se pudo reproducir esta cancion",
                isBuffering = false,
            )
        }
    }

    init {
        connect()
        scope.launch {
            while (isActive) {
                controller?.let { mediaController ->
                    if (mediaController.isPlaying) {
                        _state.value = _state.value.copy(
                            positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun connect() {
        if (controllerFuture != null) return

        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future

        future.addListener({
            runCatching { future.get() }
                .onSuccess { mediaController ->
                    Log.d(TAG, "MediaController connected")
                    controller = mediaController
                    mediaController.addListener(listener)
                    updateState(mediaController)
                    pendingQueue?.let { (songs, index) ->
                        pendingQueue = null
                        play(mediaController, songs, index)
                    }
                }
                .onFailure { Log.e(TAG, "MediaController connection failed", it) }
        }, ContextCompat.getMainExecutor(context))
    }

    fun play(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        val mediaController = controller
        if (mediaController == null) {
            Log.w(TAG, "Controller not ready, queueing ${songs.size} songs")
            pendingQueue = songs to startIndex
            connect()
            return
        }
        play(mediaController, songs, startIndex)
    }

    private fun play(mediaController: MediaController, songs: List<Song>, startIndex: Int) {
        val items = songs.map { it.toMediaItem() }
        _state.value = _state.value.copy(error = null)
        mediaController.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        mediaController.prepare()
        mediaController.play()
    }

    fun addToQueue(songs: List<Song>) {
        val mediaController = controller ?: return
        mediaController.addMediaItems(songs.map { it.toMediaItem() })
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun stop() {
        controller?.stop()
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(thumbnailUrl?.let(Uri::parse))
                .setIsPlayable(true)
                .build(),
        )
        .build()

    private fun updateState(player: Player) {
        val metadata = player.currentMediaItem?.mediaMetadata
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            hasMedia = player.currentMediaItem != null,
            mediaId = player.currentMediaItem?.mediaId.orEmpty(),
            title = metadata?.title?.toString().orEmpty(),
            artist = metadata?.artist?.toString().orEmpty(),
            artworkUrl = metadata?.artworkUri?.toString(),
            durationMs = player.duration.coerceAtLeast(0L),
            repeatMode = player.repeatMode,
            shuffle = player.shuffleModeEnabled,
        )
    }

    fun release() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    private companion object {
        const val TAG = "PlayerConnection"
    }
}
