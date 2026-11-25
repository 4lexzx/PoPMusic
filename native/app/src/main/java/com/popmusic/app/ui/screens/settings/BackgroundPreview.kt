package com.popmusic.app.ui.screens.settings

import androidx.compose.foundation.background
import com.popmusic.app.ui.components.animatedPreset
import com.popmusic.app.ui.components.BackgroundSection
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.popmusic.app.data.prefs.BackgroundMode
import com.popmusic.app.data.prefs.BackgroundSettings
import com.popmusic.app.data.recent.RecentStore
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.AnimatedPresets
import com.popmusic.app.ui.components.AppBackground
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.BackgroundPresets
import com.popmusic.app.ui.components.PopLogo
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.Surface3

/** Ancho lógico de la pantalla simulada: todo se dibuja a tamaño real y se escala. */
private val ScreenWidth = 400.dp

/** Paleta con la que quedaría la app con ese fondo. */
private class PreviewPalette(val ink: Color, val onInk: Color, val muted: Color, val card: Color, val bar: Color)

private fun previewPalette(settings: BackgroundSettings): PreviewPalette {
    if (settings.mode != BackgroundMode.ANIMATED) {
        return PreviewPalette(Color.White, Color.Black, Color(0xFFB4B4BB), Color(0xFF17171A), Color(0xFF232327))
    }
    val preset = animatedPreset(settings.presetId)
    val dim = settings.backgroundDim
    val tone = lerp(preset.colors[1], Color.Black, dim)
    return if (preset.light && dim < 0.35f) {
        val deep = lerp(preset.accent, Color.Black, 0.50f)
        PreviewPalette(
            ink = lerp(Color(0xFF0A0A0C), deep, 0.70f),
            onInk = Color.White,
            muted = lerp(Color(0xFF3C3C44), deep, 0.70f),
            card = lerp(Color.White, tone, 0.54f),
            bar = lerp(Color.White, tone, 0.82f),
        )
    } else {
        val strength = if (preset.light) 0f else (1f - dim) * 0.45f
        PreviewPalette(
            ink = lerp(Color.White, preset.accent, 0.28f),
            onInk = Color.Black,
            muted = lerp(Color(0xFFB4B4BB), preset.accent, 0.28f),
            card = lerp(Color(0xFF17171A), tone, strength),
            bar = lerp(Color(0xFF232327), tone, strength),
        )
    }
}

/** Móvil en miniatura con la pantalla de Inicio y el fondo del borrador. */
@Composable
internal fun BackgroundPhonePreview(
    settings: BackgroundSettings,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier
            .width(196.dp)
            .aspectRatio(9f / 18.5f)
            .shadow(24.dp, shape)
            .clip(shape)
            .background(Surface3)
            .clickable(onClickLabel = "Ver en pantalla completa", onClick = onExpand)
            .border(1.dp, Ink.copy(alpha = 0.14f), shape)
            .padding(5.dp),
    ) {
        BackgroundScreen(
            settings = settings,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(25.dp)),
        )
    }
}

/** La misma vista a pantalla completa, con Aplicar y Cerrar. */
@Composable
internal fun BackgroundFullPreview(
    settings: BackgroundSettings,
    canApply: Boolean,
    onApply: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            BackgroundScreen(settings = settings, modifier = Modifier.fillMaxSize(), topInset = 34.dp, bottomInset = 92.dp)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PreviewButton("Cerrar", primary = false, Modifier.weight(1f), onClose)
                PreviewButton(
                    if (canApply) "Aplicar fondo" else "Aplicado",
                    primary = canApply,
                    Modifier.weight(1f),
                ) {
                    if (canApply) onApply()
                    onClose()
                }
            }
        }
    }
}

@Composable
private fun PreviewButton(text: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) Ink else Surface3)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) OnInk else Ink,
        )
    }
}

@Composable
private fun BackgroundScreen(
    settings: BackgroundSettings,
    modifier: Modifier = Modifier,
    topInset: Dp = 8.dp,
    bottomInset: Dp = 0.dp,
) {
    val context = LocalContext.current
    val songs = remember { RecentStore.recent(context).take(8) }
    Box(modifier) {
        AppBackground(settings = settings, modifier = Modifier.fillMaxSize(), applyAmbient = false)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val scale = maxWidth / ScreenWidth
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * scale, density.fontScale),
            ) {
                HomeMock(songs, topInset, bottomInset, previewPalette(settings))
            }
        }
    }
}

