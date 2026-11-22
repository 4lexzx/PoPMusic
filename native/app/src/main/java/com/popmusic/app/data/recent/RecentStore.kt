package com.popmusic.app.data.recent

import android.content.Context
import com.popmusic.app.domain.Song
import org.json.JSONArray
import org.json.JSONObject

/**
 * Historial de reproducciones (estilo Echo "Activity History").
 * Se guarda en SharedPreferences como JSON; se limita a [MAX_ITEMS] para
 * no crecer sin control y se reordena para que la canción más reciente
 * quede siempre al principio.
 */
object RecentStore {

    private const val PREFS_NAME = "popmusic_recent"
    private const val KEY_ITEMS = "items"
    private const val KEY_LAST_ID = "last_recorded_id"
    private const val MAX_ITEMS = 80

    /** Inserta la canción al inicio del historial si aún no es la última. */
    fun record(context: Context, song: Song) {
        if (song.id.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_ID, null) == song.id) return

        val items = JSONArray(prefs.getString(KEY_ITEMS, "[]") ?: "[]")
        val filtered = JSONArray()
        for (i in 0 until items.length()) {
            val entry = items.optJSONObject(i) ?: continue
            if (entry.optString("id") != song.id) filtered.put(entry)
        }

        val fresh = JSONObject()
            .put("id", song.id)
            .put("title", song.title)
            .put("artist", song.artist)
            .put("durationSeconds", song.durationSeconds)
            .put("thumbnailUrl", song.thumbnailUrl ?: JSONObject.NULL)
            .put("playedAt", System.currentTimeMillis())

        val result = JSONArray()
        result.put(fresh)
        for (i in 0 until filtered.length()) {
            if (result.length() >= MAX_ITEMS) break
            result.put(filtered.optJSONObject(i))
        }

        prefs.edit()
            .putString(KEY_ITEMS, result.toString())
            .putString(KEY_LAST_ID, song.id)
            .apply()
    }

    /** Historial completo, más reciente primero. */
    fun recent(context: Context): List<Song> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val items = JSONArray(prefs.getString(KEY_ITEMS, "[]") ?: "[]")
        val songs = ArrayList<Song>(items.length())
        for (i in 0 until items.length()) {
            val entry = items.optJSONObject(i) ?: continue
            val id = entry.optString("id")
            if (id.isBlank()) continue
            songs.add(
                Song(
                    id = id,
                    title = entry.optString("title"),
                    artist = entry.optString("artist"),
                    durationSeconds = entry.optLong("durationSeconds", 0L),
                    thumbnailUrl = entry.optString("thumbnailUrl").takeIf {
                        it.isNotBlank() && it != "null"
                    },
                ),
            )
        }
        return songs
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ITEMS)
            .remove(KEY_LAST_ID)
            .apply()
    }
}
