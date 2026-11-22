package com.popmusic.app.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.Player
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.Lyrics
import com.popmusic.app.player.PlayerUiState
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.GlassBackdrop
import com.popmusic.app.ui.components.MusicLoader
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.components.PopLogo
import com.popmusic.app.ui.glass.backdrop.backdrops.LayerBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.layerBackdrop
import com.popmusic.app.ui.screens.lyrics.LyricsViewModel
import com.popmusic.app.ui.theme.Ambient
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.WavyShape
import com.popmusic.app.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Reproductor a pantalla completa con la estética de Echo Music ("Apple style"):
 * carátula nítida sobre fondo desenfocado, metadatos abajo a la izquierda con
 * píldora "Ver artista", fila de acciones y línea de progreso fina al pie.
 */
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenInfo: () -> Unit,
    /** Momento (epoch ms) en que se detiene la música; 0 = sin temporizador. */
    sleepAtMs: Long = 0L,
    onSetSleepMinutes: (Int?) -> Unit = {},
    onAddToQueue: () -> Unit = {},
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    onDownload: () -> Unit = {},
    onShare: () -> Unit = {},
    onViewOnYouTube: () -> Unit = {},
    onSetRingtone: () -> Unit = {},
    positionProvider: () -> Long,
    ambientBackdrop: LayerBackdrop? = null,
    /** Oculta la carátula y deja un botón play/pause compacto (Echo). */
    hideArtwork: Boolean = false,
    /** Fondo animado con zoom lento (Canvas/Ambient). */
    ambientMotion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    val dragValue = remember { mutableFloatStateOf(0f) }
    val currentDurationMs by rememberUpdatedState(state.durationMs)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnBack by rememberUpdatedState(onBack)

    // Letras (carga compartida con LyricsScreen vía el mismo ViewModel).
    val lyricsViewModel: LyricsViewModel = viewModel()
    LaunchedEffect(state.mediaId, state.title, state.artist, state.durationMs) {
        if (state.mediaId.isNotBlank() && state.title.isNotBlank()) {
            lyricsViewModel.load(state.mediaId, state.title, state.artist, state.durationMs)
        }
    }
    val lyrics = lyricsViewModel.lyrics
    val smoothPosition = rememberSmoothPosition(positionProvider)

    val favoriteScale = remember { Animatable(1f) }
    LaunchedEffect(isFavorite) {
        if (isFavorite) {
            favoriteScale.animateTo(1.3f, spring(stiffness = Spring.StiffnessHigh))
            favoriteScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // Item 4: gesto natural del sistema — arrastrar hacia abajo cierra el
    // reproductor. El detector vive en la raíz y la carátula ya no consume
    // arrastres verticales (se quitó el gesto de abrir la letra), así que
    // arrastrar sobre la foto también cierra.
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragScreenHeightPx by remember { mutableFloatStateOf(1f) }
    val dragScope = rememberCoroutineScope()
    val closeThresholdPx = with(LocalDensity.current) { 140.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { dragScreenHeightPx = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationY = dragY
                // Profundidad: al arrastrar hacia abajo la pantalla se
                // encoge un 5 % mientras se desvanece.
                val fraction = (dragY / dragScreenHeightPx).coerceIn(0f, 1f)
                scaleX = 1f - fraction * 0.05f
                scaleY = 1f - fraction * 0.05f
                alpha = if (dragY > 0f) {
                    (1f - (dragY / dragScreenHeightPx) * 0.6f).coerceIn(0.35f, 1f)
                } else {
                    1f
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY >= closeThresholdPx) {
                            // Ítem 1: cierre INSTANTÁneo. Sin animación
                            // extra: la transición de la navegación sigue
                            // desde donde quedó el arrastre y detrás ya está
                            // el fotograma de la pantalla anterior.
                            currentOnBack()
                        } else if (dragY > 0f) {
                            dragScope.launch {
                                animate(dragY, 0f) { value, _ -> dragY = value }
                            }
                        }
                    },
                    onDragCancel = {
                        if (dragY > 0f) {
                            dragScope.launch { animate(dragY, 0f) { value, _ -> dragY = value } }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f || dragY > 0f) {
                            change.consume()
                            dragY = (dragY + dragAmount).coerceAtLeast(0f)
                        }
                    },
                )
            },
    ) {
        // Fondo estilo Echo (estilo GRADIENT por defecto): gradiente con los
        // colores extraídos de la carátula (Palette) superpuesto al desenfocado,
        // que queda como respaldo mientras se extraen los colores o si fallan.
        // Ambas capas viven en el mismo Box para que el registro de backdrop
        // (glass) muestre exactamente lo que se ve en pantalla.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (ambientBackdrop != null) {
                        Modifier.layerBackdrop(ambientBackdrop)
                    } else {
                        Modifier
                    },
                ),
        ) {
            GlassBackdrop(
                url = state.artworkUrl,
                imageAlpha = 0.62f,
                dimTop = 0.45f,
                dimBottom = 0.95f,
                motion = ambientMotion,
            )
            PlayerPaletteWash(
                artworkUrl = state.artworkUrl,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            PlayerChrome(
                onBack = onBack,
                onOpenInfo = onOpenInfo,
                onShare = onShare,
                onViewOnYouTube = onViewOnYouTube,
                onSetRingtone = onSetRingtone,
                onAddToQueue = onAddToQueue,
            )

            // Carátula GRANDE (Echo): ancho de pantalla - 2·32, centrada
            // verticalmente en el espacio libre (el sobrante se reparte
            // arriba/abajo en vez de acumularse entre foto y título) y con
            // la letra sincronizada debajo en un bloque de altura FIJA:
            // pasar de 1 a 2 líneas no mueve la imagen (queda estática).
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 8.dp),
                contentAlignment = if (hideArtwork) {
                    Alignment.Center
                } else {
                    Alignment.Center
                },
            ) {
                if (hideArtwork) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CompactPlayerControl(
                            isPlaying = state.isPlaying,
                            isBuffering = state.isBuffering,
                            onTogglePlayPause = onTogglePlayPause,
                            onToggleFavorite = onToggleFavorite,
                        )
                        Spacer(Modifier.height(16.dp))
                        PlayerSyncedLine(
                            lyrics = lyrics,
                            smoothPosition = smoothPosition,
                            offsetMs = lyricsViewModel.offsetMs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                        )
                    }
                } else {
                    // Reserva 72 dp bajo el ancho disponible para la letra
                    // (54) + hueco (18): el tamaño de la imagen no depende
                    // del texto, solo del espacio físico.
                    val artSize = minOf(maxWidth, maxHeight - 72.dp)
                        .coerceAtLeast(140.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PlayerArtwork(
                            artworkUrl = state.artworkUrl,
                            isPlaying = state.isPlaying,
                            isBuffering = state.isBuffering,
                            onTogglePlayPause = onTogglePlayPause,
                            onToggleFavorite = onToggleFavorite,
                            modifier = Modifier.size(artSize),
                        )
                        Spacer(Modifier.height(18.dp))
                        PlayerSyncedLine(
                            lyrics = lyrics,
                            smoothPosition = smoothPosition,
                            offsetMs = lyricsViewModel.offsetMs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                        )
                    }
                }
            }

            Column {
                Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                    // Título + artista con la "píldora partida" de Echo a la
                    // derecha: [Descargar] (esquinas izq. redondas) +
                    // [Favorito] (esquinas der. redondas), botones blancos
                    // sólidos de 42 dp con icono negro de 24 dp (ítem 4).
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.title.ifBlank { "Sin reproducción" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    repeatDelayMillis = 1100,
                                ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.artist.ifBlank { "Busca una canción para empezar" },
                                style = MaterialTheme.typography.titleMedium,
                                // Artista en gris (el título queda blanco).
                                color = Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Descargar: media píldora IZQUIERDA.
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 50.dp,
                                            bottomStart = 50.dp,
                                            topEnd = 3.dp,
                                            bottomEnd = 3.dp,
                                        ),
                                    )
                                    .background(Ink)
                                    .clickable(
                                        interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                        indication = ripple(bounded = true),
                                        enabled = !isDownloaded && downloadProgress == null,
                                        onClick = onDownload,
                                    )
                                    .pressScale(
                                        remember { MutableInteractionSource() },
                                        pressedScale = 0.90f,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (downloadProgress != null) {
                                    CircularProgressIndicator(
                                        progress = { downloadProgress.coerceIn(0f, 1f) },
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = OnInk,
                                        trackColor = OnInk.copy(alpha = 0.25f),
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isDownloaded) {
                                            Icons.Rounded.DownloadDone
                                        } else {
                                            Icons.Rounded.Download
                                        },
                                        contentDescription = when {
                                            isDownloaded -> "Descargada"
                                            else -> "Descargar"
                                        },
                                        tint = OnInk,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                            // Favorito: media píldora DERECHA (con el "pop"
                            // animado al marcar, como HeartBurst de Echo).
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .graphicsLayer {
                                        scaleX = favoriteScale.value
                                        scaleY = favoriteScale.value
                                    }
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 3.dp,
                                            bottomStart = 3.dp,
                                            topEnd = 50.dp,
                                            bottomEnd = 50.dp,
                                        ),
                                    )
                                    .background(Ink)
                                    .clickable(
                                        interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                        indication = ripple(bounded = true),
                                        onClick = onToggleFavorite,
                                    )
                                    .pressScale(
                                        remember { MutableInteractionSource() },
                                        pressedScale = 0.90f,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) {
                                        Icons.Rounded.Favorite
                                    } else {
                                        Icons.Rounded.FavoriteBorder
                                    },
                                    contentDescription = if (isFavorite) {
                                        "Quitar de favoritos"
                                    } else {
                                        "Añadir a favoritos"
                                    },
                                    tint = if (isFavorite) {
                                        Color(0xFFFF3B30)
                                    } else {
                                        OnInk
                                    },
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    PlayerProgress(
                        durationMs = state.durationMs,
                        positionProvider = positionProvider,
                        isDragging = isDragging,
                        dragValue = dragValue.floatValue,
                        onDragStart = { fraction ->
                            isDragging = true
                            dragValue.floatValue = fraction
                        },
                        onDrag = { delta ->
                            dragValue.floatValue =
                                (dragValue.floatValue + delta).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (currentDurationMs > 0L) {
                                currentOnSeek((dragValue.floatValue * currentDurationMs).toLong())
                            }
                            isDragging = false
                        },
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Controles estilo Echo: SOLO anterior / play / siguiente.
                // Lados con círculo blanco al 20 % e icono de 32 dp; play de
                // 84 dp con galleta ondulada (icono 36); hueco de 24 dp.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SideNavButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Anterior",
                        onClick = onPrevious,
                    )
                    Spacer(Modifier.width(24.dp))
                    PlayerWavyPlayButton(
                        isPlaying = state.isPlaying,
                        isBuffering = state.isBuffering,
                        onClick = onTogglePlayPause,
                    )
                    Spacer(Modifier.width(24.dp))
                    SideNavButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente",
                        onClick = onNext,
                    )
                }

                Spacer(Modifier.height(30.dp))

                // Fila inferior segmentada estilo Echo: cuatro botones
                // pequeños de 42 dp (Letras, Aleatorio, Repetir y
                // Temporizador) con el mismo estilo de borde blanco 30 % /
                // relleno blanco, y Añadir playlist en círculo a la derecha.
                // "Añadir a la cola" vive en el menú de 3 puntos: añade a la
                // cola sin interrumpir la canción actual.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerSegmentButton(
                        icon = Icons.Filled.Subtitles,
                        contentDescription = "Letras",
                        isActive = false,
                        shape = RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 3.dp,
                            bottomEnd = 3.dp,
                        ),
                        onClick = onOpenLyrics,
                    )
                    PlayerSegmentButton(
                        icon = Icons.Filled.Shuffle,
                        contentDescription = "Aleatorio",
                        isActive = state.shuffle,
                        shape = RoundedCornerShape(3.dp),
                        onClick = onToggleShuffle,
                    )
                    PlayerSegmentButton(
                        icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Filled.RepeatOne
                        } else {
                            Icons.Filled.Repeat
                        },
                        contentDescription = "Repetir",
                        isActive = state.repeatMode != Player.REPEAT_MODE_OFF,
                        shape = RoundedCornerShape(3.dp),
                        onClick = onCycleRepeat,
                    )
                    PlayerSleepSegmentButton(
                        sleepAtMs = sleepAtMs,
                        onSelect = onSetSleepMinutes,
                        shape = RoundedCornerShape(
                            topStart = 3.dp,
                            bottomStart = 3.dp,
                            topEnd = 50.dp,
                            bottomEnd = 50.dp,
                        ),
                    )

                    Spacer(Modifier.weight(1f))

                    // "Menú" de la derecha: círculo blanco sólido con icono
                    // negro (en Echo es su menú; aquí Añadir playlist).
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Ink)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onAddToPlaylist,
                            )
                            .pressScale(
                                remember { MutableInteractionSource() },
                                pressedScale = 0.90f,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlaylistAdd,
                            contentDescription = "Añadir a playlist",
                            tint = OnInk,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Barra superior: cerrar a la izquierda, marca al centro, menú "más" arriba a la derecha. */
@Composable
private fun PlayerChrome(
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onShare: () -> Unit,
    onViewOnYouTube: () -> Unit,
    onSetRingtone: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PopIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Cerrar",
            onClick = onBack,
            iconSize = 30.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "REPRODUCIENDO",
                style = MaterialTheme.typography.labelSmall,
                color = Ink.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
            )
            // Ítem 4: en vez de la palabra "PoP Music", el logo; PopLogo
            // dibuja con filtrado bicúbico (a 11 dp se veía pixelado).
            PopLogo(
                contentDescription = "PoP Music",
                modifier = Modifier.height(13.dp),
            )
        }
        PlayerTopMenu(
            onOpenInfo = onOpenInfo,
            onShare = onShare,
            onViewOnYouTube = onViewOnYouTube,
            onSetRingtone = onSetRingtone,
            onAddToQueue = onAddToQueue,
        )
    }
}

