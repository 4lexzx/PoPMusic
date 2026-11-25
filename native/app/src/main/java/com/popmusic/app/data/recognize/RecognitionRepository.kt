package com.popmusic.app.data.recognize

import android.util.Log
import com.popmusic.app.domain.RecognizedSong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Consulta una huella acústica. Solo viaja la huella, no la grabación. */
class RecognitionRepository {

    sealed interface Result {
        data class Match(val song: RecognizedSong) : Result
        data object NoMatch : Result

        /** Sin conexión o servicio no disponible: no es "no reconocida". */
        data class Unavailable(val offline: Boolean) : Result
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Abre la conexión por adelantado: ahorra unas décimas en la consulta. */
    suspend fun warmUp() = runInterruptible(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url("https://$HOST/").head().build()).execute().close()
        }
        Unit
    }

    suspend fun recognize(uri: String, sampleMs: Int): Result = runInterruptible(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val body = JSONObject()
            .put("timezone", TimeZone.getDefault().id)
            .put(
                "signature",
                JSONObject().put("uri", uri).put("samplems", sampleMs).put("timestamp", now),
            )
            .put("timestamp", now)
            .put("context", JSONObject())
            .put("geolocation", JSONObject())
        val id = UUID.randomUUID().toString().uppercase()
        val device = UUID.randomUUID().toString().uppercase()
        val request = Request.Builder()
            .url(
                "https://$HOST/discovery/v5/es/ES/android/-/tag/$id/$device" +
                    "?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3" +
                    "&sharehub=true&video=v3",
            )
            .header("Content-Language", "es_ES")
            .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 13)")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code}")
                    return@runInterruptible Result.Unavailable(offline = false)
                }
                val json = JSONObject(response.body.string())
                val track = json.optJSONObject("track") ?: return@runInterruptible Result.NoMatch
                val title = track.optString("title").trim()
                if (title.isBlank()) return@runInterruptible Result.NoMatch
                Result.Match(
                    RecognizedSong(
                        title = title,
                        artist = track.optString("subtitle").trim(),
                        artworkUrl = track.optJSONObject("images")
                            ?.optString("coverart")?.takeIf { it.isNotBlank() },
                    ),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Sin conexión", e)
            Result.Unavailable(offline = true)
        } catch (e: Exception) {
            Log.w(TAG, "Respuesta no válida", e)
            Result.Unavailable(offline = false)
        }
    }

    private companion object {
        const val TAG = "Recognition"
        const val HOST = "amp.shazam.com"
    }
}
