package com.popmusic.app.data.recommendations

import com.popmusic.app.domain.Song

/** Canción que la app ya ha mostrado, con las etiquetas del lugar donde apareció. */
data class DiscoveredSong(
    val song: Song,
    val tags: Set<String> = emptySet(),
)

/**
 * Catálogo en memoria de lo que el usuario ha visto en búsquedas, "Para ti",
 * tendencias y Music Flow. La radio se sirve de él en vez de inventar
 * búsquedas genéricas que metían canciones sin relación.
 */
object DiscoveryPool {

    private const val MAX_ITEMS = 600
    private val lock = Any()
    private val items = LinkedHashMap<String, DiscoveredSong>()

    fun add(songs: List<Song>, tags: Collection<String> = emptySet()) = synchronized(lock) {
        val cleanTags = tags
            .flatMap { RecommendationText.tags(it) }
            .toSet()
        songs.forEach { song ->
            if (song.id.isBlank()) return@forEach
            val previous = items[song.id]
            items[song.id] = if (previous == null) {
                DiscoveredSong(song, cleanTags)
            } else {
                previous.copy(song = song, tags = previous.tags + cleanTags)
            }
        }
        while (items.size > MAX_ITEMS) {
            items.remove(items.keys.first())
        }
    }

    fun snapshot(): List<DiscoveredSong> = synchronized(lock) { items.values.toList() }

    fun tagsFor(songId: String): Set<String> = synchronized(lock) {
        items[songId]?.tags.orEmpty()
    }
}
