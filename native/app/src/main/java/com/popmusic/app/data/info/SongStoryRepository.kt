package com.popmusic.app.data.info

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SongStory(
    val pageTitle: String,
    val extract: String,
    val sourceUrl: String?,
    val lang: String,
)

/**
 * "Music DNA": historia/anécdota de la canción desde la API de Wikipedia
 * (búsqueda + extracto introductorio), sin clave de API. Prueba primero en
 * español y cae a inglés. Si la búsqueda por canción falla, reintenta solo
 * con el artista.
 */
class SongStoryRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun story(title: String, artist: String): SongStory? =
        withContext(Dispatchers.IO) {
            val cleanTitle = title.trim()
            val cleanArtist = artist.trim()
            val queries = buildList {
                if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
                    add("$cleanTitle $cleanArtist")
                }
                if (cleanTitle.isNotBlank()) add(cleanTitle)
                if (cleanArtist.isNotBlank()) add(cleanArtist)
            }
            for (lang in listOf("es", "en")) {
                for (query in queries) {
                    val found = fetch(lang, query) ?: continue
                    if (found.extract.length >= MIN_EXTRACT) return@withContext found
                }
            }
            null
        }

    private fun fetch(lang: String, query: String): SongStory? {
        val pageId = search(lang, query) ?: return null
        return extracts(lang, pageId)
    }

    private fun search(lang: String, query: String): Int? {
        val url = "https://$lang.wikipedia.org/w/api.php" +
            "?action=query&list=search&srsearch=${encode(query)}" +
            "&srlimit=1&format=json&utf8=1"
        val body = get(url) ?: return null
        return runCatching {
            JSONObject(body)
                .optJSONObject("query")
                ?.optJSONArray("search")
                ?.optJSONObject(0)
                ?.optInt("pageid", -1)
                ?.takeIf { it > 0 }
        }.onFailure { Log.w(TAG, "search parse failed", it) }.getOrNull()
    }

    private fun extracts(lang: String, pageId: Int): SongStory? {
        val url = "https://$lang.wikipedia.org/w/api.php" +
            "?action=query&prop=extracts|info&inprop=url" +
            "&pageids=$pageId&exintro=1&explaintext=1&format=json&utf8=1"
        val body = get(url) ?: return null
        return runCatching {
            val page = JSONObject(body)
                .optJSONObject("query")
                ?.optJSONObject("pages")
                ?.optJSONObject(pageId.toString())
                ?: return null
            val extract = page.optString("extract").trim()
            if (extract.isEmpty()) return null
            SongStory(
                pageTitle = page.optString("title"),
                extract = extract,
                sourceUrl = page.optString("fullurl").takeIf { it.isNotBlank() }
                    ?: "https://$lang.wikipedia.org/?curid=$pageId",
                lang = lang,
            )
        }.onFailure { Log.w(TAG, "extracts parse failed", it) }.getOrNull()
    }

    private fun get(url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            Log.d(TAG, "GET $url -> ${response.code}")
            if (!response.isSuccessful) return@use null
            response.body.string()
        }
    }.onFailure { Log.w(TAG, "GET failed: $url", it) }.getOrNull()

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val TAG = "SongStory"
        const val MIN_EXTRACT = 120
        const val USER_AGENT = "PoPMusic/0.1 (https://github.com/4lexzx/PoPMusic; contact@popmusic.app)"
    }
}
