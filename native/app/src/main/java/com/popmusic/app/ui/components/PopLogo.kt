package com.popmusic.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.popmusic.app.R
import com.popmusic.app.ui.theme.Ink
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/** Proporción del wordmark (3862×902 px). */
private const val LOGO_RATIO = 3862f / 902f

/** Altura objetivo en px -> bitmap ya reescalado (compartido entre pantallas). */
private val logoCache = ConcurrentHashMap<Int, ImageBitmap>()

private val logoLock = Any()

@Volatile
private var master: Bitmap? = null

/**
 * Wordmark "PoP Music" con render nítido.
 *
 * `Image(painterResource(...))` muestreaba el PNG de 3862×902 en UNA sola
 * pasada hacia tamaños de 9-40 dp (reducciones de hasta ~35×) y se veía
 * pixelado. Aquí el maestro se decodifica a media resolución y, por cada
 * tamaño final, se rebaja en pasos de 2× (≈ filtrado por área) hasta quedar
 * a ≤2× del objetivo; el dibujo usa FilterQuality.High (bicúbico).
 */
@Composable
fun PopLogo(
    modifier: Modifier = Modifier,
    contentDescription: String = "PoP Music",
    tint: Color = Ink,
) {
    val context = LocalContext.current
    Canvas(
        modifier = modifier
            // Sin ancho propio el Canvas mediría 0 dentro de un Row; el
            // wordmark conserva su proporción 4.28:1.
            .aspectRatio(LOGO_RATIO)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val targetHeight = size.height.roundToInt().coerceAtLeast(1)
        val image = logoBitmapFor(context, targetHeight) ?: return@Canvas
        val dstWidth = (targetHeight * LOGO_RATIO).roundToInt().coerceAtLeast(1)
        drawImage(
            image = image,
            dstOffset = IntOffset(((size.width - dstWidth) / 2f).roundToInt(), 0),
            dstSize = IntSize(dstWidth, targetHeight),
            filterQuality = FilterQuality.High,
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

/** Devuelve el bitmap reescalado para esa altura (o null si falla el decode). */
private fun logoBitmapFor(context: Context, targetHeight: Int): ImageBitmap? {
    logoCache[targetHeight]?.let { return it }
    val source = loadMaster(context) ?: return null
    val scaled = scaleDown(source, targetHeight).asImageBitmap()
    return logoCache.getOrPut(targetHeight) { scaled }
}

/** Decodifica el PNG una vez, a media resolución (inSampleSize 2). */
private fun loadMaster(context: Context): Bitmap? {
    master?.let { return it }
    synchronized(logoLock) {
        master?.let { return it }
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            // 3862×902 → ≈1931×451: menos memoria y decode más rápido; el
            // logo nunca supera ~140 px de alto en pantalla.
            inSampleSize = 2
        }
        val decoded = BitmapFactory.decodeResource(
            context.applicationContext.resources,
            R.drawable.ic_pop_logo,
            options,
        ) ?: return null
        master = decoded
        return decoded
    }
}

/**
 * Rebaja en pasos de 2× (bilineal ≈ promedio de área) hasta quedar a ≤2× del
 * objetivo: muestrear directo de 451 px a 38 px deja el borde "pixelado".
 */
private fun scaleDown(source: Bitmap, targetHeight: Int): Bitmap {
    var current = source
    while (current.height / 2 >= targetHeight) {
        val half = Bitmap.createScaledBitmap(
            current,
            (current.width / 2).coerceAtLeast(1),
            (current.height / 2).coerceAtLeast(1),
            true,
        )
        if (current !== source) current.recycle()
        current = half
    }
    return current
}
