package com.popmusic.app.data.youtube

import org.json.JSONObject
import java.util.Locale
import android.util.Log
import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import com.popmusic.app.util.cleanForDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.InfoItem
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

    /** Artistas de YouTube Music con su foto: pares nombre → URL. */
    suspend fun artists(query: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        NewPipeInitializer.ensureInitialized()
        val handler = ServiceList.YouTube.searchQHFactory
            .fromQuery(query, listOf("music_artists"), null)
        val extractor = ServiceList.YouTube.getSearchExtractor(handler) as SearchExtractor
        extractor.fetchPage()
        extractor.initialPage.items.mapNotNull { item ->
            val url = item.thumbnails
                .filter { !it.url.isNullOrBlank() }
                .maxByOrNull { it.height }
                ?.url ?: return@mapNotNull null
            // Google sirve cualquier tamaño: 300 px basta y no se ve borroso.
            item.name to url.replace(Regex("=w\\d+-h\\d+"), "=w300-h300")
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

    /** Etiquetas y categoría del vídeo semilla, para fijar el género de la radio. */
    suspend fun streamTags(videoId: String): Set<String> = withContext(Dispatchers.IO) {
        runCatching {
            NewPipeInitializer.ensureInitialized()
            val info = StreamInfo.getInfo("https://www.youtube.com/watch?v=$videoId")
            (info.tags.orEmpty() + listOfNotNull(info.category))
                .flatMap { com.popmusic.app.data.recommendations.RecommendationText.tags(it) }
                .toSet()
        }.getOrDefault(emptySet())
    }

    /**
     * Canciones similares a un vídeo: equivalente al endpoint `next`/related
     * que Echo usa para convertir una canción en radio. NewPipe expone los
     * vídeos relacionados dentro del StreamInfo; se filtran los que no son
     * canciones (duración muy corta o nula) para no mezclar shorts.
     */
    suspend fun related(videoId: String, limit: Int = 20): List<Song> = withContext(Dispatchers.IO) {
        NewPipeInitializer.ensureInitialized()
        runCatching {
            val info = StreamInfo.getInfo("https://www.youtube.com/watch?v=$videoId")
            val related: List<InfoItem> = info.relatedStreams.orEmpty()
            related.mapNotNull { item ->
                val stream = item as? StreamInfoItem ?: return@mapNotNull null
                val id = videoIdFrom(stream.url) ?: return@mapNotNull null
                if (id == videoId || stream.duration in 1..45) return@mapNotNull null
                val views = stream.viewCount
                // Subidas de canales pequeños con poquísimas reproducciones
                // suelen ser la misma canción repetida con otro título.
                if (views in 1..MIN_RELATED_VIEWS) return@mapNotNull null
                Song(
                    id = id,
                    title = stream.name.cleanForDisplay(),
                    artist = stream.uploaderName.orEmpty().cleanForDisplay(),
                    durationSeconds = stream.duration.coerceAtLeast(0L),
                    thumbnailUrl = bestThumbnail(stream, id),
                ) to views
            }.distinctBy { it.first.id }
                .sortedByDescending { it.second.coerceAtLeast(0L) }
                .map { it.first }
                .take(limit)
        }.getOrDefault(emptyList())
    }

    /**
     * Radio real de YouTube, el equivalente a lo que hace Echo con
     * `YouTubeQueue.radio()` + el endpoint `next`: YouTube genera una playlist
     * "RDAMVM{videoId}" con las canciones que encajan con la semilla. Es la
     * fuente principal de la radio, en vez de adivinar géneros con búsquedas.
     * Devuelve también la página siguiente para seguir paginando como Echo.
     */
    suspend fun radioPlaylist(videoId: String, limit: Int = 100): List<Song> =
        withContext(Dispatchers.IO) {
            NewPipeInitializer.ensureInitialized()
            val playlistId = if (videoId.startsWith("RD")) videoId else "RDAMVM$videoId"
            val url = "https://www.youtube.com/playlist?list=$playlistId"
            val songs = runCatching {
                val info = PlaylistInfo.getInfo(url)
                val all = ArrayList(info.relatedItems.mapNotNull { item ->
                    val stream = item as? StreamInfoItem ?: return@mapNotNull null
                    val id = videoIdFrom(stream.url) ?: return@mapNotNull null
                    Song(
                        id = id,
                        title = stream.name.cleanForDisplay(),
                        artist = stream.uploaderName.orEmpty().cleanForDisplay(),
                        durationSeconds = stream.duration.coerceAtLeast(0L),
                        thumbnailUrl = bestThumbnail(stream, id),
                    )
                })
                // La primera página de un mix suele traer ~100 canciones. Si la
                // radio sigue, se pide la siguiente igual que el continuation
                // de Echo (solo una página extra para no disparar la red).
                if (all.size < limit && info.hasNextPage()) {
                    runCatching {
                        val more = PlaylistInfo.getMoreItems(
                            ServiceList.YouTube, url, info.nextPage,
                        )
                        all += more.items.mapNotNull { item ->
                            val stream = item as? StreamInfoItem ?: return@mapNotNull null
                            val id = videoIdFrom(stream.url) ?: return@mapNotNull null
                            Song(
                                id = id,
                                title = stream.name.cleanForDisplay(),
                                artist = stream.uploaderName.orEmpty().cleanForDisplay(),
                                durationSeconds = stream.duration.coerceAtLeast(0L),
                                thumbnailUrl = bestThumbnail(stream, id),
                            )
                        }
                    }
                }
                all
            }.onFailure {
                Log.w(TAG, "No se pudo leer la radio de $videoId", it)
            }.getOrDefault(emptyList())
            val result = songs.distinctBy { it.id }.take(limit)
            Log.d(TAG, "Radio YouTube $playlistId: ${result.size} canciones")
            result
        }

    /** Una página de la radio de YouTube Music y el token para pedir la siguiente. */
    data class MusicRadioPage(
        val songs: List<Song> = emptyList(),
        val continuation: String? = null,
    )

    /**
     * Radio de YouTube Music (endpoint `next` del cliente web de Music). Con
     * UNA petición ligera devuelve unas 50 canciones de audio afines a la
     * semilla, sin videoclips y con paginación real.
     */
    suspend fun musicRadio(
        videoId: String,
        continuation: String? = null,
    ): MusicRadioPage = withContext(Dispatchers.IO) {
        if (!videoId.matches(VIDEO_ID_REGEX)) return@withContext MusicRadioPage()
        runCatching { MusicRadioParser.parse(musicNext(videoId, continuation)) }
            .onFailure { Log.w(TAG, "No se pudo leer la radio de Music de $videoId", it) }
            .getOrDefault(MusicRadioPage())
    }

    data class MusicMeta(val artist: String, val album: String?, val year: String?)

    /** Álbum y año oficiales de un audio de YouTube Music; null si no los tiene. */
    suspend fun musicMeta(videoId: String): MusicMeta? = withContext(Dispatchers.IO) {
        if (!videoId.matches(VIDEO_ID_REGEX)) return@withContext null
        runCatching { MusicRadioParser.meta(musicNext(videoId, null), videoId) }.getOrNull()
    }

    private fun musicNext(videoId: String, continuation: String?): JSONObject {
            NewPipeInitializer.ensureInitialized()
            val country = Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"
            val client = JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", MUSIC_CLIENT_VERSION)
                .put("hl", "es")
                .put("gl", country)
            val body = JSONObject()
                .put("context", JSONObject().put("client", client))
                .put("videoId", videoId)
                .put("playlistId", "RDAMVM$videoId")
                .put("params", "wAEB")
                .put("isAudioOnly", true)
            if (continuation != null) body.put("continuation", continuation)
            val response = NewPipe.getDownloader().post(
                "https://music.youtube.com/youtubei/v1/next?prettyPrint=false",
                mapOf(
                    "Content-Type" to listOf("application/json"),
                    "Origin" to listOf("https://music.youtube.com"),
                ),
                body.toString().toByteArray(Charsets.UTF_8),
            )
            check(response.responseCode() == 200) { "HTTP ${response.responseCode()}" }
            return JSONObject(response.responseBody())
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
        const val TAG = "YouTubeRadio"
        const val MIN_RELATED_VIEWS = 25_000L
        val VIDEO_ID_REGEX = Regex("[A-Za-z0-9_-]{11}")
        const val MUSIC_CLIENT_VERSION = "1.20250310.01.00"
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
