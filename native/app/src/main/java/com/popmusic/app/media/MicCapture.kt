package com.popmusic.app.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Micrófono en PCM mono de 16 bits. VOICE_RECOGNITION entrega el sonido sin
 * la reducción de ruido de las llamadas, que se come la música de fondo.
 */
class MicCapture(private val sampleRate: Int) {

    /**
     * Entrega bloques de ~100 ms hasta que [onChunk] devuelva false o se
     * cancele la corrutina. El micrófono se libera siempre al salir.
     */
    suspend fun capture(onChunk: (ShortArray, Int) -> Boolean) =
        withContext(Dispatchers.IO) {
            val record = open()
            try {
                record.startRecording()
                check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    "Otra aplicación está usando el micrófono."
                }
                val chunk = ShortArray(sampleRate / 10)
                while (true) {
                    ensureActive()
                    val read = record.read(chunk, 0, chunk.size)
                    check(read > 0) { "El micrófono dejó de responder." }
                    if (!onChunk(chunk, read)) break
                }
            } finally {
                runCatching { record.stop() }
                record.release()
            }
        }

    @SuppressLint("MissingPermission") // La pantalla lo pide antes de empezar.
    private fun open(): AudioRecord {
        val format = AudioFormat.ENCODING_PCM_16BIT
        val channel = AudioFormat.CHANNEL_IN_MONO
        val size = maxOf(AudioRecord.getMinBufferSize(sampleRate, channel, format), sampleRate * 2)
        for (source in SOURCES) {
            val record = runCatching {
                AudioRecord(source, sampleRate, channel, format, size)
            }.getOrNull() ?: continue
            if (record.state == AudioRecord.STATE_INITIALIZED) return record
            record.release()
        }
        error("No se pudo acceder al micrófono.")
    }

    private companion object {
        val SOURCES = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )
    }
}
