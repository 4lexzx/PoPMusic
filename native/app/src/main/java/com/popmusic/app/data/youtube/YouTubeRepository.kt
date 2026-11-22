package com.popmusic.app.data.youtube

import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import com.popmusic.app.util.cleanForDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.UUID

data class VideoDetails(
    val uploaderName: String?,
    val avatarUrl: String?,
)

class YouTubeRepository {

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        NewPipeInitializer.ensureInitialized()
        // Primero YouTube Music con filtro "solo canciones": evita videos de YouTube,
        // canales y playlists en los resultados. Fallback a la búsqueda web de YouTube.
        val fromMusic = runCatching {
            val handler = ServiceList.YouTube.searchQHFactory
                .fromQuery(query, listOf("music_songs"), null)
            val extractor = ServiceList.YouTube.getSearchExtractor(handler) as SearchExtractor
            extractor.fetchPage()
            extractor.initialPage.items.mapNotNull { item ->
                val stream = item as? StreamInfoItem ?: return@mapNotNull null
                val videoId = videoIdFrom(stream.url) ?: return@mapNotNull null
                if (stream.duration <= 0L) return@mapNotNull null

                Song(
                    id = videoId,
                    title = stream.name.cleanForDisplay(),
                    artist = stream.uploaderName.orEmpty().cleanForDisplay(),
                    durationSeconds = stream.duration.coerceAtLeast(0L),
                    thumbnailUrl = bestThumbnail(stream, videoId),
                )
            }
        }.getOrNull().orEmpty()

