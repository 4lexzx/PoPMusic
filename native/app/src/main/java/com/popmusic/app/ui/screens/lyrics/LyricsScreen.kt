package com.popmusic.app.ui.screens.lyrics

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.domain.LyricLine
import com.popmusic.app.ui.components.GlassBackdrop
import com.popmusic.app.ui.components.MusicLoader
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.components.SolidMenu
import com.popmusic.app.ui.components.SolidMenuTextRow
import com.popmusic.app.ui.glass.backdrop.backdrops.LayerBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.layerBackdrop
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.LiquidGlass
import kotlin.math.abs
import kotlinx.coroutines.delay

/** Estilos de letra disponibles (equivalente a "Lyrics styles" de Echo). */
private val lyricsStyles = listOf(
    "clasico" to "Clásico",
    "grande" to "Grande",
    "serif" to "Editorial",
    "mono" to "Máquina de escribir",
)

@Composable
fun LyricsScreen(
    songId: String,
    title: String,
    artist: String,
    artworkUrl: String?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    positionProvider: () -> Long,
    onBack: () -> Unit,
    ambientBackdrop: LayerBackdrop? = null,
    lyricsStyle: String = "clasico",
    onLyricsStyleChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LyricsViewModel = viewModel(),
) {
    LaunchedEffect(songId, title, artist, durationMs) {
        viewModel.load(songId, title, artist, durationMs)
    }

    val lyrics = viewModel.lyrics
    val lines = lyrics?.lines.orEmpty()
    val plainText = lyrics?.plainText
    val offsetMs = viewModel.offsetMs
    val smoothPosition = rememberPlaybackPositionMs(positionProvider)
    val lyricPosition = remember(smoothPosition, offsetMs) {
        derivedStateOf { (smoothPosition.value - offsetMs).coerceAtLeast(0L) }
    }
    val currentIndex by remember(lines, lyricPosition) {
        derivedStateOf {
            val position = lyricPosition.value
            lines.indexOfLast { it.timeMs <= position }
        }
    }
    val listState = rememberLazyListState()

    // Auto-scroll estilo Echo: mientras el usuario arrastra la lista se
    // suspende el seguimiento y se reanuda 4 s después de soltar.
    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(isAutoScrollEnabled) {
        if (!isAutoScrollEnabled) {
            delay(4_000)
            isAutoScrollEnabled = true
        }
    }
    LaunchedEffect(currentIndex, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && currentIndex >= 0) {
            listState.centerOn(currentIndex, center = true)
        }
    }
    val lyricsScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    isAutoScrollEnabled = false
                }
                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isAutoScrollEnabled = false
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GlassBackdrop(
            url = artworkUrl,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (ambientBackdrop != null) {
                        Modifier.layerBackdrop(ambientBackdrop)
                    } else {
                        Modifier
                    },
                ),
            imageAlpha = 0.45f,
            dimTop = 0.35f,
            dimBottom = 0.97f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PopIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack,
                )
                Spacer(Modifier.size(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Letras",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Faint,
                        maxLines = 1,
                    )
                }
                if (viewModel.canTranslate) {
                    PopIconButton(
                        icon = Icons.Rounded.Translate,
                        contentDescription = if (viewModel.translationEnabled) {
                            "Traducción activada"
                        } else {
                            "Activar traducción"
                        },
                        onClick = viewModel::toggleTranslation,
                    )
                    Spacer(Modifier.size(6.dp))
                }
                LyricsStyleMenu(
                    current = lyricsStyle,
                    onSelect = onLyricsStyleChange,
                )
            }

            Spacer(Modifier.height(8.dp))

            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        MusicLoader(
                            size = 30.dp,
                            color = Ink,
                        )
                    }
                }

                viewModel.error != null -> {
                    LyricsMessage(
                        text = viewModel.error ?: "",
                        modifier = Modifier.weight(1f),
                    )
                }

                lines.isEmpty() && !plainText.isNullOrBlank() -> {
                    Text(
                        text = "Letra no sincronizada",
                        style = MaterialTheme.typography.labelMedium,
                        color = Faint,
                        modifier = Modifier.padding(horizontal = 26.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 26.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = plainText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = lyricsTypography(lyricsStyle).fontFamily,
                            ),
                            color = Ink,
                            lineHeight = 28.sp,
                        )
                    }
                }

                lines.isEmpty() -> {
                    LyricsMessage(
                        text = "No encontramos la letra de esta canción",
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    val wordTypography = lyricsTypography(lyricsStyle)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(lyricsScrollConnection),
                        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        itemsIndexed(
                            lines,
                            key = { index, line -> "$index-${line.timeMs}" },
                        ) { index, line ->
                            PopLyricLine(
                                line = line,
                                next = lines.getOrNull(index + 1),
                                isCurrent = index == currentIndex,
                                distance = abs(index - currentIndex),
                                position = lyricPosition,
                                typography = wordTypography,
                                translation = if (viewModel.translationEnabled) {
                                    viewModel.translations[index]
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Botón "Aa" con menú compacto de estilos de letra (sólido, sin cristal). */
@Composable
private fun LyricsStyleMenu(
    current: String,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    Box {
        PopIconButton(
            icon = Icons.Rounded.TextFields,
            contentDescription = "Estilo de letra",
            onClick = { open = true },
        )
        SolidMenu(
            expanded = open,
            onDismiss = { open = false },
            width = 224.dp,
        ) { dismiss ->
            Text(
                text = "Estilo de letra",
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            lyricsStyles.forEach { (key, label) ->
                SolidMenuTextRow(
                    label = label,
                    selected = key == current,
                    style = when (key) {
                        "grande" -> MaterialTheme.typography.titleLarge

                        "serif" -> MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                        )

                        "mono" -> MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        )

                        else -> MaterialTheme.typography.titleMedium
                    },
                    onClick = {
                        dismiss()
                        onSelect(key)
                    },
                )
            }
        }
    }
}

/** Tipografía de las letras según el estilo elegido en Ajustes. */
@Composable
private fun lyricsTypography(style: String): TextStyle = when (style) {
    "grande" -> MaterialTheme.typography.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    )

    "serif" -> MaterialTheme.typography.headlineMedium.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
    )

    "mono" -> MaterialTheme.typography.headlineMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
    )

    else -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
}

@Composable
private fun rememberPlaybackPositionMs(currentPositionMs: () -> Long): State<Long> {
    val smooth = remember { mutableLongStateOf(currentPositionMs()) }
    val currentPosition by rememberUpdatedState(currentPositionMs)

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                val value = currentPosition().coerceAtLeast(0L)
                if (smooth.longValue != value) smooth.longValue = value
            }
        }
    }

    return smooth
}

/**
 * Centra la línea [index] en el viewport con un spring muy lento
 * (`StiffnessVeryLow`), igual que `performSmoothPageScroll` de Echo.
 * Con [center] = false la línea queda anclada arriba del todo (modo
 * compacto del panel dentro del reproductor, tal como hace Echo).
 */
internal suspend fun LazyListState.centerOn(index: Int, center: Boolean = true) {
    var info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (info == null) {
        // Fuera de pantalla: salta a ella y, si sigue visible, centra.
        animateScrollToItem(index)
        info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (info == null) return
    }
    val layout = layoutInfo
    val viewportHeight = layout.viewportEndOffset - layout.viewportStartOffset
    val anchor = if (center) {
        layout.viewportStartOffset + viewportHeight / 2
    } else {
        layout.viewportStartOffset
    }
    val itemAnchor = if (center) info.offset + info.size / 2 else info.offset
    val offset = itemAnchor - anchor
    if (abs(offset) > 10) {
        animateScrollBy(
            value = offset.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessVeryLow,
            ),
        )
    }
}

@Composable
private fun LyricsMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        LiquidGlass(
            shape = RoundedCornerShape(24.dp),
            elevation = 10.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Subtitles,
                    contentDescription = null,
                    tint = Faint,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Faint,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
