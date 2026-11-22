package com.popmusic.app.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.charts.ChartsRepository
import com.popmusic.app.data.library.LibraryState
import com.popmusic.app.data.music.MusicService
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImportState {
    data object Idle : ImportState
    data object Loading : ImportState
    data class Error(val message: String) : ImportState
    data class Success(val name: String, val count: Int) : ImportState
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PoPMusicApplication).libraryRepository
    private val sync = (application as PoPMusicApplication).musicSync
    private val youtube = YouTubeRepository()
    private val charts = ChartsRepository(application)

    val state: StateFlow<LibraryState> = repository.state

    /** Mensajes de sincronización (exportar/importar a cuentas) para Toast. */
    val syncEvents: StateFlow<String?> = sync.events

    /**
     * Estado de cada cuenta de música vinculada (ítem 3): la UI lo usa para
     * decidir si muestra los chips de sincronización al crear una playlist.
     */
    val syncState = sync.state

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private var importJob: Job? = null

    fun toggleFavorite(song: Song) = repository.toggleFavorite(song)

    fun addToPlaylist(playlistId: String, song: Song) = repository.addToPlaylist(playlistId, song)

    /** Exporta una playlist a un servicio vinculado (YouTube Music / Spotify). */
    fun exportPlaylist(playlistId: String, service: MusicService) =
        sync.exportPlaylist(service, playlistId)

    /**
     * "Sincronizar…" desde el menú ⋯ de una playlist todavía no vinculada
     * (Tanda 12). Guarda la elección y sube la playlist ya mismo: [target]
     * "YOUTUBE"/"SPOTIFY" = esa plataforma; null = ambas conectadas;
     * "NONE" = solo local.
     */
    fun syncPlaylist(playlistId: String, target: String?) {
        repository.setSyncTarget(playlistId, target)
        when (target) {
            null -> MusicService.entries.forEach { service ->
                if (sync.state.value[service]?.connected == true) {
                    sync.exportPlaylist(service, playlistId)
                }
            }
            "NONE" -> Unit
            else -> runCatching { MusicService.valueOf(target) }.getOrNull()
                ?.let { sync.exportPlaylist(it, playlistId) }
        }
    }

    /** Importa todas las playlists de un servicio vinculado. */
    fun importFromService(service: MusicService) = sync.importFrom(service)

    fun clearSyncEvent() = sync.clearEvent()

    /**
     * Crea una playlist. `syncTarget` (ítem 3) es la elección del usuario:
     * "YOUTUBE"/"SPOTIFY" = subirla solo a esa cuenta; "NONE" = solo local;
     * null = automático en todas las conectadas.
     */
    fun createPlaylist(name: String, syncTarget: String? = null): com.popmusic.app.domain.Playlist? {
        if (name.isBlank()) return null
        return repository.createPlaylist(name, syncTarget)
    }

    fun deletePlaylist(playlistId: String) = repository.deletePlaylist(playlistId)

    fun importPlaylist(link: String) {
        if (link.isBlank() || importJob?.isActive == true) return

        importJob = viewModelScope.launch {
            _importState.value = ImportState.Loading
            // Tanda 6 (punto 3): el import por URL acepta Spotify y YouTube.
            // Ítem 3: al importar por enlace vinculamos la playlist con su id
            // remoto para que los cambios se repliquen en la plataforma
            // original (write-back). Sin vínculo, el watcher crearía una
            // playlist remota duplicada.
            val spotifyId = charts.spotifyPlaylistId(link)
            val playlist: com.popmusic.app.domain.Playlist?
            val external: Map<String, String>
            if (spotifyId != null) {
                playlist = charts.importSpotifyPlaylist(link)
                external = if (playlist == null) {
                    emptyMap()
                } else {
                    mapOf(MusicService.SPOTIFY.name to spotifyId)
                }
            } else {
                playlist = youtube.importPlaylist(link)
                val ytId = youtube.playlistIdFrom(link)
                external = if (playlist == null || ytId == null) {
                    emptyMap()
                } else {
                    mapOf(MusicService.YOUTUBE.name to ytId)
                }
            }
            _importState.value = if (playlist == null) {
                ImportState.Error("No se pudo importar. Pega un enlace de Spotify o YouTube.")
            } else {
                repository.addPlaylist(playlist.name, playlist.songs, external)
                ImportState.Success(playlist.name, playlist.songs.size)
            }
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
}