/** Temporizador de sueño: mismo botón pequeño que Aleatorio/Repetir; el menú crece hacia arriba. */
@Composable
private fun PlayerSleepSegmentButton(
    sleepAtMs: Long,
    onSelect: (Int?) -> Unit,
    shape: RoundedCornerShape = RoundedCornerShape(3.dp),
) {
    var open by remember { mutableStateOf(false) }
    val active = sleepAtMs - System.currentTimeMillis() > 0L
    Box {
        PlayerSegmentButton(
            icon = Icons.Rounded.Timer,
            contentDescription = "Temporizador",
            isActive = active,
            shape = shape,
            onClick = { open = true },
        )
        if (open) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.BottomStart,
                offset = IntOffset(0, -with(density) { 52.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                val shown = remember { Animatable(0f) }
                LaunchedEffect(Unit) { shown.animateTo(1f, tween(150)) }
                Column(
                    modifier = Modifier
                        .width(196.dp)
                        .graphicsLayer {
                            alpha = shown.value
                            val s = 0.95f + 0.05f * shown.value
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(0f, 1f)
                        }
                        .shadow(14.dp, RoundedCornerShape(18.dp), clip = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Ambient.solid(Color(0xFF1A1A1E)))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(vertical = 6.dp),
                ) {
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        PlayerMenuRow(
                            icon = Icons.Rounded.Timer,
                            label = "$mins min",
                            onClick = {
                                open = false
                                onSelect(mins)
                            },
                        )
                    }
                    PlayerMenuRow(
                        icon = Icons.Rounded.Close,
                        label = "Desactivar",
                        onClick = {
                            open = false
                            onSelect(null)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Menú de 3 puntos de la barra superior: se despliega HACIA ABAJO desde la
 * esquina (así se ve completo al abrirse) en una tarjeta sólida limpia,
 * sin liquid glass, con filas de icono + texto.
 */
@Composable
private fun PlayerTopMenu(
    onOpenInfo: () -> Unit,
    onShare: () -> Unit,
    onViewOnYouTube: () -> Unit,
    onSetRingtone: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        val interaction = remember { MutableInteractionSource() }
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Más opciones",
            tint = Ink,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(bounded = true),
                    onClick = { menuOpen = true },
                )
                .pressScale(interaction, pressedScale = 0.90f)
                .padding(11.dp),
        )
        if (menuOpen) {
            val localDensity = LocalDensity.current
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(
                    -4,
                    with(localDensity) { 48.dp.roundToPx() },
                ),
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true),
            ) {
                val menuShown = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    menuShown.animateTo(1f, tween(150))
                }
                Column(
                    modifier = Modifier
                        .width(232.dp)
                        .graphicsLayer {
                            alpha = menuShown.value
                            val s = 0.95f + 0.05f * menuShown.value
                            scaleX = s
                            scaleY = s
                            // Crece desde la esquina superior derecha (hacia abajo).
                            transformOrigin = TransformOrigin(1f, 0f)
                        }
                        .shadow(14.dp, RoundedCornerShape(18.dp), clip = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Ambient.solid(Color(0xFF1A1A1E)))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(vertical = 6.dp),
                ) {
                    PlayerMenuRow(
                        icon = Icons.Rounded.Info,
                        label = "Información",
                        onClick = {
                            menuOpen = false
                            onOpenInfo()
                        },
                    )
                    PlayerMenuRow(
                        icon = Icons.Rounded.Share,
                        label = "Compartir",
                        onClick = {
                            menuOpen = false
                            onShare()
                        },
                    )
                    PlayerMenuRow(
                        icon = Icons.Rounded.OpenInNew,
                        label = "Ver en YouTube",
                        onClick = {
                            menuOpen = false
                            onViewOnYouTube()
                        },
                    )
                    PlayerMenuRow(
                        icon = Icons.Rounded.Notifications,
                        label = "Establecer como tono",
                        onClick = {
                            menuOpen = false
                            onSetRingtone()
                        },
                    )
                    PlayerMenuRow(
                        icon = Icons.Rounded.QueueMusic,
                        label = "Añadir a la cola",
                        onClick = {
                            menuOpen = false
                            onAddToQueue()
                        },
                    )
                }
            }
        }
    }
}

