package com.popmusic.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Line

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var wifiOnly by remember { mutableStateOf(true) }
    var crossfade by remember { mutableStateOf(false) }
    var normalize by remember { mutableStateOf(true) }
    var syncedLyrics by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                )
            }
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        Spacer(Modifier.height(8.dp))
        SectionLabel("Reproducción")
        SettingSwitch(
            title = "Transición entre canciones",
            subtitle = "Fundido suave al cambiar de pista",
            checked = crossfade,
            onCheckedChange = { crossfade = it },
        )
        SettingSwitch(
            title = "Normalizar volumen",
            subtitle = "Iguala el nivel entre canciones",
            checked = normalize,
            onCheckedChange = { normalize = it },
        )
        SettingSwitch(
            title = "Letras sincronizadas",
            subtitle = "Muestra la letra mientras suena la canción",
            checked = syncedLyrics,
            onCheckedChange = { syncedLyrics = it },
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Datos")
        SettingSwitch(
            title = "Solo Wi-Fi para audio",
            subtitle = "Evita usar datos móviles al reproducir",
            checked = wifiOnly,
            onCheckedChange = { wifiOnly = it },
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Acerca de")
        InfoRow("Versión", "0.1.0")
        InfoRow("Licencia", "GPLv3")
        InfoRow("Fuente de audio", "YouTube Music")
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Faint,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = Line)
    }
}
