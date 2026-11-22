package com.popmusic.app.ui.screens.flow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.LyricLine
import com.popmusic.app.domain.Song
import com.popmusic.app.player.PlayerUiState
import com.popmusic.app.ui.components.AnimatedFavoriteHeart
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.GlassBackdrop
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.components.PopLogo
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.pressScale
import com.popmusic.app.ui.theme.Accent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Music Flow: descubrimiento vertical tipo Resso/Echo.
 *
 * Una canción por página con carátula nítida sobre fondo desenfocado, la letra
 * sincronizada como protagonista (línea actual + siguiente) y toda la acción
 * del reproductor al alcance del pulgar.
 */
@Composable
fun MusicFlowScreen(
    playerState: PlayerUiState,
    favoriteIds: Set<String>,
    downloadedIds: Set<String> = emptySet(),
    downloadProgress: Map<String, Float> = emptyMap(),
    onToggleFavorite: (Song) -> Unit,
    onPlayAt: (List<Song>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShare: (Song) -> Unit,
    onDownload: (Song) -> Unit = {},
    onViewOnYouTube: (Song) -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onBack: () -> Unit,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    viewModel: MusicFlowViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { viewModel.ensureLoaded() }

    val scope = rememberCoroutineScope()
    val flowState = viewModel.state
    val songs = flowState.songs
    val pagerState = rememberPagerState(pageCount = { songs.size })
    val position = rememberFlowPosition(positionProvider)
    val song = songs.getOrNull(pagerState.currentPage)

    // Al asentar la página, esa canción pasa a ser la que suena (cola = feed).
    LaunchedEffect(pagerState.settledPage, songs.size) {
        val page = pagerState.settledPage
        if (songs.isNotEmpty() && page in songs.indices) {
            onPlayAt(songs, page)
        }
    }

    LaunchedEffect(song?.id, playerState.durationMs) {
        viewModel.loadLyrics(song, playerState.durationMs)
    }

    Box(modifier = modifier.fillMaxSize()) {
        GlassBackdrop(
            url = song?.thumbnailUrl ?: playerState.artworkUrl,
            modifier = Modifier.fillMaxSize(),
            imageAlpha = 0.52f,
            dimTop = 0.42f,
            dimBottom = 0.95f,
        )

        if (songs.isEmpty()) {
            FlowEmptyState(
                isLoading = flowState.isLoading,
                message = flowState.error,
                onBack = onBack,
            )
            return@Box
        }

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            val pageSong = songs.getOrNull(page) ?: return@VerticalPager
            val isActive = pageSong.id == playerState.mediaId
            FlowPage(
                song = pageSong,
                isActive = isActive,
                isPlaying = isActive && playerState.isPlaying,
                isFavorite = pageSong.id in favoriteIds,
                lyrics = if (page == pagerState.settledPage) viewModel.lyrics else null,
                positionMs = if (page == pagerState.settledPage) position.value else 0L,
                offsetMs = viewModel.offsetMs,
                durationMs = if (isActive && playerState.durationMs > 0L) {
                    playerState.durationMs
                } else {
                    pageSong.durationSeconds * 1000L
                },
                onTogglePlayPause = onTogglePlayPause,
                onToggleFavorite = { onToggleFavorite(pageSong) },
                onOpenLyrics = onOpenLyrics,
                onOpenInfo = onOpenInfo,
                onOpenPlayer = onOpenPlayer,
                onShare = { onShare(pageSong) },
                onAddToPlaylist = { onAddToPlaylist(pageSong) },
                isDownloaded = pageSong.id in downloadedIds,
                downloadProgress = downloadProgress[pageSong.id],
                onDownload = { onDownload(pageSong) },
                onViewOnYouTube = { onViewOnYouTube(pageSong) },
                onSeek = { ms -> if (isActive) onSeek(ms) },
            )
        }

        FlowChrome(
            onBack = onBack,
            onOpenPlayer = onOpenPlayer,
            onOpenSettings = onOpenSettings,
            onPrevious = {
                val target = pagerState.settledPage - 1
                if (target >= 0) {
                    scope.launch { pagerState.animateScrollToPage(target) }
                }
            },
            onNext = {
                val target = pagerState.settledPage + 1
                if (target < songs.size) {
                    scope.launch { pagerState.animateScrollToPage(target) }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun FlowPage(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    lyrics: com.popmusic.app.domain.Lyrics?,
    positionMs: Long,
    offsetMs: Int,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenPlayer: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onDownload: () -> Unit,
    onViewOnYouTube: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartBurst) 1.25f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        finishedListener = { if (it > 0f) showHeartBurst = false },
        label = "HeartBurst",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Gestos Echo: toque = play/pausa, doble toque = favorito (con
            // ráfaga de corazón), mantener pulsado = menú "más".
            .pointerInput(isFavorite) {
                detectTapGestures(
                    onTap = { onTogglePlayPause() },
                    onDoubleTap = {
                        if (!isFavorite) onToggleFavorite()
                        showHeartBurst = true
                    },
                    onLongPress = { menuOpen = true },
                )
            },
    ) {
        if (showHeartBurst || heartScale > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = heartScale / 1.25f
                        scaleY = heartScale / 1.25f
                        alpha = (heartScale / 1.25f).coerceIn(0f, 1f)
                    },
            ) {
                AnimatedFavoriteHeart(
                    active = true,
                    iconSize = 80.dp,
                    trigger = 1,
                    popOnActivate = false,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
        Spacer(Modifier.height(56.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            Artwork(
                url = song.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 24.dp,
                iconSize = 72.dp,
            )
            if (isActive && !isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Box(modifier = Modifier.weight(1f)) {
            val lines = lyrics?.lines.orEmpty()
            if (lines.isNotEmpty()) {
                val position = (positionMs - offsetMs).coerceAtLeast(0L)
                val index = lines.indexOfLast { it.timeMs <= position }
                FlowLyrics(
                    lines = lines,
                    currentIndex = index,
                    onClick = onOpenLyrics,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = "Desliza hacia arriba para descubrir más",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = song.title.ifBlank { "Sin título" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onOpenPlayer),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = song.artist.ifBlank { "Artista desconocido" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Ver artista",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .drawBehind {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.4f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                        )
                    }
                    .clickable(onClick = onOpenInfo)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Fila de acciones estilo Echo (6 botones circulares "glass"):
        // favorito · letras · playlist · descargar · compartir · más.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowAction(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                description = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                tint = if (isFavorite) Accent else Color.White,
                active = isFavorite,
                onClick = onToggleFavorite,
            )
            FlowAction(
                icon = Icons.Rounded.Subtitles,
                description = "Letras",
                onClick = onOpenLyrics,
            )
            FlowAction(
                icon = Icons.Rounded.PlaylistAdd,
                description = "Añadir a playlist",
                onClick = onAddToPlaylist,
            )
            FlowDownloadAction(
                isDownloaded = isDownloaded,
                progress = downloadProgress,
                onClick = onDownload,
            )
            FlowAction(
                icon = Icons.Rounded.Share,
                description = "Compartir",
                onClick = onShare,
            )
            Box {
                FlowAction(
                    icon = Icons.Rounded.MoreVert,
                    description = "Más opciones",
                    onClick = { menuOpen = true },
                )
                if (menuOpen) {
                    FlowMoreMenu(
                        onDismiss = { menuOpen = false },
                        onInfo = onOpenInfo,
                        onShare = onShare,
                        onViewOnYouTube = onViewOnYouTube,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        FlowProgress(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = if (isActive && durationMs > 0L) {
                { fraction -> onSeek((fraction * durationMs).toLong()) }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
        }
    }
}

/** Botón circular "glass" de Echo: fondo negro 45 %, borde blanco 20 %. */
@Composable
private fun FlowAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    active: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (active) tint.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.45f),
            )
            .border(
                width = 1.dp,
                color = if (active) tint.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.20f),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = 23.dp),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.86f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Descarga con anillo de progreso mientras baja (Echo: DownloadGlassButton). */
@Composable
private fun FlowDownloadAction(
    isDownloaded: Boolean,
    progress: Float?,
    onClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        FlowAction(
            icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
            description = when {
                progress != null -> "Descargando"
                isDownloaded -> "Descargada"
                else -> "Descargar"
            },
            tint = if (isDownloaded) Accent else Color.White,
            active = isDownloaded,
            onClick = onClick,
        )
        if (progress != null) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 2.dp,
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.16f),
            )
        }
    }
}

/** Menú emergente "más opciones" (cristal, igual que el del reproductor). */
@Composable
private fun FlowMoreMenu(
    onDismiss: () -> Unit,
    onInfo: () -> Unit,
    onShare: () -> Unit,
    onViewOnYouTube: () -> Unit,
) {
    FlowMenuPopup(onDismiss = onDismiss) {
        FlowMenuRow(
            icon = Icons.Rounded.Info,
            label = "Información",
            onClick = {
                onDismiss()
                onInfo()
            },
        )
        Spacer(Modifier.height(6.dp))
        FlowMenuRow(
            icon = Icons.Rounded.Share,
            label = "Compartir",
            onClick = {
                onDismiss()
                onShare()
            },
        )
        Spacer(Modifier.height(6.dp))
        FlowMenuRow(
            icon = Icons.Rounded.OpenInNew,
            label = "Ver en YouTube",
            onClick = {
                onDismiss()
                onViewOnYouTube()
            },
        )
    }
}

/** Cáscara de menú emergente sobre cristal, anclada arriba a la derecha. */
@Composable
private fun FlowMenuPopup(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val localDensity = LocalDensity.current
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(
            -6,
            with(localDensity) { 54.dp.roundToPx() },
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        val menuShown = remember { Animatable(0f) }
        LaunchedEffect(Unit) { menuShown.animateTo(1f, tween(180)) }
        val itemInteraction = remember { MutableInteractionSource() }
        LiquidGlass(
            shape = RoundedCornerShape(22.dp),
            tint = Color.Black.copy(alpha = 0.12f),
            elevation = 24.dp,
            modifier = Modifier
                .width(240.dp)
                .graphicsLayer {
                    alpha = menuShown.value
                    val s = 0.9f + 0.1f * menuShown.value
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(1f, 0f)
                }
                .clickable(
                    interactionSource = itemInteraction,
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun FlowMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}

/** Par de líneas estilo Echo: actual en negrita grande, siguiente al 45 %. */
@Composable
private fun FlowLyrics(
    lines: List<LyricLine>,
    currentIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val realLines = remember(lines) {
        lines.withIndex().filter { it.value.text.isNotBlank() && !it.value.isInstrumental }
    }
    val pairIndex = remember(currentIndex, realLines) {
        if (realLines.isEmpty()) {
            0
        } else {
            realLines.indexOfLast { it.index <= currentIndex }.coerceAtLeast(0)
        }
    }
    AnimatedContent(
        targetState = pairIndex,
        transitionSpec = {
            (
                slideInVertically(tween(280)) { it / 4 } +
                    fadeIn(tween(280))
                ) togetherWith (
                slideOutVertically(tween(180)) { -it / 5 } +
                    fadeOut(tween(180))
                )
        },
        label = "flowLyricPair",
        modifier = modifier.clickable(onClick = onClick),
    ) { index ->
        val current = realLines.getOrNull(index)?.value
        val next = realLines.getOrNull(index + 1)?.value
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = current?.text ?: "♪",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (next != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = next.text,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Línea de progreso de 2 dp con pulgar de 8 dp; toque o arrastre = buscar. */
@Composable
private fun FlowProgress(
    positionMs: Long,
    durationMs: Long,
    onSeek: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val seekModifier = if (onSeek != null) {
        Modifier
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(durationMs) {
                var dragged = 0f
                val trackWidth = size.width
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragged = offset.x },
                    onHorizontalDrag = { _, amount ->
                        dragged += amount
                        onSeek((dragged / trackWidth).coerceIn(0f, 1f))
                    },
                )
            }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .height(18.dp)
            .then(seekModifier)
            .drawBehind {
                val centerY = size.height / 2f
                val trackHeight = 2.dp.toPx()
                val top = centerY - trackHeight / 2f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, top),
                    size = Size(size.width * fraction, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width * fraction, centerY),
                )
            },
    )
}

@Composable
private fun FlowChrome(
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PopIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Cerrar",
            onClick = onBack,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Music Flow",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            // Ítem 4: logo en vez de "PoP Music", al mismo 55 % de
            // opacidad que tenía el texto.
            PopLogo(
                contentDescription = "PoP Music",
                modifier = Modifier
                    .height(11.dp)
                    .alpha(0.55f),
            )
        }
        Box {
            PopIconButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = "Más opciones",
                onClick = { menuOpen = true },
            )
            if (menuOpen) {
                FlowMenuPopup(onDismiss = { menuOpen = false }) {
                    FlowMenuRow(
                        icon = Icons.Rounded.SkipPrevious,
                        label = "Anterior",
                        onClick = {
                            menuOpen = false
                            onPrevious()
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowMenuRow(
                        icon = Icons.Rounded.SkipNext,
                        label = "Siguiente",
                        onClick = {
                            menuOpen = false
                            onNext()
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowMenuRow(
                        icon = Icons.Rounded.MusicNote,
                        label = "Reproductor",
                        onClick = {
                            menuOpen = false
                            onOpenPlayer()
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowMenuRow(
                        icon = Icons.Rounded.Settings,
                        label = "Ajustes",
                        onClick = {
                            menuOpen = false
                            onOpenSettings()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowEmptyState(
    isLoading: Boolean,
    message: String?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        FlowChrome(onBack = onBack, onOpenPlayer = {}, onOpenSettings = {})
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isLoading) "Preparando tu flow…" else (message ?: "Sin música todavía"),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

/** Posición de reproducción muestreada cada 50 ms (igual que Echo). */
@Composable
private fun rememberFlowPosition(provider: () -> Long): State<Long> {
    val current by rememberUpdatedState(provider)
    val position = remember { mutableLongStateOf(current().coerceAtLeast(0L)) }
    LaunchedEffect(Unit) {
        while (true) {
            position.longValue = current().coerceAtLeast(0L)
            delay(50)
        }
    }
    return position
}
