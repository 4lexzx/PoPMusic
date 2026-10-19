package com.popmusic.app.data.lyrics

import android.util.Log
import com.popmusic.app.domain.LyricLine
import com.popmusic.app.domain.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LrcLibRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun search(title: String, artist: String): Lyrics? = withContext(Dispatchers.IO) {
        val cleanTitle = clean(title)
        val cleanArtist = clean(artist)

        getExact(cleanTitle, cleanArtist)
            ?: getExact(cleanTitle, null)
            ?: fromSearchQuery("$cleanTitle $cleanArtist".trim())
            ?: fromSearchQuery(cleanTitle)
    }

    private fun getExact(trackName: String, artistName: String?): Lyrics? {
        if (trackName.isBlank()) return null
        val url = buildString {
            append("https://lrclib.net/api/get?track_name=")
            append(encode(trackName))
            if (!artistName.isNullOrBlank()) {
                append("&artist_name=")
                append(encode(artistName))
            }
        }

        val json = request(url) ?: return null
        val item = runCatching { JSONObject(json) }.getOrNull() ?: return null
        if (item.has("statusCode")) return null
        return item.toLyrics()
    }

    private fun fromSearchQuery(query: String): Lyrics? {
        if (query.isBlank()) return null
        val url = "https://lrclib.net/api/search?q=${encode(query)}"

        val json = request(url) ?: return null
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return null

        var plain: Lyrics? = null
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val lyrics = item.toLyrics() ?: continue
            if (lyrics.lines.isNotEmpty()) return lyrics
            if (plain == null) plain = lyrics
        }
        Log.d(TAG, "No synced lyrics for \"$query\" (${array.length()} candidates)")
        return plain
    }

    private fun request(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "GET $url -> ${response.code}")
                if (!response.isSuccessful) return null
                response.body.string()
            }
        }.onFailure { Log.e(TAG, "Request failed: $url", it) }.getOrNull()
    }

    private fun JSONObject.toLyrics(): Lyrics? {
        val synced = optString("syncedLyrics").takeIf { it.isNotBlank() }
        val plain = optString("plainLyrics").takeIf { it.isNotBlank() }
        if (synced == null && plain == null) return null
        return Lyrics(
            lines = synced?.let(::parseLrc).orEmpty(),
            plainText = plain,
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun clean(value: String): String {
        var result = value
        BRACKETS.forEach { result = result.replace(it, " ") }
        JUNK.forEach { result = result.replace(it, " ", ignoreCase = true) }
        return result.replace(Regex("\\s+"), " ").trim().trim('-', '–', '—').trim()
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val timestamp = Regex("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")

        return lrc.lineSequence()
            .flatMap { line ->
                val matches = timestamp.findAll(line).toList()
                if (matches.isEmpty()) {
                    emptySequence()
                } else {
                    val text = line.substring(matches.last().range.last + 1).trim()
                    matches.asSequence().map { match ->
                        val minutes = match.groupValues[1].toLong()
                        val seconds = match.groupValues[2].toLong()
                        val fraction = match.groupValues[3]
                        val millis = if (fraction.isEmpty()) {
                            0L
                        } else {
                            fraction.padEnd(3, '0').take(3).toLong()
                        }
                        LyricLine(
                            timeMs = minutes * 60_000 + seconds * 1_000 + millis,
                            text = text,
                        )
                    }
                }
            }
            .filter { it.text.isNotBlank() }
            .sortedBy { it.timeMs }
            .toList()
    }

    private companion object {
        const val TAG = "LrcLib"
        const val USER_AGENT = "PoPMusic/0.1 (https://github.com/4lexzx/PoPMusic)"

        val BRACKETS = listOf(
            Regex("[\\(\\[].*?[\\)\\]]"),
            Regex("\\([^)]*$"),
        )

        val JUNK = listOf(
            "official video", "official music video", "official audio", "official",
            "lyric video", "lyrics video", "lyrics", "letra", "video oficial",
            "music video", "audio", "hd", "hq", "4k", "8k", "remastered",
            "remaster", "visualizer", "live", "en vivo",
        )
    }
}
