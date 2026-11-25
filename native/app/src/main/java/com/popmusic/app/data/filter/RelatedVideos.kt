package com.popmusic.app.data.filter

import com.popmusic.app.domain.Song
import java.text.Normalizer

/**
 * Videos musicales del buscador: versiones en vivo, covers y canciones que no
 * existen como audio. Solo se muestran al buscarlos; nunca pasan a tendencias,
 * radios ni recomendaciones.
 */
object RelatedVideos {

    private const val MIN_SECONDS = 71L
    private const val MAX_SECONDS = 15 * 60L

    /** Palabras de relleno que no deciden si un video es lo que se buscaba. */
    private val filler = setOf(
        "de", "del", "la", "el", "los", "las", "un", "una", "y", "a", "en", "con",
        "the", "of", "and", "in", "to", "cancion", "video", "oficial", "official",
    )

    /** Lo que se añade al título de un video oficial sin cambiar la canción. */
    private val videoNoise = setOf(
        "official", "oficial", "video", "videoclip", "music", "musical", "lyric", "lyrics",
        "letra", "audio", "visualizer", "hd", "hq", "4k", "ft", "feat", "clip", "remastered",
    )

    /** IA, infantil y charla: se buscan como palabras enteras ("audio" no es "udio"). */
    private val blocked = Regex(
        "\\b(" + listOf(
            "ai", "ia", "a i", "suno", "udio", "inteligencia artificial", "artificial intelligence",
            "generad[ao] con ia", "frutas?", "fruits?", "brainrot", "tralalero", "skibidi",
            "kids?", "ninos", "ninas", "infantil(es)?", "nursery", "rhymes?", "cartoons?",
            "canticuentos", "plim plim", "peppa", "cocomelon", "coco melon", "blippi", "pinkfong",
            "baby shark", "granja de zenon", "reino infantil", "bebes?", "toddlers?",
            "gameplay", "walkthrough", "trailer", "reaction", "reaccion(a|ando)?", "reacts?",
            "vlog", "podcast", "interview", "entrevista", "noticias", "audiolibro", "audiobook",
            "tutorial", "unboxing", "parodia", "parody", "karaoke", "instrumental", "nightcore",
            "8d", "bass boosted", "sped up", "slowed", "tiktok", "shorts?", "full album",
            "album completo", "mix", "megamix", "compilacion", "compilation", "playlist",
            "como tocar", "acordes", "cifra", "fingerstyle", "piano cover", "guitar lesson", "asmr",
            "choreography", "coreografia", "ritmica", "gimnasia", "drum cover", "zumba", "fitness", "432hz", "amv", "mmv", "edit", "edits",
        ).joinToString("|") + ")\\b",
    )

    /** Se puede pedir a propósito algo que por defecto se descarta. */
    private val allowedOnRequest = setOf("karaoke", "instrumental", "mix", "parodia", "nightcore", "slowed")

    fun pick(query: String, videos: List<Song>, songs: List<Song>, limit: Int = 12): List<Song> {
        val wanted = words(query).filter { it !in filler }.ifEmpty { words(query) }
        if (wanted.isEmpty()) return emptyList()
        val songIds = songs.mapTo(HashSet()) { it.id }
        val songKeys = songs.map { words(it.title).toSet() to words(it.artist).toSet() }
        val asked = wanted.filter { it in allowedOnRequest }.toSet()

        return videos.asSequence()
            .distinctBy { it.id }
            .filter { it.id !in songIds && it.durationSeconds in MIN_SECONDS..MAX_SECONDS }
            .filter { video ->
                val text = words("${video.title} ${video.artist}").joinToString(" ")
                blocked.findAll(text).all { it.value in asked }
            }
            .map { it to relevance(wanted, it) }
            .filter { it.second >= 0.7f }
            .filterNot { (video, _) -> sameAsSong(video, songKeys) }
            .sortedByDescending { it.second }
            // Una sola copia de cada versión: las subidas con letra se repiten mucho.
            .distinctBy { (video, _) -> words(video.title).filter { it !in videoNoise && it !in filler }.toSet() }
            .map { it.first }
            .take(limit)
            .toList()
    }

    private fun relevance(wanted: List<String>, video: Song): Float {
        val text = words("${video.title} ${video.artist}")
        return wanted.count { w -> text.any { it == w || (w.length >= 3 && it.startsWith(w)) } } /
            wanted.size.toFloat()
    }

    /** El video oficial de una canción que ya sale arriba no aporta nada. */
    private fun sameAsSong(video: Song, songs: List<Pair<Set<String>, Set<String>>>): Boolean {
        val title = words(video.title).filter { it !in videoNoise }.toSet()
        val uploader = words(video.artist).toSet()
        return songs.any { (songTitle, artist) ->
            val bare = title - artist
            val core = songTitle - artist - videoNoise
            core.isNotEmpty() && bare == core && artist.isNotEmpty() &&
                (title + uploader).containsAll(artist.take(2))
        }
    }

    private fun words(text: String): List<String> =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotEmpty() }
}
