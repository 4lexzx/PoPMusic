package com.popmusic.app.ui.screens.recognize

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.data.recognize.RecognitionRepository
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import com.popmusic.app.media.AudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface RecognitionUiState {
    data object Idle : RecognitionUiState
    data object Recording : RecognitionUiState
    data object Recognizing : RecognitionUiState
    data class Found(val song: Song) : RecognitionUiState
    data class Error(val message: String) : RecognitionUiState
}

class RecognizeViewModel(application: Application) : AndroidViewModel(application) {

    private val recognitionRepository = RecognitionRepository()
    private val youtubeRepository = YouTubeRepository()
    private val recorder = AudioRecorder(application)

    var state by mutableStateOf<RecognitionUiState>(RecognitionUiState.Idle)
        private set

    fun start(context: Context) {
        if (state is RecognitionUiState.Recording || state is RecognitionUiState.Recognizing) return

        viewModelScope.launch {
            state = RecognitionUiState.Recording

            val file = runCatching { recorder.start() }.getOrElse { error ->
                state = RecognitionUiState.Error(error.message ?: "No se pudo acceder al micrófono")
                return@launch
            }

            delay(RECORDING_MILLIS)
            val recorded = recorder.stop()

            if (recorded == null) {
                state = RecognitionUiState.Error("La grabación falló. Inténtalo de nuevo.")
                return@launch
            }

            state = RecognitionUiState.Recognizing
            val recognized = runCatching { recognitionRepository.recognize(recorded) }.getOrNull()
            recorded.delete()

            if (recognized == null) {
                state = RecognitionUiState.Error(
                    if (com.popmusic.app.BuildConfig.AUDD_API_KEY.isBlank()) {
                        "Falta configurar la clave de AudD en local.properties"
                    } else {
                        "No se reconoció la canción. Acércate al altavoz e inténtalo otra vez."
                    },
                )
                return@launch
            }

            val results = runCatching {
                youtubeRepository.search("${recognized.title} ${recognized.artist}")
            }.getOrDefault(emptyList())

            val song = results.firstOrNull()
            state = if (song != null) {
                RecognitionUiState.Found(song)
            } else {
                RecognitionUiState.Error("Reconocimos \"${recognized.title}\" pero no encontramos el audio.")
            }
        }
    }

    fun reset() {
        if (state is RecognitionUiState.Idle) return
        state = RecognitionUiState.Idle
    }

    override fun onCleared() {
        recorder.cancel()
    }

    private companion object {
        const val RECORDING_MILLIS = 8_000L
    }
}
