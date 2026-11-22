package com.popmusic.app.data.party

import com.popmusic.app.domain.Song
import org.json.JSONArray
import org.json.JSONObject

/**
 * Modelo de la sala "Todos controlan": cola compartida en vivo donde
 * cualquiera puede cambiar la canción, pausar o añadir temas.
 *
 * El estado completo viaja por broadcast (último en llegar gana) para que
 * no haga falta host ni base de datos: Supabase Realtime solo transporta.
 */

/** Participante de la sala (presencia por broadcast, sin tablas). */
data class PartyParticipant(
    val id: String,
    val name: String,
    /** Foto de perfil de la cuenta (si el proveedor la compartió). */
    val avatarUrl: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
)

/** Snapshot compartido de la reproducción. */
data class PartySnapshot(
    val queue: List<Song>,
    val index: Int,
    val playing: Boolean,
    /** Posición (ms) en el instante [updatedAt] del emisor. */
    val positionMs: Long,
    val updatedAt: Long,
    val updatedBy: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("queue", JSONArray().apply {
            queue.forEach { song ->
                put(JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("duration", song.durationSeconds)
                    put("thumb", song.thumbnailUrl ?: "")
                    put("stream", song.streamUrl ?: "")
                })
            }
        })
        put("index", index)
        put("playing", playing)
        put("position", positionMs)
        put("updatedAt", updatedAt)
        put("by", updatedBy)
    }

    companion object {
        fun fromJson(obj: JSONObject): PartySnapshot? {
            val queueArray = obj.optJSONArray("queue") ?: return null
            val queue = buildList {
                for (i in 0 until queueArray.length()) {
                    val item = queueArray.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    if (id.isBlank()) continue
                    add(
                        Song(
                            id = id,
                            title = item.optString("title"),
                            artist = item.optString("artist"),
                            durationSeconds = item.optLong("duration"),
                            thumbnailUrl = item.optString("thumb").ifBlank { null },
                            streamUrl = item.optString("stream").ifBlank { null },
                        ),
                    )
                }
            }
            return PartySnapshot(
                queue = queue,
                index = obj.optInt("index").coerceAtLeast(0),
                playing = obj.optBoolean("playing"),
                positionMs = obj.optLong("position").coerceAtLeast(0L),
                updatedAt = obj.optLong("updatedAt"),
                updatedBy = obj.optString("by"),
            )
        }
    }
}

/** Estado de la conexión de la sala. */
enum class PartyStatus {
    IDLE,
    CONNECTING,
    JOINED,
    ERROR,
}
