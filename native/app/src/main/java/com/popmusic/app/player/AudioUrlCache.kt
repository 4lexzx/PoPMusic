package com.popmusic.app.player

import android.content.Context
import android.util.Log
import com.popmusic.app.data.youtube.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Caché en disco de las URLs de audio ya resueltas de YouTube.
 *
 * Resolver la URL con NewPipe tarda entre 300 ms y 2 s; recordarla permite que
 * volver a una canción (o reabrir la app) sea instantáneo. Las URLs de
 * googlevideo caducan (~6 h), así que se descartan a las 4 h.
 */
class AudioUrlCache(context: Context) {

    private val file = File(context.filesDir, "audio_urls.json")
    private val entries = ConcurrentHashMap<String, Entry>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val warming = ConcurrentHashMap.newKeySet<String>()
    private val warmPermits = Semaphore(6)

    @Volatile
    private var lastPersist = 0L

    init {
        load()
    }

    /** URL válida en caché para ese vídeo, o null si no hay/está caducada. */
    fun get(videoId: String): String? {
        val entry = entries[videoId] ?: return null
        if (System.currentTimeMillis() - entry.savedAt > TTL_MS) {
            entries.remove(videoId)
            return null
        }
        return entry.url
    }

    fun put(videoId: String, url: String) {
        if (videoId.isBlank() || url.isBlank()) return
        entries[videoId] = Entry(url, System.currentTimeMillis())
        maybePersist()
    }

    /**
     * Resuelve en segundo plano las URLs que faltan para que pulsar cualquier
     * canción (incluso una lejana en la lista) sea instantáneo: al reproducir,
     * la URL ya está aquí y no hay que esperar a YouTube. Se deduplica y se
     * limita a seis peticiones simultáneas para no saturar la red.
     */
    fun warm(ids: Collection<String>) {
        val resolver = YouTubeRepository()
        val missing = ids.filter { it.isNotBlank() && get(it) == null && warming.add(it) }
        if (missing.isEmpty()) return
        scope.launch {
            missing.forEach { id ->
                launch {
                    warmPermits.withPermit {
                        try {
                            if (get(id) == null) {
                                resolver.resolveAudioUrl(id)?.let { put(id, it) }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo precalentar $id", e)
                        } finally {
                            warming.remove(id)
                        }
                    }
                }
            }
        }
    }

    /** Descarta la URL de ese vídeo (caducada o limitada por el servidor). */
    fun invalidate(videoId: String) {
        if (videoId.isBlank()) return
        if (entries.remove(videoId) != null) {
            lastPersist = 0L
            maybePersist()
        }
    }

    private fun maybePersist() {
        val now = System.currentTimeMillis()
        if (now - lastPersist < PERSIST_INTERVAL_MS) return
        lastPersist = now
        val snapshot = entries.filterValues { now - it.savedAt <= TTL_MS }
            .mapValues { it.value }
        scope.launch {
            runCatching {
                val json = JSONObject()
                snapshot.forEach { (id, entry) ->
                    json.put(id, JSONObject().put("url", entry.url).put("at", entry.savedAt))
                }
                file.writeText(json.toString())
            }.onFailure { Log.w(TAG, "No se pudo guardar la caché de URLs", it) }
        }
    }

    private fun load() {
        if (!file.exists()) return
        val now = System.currentTimeMillis()
        runCatching {
            val json = JSONObject(file.readText())
            json.keys().forEach { id ->
                val item = json.optJSONObject(id) ?: return@forEach
                val url = item.optString("url")
                val savedAt = item.optLong("at")
                if (url.isNotBlank() && now - savedAt <= TTL_MS) {
                    entries[id] = Entry(url, savedAt)
                }
            }
        }.onFailure { Log.w(TAG, "No se pudo leer la caché de URLs", it) }
    }

    private data class Entry(val url: String, val savedAt: Long)

    private companion object {
        const val TAG = "AudioUrlCache"
        const val TTL_MS = 4L * 60L * 60L * 1000L
        const val PERSIST_INTERVAL_MS = 3_000L
    }
}
