package com.popmusic.app.data.charts

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.popmusic.app.data.filter.ContentFilters
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Una pista del chart de Spotify: título, artistas y duración en milisegundos. */
data class ChartTrack(
    val title: String,
    val artists: String,
    val durationMs: Long,
)

/** Playlist pública de Spotify leída desde su embed (nombre + pistas). */
data class SpotifyPlaylist(
    val name: String,
    val tracks: List<ChartTrack>,
)

/**
 * Ranking de tendencias de la tanda 6 (punto 1): listas DIARIAS oficiales de
 * Spotify Charts, leídas desde el embed público de `open.spotify.com` (sin
 * credenciales ni login) y resueltas a YouTube para poder reproducirlas.
 *
 * - Fuente: `Top 50 - Global` + `Top 50 - {país del dispositivo}` (solo si el
 *   país está en [LOCAL_CHARTS]); lo local que no esté ya en el global se
 *   añade por detrás.
 * - Sincronización: caché local de 24 h en SharedPreferences `charts_cache`.
 * - Filtro estricto: cada candidato de YouTube pasa por
 *   [ContentFilters.musicOnly] (descarta infantiles, humor, IA, vídeos…).
 * - También resuelve playlists de Spotify para el import de la biblioteca
 *   (punto 3: "importar con URL: Spotify y YouTube").
 */
