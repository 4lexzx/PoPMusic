package com.popmusic.app.data.podcasts

/** Categorías curadas para los chips de la pestaña Podcast. */
val PODCAST_CATEGORIES = listOf(
    "Noticias", "Comedia", "Deportes", "Ciencia", "Tecnología",
    "Historia", "Salud", "Negocios", "Entretenimiento", "Música",
)

/** Podcast del catálogo (iTunes Search / RSS). */
data class Podcast(
    val id: String,
    val title: String,
    val author: String,
    val artworkUrl: String?,
    val feedUrl: String,
    val category: String? = null,
)

/** Episodio de un feed RSS (mp3/ogg directo, sin YouTube). */
data class PodcastEpisode(
    val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val durationSeconds: Long,
    val publishedAt: Long,
    val podcastTitle: String,
    val artworkUrl: String?,
    val podcastFeedUrl: String,
)
