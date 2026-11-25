package com.popmusic.app.ui.screens.party

import androidx.compose.foundation.background
import com.popmusic.app.ui.screens.player.PlayerPaletteWash
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.AsyncImage
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.ArtworkCollage
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.components.MusicLoader
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.LineStrong
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.Surface
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.Surface3
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundAction(Icons.Rounded.ArrowBack, "Volver", onClick = onBack)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.inRoom) "Sala en vivo" else "Salas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.inRoom) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.status == PartyStatus.JOINED) Ink else Faint,
                                ),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = when {
                            state.status == PartyStatus.JOINED ->
                                if (state.participants.size == 1) {
                                    "Solo tú por ahora"
                                } else {
                                    "${state.participants.size} escuchando"
                                }

                            state.inRoom -> "Reconectando…"
                            else -> "Todos controlan la música"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
            if (state.inRoom) {
                RoundAction(Icons.Rounded.Logout, "Salir de la sala", onClick = viewModel::leave)
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
    val connecting = state.status == PartyStatus.CONNECTING
    val cardShape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 170.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(Brush.verticalGradient(listOf(Surface3, Surface2)))
                .border(1.dp, Ink.copy(alpha = 0.06f), cardShape)
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Tres oyentes solapados: la idea de la sala de un vistazo.
                listOf(Icons.Rounded.Groups, Icons.Rounded.GraphicEq, Icons.Rounded.QueueMusic)
                    .forEachIndexed { index, icon ->
                        Box(
                            modifier = Modifier
                                .offset(x = (-12 * index).dp)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (index == 0) Ink else LineStrong)
                                .border(3.dp, Surface3, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (index == 0) OnInk else Ink,
                                modifier = Modifier.size(25.dp),
                            )
                        }
                    }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Escucha con tus amigos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "La misma música, al mismo tiempo. Cualquiera puede pausar, " +
                    "saltar o añadir canciones.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Spacer(Modifier.height(20.dp))
            PrimaryAction(
                text = "Crear sala",
                icon = Icons.Rounded.Add,
                enabled = !connecting,
                onClick = viewModel::createRoom,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(Surface2)
                .border(1.dp, Ink.copy(alpha = 0.06f), cardShape)
                .padding(20.dp),
        ) {
            Text(
                text = "¿Te han invitado?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Escribe el código que te compartieron.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Spacer(Modifier.height(14.dp))
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
                        tint = Muted,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedAction(
                text = if (connecting) "Conectando…" else "Unirme a la sala",
                icon = Icons.Rounded.Login,
                enabled = !connecting && state.code.length >= 4,
                onClick = { viewModel.joinRoom(state.code) },
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (connecting) {
            Spacer(Modifier.height(20.dp))
            MusicLoader(
                size = 24.dp,
                color = Ink,
                modifier = Modifier.align(Alignment.CenterHorizontally),
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
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // ————— Código + participantes (sigue el color del fondo) —————
        val headerShape = RoundedCornerShape(22.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(headerShape)
                .background(Brush.verticalGradient(listOf(Surface3, Surface2)))
                .border(1.dp, Ink.copy(alpha = 0.06f), headerShape)
                .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Código",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted,
                )
                Text(
                    text = state.code.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    color = Ink,
                )
            }

            // Participantes: máximo 3 fotos solapadas y un chip con el resto
            // para que la fila no crezca aunque haya mucha gente en la sala.
            val shown = state.participants.take(3)
            val hidden = state.participants.size - shown.size
            Row(verticalAlignment = Alignment.CenterVertically) {
                shown.forEachIndexed { index, participant ->
                    if (index > 0) Spacer(Modifier.width((-9).dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) Ink else Surface3)
                            .border(2.dp, Surface2, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val avatar = participant.avatarUrl
                        if (!avatar.isNullOrBlank()) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = "Foto de ${participant.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = participant.name.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (index == 0) OnInk else Ink,
                            )
                        }
                    }
                }
                if (hidden > 0) {
                    Spacer(Modifier.width((-9).dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Surface3)
                            .border(2.dp, Surface2, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+$hidden",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))
            RoundAction(Icons.Rounded.ContentCopy, "Copiar código", filled = false, onClick = onCopyCode)
            Spacer(Modifier.width(6.dp))
            RoundAction(
                icon = Icons.Rounded.Share,
                label = "Invitar",
                filled = true,
                onClick = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "Escucha música conmigo en PoP Music. " +
                                "Entra en Salas con el código ${state.code.uppercase()}",
                        )
                    runCatching {
                        context.startActivity(
                            android.content.Intent.createChooser(send, "Invitar a la sala"),
                        )
                    }
                },
            )
        }

        Spacer(Modifier.height(6.dp))

        // ————— Pestañas: control segmentado —————
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Surface2)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
            PartyTab.QUEUE -> QueueTab(
                state = state,
                viewModel = viewModel,
                onSearch = { onTabChange(PartyTab.SEARCH) },
            )
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
    val content = if (selected) OnInk else Muted
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Ink else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Botón circular de la sala. */
@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (filled) Ink else Surface2)
            .border(1.dp, Ink.copy(alpha = 0.08f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.92f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (filled) OnInk else Ink, modifier = Modifier.size(19.dp))
    }
}

/** Botón ancho con icono para las acciones de la cola. */
@Composable
private fun QueueAction(
    icon: ImageVector,
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) Ink else Surface2)
            .border(1.dp, Ink.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = !loading,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (filled) OnInk else Ink
        if (loading) {
            MusicLoader(size = 18.dp, color = tint)
        } else {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
        )
    }
}

