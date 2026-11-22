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

    /** Luminancia perceptiva aproximada (0 = negro, 1 = blanco). */
    private fun luminance(c: Color): Float =
        0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue

    /**
     * Indica si las superficies efectivas son claras. Es la señal que usa
     * toda la paleta para decidir entre letras negras y blancas: con el fondo
     * negro por defecto es siempre false; con un fondo claro (imagen o preset
     * luminoso sin oscurecer) pasa a true. El umbral 0.46 es el punto en que
     * el negro empieza a dar más contraste que el blanco.
     */
    val isLight: Boolean
        get() = luminance(solid(SurfaceBase)) > 0.46f
}

/** Base de [Surface]; se conserva aparte para medir la luminancia efectiva. */
private val SurfaceBase = Color(0xFF0D0D0F)

val Black = Color(0xFF000000)
val BgElev: Color get() = Ambient.solid(Color(0xFF0A0A0B))
val Surface: Color get() = Ambient.solid(SurfaceBase)
val Surface2: Color get() = Ambient.solid(Color(0xFF17171A))
val Surface3: Color get() = Ambient.solid(Color(0xFF232327))
val Line: Color get() = Ambient.solid(Color(0xFF1D1D21))
val LineStrong: Color get() = Ambient.solid(Color(0xFF2A2A30))

/**
 * Color de texto e iconos principal. Blanco sobre fondos oscuros; negro solo
 * cuando el fondo es blanco/claro ([Ambient.isLight]). Antes era blanco fijo
 * y los textos negros sueltos quedaban ilegibles cuando el fondo aclaraba.
 */
val Ink: Color get() = if (Ambient.isLight) Color(0xFF0A0A0C) else Color(0xFFFFFFFF)

/**
 * Color de contenido sobre un relleno de [Ink] (botones sólidos, píldoras,
 * círculo de play...). Es siempre el contraste opuesto a [Ink]: negro sobre
 * botón blanco y blanco sobre botón negro. Sustituye a todos los
 * `Color.Black` fijos que había sobre superficies rellenas.
 */
val OnInk: Color get() = if (Ambient.isLight) Color(0xFFFFFFFF) else Color(0xFF000000)

/** Texto secundario y terciario: siguen a [Ink] para no desaparecer. */
val Muted: Color get() = if (Ambient.isLight) Color(0xFF3C3C44) else Color(0xFFB4B4BB)
val Faint: Color get() = if (Ambient.isLight) Color(0xFF5E5E66) else Color(0xFF74747C)
val Accent: Color get() = if (Ambient.isLight) Color(0xFF35353C) else Color(0xFFD7D7DE)
val Danger = Color(0xFFFF4D4D)

// ————— Colores de marca PoP Music (degradado del logo) —————
/** Azul del logo (esquina superior izquierda). */
val BrandBlue = Color(0xFF2E9BF0)
/** Magenta/rosa del logo (centro). */
val BrandPink = Color(0xFFE04BC8)
/** Naranja del logo (esquina inferior derecha). */
val BrandOrange = Color(0xFFFFA51E)
