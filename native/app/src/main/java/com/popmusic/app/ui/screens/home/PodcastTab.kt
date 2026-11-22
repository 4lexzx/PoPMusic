package com.popmusic.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.popmusic.app.data.podcasts.Podcast
import com.popmusic.app.data.podcasts.PodcastEpisode
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.MusicLoader
import com.popmusic.app.ui.components.enterOnVisible
import com.popmusic.app.ui.screens.onboarding.TasteChip
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.pressScale
import java.text.SimpleDateFormat
import java.util.Locale

/** Cabecera de sección dentro de la pestaña Podcast. */
@Composable
internal fun PodcastHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Chips horizontales de categorías (iTunes). */
@Composable
internal fun PodcastCategoryRow(
    categories: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            TasteChip(
                label = category,
                selected = false,
                onClick = { onSelect(category) },
            )
        }
    }
}

/** Fila de podcast (catálogo, resultados, suscripciones). */
@Composable
internal fun PodcastTile(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            url = podcast.artworkUrl,
            cornerRadius = 12.dp,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = podcast.author,
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Podcasts,
            contentDescription = null,
            tint = Faint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Cabecera del podcast abierto: arte, título y botón de suscripción. */
@Composable
internal fun PodcastDetailHeader(
    podcast: Podcast,
    subscribed: Boolean,
    onBack: () -> Unit,
    onToggleSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Volver",
                tint = Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(bounded = true),
                        onClick = onBack,
                    )
                    .padding(6.dp)
                    .size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Episodios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(
                url = podcast.artworkUrl,
                cornerRadius = 16.dp,
                modifier = Modifier.size(84.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = podcast.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (subscribed) Ink.copy(alpha = 0.18f)
                            else Color.White.copy(alpha = 0.08f),
                        )
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(bounded = true),
                            onClick = onToggleSubscribe,
                        )
                        .pressScale(interaction, pressedScale = 0.95f)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (subscribed) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = if (subscribed) "Suscrito" else "Suscribirme",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (subscribed) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (subscribed) Ink else Faint,
                        )
                    }
                }
            }
        }
    }
}

/** Fila de episodio reproducible. */
@Composable
internal fun PodcastEpisodeRow(
    episode: PodcastEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            url = episode.artworkUrl,
            cornerRadius = 12.dp,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = episodeMeta(episode),
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Tarjeta compacta para las filas horizontales (suscripciones). */
@Composable
internal fun PodcastGridTile(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(132.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(10.dp),
    ) {
        Artwork(
            url = podcast.artworkUrl,
            cornerRadius = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = podcast.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = podcast.author,
            style = MaterialTheme.typography.bodySmall,
            color = Faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Indicador de carga centrado para la pestaña. */
@Composable
internal fun PodcastLoading(modifier: Modifier = Modifier) {    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        MusicLoader(size = 26.dp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private val episodeDateFormat = SimpleDateFormat("d 'de' MMM", Locale("es", "ES"))

private fun episodeMeta(episode: PodcastEpisode): String {
    val parts = mutableListOf<String>()
    if (episode.durationSeconds > 0) parts += formatDuration(episode.durationSeconds)
    if (episode.publishedAt > 0) parts += episodeDateFormat.format(
        java.util.Date(episode.publishedAt),
    )
    return parts.joinToString(" · ")
}

private fun formatDuration(seconds: Long): String {
    val totalMinutes = seconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours h ${minutes} min" else "$minutes min"
}
