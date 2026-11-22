package com.popmusic.app.ui.screens.party

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import com.popmusic.app.data.party.PartyStatus
import com.popmusic.app.data.party.PartyUiState
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.ArtworkCollage
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.components.MusicLoader
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Surface
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.pressScale

/** Pestañas de la sala: cola en vivo, buscador y mi biblioteca. */
private enum class PartyTab(val label: String, val icon: ImageVector) {
    QUEUE("Cola", Icons.Rounded.QueueMusic),
    SEARCH("Buscar", Icons.Rounded.Search),
    MY_MUSIC("Mi música", Icons.Rounded.LibraryMusic),
}

/**
 * Sala "Todos controlan": código para entrar, cola en vivo compartida y
 * controles que funcionan para cualquiera de los participantes.
 *
 * Tanda 9: la sesión vive en la aplicación (navegar fuera no cierra la sala),
 * y dentro hay 3 pestañas — Cola / Buscar / Mi música — para que todos puedan
 * ver playlists, recomendaciones y buscar.
 */
@Composable
fun PartyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PartyViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val interaction = remember { MutableInteractionSource() }
    var tab by remember { mutableStateOf(PartyTab.QUEUE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // ————— Cabecera —————
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    .padding(8.dp)
                    .size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Salas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when {
                        state.status == PartyStatus.JOINED ->
                            "En vivo · ${state.participants.size} escuchando"

                        state.inRoom -> "Reconectando…"
                        else -> "Todos controlan"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.inRoom) Muted else Faint,
                )
            }
            if (state.inRoom) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(bounded = true),
                            onClick = viewModel::leave,
                        )
                        .pressScale(interaction, pressedScale = 0.95f)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = null,
                        tint = Faint,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Salir",
                        style = MaterialTheme.typography.labelMedium,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (state.inRoom) {
            RoomContent(
                state = state,
                viewModel = viewModel,
                tab = tab,
                onTabChange = { tab = it },
                onCopyCode = {
                    clipboard.setText(AnnotatedString(state.code.uppercase()))
                },
            )
        } else {
            JoinContent(state = state, viewModel = viewModel)
        }
    }
}

// ————————————————— Unirse / crear —————————————————

