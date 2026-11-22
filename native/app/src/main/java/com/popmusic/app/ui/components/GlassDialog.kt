package com.popmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.popmusic.app.ui.theme.Black
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.pressScale

@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissEnabled) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissEnabled,
            dismissOnClickOutside = dismissEnabled,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(180),
            label = "dialogAlpha",
        )
        val scale by animateFloatAsState(
            targetValue = if (visible) 1f else 0.94f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "dialogScale",
        )
        val scrimInteraction = remember { MutableInteractionSource() }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.52f * alpha))
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    enabled = dismissEnabled,
                    onClick = onDismiss,
                )
                .padding(horizontal = 22.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            val availableHeight = maxHeight * 0.88f
            // Sólido como el resto de la interfaz (sin cristal): los menús y
            // diálogos usan opaca con borde fino, igual que las cards de
            // Ajustes y el menú ⋯ del reproductor.
            Box(
                modifier = modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(24.dp, RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = availableHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun GlassDialogHeader(
    title: String,
    subtitle: String? = null,
    onClose: (() -> Unit)? = null,
) {
    GlassDialogHeader(
        title = AnnotatedString(title),
        subtitle = subtitle?.let(::AnnotatedString),
        onClose = onClose,
    )
}

/** Variante con texto enriquecido: permite dejar el nombre en negrita. */
@Composable
fun GlassDialogHeader(
    title: AnnotatedString,
    subtitle: AnnotatedString? = null,
    onClose: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                )
            }
        }
        if (onClose != null) {
            PopIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Cerrar",
                onClick = onClose,
                tint = Faint,
                iconSize = 20.dp,
            )
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    /** Icono inicial (tanda 9): p. ej. "person" en el correo. */
    leadingIcon: (@Composable () -> Unit)? = null,
    /** Icono final (tanda 9): p. ej. la "X" para limpiar el buscador. */
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
        cursorBrush = SolidColor(Ink),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = capitalization,
        ),
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier.padding(end = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { leadingIcon() }
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Faint,
                        )
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier.padding(start = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { trailingIcon() }
                }
            }
        },
    )
}

@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Ítem 12 (tanda 3): variante roja para acciones destructivas. */
    danger: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (danger) {
                    Danger.copy(alpha = if (enabled) 0.18f else 0.08f)
                } else {
                    Color.White.copy(alpha = if (enabled) 0.16f else 0.08f)
                },
            )
            .border(
                1.dp,
                if (danger) {
                    Danger.copy(alpha = if (enabled) 0.55f else 0.25f)
                } else {
                    Color.White.copy(alpha = 0.24f)
                },
                RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> Faint
                danger -> Danger
                else -> Ink
            },
        )
    }
}

@Composable
fun GlassTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Ink else Faint,
        )
    }
}

@Composable
fun GlassActionRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    selected: Boolean = false,
    /** Tinte de acento para la fila (p. ej. rojo para "Eliminar", ítem 12). */
    tint: Color? = null,
    /** Tinte propio del icono (Color.Unspecified conserva los colores de iconos de marca). */
    iconTint: Color? = null,
    /** Estilo propio del título (p. ej. preview tipográfico en el menú de letras). */
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    /** Oculta el cuadro del icono a la izquierda (filas de solo texto). */
    hideIcon: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val accent = tint
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                accent?.copy(alpha = 0.08f)
                    ?: Color.White.copy(alpha = if (selected) 0.13f else 0.06f),
            )
            .border(
                width = 1.dp,
                color = accent?.copy(alpha = 0.38f)
                    ?: Color.White.copy(alpha = if (selected) 0.24f else 0.11f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!hideIcon) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        accent?.copy(alpha = 0.12f)
                            ?: Color.White.copy(alpha = if (icon != null) 0.08f else 0.06f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon ?: Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = iconTint ?: accent ?: if (icon != null) Ink else Faint,
                    modifier = Modifier.size(if (icon != null) 20.dp else 19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = titleStyle,
                color = accent ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun GlassDialogActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
