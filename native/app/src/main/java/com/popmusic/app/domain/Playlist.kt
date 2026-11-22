package com.popmusic.app.domain

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    /** Ids de la misma playlist en servicios externos: "YOUTUBE"/"SPOTIFY" -> id externo. */
    val external: Map<String, String> = emptyMap(),
    /**
     * Elección al crear la playlist (ítem 3): "YOUTUBE"/"SPOTIFY" = subirla
     * solo a esa cuenta; "NONE" = nunca subirla (solo en el teléfono);
     * null = automático (se replica en todas las cuentas conectadas).
     */
    val syncTarget: String? = null,
)