@Composable
private fun JoinContent(
    state: PartyUiState,
    viewModel: PartyViewModel,
) {
    val interaction = remember { MutableInteractionSource() }
    val connecting = state.status == PartyStatus.CONNECTING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        // Anillo plateado (blanco → gris).
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color.White, Color(0xFF6E6E78))),
                )
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(42.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Escucha en vivo con amigos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Todos controlan: cualquiera puede cambiar la canción, " +
                "pausar o añadir temas a la cola compartida.",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        GlassTextField(
            value = state.code,
            onValueChange = viewModel::onCodeChange,
            placeholder = "Código de la sala",
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ConfirmationNumber,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedAction(
                text = if (connecting) "Conectando…" else "Unirse",
                icon = Icons.Rounded.Login,
                enabled = !connecting && state.code.length >= 4,
                onClick = { viewModel.joinRoom(state.code) },
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(
                text = "Crear sala",
                icon = Icons.Rounded.Add,
                enabled = !connecting,
                onClick = viewModel::createRoom,
                modifier = Modifier.weight(1f),
            )
        }

        if (connecting) {
            Spacer(Modifier.height(20.dp))
            MusicLoader(size = 24.dp, color = MaterialTheme.colorScheme.onSurface)
        }

        state.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ————————————————— Dentro de la sala —————————————————

@Composable
private fun RoomContent(
    state: PartyUiState,
    viewModel: PartyViewModel,
    tab: PartyTab,
    onTabChange: (PartyTab) -> Unit,
    onCopyCode: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }

    Column(modifier = Modifier.fillMaxSize()) {
        // ————— Código + participantes (tarjeta gris oscura) —————
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF2B2B31), Color(0xFF131316)),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Código de la sala",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text = state.code.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp,
                )
            }

            // Participantes (avatares + contador).
            Row(verticalAlignment = Alignment.CenterVertically) {
                state.participants.take(5).forEachIndexed { index, participant ->
                    if (index > 0) Spacer(Modifier.width((-8).dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 0) Color.White
                                else Color.White.copy(alpha = 0.25f),
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = participant.name.take(1).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (index == 0) Color.Black else Ink,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${state.participants.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }

            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(bounded = true),
                        onClick = onCopyCode,
                    )
                    .pressScale(interaction, pressedScale = 0.94f)
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copiar código",
                    tint = Ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ————— Pestañas —————
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PartyTab.entries.forEach { entry ->
                TabPill(
                    tab = entry,
                    selected = tab == entry,
                    onClick = { onTabChange(entry) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        when (tab) {
            PartyTab.QUEUE -> QueueTab(state = state, viewModel = viewModel)
            PartyTab.SEARCH -> SearchTab(state = state, viewModel = viewModel)
            PartyTab.MY_MUSIC -> MyMusicTab(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun TabPill(
    tab: PartyTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) Color.White else Color.White.copy(alpha = 0.06f),
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(50),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(
                    bounded = true,
                    color = if (selected) {
                        Color.Black.copy(alpha = 0.12f)
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    },
                ),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = if (selected) Color.Black else Faint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.Black else Faint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ————————————————— Pestaña: Cola —————————————————

@Composable
private fun QueueTab(
    state: PartyUiState,
    viewModel: PartyViewModel,
) {
    val snapshot = state.snapshot
    val queue = snapshot?.queue ?: emptyList()
    val currentIndex = snapshot?.index ?: -1
    val currentSong = queue.getOrNull(currentIndex)
    val playing = snapshot?.playing == true
    val playerState by viewModel.playerState.collectAsState()

    // La canción que YA suena no se lista (está en la tarjeta "Ahora suena");
    // se conserva el índice original de cada fila para que `jumpTo` siga
    // saltando a la posición correcta de la cola compartida.
    val upcoming = queue.mapIndexedNotNull { index, song ->
        if (index != currentIndex) index to song else null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Ahora suena + controles
        item(key = "now") {
            LiquidGlass(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Artwork(
                            url = currentSong?.thumbnailUrl,
                            cornerRadius = 14.dp,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong?.title ?: "Aún no hay canciones",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = currentSong?.artist
                                    ?: "Añade la primera desde Buscar o Mi música",
                                style = MaterialTheme.typography.bodySmall,
                                color = Faint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    // SpaceEvenly + fillMaxWidth: los 5 botones se reparten
                    // sin desbordar aunque la pantalla sea estrecha.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayerControlButton(
                            icon = Icons.Rounded.Shuffle,
                            description = "Aleatorio",
                            large = false,
                            active = playerState.shuffle,
                            onClick = viewModel::toggleShuffle,
                        )
                        PlayerControlButton(
                            icon = Icons.Rounded.SkipPrevious,
                            description = "Anterior",
                            large = false,
                            onClick = viewModel::previous,
                        )
                        PlayButton(
                            playing = playing,
                            onClick = viewModel::togglePlay,
                        )
                        PlayerControlButton(
                            icon = Icons.Rounded.SkipNext,
                            description = "Siguiente",
                            large = false,
                            onClick = viewModel::next,
                        )
                        PlayerControlButton(
                            icon = if (playerState.repeatMode == Player.REPEAT_MODE_ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            description = "Repetir",
                            large = false,
                            active = playerState.repeatMode != Player.REPEAT_MODE_OFF,
                            onClick = viewModel::cycleRepeatMode,
                        )
                    }
                }
            }
        }

        // Cola compartida (la que suena no se lista: está arriba)
        if (queue.isNotEmpty()) {
            item(key = "queue-header") {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = Faint,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Cola compartida · ${upcoming.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (upcoming.isEmpty()) {
                item(key = "queue-only-now") {
                    Text(
                        text = "La que suena es la única de la cola. " +
                            "Añade más desde Buscar o Mi música.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Faint,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 4.dp,
                        ),
                    )
                }
            } else {
                itemsIndexed(
                    upcoming,
                    key = { _, (index, song) -> "q-$index-${song.id}" },
                ) { _, (index, song) ->
                    QueueRow(
                        song = song,
                        index = index,
                        isCurrent = index == currentIndex,
                        justAdded = song.id in state.justAdded,
                        onClick = { viewModel.jumpTo(index) },
                    )
                }
            }
        } else {
            item(key = "queue-empty") {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = Faint,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "La cola está vacía. Cualquiera puede añadir canciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Faint,
                    )
                }
            }
        }

        item(key = "end") { Spacer(Modifier.height(24.dp)) }
    }
}

// ————————————————— Pestaña: Buscar —————————————————

@Composable
private fun SearchTab(
    state: PartyUiState,
    viewModel: PartyViewModel,
) {
    val tasteSuggestions by viewModel.tasteSuggestions.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadTasteSuggestions() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "search-field") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                GlassTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = "Buscar canción para añadir…",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Faint,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = if (state.searchQuery.isNotEmpty()) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = Faint,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .clickable { viewModel.clearSearch() }
                                    .padding(3.dp),
                            )
                        }
                    } else {
                        null
                    },
                )
                if (state.searching) {
                    Spacer(Modifier.height(10.dp))
                    MusicLoader(size = 20.dp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Sin búsqueda: sugerencias según los gustos del usuario (Tanda 12);
        // si aún no hay, el aviso genérico de siempre.
        if (state.searchQuery.isBlank()) {
            if (tasteSuggestions.isNotEmpty()) {
                item(key = "taste-header") {
                    SectionRow(icon = Icons.Rounded.AutoAwesome, title = "Según tus gustos")
                }
                itemsIndexed(
                    tasteSuggestions,
                    key = { _, song -> "taste-${song.id}" },
                ) { _, song ->
                    SearchAddRow(
                        song = song,
                        added = song.id in state.justAdded,
                        onAdd = { viewModel.addSongs(listOf(song)) },
                    )
                }
            } else {
                item(key = "search-hint") {
                    EmptyHint(
                        icon = Icons.Rounded.Search,
                        text = "Busca cualquier canción y añádela a la cola de todos.",
                    )
                }
            }
        }

        itemsIndexed(
            state.searchResults,
            key = { _, song -> "sr-${song.id}" },
        ) { _, song ->
            SearchAddRow(
                song = song,
                added = song.id in state.justAdded,
                onAdd = { viewModel.addSongs(listOf(song)) },
            )
        }

        item(key = "end") { Spacer(Modifier.height(24.dp)) }
    }
}

// ————————————————— Pestaña: Mi música —————————————————

@Composable
private fun MyMusicTab(
    state: PartyUiState,
    viewModel: PartyViewModel,
) {
    val library by viewModel.library.collectAsState()
    val recommended by viewModel.recommended.collectAsState()
    val recLoading by viewModel.recLoading.collectAsState()

    // Tanda 12: playlist abierta para elegir canciones sueltas.
    var expandedPlaylist by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadMyMusic() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "playlists-header") {
            SectionRow(icon = Icons.Rounded.LibraryMusic, title = "Tus playlists")
        }

        if (library.playlists.isEmpty()) {
            item(key = "playlists-empty") {
                EmptyHint(
                    icon = Icons.Rounded.LibraryMusic,
                    text = "Aún no tienes playlists. Créalas desde Biblioteca.",
                )
            }
        } else {
            itemsIndexed(
                library.playlists,
                key = { _, playlist -> "pl-${playlist.id}" },
            ) { _, playlist ->
                val expanded = expandedPlaylist == playlist.id
                val allAdded = playlist.songs.isNotEmpty() &&
                    playlist.songs.all { it.id in state.justAdded }
                Column {
                    PlaylistAddRow(
                        covers = playlist.songs.take(4).map { it.thumbnailUrl },
                        name = playlist.name,
                        count = playlist.songs.size,
                        added = allAdded,
                        expanded = expanded,
                        onToggleExpand = {
                            expandedPlaylist = if (expanded) null else playlist.id
                        },
                        onAdd = { viewModel.addSongs(playlist.songs) },
                    )
                    // Canciones sueltas: añadir una a una o varias de
                    // playlists distintas sin subir la playlist entera.
                    if (expanded) {
                        if (playlist.songs.isEmpty()) {
                            Text(
                                text = "Esta playlist está vacía.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Faint,
                                modifier = Modifier.padding(
                                    start = 66.dp,
                                    end = 16.dp,
                                    top = 2.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        } else {
                            playlist.songs.forEach { song ->
                                SearchAddRow(
                                    song = song,
                                    added = song.id in state.justAdded,
                                    onAdd = { viewModel.addSongs(listOf(song)) },
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "rec-header") {
            Spacer(Modifier.height(10.dp))
            SectionRow(icon = Icons.Rounded.AutoAwesome, title = "Recomendadas para la sala")
        }

        if (recLoading) {
            item(key = "rec-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MusicLoader(size = 22.dp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        } else if (recommended.isEmpty()) {
            item(key = "rec-empty") {
                EmptyHint(
                    icon = Icons.Rounded.AutoAwesome,
                    text = "Toca de nuevo para buscar recomendaciones.",
                )
            }
        } else {
            itemsIndexed(
                recommended,
                key = { _, song -> "rec-${song.id}" },
            ) { _, song ->
                SearchAddRow(
                    song = song,
                    added = song.id in state.justAdded,
                    onAdd = { viewModel.addSongs(listOf(song)) },
                )
            }
        }

        item(key = "end") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionRow(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyHint(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Faint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )
    }
}

// ————————————————— Controles y filas —————————————————

@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = interaction,
                indication = ripple(
                    bounded = true,
                    color = Color.Black.copy(alpha = 0.12f),
                ),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.93f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Pausar" else "Reproducir",
            tint = Color.Black,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun PlayerControlButton(
    icon: ImageVector,
    description: String,
    large: Boolean,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val size = if (large) 64.dp else 48.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (active) Color.White.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.07f),
            )
            .border(
                1.dp,
                if (active) Color.White.copy(alpha = 0.40f)
                else Color.White.copy(alpha = 0.12f),
                CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.93f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Ink,
            modifier = Modifier.size(if (large) 30.dp else 22.dp),
        )
    }
}

@Composable
private fun QueueRow(
    song: Song,
    index: Int,
    isCurrent: Boolean,
    justAdded: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrent) Color.White.copy(alpha = 0.10f)
                else Color.Transparent,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            when {
                justAdded -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(16.dp),
                )

                isCurrent -> Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(16.dp),
                )

                else -> Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Faint,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Artwork(
            url = song.thumbnailUrl,
            cornerRadius = 10.dp,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) Ink else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchAddRow(
    song: Song,
    added: Boolean,
    onAdd: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            url = song.thumbnailUrl,
            cornerRadius = 10.dp,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (added) Color.White.copy(alpha = 0.10f) else Color.White,
                )
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(
                        bounded = true,
                        color = Color.Black.copy(alpha = 0.12f),
                    ),
                    enabled = !added,
                    onClick = onAdd,
                )
                .pressScale(interaction, pressedScale = 0.94f)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Icon(
                imageVector = if (added) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = null,
                tint = if (added) Faint else Color.Black,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (added) "Añadido" else "Añadir",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (added) Faint else Color.Black,
            )
        }
    }
}

@Composable
private fun PlaylistAddRow(
    covers: List<String?>,
    name: String,
    count: Int,
    added: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onAdd: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val addInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Toca (o usa la flecha) para abrir la playlist y elegir canciones.
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    onClick = onToggleExpand,
                )
                .pressScale(interaction, pressedScale = 0.98f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkCollage(
                urls = covers,
                modifier = Modifier.size(40.dp),
                cornerRadius = 12.dp,
                iconSize = 16.dp,
                gap = 1.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (count == 1) "1 canción" else "$count canciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Ocultar canciones" else "Ver canciones",
                tint = Faint,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(if (expanded) 180f else 0f),
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (added) Color.White.copy(alpha = 0.10f) else Color.White,
                )
                .clickable(
                    interactionSource = addInteraction,
                    indication = ripple(
                        bounded = true,
                        color = Color.Black.copy(alpha = 0.12f),
                    ),
                    enabled = !added,
                    onClick = onAdd,
                )
                .pressScale(addInteraction, pressedScale = 0.94f)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Icon(
                imageVector = if (added) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = null,
                tint = if (added) Faint else Color.Black,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (added) "Añadida" else "Añadir",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (added) Faint else Color.Black,
            )
        }
    }
}

// ————————————————— Botones de la pantalla de entrada —————————————————

@Composable
private fun PrimaryAction(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) Color.White else Color.White.copy(alpha = 0.10f),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(
                    bounded = true,
                    color = Color.Black.copy(alpha = 0.12f),
                ),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.Black else Faint,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.Black else Faint,
        )
    }
}

@Composable
private fun OutlinedAction(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.07f else 0.04f))
            .border(
                1.dp,
                Color.White.copy(alpha = if (enabled) 0.22f else 0.10f),
                RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Ink else Faint,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Ink else Faint,
        )
    }
}
