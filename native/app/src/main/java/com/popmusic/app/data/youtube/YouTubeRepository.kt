package com.popmusic.app.data.youtube

import com.popmusic.app.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class YouTubeRepository {

    init {
        NewPipeInitializer.ensureInitialized()
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val extractor = ServiceList.YouTube.getSearchExtractor(query) as SearchExtractor
        extractor.fetchPage()

        extractor.initialPage.items.mapNotNull { item ->
            val stream = item as? StreamInfoItem ?: return@mapNotNull null
            val url = stream.url
            val videoId = videoIdFrom(url) ?: return@mapNotNull null

            Song(
                id = videoId,
                title = stream.name,
                artist = stream.uploaderName.orEmpty(),
                durationSeconds = stream.duration.coerceAtLeast(0L),
                thumbnailUrl = stream.thumbnails.firstOrNull()?.url,
            )
        }
    }

    suspend fun resolveAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        repeat(ATTEMPTS) { attempt ->
            val url = runCatching {
                StreamInfo.getInfo(
                    ServiceList.YouTube,
                    "https://www.youtube.com/watch?v=$videoId",
                ).audioStreams
                    .filter { !it.url.isNullOrBlank() }
                    .maxByOrNull { it.averageBitrate }
                    ?.url
            }.getOrNull()

            if (!url.isNullOrBlank()) return@withContext url
            if (attempt < ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        null
    }

    private fun videoIdFrom(url: String): String? {
        val match = Regex("[?&]v=([^&]+)").find(url)
        if (match != null) return match.groupValues[1]
        return url.takeIf { it.isNotBlank() }?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val ATTEMPTS = 2
        const val RETRY_DELAY_MS = 700L
    }
}

object NewPipeInitializer {
    @Volatile
    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                NewPipe.init(NewPipeDownloader())
                initialized = true
            }
        }
    }
}