/** Botón redondo de añadir: "+" y, al añadir, un check. */
@Composable
private fun AddCircle(added: Boolean, label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (added) Surface3 else Ink)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = !added,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.90f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (added) Icons.Rounded.Check else Icons.Rounded.Add,
            contentDescription = if (added) "Añadida" else label,
            tint = if (added) Ink else OnInk,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ————————————————— Pestaña: Cola —————————————————

@Composable
private fun QueueTab(
    state: PartyUiState,
    viewModel: PartyViewModel,
    onSearch: () -> Unit,
) {
    var showSuggestions by remember { mutableStateOf(false) }
    val suggestions by viewModel.tasteSuggestions.collectAsState()
    val suggesting by viewModel.tasteLoading.collectAsState()
    val snapshot = state.snapshot
    val queue = snapshot?.queue ?: emptyList()
    val currentIndex = snapshot?.index ?: -1
    val currentSong = queue.getOrNull(currentIndex)
    val playerState by viewModel.playerState.collectAsState()
    // El botón reflecting el estado REAL del reproductor, no snapshot.playing:
    // al saltar, isPlaying pasa por false durante el buffering y el snapshot
    // publicaba ese false, dejando el icono en "Reproducir" mientras sonaba.
    val playing = playerState.isPlaying

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
            NowPlayingCard(
                song = currentSong,
                playing = playing,
                shuffle = playerState.shuffle,
                repeatMode = playerState.repeatMode,
                durationMs = playerState.durationMs,
                viewModel = viewModel,
            )
        }

        item(key = "actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Usa los gustos guardados en ESTE móvil: cada quien, los suyos.
                QueueAction(
                    icon = Icons.Rounded.AutoAwesome,
                    text = if (showSuggestions) "Sugerir otras" else "Sugerir",
                    filled = true,
                    loading = suggesting,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (showSuggestions) {
                            viewModel.refreshSuggestions()
                        } else {
                            showSuggestions = true
                            viewModel.loadTasteSuggestions()
                        }
                    },
                )
                QueueAction(
                    icon = Icons.Rounded.Search,
                    text = "Buscar",
                    filled = false,
                    modifier = Modifier.weight(1f),
                    onClick = onSearch,
                )
            }
        }

        if (showSuggestions) {
            item(key = "suggestions") {
                SuggestionsCard(
                    songs = suggestions,
                    loading = suggesting,
                    justAdded = state.justAdded,
                    queued = queue.mapTo(HashSet()) { it.id },
                    onAdd = { viewModel.addSongs(listOf(it)) },
                    onClose = { showSuggestions = false },
                )
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
                        text = "A continuación · ${upcoming.size}",
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

        item(key = "end") { Spacer(Modifier.height(170.dp)) }
    }
}

