package com.popmusic.app.data.ringtone

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

/**
 * Fija una canción como tono de llamada: copia el audio a la carpeta
 * Ringtones de MediaStore (con IS_PENDING) y lo marca como tono por defecto
 * con RingtoneManager. Requiere el permiso especial WRITE_SETTINGS, que el
 * usuario concede desde la pantalla "Modificar ajustes del sistema".
 */
object RingtoneSetter {

    /** ¿Puede la app escribir ajustes del sistema (necesario para el tono)? */
    fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

    /** Abre la pantalla del sistema para conceder "Modificar ajustes". */
    fun requestWriteAccess(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure { Log.w(TAG, "No se pudo abrir WRITE_SETTINGS", it) }
    }

    suspend fun setAsRingtone(context: Context, song: Song): Result<String> =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as? PoPMusicApplication
                ?: return@withContext Result.failure(IllegalStateException("App no inicializada"))
            val source = resolveSource(app, song)
                ?: return@withContext Result.failure(
                    IllegalStateException("No se pudo obtener el audio"),
                )
            try {
                val uri = writeToMediaStore(app, song, source)
                    ?: return@withContext Result.failure(
                        IllegalStateException("No se pudo crear el archivo de tono"),
                    )
                if (canWrite(app)) {
                    RingtoneManager.setActualDefaultRingtoneUri(
                        app,
                        RingtoneManager.TYPE_RINGTONE,
                        uri,
                    )
                    Log.d(TAG, "Tono establecido: $uri")
                    Result.success("«${song.title}» es ahora tu tono de llamada")
                } else {
                    Result.failure(
                        IllegalStateException(
                            "Falta el permiso para modificar ajustes del sistema",
                        ),
                    )
                }
            } catch (error: Exception) {
                Log.w(TAG, "setAsRingtone failed", error)
                Result.failure(error)
            }
        }

    /** Descarga local ya existente o URL de audio resuelta al vuelo. */
    private suspend fun resolveSource(app: PoPMusicApplication, song: Song): Any? {
        app.downloadRepository.localFileFor(song.id)?.let { return it }
        val url = runCatching { YouTubeRepository().resolveAudioUrl(song.id) }
            .getOrNull()
        return url?.takeIf { it.isNotBlank() }
    }

    /** Escribe el audio en MediaStore/Ringtones y devuelve su content URI. */
    private suspend fun writeToMediaStore(
        app: PoPMusicApplication,
        song: Song,
        source: Any,
    ): Uri? {
        val resolver = app.contentResolver
        val displayName = sanitize("${song.title} - ${song.artist}")
        val ext = extensionOf(source)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, song.title)
            put(MediaStore.Audio.Media.ARTIST, song.artist)
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$displayName.$ext")
            put(MediaStore.Audio.Media.MIME_TYPE, mimeOf(ext))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: run {
                Log.w(TAG, "MediaStore insert devolvió null")
                return null
            }
        try {
            resolver.openOutputStream(uri)?.use { out ->
                when (source) {
                    is File -> FileInputStream(source).use { it.copyTo(out) }

                    is String -> downloadTo(source, out)
                }
            } ?: return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (error: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    private fun downloadTo(url: String, out: java.io.OutputStream) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(300, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DOWNLOAD_USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Descarga fallida: HTTP ${response.code}")
            response.body.byteStream().use { it.copyTo(out) }
        }
    }

    private fun extensionOf(source: Any): String = when (source) {
        is File -> source.name.substringAfterLast('.', "m4a").lowercase()
        is String -> source.substringAfterLast('.').substringBefore('?').lowercase()
            .takeIf { it.length in 2..5 && it.all { c -> c.isLetterOrDigit() } } ?: "m4a"
        else -> "m4a"
    }

    private fun mimeOf(ext: String): String = when (ext) {
        "mp3" -> "audio/mpeg"
        "webm" -> "audio/webm"
        "ogg", "opus" -> "audio/ogg"
        "wav" -> "audio/wav"
        else -> "audio/mp4"
    }

    private fun sanitize(value: String): String =
        value.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(80)

    private const val TAG = "RingtoneSetter"
    private const val DOWNLOAD_USER_AGENT = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"
}