/** Fila limpia del menú: icono 20 + texto, sin recuadros ni glass. */
@Composable
private fun PlayerMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = Ink,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

/**
 * Botón de play/pausa "galleta" de 72 dp (Echo): 9 lóbulos que aparecen y
 * giran lentamente mientras suena; círculo quieto en pausa. Único botón
 * con "contorno".
 */
@Composable
private fun PlayerWavyPlayButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
) {
    val wavyIndent by animateFloatAsState(
        targetValue = if (isPlaying) 0.08f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "playerWavyIndent",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "playerWavy")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "playerWavyRotation",
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(84.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.92f)
            .graphicsLayer {
                // El contorno gira (shape con rotationDegrees); el icono NO gira.
                shape = WavyShape(
                    lobes = 9,
                    indent = wavyIndent,
                    rotationDegrees = if (isPlaying) rotation else 0f,
                )
                clip = true
            }
            .background(Ink),
    ) {
        if (isBuffering && !isPlaying) {
            MusicLoader(
                size = 34.dp,
                color = OnInk,
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = OnInk,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

/**
 * Botón circular de play/pause para cuando la miniatura está oculta.
 * Toque = play/pausa, doble toque = favorito (igual que la carátula).
 */
@Composable
private fun CompactPlayerControl(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(128.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTogglePlayPause() },
                    onDoubleTap = { onToggleFavorite() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isBuffering) {
            MusicLoader(
                size = 34.dp,
                color = Ink,
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Ink.copy(alpha = 0.92f),
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

/** Carátula nítida (cuadrada, 24 dp de esquinas) con glifo de play al pausar. */
@Composable
private fun PlayerArtwork(
    artworkUrl: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTogglePlayPause() },
                    onDoubleTap = { onToggleFavorite() },
                )
            },
    ) {
        Artwork(
            url = artworkUrl,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 24.dp,
            iconSize = 84.dp,
        )
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isBuffering) {
                    MusicLoader(
                        size = 32.dp,
                        color = Color.White,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
        }
    }
}

/** Progreso fino (2 dp) con pulgar de 8 dp y tiempos al pie, como Echo. */
@Composable
private fun PlayerProgress(
    durationMs: Long,
    positionProvider: () -> Long,
    isDragging: Boolean,
    dragValue: Float,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val currentPosition by rememberUpdatedState(positionProvider)
    var positionMs by remember { mutableLongStateOf(currentPosition()) }

    LaunchedEffect(durationMs, isDragging) {
        if (isDragging) return@LaunchedEffect
        while (true) {
            positionMs = currentPosition().coerceAtLeast(0L)
            delay(500)
        }
    }

    val progress = if (isDragging) {
        dragValue
    } else if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val displayPosition = if (isDragging) {
        (dragValue * durationMs).toLong()
    } else {
        positionMs
    }

    SlimSeekBar(
        progress = progress,
        isDragging = isDragging,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(displayPosition),
            style = MaterialTheme.typography.labelMedium,
            color = Ink.copy(alpha = 0.7f),
        )
        Text(
            text = formatTime(durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = Ink.copy(alpha = 0.7f),
        )
    }
}

/** Slider estilo Echo (SLIM): pista de 10 dp que engrosa a 16 dp al
 * arrastrar, sin pulgar (thumb). */
@Composable
private fun SlimSeekBar(
    progress: Float,
    isDragging: Boolean,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    var width by remember { mutableFloatStateOf(1f) }
    val trackHeightDp by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "seekTrackHeight",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onDragStart((offset.x / width).coerceIn(0f, 1f))
                    onDragEnd()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onDragStart((offset.x / width).coerceIn(0f, 1f))
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount / width)
                    },
                )
            },
    ) {
        val centerY = size.height / 2f
        val trackHeight = trackHeightDp.toPx()
        val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        val topLeft = Offset(0f, centerY - trackHeight / 2f)

        drawRoundRect(
            color = Ink.copy(alpha = 0.25f),
            topLeft = topLeft,
            size = Size(size.width, trackHeight),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = Ink,
            topLeft = topLeft,
            size = Size(size.width * progress.coerceIn(0f, 1f), trackHeight),
            cornerRadius = corner,
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Reloj suave de posición (~100 ms) para la letra sincronizada. */
@Composable
private fun rememberSmoothPosition(positionProvider: () -> Long): State<Long> {
    val position = remember { mutableLongStateOf(positionProvider()) }
    LaunchedEffect(Unit) {
        while (true) {
            position.longValue = positionProvider()
            delay(100)
        }
    }
    return position
}

/**
 * Botón de la fila inferior segmentada estilo Echo (PlayerQueueButton):
 * 42 dp con icono de 24 dp; APAGADO = borde de 1 dp blanco al 30 %, ENCENDIDO
 * = relleno blanco sólido con icono negro. El [shape] lo une a sus vecinos.
 */
@Composable
private fun PlayerSegmentButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val base = Modifier
        .size(42.dp)
        .clip(shape)
        .then(
            if (isActive) {
                Modifier.background(Ink)
            } else {
                Modifier.border(
                    width = 1.dp,
                    color = Ink.copy(alpha = 0.3f),
                    shape = shape,
                )
            },
        )
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    Box(
        contentAlignment = Alignment.Center,
        modifier = base.pressScale(interaction, pressedScale = 0.90f),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            // 20 dp: los vectores de Echo miden 24 dp pero su glifo ocupa
            // ~16 dp dentro; el glifo de Material a 24 dp se ve mucho más
            // grande y "raro". A 20 dp el peso visual es idéntico a Echo.
            modifier = Modifier.size(20.dp),
            tint = if (isActive) OnInk else Ink,
        )
    }
}

