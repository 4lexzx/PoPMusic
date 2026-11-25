package com.popmusic.app.ui.screens.player

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.popmusic.app.ui.theme.BgElev
import com.popmusic.app.ui.theme.LocalReduceMotion
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
    /** Los colores de la carátula se mueven despacio por el fondo. */
    motion: Boolean = true,
) {
    if (artworkUrl.isNullOrBlank()) {
        // Sin carátula: base OPACA para no dejar ver la pantalla de atrás
        // a través del glass semitransparente.
        Box(modifier = modifier.fillMaxSize().background(BgElev))
        return
    }

    val context = LocalContext.current
    val moving = motion && !LocalReduceMotion.current
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

    // Base opaca: mientras se extraen los colores (o si la extracción falla)
    // el reproductor no deja ver la pantalla de atrás a través del glass.
    Box(modifier = modifier.fillMaxSize().background(BgElev)) {
        AnimatedContent(
            targetState = colors,
            transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
            label = "paletteWash",
        ) { stops ->
            if (stops != null && stops.size >= 3) {
                LivingGradient(stops, moving)
            }
        }
    }
}

/**
 * El degradado de siempre (color → color oscuro → negro) con manchas de los
 * otros tonos de la carátula flotando encima. Abajo se funde a negro para que
 * los controles se lean igual que antes.
 */
@Composable
private fun LivingGradient(stops: List<Color>, moving: Boolean) {
    val time = remember { mutableFloatStateOf(4f) }
    LaunchedEffect(moving) {
        if (!moving) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            delay(WASH_FRAME_MS)
            withFrameNanos { now ->
                time.floatValue += ((now - last) / 1e9f).coerceAtMost(0.1f)
                last = now
            }
        }
    }
    val accents = stops.drop(3).ifEmpty { listOf(stops[0], stops[1]) }
    // El tiempo solo se lee al dibujar: nada se recompone en cada fotograma.
    Canvas(Modifier.fillMaxSize()) {
        val t = time.floatValue
        drawRect(
            Brush.verticalGradient(0f to stops[0], 0.5f to stops[1], 1f to stops[2]),
        )
        repeat(5) { i ->
            val color = if (i == 4) stops[0] else accents[i % accents.size]
            val speed = 0.34f + i * 0.06f
            val center = Offset(
                size.width * (0.5f + 0.55f * sin(t * speed + i * 1.9f)),
                size.height * (0.38f + 0.34f * cos(t * speed * 0.85f + i * 2.3f)),
            )
            val radius = size.maxDimension * (0.36f + 0.09f * sin(t * 0.45f + i))
            drawCircle(
                Brush.radialGradient(
                    listOf(color.copy(alpha = 0.88f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        drawRect(Color.Black.copy(alpha = 0.12f))
        drawRect(
            Brush.verticalGradient(
                0.50f to Color.Transparent,
                0.80f to Color.Black.copy(alpha = 0.55f),
                1f to Color.Black.copy(alpha = 0.92f),
            ),
        )
    }
}

private const val WASH_FRAME_MS = 30L

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
    // Otros tonos de la carátula, asentados para que el texto blanco se lea.
    val accents = candidates
        .sortedByDescending { swatchWeight(it) }
        .map { Color(it.rgb) }
        .filter { colorDistance(it, primary) > 0.18f }
        .distinct()
        .take(3)
        .map(::settle)
    // Carátulas de un solo color: variantes vecinas para que se note el movimiento.
    val variants = listOf(shift(primary, 24f, 0.9f), shift(primary, -24f, 0.62f), shift(primary, 8f, 0.42f))
    return listOf(primary, mid, Color.Black) + (accents + variants).take(4)
}

private fun colorDistance(a: Color, b: Color): Float =
    abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)

private fun shift(color: Color, degrees: Float, brightness: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    hsv[2] = (hsv[2] * brightness).coerceIn(0.14f, 0.7f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Limita el brillo del tono. */
private fun settle(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = (hsv[1] * 1.15f).coerceAtMost(1f)
    hsv[2] = hsv[2].coerceIn(0.18f, 0.66f)
    return Color(android.graphics.Color.HSVToColor(hsv))
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
