package com.popmusic.app.data.youtube

import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class NewPipeDownloader : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val method = request.httpMethod().uppercase()
        val builder = OkHttpRequest.Builder()
            .url(request.url())
            .header("User-Agent", USER_AGENT)

        request.headers()?.forEach { (name, values) ->
            values.forEach { builder.addHeader(name, it) }
        }

        val body = if (method == "POST") {
            (request.dataToSend() ?: ByteArray(0)).toRequestBody()
        } else {
            null
        }

        builder.method(method, body)

        client.newCall(builder.build()).execute().use { response ->
            val responseBody = if (method == "HEAD") "" else response.body.string()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                response.request.url.toString(),
            )
        }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
