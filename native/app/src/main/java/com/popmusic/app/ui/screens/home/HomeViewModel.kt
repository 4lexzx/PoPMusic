package com.popmusic.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeSection(
    val title: String,
    val songs: List<Song>,
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val sections: List<HomeSection> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = YouTubeRepository()
    private val library = (application as PoPMusicApplication).libraryRepository

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val favorites: StateFlow<com.popmusic.app.data.library.LibraryState> = library.state

    init {
        load()
    }

    fun load() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val results = runCatching {
                QUERIES.map { (title, query) ->
                    async { title to repository.search(query).take(CARDS_PER_SECTION) }
                }.awaitAll()
            }

            results
                .onSuccess { sections ->
                    _state.value = HomeUiState(
                        isLoading = false,
                        sections = sections
                            .filter { it.second.isNotEmpty() }
                            .map { HomeSection(it.first, it.second) },
                    )
                }
                .onFailure {
                    _state.value = HomeUiState(
                        isLoading = false,
                        error = it.message ?: "No se pudo cargar la musica",
                    )
                }
        }
    }

    private companion object {
        const val CARDS_PER_SECTION = 8

        val QUERIES = listOf(
            "Éxitos del momento" to "top hits",
            "Pop en español" to "pop en español",
            "Para relajarse" to "chill music",
            "Fiesta" to "reggaeton",
        )
    }
}
