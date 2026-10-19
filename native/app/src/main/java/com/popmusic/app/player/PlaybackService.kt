package com.popmusic.app.player

import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.popmusic.app.data.youtube.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val repository = YouTubeRepository()
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(QueueCallback())
            .build()
    }

    private inner class QueueCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            scope.launch {
                val resolved = mediaItems.mapNotNull { resolve(it) }
                future.set(resolved.ifEmpty { mediaItems })
            }
            return future
        }
    }

    private suspend fun resolve(item: MediaItem): MediaItem? {
        if (item.localConfiguration?.uri != null) return item

        val url = runCatching { repository.resolveAudioUrl(item.mediaId) }
            .onFailure { Log.e(TAG, "No se pudo resolver el audio de ${item.mediaId}", it) }
            .getOrNull()

        if (url.isNullOrBlank()) {
            Log.w(TAG, "Sin URL de audio para ${item.mediaId}, se omite")
            return null
        }

        return item.buildUpon().setUri(url).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scopeJob.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
    }

    private companion object {
        const val TAG = "PlaybackService"
    }
}
