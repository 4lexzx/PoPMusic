package com.popmusic.app.ui.theme

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.glass.LocalAppBackdrop
import com.popmusic.app.ui.glass.echoLiquidGlass
import com.popmusic.app.ui.glass.isLiquidGlassSupported
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

val GlassTint = Color.White.copy(alpha = 0.08f)
val GlassTintStrong = Color.White.copy(alpha = 0.22f)
val GlassHighlight = Color.White.copy(alpha = 0.20f)

/**
 * Radio ÚNICO de blur del cristal: todos los componentes leen este valor.
 * 8 dp = receta de Echo/Apple (a este valor el backdrop se procesa a resolución
 * TOTAL, así la lente, la aberración cromática y el brillo especular se ven
 * nítidos; con 20 dp todo salía pastoso y apagado).
 */
val GlassBlurRadius = 8.dp

val GlassBorderBrush: Brush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.15f)),
)

/** Opacidad global del cristal (0 = transparente, 1 = opaco). Personalizable. */
val LocalGlassOpacity = staticCompositionLocalOf { 0.65f }

/**
 * Efecto "liquid glass" opcional (item 10): cuando está desactivado,
 * [LiquidGlass] cae al cristal normal (tinte + blur), sin la lente líquida.
 * Se provee desde `PopMusicApp` según la preferencia de Ajustes.
 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

/** Verdadero cuando el sistema pide "reducir movimiento". */
val LocalReduceMotion = staticCompositionLocalOf { false }

fun Modifier.blurIfSupported(radius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blur(radius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        this
    }

fun glassBorderBrush(opacity: Float = 1f): Brush = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.58f * opacity),
        Color.White.copy(alpha = 0.14f * opacity),
    ),
)

@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f,
): Modifier {
    val reduceMotion = LocalReduceMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Superficie de cristal "liquid glass": con [liquid] = true (y el switch
 * "Cristal líquido" activado) usa la lente líquida de Echo; con false
 * (por defecto) queda SÓLIDA y opaca, como el resto de la interfaz.
 * El cristal vive SOLO en la barra de navegación (y su mini reproductor).
 */
@Composable
fun LiquidGlass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = GlassTint,
    elevated: Boolean = true,
    elevation: Dp = 18.dp,
    hazeState: HazeState? = null,
    blurRadius: Dp = GlassBlurRadius,
    // Item 13 (tanda 3): la lente líquida queda SOLO para la barra de
    // navegación; el resto de superficies usan cristal normal por defecto.
    liquid: Boolean = false,
    /** Recorre el brillo especular. Solo en superficies grandes: cuesta un redibujado por frame. */
    shine: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val opacity = LocalGlassOpacity.current.coerceIn(0f, 1f)
    val blurStyle = remember(blurRadius) {
        HazeBlurStyle {
            blurRadius(blurRadius)
            noiseFactor(0.03f)
            backgroundColor(Color.Transparent)
        }
    }
    val liquidShape = shape as? CornerBasedShape
    val useLiquidGlass = liquid &&
        liquidShape != null &&
        LocalLiquidGlassEnabled.current &&
        LocalAppBackdrop.current != null &&
        isLiquidGlassSupported()
    val reduceMotion = LocalReduceMotion.current

    // Brillo "líquido": una franja especular muy suave que recorre el cristal y
    // un destello fijo en la esquina superior. Es lo que da la sensación de
    // vidrio pulido del glass de Echo Music sin coste de blur extra.
    val animateSheen = useLiquidGlass && shine && !reduceMotion
    val sheenProgress = if (animateSheen) {
        val sheenTransition = rememberInfiniteTransition(label = "glassSheen")
        val progress by sheenTransition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "glassSheenProgress",
        )
        progress
    } else {
        0.28f
    }

    val surfaceModifier = if (useLiquidGlass) {
        Modifier.echoLiquidGlass(
            shape = liquidShape,
            blurRadiusDp = blurRadius.value,
            lensHeightDp = 24f,
            lensAmountDp = 24f,
            // Receta Echo: tinte ~0.4; menos aún deja ver el fondo (agua clara).
            surfaceOpacity = 0.18f + 0.14f * opacity,
            vibrancy = 1.35f,
            applyEdgeEffects = true,
            applyLens = true,
            applyShadow = false,
        )
    } else {
        // SÓLIDO (tanda 5): el cristal queda SOLO en la barra de
        // navegación con "Cristal líquido" activado (y en el mini, que
        // forma parte de la nav). Todo lo demás —cards, menús, formularios,
        // mensajes— es opaco como el resto de la interfaz. Si la lente
        // líquida no está disponible o está apagada, también cae aquí.
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    }

        Box(
        modifier = modifier
            .then(
                if (elevated && useLiquidGlass) {
                    Modifier.border(
                        1.dp,
                        glassBorderBrush(opacity),
                        shape,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .then(surfaceModifier)
            // Capas SUPLEMENTARIAS muy suaves: SOLO sobre cristal; la
            // superficie sólida queda plana, sin brillos (tanda 5).
            .then(
                if (useLiquidGlass) {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.03f * opacity))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.07f * opacity),
                                    Color.Transparent,
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.05f * opacity),
                                    Color.Transparent,
                                ),
                                start = Offset.Zero,
                                end = Offset(0.6f, 0.4f),
                            ),
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.04f * opacity),
                                ),
                                startY = 220f,
                            ),
                        )
                } else {
                    Modifier
                }
            ),
    ) {
        if (useLiquidGlass) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .offset(x = (-30).dp, y = (-46).dp)
                        .size(205.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f * opacity),
                                    Color.White.copy(alpha = 0.05f * opacity),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 24.dp, y = (-38).dp)
                        .size(150.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.07f * opacity),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                val band = size.width * 0.42f
                val startX = -band + sheenProgress * (size.width + band * 2f)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.07f * opacity),
                            Color.White.copy(alpha = 0.14f * opacity),
                            Color.White.copy(alpha = 0.07f * opacity),
                            Color.Transparent,
                        ),
                        start = Offset(startX, -size.height * 0.15f),
                        end = Offset(startX + band, size.height * 1.15f),
                    ),
                )
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                // Filo especular superior: el "brillo tipo agua" de Echo.
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.70f * opacity),
                            Color.White.copy(alpha = 0.38f * opacity),
                            Color.Transparent,
                        ),
                    ),
                    start = Offset(size.width * 0.10f, 0.75.dp.toPx()),
                    end = Offset(size.width * 0.90f, 0.75.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                )
            }
        }
        content()
    }
}

/** Alias retrocompatible. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color = GlassTint,
    borderBrush: Brush = GlassBorderBrush,
    hazeState: HazeState? = null,
    liquid: Boolean = false,
    shine: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidGlass(
        modifier = modifier,
        shape = shape,
        tint = tint,
        hazeState = hazeState,
        liquid = liquid,
        shine = shine,
        content = content,
    )
}
