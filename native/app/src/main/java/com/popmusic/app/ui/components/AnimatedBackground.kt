package com.popmusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.popmusic.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class MotionKind {
    MIST, WAVES, STARS, AURORA, EMBERS, RINGS, HEARTS, PETALS, BUBBLES, RAIN, SNOW, LINES, DOTS, GRID, BREATH,
}

enum class BackgroundSection(val label: String) {
    DARK("Oscuros"),
    HER("Para ella"),
    PASTEL("Pastel"),
    HIM("Para él"),
    MINIMAL("Minimalistas"),
    NATURE("Naturaleza"),
}

class AnimatedPreset(
    val name: String,
    val kind: MotionKind,
    val section: BackgroundSection,
    base: Long,
    first: Long,
    second: Long,
    accent: Long,
) {
    /** Base y dos tonos. */
    val colors: List<Color> = listOf(Color(base), Color(first), Color(second))

    /** Tono con el que se tiñen textos y botones. */
    val accent: Color = Color(accent)

    /** Fondo claro: la app pasa a textos oscuros. */
    val light: Boolean = colors[0].let { 0.2126f * it.red + 0.7152f * it.green + 0.0722f * it.blue } > 0.5f
}

/** El índice se guarda en preferencias: añadir SIEMPRE al final. */
val AnimatedPresets: List<AnimatedPreset> = listOf(
    AnimatedPreset("Niebla", MotionKind.MIST, BackgroundSection.DARK, 0xFF050506, 0xFF3C3C44, 0xFF232327, 0xFFD7D7DE),
    AnimatedPreset("Estrellas", MotionKind.STARS, BackgroundSection.DARK, 0xFF020203, 0xFF20222E, 0xFFD7D7DE, 0xFFC9CFE6),
    AnimatedPreset("Marea", MotionKind.WAVES, BackgroundSection.DARK, 0xFF040506, 0xFF2A3540, 0xFF1A232B, 0xFFA9BFD1),
    AnimatedPreset("Medianoche", MotionKind.AURORA, BackgroundSection.DARK, 0xFF030409, 0xFF21315E, 0xFF28204F, 0xFFA9B8F5),
    AnimatedPreset("Brasas", MotionKind.EMBERS, BackgroundSection.DARK, 0xFF060403, 0xFF47200F, 0xFFB0623F, 0xFFF0B08E),
    AnimatedPreset("Abismo", MotionKind.AURORA, BackgroundSection.DARK, 0xFF020606, 0xFF11403F, 0xFF122C42, 0xFF8FD6D2),
    AnimatedPreset("Vinilo", MotionKind.RINGS, BackgroundSection.DARK, 0xFF040405, 0xFF26262B, 0xFF50505A, 0xFFD7D7DE),
    AnimatedPreset("Vino", MotionKind.MIST, BackgroundSection.DARK, 0xFF060304, 0xFF4C1726, 0xFF2C122A, 0xFFF0A3B8),
    AnimatedPreset("Rosa sueño", MotionKind.AURORA, BackgroundSection.HER, 0xFFFFE6EF, 0xFFFFB3CF, 0xFFF9C6E6, 0xFFC2185B),
    AnimatedPreset("Corazones", MotionKind.HEARTS, BackgroundSection.HER, 0xFFFFEAF1, 0xFFFFC4D8, 0xFFF58FB4, 0xFFC2185B),
    AnimatedPreset("Pétalos", MotionKind.PETALS, BackgroundSection.HER, 0xFFFFF1F0, 0xFFFFD3D6, 0xFFF3A0B0, 0xFFB83B5E),
    AnimatedPreset("Rosa noche", MotionKind.HEARTS, BackgroundSection.HER, 0xFF16060D, 0xFF4A1530, 0xFFD6588C, 0xFFFF9CC2),
    AnimatedPreset("Lila noche", MotionKind.AURORA, BackgroundSection.HER, 0xFF0B0714, 0xFF3C2260, 0xFF5A2A62, 0xFFD3B0FF),
    AnimatedPreset("Oro rosa", MotionKind.EMBERS, BackgroundSection.HER, 0xFF140A0A, 0xFF54302C, 0xFFE3A48F, 0xFFF7C9B8),
    AnimatedPreset("Fucsia", MotionKind.WAVES, BackgroundSection.HER, 0xFF14040F, 0xFF5E1445, 0xFF7A1D52, 0xFFFF8AD0),
    AnimatedPreset("Coqueta", MotionKind.PETALS, BackgroundSection.HER, 0xFF1A0A12, 0xFF5A2038, 0xFFF2A7C3, 0xFFFFC1D9),
    AnimatedPreset("Algodón", MotionKind.MIST, BackgroundSection.PASTEL, 0xFFFBF1FA, 0xFFFFC9E3, 0xFFC3DEFF, 0xFF8E4FA8),
    AnimatedPreset("Lavanda", MotionKind.BUBBLES, BackgroundSection.PASTEL, 0xFFF1ECFF, 0xFFD9CCFF, 0xFFB9A4F2, 0xFF5E3FBF),
    AnimatedPreset("Menta", MotionKind.WAVES, BackgroundSection.PASTEL, 0xFFE8F8F1, 0xFFBFEBD8, 0xFFA3DFC8, 0xFF1F7A5C),
    AnimatedPreset("Cielo", MotionKind.BUBBLES, BackgroundSection.PASTEL, 0xFFE8F3FF, 0xFFC4E0FF, 0xFF93C3F5, 0xFF1E5FA8),
    AnimatedPreset("Durazno", MotionKind.AURORA, BackgroundSection.PASTEL, 0xFFFFF0E6, 0xFFFFD0B0, 0xFFFFC0B8, 0xFFB4512A),
    AnimatedPreset("Vainilla", MotionKind.BREATH, BackgroundSection.PASTEL, 0xFFFFF9E6, 0xFFFFEBB0, 0xFFFFE0A0, 0xFF8A6410),
    AnimatedPreset("Acero", MotionKind.GRID, BackgroundSection.HIM, 0xFF06080A, 0xFF1C252E, 0xFF6C8299, 0xFFA9C0D6),
    AnimatedPreset("Carbono", MotionKind.LINES, BackgroundSection.HIM, 0xFF050505, 0xFF1B1B1E, 0xFF77777F, 0xFFD7D7DE),
    AnimatedPreset("Cobalto", MotionKind.AURORA, BackgroundSection.HIM, 0xFF02050D, 0xFF0F2E6E, 0xFF0B2148, 0xFF8FB4FF),
    AnimatedPreset("Militar", MotionKind.MIST, BackgroundSection.HIM, 0xFF060805, 0xFF2C3820, 0xFF1D2818, 0xFFBFCB9E),
    AnimatedPreset("Carmesí", MotionKind.EMBERS, BackgroundSection.HIM, 0xFF080203, 0xFF4A0B10, 0xFFC0323C, 0xFFFF8A8F),
    AnimatedPreset("Lluvia", MotionKind.RAIN, BackgroundSection.HIM, 0xFF04070A, 0xFF14222E, 0xFF6F93AD, 0xFFA8C8DE),
    AnimatedPreset("Asfalto", MotionKind.WAVES, BackgroundSection.HIM, 0xFF060606, 0xFF2B2B2E, 0xFF1C1C1F, 0xFFD0D0D6),
    AnimatedPreset("Línea", MotionKind.LINES, BackgroundSection.MINIMAL, 0xFF030303, 0xFF101012, 0xFF9A9AA2, 0xFFFFFFFF),
    AnimatedPreset("Puntos", MotionKind.DOTS, BackgroundSection.MINIMAL, 0xFF030303, 0xFF111113, 0xFF8C8C95, 0xFFFFFFFF),
    AnimatedPreset("Respiro", MotionKind.BREATH, BackgroundSection.MINIMAL, 0xFF050506, 0xFF2A2A30, 0xFF17171A, 0xFFD7D7DE),
    AnimatedPreset("Papel", MotionKind.DOTS, BackgroundSection.MINIMAL, 0xFFF4F3F0, 0xFFE6E4DE, 0xFFA9A59A, 0xFF4A463C),
    AnimatedPreset("Arena", MotionKind.LINES, BackgroundSection.MINIMAL, 0xFFF5EEE3, 0xFFE8DAC4, 0xFFBFA47C, 0xFF6E5430),
    AnimatedPreset("Perla", MotionKind.BREATH, BackgroundSection.MINIMAL, 0xFFF3F4F6, 0xFFE0E3E9, 0xFFD3D8E0, 0xFF404857),
    AnimatedPreset("Nieve", MotionKind.SNOW, BackgroundSection.NATURE, 0xFF05080E, 0xFF16243A, 0xFFD6E4F5, 0xFFB9D2F0),
    AnimatedPreset("Bosque", MotionKind.MIST, BackgroundSection.NATURE, 0xFF030705, 0xFF16402F, 0xFF0F2C28, 0xFF9BD6B5),
    AnimatedPreset("Luciérnagas", MotionKind.EMBERS, BackgroundSection.NATURE, 0xFF040703, 0xFF1B2F14, 0xFFC8D86A, 0xFFDDEB9A),
    AnimatedPreset("Fondo del mar", MotionKind.BUBBLES, BackgroundSection.NATURE, 0xFF02070C, 0xFF0B2F45, 0xFF3E8FB0, 0xFF8FD3EE),
    AnimatedPreset("Atardecer", MotionKind.WAVES, BackgroundSection.NATURE, 0xFF0D0508, 0xFF5A2230, 0xFF7A3A22, 0xFFFFB08A),
    AnimatedPreset("Cerezos", MotionKind.PETALS, BackgroundSection.NATURE, 0xFF0D070B, 0xFF38202C, 0xFFE7A9BE, 0xFFF7C4D5),
)

