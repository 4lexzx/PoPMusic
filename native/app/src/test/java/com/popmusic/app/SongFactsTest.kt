package com.popmusic.app

import com.popmusic.app.data.info.SongFacts
import com.popmusic.app.data.info.SongFactsRepository
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Song
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ficha de canción contra iTunes y Wikipedia (necesita red). */
class SongFactsTest {

    private val repository = SongFactsRepository { name ->
        YouTubeRepository().artists(name).firstOrNull { it.first.equals(name, true) }?.second
    }

    private fun facts(title: String, artist: String, seconds: Long, id: String? = null): SongFacts? = runBlocking {
        repository.facts(Song(id = id ?: "$title|$artist", title = title, artist = artist, durationSeconds = seconds))
            .also {
                println("FICHA $title / $artist -> ${it?.artist} | ${it?.year} | ${it?.album} | ${it?.genre} | ${it?.track} | sube=${it?.uploader} | foto=${it?.artistPhoto != null}")
                println("   BIO ${it?.bio?.take(110)}")
                println("   HISTORIA ${it?.story?.take(110)}")
            }
    }

    @Test fun cancionesConocidas() {
        assertEquals("1990", facts("Rayando el Sol", "Maná", 250, "EDkQB9b3cBw")?.year)
        assertEquals("Bad Bunny", facts("DtMF", "Bad Bunny", 237, "TiebZllW8As")?.artist)
        facts("Always", "Bon Jovi", 353, "5Ot4n64Jpbw")
        facts("The Door (Tiago PZK Version)", "Teddy Swims", 213, "Bkimmn8S18o")
        facts("Motor Y Motivo", "Grupo 5", 250, "jIIRKIcWz2c")
        facts("Besos En Guerra", "Morat", 221)
        facts("Lose Control", "Teddy Swims", 211, "AeedWEyMYYo")
        facts("Bohemian Rhapsody", "Queen", 354)
    }

    @Test fun subidaDeTercero() {
        val info = facts("Maná - Rayando el Sol", "Letras del Recuerdo HD", 250)
        assertEquals("Maná", info?.artist)
        assertEquals("Letras del Recuerdo HD", info?.uploader)
    }

    @Test fun desconocidoSinInventar() {
        val info = facts("Cancioncita casera 8841", "xqzw_usuario_7731", 187)
        assertTrue("Debe responder aunque no haya datos", info != null)
        assertNull(info?.artist)
        assertNull(info?.year)
        assertNull(info?.bio)
        assertNull(info?.story)
    }
}
