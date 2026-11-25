package com.popmusic.app.ui.screens.tastes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlife
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.popmusic.app.data.recommendations.TASTE_GENRES
import com.popmusic.app.ui.components.rememberArtistPhoto
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.Surface3
import com.popmusic.app.ui.theme.pressScale

private fun genreIcon(label: String): ImageVector = when (label) {
    "Pop" -> Icons.Rounded.Star
    "Rock" -> Icons.Rounded.Bolt
    "Reggaetón" -> Icons.Rounded.LocalFireDepartment
    "Baladas" -> Icons.Rounded.Favorite
    "Salsa" -> Icons.Rounded.Celebration
    "Cumbia" -> Icons.Rounded.MusicNote
    "Trap latino" -> Icons.Rounded.Headphones
    "Rancheras / corridos" -> Icons.Rounded.Audiotrack
    "Hip hop" -> Icons.Rounded.Mic
    "Electrónica" -> Icons.Rounded.GraphicEq
    "Metal" -> Icons.Rounded.Album
    "Indie / alternativo" -> Icons.Rounded.Piano
    "Pop internacional" -> Icons.Rounded.Public
    "Bachata" -> Icons.Rounded.Nightlife
    else -> Icons.Rounded.MusicNote
}

/** Tarjeta de sección con título, contador y contenido. */
@Composable
internal fun TasteCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface2)
            .border(1.dp, Ink.copy(alpha = 0.06f), shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Surface3),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Ink, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Text(
                    trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Surface3)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

/** Rejilla de géneros en dos columnas. */
@Composable
internal fun GenreGrid(selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TASTE_GENRES.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { genre ->
                    GenreTile(
                        label = genre.label,
                        selected = genre.label in selected,
                        onClick = { onToggle(genre.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GenreTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(if (selected) Ink else Surface3, tween(180), label = "genreBg")
    val content by animateColorAsState(if (selected) OnInk else Ink, tween(180), label = "genreFg")
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                role = Role.Checkbox,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(genreIcon(label), null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Rounded.Check, null, tint = content, modifier = Modifier.size(16.dp))
    }
}

/** Sugerencias de artistas como avatares circulares, cuatro por fila. */
@Composable
internal fun ArtistGrid(
    suggestions: List<String>,
    selected: List<String>,
    canAdd: Boolean,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        suggestions.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    val active = selected.any { it.equals(name, ignoreCase = true) }
                    ArtistAvatar(
                        name = name,
                        selected = active,
                        enabled = active || canAdd,
                        onClick = { onToggle(name) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ArtistAvatar(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(if (selected) Ink else Surface3, tween(180), label = "artistBg")
    val photo = rememberArtistPhoto(name).url
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }
    Column(
        modifier = modifier
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.93f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(background)
                .border(if (selected) 2.5.dp else 1.dp, if (selected) Ink else Ink.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (enabled) 1f else 0.4f,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (selected) {
                if (photo != null) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
                Icon(
                    Icons.Rounded.Check,
                    null,
                    tint = if (photo != null) Color.White else OnInk,
                    modifier = Modifier.size(26.dp),
                )
            } else if (photo == null) {
                Text(
                    initials.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Ink else Muted.copy(alpha = 0.5f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Ink else Muted,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Elección entre dos opciones a todo el ancho. */
@Composable
internal fun PodcastSwitch(podcast: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface3).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(true to "Música y podcasts", false to "Solo música").forEach { (value, label) ->
            val active = podcast == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) Ink else Surface3)
                    .clickable(role = Role.RadioButton) { onChange(value) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) OnInk else Muted,
                )
            }
        }
    }
}
