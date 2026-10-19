package com.popmusic.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popmusic.app.ui.components.MediaCard
import com.popmusic.app.ui.components.SectionHeader
import com.popmusic.app.ui.components.TrackRow
import com.popmusic.app.ui.theme.Faint

private data class DemoItem(val title: String, val subtitle: String)

private val demoMixes = listOf(
    DemoItem("Mezcla diaria 1", "PoP Music"),
    DemoItem("Descubrimiento", "Para ti"),
    DemoItem("Noche tranquila", "Relax"),
    DemoItem("Éxitos del momento", "Top 50"),
)

private val demoTracks = listOf(
    DemoItem("Sin señal", "PoP Music"),
    DemoItem("Madrugada", "PoP Music"),
    DemoItem("Vuelve a empezar", "PoP Music"),
    DemoItem("Ciudad dormida", "PoP Music"),
)

@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.statusBarsPadding())
        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "POP MUSIC",
                        style = MaterialTheme.typography.labelMedium,
                        color = Faint,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Buenas noches",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Ajustes",
                        tint = Faint,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Sugerencias para ti")
        }

        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(demoMixes) { item ->
                MediaCard(
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = onOpenPlayer,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionHeader(title = "Escuchado recientemente")
            Spacer(Modifier.height(8.dp))
            demoTracks.forEach { item ->
                TrackRow(
                    title = item.title,
                    artist = item.subtitle,
                    onClick = onOpenPlayer,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
