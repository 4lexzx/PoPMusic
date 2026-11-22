package com.popmusic.app.data.recommendations

import com.popmusic.app.data.library.LibraryState
import com.popmusic.app.data.youtube.YouTubeRepository
import android.util.Log
import com.popmusic.app.domain.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

data class TasteRecommendations(
    val songs: List<Song> = emptyList(),
)

data class DiscoverTheme(
    val title: String,
    val queries: List<String>,
)

data class TasteGenre(
    val label: String,
    val query: String,
)

val TASTE_GENRES = listOf(
    TasteGenre("Pop", "pop en espanol"),
    TasteGenre("Rock", "rock en espanol"),
    TasteGenre("Reggaetón", "reggaeton"),
    TasteGenre("Baladas", "baladas romanticas"),
    TasteGenre("Salsa", "salsa"),
    TasteGenre("Cumbia", "cumbia"),
    TasteGenre("Trap latino", "trap latino"),
    TasteGenre("Rancheras / corridos", "rancheras corridos"),
    TasteGenre("Hip hop", "hip hop espanol"),
    TasteGenre("Electrónica", "electronica dance"),
    TasteGenre("Metal", "metal clasico"),
    TasteGenre("Indie / alternativo", "indie rock"),
    TasteGenre("Pop internacional", "pop internacional"),
    TasteGenre("Bachata", "bachata"),
    TasteGenre("K-pop", "kpop"),
)

/** Temas de podcast que se pueden elegir en el onboarding y en los ajustes. */
val PODCAST_TOPICS = listOf(
    TasteGenre("Comedia", "podcast comedia"),
    TasteGenre("Tecnología", "podcast tecnologia"),
    TasteGenre("Ciencia", "podcast ciencia"),
    TasteGenre("Historia", "podcast historia"),
    TasteGenre("True crime", "podcast crimenes reales"),
    TasteGenre("Misterio", "podcast misterio"),
    TasteGenre("Negocios", "podcast negocios"),
    TasteGenre("Salud y bienestar", "podcast salud"),
    TasteGenre("Psicología", "podcast psicologia"),
    TasteGenre("Deportes", "podcast deportes"),
    TasteGenre("Cine y TV", "podcast cine series"),
    TasteGenre("Música", "podcast musica"),
    TasteGenre("Cultura", "podcast cultura"),
    TasteGenre("Noticias", "podcast noticias"),
    TasteGenre("Educación", "podcast educacion"),
    TasteGenre("Entrevistas", "podcast entrevistas"),
)

