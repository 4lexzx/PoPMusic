package com.popmusic.app.ui.screens.lyrics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.data.lyrics.LrcLibRepository
import com.popmusic.app.domain.Lyrics
import kotlinx.coroutines.launch

class LyricsViewModel : ViewModel() {

    private val repository = LrcLibRepository()
    private var lastKey: String? = null

    var lyrics by mutableStateOf<Lyrics?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun load(title: String, artist: String) {
        val key = "$title|$artist"
        if (key == lastKey || title.isBlank()) return
        lastKey = key

        viewModelScope.launch {
            isLoading = true
            error = null

            lyrics = runCatching { repository.search(title, artist) }
                .onFailure { error = it.message ?: "No se pudieron cargar las letras" }
                .getOrNull()

            if (lyrics == null && error == null) {
                error = "No encontramos la letra de esta canción"
            }
            isLoading = false
        }
    }
}
