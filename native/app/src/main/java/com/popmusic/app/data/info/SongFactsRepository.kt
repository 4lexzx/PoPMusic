package com.popmusic.app.data.info

import android.content.Context
import android.util.Log
import com.popmusic.app.data.lyrics.LyricsMatcher
import com.popmusic.app.data.recommendations.RecommendationText
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Ficha de una canción. Todo lo que no se pudo comprobar va a null. */
data class SongFacts(
    /** null = artista desconocido. */
    val artist: String? = null,
    val artistPhoto: String? = null,
    val bio: String? = null,
    /** Quien subió el vídeo, cuando no es el artista. */
    val uploader: String? = null,
    val title: String,
    val album: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val track: String? = null,
    val story: String? = null,
    val sources: List<String> = emptyList(),
)

/**
 * Datos verificados de una canción: iTunes (álbum, año, género) y Wikipedia
 * (artista e historia). Solo se acepta un dato si coinciden artista Y título;
 * las subidas de terceros sin coincidencia quedan como artista desconocido.
 */
class SongFactsRepository internal constructor(
    private val photoOf: suspend (String) -> String?,
) {
    constructor(context: Context) : this({ name -> ArtistPhotoStore.photo(context, name) })


    private val youtube = YouTubeRepository()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private data class Track(
        val artist: String,
        val title: String,
        val album: String?,
        val year: String?,
        val genre: String?,
        val number: Int,
        val count: Int,
    )

    /** null = no hubo conexión: no es lo mismo que "sin información". */
    suspend fun facts(song: Song): SongFacts? = withContext(Dispatchers.IO) {
        cache[song.id]?.let { return@withContext it }
        val channel = song.artist.trim().removeSuffix(" - Topic").trim()

        var online = false
        fun lookup(artist: String, title: String, strictDuration: Boolean): Track? {
            val found = itunes(artist, title) ?: return null
            online = true
            return pick(found, artist, title, song.durationSeconds, strictDuration)
        }

        // 1. El canal es el artista.
        var track = lookup(channel, song.title, strictDuration = false)
        // 2. "Artista - Título" subido por un tercero: se exige la misma duración.
        if (track == null) {
            val parts = song.title.split(Regex("\\s+[-–—]\\s+"), limit = 2)
            if (parts.size == 2 && key(parts[0]) != key(channel)) {
                track = lookup(parts[0].trim(), parts[1].trim(), strictDuration = true)
            }
        }
        // Ficha oficial del propio audio: completa y contrasta a iTunes.
        val meta = runCatching { youtube.musicMeta(song.id) }.getOrNull()
        if (meta != null) online = true
        val photo = (track?.artist ?: meta?.artist ?: channel).takeIf { it.isNotBlank() }
            ?.let { runCatching { photoOf(it) }.getOrNull() }
        // Con perfil de artista en YouTube Music también se da por conocido.
        val artist = track?.artist ?: meta?.artist?.takeIf { it.isNotBlank() } ?: channel.takeIf { photo != null && it.isNotBlank() }
        if (!online && photo == null) return@withContext null

        val (bio, story) = if (artist == null) {
            null to null
        } else {
            coroutineScope {
                val bioJob = async { biography(artist) }
                val storyJob = async { if (track != null || meta != null) songStory(track?.title ?: song.title, artist) else null }
                bioJob.await() to storyJob.await()
            }
        }
        SongFacts(
            artist = artist,
            artistPhoto = photo.takeIf { artist != null },
            bio = bio,
            uploader = channel.takeIf { it.isNotBlank() && (artist == null || key(it) != key(artist)) },
            title = track?.title ?: song.title,
            album = track?.album ?: meta?.album
                ?.removeSuffix(" - Single")
                ?.takeIf { it.isNotBlank() && key(bare(it)) != key(bare(song.title)) },
            year = releaseYear(track, meta),
            genre = track?.genre,
            track = track?.takeIf { it.number > 0 && it.count > 1 }?.let { "${it.number} de ${it.count}" },
            story = story,
            sources = buildList {
                if (track != null) add("iTunes")
                if (bio != null || story != null) add("Wikipedia")
                if (meta != null || (photo != null && artist != null)) add("YouTube Music")
            },
        ).also { if (online) cache[song.id] = it }
    }

    /**
     * iTunes guarda la fecha original, pero a veces está mal. Si contradice al
     * audio oficial y este no es una reedición, manda el audio oficial.
     */
    private fun releaseYear(track: Track?, meta: YouTubeRepository.MusicMeta?): String? {
        val fromMeta = meta?.year
        val fromTrack = track?.year ?: return fromMeta
        if (fromMeta == null) return fromTrack
        val reissue = REISSUE.containsMatchIn("${meta.album} ${track.title}".lowercase()) ||
            COMPILATION.containsMatchIn(meta.album.orEmpty().lowercase())
        val apart = abs(fromTrack.toInt() - fromMeta.toInt())
        // Solo se corrige si hablan del MISMO disco: un recopilatorio de 2024 no
        // convierte en nueva una canción de 1994.
        val sameRelease = key(meta.album.orEmpty()) == key(track.album.orEmpty())
        return if (apart > 1 && sameRelease && !reissue) fromMeta else minOf(fromTrack, fromMeta)
    }

    // ————————————————— iTunes —————————————————

    private fun itunes(artist: String, title: String): List<JSONObject>? {
        if (artist.isBlank() || title.isBlank()) return emptyList()
        val term = URLEncoder.encode("${bare(title)} $artist", "UTF-8")
        val country = Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"
        val body = get(
            "https://itunes.apple.com/search?term=$term&media=music&entity=song&limit=30&country=$country",
        ) ?: return null
        return runCatching {
            val results = JSONObject(body).getJSONArray("results")
            (0 until results.length()).mapNotNull { results.optJSONObject(it) }
        }.getOrNull()
    }

    private fun pick(
        results: List<JSONObject>,
        artist: String,
        title: String,
        seconds: Long,
        strictDuration: Boolean,
    ): Track? {
        val version = LyricsMatcher.versionTokens(title)
        val same = results.filter { item ->
            val found = item.optString("trackName")
            sameArtist(item.optString("artistName"), artist) &&
                key(bare(found)) == key(bare(title)) &&
                LyricsMatcher.versionTokens(found) == version
        }
        val timed = same.filter {
            seconds <= 0L || abs(it.optLong("trackTimeMillis") / 1000 - seconds) <= DURATION_MARGIN
        }
        val usable = timed.ifEmpty { if (strictDuration) emptyList() else same }
        // La edición más antigua es la original; recopilatorios y reediciones, después.
        val first = usable.minByOrNull { it.optString("releaseDate").ifBlank { "9999" } } ?: return null
        // El álbum y la pista, del disco del propio artista: nunca de un recopilatorio.
        val own = usable.filterNot(::isCompilation)
            .minByOrNull { it.optString("releaseDate").ifBlank { "9999" } }
        val best = own ?: first
        val album = own?.optString("collectionName").orEmpty().trim()
            .removeSuffix(" - Single").removeSuffix(" - EP")
            .replace(EDITION, "").trim()
        return Track(
            // "Morat & Juanes" buscando a Morat: el artista principal es Morat.
            artist = best.optString("artistName").trim()
                .takeIf { key(it) == key(artist) } ?: artist.trim(),
            title = best.optString("trackName").trim(),
            album = album.takeIf { it.isNotBlank() && key(it) != key(bare(title)) },
            year = first.optString("releaseDate").take(4).takeIf { it.matches(Regex("(19|20)\\d{2}")) },
            genre = best.optString("primaryGenreName").trim().takeIf { it.isNotBlank() },
            number = if (own != null) own.optInt("trackNumber") else 0,
            count = if (own != null) own.optInt("trackCount") else 0,
        )
    }

    private fun isCompilation(item: JSONObject): Boolean {
        val owner = item.optString("collectionArtistName")
        if (owner.isNotBlank() && !sameArtist(owner, item.optString("artistName"))) return true
        return COMPILATION.containsMatchIn(item.optString("collectionName").lowercase())
    }

    /** Mismo artista: igual, o el principal de una colaboración. */
    private fun sameArtist(found: String, wanted: String): Boolean {
        val w = key(wanted)
        if (w.isBlank()) return false
        if (key(found) == w) return true
        val foundParts = found.split(Regex("\\s*[,&]\\s*|\\s+(?:x|y|feat\\.?)\\s+")).map(::key)
        val wantedParts = wanted.split(Regex("\\s*[,&]\\s*|\\s+(?:x|y|feat\\.?)\\s+")).map(::key)
        return foundParts.firstOrNull() == w || wantedParts.firstOrNull() == key(found)
    }

    // ————————————————— Wikipedia —————————————————

    private fun biography(artist: String): String? {
        val wanted = key(artist)
        val attempts = listOf("", " (cantante)", " (banda)", " (grupo musical)", " (músico)")
            .map { "es" to artist + it } +
            listOf(" (singer)", " (band)", " (rapper)", "").map { "en" to artist + it }
        for ((lang, title) in attempts) {
            val page = summary(lang, title) ?: continue
            val extract = page.optString("extract").trim()
            val intro = "${page.optString("description")} ${extract.take(260)}".lowercase()
            val named = key(page.optString("title").substringBefore(" (")) == wanted ||
                key(extract.take(220)).contains(wanted)
            if (named && MUSICIAN.containsMatchIn(intro)) return shorten(extract)
        }
        return null
    }

    private fun songStory(title: String, artist: String): String? {
        val name = bare(title)
        val attempts = listOf(
            "es" to "$name (canción)",
            "es" to "$name (canción de $artist)",
            "es" to name,
            "en" to "$name ($artist song)",
            "en" to "$name (song)",
        )
        for ((lang, pageTitle) in attempts) {
            val page = summary(lang, pageTitle) ?: continue
            val extract = page.optString("extract").trim()
            val intro = "${page.optString("description")} ${extract.take(320)}".lowercase()
            // Tiene que ser una página de canción Y nombrar a este artista.
            if (SONG.containsMatchIn(intro) && key(extract).contains(key(artist)) &&
                key(page.optString("title").substringBefore(" (")) == key(name)
            ) {
                return extract
            }
        }
        return null
    }

    private fun summary(lang: String, title: String): JSONObject? {
        val path = URLEncoder.encode(title.trim().replace(' ', '_'), "UTF-8")
        val body = get("https://$lang.wikipedia.org/api/rest_v1/page/summary/$path") ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
            ?.takeIf { it.optString("type") == "standard" && it.optString("extract").isNotBlank() }
    }

    /** Dos o tres frases: una descripción breve. */
    private fun shorten(text: String): String {
        if (text.length <= MAX_BIO) return text
        val cut = text.take(MAX_BIO)
        val end = cut.lastIndexOf(". ")
        return if (end > 120) cut.take(end + 1) else cut.substringBeforeLast(' ') + "…"
    }

    private fun get(url: String): String? = runCatching {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body.string() else null
        }
    }.onFailure { Log.w(TAG, "GET $url", it) }.getOrNull()

    private fun bare(title: String): String = title
        .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), " ")
        .replace(Regex("(?i)\\s+(feat|ft)\\.?\\s.*$"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { title.trim() }

    private fun key(value: String): String =
        RecommendationText.key(value).ifBlank { value.trim().lowercase() }

    private companion object {
        const val TAG = "SongFacts"
        const val DURATION_MARGIN = 6L
        const val MAX_BIO = 420
        const val USER_AGENT = "PoPMusic/0.1 (https://github.com/4lexzx/PoPMusic)"
        val cache = ConcurrentHashMap<String, SongFacts>()
        val MUSICIAN = Regex(
            "cantante|cantautor|músic|rapero|rapera|banda|grupo|dúo|orquesta|compositor|productor|" +
                "\\bdj\\b|singer|musician|rapper|\\bband\\b|\\bduo\\b|songwriter|record producer|girl group|boy band",
        )
        /** "(Deluxe Box Set)", "[Remastered]"...: es el mismo álbum. */
        val EDITION = Regex("(?i)\\s*[(\\[][^)\\]]*(deluxe|box set|edition|edici[oó]n|remaster)[^)\\]]*[)\\]]")
        val REISSUE = Regex("remaster|deluxe|edici[oó]n|edition|anivers|anniversary|reissue|\\blive\\b|en vivo")
        val COMPILATION = Regex(
            "cl[aá]sic[ao]s|best of|greatest|grandes? [eé]xitos|\b[eé]xitos\b|\bhits\b|colecci[oó]n|collection|" +
                "lo mejor|antolog|anthology|recopila|essential|soundtrack|banda sonora|karaoke|tribut|now that",
        )
        val SONG = Regex("canci[oó]n|sencillo|\\btema\\b|\\bsong\\b|\\bsingle\\b")
    }
}
