package com.popmusic.app

import com.popmusic.app.data.lyrics.LrcLibRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class LrcLibRepositoryTest {

    private val repository = LrcLibRepository()

    @Test
    fun findsSyncedLyrics() = runBlocking {
        val candidates = listOf(
            "The Scientist" to "Coldplay",
            "Coldplay - The Scientist (Official 4K Video)" to "Coldplay",
            "Hymn For The Weekend" to "Coldplay",
        )

        candidates.forEach { (title, artist) ->
            val lyrics = repository.search(title, artist)
            println("LOOKUP title=$title artist=$artist -> lines=${lyrics?.lines?.size} plain=${lyrics?.plainText != null}")
        }

        val lyrics = repository.search("The Scientist", "Coldplay")
        assertNotNull("No se encontraron letras", lyrics)
    }
}
