package com.popmusic.app.ui.screens.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.popmusic.app.data.info.SongFacts
import com.popmusic.app.data.info.SongFactsRepository
import com.popmusic.app.domain.Song
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.GlassBackdrop
import com.popmusic.app.ui.components.MusicLoader
import com.popmusic.app.ui.components.PopIconButton
import com.popmusic.app.ui.glass.backdrop.backdrops.LayerBackdrop
import com.popmusic.app.ui.glass.backdrop.backdrops.layerBackdrop
import com.popmusic.app.ui.screens.player.PlayerPaletteWash
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Muted
import java.util.Locale
import kotlinx.coroutines.CancellationException

/**
 * Ficha de la canción: primero el artista y después los datos. Solo muestra
 * información comprobada; lo que no se encuentra, no aparece.
 */
@Composable
fun SongInfoScreen(
    song: Song?,
    onBack: () -> Unit,
    ambientBackdrop: LayerBackdrop? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val songId = song?.id.orEmpty()
    var facts by remember(songId) { mutableStateOf<SongFacts?>(null) }
    var loading by remember(songId) { mutableStateOf(song != null) }
    var attempt by remember(songId) { mutableStateOf(0) }

    LaunchedEffect(songId, attempt) {
        val current = song ?: return@LaunchedEffect
        loading = true
        facts = try {
            SongFactsRepository(context.applicationContext).facts(current)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        loading = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (ambientBackdrop != null) Modifier.layerBackdrop(ambientBackdrop) else Modifier,
                ),
        ) {
            GlassBackdrop(
                url = song?.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                imageAlpha = 0.5f,
                dimTop = 0.4f,
                dimBottom = 0.97f,
            )
            PlayerPaletteWash(artworkUrl = song?.thumbnailUrl)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
        }

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PopIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    onClick = onBack,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Información",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    song == null -> Unit

                    loading -> Box(
                        Modifier.fillMaxWidth().padding(vertical = 90.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MusicLoader(size = 34.dp, color = Ink)
                    }

                    facts == null -> {
                        SongCard(song, null)
                        InfoCard {
                            Text(
                                "No se pudo conectar para buscar la información.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Muted,
                            )
                            Text(
                                "Reintentar",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { attempt++ }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }

                    else -> {
                        val info = facts ?: return@Column
                        Appear { ArtistCard(info) }
                        Appear { SongCard(song, info) }
                        info.story?.let { story -> Appear { StoryCard(story) } }
                        if (info.sources.isNotEmpty()) {
                            Text(
                                "Fuentes: ${info.sources.joinToString(" · ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Ink.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Appear(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 320f)) { it / 6 },
    ) { content() }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.34f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .animateContentSize(spring(dampingRatio = 0.9f, stiffness = 400f))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = Muted,
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ArtistCard(info: SongFacts) {
    InfoCard {
        CardTitle("Artista")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    null,
                    tint = Ink.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp),
                )
                if (info.artistPhoto != null) {
                    AsyncImage(
                        model = info.artistPhoto,
                        contentDescription = "Foto de ${info.artist}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    info.artist ?: "Artista desconocido",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail = when {
                    info.artist == null -> "Sin información disponible"
                    info.genre != null -> info.genre
                    else -> null
                }
                if (detail != null) {
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
            }
        }
        info.bio?.let { bio ->
            Spacer(Modifier.height(14.dp))
            Text(
                bio,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.copy(alpha = 0.86f),
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun SongCard(song: Song, info: SongFacts?) {
    InfoCard {
        CardTitle("Canción")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(
                url = song.thumbnailUrl,
                modifier = Modifier.size(72.dp),
                cornerRadius = 16.dp,
                iconSize = 28.dp,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                info?.title ?: song.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        val rows = buildList {
            info?.year?.let { add("Año de lanzamiento" to it) }
            info?.album?.let { add("Álbum" to it) }
            info?.genre?.let { add("Género" to it) }
            info?.track?.let { add("Pista" to it) }
            if (song.durationSeconds > 0L) add("Duración" to formatDuration(song.durationSeconds))
            info?.uploader?.let { add("Publicado por" to it) }
        }
        if (rows.isNotEmpty()) Spacer(Modifier.height(10.dp))
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.width(140.dp),
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StoryCard(story: String) {
    var expanded by remember(story) { mutableStateOf(false) }
    InfoCard {
        CardTitle("Sobre la canción")
        Text(
            story,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink.copy(alpha = 0.86f),
            lineHeight = 21.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 6,
            overflow = TextOverflow.Ellipsis,
        )
        if (story.length > 300) {
            Text(
                if (expanded) "Ver menos" else "Ver más",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

private fun formatDuration(seconds: Long): String =
    String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
