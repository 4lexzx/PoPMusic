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

    /** Último error HTTP traducido, para explicar al usuario por qué falló. */
    @Volatile
    private var lastError: String? = null

    fun consumeError(): String? = lastError.also { lastError = null }

    override fun isConnected(): Boolean = accounts.isConnected(MusicService.SPOTIFY)

    override suspend fun importPlaylists(): List<ExternalPlaylist> = withContext(Dispatchers.IO) {
        val token = oauth.freshToken(MusicService.SPOTIFY)
            ?: throw IllegalStateException("Conecta tu cuenta de Spotify antes de importar")
        val result = mutableListOf<ExternalPlaylist>()
        var url: String? = "https://api.spotify.com/v1/me/playlists?limit=50"
        while (url != null) {
            val json = get(url, token)
            if (json == null) {
                consumeError()?.let { throw IllegalStateException(it) }
                break
            }
            val items = json.optJSONArray("items") ?: JSONArray()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                result += ExternalPlaylist(
                    service = MusicService.SPOTIFY,
                    externalId = id,
                    name = item.optString("name").ifBlank { "Playlist de Spotify" },
                    // Spotify renombró `tracks` a `items` en febrero de 2026.
                    trackCount = item.optJSONObject("items")?.optInt("total")
                        ?: item.optJSONObject("tracks")?.optInt("total") ?: 0,
                    thumbnailUrl = item.optJSONArray("images")?.optJSONObject(0)?.optString("url"),
                )
            }
            url = json.optString("next").takeIf { it.isNotBlank() && it != "null" }
        }
        result
    }

    override suspend fun importPlaylistSongs(externalPlaylistId: String): List<ExternalSong> =
        withContext(Dispatchers.IO) {
            val token = oauth.freshToken(MusicService.SPOTIFY)
                ?: throw IllegalStateException("Conecta tu cuenta de Spotify antes de importar")
            val result = mutableListOf<ExternalSong>()
            // `/items` sustituye a `/tracks` (febrero 2026) y admite máximo 50 por página.
            var url: String? = "https://api.spotify.com/v1/playlists/" +
                Uri.encode(externalPlaylistId) + "/items?limit=50"
            while (url != null) {
                val json = get(url, token)
                if (json == null) {
                    consumeError()?.let { throw IllegalStateException(it) }
                    break
                }
                val items = json.optJSONArray("items") ?: JSONArray()
                for (index in 0 until items.length()) {
                    val entry = items.optJSONObject(index) ?: continue
                    val track = entry.optJSONObject("item") ?: entry.optJSONObject("track") ?: continue
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
        val body = JSONObject()
            .put("name", name)
            .put("public", false)
            .toString()
        // `POST /users/{id}/playlists` fue eliminado en febrero de 2026.
        val json = post(
            "https://api.spotify.com/v1/me/playlists",
            token,
            body,
        ) ?: return@withContext null
        json.optString("id").takeIf { it.isNotBlank() }
    }

    /** Cambia el nombre de la playlist (`PUT /playlists/{id}` con `{"name": …}`). */
    override suspend fun renamePlaylist(externalPlaylistId: String, name: String): Boolean =
        withContext(Dispatchers.IO) {
            val token = oauth.freshToken(MusicService.SPOTIFY) ?: return@withContext false
            val body = JSONObject().put("name", name).toString()
            put(
                "https://api.spotify.com/v1/playlists/" + Uri.encode(externalPlaylistId),
                token,
                body,
            ) != null
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
                    Uri.encode(externalPlaylistId) + "/items",
                token,
                body,
            ) != null
        }

    /**
     * Borra la pista de la playlist (`DELETE /playlists/{id}/items` con la
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
            .put("items", JSONArray().put(JSONObject().put("uri", "spotify:track:$trackId")))
            .toString()
        delete(
            "https://api.spotify.com/v1/playlists/" +
                Uri.encode(externalPlaylistId) + "/items",
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
                lastError = errorText(response.code, text)
                Log.w(TAG, "Spotify ${response.code} en $url: ${text.take(300)}")
                return null
            }
            JSONObject(text)
        }
    }.getOrElse {
        lastError = "No se pudo contactar con Spotify: ${it.message ?: "error de red"}"
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
                lastError = errorText(response.code, text)
                Log.w(TAG, "Spotify ${response.code} en $url: ${text.take(300)}")
                return null
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }.getOrElse {
        lastError = "No se pudo contactar con Spotify: ${it.message ?: "error de red"}"
        Log.e(TAG, "Fallo de red en Spotify", it)
        null
    }

    private fun put(url: String, token: String, body: String): JSONObject? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .put(body.toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                lastError = errorText(response.code, text)
                Log.w(TAG, "Spotify ${response.code} en PUT $url: ${text.take(300)}")
                return null
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }.getOrElse {
        lastError = "No se pudo contactar con Spotify: ${it.message ?: "error de red"}"
        Log.e(TAG, "Fallo de red en Spotify (PUT)", it)
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
                lastError = errorText(response.code, text)
                Log.w(TAG, "Spotify ${response.code} en DELETE $url: ${text.take(300)}")
                return null
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }.getOrElse {
        lastError = "No se pudo contactar con Spotify: ${it.message ?: "error de red"}"
        Log.e(TAG, "Fallo de red en Spotify (DELETE)", it)
        null
    }

    /** Traduce los errores típicos de Spotify a un aviso accionable. */
    private fun errorText(code: Int, body: String): String = when (code) {
        401 -> "La sesión de Spotify venció. Vuelve a conectar la cuenta."
        403 -> "Spotify no autoriza esa cuenta: el dueño de la app debe añadirla en " +
            "Users Management (máx. 5 cuentas) y tener Premium."
        429 -> "Spotify limitó las peticiones. Espera unos minutos e inténtalo de nuevo."
        else -> "Spotify devolvió $code: ${body.take(120)}"
    }

    companion object {
        private const val TAG = "SpotifyMusicService"
    }
}
