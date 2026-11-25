package com.popmusic.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.prefs.PlaybackSettings
import com.popmusic.app.data.recent.RecentStore
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.PopLogo
import com.popmusic.app.ui.screens.player.PlayerPaletteWash
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.Surface3

/**
 * El reproductor en miniatura con lo que suena (o lo último escuchado) y los
 * ajustes de reproducción activos. Cambia al mover los interruptores.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlayerPhonePreview(playback: PlaybackSettings) {
    val context = LocalContext.current
    val app = context.applicationContext as PoPMusicApplication
    val playing by app.playerConnection.state.collectAsState()
    val last = remember { RecentStore.recent(context).firstOrNull() }
    val title = playing.title.ifBlank { last?.title ?: "Tu canción" }
    val artist = playing.artist.ifBlank { last?.artist ?: "Artista" }
    val artwork = playing.artworkUrl ?: last?.thumbnailUrl

    val active = buildList {
        if (playback.fadeIn) add("Transición suave")
        if (playback.crossfadeSeconds > 0) add("Crossfade ${playback.crossfadeSeconds} s")
        if (playback.skipSilence) add("Omite silencios")
        if (playback.instantPlay) add("Inicio instantáneo")
        if (playback.pauseOnMute) add("Pausa al silenciar")
        if (playback.resumeOnHeadset) add("Reanuda al conectar")
        if (playback.equalizerEnabled) add("Ecualizador ${playback.equalizerPreset}")
        if (playback.sleepAtEpochMs > 0L) add("Temporizador")
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = RoundedCornerShape(30.dp)
        Box(
            Modifier
                .width(196.dp)
                .aspectRatio(9f / 18.5f)
                .shadow(24.dp, shape)
                .clip(shape)
                .background(Surface3)
                .border(1.dp, Ink.copy(alpha = 0.14f), shape)
                .padding(5.dp),
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(25.dp))) {
                PlayerPaletteWash(artworkUrl = artwork, motion = playback.ambientMotion)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val scale = maxWidth / 400.dp
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density * scale, density.fontScale),
                    ) {
                        PlayerMock(title, artist, artwork, playing.isPlaying, playback)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            active.ifEmpty { listOf("Valores por defecto") }.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Surface2)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PlayerMock(
    title: String,
    artist: String,
    artwork: String?,
    isPlaying: Boolean,
    playback: PlaybackSettings,
) {
    // El reproductor real siempre es oscuro: blancos fijos, no la paleta.
    val white = Color.White
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = white, modifier = Modifier.size(30.dp))
            Spacer(Modifier.weight(1f))
            PopLogo(Modifier.height(16.dp), tint = white)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.MoreVert, null, tint = white, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(30.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (playback.hideThumbnail) {
                Circle(128.dp, white.copy(alpha = 0.14f), Icons.Rounded.PlayArrow, white, 56.dp)
            } else {
                Artwork(artwork, Modifier.fillMaxSize(), cornerRadius = 24.dp, iconSize = 80.dp)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = white,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = white.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Circle(44.dp, white, Icons.Rounded.Download, Color.Black, 22.dp)
            Spacer(Modifier.width(6.dp))
            Circle(44.dp, white, Icons.Rounded.Favorite, Color.Black, 22.dp)
        }
        Spacer(Modifier.height(26.dp))
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(white.copy(alpha = 0.25f)),
        ) {
            Box(Modifier.fillMaxWidth(0.38f).fillMaxHeight().background(white))
        }
        Spacer(Modifier.height(30.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Circle(68.dp, white.copy(alpha = 0.2f), Icons.Rounded.SkipPrevious, white, 30.dp)
            Circle(
                84.dp,
                white,
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                Color.Black,
                38.dp,
            )
            Circle(68.dp, white.copy(alpha = 0.2f), Icons.Rounded.SkipNext, white, 30.dp)
        }
        Spacer(Modifier.height(34.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                Icons.Rounded.Subtitles, Icons.Rounded.Shuffle, Icons.Rounded.Repeat, Icons.Rounded.Timer,
            ).forEach { icon ->
                val lit = icon == Icons.Rounded.Timer && playback.sleepAtEpochMs > 0L
                Circle(
                    44.dp,
                    if (lit) white else white.copy(alpha = 0.12f),
                    icon,
                    if (lit) Color.Black else white,
                    21.dp,
                )
            }
            Circle(44.dp, white, Icons.Rounded.PlaylistAdd, Color.Black, 22.dp)
        }
    }
}

@Composable
private fun Circle(size: Dp, background: Color, icon: ImageVector, tint: Color, iconSize: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
    }
}
