package com.popmusic.app.ui.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val repository = YouTubeRepository()

    var query by mutableStateOf("")
        private set

    var results by mutableStateOf<List<Song>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun onQueryChange(value: String) {
        query = value
    }

    fun search() {
        val term = query.trim()
        if (term.isEmpty() || isLoading) return

        viewModelScope.launch {
            isLoading = true
            error = null
            results = runCatching { repository.search(term) }
                .onFailure { error = it.message ?: "Error de búsqueda" }
                .getOrDefault(emptyList())
            isLoading = false
        }
    }

    fun clearError() {
        error = null
    }
}
