package com.popmusic.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Tinte ambiental (tanda 6, punto 5): las superficies sólidas del tema se
 * mezclan un poco con el color del fondo activo (Ajustes → Fondo) para que
 * "absorban" su tono. Con fondo negro [Ambient.strength] es 0 y todos los
 * colores quedan exactamente iguales a los de siempre.
 *
 * [Ambient.color] y [Ambient.strength] los calcula
 * [com.popmusic.app.ui.components.AppBackground] en cada cambio de fondo.
 */
object Ambient {
    /** Color dominante del fondo activo. */
    var color by mutableStateOf(Color.Black)
        internal set

    /** 0f sin fondo; sube con la luminosidad del fondo y baja con el "dim". */
    var strength by mutableStateOf(0f)
        internal set

    /**
     * Mezcla [base] hacia el color del fondo. El factor 0.35 mantiene el
     * cambio sutil: en negro no se nota nada y con fondo de color las
     * superficies toman su tono.
     */
    fun solid(base: Color): Color {
        val currentColor = color
        val currentStrength = strength
        if (currentStrength <= 0f) return base
        val fraction = (currentStrength * 0.6f).coerceIn(0f, 0.6f)
        return lerp(base, currentColor, fraction)
    }
}

val Black = Color(0xFF000000)
val BgElev: Color get() = Ambient.solid(Color(0xFF0A0A0B))
val Surface: Color get() = Ambient.solid(Color(0xFF0D0D0F))
val Surface2: Color get() = Ambient.solid(Color(0xFF17171A))
val Surface3: Color get() = Ambient.solid(Color(0xFF232327))
val Line: Color get() = Ambient.solid(Color(0xFF1D1D21))
val LineStrong: Color get() = Ambient.solid(Color(0xFF2A2A30))
val Ink = Color(0xFFFFFFFF)
val Muted = Color(0xFFB4B4BB)
val Faint = Color(0xFF74747C)
val Accent = Color(0xFFD7D7DE)
val Danger = Color(0xFFFF4D4D)

// ————— Colores de marca PoP Music (degradado del logo) —————
/** Azul del logo (esquina superior izquierda). */
val BrandBlue = Color(0xFF2E9BF0)
/** Magenta/rosa del logo (centro). */
val BrandPink = Color(0xFFE04BC8)
/** Naranja del logo (esquina inferior derecha). */
val BrandOrange = Color(0xFFFFA51E)
