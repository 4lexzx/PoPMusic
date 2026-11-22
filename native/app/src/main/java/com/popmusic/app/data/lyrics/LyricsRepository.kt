package com.popmusic.app.data.lyrics

import android.content.Context
import android.util.Log
import com.popmusic.app.domain.LyricLine
import com.popmusic.app.domain.Lyrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class LyricsRepository(context: Context) {

    private val network = LrcLibRepository()
    private val directory = File(context.cacheDir, "lyrics").apply { mkdirs() }
    private val offsets = context.getSharedPreferences(OFFSETS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Deferred<Lyrics?>>()
    private val memory = ConcurrentHashMap<String, Lyrics>()

    fun prefetch(
        songId: String,
        title: String,
        artist: String,
        durationMs: Long,
    ): Deferred<Lyrics?> {
        val key = key(songId, title, artist, durationMs)
        memory[key]?.let { return scope.async { it } }
        return jobs.computeIfAbsent(key) {
            val deferred = scope.async {
                read(key)?.also { memory[key] = it }
                    ?: network.search(title, artist, durationMs)?.also {
                        memory[key] = it
                        write(key, it)
                    }
            }
            deferred.invokeOnCompletion { jobs.remove(key, deferred) }
            deferred
        }
    }

    suspend fun load(
        songId: String,
        title: String,
        artist: String,
        durationMs: Long,
    ): Lyrics? {
        if (title.isBlank()) return null
        val key = key(songId, title, artist, durationMs)
        memory[key]?.let { return it }
        return prefetch(songId, title, artist, durationMs).await()
    }

    fun offsetMs(songId: String, title: String, artist: String, durationMs: Long): Int =
        offsets.getInt(key(songId, title, artist, durationMs), 0)

    fun setOffsetMs(songId: String, title: String, artist: String, durationMs: Long, value: Int) {
        offsets.edit()
            .putInt(key(songId, title, artist, durationMs), value.coerceIn(-5_000, 5_000))
            .apply()
    }

    private fun key(songId: String, title: String, artist: String, durationMs: Long): String {
        if (songId.isNotBlank()) return "id_$songId"
        return "meta_${title.trim()}|${artist.trim()}|${durationMs / 1_000}"
    }

    private fun file(key: String): File {
        val safe = key.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)
        return File(directory, "$safe-${key.hashCode().toUInt().toString(16)}.json")
    }

    private fun read(key: String): Lyrics? = runCatching {
        val file = file(key)
        if (!file.exists()) return null
        val root = JSONObject(file.readText())
        val array = root.optJSONArray("lines") ?: JSONArray()
        val lines = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val text = item.optString("text")
                val wordTimesArray = item.optJSONArray("wordTimes") ?: JSONArray()
                val wordTimes = buildList {
                    for (j in 0 until wordTimesArray.length()) {
                        add(wordTimesArray.optLong(j))
                    }
                }
                if (text.isBlank() && !item.optBoolean("isInstrumental")) continue
                add(
                    LyricLine(
                        timeMs = item.optLong("timeMs"),
                        text = text,
                        wordTimes = wordTimes,
                        isInstrumental = item.optBoolean("isInstrumental"),
                    ),
                )
            }
        }
        val plain = root.optString("plainText").takeIf { it.isNotBlank() }
        if (lines.isEmpty() && plain == null) null else Lyrics(
            lines = lines,
            plainText = plain,
            albumName = root.optString("albumName").takeIf { it.isNotBlank() },
            artistName = root.optString("artistName").takeIf { it.isNotBlank() },
        )
    }.onFailure { Log.w(TAG, "Could not read cached lyrics", it) }.getOrNull()

    private fun write(key: String, lyrics: Lyrics) = runCatching {
        val file = file(key)
        val temp = File(file.parentFile, "${file.name}.tmp")
        val root = JSONObject().apply {
            put("lines", JSONArray().apply {
                lyrics.lines.forEach { line ->
                    put(JSONObject().apply {
                        put("timeMs", line.timeMs)
                        put("text", line.text)
                        put("wordTimes", JSONArray().apply {
                            line.wordTimes.forEach { put(it) }
                        })
                        put("isInstrumental", line.isInstrumental)
                    })
                }
            })
            lyrics.plainText?.let { put("plainText", it) }
            lyrics.albumName?.let { put("albumName", it) }
            lyrics.artistName?.let { put("artistName", it) }
        }
        temp.writeText(root.toString())
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }.onFailure { Log.w(TAG, "Could not cache lyrics", it) }

    private companion object {
        const val TAG = "LyricsRepository"
        const val OFFSETS_NAME = "lyrics_offsets"
    }
}
