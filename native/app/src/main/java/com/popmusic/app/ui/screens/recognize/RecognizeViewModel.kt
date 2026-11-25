package com.popmusic.app.ui.screens.recognize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.data.recognize.AudioFingerprint
import com.popmusic.app.data.recognize.RecognitionRepository
import com.popmusic.app.data.recognize.RecognitionRepository.Result
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.RecognizedSong
import com.popmusic.app.domain.Song
import com.popmusic.app.media.MicCapture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt

sealed interface RecognitionUiState {
    data object Idle : RecognitionUiState
    data object Listening : RecognitionUiState

    /** Ya se sabe qué canción es; se está abriendo. */
    data class Matched(val song: RecognizedSong) : RecognitionUiState
    data class Found(val song: Song) : RecognitionUiState
    data class Error(val title: String, val message: String) : RecognitionUiState
}

class RecognizeViewModel : ViewModel() {

    private val recognition = RecognitionRepository()
    private val youtube = YouTubeRepository()

    var state by mutableStateOf<RecognitionUiState>(RecognitionUiState.Idle)
        private set

    /** Volumen que capta el micrófono (0..1), para la animación. */
    var level by mutableFloatStateOf(0f)
        private set

    private var job: Job? = null

    /** Deja la conexión lista para que la primera consulta no espere. */
    fun prepare() {
        viewModelScope.launch { recognition.warmUp() }
    }

    fun start() {
        if (job?.isActive == true) return
        state = RecognitionUiState.Listening
        level = 0f
        job = viewModelScope.launch {
            try {
                state = when (val outcome = listen()) {
                    is Result.Match -> {
                        state = RecognitionUiState.Matched(outcome.song)
                        open(outcome.song)
                    }
                    is Result.Unavailable -> RecognitionUiState.Error(
                        title = if (outcome.offline) "Sin conexión" else "Servicio no disponible",
                        message = if (outcome.offline) {
                            "Revisa tu internet e inténtalo otra vez."
                        } else {
                            "Inténtalo de nuevo en unos segundos."
                        },
                    )
                    Result.NoMatch -> RecognitionUiState.Error(
                        title = "No la reconocimos",
                        message = "Acerca el teléfono al altavoz e inténtalo otra vez.",
                    )
                    null -> RecognitionUiState.Error(
                        title = "No se oye nada",
                        message = "El micrófono no capta sonido. Sube el volumen o acércate.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = RecognitionUiState.Error(
                    title = "No se pudo escuchar",
                    message = e.message ?: "No se pudo acceder al micrófono.",
                )
            }
        }
    }

    /**
     * Escucha y consulta a la vez: en cuanto hay audio suficiente pregunta,
     * y sigue escuchando mientras llega la respuesta. Gana el primer acierto.
     * Devuelve null si el micrófono no captó nada.
     */
    private suspend fun listen(): Result? = coroutineScope {
        val scope = this
        val match = CompletableDeferred<Result.Match>()
        val offline = AtomicInteger()
        val down = AtomicInteger()
        val answered = AtomicInteger()
        val print = AudioFingerprint()
        val queries = java.util.concurrent.CopyOnWriteArrayList<Job>()
        var next = 0

        val mic = launch {
            MicCapture(AudioFingerprint.RATE).capture { pcm, count ->
                print.feed(pcm, count)
                var sum = 0.0
                for (i in 0 until count) sum += pcm[i] * pcm[i].toDouble()
                level = sqrt((sqrt(sum / count) / 3_000.0).coerceIn(0.0, 1.0)).toFloat()

                if (print.heardMs >= CHECKPOINTS_MS[next]) {
                    next++
                    if (print.peakCount >= MIN_PEAKS) {
                        val uri = print.encode()
                        val ms = print.totalMs
                        queries += scope.launch {
                            when (val result = recognition.recognize(uri, ms)) {
                                is Result.Match -> match.complete(result)
                                is Result.Unavailable ->
                                    (if (result.offline) offline else down).incrementAndGet()
                                Result.NoMatch -> answered.incrementAndGet()
                            }
                        }
                    }
                }
                next < CHECKPOINTS_MS.size && !match.isCompleted &&
                    // Si la red falla varias veces seguidas no sirve seguir oyendo.
                    (answered.get() > 0 || offline.get() + down.get() < 3)
            }
        }
        match.invokeOnCompletion {
            mic.cancel()
            queries.forEach { it.cancel() }
        }
        mic.join()
        queries.toList().joinAll()
        level = 0f
        when {
            match.isCompleted -> match.await()
            answered.get() > 0 -> Result.NoMatch
            offline.get() > 0 -> Result.Unavailable(offline = true)
            down.get() > 0 -> Result.Unavailable(offline = false)
            else -> null
        }
    }

    /** Busca el audio de la canción reconocida. */
    private suspend fun open(found: RecognizedSong): RecognitionUiState {
        val query = "${found.title} ${found.artist}".trim()
        val best = attempt { youtube.search(query) }.maxByOrNull { score(it, found) }
        val song = best?.takeIf { score(it, found) >= 4 }
            ?: attempt { youtube.searchVideos(query) }
                .maxByOrNull { score(it, found) }
                ?.takeIf { score(it, found) >= 4 }
            ?: best?.takeIf { score(it, found) >= 2 }
        return if (song != null) {
            RecognitionUiState.Found(song)
        } else {
            RecognitionUiState.Error(
                title = found.title,
                message = "Es de ${found.artist}, pero no encontramos el audio.",
            )
        }
    }

    private suspend fun attempt(block: suspend () -> List<Song>): List<Song> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }

    /** +2 título, +2 artista, +1 título idéntico (ni remix ni en vivo). */
    private fun score(song: Song, found: RecognizedSong): Int {
        val title = key(song.title)
        val bare = key(found.title.substringBefore('(').substringBefore('['))
        var points = 0
        if (title == key(found.title)) points++
        if (bare.isNotEmpty() && title.contains(bare)) points += 2
        val artist = key(found.artist.split(',', '&').first().substringBefore(" feat"))
        if (artist.isNotEmpty() && "${key(song.artist)} $title".contains(artist)) points += 2
        return points
    }

    private fun key(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(' ').filter { it.isNotEmpty() }.joinToString(" ")

    /** Deja de escuchar y vuelve al inicio. */
    fun cancel() {
        job?.cancel()
        job = null
        level = 0f
        state = RecognitionUiState.Idle
    }

    private companion object {
        /** Momentos en los que se consulta con lo oído hasta entonces. */
        val CHECKPOINTS_MS = intArrayOf(1_700, 2_300, 3_100, 4_500, 7_000, 11_000)

        /** Con menos picos no hay música: silencio o micrófono tapado. */
        const val MIN_PEAKS = 20
    }
}