        if (fromMusic.isNotEmpty()) {
            fromMusic.distinctBy { it.id }
        } else {
            youtubeWebSearch(query)
        }
    }

    private suspend fun youtubeWebSearch(query: String): List<Song> = withContext(Dispatchers.IO) {
        val extractor = ServiceList.YouTube.getSearchExtractor(query) as SearchExtractor
        extractor.fetchPage()

        extractor.initialPage.items.mapNotNull { item ->
            val stream = item as? StreamInfoItem ?: return@mapNotNull null
            val url = stream.url
            val videoId = videoIdFrom(url) ?: return@mapNotNull null

            Song(
                id = videoId,
                title = stream.name.cleanForDisplay(),
                artist = stream.uploaderName.orEmpty().cleanForDisplay(),
                durationSeconds = stream.duration.coerceAtLeast(0L),
                thumbnailUrl = bestThumbnail(stream, videoId),
            )
        }
    }

    /** Búsqueda de vídeos sin el filtro de "solo canciones" (podcasts, entrevistas…). */
    suspend fun searchVideos(query: String): List<Song> = withContext(Dispatchers.IO) {
        NewPipeInitializer.ensureInitialized()
        youtubeWebSearch(query).filterNot { it.durationSeconds in 0..15 }
    }

    /**
     * Chart de tendencias musicales de YouTube (kiosco "trending_music"),
     * el equivalente a los "Top 100" de Echo. [country] es un código ISO
     * tipo "MX", "US"; null usa el país por defecto del extractor.
     */
    suspend fun trendingMusic(country: String? = null): List<Song> = withContext(Dispatchers.IO) {
        NewPipeInitializer.ensureInitialized()
        runCatching {
            val kioskList = ServiceList.YouTube.getKioskList()
            if (!country.isNullOrBlank()) {
                runCatching {
                    kioskList.forceContentCountry(
                        org.schabi.newpipe.extractor.localization.ContentCountry(country),
                    )
                }
            }
            val kiosk = kioskList.getExtractorById("trending_music", null)
            kiosk.fetchPage()
            kiosk.initialPage.items.mapNotNull { item ->
                val stream = item as? StreamInfoItem ?: return@mapNotNull null
                val videoId = videoIdFrom(stream.url) ?: return@mapNotNull null
                if (stream.duration <= 0L) return@mapNotNull null

                Song(
                    id = videoId,
                    title = stream.name.cleanForDisplay(),
                    artist = stream.uploaderName.orEmpty().cleanForDisplay(),
                    durationSeconds = stream.duration.coerceAtLeast(0L),
                    thumbnailUrl = bestThumbnail(stream, videoId),
                )
            }.distinctBy { it.id }
        }.getOrNull().orEmpty()
    }

    suspend fun importPlaylist(link: String): Playlist? = withContext(Dispatchers.IO) {
        NewPipeInitializer.ensureInitialized()
        val playlistId = playlistIdFrom(link) ?: return@withContext null
        runCatching {
            val extractor = ServiceList.YouTube.getPlaylistExtractor(
                "https://www.youtube.com/playlist?list=$playlistId",
            )
            extractor.fetchPage()

            val name = runCatching { extractor.name }
                .getOrNull()
                ?.cleanForDisplay()
                ?.takeIf { it.isNotBlank() }
                ?: "Playlist importada"

            val songs = extractor.initialPage.items.mapNotNull { item ->
                val stream = item as? StreamInfoItem ?: return@mapNotNull null
                val videoId = videoIdFrom(stream.url) ?: return@mapNotNull null

                Song(
                    id = videoId,
                    title = stream.name.cleanForDisplay(),
                    artist = stream.uploaderName.orEmpty().cleanForDisplay(),
                    durationSeconds = stream.duration.coerceAtLeast(0L),
                    thumbnailUrl = bestThumbnail(stream, videoId),
                )
            }.distinctBy { it.id }

            if (songs.isEmpty()) {
                null
            } else {
                Playlist(id = UUID.randomUUID().toString(), name = name, songs = songs)
            }
        }.getOrNull()
    }

    suspend fun resolveAudioUrl(
        videoId: String,
        quality: String = "alta",
    ): String? = withContext(Dispatchers.IO) {
        NewPipeInitializer.ensureInitialized()
        repeat(ATTEMPTS) { attempt ->
            val url = runCatching {
                val extractor = ServiceList.YouTube.getStreamExtractor(
                    "https://www.youtube.com/watch?v=$videoId",
                )
                extractor.fetchPage()
                val streams = extractor.audioStreams
                    .filter {
                        !it.url.isNullOrBlank() &&
                            it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
                    }
                    .sortedBy { it.averageBitrate }
                if (streams.isEmpty()) {
                    null
                } else {
                    when (quality) {
                        "baja" -> streams.first().url
                        "media" -> streams[streams.size / 2].url
                        else -> streams.last().url
                    }
                }
            }.getOrNull()

            if (!url.isNullOrBlank()) return@withContext url
            if (attempt < ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        null
    }

    suspend fun videoDetails(videoId: String): VideoDetails? = withContext(Dispatchers.IO) {
        NewPipeInitializer.ensureInitialized()
        runCatching {
            val info = StreamInfo.getInfo("https://www.youtube.com/watch?v=$videoId")
            VideoDetails(
                uploaderName = info.uploaderName?.trim()?.takeIf { it.isNotBlank() },
                avatarUrl = info.uploaderAvatars
                    .filter { !it.url.isNullOrBlank() }
                    .maxByOrNull { it.height }
                    ?.url,
            )
        }.getOrNull()
    }

    private fun bestThumbnail(stream: StreamInfoItem, videoId: String): String {
        val fromStream = stream.thumbnails
            .filter { !it.url.isNullOrBlank() }
            .maxByOrNull { it.height }
            ?.url
        return fromStream ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }

    private fun videoIdFrom(url: String): String? {
        val match = Regex("[?&]v=([^&]+)").find(url)
        if (match != null) return match.groupValues[1]
        return url.takeIf { it.isNotBlank() }?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
    }

    internal fun playlistIdFrom(link: String): String? {
        val trimmed = link.trim()
        Regex("[?&]list=([A-Za-z0-9_-]+)").find(trimmed)?.let { match ->
            return match.groupValues[1]
        }
        return trimmed.takeIf { it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
    }

    private companion object {
        const val ATTEMPTS = 2
        const val RETRY_DELAY_MS = 300L
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
