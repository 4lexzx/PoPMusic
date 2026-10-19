package com.popmusic.app.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Surface2

private val libraryTabs = listOf("Todo", "Playlists", "Canciones", "Artistas")

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onOpenPlaylist: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Biblioteca",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            libraryTabs.forEachIndexed { index, tab ->
                val selected = index == selectedTab
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary else Surface2,
                            shape = RoundedCornerShape(50),
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        EmptyState(
            title = if (selectedTab == 1) "Aún no tienes playlists" else "Tu biblioteca está vacía",
            subtitle = if (selectedTab == 1) {
                "Crea una playlist y añade tus canciones favoritas."
            } else {
                "Guarda canciones, álbumes y artistas para tenerlos siempre a mano."
            },
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = Faint,
                )
            },
        )
        Spacer(Modifier.height(16.dp))
    }
}
