package com.popmusic.app.ui.screens.lyrics

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.info.ArtistInfo
import com.popmusic.app.domain.Lyrics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LyricsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PoPMusicApplication
    private val repository = app.lyricsRepository
    private val translationRepository = app.translationRepository
    private val artistInfoRepository = app.artistInfoRepository
    private val albumRepository = com.popmusic.app.data.info.AlbumRepository()
    private val viewPrefs = application.getSharedPreferences("lyrics_view", Context.MODE_PRIVATE)

    private var lastKey: String? = null
    private var songId = ""
    private var title = ""
    private var artist = ""
    private var durationMs = 0L
    private var translationJob: Job? = null
    private var artistJob: Job? = null

    var lyrics by mutableStateOf<Lyrics?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var offsetMs by mutableIntStateOf(0)
        private set

    var translationEnabled by mutableStateOf(viewPrefs.getBoolean(KEY_TRANSLATE, false))
        private set

    var canTranslate by mutableStateOf(false)
        private set

    var translations by mutableStateOf(emptyMap<Int, String>())
        private set

    var artistInfo by mutableStateOf<ArtistInfo?>(null)
        private set

    var artistInfoLoading by mutableStateOf(false)
        private set

    var albumDisplay by mutableStateOf<String?>(null)
        private set

    fun load(songId: String, title: String, artist: String, durationMs: Long) {
        val key = "$songId|$title|$artist|${durationMs / 1000}"
        if (key == lastKey || title.isBlank()) return
        lastKey = key
        this.songId = songId
        this.title = title
        this.artist = artist
        this.durationMs = durationMs
        offsetMs = repository.offsetMs(songId, title, artist, durationMs)
        lyrics = null
        translations = emptyMap()
        canTranslate = false
        artistInfo = null
        artistInfoLoading = false
        albumDisplay = null

        viewModelScope.launch {
            isLoading = true
            error = null

            lyrics = runCatching {
                repository.load(songId, title, artist, durationMs)
            }.onFailure {
                error = it.message ?: "No se pudieron cargar las letras"
            }.getOrNull()

            albumDisplay = validAlbum(lyrics?.albumName, title, artist)
                ?: runCatching { albumRepository.albumFor(title, artist) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }

            if (lyrics == null && error == null) {
                error = "No encontramos la letra de esta canción"
            }
            isLoading = false
            lyrics?.let { loaded ->
                canTranslate = !looksSpanish(loaded)
                if (translationEnabled && canTranslate) {
                    runTranslation()
                }
            }
        }

        fetchArtistInfo(artist)
    }

    fun toggleTranslation() {
        val next = !translationEnabled
        translationEnabled = next
        viewPrefs.edit().putBoolean(KEY_TRANSLATE, next).apply()
        if (next) {
            runTranslation()
        } else {
            translationJob?.cancel()
            translations = emptyMap()
        }
    }

    private fun runTranslation() {
        translationJob?.cancel()
        val loaded = lyrics ?: return
        if (loaded.lines.isEmpty() || !canTranslate) return
        translationJob = viewModelScope.launch {
            val targetKey = lastKey ?: return@launch
            loaded.lines.forEachIndexed { index, line ->
                if (currentKey() != targetKey) return@launch
                if (line.isInstrumental || line.text.isBlank()) return@forEachIndexed
                if (translations[index] != null) return@forEachIndexed
                val translated = runCatching { translationRepository.translate(line.text) }
                    .getOrNull()
                if (translated != null && currentKey() == targetKey) {
                    translations = translations + (index to translated)
                }
            }
        }
    }

    private fun currentKey(): String? = lastKey

    private fun validAlbum(album: String?, title: String, artist: String): String? {
        val value = album?.trim().orEmpty()
        if (value.isEmpty()) return null
        val lowered = value.lowercase()
        if (lowered in BLACKLIST_ALBUMS) return null
        if (lowered.endsWith(" - topic")) return null
        if (artist.isNotBlank() && lowered == "${artist.trim().lowercase()} videos") return null
        if (lowered == artist.trim().lowercase()) return null
        if (lowered == title.trim().lowercase()) return null
        val artistParts = artist.split(',', '&', '/').map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (artistParts.size > 1 && lowered in artistParts) return null
        return value
    }

    private fun fetchArtistInfo(name: String) {
        artistJob?.cancel()
        if (name.isBlank()) return
        artistJob = viewModelScope.launch {
            artistInfoLoading = true
            artistInfo = runCatching { artistInfoRepository.artistInfo(name) }.getOrNull()
            artistInfoLoading = false
        }
    }

    private companion object {
        const val KEY_TRANSLATE = "lyrics_translate"

        val BLACKLIST_ALBUMS = setOf(
            "youtube", "single", "none", "unknown", "desconocido", "n/a", "-",
        )

        val SPANISH_MARKERS = setOf(
            "el", "la", "los", "las", "de", "que", "y", "a", "en", "un", "una",
            "con", "por", "para", "tu", "te", "es", "mas", "más", "mi", "no",
            "se", "si", "sí", "yo", "me", "cuando", "porque", "pero", "como",
            "muy", "tú", "mi", "amor", "nada", "todo", "esto", "esta", "eres",
            "vida", "cielo", "dime", "ahora", "hasta", "desde", "sobre", "entre",
            "también", "sin", "noche", "quiero", "voy", "verdad",
        )

        fun looksSpanish(lyrics: com.popmusic.app.domain.Lyrics): Boolean {
            val sample = buildString {
                append(lyrics.lines.take(14).joinToString(" ") { it.text })
                append(" ")
                append(lyrics.plainText.orEmpty())
            }
            val tokens = sample.lowercase()
                .split(Regex("[^a-záéíóúüñ]+"))
                .filter { it.length >= 2 }
            if (tokens.size < 12) return false
            val hits = tokens.count { it in SPANISH_MARKERS }
            val accents = tokens.count { it.any { c -> "áéíóúñü".contains(c) } }
            return hits.toFloat() / tokens.size > 0.10f ||
                accents.toFloat() / tokens.size > 0.08f
        }
    }
}
