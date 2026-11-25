package com.popmusic.app.ui.components

import androidx.compose.foundation.border
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.Ink
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.pressScale

@Composable
fun TopBarIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(38.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
    playAll: Boolean = false,
    onPlayAll: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().enterOnVisible(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (playAll && onPlayAll != null) {
            val playInteraction = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Surface2)
                    .border(1.dp, Ink.copy(alpha = 0.07f), RoundedCornerShape(50))
                    .clickable(
                        interactionSource = playInteraction,
                        indication = ripple(bounded = true),
                        onClick = onPlayAll,
                    )
                    .pressScale(playInteraction, pressedScale = 0.94f)
                    .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
            ) {
                Box(
                    Modifier.size(26.dp).clip(RoundedCornerShape(50)).background(Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = OnInk,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Reproducir",
                    style = MaterialTheme.typography.labelLarge,
                    color = Ink,
                    maxLines = 1,
                )
            }
        } else if (action != null && onActionClick != null) {
            val interaction = remember { MutableInteractionSource() }
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(bounded = true),
                        onClick = onActionClick,
                    )
                    .pressScale(interaction, pressedScale = 0.90f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun MediaCard(
    title: String,
    subtitle: String,
    artworkUrl: String? = null,
    width: Dp = 148.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(width)
            .enterOnVisible()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.95f)
            .padding(4.dp),
    ) {
        Artwork(
            url = artworkUrl,
            modifier = Modifier.size(width - 8.dp),
            cornerRadius = 14.dp,
            iconSize = 34.dp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TrackRow(
    title: String,
    artist: String,
    artworkUrl: String? = null,
    liked: Boolean = false,
    /** Id de vídeo: permite resolver su URL de audio antes de que se pulse. */
    songId: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onMoreClick: (() -> Unit)? = null,
    /** Menú compacto sólido anclado al ⋯; recibe el cierre del menú. */
    moreMenu: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current
    LaunchedEffect(songId) {
        if (songId != null) {
            (context.applicationContext as? PoPMusicApplication)?.audioUrlCache?.warm(listOf(songId))
        }
    }
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .enterOnVisible()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            url = artworkUrl,
            modifier = Modifier.size(52.dp),
            cornerRadius = 10.dp,
            iconSize = 22.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Item 4 (tanda 3): el favorito va a la derecha del título con
            // el texto ocupando todo el ancho, de modo que todos los
            // corazones quedan alineados en la misma posición.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (liked) {
                    Spacer(Modifier.width(6.dp))
                    FavoriteBadge(active = true)
                }
            }
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onMoreClick != null || moreMenu != null) {
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                PopIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "Más opciones",
                    onClick = {
                        if (moreMenu != null) {
                            menuOpen = true
                        } else {
                            onMoreClick?.invoke()
                        }
                    },
                    tint = Faint,
                    iconSize = 19.dp,
                )
                if (moreMenu != null) {
                    SolidMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                    ) { dismiss ->
                        moreMenu(dismiss)
                    }
                }
            }
        }
    }
}

/**
 * Lista de canciones agrupada en un único contenedor redondeado, como en
 * Echo Music (una tarjeta con filas separadas por divisores sutiles).
 */
@Composable
fun GroupedSongList(
    songs: List<Song>,
    onPlay: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    onMore: ((Song) -> Unit)? = null,
    /** Botón "+" para añadir la canción a una playlist (ítem 2). */
    onAdd: ((Song) -> Unit)? = null,
    /** Ids de favoritos: se marca el corazón junto al nombre (ítem 4). */
    favoriteIds: Set<String> = emptySet(),
) {
    val context = LocalContext.current
    // Calienta las URLs de TODA la lista al mostrarla: saltar a una canción
    // lejana no espera a YouTube porque ya está en la caché.
    val warmKey = songs.joinToString(",") { it.id }
    LaunchedEffect(warmKey) {
        (context.applicationContext as? PoPMusicApplication)
            ?.audioUrlCache?.warm(songs.map { it.id })
    }
    if (songs.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column {
            songs.forEachIndexed { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Entrada escalonada: 0, 25, 50… máx. 175 ms de retardo.
                        .enterOnVisible(delayMillis = (index * 25).coerceAtMost(175))
                        .clickable { onPlay(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(
                        url = song.thumbnailUrl,
                        modifier = Modifier.size(48.dp),
                        cornerRadius = 10.dp,
                        iconSize = 20.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (song.id in favoriteIds) {
                                Spacer(Modifier.width(6.dp))
                                FavoriteBadge(active = true)
                            }
                        }
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (onAdd != null) {
                        PopIconButton(
                            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = "Añadir a playlist",
                            onClick = { onAdd(song) },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconSize = 22.dp,
                        )
                    }
                    if (onMore != null) {
                        PopIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = "Más opciones",
                            onClick = { onMore(song) },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconSize = 22.dp,
                        )
                    }
                }
                if (index != songs.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 72.dp, end = 12.dp)
                            .height(1.dp)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp),
        ) {
            icon()
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}