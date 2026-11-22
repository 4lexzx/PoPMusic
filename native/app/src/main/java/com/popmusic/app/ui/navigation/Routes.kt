package com.popmusic.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val RECOGNIZE = "recognize"
    const val LIBRARY = "library"
    const val NOW_PLAYING = "now_playing"
    const val MUSIC_FLOW = "music_flow"
    const val LYRICS = "lyrics"
    const val SONG_INFO = "song_info"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val AUTH = "auth"
    const val ACCOUNT = "account"
    const val TASTES = "tastes"
    const val PARTY = "party"
    const val PLAYLIST = "playlist/{playlistId}"
    const val ARTIST = "artist"

    fun playlist(playlistId: String): String = "playlist/$playlistId"
}
