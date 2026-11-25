package com.popmusic.app.ui.screens.account

import com.popmusic.app.ui.theme.Surface3
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassDialogHeader
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.PoPMusicApplication
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
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
    val preferences = (LocalContext.current.applicationContext as PoPMusicApplication).preferences
    val nickname by preferences.nickname.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }
    val fallback = session?.email?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: "Invitado"
    val name = nickname.ifBlank { fallback }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.clip(CircleShape).background(Surface2)) {
                PopIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack,
                )
            }
            Spacer(Modifier.size(14.dp))
            Text(
                text = "Cuenta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
        }

        // Todo en una sola rejilla: el perfil sube con el contenido.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 170.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "profile", span = { GridItemSpan(maxLineSpan) }) {
                ProfileCard(
                    name = name,
                    detail = session?.email ?: "Inicia sesión para sincronizar tu música",
                    avatarUrl = session?.avatarUrl,
                    signedIn = session != null,
                    onEdit = { editing = true },
                    onSession = { if (session != null) onSignOut() else onOpenAuth() },
                )
            }
            item(key = "stats", span = { GridItemSpan(maxLineSpan) }) {
                val shape = RoundedCornerShape(22.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Surface2)
                        .border(1.dp, Ink.copy(alpha = 0.06f), shape),
                ) {
                    Stat(state.favorites.size, "Favoritas", Modifier.weight(1f)) {
                        onOpenPlaylist(LibraryRepository.FAVORITES_ID)
                    }
                    Stat(state.playlists.size, "Playlists", Modifier.weight(1f)) {
                        tab = AccountTab.PLAYLISTS
                    }
                    Stat(state.downloads.size, "Descargas", Modifier.weight(1f)) {
                        onOpenPlaylist(LibraryRepository.DOWNLOADS_ID)
                    }
                }
            }
            item(key = "tabs", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface2)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AccountTab.entries.forEach { item ->
                        val active = item == tab
                        val background by animateColorAsState(if (active) Ink else Surface2, tween(200), label = "tab")
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(background)
                                .clickable { tab = item }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) OnInk else Muted,
                            )
                        }
                    }
                }
            }
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

    if (editing) {
        var draft by rememberSaveable { mutableStateOf(nickname) }
        GlassDialog(onDismiss = { editing = false }) {
            GlassDialogHeader(
                title = "Tu apodo",
                subtitle = "Se muestra en tu cuenta y en las salas.",
                onClose = { editing = false },
            )
            Spacer(Modifier.height(16.dp))
            GlassTextField(
                value = draft,
                onValueChange = { draft = it.take(20) },
                placeholder = fallback,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${draft.length} de 20 · Déjalo vacío para usar «$fallback»",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Spacer(Modifier.height(18.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { editing = false },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Guardar",
                    onClick = {
                        preferences.setNickname(draft)
                        editing = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    name: String,
    detail: String,
    avatarUrl: String?,
    signedIn: Boolean,
    onEdit: () -> Unit,
    onSession: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Surface3, Surface2)))
            .border(1.dp, Ink.copy(alpha = 0.06f), shape)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .border(2.dp, Ink.copy(alpha = 0.85f), CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(Surface2),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClickLabel = "Cambiar apodo", onClick = onEdit)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Edit, "Cambiar apodo", tint = Muted, modifier = Modifier.size(18.dp))
        }
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(18.dp))
        val interaction = remember { MutableInteractionSource() }
        Text(
            if (signedIn) "Cerrar sesión" else "Iniciar sesión",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (signedIn) Ink else OnInk,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (signedIn) Ink.copy(alpha = 0.08f) else Ink)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(bounded = true),
                    onClick = onSession,
                )
                .pressScale(interaction, pressedScale = 0.95f)
                .padding(horizontal = 24.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun Stat(count: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "$count",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
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