class ChartsRepository(
    context: Context,
    private val youTube: YouTubeRepository = YouTubeRepository(),
) {

    private val appContext = context.applicationContext
    private val cache = appContext.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    /** Chart listo para mostrar: pistas del chart resueltas a canciones. */
    suspend fun topSongs(): List<Song> {
        cached()?.let { cached ->
            // Precarga las fotos en Coil: si vuelven a las 24 h siguen listas.
            cached.forEach { warmThumbnail(it.thumbnailUrl) }
            return cached
        }
        // Un solo proceso de carga a la vez: si Home y Buscar lo piden juntos,
        // el segundo espera y luego lee la caché en vez de repetir 70 búsquedas.
        return fetchMutex.withLock {
            cached()?.let { return@withLock it }
            withContext(Dispatchers.IO) {
                val songs = resolve(chartTracks())
                if (songs.isNotEmpty()) saveCache(songs)
                songs
            }
        }
    }

    /** true si el enlace apunta a una playlist de Spotify (cualquier formato). */
    fun spotifyPlaylistId(link: String): String? {
        val raw = SPOTIFY_PLAYLIST.find(link)?.groupValues?.get(1) ?: return null
        return raw.takeIf { it.isNotBlank() }
    }

    /**
     * Importa una playlist de Spotify por enlace (punto 3): lee el embed
     * público y resuelve cada pista a YouTube. Devuelve null si no es un
     * enlace de Spotify o si no se pudo resolver nada.
     */
    suspend fun importSpotifyPlaylist(link: String): Playlist? = withContext(Dispatchers.IO) {
        val id = spotifyPlaylistId(link) ?: return@withContext null
        val playlist = fetchPlaylist(id) ?: return@withContext null
        val songs = resolve(playlist.tracks)
        if (songs.isEmpty()) {
            null
        } else {
            Playlist(
                id = UUID.randomUUID().toString(),
                name = playlist.name,
                songs = songs,
            )
        }
    }

    // ---------------------------------------------------------------- chart

    /** Global primero; después las pistas locales que no estén en el global. */
    private fun chartTracks(): List<ChartTrack> {
        val global = fetchPlaylist(GLOBAL_CHART)?.tracks.orEmpty()
        val localId = LOCAL_CHARTS[deviceCountry()]
        val local = localId?.let { fetchPlaylist(it)?.tracks }.orEmpty()

        val seen = HashSet<String>()
        val merged = ArrayList<ChartTrack>(global.size + local.size)
        (global + local).forEach { track ->
            if (seen.add(track.dedupeKey())) merged.add(track)
        }
        return merged.take(MAX_TRACKS)
    }

    private fun deviceCountry(): String =
        runCatching {
            Locale.getDefault().country?.uppercase(Locale.ROOT).orEmpty()
        }.getOrDefault("")

    /**
     * Lee el embed público de Spotify (`__NEXT_DATA__` con el JSON de la
     * playlist). Devuelve null si Spotify cambia de formato o el id no existe.
     */
    private fun fetchPlaylist(id: String): SpotifyPlaylist? {
        val html = get("$EMBED_BASE$id") ?: return null
        val payload = NEXT_DATA.find(html)?.groupValues?.get(1) ?: return null
        return runCatching {
            val entity = JSONObject(payload)
                .getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("state")
                .getJSONObject("data")
                .getJSONObject("entity")
            val name = entity.optString("name")
                .ifBlank { entity.optString("title") }
                .ifBlank { "Playlist importada" }
            val list = entity.optJSONArray("trackList") ?: JSONArray()
            SpotifyPlaylist(name = name, tracks = tracksOf(list))
        }.getOrNull()
    }

    private fun tracksOf(list: JSONArray): List<ChartTrack> = buildList {
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            val title = item.optString("title")
            if (title.isBlank()) continue
            add(
                ChartTrack(
                    title = title,
                    artists = item.optString("subtitle"),
                    durationMs = item.optLong("duration"),
                ),
            )
        }
    }

    // ------------------------------------------------------------ resolución

    /**
     * Convierte las pistas del chart en canciones reproducibles: búsqueda en
     * YouTube en paralelo (6 a la vez), filtro estricto de música y ajuste de
     * duración contra el chart para descartar versiones raras.
     */
    private suspend fun resolve(tracks: List<ChartTrack>): List<Song> = coroutineScope {
        if (tracks.isEmpty()) return@coroutineScope emptyList()
        val semaphore = Semaphore(6)
        tracks.map { track ->
            async {
                semaphore.withPermit { bestMatch(track) }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun bestMatch(track: ChartTrack): Song? {
        val candidates = runCatching { youTube.search("${track.title} ${track.artists}") }
            .getOrDefault(emptyList())
            .filter { it.durationSeconds >= MIN_DURATION_S }
            .let { ContentFilters.musicOnly(it) }
        if (candidates.isEmpty()) return null

        val expected = track.durationMs / 1000L
        val close = if (expected > 0L) {
            candidates.filter { abs(it.durationSeconds - expected) <= DURATION_TOLERANCE_S }
        } else {
            emptyList()
        }
        val chosen = close.firstOrNull() ?: candidates.firstOrNull()
        // Descarga su foto YA, mientras se resuelven las demás pistas: cuando
        // la sección de Tendencias se pinta, la miniatura está en caché (punto 1).
        warmThumbnail(chosen?.thumbnailUrl)
        return chosen
    }

    /**
     * Precarga una miniatura en Coil (memoria + disco) sin bloquear la
     * publicación del chart.
     */
    private fun warmThumbnail(url: String?) {
        if (url.isNullOrBlank()) return
        thumbScope.launch {
            runCatching {
                val request = ImageRequest.Builder(appContext)
                    .data(url)
                    .size(THUMB_WARM_PX, THUMB_WARM_PX)
                    .build()
                SingletonImageLoader.get(appContext).execute(request)
            }
        }
    }

    // ---------------------------------------------------------------- caché

    private fun cached(): List<Song>? {
        val raw = cache.getString(CACHE_KEY, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val savedAt = root.optLong(KEY_SAVED_AT)
            if (System.currentTimeMillis() - savedAt >= TTL_MS) return@runCatching null
            decodeSongs(root.optJSONArray(KEY_SONGS) ?: JSONArray())
        }.getOrNull()
    }

    private fun saveCache(songs: List<Song>) {
        val root = JSONObject()
        root.put(KEY_SAVED_AT, System.currentTimeMillis())
        root.put(KEY_SONGS, JSONArray().apply {
            songs.forEach { song ->
                put(JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("durationSeconds", song.durationSeconds)
                    if (song.thumbnailUrl != null) put("thumbnail", song.thumbnailUrl)
                })
            }
        })
        cache.edit().putString(CACHE_KEY, root.toString()).apply()
    }

    private fun decodeSongs(array: JSONArray): List<Song> = buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val title = item.optString("title")
            if (id.isBlank() || title.isBlank()) continue
            add(
                Song(
                    id = id,
                    title = title,
                    artist = item.optString("artist"),
                    durationSeconds = item.optLong("durationSeconds"),
                    thumbnailUrl = if (item.has("thumbnail")) item.optString("thumbnail") else null,
                ),
            )
        }
    }

    // ----------------------------------------------------------------- http

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en,es;q=0.8")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string()
        }
    }

    private fun ChartTrack.dedupeKey(): String =
        "${title.trim().lowercase()}|${artists.trim().lowercase()}"

    private companion object {
        /** Evita cargas simultáneas del chart desde varios ViewModels. */
        val fetchMutex = Mutex()

        /** Precarga de miniaturas: no bloquea la publicación del chart. */
        val thumbScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /** Tamaño de la foto pre-cargada (las listas se pintan a ese ancho). */
        const val THUMB_WARM_PX = 360

        const val EMBED_BASE = "https://open.spotify.com/embed/playlist/"
        val NEXT_DATA = Regex(
            """<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val SPOTIFY_PLAYLIST = Regex(
            """spotify\.com/(?:intl-[a-z-]+/|user/[\w.-]+/)?playlist/([A-Za-z0-9]+)""",
        )

        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Safari/537.36"

        /** Top 50 - Global (lista diaria oficial de Spotify Charts). */
        const val GLOBAL_CHART = "37i9dQZEVXbMDoHDwVN2tF"

        /** Top 50 diarios por país (sin patrón derivable: ids oficiales). */
        val LOCAL_CHARTS = mapOf(
            "PE" to "37i9dQZEVXbJfdy5b0KP7W",
            "MX" to "37i9dQZEVXbO3qyFxbkOE1",
            "CO" to "37i9dQZEVXbOa2lmxNORXQ",
            "CL" to "37i9dQZEVXbL0GavIqMTeb",
            "EC" to "37i9dQZEVXbJlM6nvL1nD1",
            "AR" to "37i9dQZEVXbMMy2roB9myp",
            "US" to "37i9dQZEVXbLRQDuF5jeBp",
            "ES" to "37i9dQZEVXbNFJfN1Vw8d9",
            "FR" to "37i9dQZEVXbIPWwFssbupI",
            "DE" to "37i9dQZEVXbJiZcmkrIHGU",
        )

        const val CACHE_NAME = "charts_cache"
        const val CACHE_KEY = "last_chart"
        const val KEY_SAVED_AT = "savedAt"
        const val KEY_SONGS = "songs"

        /** Sincronización del chart: una vez cada 24 h. */
        const val TTL_MS = 24L * 60L * 60L * 1000L
        const val MAX_TRACKS = 70
        const val MIN_DURATION_S = 45L
        const val DURATION_TOLERANCE_S = 15L
    }
}
