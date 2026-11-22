package com.popmusic.app.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

/**
 * Forma de "galleta" ondulada (pétalos) para el botón de play/pausa,
 * igual que el estilo Echo Music: el radio varía con el ángulo según
 * r(θ) = R · (1 − indent + indent · cos(lóbulos · (θ − rotación))).
 *
 * Con indent = 0 el resultado es un círculo perfecto; con indent > 0
 * aparecen los lóbulos (0.08f mientras suena la canción) y [rotationDegrees]
 * hace girar SOLO el contorno (el icono central no gira), como en Echo.
 */
class WavyShape(
    private val lobes: Int = 9,
    private val indent: Float = 0.08f,
    private val rotationDegrees: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val radius = size.minDimension / 2f
        val center = size.center
        val steps = 144
        val rotationRad = Math.toRadians(rotationDegrees.toDouble())
        for (i in 0..steps) {
            val angle = i * 2.0 * Math.PI / steps
            val r = radius * (1.0 - indent + indent * cos(lobes * (angle - rotationRad)))
            val x = (center.x + r * cos(angle)).toFloat()
            val y = (center.y + r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}
