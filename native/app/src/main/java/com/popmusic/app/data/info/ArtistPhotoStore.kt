package com.popmusic.app.data.info

import android.content.Context
import com.popmusic.app.data.recommendations.RecommendationText
import com.popmusic.app.data.youtube.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * Fotos de perfil de artistas (YouTube Music). Las encontradas se guardan 30
 * días; las no encontradas solo se recuerdan durante la sesión.
 */
object ArtistPhotoStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val youtube = YouTubeRepository()
    private val permits = Semaphore(3)
    private val memory = ConcurrentHashMap<String, String>()
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val jobs = ConcurrentHashMap<String, Deferred<String?>>()

    private fun key(name: String): String = RecommendationText.key(primary(name))
        // Nombres sin letras latinas ("방탄소년단") darían una clave vacía.
        .ifBlank { name.trim().lowercase() }

    /** "Teddy Swims & Tiago PZK" → "Teddy Swims". */
    private fun primary(name: String): String = name
        .removeSuffix(" - Topic")
        .split(Regex("\\s*[,&•]\\s*|\\s+(?:feat\\.?|ft\\.?|x)\\s+", RegexOption.IGNORE_CASE))
        .first()
        .trim()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("artist_photos", Context.MODE_PRIVATE)

    /** Foto ya conocida, sin red. */
    fun cached(context: Context, name: String): String? {
        val key = key(name)
        memory[key]?.let { return it }
        val stored = prefs(context).getString(key, null) ?: return null
        val savedAt = stored.substringBefore('|').toLongOrNull() ?: return null
        if (System.currentTimeMillis() - savedAt > TTL_MS) return null
        return stored.substringAfter('|').also { memory[key] = it }
    }

    fun isMissing(name: String): Boolean = key(name) in missing

    suspend fun photo(context: Context, name: String): String? {
        cached(context, name)?.let { return it }
        val key = key(name)
        if (key.isBlank() || key in missing) return null
        val app = context.applicationContext
        val job = jobs.computeIfAbsent(key) {
            scope.async {
                permits.withPermit {
                    // null = fallo de red: se volverá a intentar.
                    val found = runCatching { youtube.artists(primary(name)) }.getOrNull()
                        ?: return@withPermit null
                    val url = found.firstOrNull { RecommendationText.key(it.first) == key }?.second
                        ?: found.firstOrNull { it.first.equals(primary(name), ignoreCase = true) }?.second
                    if (url == null) {
                        missing += key
                    } else {
                        memory[key] = url
                        prefs(app).edit()
                            .putString(key, "${System.currentTimeMillis()}|$url")
                            .apply()
                    }
                    url
                }
            }
        }
        return try {
            job.await()
        } finally {
            if (job.isCompleted) jobs.remove(key, job)
        }
    }

    private const val TTL_MS = 30L * 24L * 60L * 60L * 1000L
}
