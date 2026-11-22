package com.popmusic.app.data.recommendations

import com.popmusic.app.data.library.LibraryState
import com.popmusic.app.data.youtube.YouTubeRepository
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
        fun freshSongs(source: List<Song>, excluded: Set<String> = savedIds) = source
            .filterNot { it.id in excluded }
            .groupBy(::baseTitle)
            .map { (_, group) -> group.maxByOrNull(::officialScore) ?: group.first() }

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
        return pool
            .filterNot { it.id in excludeIds }
            .groupBy(::baseTitle)
            .map { (_, group) -> group.maxByOrNull(::officialScore) ?: group.first() }
            .shuffled()
            .sortedByDescending(::officialScore)
            .take(limit)
    }

    /**
     * Amplía la cola. Si se pasa la canción que está sonando, la radio se
     * queda con canciones DE ESE ARTISTA: antes se rellenaba con búsquedas
     * genéricas y saltaban temas que no tenían nada que ver.
     */
    suspend fun radio(
        library: LibraryState,
        excludeIds: Set<String>,
        limit: Int = RADIO_BATCH,
        seedArtist: String? = null,
    ): List<Song> {
        val saved = library.favorites + library.playlists.flatMap { it.songs }
        val topArtists = saved
            .map { it.artist.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(4)

        val artist = seedArtist
            ?.trim()
            ?.removeSuffix(" - Topic")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val artistKey = artist?.let(::artistKey).orEmpty()

        val queries = buildList {
            if (artist != null) {
                add("$artist songs")
                add("$artist exitos")
                add("$artist canciones")
            }
            addAll(topArtists.shuffled().take(2).map { "$it songs" })
            addAll(
                (FALLBACK_QUERIES + EXTRA_QUERIES)
                    .shuffled()
                    .take(if (artist == null) 5 else 2),
            )
        }.distinct()

        val savedIds = saved.mapTo(HashSet()) { it.id }
        val pool = (searchAll(queries) + searchAll(EXTRA_QUERIES.shuffled().take(2)))
            .distinctBy { it.id }

        val candidates = pool
            .filterNot { it.id in excludeIds || it.id in savedIds }
            .groupBy(::baseTitle)
            .map { (_, group) -> group.maxByOrNull(::officialScore) ?: group.first() }

        if (artistKey.isBlank()) {
            return candidates.shuffled().sortedByDescending(::officialScore).take(limit)
        }

        // Con semilla solo entran canciones del artista. Si YouTube no devuelve
        // suficientes, se añaden menos a la cola; mejor eso que meter canciones
        // de otros géneros solo para llegar a 20.
        val related = candidates
            .filter { matchesArtist(it, artistKey) }
            .shuffled()
            .sortedByDescending(::officialScore)
        if (related.isNotEmpty()) return related.take(limit)

        // Último recurso: no encontramos nada del artista (red caída, nombre
        // raro). Ahí sí se mantiene el comportamiento anterior.
        return candidates.shuffled().sortedByDescending(::officialScore).take(limit)
    }

    /** Compara artistas ignorando acentos, "Topic", "VEVO" y puntuación. */
    private fun artistKey(name: String): String = java.text.Normalizer
        .normalize(name.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace(Regex("\\b(official|topic|vevo|audio|music)\\b"), " ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun matchesArtist(song: Song, key: String): Boolean {
        if (key.isBlank()) return false
        val songKey = artistKey(song.artist)
        if (songKey == key) return true
        if (key.length >= 3 && songKey.length >= 3 &&
            (songKey.contains(key) || key.contains(songKey))
        ) {
            return true
        }
        // Colaboraciones donde el artista aparece en el título.
        return key.length >= 3 &&
            song.title.lowercase().replace(Regex("[^a-z0-9]+"), " ").contains(key)
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

    private fun baseTitle(song: Song): String {
        val normalized = song.title.lowercase()
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), " ")
            .replace(
                Regex(
                    "\\b(official\\s+)?(music\\s+)?(video|audio|lyric\\s+video|visualizer|remastered|hd|hq|4k)\\b",
                ),
                " ",
            )
            .replace(Regex("\\s+"), " ")
            .trim()
        return "$normalized|${song.artist.lowercase()}"
    }

    private companion object {
        const val SONG_LIMIT = 20
        const val RADIO_BATCH = 20
        const val MIN_SONGS = 8
        const val MIN_DURATION = 45L
        const val MAX_DURATION = 600L
        val MIX_REGEX = Regex("\\bmix\\b")
        val BAD_TITLE_WORDS = listOf(
            "megamix",
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
