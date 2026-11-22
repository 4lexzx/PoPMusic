package com.popmusic.app.data.music

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente de Spotify Web API para importar y escribir playlists de la cuenta
 * vinculada. Requiere un Client ID de Spotify configurado en local.properties
 * (`spotify.clientId`) y el redirect `http://127.0.0.1:5599/callback` dado de
 * alta en el panel de desarrolladores.
 */
class SpotifyMusicService(
    private val accounts: MusicAccountRepository,
    private val oauth: MusicOAuthManager,
) : MusicServiceRepository {

    override val service: MusicService = MusicService.SPOTIFY

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    override fun isConnected(): Boolean = accounts.isConnected(MusicService.SPOTIFY)

    override suspend fun importPlaylists(): List<ExternalPlaylist> = withContext(Dispatchers.IO) {
        val token = oauth.freshToken(MusicService.SPOTIFY) ?: return@withContext emptyList()
        val result = mutableListOf<ExternalPlaylist>()
        var url: String? = "https://api.spotify.com/v1/me/playlists?limit=50"
        while (url != null) {
            val json = get(url, token) ?: break
            val items = json.optJSONArray("items") ?: JSONArray()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                result += ExternalPlaylist(
                    service = MusicService.SPOTIFY,
                    externalId = id,
                    name = item.optString("name").ifBlank { "Playlist de Spotify" },
                    trackCount = item.optJSONObject("tracks")?.optInt("total") ?: 0,
                    thumbnailUrl = item.optJSONArray("images")?.optJSONObject(0)?.optString("url"),
                )
            }
            url = json.optString("next").takeIf { it.isNotBlank() && it != "null" }
        }
        result
    }

    override suspend fun importPlaylistSongs(externalPlaylistId: String): List<ExternalSong> =
        withContext(Dispatchers.IO) {
            val token = oauth.freshToken(MusicService.SPOTIFY) ?: return@withContext emptyList()
            val result = mutableListOf<ExternalSong>()
            var url: String? = "https://api.spotify.com/v1/playlists/" +
                Uri.encode(externalPlaylistId) + "/tracks?limit=100"
            while (url != null) {
                val json = get(url, token) ?: break
                val items = json.optJSONArray("items") ?: JSONArray()
                for (index in 0 until items.length()) {
                    val track = items.optJSONObject(index)?.optJSONObject("track") ?: continue
                    val id = track.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val artists = track.optJSONArray("artists")
                    val artist = buildList {
                        if (artists != null) {
                            for (a in 0 until artists.length()) {
                                val name = artists.optJSONObject(a)?.optString("name").orEmpty()
                                if (name.isNotBlank()) add(name)
                            }
                        }
                    }.joinToString(", ")
                    result += ExternalSong(
                        service = MusicService.SPOTIFY,
                        externalId = id,
                        title = track.optString("name").ifBlank { "Canción de Spotify" },
                        artist = artist,
                        album = track.optJSONObject("album")?.optString("name"),
                        durationSeconds = track.optLong("duration_ms") / 1000,
                        thumbnailUrl = track.optJSONObject("album")
                            ?.optJSONArray("images")?.optJSONObject(0)?.optString("url"),
                    )
                }
                url = json.optString("next").takeIf { it.isNotBlank() && it != "null" }
            }
            result
        }

    override suspend fun createPlaylist(name: String): String? = withContext(Dispatchers.IO) {
        val token = oauth.freshToken(MusicService.SPOTIFY) ?: return@withContext null
        val userId = accounts.accountFor(MusicService.SPOTIFY)?.userId ?: return@withContext null
        val body = JSONObject()
            .put("name", name)
            .put("public", false)
            .toString()
        val json = post(
            "https://api.spotify.com/v1/users/${Uri.encode(userId)}/playlists",
            token,
            body,
        ) ?: return@withContext null
        json.optString("id").takeIf { it.isNotBlank() }
    }

    override suspend fun addToPlaylist(externalPlaylistId: String, song: ExternalSong): Boolean =
        withContext(Dispatchers.IO) {
            val token = oauth.freshToken(MusicService.SPOTIFY) ?: return@withContext false
            val trackId = song.externalId.takeIf { it.length == 22 }
                ?: searchTrackId(song.title, song.artist, token)
                ?: return@withContext false
            val body = JSONObject()
                .put("uris", JSONArray().put("spotify:track:$trackId"))
                .toString()
            post(
                "https://api.spotify.com/v1/playlists/" +
                    Uri.encode(externalPlaylistId) + "/tracks",
                token,
                body,
            ) != null
        }

    /**
     * Borra la pista de la playlist (`DELETE /playlists/{id}/tracks` con la
     * uri). Si el id remoto no es el de Spotify se resuelve con la misma
     * búsqueda que usa `addToPlaylist`, para que el par añadir/quitar casen.
     */
    override suspend fun removeFromPlaylist(
        externalPlaylistId: String,
        song: ExternalSong,
    ): Boolean = withContext(Dispatchers.IO) {
        val token = oauth.freshToken(MusicService.SPOTIFY) ?: return@withContext false
        val trackId = song.externalId.takeIf { it.length == 22 }
            ?: searchTrackId(song.title, song.artist, token)
            ?: return@withContext false
        val body = JSONObject()
            .put("tracks", JSONArray().put(JSONObject().put("uri", "spotify:track:$trackId")))
            .toString()
        delete(
            "https://api.spotify.com/v1/playlists/" +
                Uri.encode(externalPlaylistId) + "/tracks",
            token,
            body,
        ) != null
    }

    override suspend fun searchTrackId(title: String, artist: String): String? =
        withContext(Dispatchers.IO) {
            if (!isConnected()) return@withContext null
            val token = oauth.freshToken(MusicService.SPOTIFY) ?: return@withContext null
            searchTrackId(title, artist, token)
        }

    private fun searchTrackId(title: String, artist: String, token: String): String? {
        val query = Uri.encode("track:$title artist:$artist")
        val json = get("https://api.spotify.com/v1/search?type=track&limit=1&q=$query", token)
            ?: return null
        val first = json.optJSONObject("tracks")?.optJSONArray("items")?.optJSONObject(0)
            ?: return null
        return first.optString("id").takeIf { it.isNotBlank() }
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
                Log.w(TAG, "Spotify ${response.code} en $url: ${text.take(300)}")
                return null
            }
            JSONObject(text)
        }
    }.getOrElse {
        Log.e(TAG, "Fallo de red en Spotify", it)
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
                Log.w(TAG, "Spotify ${response.code} en $url: ${text.take(300)}")
                return null
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }.getOrElse {
        Log.e(TAG, "Fallo de red en Spotify", it)
        null
    }

    private fun delete(url: String, token: String, body: String): JSONObject? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete(body.toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "Spotify ${response.code} en DELETE $url: ${text.take(300)}")
                return null
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }.getOrElse {
        Log.e(TAG, "Fallo de red en Spotify (DELETE)", it)
        null
    }

    companion object {
        private const val TAG = "SpotifyMusicService"
    }
}
