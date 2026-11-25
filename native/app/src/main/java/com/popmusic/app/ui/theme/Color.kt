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

    /** Tono vivo del fondo animado: tiñe textos y botones. Null = neutro. */
    var accent by mutableStateOf<Color?>(null)
        internal set

    /** Fondo claro (pastel): superficies claras y textos oscuros. */
    var pastel by mutableStateOf(false)
        internal set

    /**
     * Reproductor, letras y similares pintan su propio fondo oscuro con la
     * carátula: ahí la paleta vuelve a ser la oscura aunque el fondo sea pastel.
     */
    var immersive by mutableStateOf(false)
        internal set

    private val lightMode: Boolean get() = pastel && !immersive

    /**
     * Mezcla [base] hacia el color del fondo. En negro no se nota nada y con
     * fondo de color las superficies toman su tono.
     */
    fun solid(base: Color): Color {
        if (lightMode) {
            // De casi blanco a más teñido según el nivel de la superficie.
            return lerp(Color.White, color, (base.red * 6f).coerceIn(0.2f, 1f))
        }
        val currentStrength = strength
        if (currentStrength <= 0f || (pastel && immersive)) return base
        val fraction = (currentStrength * 0.6f).coerceIn(0f, 0.6f)
        return lerp(base, color, fraction)
    }

    /** Tiñe un color de texto o botón con el acento, sin perder contraste. */
    fun ink(dark: Color, light: Color): Color {
        val tint = accent
        if (isLight) {
            if (tint == null || !lightMode) return light
            return lerp(light, lerp(tint, Color.Black, 0.50f), 0.70f)
        }
        if (tint == null || (pastel && immersive)) return dark
        return lerp(dark, tint, 0.28f)
    }

    /** Luminancia perceptiva aproximada (0 = negro, 1 = blanco). */
    private fun luminance(c: Color): Float =
        0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue

    /**
     * Indica si las superficies efectivas son claras. Es la señal que usa
     * toda la paleta para decidir entre letras negras y blancas. El umbral
     * 0.46 es el punto en que el negro da más contraste que el blanco.
     */
    val isLight: Boolean
        get() = !immersive && (pastel || luminance(solid(SurfaceBase)) > 0.46f)
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
val Ink: Color get() = Ambient.ink(Color(0xFFFFFFFF), Color(0xFF0A0A0C))

/**
 * Color de contenido sobre un relleno de [Ink] (botones sólidos, píldoras,
 * círculo de play...). Es siempre el contraste opuesto a [Ink]: negro sobre
 * botón blanco y blanco sobre botón negro. Sustituye a todos los
 * `Color.Black` fijos que había sobre superficies rellenas.
 */
val OnInk: Color get() = if (Ambient.isLight) Color(0xFFFFFFFF) else Color(0xFF000000)

/** Texto secundario y terciario: siguen a [Ink] para no desaparecer. */
val Muted: Color get() = Ambient.ink(Color(0xFFB4B4BB), Color(0xFF3C3C44))
val Faint: Color get() = Ambient.ink(Color(0xFF74747C), Color(0xFF5E5E66))
val Accent: Color get() = Ambient.ink(Color(0xFFD7D7DE), Color(0xFF35353C))
val Danger = Color(0xFFFF4D4D)

// ————— Colores de marca PoP Music (degradado del logo) —————
/** Azul del logo (esquina superior izquierda). */
val BrandBlue = Color(0xFF2E9BF0)
/** Magenta/rosa del logo (centro). */
val BrandPink = Color(0xFFE04BC8)
/** Naranja del logo (esquina inferior derecha). */
val BrandOrange = Color(0xFFFFA51E)
