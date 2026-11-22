package com.popmusic.app.ui.screens.lyrics

import com.popmusic.app.domain.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricTimingTest {

    @Test
    fun `line synced words spread by word length`() {
        val words = wordsFor(
            line = LyricLine(timeMs = 1_000L, text = "I will always love you"),
            next = LyricLine(timeMs = 10_000L, text = "Next line"),
        )

        assertEquals(5, words.size)
        assertFalse(words.any { it.isExact })
        assertEquals(1_000L, words.first().startMs)
        assertEquals(1_857L, words[1].startMs)
        assertEquals(7_570L, words[4].startMs)
        assertEquals(9_000L, words.last().endMs)
    }

    @Test
    fun `enhanced word times are used exactly`() {
        val words = wordsFor(
            line = LyricLine(
                timeMs = 2_000L,
                text = "one two three",
                wordTimes = listOf(2_000L, 2_400L, 3_100L),
            ),
            next = LyricLine(timeMs = 6_000L, text = "Next line"),
        )

        assertEquals(3, words.size)
        assertTrue(words.all { it.isExact })
        assertEquals(2_000L, words[0].startMs)
        assertEquals(2_400L, words[1].startMs)
        assertEquals(3_100L, words[2].startMs)
        assertEquals(6_000L, words.last().endMs)
    }

    @Test
    fun `single word covers the whole line`() {
        val words = wordsFor(
            line = LyricLine(timeMs = 1_000L, text = "Oh"),
            next = LyricLine(timeMs = 6_000L, text = "Next line"),
        )

        assertEquals(1, words.size)
        assertEquals(1_000L, words.single().startMs)
        assertEquals(6_000L, words.single().endMs)
    }

    @Test
    fun `missing word times fall back to line synced spread`() {
        val words = wordsFor(
            line = LyricLine(timeMs = 0L, text = "one two three"),
            next = null,
        )

        assertEquals(3, words.size)
        assertFalse(words.any { it.isExact })
        assertEquals(1_470L, words[1].startMs - words[0].startMs)
    }
}
