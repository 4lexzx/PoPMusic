package com.popmusic.app.data.recognize

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

/**
 * Huella acústica: localiza los picos del espectrograma (frecuencia y
 * momento) y los empaqueta. Ocupa unos pocos KB: lo que sale del móvil es
 * esta huella, nunca la grabación. Entrada: PCM mono de 16 bits a 16 kHz.
 */
internal class AudioFingerprint {

    private class Peak(val pass: Int, val magnitude: Int, val bin: Int)

    private val samples = FloatArray(FFT)
    private var sampleIndex = 0
    private val spectra = Array(HISTORY) { FloatArray(BINS) }
    private var spectrumIndex = 0
    private val spread = Array(HISTORY) { FloatArray(BINS) }
    private var spreadIndex = 0
    private var passes = 0
    private val bands = Array(4) { ArrayList<Peak>() }
    private val pending = ShortArray(HOP)
    private var pendingCount = 0
    private val re = FloatArray(FFT)
    private val im = FloatArray(FFT)

    var sampleCount = 0
        private set

    /** Audio real oído, sin contar el silencio inicial. */
    val heardMs: Int get() = ((sampleCount - LEAD) * 1000L / RATE).toInt()

    val totalMs: Int get() = (sampleCount * 1000L / RATE).toInt()

    val peakCount: Int get() = bands.sumOf { it.size }

    init {
        // El servicio descarta huellas de menos de 3 s. Con silencio delante
        // se puede preguntar antes, cuando ya hay picos suficientes.
        feed(ShortArray(LEAD), LEAD)
    }

    fun feed(pcm: ShortArray, count: Int) {
        for (i in 0 until count) {
            pending[pendingCount++] = pcm[i]
            if (pendingCount == HOP) {
                pendingCount = 0
                process()
            }
        }
    }

    private fun process() {
        for (i in 0 until HOP) samples[sampleIndex + i] = pending[i].toFloat()
        sampleIndex = (sampleIndex + HOP) and (FFT - 1)
        sampleCount += HOP
        for (i in 0 until FFT) {
            re[i] = samples[(i + sampleIndex) and (FFT - 1)] * HANN[i]
            im[i] = 0f
        }
        fft(re, im)
        val out = spectra[spectrumIndex]
        for (i in 0 until BINS) {
            out[i] = max((re[i] * re[i] + im[i] * im[i]) / (1 shl 17), 1e-10f)
        }
        spectrumIndex = (spectrumIndex + 1) and MASK
        spreadPeaks(out)
        passes++
        if (passes >= 46) findPeaks()
    }

    private fun spreadPeaks(source: FloatArray) {
        val now = spread[spreadIndex]
        source.copyInto(now)
        for (i in 0..BINS - 3) now[i] = max(now[i], max(now[i + 1], now[i + 2]))
        for (i in 0 until BINS) {
            var value = now[i]
            for (back in FORMER) {
                val former = spread[(spreadIndex - back) and MASK]
                former[i] = max(former[i], value)
                value = former[i]
            }
        }
        spreadIndex = (spreadIndex + 1) and MASK
    }

    private fun findPeaks() {
        val fft = spectra[(spectrumIndex - 46) and MASK]
        val near = spread[(spreadIndex - 49) and MASK]
        for (bin in 10..1014) {
            val value = fft[bin]
            if (value < 1f / 64f || value < near[bin - 1]) continue
            var neighbour = 0f
            for (offset in NEAR_BINS) neighbour = max(neighbour, near[bin + offset])
            if (value <= neighbour) continue
            for (offset in OTHER_PASSES) {
                neighbour = max(neighbour, spread[(spreadIndex + offset) and MASK][bin - 1])
            }
            if (value <= neighbour) continue

            val magnitude = level(value)
            val before = level(fft[bin - 1])
            val after = level(fft[bin + 1])
            val curve = magnitude * 2 - before - after
            if (curve <= 0f) continue
            val exact = (bin * 64 + (after - before) * 32 / curve).toInt()
            val hz = (exact * (RATE / 2f / 1024f / 64f)).toInt()
            val band = when (hz) {
                in 250..519 -> 0
                in 520..1449 -> 1
                in 1450..3499 -> 2
                in 3500..5500 -> 3
                else -> continue
            }
            bands[band] += Peak(passes - 46, magnitude.toInt().coerceIn(0, 65535), exact)
        }
    }

