package com.popmusic.app.ui.screens.lyrics

import com.popmusic.app.domain.LyricLine

internal data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val isExact: Boolean,
)

/**
 * Reparte las palabras de una línea.
 *
 * Si el LRC trae marcas por palabra (<mm:ss.xx>) se usan esos tiempos exactos.
 * Si no, se reparten por longitud de palabra a lo largo de la línea (acotada
 * para que una pausa instrumental no estire la última palabra).
 */
internal fun wordsFor(line: LyricLine, next: LyricLine?): List<LyricWord> {
    val tokens = line.text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return emptyList()

    val start = line.timeMs
    val lineEnd = (next?.timeMs ?: (start + DEFAULT_LINE_HOLD_MS))
        .coerceAtMost(start + MAX_LINE_HOLD_MS)
        .coerceAtLeast(start + MIN_LINE_HOLD_MS)

    val wordTimes = line.wordTimes
    if (wordTimes.size == tokens.size) {
        return tokens.mapIndexed { index, token ->
            val wordStart = wordTimes[index]
            val wordEnd = if (index == tokens.lastIndex) {
                lineEnd
            } else {
                wordTimes[index + 1]
            }
            LyricWord(
                text = token,
                startMs = wordStart,
                endMs = wordEnd.coerceAtLeast(wordStart + 1L),
                isExact = true,
            )
        }
    }

    val span = (lineEnd - start).coerceAtLeast(MIN_LINE_HOLD_MS)
    val weights = tokens.map { it.length + 2 }
    val totalWeight = weights.sum()
    val steps = LongArray(tokens.size)
    for (index in 0 until tokens.size - 1) {
        steps[index] = (span * weights[index].toLong() / totalWeight).coerceAtLeast(1L)
    }
    steps[tokens.size - 1] = span - steps.sum()

    var cursor = start
    return tokens.mapIndexed { index, token ->
        val wordStart = cursor
        cursor += steps[index]
        LyricWord(
            text = token,
            startMs = wordStart,
            endMs = cursor.coerceAtLeast(wordStart + 1L),
            isExact = false,
        )
    }
}

private const val DEFAULT_LINE_HOLD_MS = 5_000L
private const val MAX_LINE_HOLD_MS = 8_000L
private const val MIN_LINE_HOLD_MS = 600L
