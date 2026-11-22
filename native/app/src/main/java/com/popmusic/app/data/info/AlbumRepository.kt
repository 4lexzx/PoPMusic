package com.popmusic.app.data.info

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Busca el nombre real del álbum (iTunes Search API, sin clave). */
class AlbumRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun albumFor(title: String, artist: String): String? =
        withContext(Dispatchers.IO) {
            val artistClean = normalize(artist)
            var titleClean = normalize(title)
            val prefix = "$artistClean - "
            if (artistClean.isNotBlank() && titleClean.startsWith(prefix, ignoreCase = true)) {
                titleClean = titleClean.substring(prefix.length)
            }
            val term = listOf(titleClean, artistClean)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" ")
            if (term.isBlank()) return@withContext null

            val url = "https://itunes.apple.com/search?term=${encode(term)}&entity=song&limit=12"
            android.util.Log.d("AlbumRepo", "term=$term")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val json = runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body.string()
                }
            }.onFailure { android.util.Log.e("AlbumRepo", "request failed", it) }
                .getOrNull() ?: return@withContext null

            val results = runCatching { JSONArray(json) }.getOrNull() ?: return@withContext null
            pickAlbum(results, title).also { android.util.Log.d("AlbumRepo", "picked=$it") }
        }

    private fun pickAlbum(results: JSONArray, title: String): String? {
        val target = normalize(title)
        var fallback: String? = null
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val collection = item.optString("collectionName").trim()
            if (collection.isBlank()) continue
            val track = normalize(item.optString("trackName"))
            if (track.isNotBlank() && (track == target || target.contains(track) || track.contains(target))) {
                return collection
            }
            if (fallback == null) fallback = collection
        }
        return fallback
    }

    private fun normalize(value: String): String = value.lowercase()
        .replace(Regex("[\\(\\[].*?[\\)\\]]"), " ")
        .replace(
            Regex(
                "\\b(feat|ft|official|video|audio|lyric|remaster(ed)?|hd|4k)\\b",
                RegexOption.IGNORE_CASE,
            ),
            " ",
        )
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val USER_AGENT = "PoPMusic/0.1 (https://github.com/4lexzx/PoPMusic)"
    }
}
