package com.popmusic.app.ui.screens.player

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fondo del reproductor estilo Echo (PlayerBackgroundStyle.GRADIENT, que es su
 * valor por defecto): degradado vertical de 3 paradas —color extraído de la
 * carátula, el mismo al 60% y negro— con una capa de negro 0.2 encima y fundido
 * de 800 ms al cambiar de canción, igual que Echo.
 *
 * Se superpone a [com.popmusic.app.ui.components.GlassBackdrop], que queda
 * visible mientras se extraen los colores o si la extracción falla.
 */
@Composable
internal fun PlayerPaletteWash(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (artworkUrl.isNullOrBlank()) return

    val context = LocalContext.current
    var colors by remember(artworkUrl) { mutableStateOf(gradientCache.get(artworkUrl)) }

    LaunchedEffect(artworkUrl) {
        val cached = gradientCache.get(artworkUrl)
        if (cached != null) {
            colors = cached
            return@LaunchedEffect
        }
        val extracted = extractGradientColors(context, artworkUrl) ?: return@LaunchedEffect
        gradientCache.put(artworkUrl, extracted)
        colors = extracted
    }

    AnimatedContent(
        targetState = colors,
        transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
        label = "paletteWash",
        modifier = modifier,
    ) { stops ->
        if (stops != null && stops.size >= 3) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to stops[0],
                                0.5f to stops[1],
                                1f to stops[2],
                            ),
                        ),
                    )
                    .background(Color.Black.copy(alpha = 0.2f)),
            )
        }
    }
}

/** Colores del degradado por URL de carátula (evita re-extracciones). */
private val gradientCache = object : LruCache<String, List<Color>>(32) {}

/** Descarga la carátula a 100×100 y extrae el gradiente con Palette. */
private suspend fun extractGradientColors(
    context: Context,
    artworkUrl: String,
): List<Color>? {
    val bitmap = withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .size(100, 100)
                .build()
            val image = SingletonImageLoader.get(context).execute(request).image
            (image as? BitmapImage)?.bitmap
        }.getOrNull()
    } ?: return null

    return withContext(Dispatchers.Default) {
        runCatching {
            // Palette necesita leer los píxeles: los bitmaps "hardware" se copian
            // a ARGB_8888 antes de muestrear.
            val sampled = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap
            }
            val palette = Palette.from(sampled)
                .maximumColorCount(8)
                .resizeBitmapArea(100 * 100)
                .generate()
            gradientColorsOf(palette)
        }.getOrNull()
    }
}

private const val DEFAULT_COLOR = 0xFF1C1C1E.toInt()

/**
 * Tres paradas del degradado: mejor swatch (población × saturación, con el
 * color realzado), el mismo color al 60% y negro al pie.
 */
private fun gradientColorsOf(palette: Palette): List<Color> {
    val candidates = listOfNotNull(
        palette.dominantSwatch,
        palette.vibrantSwatch,
        palette.darkVibrantSwatch,
        palette.lightVibrantSwatch,
        palette.mutedSwatch,
        palette.darkMutedSwatch,
        palette.lightMutedSwatch,
    )
    val best = candidates.maxByOrNull { swatchWeight(it) }
    val fallback = palette.dominantSwatch?.rgb?.let { Color(it) }
        ?: Color(palette.getDominantColor(DEFAULT_COLOR))

    val primary = if (best != null) {
        val bestColor = Color(best.rgb)
        if (isVibrant(bestColor)) boost(bestColor, 1.3f) else boost(fallback, 1.1f)
    } else {
        boost(fallback, 1.1f)
    }

    val mid = primary.copy(
        red = (primary.red * 0.6f).coerceAtLeast(0f),
        green = (primary.green * 0.6f).coerceAtLeast(0f),
        blue = (primary.blue * 0.6f).coerceAtLeast(0f),
    )
    return listOf(primary, mid, Color.Black)
}

private fun isVibrant(color: Color): Boolean {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[1] > 0.25f && hsv[2] > 0.2f && hsv[2] < 0.9f
}

/** Satura un poco y asienta el brillo en una gama legible. */
private fun boost(color: Color, saturationFactor: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = (hsv[1] * saturationFactor).coerceAtMost(1f)
    hsv[2] = (hsv[2] * 0.9f).coerceIn(0.4f, 0.85f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Peso del swatch: población premiada por saturación/brillo y por vivacidad. */
private fun swatchWeight(swatch: Palette.Swatch?): Float {
    if (swatch == null) return 0f
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(swatch.rgb, hsv)
    val saturation = hsv[1]
    val brightness = hsv[2]
    val vibrancyBonus = if (saturation > 0.3f && brightness > 0.3f) 1.5f else 1f
    return swatch.population.toFloat() * 2f * vibrancyBonus * (saturation + brightness) / 2f
}