/**
 * Botón anterior/siguiente estilo Echo: círculo con relleno blanco al 20 %,
 * icono de 32 dp y área de 68 dp (no se achica: tamaño Apple Music/Echo).
 */
@Composable
private fun SideNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.90f),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Ink,
            modifier = Modifier.size(32.dp),
        )
    }
}

/**
 * Hueco entre líneas a partir del cual se asume que la voz ya paró (los LRC
 * sin marcador instrumental no traenen línea vacía): la mini-letra se oculta
 * a los [LYRIC_SING_HOLD_MS] de haber entrado la línea.
 */
private const val LYRIC_LONG_GAP_MS = 8_000L
private const val LYRIC_SING_HOLD_MS = 6_000L

/**
 * Letra sincronizada bajo la portada (Echo: PlayerSyncedLyricsView):
 * MISMA selección que la pantalla de Letras — se queda en la última línea
 * cuyo tiempo ya pasó, **con el mismo offsetMs** que usa LyricsScreen (sin
 * él la mini-letra iba por delante cuando el usuario ha ajustado la
 * sincronía). Antes de la primera línea no se muestra NADA (antes caía a la
 * línea 0 desde el segundo 0 y se adelantaba). Durante los tramos
 * instrumentales TAMPUCO se muestra nada: las líneas de marcador (timestamp
 * con texto vacío, p. ej. `[02:16.99]` → siguiente voz a `[03:06.69]`) se
 * mantienen en la lista y devuelven texto vacío; y si un hueco entre líneas
 * supera [LYRIC_LONG_GAP_MS] la línea visible se oculta a los
 * [LYRIC_SING_HOLD_MS] (los LRC sin marcador no traen línea instrumental).
 * Texto de 18 sp en negrita y la línea entra deslizándose desde abajo
 * saliendo hacia arriba, sin recorte (SizeTransform clip = false), igual que
 * Echo. Bloque de altura FIJA: cambiar de 0/1/2 líneas no mueve la portada.
 */
