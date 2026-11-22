package com.popmusic.app.data.info

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ArtistInfo(
    val name: String,
    val summary: String,
    val imageUrl: String?,
    val sourceUrl: String?,
)

/**
 * Bio y foto del artista desde Wikipedia (REST summary), sin clave de API.
 * Busca primero en español y cae a inglés si no existe.
 */
class ArtistInfoRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun artistInfo(name: String): ArtistInfo? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        for (lang in listOf("es", "en")) {
            val info = fetch(lang, clean) ?: continue
            if (info.summary.isNotBlank()) return info
        }
        return null
    }

    private suspend fun fetch(lang: String, artist: String): ArtistInfo? =
        withContext(Dispatchers.IO) {
            val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/${encode(artist)}"
            val body = runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "GET $url -> ${response.code}")
                    if (!response.isSuccessful) return@use null
                    response.body.string()
                }
            }.onFailure { Log.w(TAG, "Artist info failed", it) }.getOrNull()
                ?: return@withContext null

            runCatching {
                val json = JSONObject(body)
                ArtistInfo(
                    name = json.optString("title", artist),
                    summary = json.optString("extract").trim(),
                    imageUrl = json.optJSONObject("thumbnail")?.optString("source"),
                    sourceUrl = json.optJSONObject("content_urls")
                        ?.optJSONObject("desktop")
                        ?.optString("page"),
                )
            }.onFailure { Log.w(TAG, "Artist info parse failed", it) }.getOrNull()
        }

    private fun encode(value: String): String =
        URLEncoder.encode(value.replace(' ', '_'), "UTF-8")

    private companion object {
        const val TAG = "ArtistInfo"
        const val USER_AGENT = "PoPMusic/0.1 (https://github.com/4lexzx/PoPMusic; contact@popmusic.app)"
    }
}
