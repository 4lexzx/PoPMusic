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

            val version = LyricsMatcher.versionTokens(rawTitle)
            val queries = buildList {
                // Primero con la versión: al quitar "(Tiago PZK Version)" se
                // encontraba la letra de la canción normal.
                if (version.isNotEmpty()) {
                    add("${LyricsMatcher.titleWithVersion(rawTitle)} $cleanArtist".trim())
                }
                if (version.isEmpty() && cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
                    // Por campos: solo el título y artista pedidos, sin colaboraciones.
                    add(
                        "track_name=${encode(cleanTitle)}&artist_name=${encode(cleanArtist)}",
                    )
                }
                val combined = listOf(cleanTitle, cleanArtist)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (combined.isNotBlank()) add(combined)
                if (cleanTitle.isNotBlank() && cleanTitle != combined) add(cleanTitle)
            }
            if (queries.isEmpty()) return@withContext null

            var plain: Lyrics? = null
            for (query in queries) {
                val result = fromSearchQuery(
                    query, rawTitle, cleanArtist, version, expectedSeconds, preferOfficialVideo,
                ) ?: continue
                if (result.lines.isNotEmpty()) return@withContext result
                if (plain == null) plain = result
            }
            plain
        }

    private fun fromSearchQuery(
        query: String,
        title: String,
        artist: String,
        version: Set<String>,
        expectedSeconds: Double,
        preferOfficialVideo: Boolean,
    ): Lyrics? {
        if (query.isBlank()) return null
        val url = "https://lrclib.net/api/search?" +
            if (query.startsWith("track_name=")) query else "q=${encode(query)}"

        val json = request(url) ?: return null
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return null

        val found = ArrayList<Pair<LyricsMatcher.Candidate, Lyrics>>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val lyrics = item.toLyrics() ?: continue
            val track = item.optString("trackName")
            val trackArtist = item.optString("artistName")
            // La letra de otra versión no sirve: mejor ninguna que una que no
            // es la que suena. En vivo o acústica sí comparten la original.
            if (!LyricsMatcher.keepsLyrics(version) &&
                !LyricsMatcher.matchesVersion(version, track, trackArtist)
            ) {
                continue
            }
            val duration = item.optDouble("duration", 0.0).takeIf { !it.isNaN() } ?: 0.0
            val durationDiff = when {
                expectedSeconds <= 0.0 -> 0.0
                duration > 0.0 -> abs(duration - expectedSeconds)
                else -> UNKNOWN_DURATION_PENALTY
            }
            val videoPenalty = if (preferOfficialVideo && !isOfficialVideo(track)) 12.0 else 0.0
            val score = durationDiff + videoPenalty +
                LyricsMatcher.penalty(version, title, artist, track, trackArtist)
            val text = lyrics.lines.joinToString(" ") { it.text }
                .ifBlank { lyrics.plainText.orEmpty() }
            found += LyricsMatcher.Candidate(index, track, trackArtist, text, score) to lyrics
        }

        val synced = found.filter { it.second.lines.isNotEmpty() }
        val usable = synced.ifEmpty { found }
        val chosen = LyricsMatcher.choose(usable.map { it.first })
        if (chosen == null) {
            Log.d(TAG, "No lyrics for \"$query\" (${array.length()} candidates)")
            return null
        }
        Log.d(
            TAG,
            "Selected \"${chosen.track}\" (${chosen.artist}) for \"$query\" " +
                "(score ${"%.1f".format(chosen.score)})",
        )
        return usable.first { it.first.index == chosen.index }.second
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
        val synced = text("syncedLyrics")
        val plain = text("plainLyrics")
        if (synced == null && plain == null) return null
        return Lyrics(
            lines = synced?.let(::parseLrc).orEmpty(),
            plainText = plain,
            albumName = text("albumName"),
            artistName = text("artistName"),
        )
    }

    /** En Android, optString devuelve "null" (texto) cuando el valor es nulo. */
    private fun JSONObject.text(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

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
        const val UNKNOWN_DURATION_PENALTY = 20.0
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
