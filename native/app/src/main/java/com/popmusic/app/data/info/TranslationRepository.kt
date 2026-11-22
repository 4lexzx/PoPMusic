package com.popmusic.app.data.info

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TranslationRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, String>()

    suspend fun translate(text: String, targetLang: String = "es"): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        cache[clean]?.let { return it }

        val url = buildString {
            append("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=")
            append(targetLang)
            append("&dt=t&q=")
            append(URLEncoder.encode(clean, "UTF-8"))
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body.string()
                    val root = JSONArray(body)
                    val segments = root.optJSONArray(0) ?: return@use null
                    buildString {
                        for (i in 0 until segments.length()) {
                            append(segments.optJSONArray(i)?.optString(0).orEmpty())
                        }
                    }.trim()
                }
            }.onFailure { Log.w(TAG, "Translation failed", it) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.also { cache[clean] = it }
        }
    }

    private companion object {
        const val TAG = "Translation"
    }
}
