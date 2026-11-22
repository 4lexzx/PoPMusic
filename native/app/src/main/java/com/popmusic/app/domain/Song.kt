package com.popmusic.app.domain

/**
 * @param streamUrl URL directa de audio (mp3 de podcasts). Si existe, se
 * reproduce tal cual sin pasar por el resolutor de YouTube.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long = 0L,
    val thumbnailUrl: String? = null,
    val streamUrl: String? = null,
)
