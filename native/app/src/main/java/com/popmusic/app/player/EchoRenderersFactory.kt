@file:Suppress("UnsafeOptInUsageError")

package com.popmusic.app.player

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor

/**
 * RenderersFactory con la cadena de audio al estilo de Echo.
 *
 * Media3 por defecto monta un [SilenceSkippingAudioProcessor] que conserva
 * hasta 2 s de silencio por tramo (y solo recorta colas largas al final de
 * la pista). Aquí se baja el mínimo de detección a 1 s para que el saltado
 * funcione también en tramos NO instrumentales —pausas entre versos de
 * ~1-2 s— no solo al terminar la canción (item 8), conservando el resto de
 * la receta: recorte al 1 % (20 ms por tramo) y umbral de 256.
 *
 * El procesador solo actúa si el usuario activó "Omitir silencios"
 * (`player.skipSilenceEnabled`, preferencia `skip_silence`).
 */
class EchoRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    @Suppress("DEPRECATION")
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        val silence = SilenceSkippingAudioProcessor(
            MIN_SILENCE_DURATION_US,
            PADDING_SILENCE_US,
            SILENCE_THRESHOLD_LEVEL,
        )
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(
                    emptyArray<AudioProcessor>(),
                    silence,
                    SonicAudioProcessor(),
                ),
            )
            .build()
    }

    private companion object {
        /**
         * Silencio mínimo antes de empezar a recortar: 1 s (antes 2 s, como
         * en Echo). Con 2 s no se detectaban las pausas normales entre versos
         * de las canciones con voz (item 8).
         */
        const val MIN_SILENCE_DURATION_US = 1_000_000L

        /** De ese silencio solo se conserva el 1 % (20 ms de cada segundo). */
        const val PADDING_SILENCE_US = 20_000L

        /** Umbral bajo: también recorta colas con ruido de fondo tenue. */
        const val SILENCE_THRESHOLD_LEVEL: Short = 256
    }
}
