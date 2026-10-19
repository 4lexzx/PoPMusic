package com.popmusic.app.data.lyrics

import com.popmusic.app.domain.LyricLine
import com.popmusic.app.domain.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LrcLibRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun search(title: String, artist: String): Lyrics? = withContext(Dispatchers.IO) {
        val query = URLEncoder.encode("$title $artist".trim(), "UTF-8")
        val request = Request.Builder()
            .url("https://lrclib.net/api/search?q=$query")
            .header("User-Agent", "PoPMusic/0.1 (https://github.com/4lexzx/PoPMusic)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null

            val array = JSONArray(response.body.string())
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val synced = item.optString("syncedLyrics").takeIf { it.isNotBlank() }
                if (synced != null) {
                    return@withContext Lyrics(
                        lines = parseLrc(synced),
                        plainText = item.optString("plainLyrics").takeIf { it.isNotBlank() },
                    )
                }
            }
            null
        }
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
}
