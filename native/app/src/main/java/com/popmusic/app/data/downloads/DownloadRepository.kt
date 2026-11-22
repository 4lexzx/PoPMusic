package com.popmusic.app.data.downloads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.data.prefs.AppPreferences
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Descarga el audio de una canción a almacenamiento privado de la app
 * y lo registra en la biblioteca como "Descargas".
 */
class DownloadRepository(
    context: Context,
    private val library: LibraryRepository,
    private val preferences: AppPreferences,
) {

    private val appContext = context.applicationContext
    private val youtube = YouTubeRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = ConcurrentHashMap.newKeySet<String>()
    private val queue = Semaphore(MAX_PARALLEL_DOWNLOADS)

    init {
        library.downloadsDir.listFiles()
            ?.filter { it.name.endsWith(".part") }
            ?.forEach { runCatching { it.delete() } }
    }

    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress: StateFlow<Map<String, Float>> = _progress.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(600, TimeUnit.SECONDS)
        .build()

    fun isDownloading(songId: String): Boolean = songId in active

    fun isDownloaded(songId: String): Boolean = library.isDownloaded(songId)

    /** Comprueba si el dispositivo está en Wi-Fi (o Ethernet). */
    fun isOnWifi(): Boolean {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return true
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun localFileFor(songId: String): File? =
        library.downloadsDir.listFiles()
            ?.firstOrNull { it.isFile && it.name.startsWith("$songId.") && !it.name.endsWith(".part") }

    /**
     * Encola la descarga. Devuelve false cuando está bloqueada por la
     * preferencia "solo Wi-Fi" o porque ya existe.
     */
    fun download(song: Song): Boolean {
        if (library.isDownloaded(song.id)) return true
        if (preferences.playback.value.downloadWifiOnly && !isOnWifi()) return false
        if (!active.add(song.id)) return true

        scope.launch {
            try {
                queue.withPermit {
                    var lastError: Throwable? = null
                    // Reintentos: las URLs de googlevideo caducan y la conexión
                    // puede resetearse a mitad de stream (SocketException).
                    for (attempt in 1..MAX_ATTEMPTS) {
                        try {
                            downloadNow(song)
                            lastError = null
                            break
                        } catch (t: Throwable) {
                            lastError = t
                            Log.w(TAG, "Intento $attempt/$MAX_ATTEMPTS fallido (${song.id})", t)
                            cleanPartFiles(song.id)
                            if (attempt < MAX_ATTEMPTS) {
                                delay(RETRY_DELAY_MS)
                            }
                        }
                    }
                    lastError?.let { Log.e(TAG, "Fallo al descargar ${song.id}", it) }
                }
            } finally {
                active.remove(song.id)
                _progress.value = _progress.value - song.id
            }
        }
        return true
    }

    private fun cleanPartFiles(songId: String) {
        library.downloadsDir.listFiles()
            ?.filter { it.name.startsWith("$songId.") && it.name.endsWith(".part") }
            ?.forEach { runCatching { it.delete() } }
    }

    private suspend fun downloadNow(song: Song) {
        if (library.isDownloaded(song.id)) return
        // URL fresca en cada intento: la anterior puede haber caducado.
        val url = youtube.resolveAudioUrl(song.id) ?: error("No se encontró audio para descargar")
        val extension = extensionFrom(url)
        val partFile = File(library.downloadsDir, "${song.id}.$extension.part")
        val finalFile = File(library.downloadsDir, "${song.id}.$extension")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body
            val total = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            _progress.value =
                                _progress.value + (song.id to downloaded.toFloat() / total)
                        }
                    }
                }
            }
        }

        if (partFile.length() <= 0L) error("Archivo vacío")
        if (finalFile.exists()) finalFile.delete()
        if (!partFile.renameTo(finalFile)) {
            partFile.copyTo(finalFile, overwrite = true)
            partFile.delete()
        }
        library.addDownload(song)
        Log.d(TAG, "Descargada ${song.id} -> ${finalFile.name} (${finalFile.length()} bytes)")
    }

    private fun extensionFrom(url: String): String {
        val mime = Regex("[?&]mime=audio(?:%2F|/)(\\w+)").find(url)?.groupValues?.get(1)?.lowercase()
        return when (mime) {
            "mp4", "m4a" -> "m4a"
            "webm" -> "webm"
            "3gpp" -> "3gp"
            else -> "m4a"
        }
    }

    private companion object {
        const val TAG = "Downloads"
        const val DEFAULT_BUFFER = 64 * 1024
        const val MAX_PARALLEL_DOWNLOADS = 3
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 2_000L
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"
    }
}
