package com.popmusic.app.domain

data class LyricLine(
    val timeMs: Long,
    val text: String,
)

data class Lyrics(
    val lines: List<LyricLine>,
    val plainText: String? = null,
)
