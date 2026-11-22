package com.popmusic.app.data.music

/** Cuenta de un servicio de música vinculada a la app. */
data class MusicAccount(
    val service: MusicService,
    val userId: String,
    val userName: String,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAt: Long?,
    val connectedAt: Long = System.currentTimeMillis(),
) {
    fun isExpired(): Boolean {
        val exp = expiresAt ?: return false
        return System.currentTimeMillis() >= exp
    }

    /**
     * Vinculada = hay un camino para obtener un token válido: el access token
     * todavía vale, o hay refresh_token para renovarlo. Antes exigía
     * `accessToken != null && !isExpired()`: a la ~1 h la cuenta se
     * "desconectaba" sola aunque hubiera refresh_token, y el watcher de sync
     * la ignoraba (`if (!repository.isConnected()) continue`).
     * (Token vencido SIN refresh → desconectada: ya no sirve para llamar a la API.)
     */
    fun isConnected(): Boolean =
        (accessToken != null && !isExpired()) || refreshToken != null
}

/** Servicios de música soportados para vinculación. */
enum class MusicService {
    YOUTUBE,
    SPOTIFY;

    fun displayName(): String = when (this) {
        // Sin "Music": en la app solo se habla de "YouTube" (nunca se da a
        // entender que las canciones vienen de ahí salvo al importar/exportar).
        YOUTUBE -> "YouTube"
        SPOTIFY -> "Spotify"
    }
}

/** Playlist obtenida de un servicio externo. */
data class ExternalPlaylist(
    val service: MusicService,
    val externalId: String,
    val name: String,
    val trackCount: Int,
    val thumbnailUrl: String?,
)

/** Canción de un servicio externo, con metadatos para convertir a Song de la app. */
data class ExternalSong(
    val service: MusicService,
    val externalId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Long = 0,
    val thumbnailUrl: String?,
    /** Id reproducible en la app (videoId de YouTube). Null si hay que resolverlo. */
    val playbackId: String? = null,
)
