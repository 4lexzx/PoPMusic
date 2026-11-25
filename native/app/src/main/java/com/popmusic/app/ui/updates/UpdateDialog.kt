package com.popmusic.app.ui.updates

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.popmusic.app.BuildConfig
import com.popmusic.app.R
import com.popmusic.app.data.updates.UpdateInfo
import com.popmusic.app.data.updates.UpdateInstaller
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.theme.BrandBlue
import com.popmusic.app.ui.theme.BrandOrange
import com.popmusic.app.ui.theme.BrandPink
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.pressScale
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
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = tween(180),
        label = "updateProgress",
    )

    GlassDialog(
        onDismiss = onDismiss,
        dismissEnabled = dismissible,
    ) {
        UpdateHeader(info)

        if (info.changelog.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Novedades",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Muted,
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 230.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Ink.copy(alpha = 0.06f))
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                info.changelog.forEach { entry -> ChangelogRow(entry) }
            }
        }

        when (state.phase) {
            UpdatePhase.Downloading -> {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Ink.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(BrandGradient),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (info.sizeBytes > 0L) {
                            "${formatSize((info.sizeBytes * state.progress).toLong())} " +
                                "de ${formatSize(info.sizeBytes)}"
                        } else {
                            "Descargando…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                }
            }

            UpdatePhase.NeedsPermission -> StatusText(
                state.error ?: (
                    "Activa «Permitir desde esta fuente» y vuelve a PoP Music. " +
                        "El instalador se abrirá automáticamente."
                    ),
            )

            UpdatePhase.Ready -> StatusText(
                "La descarga está lista. Toca «Instalar ahora» para continuar.",
            )

            UpdatePhase.Error -> StatusText(
                state.error ?: "No se pudo completar la actualización.",
                MaterialTheme.colorScheme.error,
            )

            else -> Unit
        }

        Spacer(Modifier.height(18.dp))
        UpdateButton(
            text = when (state.phase) {
                UpdatePhase.Downloading -> "Descargando…"
                UpdatePhase.Ready -> "Instalar ahora"
                UpdatePhase.NeedsPermission -> "Permitir instalación"
                UpdatePhase.Error -> "Reintentar"
                else -> "Actualizar ahora"
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
            enabled = !downloading,
        )
        if (dismissible) {
            Spacer(Modifier.height(8.dp))
            GlassTextButton(
                text = "Ahora no",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Logo con halo de marca y el salto de versión: instalada → nueva. */
@Composable
private fun UpdateHeader(info: UpdateInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .background(
                    Brush.radialGradient(
                        listOf(BrandPink.copy(alpha = 0.34f), Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(22.dp)),
            )
        }
        Text(
            text = "Nueva versión disponible",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Ink,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "a",
                tint = Muted,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = info.versionName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BrandGradient)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            if (info.sizeBytes > 0L) {
                Text(
                    text = formatSize(info.sizeBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
            }
        }
    }
}

/** "Título: detalle" se muestra con el título destacado. */
@Composable
private fun ChangelogRow(entry: String) {
    val title = entry.substringBefore(": ", "").takeIf { it.length in 1..40 }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(BrandGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(14.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
            }
            Text(
                text = if (title != null) entry.removePrefix("$title: ").replaceFirstChar { it.uppercase() } else entry,
                style = MaterialTheme.typography.bodyMedium,
                color = if (title != null) Muted else Ink,
            )
        }
    }
}

@Composable
private fun StatusText(text: String, color: Color = Muted) {
    Spacer(Modifier.height(14.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = color,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UpdateButton(text: String, onClick: () -> Unit, enabled: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.97f)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (enabled) {
                    Modifier.background(BrandGradient)
                } else {
                    Modifier.background(Ink.copy(alpha = 0.10f))
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.Black else Muted,
        )
    }
}

private val BrandGradient = Brush.linearGradient(listOf(BrandBlue, BrandPink, BrandOrange))

private fun formatSize(bytes: Long): String =
    String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
