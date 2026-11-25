package com.popmusic.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.popmusic.app.data.recent.RecentStore
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PlayerConnection(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<Song>, Int>? = null
    private var nearEndNotified = false
    private var lastRecordedId: String? = null
    private var playJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefetchPermits = kotlinx.coroutines.sync.Semaphore(6)
    private val youtube = YouTubeRepository()
    private val resolvedUrls = ConcurrentHashMap<String, Pair<String, Long>>()

    var onNearQueueEnd: (() -> Unit)? = null

    /** True mientras se están buscando canciones para alargar la cola. */
    var isQueueGrowing: (() -> Boolean)? = null

    /** Pista desde la que se pidió "siguiente" cuando aún no había ninguna. */
    private var pendingSkipId: String? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Error de reproduccion", error)
            // PlaybackService es quien reintenta: invalida la URL cacheada y
            // re-resuelve el audio. Aquí no duplicamos el reintento (competir
            // con el servicio provocaba errores en cadena); solo reflejamos el
            // error en la UI, que se limpia en cuanto el player vuelve a READY.
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
                        _positionMs.value = mediaController.currentPosition.coerceAtLeast(0L)
                    }
                    maybeRequestMore(mediaController)
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

        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val selected = songs[safeIndex]
        nearEndNotified = false
        pendingSkipId = null
        _positionMs.value = 0L
        _state.value = _state.value.copy(
            isPlaying = false,
            isBuffering = true,
            hasMedia = true,
            mediaId = selected.id,
            title = selected.title,
            artist = selected.artist,
            artworkUrl = selected.thumbnailUrl,
            positionMs = 0L,
            durationMs = selected.durationSeconds * 1000L,
            error = null,
        )

        val mediaController = controller
        if (mediaController == null) {
            Log.w(TAG, "Controller not ready, queueing ${songs.size} songs")
            pendingQueue = songs to startIndex
            connect()
            return
        }
        play(mediaController, songs, safeIndex)
    }

    private fun play(mediaController: MediaController, songs: List<Song>, startIndex: Int) {
        playJob?.cancel()
        playJob = scope.launch {
            val safeIndex = startIndex.coerceIn(0, songs.lastIndex)

            val items = songs.mapIndexed { index, song ->
                song.toMediaItem(safeIndex, null)
            }
            if (controller !== mediaController) return@launch

            mediaController.setMediaItems(items, safeIndex, 0L)
            mediaController.prepare()
            mediaController.play()
        }
    }

    private suspend fun resolveUrl(videoId: String): String? {
        val app = context.applicationContext as? com.popmusic.app.PoPMusicApplication
        val local = app?.downloadRepository?.localFileFor(videoId)
        if (local != null) {
            return Uri.fromFile(local).toString()
        }
        val cached = resolvedUrls[videoId]
        if (cached != null && SystemClock.elapsedRealtime() - cached.second < URL_TTL_MS) {
            return cached.first
        }
        app?.audioUrlCache?.get(videoId)?.let { persisted ->
            resolvedUrls[videoId] = persisted to SystemClock.elapsedRealtime()
            return persisted
        }
        val url = runCatching { youtube.resolveAudioUrl(videoId) }
            .onFailure { Log.w(TAG, "Could not resolve $videoId", it) }
            .getOrNull()
        if (!url.isNullOrBlank()) {
            resolvedUrls[videoId] = url to SystemClock.elapsedRealtime()
            app?.audioUrlCache?.put(videoId, url)
        }
        return url
    }

    fun addToQueue(songs: List<Song>) {
        val mediaController = controller
        if (mediaController == null) {
            // Arranque en frío: sin esto, las canciones de la radio se perdían.
            pendingQueue = pendingQueue?.let { (queue, index) -> (queue + songs) to index }
            return
        }
        mediaController.addMediaItems(songs.map { it.toMediaItem(0) })
        nearEndNotified = false
        val skipFrom = pendingSkipId ?: return
        pendingSkipId = null
        if (mediaController.currentMediaItem?.mediaId == skipFrom) {
            mediaController.seekToNextMediaItem()
        }
    }

    fun cancelPendingSkip() {
        pendingSkipId = null
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
    }

    fun next() {
        // Salto INMEDIATO. Resolver aquí la URL de destino bloqueaba el gesto
        // 2-4 s con la canción anterior sonando. PlaybackService resuelve la
        // pista entrante en onMediaItemTransition, fuera del hilo del gesto.
        Log.d("SkipPerf", "next @${SystemClock.elapsedRealtime()}")
        val mediaController = controller ?: return
        val nextIndex = mediaController.nextMediaItemIndex
        val noNext = nextIndex < 0 || nextIndex == mediaController.currentMediaItemIndex
        if (noNext && mediaController.mediaItemCount > 0) {
            // Primera canción de la radio: la cola aún no ha llegado. Se
            // recuerda el gesto y se salta en cuanto haya una siguiente.
            onNearQueueEnd?.invoke()
            if (isQueueGrowing?.invoke() == true) {
                pendingSkipId = mediaController.currentMediaItem?.mediaId
                return
            }
        }
        mediaController.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L)
        _positionMs.value = safePosition
        controller?.seekTo(safePosition)
    }

    // ————— Sala "Todos controlan": lectura y mando compartido —————

    /** Cola actual reconstruida desde los MediaItems (para compartirla). */
    fun currentQueue(): List<Song> {
        val mediaController = controller ?: return emptyList()
        return runCatching {
            (0 until mediaController.mediaItemCount).map { i ->
                val item = mediaController.getMediaItemAt(i)
                val meta = item.mediaMetadata
                val uri = item.localConfiguration?.uri?.toString().orEmpty()
                val durationMs = if (i == mediaController.currentMediaItemIndex) {
                    mediaController.duration.takeIf { it > 0 } ?: 0L
                } else {
                    0L
                }
                Song(
                    id = item.mediaId,
                    title = meta.title?.toString().orEmpty(),
                    artist = meta.artist?.toString().orEmpty(),
                    durationSeconds = durationMs / 1000L,
                    thumbnailUrl = meta.artworkUri?.toString(),
                    streamUrl = uri.takeIf {
                        it.isNotBlank() && !it.contains("youtube.com/watch")
                    },
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Índice de la pista actual dentro de la cola. */
    fun currentIndex(): Int = controller?.currentMediaItemIndex ?: -1

    /** Pausa o reanuda de forma explícita (control remoto de la sala). */
    fun setPlaying(playing: Boolean) {
        val mediaController = controller ?: return
        if (playing) {
            if (mediaController.playbackState == Player.STATE_IDLE) mediaController.prepare()
            mediaController.play()
        } else {
            mediaController.pause()
        }
    }

    /** Salta directo a un índice de la cola (sin pasar por anterior/siguiente). */
    fun skipTo(index: Int) {
        val mediaController = controller ?: return
        if (index !in 0 until mediaController.mediaItemCount) return
        // Igual que next/previous: el servicio resuelve la entrante al cambiar
        // de pista, así que el salto de la sala también es instantáneo.
        mediaController.seekTo(index, 0L)
    }

    fun currentPositionMs(): Long =
        controller?.currentPosition?.coerceAtLeast(0L) ?: _positionMs.value

    fun stop() {
        controller?.stop()
    }

    /**
     * Vacía la cola y detiene la reproducción. Lo usa la sala al CREAR una
     * sala nueva: la cola de la sala es la del reproductor, así que para
     * que la sala empiece sin ninguna canción hay que vaciarla también aquí
     * (si no, las canciones anteriores seguían apareciendo en la nueva sala).
     */
    fun clearQueue() {
        playJob?.cancel()
        pendingQueue = null
        pendingSkipId = null
        val mediaController = controller
        if (mediaController != null) {
            mediaController.setMediaItems(emptyList())
        } else {
            _positionMs.value = 0L
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                hasMedia = false,
                mediaId = "",
                title = "",
                artist = "",
                artworkUrl = null,
                positionMs = 0L,
                durationMs = 0L,
                error = null,
            )
        }
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        if (enabled) {
            prefetchAllQueue()
        }
    }

    /** Precarga las URL de audio de toda la cola actual para que el aleatorio sea instantáneo. */
    fun prefetchAllQueue() {
        val mediaController = controller ?: return
        val count = mediaController.mediaItemCount
        val currentIndex = mediaController.currentMediaItemIndex
        // La siguiente pista en orden de reproducción primero; el resto al azar.
        val nextIndex = mediaController.nextMediaItemIndex
        val order = buildList {
            if (nextIndex in 0 until count && nextIndex != currentIndex) add(nextIndex)
            addAll((0 until count).filter { it != currentIndex && it != nextIndex }.shuffled())
        }
        for (index in order) {
            val uri = mediaController.getMediaItemAt(index).localConfiguration?.uri?.toString() ?: ""
            if (!uri.contains("youtube.com/watch")) continue
            scope.launch {
                prefetchPermits.withPermit {
                    val item = runCatching { mediaController.getMediaItemAt(index) }.getOrNull()
                        ?: return@withPermit
                    val resolved = resolveUrl(item.mediaId) ?: return@withPermit
                    if (resolved == uri) return@withPermit
                    runCatching {
                        val current = mediaController.getMediaItemAt(index)
                        if (current.mediaId == item.mediaId) {
                            mediaController.replaceMediaItem(
                                index,
                                current.buildUpon().setUri(resolved).build(),
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun Song.toMediaItem(startIndex: Int, resolvedUrl: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(resolvedUrl ?: streamUrl ?: "https://www.youtube.com/watch?v=$id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(thumbnailUrl?.let(Uri::parse))
                    .setIsPlayable(true)
                    .setExtras(Bundle().apply { putInt(EXTRA_START_INDEX, startIndex) })
                    .build(),
            )
            .build()

    private fun maybeRequestMore(player: Player) {
        if (player.mediaItemCount == 0) {
            nearEndNotified = false
            return
        }
        val threshold = (player.mediaItemCount - 5).coerceAtLeast(0)
        val nearEnd = player.currentMediaItemIndex >= threshold
        if (nearEnd && !nearEndNotified) {
            nearEndNotified = true
            onNearQueueEnd?.invoke()
        } else if (!nearEnd) {
            nearEndNotified = false
        }
    }

    private fun updateState(player: Player) {
        val metadata = player.currentMediaItem?.mediaMetadata
        val position = player.currentPosition.coerceAtLeast(0L)
        _positionMs.value = position
        val mediaId = player.currentMediaItem?.mediaId.orEmpty()
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            hasMedia = player.currentMediaItem != null,
            mediaId = mediaId,
            title = metadata?.title?.toString().orEmpty(),
            artist = metadata?.artist?.toString().orEmpty(),
            artworkUrl = metadata?.artworkUri?.toString(),
            positionMs = position,
            durationMs = player.duration.coerceAtLeast(0L),
            repeatMode = player.repeatMode,
            shuffle = player.shuffleModeEnabled,
            // Un reintento correcto (o simplemente seguir reproduciendo) borra
            // el mensaje de error anterior.
            error = if (player.playbackState == Player.STATE_READY) null else _state.value.error,
        )
        recordHistory(player, mediaId, metadata)
        maybeRequestMore(player)
    }

    /**
     * Guarda la canción en el historial (pantalla "Recientes") solo cuando
     * empieza a sonar de verdad y cambia de pista, para no duplicar entradas
     * en cada evento de reproducción.
     */
    private fun recordHistory(player: Player, mediaId: String, metadata: MediaMetadata?) {
        if (!player.isPlaying || mediaId.isBlank() || mediaId == lastRecordedId) return
        val title = metadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: return
        lastRecordedId = mediaId
        RecentStore.record(
            context,
            Song(
                id = mediaId,
                title = title,
                artist = metadata.artist?.toString().orEmpty(),
                durationSeconds = player.duration.coerceAtLeast(0L) / 1000L,
                thumbnailUrl = metadata.artworkUri?.toString(),
            ),
        )
    }

    fun release() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    /**
     * Cierre total a los 8 min ([com.popmusic.app.AppCloseReceiver]): suelta
     * el controlador (así el servicio queda sin clientes y puede destruirse)
     * y limpia el estado en memoria para que, al volver, no quede canción en
     * el mini reproductor. Sin killProcess: matar el proceso provocaba que
     * media3 (START_STICKY) lo reviviera con la notificación huérfana.
     */
    fun resetAfterClose() {
        release()
        playJob?.cancel()
        pendingQueue = null
        nearEndNotified = false
        pendingSkipId = null
        lastRecordedId = null
        _positionMs.value = 0L
        _state.value = PlayerUiState()
    }

    companion object {
        const val EXTRA_START_INDEX = "com.popmusic.app.START_INDEX"
        private const val TAG = "PlayerConnection"
        private const val URL_TTL_MS = 20 * 60 * 1000L
    }
}
