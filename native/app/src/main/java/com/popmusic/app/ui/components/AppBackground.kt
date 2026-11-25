package com.popmusic.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.popmusic.app.data.prefs.BackgroundMode
import com.popmusic.app.data.prefs.BackgroundSettings
import com.popmusic.app.ui.theme.Ambient
import com.popmusic.app.ui.theme.Black
import com.popmusic.app.ui.theme.blurIfSupported
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

data class BackgroundPreset(
    val name: String,
    val colors: List<Color>,
)

/** Degradados desenfocados predefinidos (estilo Apple Music / Spotify). */
val BackgroundPresets: List<BackgroundPreset> = listOf(
    BackgroundPreset(
        "Aurora",
        listOf(Color(0xFF241546), Color(0xFF131C46), Color(0xFF0B0B14)),
    ),
    BackgroundPreset(
        "Atardecer",
        listOf(Color(0xFF4A1E1E), Color(0xFF261434), Color(0xFF120B17)),
    ),
    BackgroundPreset(
        "Océano",
        listOf(Color(0xFF0A2933), Color(0xFF0D2540), Color(0xFF060A12)),
    ),
    BackgroundPreset(
        "Bosque",
        listOf(Color(0xFF0F2C20), Color(0xFF0C261E), Color(0xFF070D0B)),
    ),
    BackgroundPreset(
        "Magma",
        listOf(Color(0xFF3A1710), Color(0xFF26111B), Color(0xFF0F0709)),
    ),
    BackgroundPreset(
        "Grafito",
        listOf(Color(0xFF222228), Color(0xFF171719), Color(0xFF050506)),
    ),
)

private const val TWO_PI = (Math.PI * 2).toFloat()

@Composable
fun AppBackground(
    settings: BackgroundSettings,
    modifier: Modifier = Modifier,
    /** Las vistas previas no deben teñir el tema de toda la app. */
    applyAmbient: Boolean = true,
) {
    val blurDp = (settings.backgroundBlur.coerceIn(0f, 1f) * 56f).dp
    val dim = settings.backgroundDim.coerceIn(0f, 0.92f)

    // Tinte ambiental (tanda 6, punto 5): las superficies sólidas del tema
    // mezclan su color con el fondo activo para que lo absorban.
    val context = LocalContext.current
    val imageUri = if (settings.mode == BackgroundMode.IMAGE) {
        settings.imageUri?.takeIf { it.isNotBlank() }
    } else {
        null
    }
    var imageTint by remember(imageUri) {
        mutableStateOf(imageUri?.let { imageTintCache.get(it) })
    }
    LaunchedEffect(imageUri) {
        if (imageUri == null) {
            imageTint = null
            return@LaunchedEffect
        }
        val cached = imageTintCache.get(imageUri)
        if (cached != null) {
            imageTint = cached
            return@LaunchedEffect
        }
        val extracted = extractImageTint(context, imageUri) ?: return@LaunchedEffect
        imageTintCache.put(imageUri, extracted)
        imageTint = extracted
    }

    if (applyAmbient) SideEffect { applyAmbientPalette(settings, imageTint) }

    Box(modifier = modifier.fillMaxSize().background(Black)) {
        when (settings.mode) {
            BackgroundMode.NONE -> Unit
            BackgroundMode.IMAGE -> {
                val uri = settings.imageUri
                if (uri != null) {
                    AsyncImage(
                        model = Uri.parse(uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blurIfSupported(blurDp),
                    )
                }
            }

            BackgroundMode.ANIMATED ->
                AnimatedBackgroundCanvas(preset = animatedPreset(settings.presetId))

            BackgroundMode.PRESET -> {
                val preset = BackgroundPresets[
                    settings.presetId.coerceIn(0, BackgroundPresets.lastIndex),
                ]
                PresetGradient(colors = preset.colors)
            }
        }

        if (settings.mode != BackgroundMode.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black.copy(alpha = dim)),
            )
        }
    }
}

/** Tiñe la paleta de toda la app según el fondo elegido. */
internal fun applyAmbientPalette(settings: BackgroundSettings, imageTint: Color? = null) {
    val dim = settings.backgroundDim.coerceIn(0f, 0.92f)
    val presetTint = when (settings.mode) {
        BackgroundMode.PRESET -> BackgroundPresets[
            settings.presetId.coerceIn(0, BackgroundPresets.lastIndex),
        ].colors.first()

        BackgroundMode.ANIMATED -> animatedPreset(settings.presetId).colors[1]

        else -> null
    }
    val tint = presetTint ?: imageTint
    val animated = settings.mode == BackgroundMode.ANIMATED
    if (settings.mode == BackgroundMode.NONE || tint == null) {
        // Sin fondo (negro): los colores quedan exactamente como siempre.
        Ambient.color = Black
        Ambient.strength = 0f
        Ambient.accent = null
        Ambient.pastel = false
    } else if (animated) {
        val preset = animatedPreset(settings.presetId)
        // Un pastel muy oscurecido deja de ser claro.
        Ambient.pastel = preset.light && dim < 0.35f
        Ambient.color = lerp(tint, Black, dim)
        Ambient.strength = if (preset.light) 0f else (1f - dim) * 0.75f
        Ambient.accent = preset.accent
    } else {
        Ambient.pastel = false
        Ambient.accent = null
        Ambient.color = tint
        val luminance = 0.2126f * tint.red + 0.7152f * tint.green + 0.0722f * tint.blue
        Ambient.strength = (1f - dim) * (0.30f + 0.70f * luminance)
    }
}

@Composable
private fun PresetGradient(colors: List<Color>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val p = 0.25f
        drawRect(brush = Brush.linearGradient(colors))

        colors.forEachIndexed { index, color ->
            val angle = (p + index * 0.33f) * TWO_PI
            val center = Offset(
                x = size.width * (0.5f + 0.42f * sin(angle)),
                y = size.height * (0.5f + 0.40f * cos(angle * 0.8f + index)),
            )
            val radius = size.minDimension * (0.58f + (index % 2) * 0.10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.92f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}

/** Tinte extraído de la imagen de fondo por URI (evita re-extracciones). */
private val imageTintCache = object : LruCache<String, Color>(8) {}

/** Color dominante de la imagen de fondo (Coil + Palette, sin bloquear UI). */
private suspend fun extractImageTint(context: Context, uri: String): Color? =
    withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(64, 64)
                .build()
            val image = SingletonImageLoader.get(context).execute(request).image
            val bitmap = (image as? BitmapImage)?.bitmap ?: return@runCatching null
            // Palette necesita leer píxeles: los bitmaps "hardware" se copian.
            val sampled = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap
            }
            val palette = Palette.from(sampled)
                .maximumColorCount(6)
                .resizeBitmapArea(64 * 64)
                .generate()
            palette.dominantSwatch?.rgb?.let { Color(it) }
                ?: Color(palette.getDominantColor(0xFF1C1C1E.toInt()))
        }.getOrNull()
    }
