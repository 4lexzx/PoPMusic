package com.popmusic.app.data.music

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente de YouTube Data API v3 para importar y escribir playlists de la
 * cuenta vinculada. Requiere un OAuth Client ID configurado en local.properties
 * (`youtube.clientId`) con el scope `https://www.googleapis.com/auth/youtube`.
 */
class YouTubeMusicService(
    private val accounts: MusicAccountRepository,
    private val oauth: MusicOAuthManager,
    private val youtube: com.popmusic.app.data.youtube.YouTubeRepository =
        com.popmusic.app.data.youtube.YouTubeRepository(),
) : MusicServiceRepository {

    override val service: MusicService = MusicService.YOUTUBE

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val importMutex = Mutex()

    override fun isConnected(): Boolean = accounts.isConnected(MusicService.YOUTUBE)

    override suspend fun importPlaylists(): List<ExternalPlaylist> = withContext(Dispatchers.IO) {
        val token = oauth.freshToken(MusicService.YOUTUBE) ?: return@withContext emptyList()
        val result = mutableListOf<ExternalPlaylist>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("https://www.googleapis.com/youtube/v3/playlists")
                append("?part=snippet,contentDetails&mine=true&maxResults=50")
                if (pageToken != null) append("&pageToken=").append(Uri.encode(pageToken))
            }
            val json = get(url, token) ?: break
            val items = json.optJSONArray("items") ?: JSONArray()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val snippet = item.optJSONObject("snippet") ?: JSONObject()
                result += ExternalPlaylist(
                    service = MusicService.YOUTUBE,
                    externalId = id,
                    name = snippet.optString("title").ifBlank { "Playlist de YouTube" },
                    trackCount = item.optJSONObject("contentDetails")?.optInt("itemCount") ?: 0,
                    thumbnailUrl = bestThumb(snippet),
                )
            }
            pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (pageToken != null)
        result
    }

    override suspend fun importPlaylistSongs(externalPlaylistId: String): List<ExternalSong> =
        withContext(Dispatchers.IO) {
            val token = oauth.freshToken(MusicService.YOUTUBE) ?: return@withContext emptyList()
            val raw = mutableListOf<ExternalSong>()
            var pageToken: String? = null
            do {
                val url = buildString {
                    append("https://www.googleapis.com/youtube/v3/playlistItems")
                    append("?part=snippet,contentDetails&maxResults=50")
                    append("&playlistId=").append(Uri.encode(externalPlaylistId))
                    if (pageToken != null) append("&pageToken=").append(Uri.encode(pageToken))
                }
                val json = get(url, token) ?: break
                val items = json.optJSONArray("items") ?: JSONArray()
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val snippet = item.optJSONObject("snippet") ?: continue
                    val videoId = snippet.optJSONObject("resourceId")?.optString("videoId")
                        ?.takeIf { it.isNotBlank() }
                        ?: item.optJSONObject("contentDetails")?.optString("videoId")
                            ?.takeIf { it.isNotBlank() }
                        ?: continue
                    raw += ExternalSong(
                        service = MusicService.YOUTUBE,
                        externalId = videoId,
                        title = snippet.optString("title").ifBlank { "Canción de YouTube" },
                        artist = snippet.optString("videoOwnerChannelTitle").ifBlank { "" },
                        durationSeconds = 0,
                        thumbnailUrl = bestThumb(snippet),
                        playbackId = videoId,
                    )
                }
                pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
            } while (pageToken != null)

            withDurations(raw, token)
        }

    /** Completa la duración de cada vídeo en un único lote (videos.list acepta 50 ids). */
    private fun withDurations(songs: List<ExternalSong>, token: String): List<ExternalSong> {
        if (songs.isEmpty()) return songs
        val durations = mutableMapOf<String, Long>()
        songs.chunked(50).forEach { chunk ->
            val ids = chunk.joinToString(",") { it.externalId }
            val url = "https://www.googleapis.com/youtube/v3/videos" +
                "?part=contentDetails&id=" + Uri.encode(ids)
            val json = get(url, token) ?: return@forEach
            val items = json.optJSONArray("items") ?: return@forEach
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val iso = item.optJSONObject("contentDetails")?.optString("duration").orEmpty()
                durations[id] = parseIsoDuration(iso)
            }
        }
        return songs.map { it.copy(durationSeconds = durations[it.externalId] ?: it.durationSeconds) }
    }

    /**
     * Mayor miniatura disponible en la Data API (`default` 120×90, `medium`
     * 320×180, `high` 480×360, `standard` 640×480, `maxres` 1280×720). Antes
     * se usaba `medium`, que se veía pixelada en la carátula del reproductor.
     */
    private fun bestThumb(snippet: JSONObject): String? {
        val thumbs = snippet.optJSONObject("thumbnails") ?: return null
        return listOf("maxres", "standard", "high", "medium", "default")
            .firstNotNullOfOrNull { key ->
                thumbs.optJSONObject(key)?.optString("url")?.takeIf { it.isNotBlank() }
            }
    }

    override suspend fun createPlaylist(name: String): String? = withContext(Dispatchers.IO) {
        val token = oauth.freshToken(MusicService.YOUTUBE) ?: return@withContext null
        val body = JSONObject()
            .put("snippet", JSONObject().put("title", name))
            .put("status", JSONObject().put("privacyStatus", "private"))
            .toString()
        val json = post(
            "https://www.googleapis.com/youtube/v3/playlists?part=snippet,status",
            token,
            body,
        ) ?: return@withContext null
        json.optString("id").takeIf { it.isNotBlank() }
    }

    override suspend fun addToPlaylist(externalPlaylistId: String, song: ExternalSong): Boolean =
        withContext(Dispatchers.IO) {
            val token = oauth.freshToken(MusicService.YOUTUBE) ?: return@withContext false
            val videoId = if (song.externalId.length == 11) {
                song.externalId
            } else {
                searchVideoId("${song.title} ${song.artist}", token) ?: return@withContext false
            }
            val body = JSONObject()
                .put(
                    "snippet",
                    JSONObject()
                        .put("playlistId", externalPlaylistId)
                        .put(
                            "resourceId",
                            JSONObject()
                                .put("kind", "youtube#video")
                                .put("videoId", videoId),
                        ),
                )
                .toString()
            post(
                "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet",
                token,
                body,
            ) != null
        }

    /**
     * Borra la canción de la playlist: la Data API necesita el id del
     * `playlistItem` (no el del vídeo), así que se localiza paginando.
     */
    override suspend fun removeFromPlaylist(
        externalPlaylistId: String,
        song: ExternalSong,
    ): Boolean = withContext(Dispatchers.IO) {
        val token = oauth.freshToken(MusicService.YOUTUBE) ?: return@withContext false
        // El id local de la canción es el videoId de YouTube (11 chars).
        val videoId = song.externalId.takeIf { it.length == 11 }
            ?: return@withContext false
        var pageToken: String? = null
        var itemId: String? = null
        do {
            val url = buildString {
                append("https://www.googleapis.com/youtube/v3/playlistItems")
                append("?part=snippet,contentDetails&maxResults=50")
                append("&playlistId=").append(Uri.encode(externalPlaylistId))
                if (pageToken != null) append("&pageToken=").append(Uri.encode(pageToken))
            }
            val json = get(url, token) ?: break
            val items = json.optJSONArray("items") ?: JSONArray()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val found = item.optJSONObject("snippet")
                    ?.optJSONObject("resourceId")?.optString("videoId")
                    ?: item.optJSONObject("contentDetails")?.optString("videoId")
                if (found == videoId) {
                    itemId = item.optString("id").takeIf { it.isNotBlank() }
                    break
                }
            }
            pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (pageToken != null && itemId == null)

        val id = itemId ?: return@withContext false
        delete(
            "https://www.googleapis.com/youtube/v3/playlistItems?id=" + Uri.encode(id),
            token,
        ) != null
    }

    override suspend fun searchTrackId(title: String, artist: String): String? =
        withContext(Dispatchers.IO) {
            if (!isConnected()) return@withContext null
            val token = oauth.freshToken(MusicService.YOUTUBE) ?: return@withContext null
            searchVideoId("$title $artist", token)
        }

    private fun searchVideoId(query: String, token: String): String? {
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video" +
            "&maxResults=1&q=" + Uri.encode(query)
        val json = get(url, token) ?: return null
        val first = json.optJSONArray("items")?.optJSONObject(0) ?: return null
        return first.optJSONObject("id")?.optString("videoId")?.takeIf { it.isNotBlank() }
    }

    // ---- HTTP ----

    private fun get(url: String, token: String): JSONObject? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "YouTube ${response.code} en $url: ${text.take(300)}")
                return null
            }
            JSONObject(text)
        }
    }.getOrElse {
        Log.e(TAG, "Fallo de red en YouTube", it)
        null
    }

    private fun post(url: String, token: String, body: String): JSONObject? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(body.toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "YouTube ${response.code} en $url: ${text.take(300)}")
                return null
            }
            JSONObject(text)
        }
    }.getOrElse {
        Log.e(TAG, "Fallo de red en YouTube", it)
        null
    }

    private fun delete(url: String, token: String): JSONObject? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "YouTube ${response.code} en DELETE $url: ${text.take(300)}")
                return null
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }.getOrElse {
        Log.e(TAG, "Fallo de red en YouTube (DELETE)", it)
        null
    }

    private suspend fun <T> resolveMany(
        items: List<T>,
        block: suspend (T) -> Unit,
    ) = importMutex.withLock {
        items.forEach { block(it) }
    }

    companion object {
        private const val TAG = "YouTubeMusicService"

        /** Convierte una duración ISO-8601 (PT1H2M3S) a segundos. */
        fun parseIsoDuration(iso: String): Long {
            if (iso.isBlank()) return 0
            val match = Regex("P(?:(\\d+)D)?T?(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
                .find(iso) ?: return 0
            val days = match.groupValues[1].toLongOrNull() ?: 0
            val hours = match.groupValues[2].toLongOrNull() ?: 0
            val minutes = match.groupValues[3].toLongOrNull() ?: 0
            val seconds = match.groupValues[4].toLongOrNull() ?: 0
            return days * 86_400 + hours * 3_600 + minutes * 60 + seconds
        }
    }
}