@Composable
private fun HomeMock(songs: List<Song>, topInset: Dp, bottomInset: Dp, palette: PreviewPalette) {
    fun song(index: Int): Song? = songs.getOrNull(index)
    Column(Modifier.fillMaxSize().padding(top = topInset, bottom = bottomInset)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopLogo(Modifier.height(28.dp), tint = palette.ink)
            Spacer(Modifier.weight(1f))
            listOf(
                Icons.Rounded.History, Icons.Rounded.Groups, Icons.Rounded.Person, Icons.Rounded.Settings,
            ).forEach {
                Spacer(Modifier.width(18.dp))
                Icon(it, null, tint = palette.ink, modifier = Modifier.size(24.dp))
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState(), enabled = false).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Todo", "Para ti", "Favoritos", "Descubrir").forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (i == 0) palette.onInk else palette.ink,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (i == 0) palette.ink else palette.card)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bar),
        ) {
            Artwork(song(0)?.thumbnailUrl, Modifier.fillMaxSize(), cornerRadius = 24.dp, iconSize = 56.dp)
            Box(
                Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    ),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    song(0)?.title ?: "Tu próxima canción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(song(0)?.artist ?: "PoP Music", color = Color(0xFFB4B4BB), maxLines = 1)
            }
        }
        Text(
            "Para ti",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = palette.ink,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { i ->
                Column(Modifier.weight(1f)) {
                    Artwork(
                        song(i + 1)?.thumbnailUrl,
                        Modifier.fillMaxWidth().aspectRatio(1f).background(palette.bar, RoundedCornerShape(16.dp)),
                        cornerRadius = 16.dp,
                    )
                    Text(
                        song(i + 1)?.title ?: "Canción",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // El peso recorta las filas que no quepan en pantallas bajas.
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            repeat(3) { i ->
                MockRow(song(i + 4), Icons.Rounded.MoreVert, palette)
            }
        }
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(palette.card)
                .border(1.dp, palette.ink.copy(alpha = 0.08f), CircleShape),
        ) {
            MockRow(song(0), Icons.Rounded.PlayArrow, palette)
        }
        Row(
            Modifier.padding(horizontal = 12.dp).padding(bottom = 14.dp).align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.clip(CircleShape).background(palette.bar).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Home, null, tint = palette.ink)
                    Text("Inicio", color = palette.ink, modifier = Modifier.padding(start = 4.dp))
                }
                listOf(Icons.Rounded.Mic, Icons.Rounded.LibraryMusic).forEach {
                    Icon(it, null, tint = palette.muted, modifier = Modifier.padding(horizontal = 18.dp))
                }
            }
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(palette.bar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Search, null, tint = palette.muted)
            }
        }
    }
}

@Composable
private fun MockRow(song: Song?, trailing: ImageVector, palette: PreviewPalette) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            song?.thumbnailUrl,
            Modifier.size(44.dp).background(palette.bar, RoundedCornerShape(12.dp)),
            cornerRadius = 12.dp,
            iconSize = 18.dp,
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                song?.title ?: "Canción",
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                song?.artist ?: "Artista",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(trailing, null, tint = palette.ink, modifier = Modifier.size(22.dp))
    }
}

/** Filas de fondos: animados, degradados y personalizados. */
@Composable
internal fun BackgroundGallery(
    draft: BackgroundSettings,
    onChange: (BackgroundSettings) -> Unit,
    onPickImage: () -> Unit,
) {
    var section by rememberSaveable {
        mutableStateOf(
            if (draft.mode == BackgroundMode.ANIMATED) {
                animatedPreset(draft.presetId).section
            } else {
                BackgroundSection.DARK
            },
        )
    }
    GalleryTitle("Animados")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(BackgroundSection.entries) { entry ->
            val active = entry == section
            Text(
                entry.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) OnInk else Muted,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) Ink else Surface2)
                    .clickable { section = entry }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    // Solo se anima la sección visible.
    val entries = AnimatedPresets.withIndex().filter { it.value.section == section }
    androidx.compose.runtime.key(section) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(entries, key = { it.index }) { (index, preset) ->
                BackgroundTile(
                    label = preset.name,
                    selected = draft.mode == BackgroundMode.ANIMATED && draft.presetId == index,
                    settings = BackgroundSettings(
                        mode = BackgroundMode.ANIMATED, presetId = index, backgroundDim = 0f,
                    ),
                ) {
                    onChange(
                        draft.copy(
                            mode = BackgroundMode.ANIMATED,
                            presetId = index,
                            backgroundDim = if (preset.light) 0f else 0.10f,
                        ),
                    )
                }
            }
        }
    }
    GalleryTitle("Degradados")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(BackgroundPresets) { index, preset ->
            BackgroundTile(
                label = preset.name,
                selected = draft.mode == BackgroundMode.PRESET && draft.presetId == index,
                settings = BackgroundSettings(
                    mode = BackgroundMode.PRESET, presetId = index, backgroundDim = 0.2f,
                ),
            ) { onChange(draft.copy(mode = BackgroundMode.PRESET, presetId = index)) }
        }
    }
    GalleryTitle("Personalizado")
    Row(
        Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BackgroundTile(
            label = "Negro",
            selected = draft.mode == BackgroundMode.NONE,
            settings = BackgroundSettings(),
        ) { onChange(draft.copy(mode = BackgroundMode.NONE)) }
        BackgroundTile(
            label = "Tu foto",
            selected = draft.mode == BackgroundMode.IMAGE,
            settings = if (draft.imageUri != null) {
                BackgroundSettings(mode = BackgroundMode.IMAGE, imageUri = draft.imageUri, backgroundBlur = 0f)
            } else {
                BackgroundSettings()
            },
            icon = Icons.Rounded.Image,
            onClick = onPickImage,
        )
    }
}

@Composable
private fun GalleryTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Ink,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp),
    )
}

@Composable
private fun BackgroundTile(
    label: String,
    selected: Boolean,
    settings: BackgroundSettings,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.width(88.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(shape)
                .clickable(onClickLabel = "Probar $label", onClick = onClick)
                .border(if (selected) 2.dp else 1.dp, if (selected) Ink else Ink.copy(alpha = 0.10f), shape),
            contentAlignment = Alignment.Center,
        ) {
            AppBackground(settings = settings, modifier = Modifier.fillMaxSize(), applyAmbient = false)
            if (icon != null) Icon(icon, null, tint = Ink, modifier = Modifier.size(24.dp))
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, "Seleccionado", tint = OnInk, modifier = Modifier.size(13.dp))
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Ink else Faint,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
        )
    }
}