/** Lo que suena, sobre los colores animados de su carátula. */
@Composable
private fun NowPlayingCard(
    song: Song?,
    playing: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    durationMs: Long,
    viewModel: PartyViewModel,
) {
    val position by viewModel.positionMs.collectAsState()
    val shape = RoundedCornerShape(28.dp)
    // Con carátula el fondo es oscuro: textos blancos fijos.
    val over = if (song != null) Color.White else Ink
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape)
            .background(Surface2)
            .border(1.dp, Ink.copy(alpha = 0.06f), shape),
    ) {
        if (song != null) {
            PlayerPaletteWash(artworkUrl = song.thumbnailUrl, modifier = Modifier.matchParentSize())
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(over.copy(alpha = 0.08f)),
                ) {
                    Artwork(
                        url = song?.thumbnailUrl,
                        cornerRadius = 20.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(over.copy(alpha = 0.12f))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = over, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = when {
                                song == null -> "Sala lista"
                                playing -> "Sonando para todos"
                                else -> "En pausa"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = over,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = song?.title ?: "Elige la primera canción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = over,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song?.artist ?: "Pulsa Sugerir o Buscar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = over.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (song != null) {
                Spacer(Modifier.height(16.dp))
                val progress = if (durationMs > 0L) {
                    (position.toFloat() / durationMs).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(over.copy(alpha = 0.22f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(over),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlIcon(Icons.Rounded.Shuffle, "Aleatorio", over, 22.dp, shuffle, viewModel::toggleShuffle)
                ControlIcon(Icons.Rounded.SkipPrevious, "Anterior", over, 32.dp, false, viewModel::previous)
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(over)
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(bounded = true),
                            onClick = viewModel::togglePlay,
                        )
                        .pressScale(interaction, pressedScale = 0.92f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pausar" else "Reproducir",
                        tint = if (song != null) Color.Black else OnInk,
                        modifier = Modifier.size(34.dp),
                    )
                }
                ControlIcon(Icons.Rounded.SkipNext, "Siguiente", over, 32.dp, false, viewModel::next)
                ControlIcon(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    "Repetir",
                    over,
                    22.dp,
                    repeatMode != Player.REPEAT_MODE_OFF,
                    viewModel::cycleRepeatMode,
                )
            }
        }
    }
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    label: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    active: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) color.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.90f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            label,
            tint = if (active || size > 24.dp) color else color.copy(alpha = 0.65f),
            modifier = Modifier.size(size),
        )
    }
}

