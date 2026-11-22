package com.popmusic.app.data.share

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Convierte un enlace de YouTube en un enlace universal de song.link
 * (Odesli), para compartir canciones que se abren en Spotify, Apple
 * Music, YouTube u otras plataformas. Si la API falla se devuelve null
 * y el llamador comparte el enlace original.
 */
object SongLink {

    private const val API = "https://api.song.link/v1-alpha.1/links"
    private const val TIMEOUT_MS = 5_000

    suspend fun resolve(videoId: String): String? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        runCatching {
            val source = URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")
            val connection = URL("$API?url=$source&userCountry=MX")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                connection.inputStream.use { stream ->
                    val json = JSONObject(stream.readBytes().toString(Charsets.UTF_8))
                    json.optString("pageUrl")
                        .takeIf { it.startsWith("http") && it.isNotBlank() }
                        ?: json.optJSONObject("linksByPlatform")
                            ?.optJSONObject("youtube")
                            ?.optString("url")
                            ?.takeIf { it.startsWith("http") }
                }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
