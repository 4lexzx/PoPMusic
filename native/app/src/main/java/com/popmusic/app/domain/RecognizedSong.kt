package com.popmusic.app.domain

data class RecognizedSong(
    val title: String,
    val artist: String,
    val album: String? = null,
)