/** Canciones sugeridas en la cola según los gustos de quien pulsó. */
@Composable
private fun SuggestionsCard(
    songs: List<Song>,
    loading: Boolean,
    justAdded: Set<String>,
    queued: Set<String>,
    onAdd: (Song) -> Unit,
    onClose: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape)
            .background(Surface2)
            .border(1.dp, Ink.copy(alpha = 0.06f), shape)
            .padding(top = 14.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Para ti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    "Según tus gustos musicales",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
            Icon(
                Icons.Rounded.Close,
                "Cerrar sugerencias",
                tint = Muted,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(9.dp)
                    .size(20.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        when {
            songs.isNotEmpty() -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(songs, key = { it.id }) { song ->
                    Column(Modifier.width(124.dp)) {
                        Box {
                            Artwork(
                                url = song.thumbnailUrl,
                                cornerRadius = 16.dp,
                                modifier = Modifier.size(124.dp),
                            )
                            Box(Modifier.align(Alignment.BottomEnd).padding(7.dp)) {
                                AddCircle(
                                    added = song.id in justAdded || song.id in queued,
                                    label = "Añadir a la cola",
                                    onClick = { onAdd(song) },
                                )
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            song.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            loading -> Box(
                Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) { MusicLoader(size = 24.dp, color = Ink) }

            else -> Text(
                "No encontramos sugerencias. Revisa tu conexión y pulsa Sugerir otras.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

// ————————————————— Pestaña: Buscar —————————————————

@Composable
private fun SearchTab(
    state: PartyUiState,
    viewModel: PartyViewModel,
) {
    val tasteSuggestions by viewModel.tasteSuggestions.collectAsState()
    val tasteLoading by viewModel.tasteLoading.collectAsState()
    val taste by viewModel.taste.collectAsState()
    val queued = state.snapshot?.queue?.mapTo(HashSet()) { it.id }.orEmpty()
    LaunchedEffect(taste) { viewModel.loadTasteSuggestions() }

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
                                    .background(Ink.copy(alpha = 0.10f))
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
            // Atajos a mis artistas y géneros.
            val shortcuts = taste.artists + taste.genres.sorted()
            if (shortcuts.isNotEmpty()) {
                item(key = "taste-shortcuts") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(shortcuts) { name ->
                            Text(
                                name,
                                style = MaterialTheme.typography.labelLarge,
                                color = Ink,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Surface2)
                                    .border(1.dp, Ink.copy(alpha = 0.06f), CircleShape)
                                    .clickable { viewModel.onSearchQueryChange(name) }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }
            item(key = "taste-header") {
                SectionRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Según tus gustos",
                    action = {
                        RoundAction(
                            icon = Icons.Rounded.Refresh,
                            label = "Otras sugerencias",
                            onClick = viewModel::refreshSuggestions,
                        )
                    },
                )
            }
            itemsIndexed(
                tasteSuggestions,
                key = { _, song -> "taste-${song.id}" },
            ) { _, song ->
                SearchAddRow(
                    song = song,
                    added = song.id in state.justAdded || song.id in queued,
                    onAdd = { viewModel.addSongs(listOf(song)) },
                )
            }
            item(key = "taste-more") {
                if (tasteSuggestions.isEmpty() && !tasteLoading) {
                    EmptyHint(
                        icon = Icons.Rounded.Search,
                        text = "Busca cualquier canción y añádela a la cola de todos.",
                    )
                } else {
                    QueueAction(
                        icon = Icons.Rounded.AutoAwesome,
                        text = if (tasteLoading) "Buscando…" else "Más sugerencias",
                        filled = false,
                        loading = tasteLoading,
                        onClick = viewModel::moreSuggestions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                added = song.id in state.justAdded || song.id in queued,
                onAdd = { viewModel.addSongs(listOf(song)) },
            )
        }

        item(key = "end") { Spacer(Modifier.height(170.dp)) }
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

        item(key = "end") { Spacer(Modifier.height(170.dp)) }
    }
}

@Composable
private fun SectionRow(
    icon: ImageVector,
    title: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface3),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Ink, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
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
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isCurrent) Ink.copy(alpha = 0.10f)
                else Surface2.copy(alpha = 0.55f),
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
            modifier = Modifier.size(48.dp),
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
                color = Muted,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface2.copy(alpha = 0.55f))
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            url = song.thumbnailUrl,
            cornerRadius = 12.dp,
            modifier = Modifier.size(50.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        AddCircle(added = added, label = "Añadir a la cola", onClick = onAdd)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface2)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
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
                modifier = Modifier.size(50.dp),
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
                    color = Muted,
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
        AddCircle(added = added, label = "Añadir toda la playlist", onClick = onAdd)
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
                if (enabled) Ink else Ink.copy(alpha = 0.10f),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(
                    bounded = true,
                    color = OnInk.copy(alpha = 0.12f),
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
            tint = if (enabled) OnInk else Faint,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) OnInk else Faint,
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
            .background(Ink.copy(alpha = if (enabled) 0.07f else 0.04f))
            .border(
                1.dp,
                Ink.copy(alpha = if (enabled) 0.14f else 0.06f),
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