@Composable
private fun PlayerSyncedLine(
    lyrics: Lyrics?,
    smoothPosition: State<Long>,
    offsetMs: Int,
    modifier: Modifier = Modifier,
) {
    // Sin filtrar: las instrumentales (texto vacío) son las que marcan los
    // tramos de solo música.
    val lines = remember(lyrics) { lyrics?.lines.orEmpty() }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        if (lines.isNotEmpty()) {
            val currentLine = remember(smoothPosition, lines, offsetMs) {
                derivedStateOf {
                    val position = (smoothPosition.value - offsetMs).coerceAtLeast(0L)
                    val index = lines.indexOfLast { it.timeMs <= position }
                    val line = lines.getOrNull(index)
                    when {
                        // Antes de la primera línea o en marcador instrumental.
                        line == null -> ""
                        line.isInstrumental || line.text.isBlank() -> ""
                        else -> {
                            val windowMs = (lines.getOrNull(index + 1)?.timeMs ?: Long.MAX_VALUE) -
                                line.timeMs
                            val stillSinging = if (windowMs > LYRIC_LONG_GAP_MS) {
                                position < line.timeMs + LYRIC_SING_HOLD_MS
                            } else {
                                true
                            }
                            if (stillSinging) line.text else ""
                        }
                    }
                }
            }.value
            AnimatedContent(
                targetState = currentLine,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn() + slideInVertically { it },
                        initialContentExit = fadeOut() + slideOutVertically { -it },
                        sizeTransform = SizeTransform(clip = false),
                    )
                },
                label = "playerSyncedLyric",
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
