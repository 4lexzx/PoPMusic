/**
 * Liquid glass rendering ported from Echo Music (Kyant0/backdrop, Apache 2.0).
 * The backdrop itself lives in [com.popmusic.app.ui.glass.backdrop].
 */
package com.popmusic.app.ui.glass

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.glass.backdrop.Backdrop
import com.popmusic.app.ui.glass.backdrop.drawBackdrop
import com.popmusic.app.ui.glass.backdrop.effects.blur
import com.popmusic.app.ui.glass.backdrop.effects.colorControls
import com.popmusic.app.ui.glass.backdrop.effects.lens
import com.popmusic.app.ui.glass.backdrop.highlight.Highlight
import com.popmusic.app.ui.glass.backdrop.shadow.Shadow

val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private const val MIN_RESOLUTION_SCALE = 0.5f
private const val FULL_QUALITY_BLUR_DP = 20f

private fun glassResolutionScale(blurRadiusDp: Float): Float {
    val t = (blurRadiusDp / FULL_QUALITY_BLUR_DP).coerceIn(0f, 1f)
    return 1f - t * (1f - MIN_RESOLUTION_SCALE)
}

@Composable
fun Modifier.echoLiquidGlass(
    shape: CornerBasedShape,
    blurRadiusDp: Float = 20f,
    lensHeightDp: Float = 20f,
    lensAmountDp: Float = 20f,
    surfaceOpacity: Float = 0.34f,
    vibrancy: Float = 1.15f,
    applyEdgeEffects: Boolean = true,
    applyLens: Boolean = true,
    applyShadow: Boolean = false,
): Modifier {
    if (!isLiquidGlassSupported()) return this
    val backdrop = LocalAppBackdrop.current ?: return this
    val density = LocalDensity.current
    val resolutionScale = glassResolutionScale(blurRadiusDp)
    val blurPx = with(density) { blurRadiusDp.dp.toPx() } * resolutionScale
    val lensHeightPx = with(density) { lensHeightDp.dp.toPx() } * resolutionScale
    val lensAmountPx = with(density) { lensAmountDp.dp.toPx() } * resolutionScale
    val saturation = 1f + 0.5f * vibrancy.coerceIn(0f, 2f)
    val surfaceTint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF101014)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = saturation)
            if (blurPx > 0f) blur(blurPx)
            if (
                applyEdgeEffects &&
                applyLens &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (lensHeightPx > 0f || lensAmountPx > 0f)
            ) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = true,
                    chromaticAberration = true,
                )
            }
        },
        highlight = if (applyEdgeEffects) ({ Highlight.Default }) else null,
        shadow = if (applyShadow) ({ Shadow.Default }) else null,
        onDrawSurface = {
            if (surfaceOpacity > 0f) {
                drawRect(color = surfaceTint.copy(alpha = surfaceOpacity), size = size)
            }
        },
        backdropScale = resolutionScale,
    )
}