    private fun level(value: Float): Float = ln(max(1f / 64f, value)) * 1477.3f + 6144f

    /** Huella lista para consultar, como URI de datos. */
    fun encode(): String {
        val body = ByteArrayOutputStream()
        bands.forEachIndexed { band, peaks ->
            if (peaks.isEmpty()) return@forEachIndexed
            val data = ByteArrayOutputStream()
            var last = 0
            peaks.forEach { peak ->
                if (peak.pass - last >= 255) {
                    data.write(0xff)
                    data.write(int(peak.pass))
                    last = peak.pass
                }
                data.write(peak.pass - last)
                data.write(short(peak.magnitude))
                data.write(short(peak.bin))
                last = peak.pass
            }
            val bytes = data.toByteArray()
            body.write(int(0x60030040 + band))
            body.write(int(bytes.size))
            body.write(bytes)
            repeat((4 - bytes.size % 4) % 4) { body.write(0) }
        }
        val content = body.toByteArray()
        val size = content.size + 8
        val buffer = ByteBuffer.allocate(48 + size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0xcafe2580.toInt())
        buffer.putInt(0) // crc, al final
        buffer.putInt(size)
        buffer.putInt(0x94119c00.toInt())
        repeat(3) { buffer.putInt(0) }
        buffer.putInt(3 shl 27) // 16 kHz
        repeat(2) { buffer.putInt(0) }
        buffer.putInt((sampleCount + RATE * 0.24f).toInt())
        buffer.putInt((15 shl 19) + 0x40000)
        buffer.putInt(0x40000000)
        buffer.putInt(size)
        buffer.put(content)
        val bytes = buffer.array()
        val crc = CRC32().apply { update(bytes, 8, bytes.size - 8) }.value.toInt()
        buffer.putInt(4, crc)
        return "data:audio/vnd.shazam.sig;base64," + Base64.getEncoder().encodeToString(bytes)
    }

    private fun int(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun short(value: Int): ByteArray =
        byteArrayOf(value.toByte(), (value shr 8).toByte())

    companion object {
        const val RATE = 16_000
        private const val LEAD = RATE * 3 / 2
        private const val FFT = 2048
        private const val BINS = FFT / 2 + 1
        private const val HOP = 128
        private const val HISTORY = 256
        private const val MASK = HISTORY - 1
        private val FORMER = intArrayOf(1, 3, 6)
        private val NEAR_BINS = intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)
        private val OTHER_PASSES =
            intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249)
        private val HANN = FloatArray(FFT) { (0.5 * (1 - cos(2 * PI * (it + 1) / (FFT + 1)))).toFloat() }
        private val COS = FloatArray(FFT / 2) { cos(2 * PI * it / FFT).toFloat() }
        private val SIN = FloatArray(FFT / 2) { -sin(2 * PI * it / FFT).toFloat() }

        private fun fft(re: FloatArray, im: FloatArray) {
            var j = 0
            for (i in 1 until FFT) {
                var bit = FFT shr 1
                while (j and bit != 0) {
                    j = j xor bit
                    bit = bit shr 1
                }
                j = j or bit
                if (i < j) {
                    val r = re[i]; re[i] = re[j]; re[j] = r
                    val m = im[i]; im[i] = im[j]; im[j] = m
                }
            }
            var length = 2
            while (length <= FFT) {
                val half = length / 2
                val step = FFT / length
                var start = 0
                while (start < FFT) {
                    for (k in 0 until half) {
                        val wr = COS[k * step]
                        val wi = SIN[k * step]
                        val a = start + k
                        val b = a + half
                        val tr = re[b] * wr - im[b] * wi
                        val ti = re[b] * wi + im[b] * wr
                        re[b] = re[a] - tr
                        im[b] = im[a] - ti
                        re[a] += tr
                        im[a] += ti
                    }
                    start += length
                }
                length *= 2
            }
        }
    }
}
