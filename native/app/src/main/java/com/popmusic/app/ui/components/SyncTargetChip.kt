package com.popmusic.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popmusic.app.data.music.MusicService
import com.popmusic.app.ui.theme.pressScale

/**
 * Ítem 3 / Tanda 12: selector de sincronización. Muestra "No sincronizar"
 * (opcional), "Ambas" (si hay 2+ cuentas) y una pestaña por cada cuenta de
 * música vinculada. `selected == null` = ambas plataformas.
 * No pinta nada si no hay cuentas conectadas (todo queda local).
 */
@Composable
fun SyncTargetSelector(
    services: List<MusicService>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    showNone: Boolean = true,
) {
    if (services.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            text = "Sincronización",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            if (showNone) {
                SyncTargetChip(
                    label = "No sincronizar",
                    selected = selected == "NONE",
                    onClick = { onSelect("NONE") },
                )
            }
            if (services.size >= 2) {
                SyncTargetChip(
                    label = "Ambas",
                    selected = selected == null,
                    onClick = { onSelect(null) },
                )
            }
            services.forEach { service ->
                SyncTargetChip(
                    label = service.displayName(),
                    selected = selected == service.name,
                    onClick = { onSelect(service.name) },
                )
            }
        }
    }
}

/** Chip individual de elección de sincronización (blanco/gris). */
@Composable
private fun SyncTargetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Seleccionado = blanco sólido con texto negro; libre = contorno gris.
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        if (selected) Color.White else Color.White.copy(alpha = 0.06f),
        label = "syncChipBg",
    )
    val contentColor = if (selected) Color.Black else Color.White.copy(alpha = 0.85f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                    )
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.94f)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}
