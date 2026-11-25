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
    /** Ids a los que seguir el "up next", como la paginación de Echo. */
    private val radioChain = ArrayList<String>()
    private var radioChainCursor = 0
    /** Playlist RDAMVM de YouTube ya descargada: la radio real, como en Echo. */
    private var radioPool = emptyList<Song>()
    private var radioPoolSeed: String? = null
    /** Radio de YouTube Music: fuente principal, ya filtrada por afinidad. */
    private val musicPool = LinkedHashMap<String, Song>()
    private var musicSeed: String? = null
    private var musicRadioId: String? = null
    private var musicContinuation: String? = null
    private var musicReseedCursor = 0
    private var musicLanguage: String? = null

    fun resetRadio() {
        radioSeenKeys.clear()
        radioSeenIds.clear()
        radioQueries = emptyList()
        radioGenre = null
        radioCursor = 0
        radioChain.clear()
        radioChainCursor = 0
        radioPool = emptyList()
        radioPoolSeed = null
        musicPool.clear()
        musicSeed = null
        musicRadioId = null
        musicContinuation = null
        musicReseedCursor = 0
        musicLanguage = null
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

    /**
     * Mezcla para proponer en una sala: SOLO los gustos de quien la pide (sus
     * artistas, sus géneros y canciones afines). Sin gustos configurados se usan
     * los artistas de su biblioteca, y las búsquedas genéricas como último recurso.
     */
    suspend fun tasteMix(
        library: LibraryState,
        tasteGenres: Set<String>,
        tasteArtists: List<String>,
        excludeIds: Set<String> = emptySet(),
        limit: Int = 30,
    ): List<Song> {
        val ownArtists = (library.favorites + library.playlists.flatMap { it.songs })
            .map { it.artist.trim().removeSuffix(" - Topic") }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(6)
        val genres = tasteGenres.mapNotNull { label ->
            TASTE_GENRES.firstOrNull { it.label == label }?.query
        }
        val queries = buildList {
            addAll(tasteArtists.shuffled().take(4).map { "$it canciones" })
            addAll(genres.shuffled().take(4).map { "$it ${RADIO_SUFFIXES.random()}" })
            if (size < 4) addAll(ownArtists.shuffled().take(4).map { "$it canciones" })
            if (isEmpty()) addAll(FALLBACK_QUERIES)
        }.distinct()

        val lists = supervisorScope {
            queries.map { query ->
                async {
                    val songs = runCatching { youtube.search(query) }
                        .getOrDefault(emptyList())
                        .filter(::isSongLike)
                        .take(12)
                    // Canciones afines a la primera: no solo los mismos artistas.
                    val related = songs.firstOrNull()
                        ?.let { youtube.musicRadio(it.id).songs.drop(1).take(6) }
                        .orEmpty()
                    (songs.shuffled().take(8) + related).shuffled()
                }
            }.awaitAll()
        }
        // Se reparten por turnos para que ningún gusto acapare la lista.
        val mixed = (0 until (lists.maxOfOrNull { it.size } ?: 0)).flatMap { turn ->
            lists.shuffled().mapNotNull { it.getOrNull(turn) }
        }
        return diversify(
            dedupeBySong(mixed.filter { it.id !in excludeIds && isSongLike(it) }),
        ).take(limit)
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
        genreHint: String? = null,
        artistOnly: String? = null,
    ): List<Song> {
        val saved = library.favorites + library.playlists.flatMap { it.songs }
        val savedIds = saved.mapTo(HashSet()) { it.id }
        val onlyKey = artistOnly?.let(RecommendationText::key)?.takeIf { it.isNotBlank() }

        // Echo construye la radio desde el endpoint `next` de YouTube, que
        // devuelve la playlist RDAMVM de la canción semilla. Esa playlist es
        // la fuente principal: el propio YouTube decide qué suena después.
        // NewPipe no expone el `continuation`, pero la playlist pública da el
        // mismo catálogo y se reabre desde la última canción al agotarse.
        val seedId = seedSong?.id
        if (seedSong != null && onlyKey == null) {
            val fromMusic = musicBatch(seedSong, excludeIds + savedIds, limit, genreHint)
            if (fromMusic.isNotEmpty()) return fromMusic
        }

        // De aquí en adelante: modo artista, o respaldo si Music no responde.
        if (seedId != null && radioPoolSeed != seedId) {
            radioPoolSeed = seedId
            radioPool = youtube.radioPlaylist(seedId)
            Log.d("RadioGenero", "Playlist RD de YouTube: ${radioPool.size} canciones")
        }
        if (radioPool.isNotEmpty() && radioPool.count { it.id !in radioSeenIds } < limit) {
            radioChain.lastOrNull()?.let { nextSeed ->
                if (nextSeed != radioPoolSeed) {
                    radioPoolSeed = nextSeed
                    radioPool = youtube.radioPlaylist(nextSeed)
                }
            }
        }

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
            // En radio de artista valen las versiones en vivo o remasterizadas:
            // deducir por título dejaría solo tres canciones de cada intérprete.
            if (onlyKey != null) return true
            return titleKeys(song).none { it in seedKeys || it in radioSeenKeys }
        }

        // NewPipe no expone el continuation del endpoint `next` que Echo usa
        // para paginar el radio. Se fija UN género al empezar (igual que el
        // playlist RD de YouTube) y solo se rotan búsquedas dentro de ese
        // género: cambian las canciones y los cantantes, nunca el estilo.
        if (radioQueries.isEmpty()) {
            if (onlyKey != null) {
                // "Grandes éxitos": solo canciones de ese artista, aunque los
                // relacionados de YouTube mezclen a otros grupos.
                val name = artistOnly!!.trim()
                radioGenre = null
                radioQueries = listOf(
                    "$name exitos",
                    "$name canciones",
                    "$name musica",
                    "$name greatest hits",
                    "$name canciones completas",
                    "$name live",
                ).distinct().shuffled()
                Log.d("RadioGenero", "Radio de artista: $name")
            } else {
                // El lugar donde se eligió la canción (sección "Rock en español",
                // chip de ánimo, búsqueda...) manda: las etiquetas del vídeo suelen
                // ser genéricas y antes hacían caer casi toda la radio en reggaetón.
                val explicit = genreHint?.takeIf { it.isNotBlank() }?.let(::genreQueryFor)
                val streamTags = if (explicit == null) {
                    seedSong?.id?.let { youtube.streamTags(it) }.orEmpty()
                } else {
                    emptySet()
                }
                val genre = explicit ?: pickGenre(seedTags + streamTags, artist, language, seedSong)
                radioGenre = genre
                Log.d("RadioGenero", "Género fijado: $genre (semilla=${seedSong?.title}, pista=$genreHint)")
                radioQueries = if (genre.isBlank()) {
                    // Género desconocido: buscar por el artista. Inventar uno
                    // ("latin pop") llenaba la cola de música sin relación.
                    listOf("$artist canciones", "$artist exitos", "$artist")
                } else {
                    (RADIO_SUFFIXES + listOf("exitos", "canciones"))
                        .shuffled().take(3).map { suffix -> "$genre $suffix" }
                        .let { if (artist != null) it + "$artist $genre" else it }
                        .distinct().shuffled()
                }
            }
        }
        val fixedGenre = radioGenre.orEmpty()
        // "Rock en español" fija también el idioma, aunque el vídeo elegido
        // tenga un título en inglés o la letra no se pueda detectar.
        val radioLanguage = when {
            onlyKey != null -> null
            fixedGenre.contains("espanol") || fixedGenre.contains("latino") -> "es"
            fixedGenre.startsWith("english") || fixedGenre.contains("kpop") -> "en"
            else -> language
        }
        val queriesPerBatch = if (onlyKey != null) radioQueries.size else 3
        val batchQueries = (0 until minOf(queriesPerBatch, radioQueries.size)).map {
            radioQueries[(radioCursor + it) % radioQueries.size]
        }
        radioCursor += batchQueries.size

        // Echo recorre el "up next" de YouTube página a página. NewPipe no da
        // el continuation, pero getRelatedStreams() de cada vídeo de la cadena
        // produce el mismo efecto: saltamos de una canción a la siguiente
        // relacionada en vez de repetir búsquedas genéricas.
        val chainIds = if (radioChain.isEmpty() && seedSong != null) {
            listOf(seedSong.id)
        } else {
            val ids = (0 until minOf(2, radioChain.size - radioChainCursor))
                .map { radioChain[radioChainCursor + it] }
            radioChainCursor += ids.size
            ids
        }
        val fromRelated = supervisorScope {
            chainIds.map { id ->
                async { runCatching { youtube.related(id, limit = limit + 10) }.getOrDefault(emptyList()) }
            }.awaitAll()
        }.flatten()

        // En modo artista el catálogo que la app ya ha mostrado es oro: no
        // cuesta red y son canciones seguras de ese intérprete.
        val fromDiscovery = if (onlyKey != null) {
            discovered.map { it.song }.filter { song ->
                RecommendationText.artistMatchesStrict(song.artist, onlyKey) ||
                    RecommendationText.key(song.title).startsWith("$onlyKey - ")
            }
        } else {
            emptyList()
        }
        // Las búsquedas de YouTube Music con filtro "solo canciones" dan las
        // pistas de audio limpias (canal del artista). La playlist RD de
        // YouTube se usa como complemento, nunca como fuente principal: mezcla
        // videoclips y canales "Oficial<Artista>" que el usuario no quiere.
        val fromSearch = searchAll(batchQueries)
        val candidates = (fromSearch + fromDiscovery + fromRelated + radioPool)
            .filter(::isSongLike)
            .filter(::usable)
            .filter { song ->
                if (onlyKey == null) return@filter true
                if (RecommendationText.artistMatchesStrict(song.artist, onlyKey)) return@filter true
                // Algunos canales ponen el nombre en el título ("Maná - Hechicera").
                // Debe ir seguido de guion: "Corazón espinado (feat. Mana)" es de
                // Santana y "Nirvana Shatakam" es de un canal de música zen.
                RecommendationText.key(song.title).startsWith("$onlyKey - ")
            }
            .distinctBy { it.id }

        // Los relacionados mezclan estilos: una canción de rock arrastra
        // reggaetón al lado. Se descarta lo que no encaja con el género fijado,
        // que es exactamente lo que hace el filtro de página de Echo. En modo
        // artista el filtro anterior ya basta y no se mira el género.
        val coherent = if (onlyKey != null) {
            candidates
        } else {
            candidates.filter { genreMatches(it, fixedGenre, discovered) }
        }
        fun languageGroup(song: Song): String {
            if (RecommendationText.hasForeignScript("${song.title} ${song.artist}")) {
                return "foreign"
            }
            return RecommendationText.languageOf(song) ?: "unknown"
        }
        val sameLanguage = coherent.filter { song ->
            radioLanguage == null || languageGroup(song) == radioLanguage ||
                languageGroup(song) == "unknown"
        }
        // Antes, con menos de cuatro resultados se devolvían los candidatos SIN
        // filtrar: por eso a veces sonaban otros géneros e idiomas.
        val pool = sameLanguage
        val batch = if (onlyKey != null) {
            // Aquí queremos todas las del artista, no un máximo por intérprete.
            pool.shuffled().take(limit)
        } else {
            diversify(dedupeBySong(pool.shuffled())).take(limit)
        }
        radioSeenKeys += batch.flatMap(::titleKeys)
        radioSeenIds += batch.map { it.id }
        radioChain += batch.map { it.id }
        Log.d(
            "RadioGenero",
            "Lote ${batch.size}: ${coherent.size} válidas de ${candidates.size} " +
                "(${onlyKey?.let { "artista $it" } ?: fixedGenre}, " +
                "${radioLanguage ?: "cualquiera"})",
        )
        return batch
    }

    /**
     * Siguiente lote de la radio de YouTube Music. Respeta su orden (primero
     * lo más afín) y descarta lo ya servido y lo que está en otro idioma.
     */
    private suspend fun musicBatch(
        seed: Song,
        excluded: Set<String>,
        limit: Int,
        genreHint: String?,
    ): List<Song> {
        if (musicSeed != seed.id) {
            var radioId = seed.id
            var page = youtube.musicRadio(radioId)
            if (page.songs.count { it.id != seed.id } < MIN_MUSIC_RESULTS) {
                // La semilla es un videoclip (Tendencias, búsqueda web): Music
                // responde con más videoclips. Se usa su versión de audio.
                radioId = audioVersionOf(seed)?.id ?: return emptyList()
                page = youtube.musicRadio(radioId)
            }
            if (page.songs.isEmpty()) return emptyList()
            musicPool.clear()
            page.songs.forEach { musicPool[it.id] = it }
            musicSeed = seed.id
            musicRadioId = radioId
            musicContinuation = page.continuation
            musicReseedCursor = 0
            musicLanguage = radioLanguageOf(seed, page.songs, genreHint)
        }
        val seedKeys = titleKeys(seed)
        fun available(): List<Song> = diversify(
            dedupeBySong(
                musicPool.values.filter { song ->
                    song.id != seed.id && song.id !in excluded && song.id !in radioSeenIds &&
                        isSongLike(song) &&
                        titleKeys(song).none { it in seedKeys || it in radioSeenKeys } &&
                        languageFits(song, musicLanguage)
                },
            ),
        )

        var ready = available()
        var requests = 0
        while (ready.size < limit / 2 && requests < MAX_MUSIC_REQUESTS) {
            requests++
            val token = musicContinuation
            val page = if (token != null) {
                youtube.musicRadio(musicRadioId ?: seed.id, token)
            } else {
                // Radio agotada: se reabre desde las PRIMERAS canciones
                // servidas, las más cercanas a la semilla, para no derivar.
                val next = radioChain.getOrNull(musicReseedCursor) ?: break
                musicReseedCursor++
                youtube.musicRadio(next)
            }
            val before = musicPool.size
            page.songs.forEach { song -> if (song.id !in musicPool) musicPool[song.id] = song }
            // Una página sin nada nuevo significa que esa radio se agotó.
            musicContinuation = page.continuation?.takeIf { musicPool.size > before }
            ready = available()
        }
        val batch = ready.take(limit)
        radioSeenKeys += batch.flatMap(::titleKeys)
        radioSeenIds += batch.map { it.id }
        radioChain += batch.map { it.id }
        Log.d(
            "RadioGenero",
            "Radio Music: ${batch.size} de ${musicPool.size} (${musicLanguage ?: "cualquiera"})",
        )
        return batch
    }

    /** La misma canción en audio o, si no existe, otra del MISMO artista. */
    private suspend fun audioVersionOf(seed: Song): Song? {
        val keys = titleKeys(seed)
        val seedText = " ${RecommendationText.key("${seed.title} ${seed.artist}")} "
        val query = RecommendationText.key(
            "${seed.artist} ${seed.title.replace(Regex("\\[.*?\\]|\\(.*?\\)"), " ")}",
        ).ifBlank { return null }
        val sameArtist = searchAll(listOf(query)).filter { song ->
            val artistKey = RecommendationText.key(song.artist)
            song.id != seed.id && artistKey.isNotBlank() && " $artistKey " in seedText
        }
        return sameArtist.firstOrNull { song ->
            val titleKey = RecommendationText.key(song.title)
            titleKeys(song).any { it in keys } ||
                (titleKey.isNotBlank() && " $titleKey " in seedText)
        } ?: sameArtist.firstOrNull()
    }

    /**
     * Idioma de la radio. Un título suelto engaña ("Halo", "BbY WOW"), así que
     * manda lo que suena alrededor; después la semilla y la sección de origen.
     */
    private fun radioLanguageOf(seed: Song, songs: List<Song>, genreHint: String?): String? {
        if (RecommendationText.hasForeignScript(seed.title)) return null
        if (songs.count { RecommendationText.hasForeignScript(it.title) } * 3 > songs.size) return null
        val votes = songs.mapNotNull(::titleLanguage)
        val top = votes.groupingBy { it }.eachCount().maxByOrNull { it.value }
        if (top != null && votes.size >= MIN_LANGUAGE_VOTES && top.value * 10 >= votes.size * 7) {
            return top.key
        }
        titleLanguage(seed)?.let { return it }
        if (RecommendationText.likelyLatinArtist(seed.artist)) return "es"
        val hint = genreHint?.let(::genreQueryFor).orEmpty()
        return if ("espanol" in hint || "latino" in hint) "es" else null
    }

    /** Solo el título: la tilde de "Beyoncé" no convierte su música en español. */
    private fun titleLanguage(song: Song): String? =
        RecommendationText.languageOf(song.copy(artist = ""))

    private fun languageFits(song: Song, language: String?): Boolean {
        if (language == null) return true
        if (RecommendationText.hasForeignScript(song.title)) return false
        val detected = titleLanguage(song)
        return detected == null || detected == language
    }

    /**
     * Una canción encaja con el género de la radio si no hay señales claras de
     * otro estilo. Se usan las etiquetas del lugar donde se descubrió, el
     * artista conocido y las palabras del título. Sin señales se conserva:
     * mejor un vídeo dudoso que vaciar la cola.
     */
    private fun genreMatches(
        song: Song,
        genre: String,
        discovered: List<DiscoveredSong>,
    ): Boolean {
        if (genre.isBlank()) return true
        val targetWords = RecommendationText.tags(genre) - GENERIC_GENRE_WORDS
        if (targetWords.isEmpty()) return true

        val tags = discovered.firstOrNull { it.song.id == song.id }?.tags.orEmpty() +
            RecommendationText.tags("${song.title} ${song.artist}")
        val artistKey = RecommendationText.key(song.artist)
        val artistGenre = ARTIST_GENRES
            .firstOrNull { " ${RecommendationText.key(it.first)} " in " $artistKey " }
            ?.second
        val signals = (tags + artistGenre?.let(RecommendationText::tags).orEmpty()) -
            GENERIC_GENRE_WORDS
        if (signals.any { it in targetWords }) return true
        val otherGenre = (TASTE_GENRES.map { it.query } + DISCOVER_GENRES).firstOrNull { other ->
            other != genre &&
                (RecommendationText.tags(other) - GENERIC_GENRE_WORDS).any { it in signals }
        }
        return otherGenre == null
    }

    /**
     * Convierte la etiqueta de una sección ("Rock", "Rock en español") en una
     * consulta de género. Devuelve null si no es un género (por ejemplo
     * "Tu artista: Maná"): en ese caso se detecta por el artista de la canción.
     */
    private fun genreQueryFor(hint: String): String? {
        val key = RecommendationText.key(hint)
        if (key.isBlank() || "artista" in key || "podcast" in key) return null
        TASTE_GENRES.firstOrNull { RecommendationText.key(it.label) == key }
            ?.let { return it.query }
        TASTE_GENRES.firstOrNull { RecommendationText.key(it.query) == key }
            ?.let { return it.query }
        val words = RecommendationText.tags(hint) - GENERIC_GENRE_WORDS
        val genres = TASTE_GENRES.map { it.query } + DISCOVER_GENRES
        return genres.firstOrNull { genre ->
            (RecommendationText.tags(genre) - GENERIC_GENRE_WORDS).any { it in words }
        }
    }

    /**
     * Género de la radio cuando la pantalla no lo dice explícitamente. Antes se
     * elegía el primer género que compartiera cualquier palabra ("español" hacía
     * que "Rock en español" cayera en "Pop en español") y, sin coincidencia, el
     * comodín era reggaetón. Ahora solo cuentan las palabras que identifican el
     * estilo y el comodín es neutro.
     */
    private fun pickGenre(
        tags: Set<String>,
        artist: String?,
        language: String?,
        song: Song?,
    ): String {
        val distinctive = tags - GENERIC_GENRE_WORDS
        val genres = TASTE_GENRES.map { it.query } + DISCOVER_GENRES
        genres
            .map { genre ->
                val words = RecommendationText.tags(genre) - GENERIC_GENRE_WORDS
                genre to words.count { it in distinctive }
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.let { return it.first }

        if (artist != null) {
            val key = RecommendationText.key(artist)
            ARTIST_GENRES.firstOrNull { " ${RecommendationText.key(it.first)} " in " $key " }
                ?.let { return it.second }
        }
        if (song != null) {
            val fromTitle = RecommendationText.tags("${song.title} ${song.artist}") -
                GENERIC_GENRE_WORDS
            genres.firstOrNull { genre ->
                (RecommendationText.tags(genre) - GENERIC_GENRE_WORDS).any { it in fromTitle }
            }?.let { return it }
        }
        if (artist != null) return ""
        return when (language) {
            "en" -> "english pop"
            else -> "latin pop"
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
        // Radio = solo música, nunca videoclips. YouTube Music mezcla los
        // "Video Oficial" con las canciones, y sus canales son "OficialMana"
        // en vez de "Maná - Topic". Se descartan ambos.
        if (VIDEO_TITLE_WORDS.any { title.contains(it) }) return false
        if (VIDEO_TITLE_REGEX.containsMatchIn(title)) return false
        val artistKey = RecommendationText.key(song.artist)
        // Canales de VIDEOS: "OficialMana", "Bacilos Oficial", "Bacilos Official".
        // El canal de audio es "<Artista> - Topic", no estos.
        if (artistKey.startsWith("oficial") && artistKey.length > 7) return false
        if (artistKey.endsWith("oficial") && artistKey.length > 7) return false
        if (artistKey.endsWith("official") && artistKey.length > 8) return false
        if ("vevo" in artistKey) return false
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
        const val MIN_LANGUAGE_VOTES = 5
        const val MIN_MUSIC_RESULTS = 5
        const val MAX_MUSIC_REQUESTS = 3
        const val MIN_SONGS = 8
        const val MIN_DURATION = 45L
        const val MAX_DURATION = 600L
        val MIX_REGEX = Regex("\\bmix\\b")
        /** Títulos de videoclip: la radio debe quedarse solo con el audio. */
        val VIDEO_TITLE_WORDS = listOf(
            "video oficial", "official video", "video official",
            "videoclip", "video musical", "official music video",
            "music video", "videolyric", "video lyric", "video lyrics",
            "(video)", "[video]", "(vevo)", "vevo",
            "karaoke", "karaoke version",
        )
        /** "Official HD Video", "Official Music Video HD", "(Official Video 4K)". */
        val VIDEO_TITLE_REGEX = Regex(
            "\\bofficial\\b.{0,20}\\bvideo\\b|\\bvideo\\b.{0,20}\\bofficial\\b|\\bofficial\\b.{0,20}\\bhd\\b|\\bhd\\b.{0,20}\\bofficial\\b",
        )
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
        /** Palabras que no identifican un estilo y no deben decidir el género. */
        val GENERIC_GENRE_WORDS = setOf(
            "espanol", "ingles", "inglesa", "latino", "latin", "musica", "music",
            "canciones", "cancion", "songs", "song", "exitos", "exit", "hits",
            "top", "actual", "clasico", "clasicos", "nuevas", "nuevo", "new",
            "version", "radio", "official", "video", "audio", "todo", "los",
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
            "heroes del silencio" to "rock en espanol",
            "héroe" to "rock en espanol",
            "hombres g" to "pop en espanol",
            "la union" to "rock en espanol",
            "el tri" to "rock en espanol",
            "elefante" to "rock en espanol",
            "bon jovi" to "rock clasico",
            "la oreja de van gogh" to "pop en espanol",
            "cristian castro" to "baladas romanticas",
            "alejandro sanz" to "baladas romanticas",
            "chayanne" to "baladas romanticas",
            "ricky martin" to "latin pop",
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
