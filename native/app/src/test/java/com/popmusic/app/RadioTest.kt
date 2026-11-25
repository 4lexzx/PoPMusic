package com.popmusic.app

import com.popmusic.app.data.library.LibraryState
import com.popmusic.app.data.recommendations.RecommendationsRepository
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueba la radio contra YouTube (necesita red). */
class RadioTest {

    private fun check(id: String, title: String, artist: String) = runBlocking {
        val radio = RecommendationsRepository()
        val served = ArrayList<Song>()
        repeat(4) { turn ->
            val start = System.currentTimeMillis()
            val batch = radio.radio(
                library = LibraryState(),
                excludeIds = served.mapTo(hashSetOf(id)) { it.id },
                seedSong = Song(id, title, artist),
            )
            println("$title lote ${turn + 1}: ${batch.size} en ${System.currentTimeMillis() - start} ms")
            batch.forEach { println("   ${it.title} / ${it.artist} / ${it.durationSeconds}s") }
            assertTrue("Lote vacío", batch.isNotEmpty())
            served += batch
        }
        assertEquals("Se repiten canciones", served.size, served.distinctBy { it.id }.size)
    }

    @Test fun rockEnEspanol() = check("EDkQB9b3cBw", "Rayando el Sol", "Maná")
    @Test fun metal() = check("tlO3lZhDukU", "One", "Metallica")
    @Test fun urbano() = check("TiebZllW8As", "DtMF", "Bad Bunny")
    @Test fun rockIngles() = check("5Ot4n64Jpbw", "Always", "Bon Jovi")
    @Test fun cumbia() = check("jIIRKIcWz2c", "Motor Y Motivo", "Grupo 5")
    @Test fun semillaVideoclip() = check("kXYiU_JCYtU", "Numb (Official Music Video)", "Linkin Park")
    @Test fun videoclipEnEspanol() = check("kJQP7kiw5Fk", "Luis Fonsi - Despacito ft. Daddy Yankee", "Luis Fonsi")
    @Test fun kpop() = check("gdZLi9oWNZg", "BTS (방탄소년단) 'Dynamite' Official MV", "HYBE LABELS")

    @Test
    fun idNoYoutubeNoFalla() = runBlocking {
        assertTrue(YouTubeRepository().musicRadio("podcast:1").songs.isEmpty())
    }
}
