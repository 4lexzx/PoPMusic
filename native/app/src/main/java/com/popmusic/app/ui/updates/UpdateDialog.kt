package com.popmusic.app.ui.updates

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.popmusic.app.data.updates.UpdateInstaller
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.theme.Accent
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import java.util.Locale

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onPermissionResult: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { onPermissionResult() }

    val info = state.info ?: return
    val downloading = state.phase == UpdatePhase.Downloading
    val dismissible = !downloading && !info.mandatory
    val blockShape = RoundedCornerShape(18.dp)
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = tween(180),
        label = "updateProgress",
    )

    GlassDialog(
        onDismiss = onDismiss,
        dismissEnabled = dismissible,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Actualización disponible",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
                Text(
                    text = buildString {
                        append("PoP Music ")
                        append(info.versionName)
                        if (info.sizeBytes > 0L) {
                            append(" · ")
                            append(formatSize(info.sizeBytes))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Hay una versión nueva lista para instalar.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink.copy(alpha = 0.86f),
        )

        if (info.changelog.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 190.dp)
                    .clip(blockShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), blockShape)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                info.changelog.forEach { entry ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink.copy(alpha = 0.90f),
                        )
                    }
                }
            }
        }

        when (state.phase) {
            UpdatePhase.Downloading -> {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(Ink),
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "Descargando… ${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }

            UpdatePhase.NeedsPermission -> {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Activa «Permitir desde esta fuente» y vuelve a PoP Music. " +
                        "El instalador se abrirá automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }

            UpdatePhase.Ready -> {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "La descarga está lista. Toca «Instalar ahora» para continuar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }

            UpdatePhase.Error -> {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = state.error ?: "No se pudo completar la actualización.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            else -> Unit
        }

        Spacer(Modifier.height(16.dp))
        GlassDialogActions {
            if (dismissible) {
                GlassTextButton(
                    text = "Ahora no",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
            GlassPrimaryButton(
                text = when (state.phase) {
                    UpdatePhase.Downloading ->
                        "Descargando ${(state.progress * 100).toInt()}%"
                    UpdatePhase.Ready -> "Instalar ahora"
                    UpdatePhase.NeedsPermission -> "Permitir instalación"
                    UpdatePhase.Error -> "Reintentar"
                    else -> "Descargar e instalar"
                },
                onClick = {
                    when (state.phase) {
                        UpdatePhase.Available, UpdatePhase.Error -> onDownload()
                        UpdatePhase.Ready -> onInstall()
                        UpdatePhase.NeedsPermission -> {
                            if (UpdateInstaller.canInstall(context)) {
                                onInstall()
                            } else {
                                permissionLauncher.launch(
                                    UpdateInstaller.permissionIntent(context),
                                )
                            }
                        }
                        else -> Unit
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !downloading,
            )
        }
    }
}

private fun formatSize(bytes: Long): String =
    String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