class RecommendationsRepository(
    private val youtube: YouTubeRepository = YouTubeRepository(),
) {

    /** Estado de la radio en curso: cada lote debe traer canciones nuevas. */
    private val radioSeenKeys = HashSet<String>()
    private val radioSeenIds = HashSet<String>()
    private var radioQueries = emptyList<String>()
    private var radioGenre: String? = null
    private var radioCursor = 0
    private var radioRelatedUsed = false

    fun resetRadio() {
        radioSeenKeys.clear()
        radioSeenIds.clear()
        radioQueries = emptyList()
        radioGenre = null
        radioCursor = 0
        radioRelatedUsed = false
    }

    suspend fun forTaste(
        library: LibraryState,
        tasteGenres: Set<String> = emptySet(),
        tasteArtists: List<String> = emptyList(),
    ): TasteRecommendations {
        val saved = library.favorites + library.playlists.flatMap { it.songs }
        val topArtists = saved
            .map { it.artist.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        val genreQueries = tasteGenres.mapNotNull { label ->
            TASTE_GENRES.firstOrNull { it.label == label }?.query
        }
        val queries = buildList {
            addAll(tasteArtists.shuffled().take(3).map { "$it songs" })
            addAll(genreQueries.shuffled().take(3).map { "$it exitos" })
            addAll(topArtists.shuffled().take(2).map { "$it songs" })
            addAll((FALLBACK_QUERIES + EXTRA_QUERIES).shuffled().take(3))
        }.distinct()

        val savedIds = saved.mapTo(HashSet()) { it.id }
        fun freshSongs(source: List<Song>, excluded: Set<String> = savedIds) =
            dedupeBySong(source.filterNot { it.id in excluded })

        var pool = searchAll(queries)
        if (freshSongs(pool).size < MIN_SONGS) {
            pool = (pool + searchAll(EXTRA_QUERIES.shuffled().take(3))).distinctBy { it.id }
        }

        val songs = freshSongs(pool)
            .shuffled()
            .sortedByDescending(::officialScore)
            .take(SONG_LIMIT)

        return TasteRecommendations(songs = songs)
    }

    /** Temas de secciones horizontales: gustos elegidos, luego artistas de la biblioteca, luego géneros. */
    fun discoverThemes(
        library: LibraryState,
        tasteGenres: Set<String> = emptySet(),
        tasteArtists: List<String> = emptyList(),
    ): List<DiscoverTheme> {
        val saved = library.favorites + library.playlists.flatMap { it.songs }
        val topArtists = saved
            .map { it.artist.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .distinct()
            .take(8)

        val onboardingArtistThemes = tasteArtists.map { name ->
            val clean = name.filter { it.isLetterOrDigit() || it == ' ' }.trim()
            if (clean.isBlank()) return@map null
            DiscoverTheme(
                title = "Éxitos de $clean",
                queries = listOf("$clean exitos", "$clean canciones", "$clean songs"),
            )
        }.filterNotNull().shuffled()

        val tasteGenreThemes = tasteGenres.mapNotNull { label ->
            TASTE_GENRES.firstOrNull { it.label == label }?.query
        }.flatMap { genre ->
            DISCOVER_SUFFIXES.shuffled().chunked(2).take(2).map { pair ->
                DiscoverTheme(
                    title = "${genre.replaceFirstChar { it.uppercase() }} ${pair.first()}",
                    queries = pair.map { suffix -> "$genre $suffix" },
                )
            }
        }.shuffled()

        val artistThemes = topArtists.map { name ->
            val clean = name.removeSuffix(" - Topic").trim()
            DiscoverTheme(
                title = "Éxitos de $clean",
                queries = listOf("$clean exitos", "$clean canciones", "$clean songs"),
            )
        }

        val chosenQueries = tasteGenreThemes.flatMap { it.queries }.toSet()
        val genreThemes = DISCOVER_GENRES.flatMap { genre ->
            DISCOVER_SUFFIXES.shuffled().chunked(2).map { pair ->
                DiscoverTheme(
                    title = "${genre.replaceFirstChar { it.uppercase() }} ${pair.first()}",
                    queries = pair.map { suffix -> "$genre $suffix" },
                )
            }
        }.filter { theme -> theme.queries.none { it in chosenQueries } }

        return onboardingArtistThemes + tasteGenreThemes + artistThemes + genreThemes
    }

    suspend fun discoverSongs(
        queries: List<String>,
        excludeIds: Set<String>,
        limit: Int = 8,
    ): List<Song> {
        val pool = searchAll(queries)
        return dedupeBySong(pool.filterNot { it.id in excludeIds })
            .shuffled()
            .sortedByDescending(::officialScore)
            .take(limit)
    }

    /**
     * Amplía la cola con canciones que la app ya ha mostrado: resultados de
     * búsqueda, "Para ti", tendencias, Music Flow y biblioteca. Solo pide más
     * a YouTube si ese catálogo no llega a suficientes, y entonces acota la
     * búsqueda al artista o al idioma de lo que está sonando. Se acabaron las
     * consultas genéricas que metían canciones sin relación.
     */
    suspend fun radio(
        library: LibraryState,
        excludeIds: Set<String>,
        limit: Int = RADIO_BATCH,
        seedSong: Song? = null,
        discovered: List<DiscoveredSong> = emptyList(),
    ): List<Song> {
        val saved = library.favorites + library.playlists.flatMap { it.songs }
        val savedIds = saved.mapTo(HashSet()) { it.id }

        val artist = seedSong?.artist
            ?.trim()
            ?.removeSuffix(" - Topic")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val seedTags = seedSong?.id
            ?.let { id -> discovered.firstOrNull { it.song.id == id }?.tags }
            .orEmpty()
        // Si el título no delata el idioma ("BbY WOW"), lo sacamos de las
        // etiquetas del género (reggaeton, urbano, salsa...) con el que se
        // descubrió la canción.
        val language = seedSong?.let(RecommendationText::languageOf)
            ?: if (LATIN_TAGS.any { it in seedTags } ||
                RecommendationText.likelyLatinArtist(artist.orEmpty())
            ) "es" else null

        // La misma canción suele aparecer en varios canales (VEVO, Topic,
        // subidas de fans). Se descarta por título normalizado, no solo por id,
        // y se limita el número de canciones por artista para que la radio no
        // se quede girando alrededor de la semilla.
        val seedKeys = seedSong?.let(::titleKeys).orEmpty()
        fun usable(song: Song): Boolean {
            if (song.id in excludeIds || song.id in savedIds ||
                song.id == seedSong?.id || song.id in radioSeenIds
            ) {
                return false
            }
            return titleKeys(song).none { it in seedKeys || it in radioSeenKeys }
        }

        fun languageGroup(song: Song): String {
            if (RecommendationText.hasForeignScript("${song.title} ${song.artist}")) {
                return "foreign"
            }
            return RecommendationText.languageOf(song) ?: "unknown"
        }

        // NewPipe no expone el continuation del endpoint `next` que Echo usa
        // para paginar el radio. Se fija UN género al empezar (igual que el
        // playlist RD de YouTube) y solo se rotan búsquedas dentro de ese
        // género: cambian las canciones y los cantantes, nunca el estilo.
        if (radioQueries.isEmpty()) {
            val streamTags = seedSong?.id
                ?.let { youtube.streamTags(it) }
                .orEmpty()
            val genre = pickGenre(seedTags + streamTags, artist, language)
            radioGenre = genre
            Log.d("RadioGenero", "Género fijado: $genre")
            radioQueries = buildList {
                addAll(
                    RADIO_SUFFIXES.shuffled().take(5).map { suffix -> "$genre $suffix" },
                )
                if (artist != null) add("$artist $genre")
            }.distinct().shuffled()
        }
        val batchQueries = (0 until minOf(5, radioQueries.size)).map {
            radioQueries[(radioCursor + it) % radioQueries.size]
        }
        radioCursor += batchQueries.size

        // El primer lote conserva el punto de partida de YouTube (equivalente
        // a la primera página de Echo); los siguientes traen canciones nuevas.
        val fromRelated = if (seedSong != null && !radioRelatedUsed) {
            radioRelatedUsed = true
            youtube.related(seedSong.id, limit = limit + 20)
        } else {
            emptyList()
        }
        val candidates = (fromRelated + searchAll(batchQueries))
            .filter(::isSongLike)
            .filter(::usable)
            .distinctBy { it.id }

        val same = candidates.filter { language != null && languageGroup(it) == language }
        val unknown = candidates.filter { languageGroup(it) == "unknown" }
        val other = candidates.filterNot { it in same || it in unknown }
        val ordered = when {
            language == "es" && same.size >= MIN_RADIO_RESULTS -> same.shuffled()
            same.size >= limit -> same.shuffled()
            same.size + unknown.size >= limit -> (same + unknown).shuffled()
            else -> (same + unknown + other).shuffled()
        }
        val batch = diversify(dedupeBySong(ordered)).take(limit)
        radioSeenKeys += batch.flatMap(::titleKeys)
        radioSeenIds += batch.map { it.id }
        return batch
    }

    /** Un único género para toda la radio: etiquetas del vídeo, artista o idioma. */
    private fun pickGenre(tags: Set<String>, artist: String?, language: String?): String {
        val genres = TASTE_GENRES.map { it.query } + DISCOVER_GENRES
        genres.firstOrNull { genre ->
            RecommendationText.tags(genre).any { it in tags }
        }?.let { return it }
        if (artist != null) {
            val key = RecommendationText.key(artist)
            ARTIST_GENRES.firstOrNull { key.contains(RecommendationText.key(it.first)) }
                ?.let { return it.second }
        }
        return when (language) {
            "en" -> "english pop"
            else -> "reggaeton"
        }
    }

    private suspend fun searchAll(queries: List<String>): List<Song> =
        supervisorScope {
            queries
                .map { query ->
                    async {
                        runCatching { youtube.search(query) }.getOrDefault(emptyList())
                    }
                }
                .awaitAll()
        }
            .flatten()
            .filter(::isSongLike)
            .distinctBy { it.id }

    private fun isSongLike(song: Song): Boolean {
        val title = song.title.lowercase()
        if (MIX_REGEX.containsMatchIn(title)) return false
        if (BAD_TITLE_WORDS.any { title.contains(it) }) return false
        val duration = song.durationSeconds
        return duration == 0L || duration in MIN_DURATION..MAX_DURATION
    }

    private fun officialScore(song: Song): Int {
        val title = song.title.lowercase()
        var score = 0
        if ("official" in title) score += 2
        if ("audio" in title) score += 4
        if ("video" in title) score -= 2
        if (" - " in song.title) score += 1
        return score
    }

    /**
     * Variantes del título de una canción. YouTube entrega la misma canción
     * como "Artista - Título", "Título (Official Video)", "Título [Audio]",
     * etc.; todas deben dar la misma clave para no repetirla en la radio.
     */
    private fun titleKeys(song: Song): Set<String> {
        var bare = song.title.lowercase()
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), " ")
            .replace(Regex("(?i)\\b(feat|featuring|ft)\\.?\\b.*$"), " ")
        // YouTube añade sufijos como "| Letra" o "// Lyrics": cortar ahí evita
        // que la misma canción parezca distinta según el canal que la subió.
        bare = bare.split(Regex("\\s*[|·]|\\s+//\\s*")).first().trim()
        val clean = RecommendationText.key(bare)
        val artistKey = RecommendationText.key(song.artist)
        val keys = mutableSetOf(clean)
        if (artistKey.length >= 2) {
            if (clean.startsWith(artistKey)) keys += clean.removePrefix(artistKey).trim()
            if (clean.endsWith(artistKey)) keys += clean.removeSuffix(artistKey).trim()
        }
        // "Artista - Título", "Artista1, Artista2 - Título - Lyrics": las
        // partes a la derecha del guion son el título real aunque el canal sea
        // otro. Se ignoran las etiquetas genéricas de las subidas de fans.
        val parts = bare.split(Regex("\\s+[-–—]\\s+"))
            .map(RecommendationText::key)
            .filter { it.length >= 3 && it !in TITLE_JUNK }
        if (parts.size >= 2) {
            keys += parts.drop(1)
            // "coqueta letra" y "coqueta" deben dar la misma clave.
            val core = RecommendationText.key(
                parts.drop(1).joinToString(" ").split(' ')
                    .filterNot { it in TITLE_JUNK }
                    .joinToString(" "),
            )
            if (core.length >= 3) keys += core
        }
        return keys.filter { it.length >= 3 }.toSet()
    }

    /** Conserva el orden de la radio, pero evita repetir título ni id. */
    private fun dedupeBySong(songs: List<Song>): List<Song> {
        val seenIds = HashSet<String>()
        val seenTitles = HashSet<String>()
        return songs.filter { song ->
            val keys = titleKeys(song)
            if (song.id in seenIds || keys.any { it in seenTitles }) {
                false
            } else {
                seenIds += song.id
                seenTitles += keys
                true
            }
        }
    }

    /** como máximo MAX_PER_ARTIST seguidas del mismo artista en un lote. */
    private fun diversify(songs: List<Song>): List<Song> {
        val counts = HashMap<String, Int>()
        val picked = ArrayList<Song>(songs.size)
        for (song in songs) {
            val artistKey = RecommendationText.key(song.artist)
            val count = counts[artistKey] ?: 0
            if (count < MAX_PER_ARTIST) {
                picked += song
                counts[artistKey] = count + 1
            }
        }
        return picked
    }

    private companion object {
        const val SONG_LIMIT = 20
        const val RADIO_BATCH = 20
        const val MAX_PER_ARTIST = 3
        val LATIN_TAGS = setOf(
            "reggaeton", "regueton", "urbano", "urbana", "latin", "latino",
            "latina", "espanol", "español", "cumbia", "salsa", "bachata",
            "merengue", "mexicano", "colombiano", "argentino", "peruano",
            "chileno", "venezolano",
        )
        val TITLE_JUNK = setOf(
            "lyrics", "letras", "letra", "video", "oficial", "official",
            "audio", "hd", "hq", "4k", "live", "en vivo", "subtitulado",
        )
        const val MIN_RADIO_RESULTS = 4
        const val MIN_SONGS = 8
        const val MIN_DURATION = 45L
        const val MAX_DURATION = 600L
        val MIX_REGEX = Regex("\\bmix\\b")
        val BAD_TITLE_WORDS = listOf(
            "megamix",
            "letra (",
            "lyrics video",
            "recopilaci",
            "playlist",
            "nonstop",
            "non-stop",
            "compilation",
            "grandes exitos",
            "grandes éxitos",
            "mejores canciones",
            "lo mejor de",
            "las mejores",
            "seleccion",
            "selección",
            "1 hora",
            "horas de",
            "old school",
            "engañchado",
            "enganchado",
            "top hits",
            "top 10",
            "top 20",
            "top 50",
            "album completo",
            "disco completo",
            "intro",
        )
        val RADIO_SUFFIXES = listOf(
            "exitos", "canciones", "2024", "clasico", "actual",
            "mas escuchadas", "top", "nuevas canciones",
        )
        val ARTIST_GENRES = listOf(
            "karol g" to "reggaeton",
            "bad bunny" to "reggaeton",
            "j balvin" to "reggaeton",
            "ozuna" to "reggaeton",
            "daddy yankee" to "reggaeton",
            "anuel aa" to "reggaeton",
            "floyy menor" to "reggaeton",
            "danny ocean" to "reggaeton",
            "maluma" to "reggaeton",
            "shakira" to "latin pop",
            "enrique iglesias" to "latin pop",
            "luis miguel" to "baladas romanticas",
            "maná" to "rock en espanol",
            "mana" to "rock en espanol",
            "la oreja de van gogh" to "pop en espanol",
            "cristian castro" to "baladas romanticas",
            "alejandro sanz" to "baladas romanticas",
            "willie colon" to "salsa",
            "oscar d'leon" to "salsa",
        )
        val FALLBACK_QUERIES = listOf(
            "pop en espanol exitos",
            "rock en espanol exitos",
            "reggaeton exitos",
            "latin pop exitos",
        )
        val EXTRA_QUERIES = listOf(
            "pop actual canciones",
            "indie pop canciones",
            "rock alternativo canciones",
            "musica urbana canciones",
            "pop rock clasicos",
            "baladas pop canciones",
            "pop 2000 exitos",
            "rock clasico canciones",
        )
        val DISCOVER_GENRES = listOf(
            "cumbia",
            "salsa",
            "banda sinaloense",
            "rock 90",
            "hip hop espanol",
            "trap latino",
            "baladas romanticas",
            "electronica latina",
            "indie rock",
            "pop mexicano",
            "reggae en espanol",
            "metal clasico",
            "musica de los 80",
            "musica de los 2010",
            "punk espanol",
            "pop internacional",
            "reggaeton clasico",
            "latin trap",
            "rock en ingles 2000",
            "pop punk",
            "funk",
            "soul y r&b en espanol",
            "rancheras",
            "reggaeton actual",
            "pop actual",
            "rock alternativo",
            "musica urbana",
            "baladas pop",
            "pop 2000",
            "rock clasico",
            "pop rock",
            "dance en espanol",
            "kizomba y kuduro",
            "merengue",
            "bachata",
            "cumbia villera",
        )
        val DISCOVER_SUFFIXES = listOf(
            "exitos",
            "canciones",
            "hits",
            "2024",
            "2023",
            "nuevas canciones",
            "top",
            "mas escuchadas",
        )
    }
}
