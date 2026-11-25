package com.popmusic.app.data.lyrics

import java.text.Normalizer

/** Decide qué versión de una canción (normal, remix, en vivo...) es cada letra. */
internal object LyricsMatcher {

    data class Candidate(
        val index: Int,
        val track: String,
        val artist: String,
        val text: String,
        val score: Double,
    )

    /** Palabras que distinguen la versión: "(Tiago PZK Version)" → tiago, pzk. */
    fun versionTokens(title: String): Set<String> =
        versionParts(title).flatMap(::words).filterNot { it in FILLER }.toSet()

    /** Título sin corchetes, salvo los que indican la versión. */
    fun titleWithVersion(title: String): String {
        val base = title.replace(BRACKETS, " ")
        val missing = versionParts(title).filterNot { base.contains(it) }
        return (listOf(base) + missing).joinToString(" ").replace(Regex("\\s+"), " ").trim()
    }

    /** True si la versión conserva la letra original (en vivo, acústica...). */
    fun keepsLyrics(tokens: Set<String>): Boolean = tokens.all { it in SAME_LYRICS }

    fun matchesVersion(wanted: Set<String>, track: String, artist: String): Boolean =
        words("$track $artist").toSet().containsAll(wanted)

    fun penalty(
        wanted: Set<String>,
        songTitle: String,
        songArtist: String,
        track: String,
        artist: String,
    ): Double {
        val found = words("$track $artist").toSet()
        val known = wanted + words(songTitle) + words(songArtist)
        var penalty = wanted.count { it !in found } * VERSION_PENALTY
        // La letra es de OTRA versión que la canción no menciona.
        if (versionTokens(track).any { it !in known }) penalty += VERSION_PENALTY
        val artistWords = words(songArtist)
        val sameArtist = artistWords.isEmpty() || artistWords.all { it in found } ||
            words(artist).let { it.isNotEmpty() && it.all(known::contains) }
        if (!sameArtist) penalty += ARTIST_PENALTY
        // "Despacito (Audio) ft. Justin Bieber" no es "Despacito".
        if (words("$track $artist").any { it !in known && it !in FILLER && it !in SAME_LYRICS }) {
            penalty += EXTRA_WORDS_PENALTY
        }
        return penalty
    }

    /**
     * LRCLIB tiene letras mal etiquetadas. Entre las que encajan igual de bien
     * gana el texto en el que coinciden más entradas.
     */
    fun choose(candidates: List<Candidate>): Candidate? {
        val best = candidates.minOfOrNull { it.score } ?: return null
        val group = candidates.filter { it.score <= best + TIE_MARGIN }
            .groupBy { fingerprint(it.text) }
            .values
            .maxWithOrNull(
                compareBy<List<Candidate>> { it.size }.thenBy { list -> -list.minOf { it.score } },
            ) ?: return null
        return group.minByOrNull { it.score }
    }

    private fun fingerprint(text: String): String =
        words(text.replace(Regex("\\[[^]]*]|<[^>]*>"), " ")).take(6).joinToString(" ")

    private fun versionParts(title: String): List<String> {
        val brackets = BRACKETS.findAll(title).map { it.value.trim('(', ')', '[', ']') }
        val suffix = title.replace(BRACKETS, " ").split(Regex("\\s+[-–—]\\s+")).drop(1)
        return (brackets + suffix)
            .map { it.trim() }
            .filter { part ->
                val found = words(part)
                found.any { it in MARKERS } && !NEUTRAL.containsMatchIn(found.joinToString(" "))
            }
            .toList()
    }

    private fun words(value: String): List<String> = Normalizer
        .normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }

    private const val VERSION_PENALTY = 40.0
    private const val ARTIST_PENALTY = 30.0
    private const val EXTRA_WORDS_PENALTY = 8.0
    private const val TIE_MARGIN = 2.0
    private val BRACKETS = Regex("\\([^)]*\\)|\\[[^]]*]")
    private val MARKERS = setOf(
        "version", "remix", "rmx", "mix", "edit", "live", "vivo", "directo",
        "acoustic", "acustico", "acustica", "stripped", "unplugged", "cover",
        "demo", "slowed", "sped", "spanglish", "bachata", "salsa",
    )
    /** Ediciones con la misma letra que la original. */
    private val NEUTRAL = Regex(
        "\\b(single|album|original|radio|explicit|clean|deluxe|lp|ep) (version|edit|mix)\\b|\\bremaster\\w*",
    )
    private val FILLER = setOf(
        "version", "edit", "mix", "the", "en", "de", "con", "feat", "ft", "and", "y", "x",
        "official", "oficial", "video", "lyric", "lyrics", "audio", "music", "at", "from", "in",
    )
    private val SAME_LYRICS = setOf(
        "live", "vivo", "directo", "acoustic", "acustico", "acustica", "stripped",
        "unplugged", "demo", "slowed", "sped", "up", "reverb",
    )
}
