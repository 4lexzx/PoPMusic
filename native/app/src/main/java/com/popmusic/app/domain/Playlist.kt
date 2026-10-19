package com.popmusic.app.domain

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
)