fun animatedPreset(id: Int): AnimatedPreset =
    AnimatedPresets[id.coerceIn(0, AnimatedPresets.lastIndex)]

private const val TAU = (PI * 2).toFloat()

/** ~25 fps bastan para movimientos lentos y gastan menos batería. */
private const val FRAME_MS = 32L

private class Particle(val x: Float, val y: Float, val size: Float, val phase: Float, val speed: Float)

@Composable
fun AnimatedBackgroundCanvas(
    preset: AnimatedPreset,
    modifier: Modifier = Modifier,
    running: Boolean = true,
) {
    val still = LocalReduceMotion.current || !running
    val time = remember { mutableFloatStateOf(6f) }
    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            // Entre pasos no se pide ningún fotograma: así no gasta batería.
            delay(FRAME_MS)
            withFrameNanos { now ->
                // Se limita el salto al volver de segundo plano.
                time.floatValue += ((now - last) / 1e9f).coerceAtMost(0.1f)
                last = now
            }
        }
    }
    val particles = remember(preset) {
        val random = Random(preset.name.hashCode())
        List(
            when (preset.kind) {
                MotionKind.STARS -> 90
                MotionKind.SNOW, MotionKind.RAIN -> 60
                MotionKind.HEARTS, MotionKind.PETALS, MotionKind.BUBBLES -> 22
                else -> 34
            },
        ) {
            Particle(
                random.nextFloat(), random.nextFloat(), random.nextFloat(),
                random.nextFloat() * TAU, 0.4f + random.nextFloat() * 0.6f,
            )
        }
    }
    val path = remember { Path() }

    // El tiempo solo se lee al DIBUJAR: no recompone nada en cada fotograma.
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = time.floatValue
        val (base, first, second) = preset.colors
        drawRect(base)
        when (preset.kind) {
            MotionKind.MIST -> mist(t, first, second)
            MotionKind.AURORA -> aurora(t, first, second)
            MotionKind.WAVES -> waves(t, first, second, path)
            MotionKind.STARS -> stars(t, first, second, particles)
            MotionKind.EMBERS -> embers(t, first, second, particles)
            MotionKind.RINGS -> rings(t, first, second)
            MotionKind.HEARTS -> rising(t, first, second, particles) { center, r, color, turn ->
                heart(path, center, r * 1.3f, color, turn)
            }
            MotionKind.BUBBLES -> rising(t, first, second, particles) { center, r, color, _ ->
                drawCircle(color.copy(alpha = color.alpha * 0.35f), r * 1.4f, center)
                drawCircle(color, r * 1.4f, center, style = Stroke(width = size.minDimension / 260f))
            }
            MotionKind.PETALS -> falling(t, first, second, particles, 0.030f) { center, r, color, turn ->
                rotate(turn * 57.3f, center) {
                    drawOval(
                        color,
                        topLeft = Offset(center.x - r * 1.5f, center.y - r * 0.8f),
                        size = Size(r * 3f, r * 1.6f),
                    )
                }
            }
            MotionKind.SNOW -> falling(t, first, second, particles, 0.030f) { center, r, color, _ ->
                drawCircle(color, r * 0.35f, center)
            }
            MotionKind.RAIN -> rain(t, first, second, particles)
            MotionKind.LINES -> lines(t, first, second, path)
            MotionKind.DOTS -> dots(t, first, second)
            MotionKind.GRID -> grid(t, first, second)
            MotionKind.BREATH -> breath(t, first, second)
        }
    }
}

private fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.mist(t: Float, a: Color, b: Color) {
    repeat(4) { i ->
        val angle = t * (0.10f + i * 0.025f) + i * 1.7f
        val center = Offset(
            size.width * (0.5f + 0.42f * cos(angle)),
            size.height * (0.5f + 0.38f * sin(angle * 0.8f + i)),
        )
        glow(center, size.maxDimension * (0.42f + 0.05f * sin(t * 0.2f + i)), if (i % 2 == 0) a else b, 0.9f)
    }
}

private fun DrawScope.aurora(t: Float, a: Color, b: Color) {
    repeat(5) { i ->
        val sway = sin(t * 0.22f + i * 1.3f)
        val center = Offset(
            size.width * (0.1f + i * 0.2f + 0.14f * sway),
            size.height * (0.16f + 0.10f * cos(t * 0.17f + i)),
        )
        glow(center, size.maxDimension * 0.34f, if (i % 2 == 0) a else b, 0.75f + 0.2f * sway)
    }
    glow(
        Offset(size.width * (0.5f + 0.3f * cos(t * 0.13f)), size.height),
        size.maxDimension * 0.4f, b, 0.6f,
    )
}

private fun DrawScope.waves(t: Float, a: Color, b: Color, path: Path) {
    repeat(4) { i ->
        val top = size.height * (0.42f + i * 0.14f)
        val amplitude = size.height * (0.030f + 0.006f * i)
        path.rewind()
        path.moveTo(0f, top)
        val steps = 24
        for (s in 0..steps) {
            val x = size.width * s / steps
            val phase = s.toFloat() / steps * TAU * (0.8f + i * 0.15f)
            path.lineTo(x, top + amplitude * sin(phase + t * (0.35f + i * 0.08f) + i * 1.4f))
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
        val color = if (i % 2 == 0) a else b
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.1f)),
                startY = top - amplitude,
                endY = size.height,
            ),
        )
    }
}

