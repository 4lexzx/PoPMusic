package com.popmusic.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val RECOGNIZE = "recognize"
    const val LIBRARY = "library"
    const val NOW_PLAYING = "now_playing"
    const val LYRICS = "lyrics"
    const val SETTINGS = "settings"
    const val PLAYLIST = "playlist/{playlistId}"

    fun playlist(playlistId: String): String = "playlist/$playlistId"
}
