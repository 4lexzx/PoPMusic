package com.popmusic.app.data.recommendations

import com.popmusic.app.domain.Song
import java.text.Normalizer

/** Normalización compartida por la radio y el catálogo de canciones descubiertas. */
internal object RecommendationText {

    /** Clave sin acentos ni sufijos de canal, para comparar artistas y etiquetas. */
    fun key(value: String): String = Normalizer
        .normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace(Regex("\\b(official|topic|vevo|audio|music)\\b"), " ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Palabras útiles de una búsqueda o de un gusto: sirven como etiqueta de género. */
    fun tags(value: String): Set<String> = key(value)
        .split(' ')
        .filter { it.length >= 3 && it !in STOP_WORDS }
        .toSet()

    fun artistMatches(songArtist: String, key: String): Boolean {
        if (key.isBlank()) return false
        val candidate = key(songArtist)
        if (candidate == key) return true
        return key.length >= 3 && candidate.length >= 3 &&
            (candidate.contains(key) || key.contains(candidate))
    }

    /**
     * Comparación estricta para la radio de un artista. `artistMatches` acepta
     * cualquier coincidencia parcial, y eso metía canales como "Paz Nirvana"
     * (música zen) en la radio de Nirvana. Solo vale el nombre exacto o un
     * prefijo de canal que sube canciones del artista ("Todo Maná", "Oficial...").
     */
    fun artistMatchesStrict(songArtist: String, key: String): Boolean {
        if (key.isBlank()) return false
        val candidate = key(songArtist)
        if (candidate == key) return true
        // "Oficial<Artista>" suele ser el canal de VIDEOS, no el de las
        // canciones (ese es "<Artista> - Topic"). No lo aceptamos aquí.
        val channelWords = setOf(
            "todo", "letras", "lyrics", "vevo",
            "topic", "audio", "music", "musica", "canal", "hits",
        )
        if (candidate.endsWith(key) && candidate.length > key.length) {
            val prefix = candidate.removeSuffix(key).trim()
            if (prefix.isEmpty() || prefix.split(' ').any { it in channelWords }) return true
        }
        return candidate.startsWith(key) && candidate.removePrefix(key).trim().isEmpty()
    }

    /** Artistas conocidos del ámbito latino, para inferir el idioma de la radio. */
    private val LATIN_ARTISTS = listOf(
        "karol g", "karol", "bad bunny", "j balvin", "ozuna", "daddy yankee",
        "wisin", "yandel", "nicky jam", "shakira", "enrique iglesias",
        "luis miguel", "romo santos", "floyy menor", "peso pluma",
        "fuerza regida", "grupo frontera", "danny ocean", "anuel aa",
        "raul alejandro", "rosalia", "natti natasha", "sech", "arcangel",
        "alberto plaza", "la factoria", "eddy lover", "willie colon",
    )

    fun likelyLatinArtist(name: String): Boolean {
        val candidate = key(name)
        return candidate.isNotBlank() && LATIN_ARTISTS.any { key(it) in candidate }
    }

    /** Detecta textos en cirílico, chino, griego, etc. (no aptos para la radio). */
    fun hasForeignScript(text: String): Boolean = text.any { char ->
        if (!char.isLetter()) false else {
            val lower = char.lowercaseChar()
            lower !in 'a'..'z' && lower !in "áéíóúüñ"
        }
    }

    /** Idioma aproximado. Devuelve null si el texto no aporta pistas fiables. */
    fun languageOf(song: Song): String? {
        val raw = "${song.title} ${song.artist}".lowercase()
        val text = " ${raw.replace(Regex("[^a-záéíóúüñ]+"), " ")} "
        var es = 0
        var en = 0
        SPANISH_WORDS.forEach { word ->
            if (" $word " in text) es += if (word.length > 3) 2 else 1
        }
        ENGLISH_WORDS.forEach { word ->
            if (" $word " in text) en += if (word.length > 3) 2 else 1
        }
        if (Regex("[áéíóúñ¿¡]").containsMatchIn(raw)) es += 3
        return when {
            es - en >= 2 -> "es"
            en - es >= 2 -> "en"
            else -> null
        }
    }

    private val STOP_WORDS = setOf(
        "songs", "song", "exitos", "exit", "canciones", "cancion", "musica",
        "los", "las", "del", "para", "con", "the", "and", "mix", "version",
        "nuevo", "new", "full", "album", "audio", "official", "video",
    )

    private val SPANISH_WORDS = listOf(
        "de", "la", "el", "los", "las", "un", "una", "que", "en", "con",
        "por", "para", "es", "no", "se", "su", "al", "lo", "como", "mas",
        "me", "mi", "tu", "te", "nos", "amor", "vida", "corazon", "cancion",
        "siempre", "nunca", "porque", "pero", "donde", "cuando", "esta",
        "este", "solo", "bien", "todo", "sin", "ya", "aun", "hoy",
    )

    private val ENGLISH_WORDS = listOf(
        "the", "you", "love", "baby", "girl", "dont", "im", "its", "my",
        "your", "we", "are", "this", "that", "with", "for", "one", "two",
        "night", "day", "know", "want", "need", "feel", "going", "get",
        "got", "never", "always", "just", "like", "all", "out", "up",
    )
}
