package com.popmusic.app.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.recommendations.DiscoveryPool
import com.popmusic.app.data.recommendations.RecommendationsRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    private val connection = app.playerConnection
    private val recommendations = RecommendationsRepository()
    private var autoExtend = false
    private var queueIds: Set<String> = emptySet()
    private var radioJob: Job? = null
    private var prefetchedKey: String? = null
    /** Semilla original de la radio: el género no cambia al avanzar la cola. */
    private var radioSeed: Song? = null
    /** Sección o chip de origen, que identifica el género mejor que el vídeo. */
    private var radioGenreHint: String? = null
    /** Artista de "Grandes éxitos": la radio solo debe sonar ese artista. */
    private var radioArtistOnly: String? = null

    val state: StateFlow<PlayerUiState> = connection.state
    val positionMs: StateFlow<Long> = connection.positionMs

    init {
        connection.onNearQueueEnd = {
            if (autoExtend) {
                // Igual que el playlist RD de YouTube: la radio se construye
                // desde la semilla elegida, no desde la última canción que
                // sonó. Así el género queda fijo y solo cambian las canciones.
                radioSeed?.let(::extendRadio)
            }
        }

        connection.isQueueGrowing = { autoExtend && radioJob?.isActive == true }

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
        queueIds = songs.mapTo(HashSet()) { it.id }
        radioJob?.cancel()
        // Sin esto, Music Flow alargaba su cola con la semilla (y el artista)
        // de la radio ANTERIOR y metía canciones de otro estilo.
        radioSeed = if (autoExtend) songs.getOrNull(startIndex) else null
        radioGenreHint = null
        radioArtistOnly = null
        recommendations.resetRadio()
        connection.play(songs, startIndex)
    }

    /**
     * Reproduce una sola canción como semilla de radio, igual que hace Echo
     * al tocar un resultado de búsqueda o un tema de Tendencias: la cola se
     * construye con canciones parecidas a la elegida, no con el resto de la
     * lista ordenada que viniera de detrás.
     */
    fun setPlaying(playing: Boolean) = connection.setPlaying(playing)

    fun playRadio(
        seed: Song,
        genreHint: String? = null,
        artistOnly: String? = null,
    ) {
        autoExtend = true
        radioSeed = seed
        radioGenreHint = genreHint
        radioArtistOnly = artistOnly
        queueIds = setOf(seed.id)
        radioJob?.cancel()
        recommendations.resetRadio()
        connection.play(listOf(seed), 0)
        extendRadio(seed)
    }

    /**
     * "Grandes éxitos" de un artista: se reproducen sus canciones y la cola
     * sigue alimentándose solo con ese artista, nunca con otros grupos.
     */
    fun playArtist(artist: String, songs: List<Song>, startIndex: Int) {
        val seed = songs.getOrNull(startIndex)
        autoExtend = seed != null
        radioSeed = seed
        radioGenreHint = null
        radioArtistOnly = artist
        queueIds = songs.mapTo(HashSet()) { it.id }
        radioJob?.cancel()
        recommendations.resetRadio()
        connection.play(songs, startIndex)
        if (seed != null) extendRadio(seed)
    }

    private fun extendRadio(seed: Song) {
        if (radioJob?.isActive == true) return
        radioJob = viewModelScope.launch {
            // La radio de una canción es UNA petición ligera: se lanza ya, para
            // que "siguiente" funcione desde la primera pista. La de artista
            // hace varias búsquedas y trae su propia cola, así que espera a que
            // suene para no competir por la red.
            if (radioArtistOnly != null && queueIds.size > 1) {
                withTimeoutOrNull(2_000L) {
                    state.first { it.isPlaying && it.mediaId == seed.id }
                }
            }
            var batch = fetchRadio(seed)
            for (waitMs in RADIO_RETRY_MS) {
                if (batch.isNotEmpty()) break
                delay(waitMs)
                batch = fetchRadio(seed)
            }
            if (batch.isEmpty()) connection.cancelPendingSkip()
            if (batch.isNotEmpty()) {
                queueIds = queueIds + batch.map { it.id }
                connection.addToQueue(batch)
                Log.d(
                    TAG,
                    "Radio de ${seed.title}: ${batch.size} canciones parecidas → " +
                        batch.joinToString(" | ") { "${it.title} (${it.artist})" },
                )
            }
        }
    }

    private suspend fun fetchRadio(seed: Song): List<Song> = try {
        recommendations.radio(
            library = app.libraryRepository.state.value,
            excludeIds = queueIds,
            seedSong = seed,
            discovered = DiscoveryPool.snapshot(),
            genreHint = radioGenreHint,
            artistOnly = radioArtistOnly,
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo crear la radio de ${seed.title}", e)
        emptyList()
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
        connection.isQueueGrowing = null
        super.onCleared()
    }

    private companion object {
        const val TAG = "PlayerViewModel"

        /** Reintentos si la radio llega vacía (corte de red puntual). */
        val RADIO_RETRY_MS = listOf(1_500L, 4_000L)
    }
}