private fun DrawScope.stars(t: Float, a: Color, b: Color, particles: List<Particle>) {
    glow(Offset(size.width * 0.7f, size.height * 0.2f), size.maxDimension * 0.5f, a, 1f)
    val unit = size.minDimension / 400f
    particles.forEach { p ->
        val twinkle = 0.5f + 0.5f * sin(t * p.speed * 1.4f + p.phase)
        val x = ((p.x + t * 0.0035f * p.speed) % 1f) * size.width
        drawCircle(
            b.copy(alpha = 0.10f + 0.55f * twinkle * (0.3f + 0.7f * p.size)),
            radius = unit * (0.7f + 1.7f * p.size),
            center = Offset(x, p.y * size.height),
        )
    }
}

private fun DrawScope.embers(t: Float, a: Color, b: Color, particles: List<Particle>) {
    glow(Offset(size.width * 0.5f, size.height * 1.02f), size.maxDimension * 0.55f, a, 1f)
    val unit = size.minDimension / 400f
    particles.forEach { p ->
        val life = (p.y + t * 0.035f * p.speed) % 1f
        val x = (p.x + 0.04f * sin(t * 0.5f * p.speed + p.phase)) * size.width
        drawCircle(
            b.copy(alpha = (0.55f * sin(life * PI.toFloat())).coerceIn(0f, 1f)),
            radius = unit * (1.0f + 2.2f * p.size),
            center = Offset(x, size.height * (1.05f - life * 0.95f)),
        )
    }
}

