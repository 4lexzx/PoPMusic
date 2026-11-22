package com.popmusic.app.domain

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val wordTimes: List<Long> = emptyList(),
    val isInstrumental: Boolean = false,
)

data class Lyrics(
    val lines: List<LyricLine>,
    val plainText: String? = null,
    val albumName: String? = null,
    val artistName: String? = null,
)
