package com.popmusic.app.util

private val multiSpace = Regex("\\s{2,}")
private val trailingSeparators = Regex("\\s*[|•·\\-–—]+\\s*$")

/**
 * Limpia un título o nombre de artista para mostrarlo en la interfaz:
 * elimina emojis y símbolos decorativos, colapsa espacios y quita
 * separadores sueltos al final. No altera letras, números ni acentos.
 */
fun String.cleanForDisplay(): String {
    if (isEmpty()) return this
    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        val codePoint = codePointAt(i)
        if (!isEmojiCodePoint(codePoint)) {
            out.appendCodePoint(codePoint)
        }
        i += Character.charCount(codePoint)
    }
    return out.toString()
        .replace(multiSpace, " ")
        .replace(trailingSeparators, "")
        .trim()
}

private fun isEmojiCodePoint(codePoint: Int): Boolean = when (codePoint) {
    0x200D, 0x20E3, 0xFE0E, 0xFE0F, 0x203C, 0x2049, 0x2122, 0x24C2 -> true
    in 0x1F000..0x1FAFF -> true
    in 0x1F1E6..0x1F1FF -> true
    in 0x2190..0x21FF -> true
    in 0x2300..0x23FF -> true
    in 0x25A0..0x25FF -> true
    in 0x2600..0x27BF -> true
    in 0x2900..0x297F -> true
    in 0x2B00..0x2BFF -> true
    else -> false
}
