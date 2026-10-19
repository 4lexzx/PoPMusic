package com.popmusic.app

import com.popmusic.app.data.youtube.YouTubeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeRepositoryTest {

    private val repository = YouTubeRepository()

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
