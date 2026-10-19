package com.popmusic.app.domain

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long = 0L,
    val thumbnailUrl: String? = null,
)