private fun DrawScope.rings(t: Float, a: Color, b: Color) {
    val center = Offset(size.width * 0.5f, size.height * 0.34f)
    val max = size.maxDimension * 0.75f
    glow(center, max, a, 1f)
    repeat(9) { i ->
        val progress = ((i / 9f) + t * 0.018f) % 1f
        drawCircle(
            b.copy(alpha = 0.55f * sin(progress * PI.toFloat()).coerceAtLeast(0f)),
            radius = max * progress,
            center = center,
            style = Stroke(width = size.minDimension / 300f),
        )
    }
    // Destello suave que gira, como la luz sobre un vinilo.
    val angle = t * 0.25f
    glow(
        Offset(center.x + max * 0.35f * cos(angle), center.y + max * 0.35f * sin(angle)),
        max * 0.5f, b, 0.22f,
    )
}

/** Formas que suben despacio balanceándose. */
private inline fun DrawScope.rising(
    t: Float, a: Color, b: Color, particles: List<Particle>,
    shape: DrawScope.(Offset, Float, Color, Float) -> Unit,
) {
    glow(Offset(size.width * 0.3f, size.height * 0.9f), size.maxDimension * 0.6f, a, 1f)
    glow(Offset(size.width * 0.8f, size.height * 0.1f), size.maxDimension * 0.5f, a, 0.8f)
    val unit = size.minDimension / 400f
    particles.forEach { p ->
        val life = (p.y + t * 0.022f * p.speed) % 1f
        val x = (p.x + 0.05f * sin(t * 0.4f * p.speed + p.phase)) * size.width
        val alpha = 0.60f * sin(life * PI.toFloat()).coerceAtLeast(0f)
        shape(
            Offset(x, size.height * (1.08f - life * 1.16f)),
            unit * (4f + 11f * p.size),
            b.copy(alpha = alpha),
            0.25f * sin(t * 0.5f + p.phase),
        )
    }
}

/** Formas que caen girando. */
private inline fun DrawScope.falling(
    t: Float, a: Color, b: Color, particles: List<Particle>, speed: Float,
    shape: DrawScope.(Offset, Float, Color, Float) -> Unit,
) {
    glow(Offset(size.width * 0.7f, size.height * 0.15f), size.maxDimension * 0.6f, a, 1f)
    val unit = size.minDimension / 400f
    particles.forEach { p ->
        val life = (p.y + t * speed * p.speed) % 1f
        val x = (p.x + 0.07f * sin(t * 0.35f * p.speed + p.phase)) * size.width
        val alpha = 0.65f * sin(life * PI.toFloat()).coerceAtLeast(0f)
        shape(
            Offset(x, size.height * (life * 1.1f - 0.05f)),
            unit * (4f + 6f * p.size),
            b.copy(alpha = alpha),
            p.phase + t * 0.4f * p.speed,
        )
    }
}

