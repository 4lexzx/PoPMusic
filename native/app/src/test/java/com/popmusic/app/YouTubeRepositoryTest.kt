package com.popmusic.app

import com.popmusic.app.data.youtube.YouTubeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeRepositoryTest {

    private val repository = YouTubeRepository()

    @Test
    fun playlistIdIsParsedFromLinks() {
        assertEquals(
            "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
            repository.playlistIdFrom("https://www.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"),
        )
        assertEquals(
            "RDCLAK5uy_k0p8mK",
            repository.playlistIdFrom("https://music.youtube.com/watch?v=abc&list=RDCLAK5uy_k0p8mK"),
        )
        assertEquals(
            "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI",
            repository.playlistIdFrom("  PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI  "),
        )
        assertNull(repository.playlistIdFrom("https://www.youtube.com/watch?v=k4V3Mo61fJM"))
        assertNull(repository.playlistIdFrom(""))
    }

    @Test
    fun searchReturnsSongs() = runBlocking {
        val results = repository.search("coldplay")
        println("SEARCH_RESULTS=${results.size}")
        results.take(3).forEach { println("SONG=${it.id} | ${it.title} | ${it.artist}") }
        assertTrue("La búsqueda no devolvió resultados", results.isNotEmpty())
    }

    @Test
    fun resolveAudioUrlWorks() = runBlocking {
        val url = repository.resolveAudioUrl("k4V3Mo61fJM")
        println("AUDIO_URL=${url?.take(100)}")
        assertNotNull("No se resolvió una URL de audio", url)
    }
}
