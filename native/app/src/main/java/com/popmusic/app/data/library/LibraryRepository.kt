package com.popmusic.app.data.library

import android.content.Context
import android.util.Log
import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class LibraryState(
    val favorites: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

class LibraryRepository(context: Context) {

    private val file = File(context.filesDir, "library.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    fun isFavorite(songId: String): Boolean = _state.value.favorites.any { it.id == songId }

    fun toggleFavorite(song: Song) = mutate { current ->
        val favorites = if (current.favorites.any { it.id == song.id }) {
            current.favorites.filterNot { it.id == song.id }
        } else {
            current.favorites + song
        }
        current.copy(favorites = favorites)
    }

    fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(id = UUID.randomUUID().toString(), name = name.trim(), songs = emptyList())
        mutate { it.copy(playlists = it.playlists + playlist) }
        return playlist
    }

    fun deletePlaylist(playlistId: String) = mutate { current ->
        current.copy(playlists = current.playlists.filterNot { it.id == playlistId })
    }

    fun renamePlaylist(playlistId: String, name: String) = mutate { current ->
        current.copy(
            playlists = current.playlists.map {
                if (it.id == playlistId) it.copy(name = name.trim()) else it
            },
        )
    }

    fun addToPlaylist(playlistId: String, song: Song) {
        if (playlistId == FAVORITES_ID) {
            if (!isFavorite(song.id)) toggleFavorite(song)
            return
        }
        mutate { current ->
            current.copy(
                playlists = current.playlists.map { playlist ->
                    when {
                        playlist.id != playlistId -> playlist
                        playlist.songs.any { it.id == song.id } -> playlist
                        else -> playlist.copy(songs = playlist.songs + song)
                    }
                },
            )
        }
    }

    fun removeFromPlaylist(playlistId: String, songId: String) {
        if (playlistId == FAVORITES_ID) {
            mutate { current ->
                current.copy(favorites = current.favorites.filterNot { it.id == songId })
            }
            return
        }
        mutate { current ->
            current.copy(
                playlists = current.playlists.map { playlist ->
                    if (playlist.id == playlistId) {
                        playlist.copy(songs = playlist.songs.filterNot { it.id == songId })
                    } else {
                        playlist
                    }
                },
            )
        }
    }

    fun playlist(playlistId: String): Playlist? {
        if (playlistId == FAVORITES_ID) {
            return Playlist(FAVORITES_ID, "Canciones favoritas", _state.value.favorites)
        }
        return _state.value.playlists.firstOrNull { it.id == playlistId }
    }

    private fun mutate(block: (LibraryState) -> LibraryState) {
        _state.value = block(_state.value)
        persist()
    }

    private fun persist() {
        val json = encode(_state.value) ?: return
        scope.launch {
            runCatching { file.writeText(json) }
                .onFailure { Log.e(TAG, "No se pudo guardar la biblioteca", it) }
        }
    }

    private fun load(): LibraryState {
        if (!file.exists()) return LibraryState()
        return runCatching { decode(file.readText()) }
            .onFailure { Log.e(TAG, "No se pudo leer la biblioteca", it) }
            .getOrDefault(LibraryState())
    }

    private fun encode(state: LibraryState): String? = runCatching {
        JSONObject()
            .put("favorites", JSONArray().apply { state.favorites.forEach { put(it.toJson()) } })
            .put(
                "playlists",
                JSONArray().apply {
                    state.playlists.forEach { playlist ->
                        put(
                            JSONObject()
                                .put("id", playlist.id)
                                .put("name", playlist.name)
                                .put("songs", JSONArray().apply { playlist.songs.forEach { put(it.toJson()) } }),
                        )
                    }
                },
            )
            .toString()
    }.getOrNull()

    private fun decode(raw: String): LibraryState {
        if (raw.isBlank()) return LibraryState()
        val root = JSONObject(raw)

        val favorites = root.optJSONArray("favorites")?.toSongs().orEmpty()

        val playlists = buildList {
            val array = root.optJSONArray("playlists") ?: return@buildList
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    Playlist(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        songs = item.optJSONArray("songs").toSongs(),
                    ),
                )
            }
        }

        return LibraryState(favorites = favorites, playlists = playlists)
    }

    private fun JSONArray?.toSongs(): List<Song> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                add(
                    Song(
                        id = id,
                        title = item.optString("title"),
                        artist = item.optString("artist"),
                        durationSeconds = item.optLong("durationSeconds"),
                        thumbnailUrl = item.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    private fun Song.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("artist", artist)
        .put("durationSeconds", durationSeconds)
        .put("thumbnailUrl", thumbnailUrl ?: "")

    companion object {
        const val FAVORITES_ID = "favoritos"
        private const val TAG = "LibraryRepository"
    }
}
