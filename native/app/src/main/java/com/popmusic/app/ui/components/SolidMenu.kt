package com.popmusic.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.popmusic.app.ui.theme.Ambient
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.pressScale
import kotlin.math.roundToInt

/**
 * Burbuja de opciones SÓLIDA y compacta (sin liquid glass): la misma tarjeta
 * del menú ⋯ del reproductor (fondo opaco #1A1A1E, borde fino, r18, sombra).
 *
 * Se ancla al padre (el Box que contiene el botón que la abre), se despliega
 * hacia abajo y, si el ancla queda en la mitad inferior de la pantalla, hacia
 * arriba para no salirse. [content] recibe el cierre para que cada fila pueda
 * cerrar el menú tras ejecutar su acción.
 */
@Composable
fun SolidMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 232.dp,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels.toFloat()
    var anchorTop by remember { mutableStateOf(0f) }
    var anchorHeight by remember { mutableStateOf(0f) }

    // Nodo de medición dentro del anchor (posición del botón que la abre).
    // Siempre compuesto: así el menú abre ya anclado aunque el anchor se mueva.
    Spacer(
        modifier = modifier
            .size(0.dp)
            .onGloballyPositioned { coords ->
                anchorTop = coords.positionInWindow().y
                anchorHeight = coords.size.height.toFloat()
            },
    )

    if (!expanded) return

    val openUp = anchorTop > 0f && anchorTop + anchorHeight / 2f > screenHeight * 0.6f
    val offsetY = with(density) {
        val gap = 4.dp.toPx()
        (if (openUp) -(anchorHeight + gap) else anchorHeight + gap).roundToInt()
    }

    Popup(
        alignment = if (openUp) Alignment.BottomEnd else Alignment.TopEnd,
        offset = IntOffset(-4, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        val menuShown = remember { Animatable(0f) }
        LaunchedEffect(Unit) { menuShown.animateTo(1f, tween(150)) }
        Column(
            modifier = Modifier
                .width(width)
                .graphicsLayer {
                    alpha = menuShown.value
                    val s = 0.95f + 0.05f * menuShown.value
                    scaleX = s
                    scaleY = s
                    transformOrigin = if (openUp) {
                        TransformOrigin(1f, 1f)
                    } else {
                        TransformOrigin(1f, 0f)
                    }
                }
                .shadow(14.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(Ambient.solid(Color(0xFF1A1A1E)))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(vertical = 6.dp),
            content = { content(onDismiss) },
        )
    }
}

/** Fila del menú compacto: icono 20 + texto (mismo estilo que el ⋯ del player). */
@Composable
fun SolidMenuRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    /** Tinte de acento (p. ej. rojo para "Eliminar"). */
    tint: Color? = null,
    /** Tinte propio del icono (Color.Unspecified conserva los colores de iconos de marca). */
    iconTint: Color? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint ?: tint ?: Ink,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = tint ?: Ink,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

/** Fila de solo texto (con preview tipográfico propio), p. ej. estilos de letra. */
@Composable
fun SolidMenuTextRow(
    label: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = Ink,
            style = style,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(
                text = "●",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
