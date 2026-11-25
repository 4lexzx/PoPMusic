package com.popmusic.app

import com.popmusic.app.data.youtube.YouTubeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fotos de artistas contra YouTube Music (necesita red). */
class ArtistPhotoTest {
    @Test fun encuentraLaFotoDelArtista() = runBlocking {
        for (name in listOf("Bad Bunny", "Maná", "Rosalía", "Grupo Frontera", "The Weeknd", "Quevedo", "Feid")) {
            val found = YouTubeRepository().artists(name)
            println("FOTO $name -> ${found.take(2)}")
            assertTrue(name, found.any { it.first.equals(name, true) && it.second.startsWith("https") })
        }
    }
}
