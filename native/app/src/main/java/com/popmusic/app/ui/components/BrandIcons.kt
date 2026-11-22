package com.popmusic.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos de marca ORIGINALES para importar/exportar playlists: YouTube y
 * Spotify. `material-icons-extended` no incluye iconos de marca (falló
 * `Icons.Rounded.YouTube`), así que se dibujan aquí con los glifos oficiales
 * (simple-icons, CC0) y sus colores de marca.
 *
 * Nota de API (Compose 1.12.1): el builder se llama `curveTo` (no `cubicTo`)
 * y `arcTo` es (rx, ry, angle, isMoreThanHalf, isPositiveArc, x, y).
 */
object BrandIcons {

    /** YouTube: cuerpo rojo #FF0000 + triángulo blanco del "play". */
    val YouTube: ImageVector by lazy {
        ImageVector.Builder(
            name = "YouTube",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Glifo oficial (SVG "path" de simple-icons) transcrito a la API
            // de PathBuilder. Los 4 arcos de esquina (r ≈ 3.016, fA=0 fS=0)
            // se convierten a béziers exactos en vez de usar arcTo, cuya
            // semántica de dirección puede diferir de la de SVG.
            path(fill = SolidColor(Color(0xFFFF0000))) {
                moveTo(23.498f, 6.186f)
                // a 3.016 3.016 0 0 0 -2.122 -2.136 (esquina superior derecha)
                curveTo(23.222f, 5.147f, 22.414f, 4.333f, 21.376f, 4.050f)
                // C 19.505 3.545 12 3.545 12 3.545
                curveTo(19.505f, 3.545f, 12f, 3.545f, 12f, 3.545f)
                // s -7.505 0 -9.377 .505 (c1 = reflejo del c2 previo = actual)
                curveToRelative(0f, 0f, -7.505f, 0f, -9.377f, 0.505f)
                // A 3.017 3.017 0 0 0 .502 6.186 (esquina superior izquierda)
                curveTo(1.586f, 4.333f, 0.778f, 5.147f, 0.502f, 6.186f)
                // C 0 8.07 0 12 0 12
                curveTo(0f, 8.07f, 0f, 12f, 0f, 12f)
                // s 0 3.93 .502 5.814
                curveToRelative(0f, 0f, 0f, 3.93f, 0.502f, 5.814f)
                // a 3.016 3.016 0 0 0 2.122 2.136 (esquina inferior izquierda)
                curveTo(0.778f, 18.853f, 1.586f, 19.667f, 2.624f, 19.950f)
                // c 1.871 .505 9.376 .505 9.376 .505
                curveToRelative(1.871f, 0.505f, 9.376f, 0.505f, 9.376f, 0.505f)
                // s 7.505 0 9.377 -.505 (c1 = reflejo del c2 previo = actual)
                curveToRelative(0f, 0f, 7.505f, 0f, 9.377f, -0.505f)
                // a 3.015 3.015 0 0 0 2.122 -2.136 (esquina inferior derecha)
                curveTo(22.414f, 19.667f, 23.222f, 18.853f, 23.499f, 17.814f)
                // C 24 15.93 24 12 24 12
                curveTo(24f, 15.93f, 24f, 12f, 24f, 12f)
                // s 0 -3.93 -.502 -5.814
                curveToRelative(0f, 0f, 0f, -3.93f, -0.502f, -5.814f)
                close()
            }
            // Triángulo blanco del "play".
            path(fill = SolidColor(Color.White)) {
                moveTo(9.545f, 15.568f)
                lineTo(9.545f, 8.432f)
                lineTo(15.818f, 12f)
                close()
            }
        }.build()
    }

    /** Spotify: círculo verde #1DB954 con las tres ondas oficiales. */
    val Spotify: ImageVector by lazy {
        ImageVector.Builder(
            name = "Spotify",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val green = SolidColor(Color(0xFF1DB954))
            val wave = SolidColor(Color(0xFF191414))
            // Círculo (Kappa = 0.5522848 para que el óvalo sea redondo).
            path(fill = green) {
                moveTo(12f, 0f)
                curveTo(18.627f, 0f, 24f, 5.373f, 24f, 12f)
                curveTo(24f, 18.627f, 18.627f, 24f, 12f, 24f)
                curveTo(5.373f, 24f, 0f, 18.627f, 0f, 12f)
                curveTo(0f, 5.373f, 5.373f, 0f, 12f, 0f)
                close()
            }
            // Onda inferior (paths oficiales, bézier a bézier).
            path(fill = wave) {
                moveTo(17.521f, 17.34f)
                curveTo(17.281f, 17.699f, 16.861f, 17.82f, 16.5f, 17.58f)
                curveTo(13.68f, 15.84f, 10.14f, 15.479f, 5.939f, 16.439f)
                curveTo(5.521f, 16.561f, 5.16f, 16.26f, 5.04f, 15.9f)
                curveTo(4.92f, 15.479f, 5.22f, 15.12f, 5.58f, 15f)
                curveTo(10.14f, 13.979f, 14.1f, 14.4f, 17.22f, 16.32f)
                curveTo(17.64f, 16.5f, 17.699f, 16.979f, 17.521f, 17.34f)
                close()
            }
            // Onda media.
            path(fill = wave) {
                moveTo(18.961f, 14.04f)
                curveTo(18.66f, 14.46f, 18.12f, 14.64f, 17.699f, 14.34f)
                curveTo(14.46f, 12.36f, 9.54f, 11.76f, 5.76f, 12.96f)
                curveTo(5.281f, 13.08f, 4.74f, 12.84f, 4.62f, 12.36f)
                curveTo(4.5f, 11.88f, 4.74f, 11.339f, 5.22f, 11.219f)
                curveTo(9.6f, 9.9f, 15f, 10.561f, 18.72f, 12.84f)
                curveTo(19.081f, 13.021f, 19.26f, 13.62f, 18.961f, 14.04f)
                close()
            }
            // Onda superior.
            path(fill = wave) {
                moveTo(19.081f, 10.68f)
                curveTo(15.24f, 8.4f, 8.82f, 8.16f, 5.16f, 9.301f)
                curveTo(4.56f, 9.48f, 3.96f, 9.12f, 3.78f, 8.58f)
                curveTo(3.6f, 7.979f, 3.96f, 7.38f, 4.5f, 7.199f)
                curveTo(8.76f, 5.939f, 15.78f, 6.179f, 20.221f, 8.82f)
                curveTo(20.76f, 9.12f, 20.94f, 9.84f, 20.64f, 10.38f)
                curveTo(20.341f, 10.801f, 19.62f, 10.979f, 19.081f, 10.68f)
                close()
            }
        }.build()
    }
}
