package com.popmusic.app.ui.screens.playlist

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.components.Artwork
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.components.SectionHeader
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Surface
import com.popmusic.app.ui.theme.Surface2

@Composable
fun PlaylistScreen(
    playlistId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Surface2, Surface, Surface),
                ),
            )
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                )
            }
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.labelLarge,
                color = Faint,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Artwork(
                url = null,
                modifier = Modifier.size(196.dp),
                cornerRadius = 18.dp,
                iconSize = 64.dp,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Playlist $playlistId",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "0 canciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Reproducir playlist",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionHeader(title = "Canciones")
            Spacer(Modifier.height(8.dp))
            EmptyState(
                title = "Playlist vacía",
                subtitle = "Añade canciones desde la búsqueda o desde tu biblioteca.",
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Faint,
                    )
                },
            )
        }
    }
}