private fun DrawScope.heart(path: Path, c: Offset, r: Float, color: Color, turn: Float) {
    path.rewind()
    path.moveTo(c.x, c.y + r)
    path.cubicTo(c.x - r * 1.9f, c.y - r * 0.2f, c.x - r * 0.9f, c.y - r * 1.5f, c.x, c.y - r * 0.5f)
    path.cubicTo(c.x + r * 0.9f, c.y - r * 1.5f, c.x + r * 1.9f, c.y - r * 0.2f, c.x, c.y + r)
    path.close()
    rotate(turn * 57.3f, c) { drawPath(path, color) }
}

private fun DrawScope.rain(t: Float, a: Color, b: Color, particles: List<Particle>) {
    glow(Offset(size.width * 0.5f, 0f), size.maxDimension * 0.7f, a, 1f)
    particles.forEach { p ->
        val life = (p.y + t * (0.22f + 0.25f * p.speed)) % 1f
        val y = size.height * (life * 1.2f - 0.1f)
        val length = size.height * (0.02f + 0.03f * p.size)
        val x = p.x * size.width
        drawLine(
            b.copy(alpha = 0.08f + 0.22f * p.size),
            Offset(x, y), Offset(x - length * 0.12f, y + length),
            strokeWidth = size.minDimension / 360f,
        )
    }
}

private fun DrawScope.lines(t: Float, a: Color, b: Color, path: Path) {
    glow(Offset(size.width * 0.5f, size.height * 0.5f), size.maxDimension * 0.6f, a, 1f)
    repeat(7) { i ->
        val y = size.height * (0.36f + i * 0.05f)
        val amplitude = size.height * 0.05f * (1f - abs(i - 3) / 5f)
        path.rewind()
        for (s in 0..40) {
            val fraction = s / 40f
            val envelope = sin(fraction * PI.toFloat())
            val py = y + amplitude * envelope * sin(fraction * TAU * 1.4f + t * 0.45f + i * 0.5f)
            if (s == 0) path.moveTo(0f, py) else path.lineTo(size.width * fraction, py)
        }
        drawPath(path, b.copy(alpha = 0.34f - abs(i - 3) * 0.07f), style = Stroke(size.minDimension / 320f))
    }
}

private fun DrawScope.dots(t: Float, a: Color, b: Color) {
    glow(Offset(size.width * 0.5f, size.height * 0.5f), size.maxDimension * 0.6f, a, 1f)
    val step = size.width / 12f
    val unit = size.minDimension / 400f
    val rows = (size.height / step).toInt() + 1
    for (row in 0..rows) for (column in 0 until 12) {
        val wave = 0.5f + 0.5f * sin(t * 0.7f - row * 0.35f - column * 0.28f)
        drawCircle(
            b.copy(alpha = 0.08f + 0.34f * wave),
            unit * (0.8f + 1.4f * wave),
            Offset((column + 0.5f) * step, (row + 0.5f) * step),
        )
    }
}

private fun DrawScope.grid(t: Float, a: Color, b: Color) {
    val horizon = size.height * 0.42f
    glow(Offset(size.width * 0.5f, horizon), size.maxDimension * 0.5f, a, 1f)
    val stroke = size.minDimension / 380f
    for (i in -8..8) {
        drawLine(
            b.copy(alpha = 0.14f),
            Offset(size.width * (0.5f + i * 0.04f), horizon),
            Offset(size.width * (0.5f + i * 0.34f), size.height),
            strokeWidth = stroke,
        )
    }
    repeat(12) { i ->
        val depth = ((i + t * 0.25f) % 12f) / 12f
        val y = horizon + (size.height - horizon) * depth * depth
        drawLine(b.copy(alpha = 0.26f * depth), Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
    }
}

private fun DrawScope.breath(t: Float, a: Color, b: Color) {
    val pulse = 0.5f + 0.5f * sin(t * 0.5f)
    glow(Offset(size.width * 0.5f, size.height * 0.4f), size.maxDimension * (0.42f + 0.16f * pulse), a, 0.95f)
    glow(Offset(size.width * 0.5f, size.height * 0.9f), size.maxDimension * (0.5f - 0.12f * pulse), b, 0.8f)
}
