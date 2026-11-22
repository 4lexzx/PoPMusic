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
import kotlin.math.abs

class LrcLibRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun search(title: String, artist: String, durationMs: Long = 0L): Lyrics? =
        withContext(Dispatchers.IO) {
            val rawTitle = title.trim()
            val cleanTitle = clean(title)
            val cleanArtist = clean(artist)
            val expectedSeconds = if (durationMs > 0L) durationMs / 1000.0 else 0.0
            val preferOfficialVideo = isOfficialVideo(rawTitle)

            val queries = buildList {
                val combined = listOf(cleanTitle, cleanArtist)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (combined.isNotBlank()) add(combined)
                if (cleanTitle.isNotBlank() && cleanTitle != combined) add(cleanTitle)
            }
            if (queries.isEmpty()) return@withContext null

            var plain: Lyrics? = null
            for (query in queries) {
                val result = fromSearchQuery(query, expectedSeconds, preferOfficialVideo) ?: continue
                if (result.lines.isNotEmpty()) return@withContext result
                if (plain == null) plain = result
            }
            plain
        }

    private fun fromSearchQuery(
        query: String,
        expectedSeconds: Double,
        preferOfficialVideo: Boolean,
    ): Lyrics? {
        if (query.isBlank()) return null
        val url = "https://lrclib.net/api/search?q=${encode(query)}"

        val json = request(url) ?: return null
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return null

        var plain: Lyrics? = null
        var best: Lyrics? = null
        var bestTrack = ""
        var bestScore = Double.MAX_VALUE
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val lyrics = item.toLyrics() ?: continue
            if (lyrics.lines.isEmpty()) {
                if (plain == null) plain = lyrics
                continue
            }
            if (expectedSeconds <= 0.0) return lyrics

            val trackName = item.optString("trackName")
            val duration = item.optDouble("duration", 0.0)
            val durationDiff = if (duration > 0.0) {
                abs(duration - expectedSeconds)
            } else {
                Double.MAX_VALUE
            }
            val titlePenalty = if (preferOfficialVideo && !isOfficialVideo(trackName)) 12.0 else 0.0
            val score = durationDiff + titlePenalty
            if (best == null || score < bestScore) {
                bestScore = score
                bestTrack = trackName
                best = lyrics
            }
        }

        if (best != null) {
            Log.d(
                TAG,
                "Selected \"$bestTrack\" for \"$query\" (score ${"%.1f".format(bestScore)})",
            )
            return best
        }
        Log.d(TAG, "No synced lyrics for \"$query\" (${array.length()} candidates)")
        return plain
    }

    private fun isOfficialVideo(trackName: String): Boolean =
        VIDEO_MARKERS.any { trackName.contains(it, ignoreCase = true) }

    private fun request(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        repeat(2) { attempt ->
            val result = runCatching {
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "GET $url -> ${response.code} (attempt ${attempt + 1})")
                    if (response.code >= 500 && attempt == 0) return@use null
                    if (!response.isSuccessful) return null
                    response.body.string()
                }
            }.onFailure { Log.e(TAG, "Request failed: $url", it) }.getOrNull()

            if (result != null) return result
            if (attempt == 0) Thread.sleep(250)
        }
        return null
    }

    private fun JSONObject.toLyrics(): Lyrics? {
        val synced = optString("syncedLyrics").takeIf { it.isNotBlank() }
        val plain = optString("plainLyrics").takeIf { it.isNotBlank() }
        if (synced == null && plain == null) return null
        return Lyrics(
            lines = synced?.let(::parseLrc).orEmpty(),
            plainText = plain,
            albumName = optString("albumName").takeIf { it.isNotBlank() },
            artistName = optString("artistName").takeIf { it.isNotBlank() },
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
        val wordTimestamp = Regex("<(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?>")
        val musicSymbols = Regex("[\\s\u266A\u266B\u266C\u2669\uD83C\uDFB5\uD83C\uDFB6]+")
        val lrcOffset = OFFSET_TAG.find(lrc)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L

        return lrc.lineSequence()
            .flatMap { line ->
                val matches = timestamp.findAll(line).toList()
                if (matches.isEmpty()) {
                    emptySequence()
                } else {
                    val content = line.replace(timestamp, "").trim()
                    val words = parseWordTags(content, wordTimestamp)
                    val plainText = if (words.isEmpty()) {
                        content.replace(wordTimestamp, "").trim()
                    } else {
                        words.joinToString(" ") { it.text }
                    }
                    val isInstrumental = plainText.isEmpty() || musicSymbols.matches(plainText)
                    val text = if (isInstrumental) "" else plainText

                    matches.asSequence().map { match ->
                        val lineTime = timestampToMs(match.groupValues[1], match.groupValues[2], match.groupValues[3]) + lrcOffset
                        LyricLine(
                            timeMs = lineTime.coerceAtLeast(0L),
                            text = text,
                            wordTimes = words.map { (lineTime + it.timeMs).coerceAtLeast(0L) },
                            isInstrumental = isInstrumental,
                        )
                    }
                }
            }
            .sortedBy { it.timeMs }
            .toList()
    }

    private fun parseWordTags(content: String, wordTimestamp: Regex): List<LyricWord> {
        if (!content.contains('<')) return emptyList()
        val words = mutableListOf<LyricWord>()
        var pendingTime: Long? = null
        for (part in content.split(wordTimestamp)) {
            val tag = wordTimestamp.matchEntire(part)
            if (tag != null) {
                pendingTime = timestampToMs(tag.groupValues[1], tag.groupValues[2], tag.groupValues[3])
            } else if (pendingTime != null && part.isNotBlank()) {
                part.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { token ->
                    words.add(LyricWord(pendingTime, token))
                }
                pendingTime = null
            }
        }
        return words
    }

    private fun timestampToMs(minutes: String, seconds: String, fraction: String): Long {
        val millis = if (fraction.isEmpty()) {
            0L
        } else {
            fraction.padEnd(3, '0').take(3).toLong()
        }
        return minutes.toLong() * 60_000 + seconds.toLong() * 1_000 + millis
    }

    private data class LyricWord(val timeMs: Long, val text: String)

    private companion object {
        const val TAG = "LrcLib"
        const val USER_AGENT = "PoPMusic/0.1 (https://github.com/4lexzx/PoPMusic)"
        val OFFSET_TAG = Regex("\\[offset:\\s*([+-]?\\d+)\\s*]", RegexOption.IGNORE_CASE)

        val BRACKETS = listOf(
            Regex("[\\(\\[].*?[\\)\\]]"),
            Regex("\\([^)]*$"),
        )

        val VIDEO_MARKERS = listOf(
            "official video", "official music video", "video oficial", "videoclip oficial",
        )

        val JUNK = listOf(
            "official video", "official music video", "official audio", "official",
            "lyric video", "lyrics video", "lyrics", "letra", "video oficial",
            "music video", "audio", "hd", "hq", "4k", "8k", "remastered",
            "remaster", "visualizer", "live", "en vivo",
        )
    }
}
