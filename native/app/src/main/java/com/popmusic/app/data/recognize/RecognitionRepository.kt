package com.popmusic.app.data.recognize

import com.popmusic.app.BuildConfig
import com.popmusic.app.domain.RecognizedSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class RecognitionRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun recognize(file: File): RecognizedSong? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.AUDD_API_KEY
        if (apiKey.isBlank()) return@withContext null

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api_token", apiKey)
            .addFormDataPart("return", "timecode,apple_music,spotify")
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("audio/mp4".toMediaType()),
            )
            .build()

        val request = Request.Builder()
            .url("https://api.audd.io/")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(response.body.string())
            if (json.optString("status") != "success") return@withContext null

            val result = json.optJSONObject("result") ?: return@withContext null
            val title = result.optString("title").takeIf { it.isNotBlank() }
                ?: return@withContext null

            RecognizedSong(
                title = title,
                artist = result.optString("artist"),
                album = result.optString("album").takeIf { it.isNotBlank() },
            )
        }
    }
}
