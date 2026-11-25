package com.popmusic.app

import com.popmusic.app.data.recognize.AudioFingerprint
import com.popmusic.app.data.recognize.RecognitionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reconoce los PCM (16 kHz mono) de POP_PCM; sin esa carpeta se omite. */
class RecognitionTest {
    @Test fun reconoce() = runBlocking {
        val dir = System.getenv("POP_PCM")?.let(::File)
        assumeTrue(dir?.isDirectory == true)
        val repository = RecognitionRepository()
        repository.warmUp()
        dir!!.listFiles { f -> f.extension == "pcm" }!!.sorted().forEach { file ->
            val shorts = ShortArray((file.length() / 2).toInt())
            ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            for (seconds in listOf(1.7, 2.3, 3.1, 4.5, 7.0, 11.0)) {
                val print = AudioFingerprint()
                val t0 = System.currentTimeMillis()
                print.feed(shorts, (seconds * 16000).toInt().coerceAtMost(shorts.size))
                val uri = print.encode()

                val t1 = System.currentTimeMillis()
                val result = repository.recognize(uri, print.totalMs)
                Thread.sleep(1200)
                println("RECO ${file.name} ${seconds}s picos=${print.peakCount} huella=${t1 - t0}ms red=${System.currentTimeMillis() - t1}ms -> $result")
                if (result is RecognitionRepository.Result.Match) break
            }
        }
    }

    /** Búsqueda real de videos; solo con POP_PCM definido (usa red). */
    @Test fun videos() = runBlocking {
        assumeTrue(System.getenv("POP_PCM") != null)
        val youtube = com.popmusic.app.data.youtube.YouTubeRepository()
        listOf(
            "la nave del olvido mon laferte", "la nave del olvido", "bad bunny",
            "canciones de frutas", "baby shark", "blinding lights",
        ).forEach { term ->
            val songs = youtube.search(term)
            val found = youtube.musicVideos(term) + youtube.searchVideos(term)
            println("VID == $term canciones=${songs.size} videos=${found.size}")
            com.popmusic.app.data.filter.RelatedVideos.pick(term, found, songs)
                .forEach { println("VID    ${it.title} | ${it.artist} | ${it.durationSeconds}") }
        }
    }
}
