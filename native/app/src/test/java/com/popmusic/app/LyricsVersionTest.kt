package com.popmusic.app

import com.popmusic.app.data.lyrics.LrcLibRepository
import com.popmusic.app.data.lyrics.LyricsMatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsVersionTest {

    private fun first(title: String, artist: String, seconds: Long): String = runBlocking {
        val lyrics = LrcLibRepository().search(title, artist, seconds * 1000L)
        val text = lyrics?.lines?.firstOrNull { it.text.isNotBlank() }?.text.orEmpty()
        println("LETRA $title / $artist -> $text (${lyrics?.lines?.size} lineas)")
        text
    }

    @Test fun versionConColaborador() {
        assertTrue(first("The Door (Tiago PZK Version)", "Teddy Swims", 213).startsWith("Siento"))
    }

    @Test fun versionNormal() {
        assertTrue(first("The Door", "Teddy Swims", 213).startsWith("I took"))
    }

    @Test fun remixYOriginal() {
        val original = first("Despacito", "Luis Fonsi", 229)
        val remix = first("Despacito (Remix)", "Luis Fonsi", 229)
        assertTrue(original.isNotBlank() && remix.isNotBlank())
        assertTrue("El remix empieza en inglés con Bieber", remix != original)
    }

    @Test fun edicionesNeutrasYVideos() {
        assertTrue(first("Oye Mi Amor (2019 Remasterizado)", "Maná", 274).isNotBlank())
        assertTrue(first("Numb (Official Music Video)", "Linkin Park", 187).isNotBlank())
        assertTrue(first("Bailando (Spanish Version) (con Descemer Bueno y Gente De Zona)", "Enrique Iglesias", 243).isNotBlank())
        assertTrue(first("Calma (Remix)", "Pedro Capó", 239).isNotBlank())
    }

    @Test fun etiquetasDeVersion() {
        assertEquals(setOf("tiago", "pzk"), LyricsMatcher.versionTokens("The Door (Tiago PZK Version)"))
        assertEquals(setOf("tiago", "pzk"), LyricsMatcher.versionTokens("The Door - Tiago PZK Version"))
        assertEquals(emptySet<String>(), LyricsMatcher.versionTokens("Nothing Else Matters (Radio Edit)"))
        assertEquals(emptySet<String>(), LyricsMatcher.versionTokens("Numb (Official Music Video)"))
        assertEquals(emptySet<String>(), LyricsMatcher.versionTokens("Forever Young"))
    }
}
