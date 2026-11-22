package com.popmusic.app.data.music

/**
 * Interfaz común para servicios de música externos (YouTube, Spotify).
 * Permite listar playlists de la cuenta vinculada, leer sus canciones y
 * escribir en ellas para sincronizar los cambios de la app.
 */
interface MusicServiceRepository {

    val service: MusicService

    /** Playlists de la cuenta vinculada. */
    suspend fun importPlaylists(): List<ExternalPlaylist>

    /** Canciones de una playlist externa. */
    suspend fun importPlaylistSongs(externalPlaylistId: String): List<ExternalSong>

    /** Crea una playlist en el servicio externo y devuelve su id. */
    suspend fun createPlaylist(name: String): String?

    /** Cambia el nombre de una playlist ya creada en el servicio externo. */
    suspend fun renamePlaylist(externalPlaylistId: String, name: String): Boolean

    /** Añade una canción a una playlist del servicio externo. */
    suspend fun addToPlaylist(externalPlaylistId: String, song: ExternalSong): Boolean

    /**
     * Elimina una canción de una playlist del servicio externo (sync en
     * tiempo real cuando se borra localmente). `song.externalId` es el id
     * remoto si se conoce; si no, se resuelve por título/artista.
     */
    suspend fun removeFromPlaylist(externalPlaylistId: String, song: ExternalSong): Boolean

    /** Busca el id del servicio para una canción (para añadirla a una playlist). */
    suspend fun searchTrackId(title: String, artist: String): String?

    /** Hay una cuenta vinculada con token utilizable. */
    fun isConnected(): Boolean
}
