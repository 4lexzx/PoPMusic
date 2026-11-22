package com.popmusic.app.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.popmusic.app.data.auth.AuthSession
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.ArtworkCollage
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.components.TopBarIcon
import com.popmusic.app.ui.components.enterOnVisible
import com.popmusic.app.ui.screens.library.LibraryViewModel
import com.popmusic.app.ui.theme.pressScale

private enum class AccountTab(val label: String) {
    PLAYLISTS("Playlists"),
    FAVORITES("Favoritos"),
    DOWNLOADS("Descargas"),
}

/**
 * Perfil de cuenta (icono de la persona). Replica la estructura del
 * AccountScreen de Echo Music: cabecera con avatar/estado de sesión,
 * accesos rápidos, chips de filtro y grid con el contenido de la biblioteca.
 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onOpenAuth: () -> Unit,
    onSignOut: () -> Unit,
    session: AuthSession?,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(AccountTab.PLAYLISTS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Volver",
                onClick = onBack,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = "Cuenta",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        // — Cabecera de perfil —
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatar = session?.avatarUrl
                    if (!avatar.isNullOrBlank()) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (session != null) {
                        Text(
                            text = session.email.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session?.email?.substringBefore("@") ?: "Invitado",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = session?.email ?: "Inicia sesión para sincronizar tu cuenta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                ProfilePillButton(
                    text = if (session != null) "Salir" else "Iniciar sesión",
                    filled = session == null,
                    onClick = { if (session != null) onSignOut() else onOpenAuth() },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // — Accesos rápidos con estadísticas —
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatCard(
                count = state.favorites.size,
                label = "Favoritos",
                icon = Icons.Rounded.Favorite,
                modifier = Modifier.weight(1f),
                onClick = { onOpenPlaylist(LibraryRepository.FAVORITES_ID) },
            )
            StatCard(
                count = state.playlists.size,
                label = "Playlists",
                icon = Icons.Rounded.QueueMusic,
                modifier = Modifier.weight(1f),
                onClick = { tab = AccountTab.PLAYLISTS },
            )
            StatCard(
                count = state.downloads.size,
                label = "Descargas",
                icon = Icons.Rounded.Download,
                modifier = Modifier.weight(1f),
                onClick = { onOpenPlaylist(LibraryRepository.DOWNLOADS_ID) },
            )
        }

        Spacer(Modifier.height(16.dp))

        // — Chips de filtro —
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AccountTab.values().forEach { item ->
                val active = item == tab
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            },
                        )
                        .clickable { tab = item }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // — Grid de contenido —
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 170.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                AccountTab.PLAYLISTS -> {
                    if (state.playlists.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                title = "Sin playlists todavía",
                                subtitle = "Crea una desde Biblioteca y aparecerá aquí.",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(44.dp),
                                    )
                                },
                            )
                        }
                    } else {
                        items(state.playlists, key = { it.id }) { playlist ->
                            AccountPlaylistCard(
                                covers = playlist.songs.take(4).map { it.thumbnailUrl },
                                title = playlist.name,
                                subtitle = songCount(playlist.songs.size),
                                onClick = { onOpenPlaylist(playlist.id) },
                            )
                        }
                    }
                }
                AccountTab.FAVORITES -> {
                    if (state.favorites.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                title = "Aún no tienes favoritos",
                                subtitle = "Toca el corazón de cualquier canción.",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(44.dp),
                                    )
                                },
                            )
                        }
                    } else {
                        items(state.favorites, key = { "fav_${it.id}" }) { song ->
                            AccountSongCard(
                                song = song,
                                onClick = { onPlay(state.favorites, state.favorites.indexOf(song)) },
                            )
                        }
                    }
                }
                AccountTab.DOWNLOADS -> {
                    if (state.downloads.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                title = "Sin descargas",
                                subtitle = "Descarga canciones para escucharlas sin conexión.",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(44.dp),
                                    )
                                },
                            )
                        }
                    } else {
                        items(state.downloads, key = { "dl_${it.id}" }) { song ->
                            AccountSongCard(
                                song = song,
                                onClick = { onPlay(state.downloads, state.downloads.indexOf(song)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePillButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (filled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (filled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.92f)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun StatCard(
    count: Int,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccountPlaylistCard(
    covers: List<String?>,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .enterOnVisible()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f),
    ) {
        ArtworkCollage(
            urls = covers,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 20.dp,
            iconSize = 38.dp,
        )
        Spacer(Modifier.height(10.dp))
        // El clip redondeado de la tarjeta cortaba la primera/última letra
        // y el número: el texto va con padding y un margen inferior para
        // salir de la zona de las esquinas (tanda 5, ítem 6).
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AccountSongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .enterOnVisible()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f),
    ) {
        Artwork(
            url = song.thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 20.dp,
            iconSize = 38.dp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

private fun songCount(count: Int): String = when (count) {
    0 -> "Vacía"
    1 -> "1 canción"
    else -> "$count canciones"
}
