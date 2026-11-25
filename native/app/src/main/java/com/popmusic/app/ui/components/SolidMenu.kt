package com.popmusic.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.popmusic.app.ui.theme.Ambient
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.pressScale

private val MenuShape = RoundedCornerShape(14.dp)

/**
 * Coloca el menú pegado al botón que lo abre: alineado a su derecha, debajo
 * si cabe y encima si no. Nunca se sale de la pantalla.
 */
private class MenuPosition(
    private val gap: Int,
    private val margin: Int,
    private val alignStart: Boolean,
    private val onPlaced: (opensUp: Boolean) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val wantedX = if (alignStart) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val below = anchorBounds.bottom + gap
        val above = anchorBounds.top - gap - popupContentSize.height
        val up = below + popupContentSize.height > windowSize.height - margin && above >= margin
        onPlaced(up)
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        return IntOffset(
            wantedX.coerceIn(margin, maxX),
            (if (up) above else below).coerceIn(margin, maxY),
        )
    }
}

/**
 * Burbuja de opciones SÓLIDA y compacta: se ajusta al ancho de su texto y se
 * ancla al Box que contiene el botón que la abre. [content] recibe el cierre.
 */
@Composable
fun SolidMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** Ancho fijo; si no se indica, el menú se ajusta a su contenido. */
    width: Dp? = null,
    alignStart: Boolean = false,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    if (!expanded) return

    val density = LocalDensity.current
    var opensUp by remember { mutableStateOf(false) }
    val position = remember(density, alignStart) {
        with(density) {
            MenuPosition(6.dp.roundToPx(), 10.dp.roundToPx(), alignStart) { opensUp = it }
        }
    }

    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        val menuShown = remember { Animatable(0f) }
        LaunchedEffect(Unit) { menuShown.animateTo(1f, tween(140)) }
        Column(
            modifier = modifier
                .then(
                    if (width != null) {
                        Modifier.width(width)
                    } else {
                        Modifier
                            .widthIn(min = 148.dp, max = 240.dp)
                            .width(IntrinsicSize.Max)
                    },
                )
                .graphicsLayer {
                    alpha = menuShown.value
                    val s = 0.92f + 0.08f * menuShown.value
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(
                        if (alignStart) 0f else 1f,
                        if (opensUp) 1f else 0f,
                    )
                }
                .shadow(16.dp, MenuShape, clip = false)
                .clip(MenuShape)
                .background(Ambient.solid(Color(0xFF151517)))
                .border(1.dp, Ink.copy(alpha = 0.07f), MenuShape)
                .verticalScroll(rememberScrollState())
                .padding(4.dp),
            content = { content(onDismiss) },
        )
    }
}

/** Línea fina que separa las acciones delicadas (p. ej. eliminar). */
@Composable
fun SolidMenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .height(1.dp)
            .background(Ink.copy(alpha = 0.07f)),
    )
}

/** Fila compacta: icono 17 + texto. */
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
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint ?: tint ?: Ink.copy(alpha = 0.78f),
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = tint ?: Ink,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 4.dp),
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
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Ink.copy(alpha = 0.07f) else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = Ink,
            style = style,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Ink))
        }
    }
}
