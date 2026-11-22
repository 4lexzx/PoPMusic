package com.popmusic.app.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.popmusic.app.data.auth.AuthRepository
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.data.music.MusicService
import com.popmusic.app.data.music.MusicSyncManager
import com.popmusic.app.data.music.ServiceSyncState
import com.popmusic.app.data.prefs.AppPreferences
import com.popmusic.app.data.prefs.BackgroundMode
import com.popmusic.app.data.prefs.BackgroundSettings
import com.popmusic.app.data.prefs.PlaybackSettings
import com.popmusic.app.data.prefs.TasteSettings
import com.popmusic.app.ui.components.AppBackground
import com.popmusic.app.ui.components.BackgroundPresets
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassDialogHeader
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.components.PopLogo
import com.popmusic.app.ui.theme.Accent
import com.popmusic.app.ui.theme.Black
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Line
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.Surface3
import com.popmusic.app.ui.theme.Surface
import com.popmusic.app.ui.theme.BgElev
import com.popmusic.app.ui.theme.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class SettingsView {
    ACCOUNTS,
    PLAYBACK,
    AUDIO,
    EQUALIZER,
    DOWNLOADS,
    APPEARANCE,
    CUSTOMIZATION,
    EXPORT,
    STORAGE,
    ABOUT,
    DOCS,
    SUPPORT,
}

private data class RootItem(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    authRepository: AuthRepository,
    libraryRepository: LibraryRepository,
    musicSync: MusicSyncManager,
    onOpenAuth: () -> Unit,
    onOpenTastes: () -> Unit,
    onOpenDownloads: () -> Unit,
    onCheckUpdates: (onResult: (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings by preferences.settings.collectAsState()
    val playback by preferences.playback.collectAsState()
    val taste by preferences.taste.collectAsState()
    val library by libraryRepository.state.collectAsState()
    val session by authRepository.session.collectAsState()
    val syncAccounts by musicSync.state.collectAsState()
    val syncEvent by musicSync.events.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(syncEvent) {
        val message = syncEvent ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        musicSync.clearEvent()
    }

    var view by remember { mutableStateOf<SettingsView?>(null) }
    var filter by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClearDownloads by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val json = libraryRepository.exportJson() ?: error("vacío")
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("sin stream")
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (success) "Biblioteca exportada" else "No se pudo exportar",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            preferences.update { it.copy(mode = BackgroundMode.IMAGE, imageUri = uri.toString()) }
        }
    }

    BackHandler(enabled = view != null) { view = null }

    val current = view
    if (current == null) {
        val tastesSummary = tasteSummary(taste)
        val groups = listOf(
            "Tu música" to listOf(
                RootItem(
                    label = "Tus gustos musicales",
                    subtitle = tastesSummary,
                    icon = Icons.Rounded.Equalizer,
                    onClick = onOpenTastes,
                ),
                RootItem(
                    label = "Cuentas de música",
                    subtitle = buildList {
                        MusicService.entries.forEach { service ->
                            if (syncAccounts[service]?.connected == true) {
                                add("${service.displayName()} conectado")
                            }
                        }
                        if (isEmpty()) add("Sincroniza playlists con YouTube y Spotify")
                    }.joinToString(" · "),
                    icon = Icons.Rounded.Sync,
                    onClick = { view = SettingsView.ACCOUNTS },
                ),
                RootItem(
                    label = "Reproducción",
                    subtitle = buildList {
                        add(if (playback.fadeIn) "Transición suave" else "Sin transición")
                        if (playback.crossfadeSeconds > 0) {
                            add("Crossfade ${playback.crossfadeSeconds} s")
                        }
                        add(if (playback.skipSilence) "Omite silencios" else "Silencios normales")
                        if (playback.sleepAtEpochMs > 0L) add("Temporizador activo")
                    }.joinToString(" · "),
                    icon = Icons.Rounded.PlayArrow,
                    onClick = { view = SettingsView.PLAYBACK },
                ),
                RootItem(
                    label = "Audio y ecualizador",
                    subtitle = "${qualityLabel(playback.audioQuality)} · " +
                        if (playback.equalizerEnabled) {
                            "Ecualizador ${playback.equalizerPreset}"
                        } else {
                            "Ecualizador desactivado"
                        },
                    icon = Icons.Rounded.VolumeUp,
                    onClick = { view = SettingsView.AUDIO },
                ),
            ),
            "Apariencia" to listOf(
                RootItem(
                    label = "Cristal y desenfoque",
                    subtitle = "Nitidez, oscurecimiento y transparencia",
                    icon = Icons.Rounded.Palette,
                    onClick = { view = SettingsView.APPEARANCE },
                ),
                RootItem(
                    label = "Fondos",
                    subtitle = backgroundLabel(settings),
                    icon = Icons.Rounded.Image,
                    onClick = { view = SettingsView.CUSTOMIZATION },
                ),
            ),
            "Aplicación" to listOf(
                RootItem(
                    label = "Descargas",
                    subtitle = "${songCount(library.downloads.size)} · " +
                        if (playback.downloadWifiOnly) "Solo Wi-Fi" else "Wi-Fi y datos",
                    icon = Icons.Rounded.Download,
                    onClick = { view = SettingsView.DOWNLOADS },
                ),
                RootItem(
                    label = "Exportar biblioteca",
                    subtitle = "Copia de seguridad en un archivo",
                    icon = Icons.Rounded.FileDownload,
                    onClick = { view = SettingsView.EXPORT },
                ),
                RootItem(
                    label = "Caché y almacenamiento",
                    subtitle = "Libera espacio temporal",
                    icon = Icons.Rounded.Storage,
                    onClick = { view = SettingsView.STORAGE },
                ),
                RootItem(
                    label = "Restablecer ajustes",
                    subtitle = "Apariencia y audio de fábrica",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = { confirmReset = true },
                ),
                RootItem(
                    label = "Acerca de PoP Music",
                    subtitle = "Versión 0.1.0",
                    icon = Icons.Rounded.Info,
                    onClick = { view = SettingsView.ABOUT },
                ),
            ),
            "Ayuda" to listOf(
                RootItem(
                    label = "Documentación",
                    subtitle = "Cómo funciona PoP Music",
                    icon = Icons.Rounded.MenuBook,
                    onClick = { view = SettingsView.DOCS },
                ),
                RootItem(
                    label = "Soporte",
                    subtitle = "Contacta con nosotros",
                    icon = Icons.Rounded.HelpOutline,
                    onClick = { view = SettingsView.SUPPORT },
                ),
            ),
        )

        SettingsRoot(
            filter = filter,
            onFilterChange = { filter = it },
            onBack = onBack,
            groups = groups,
            accountEmail = session?.let { it.email.ifBlank { "Cuenta conectada" } },
            accountAvatar = session?.avatarUrl,
            onOpenAuth = onOpenAuth,
            onSignOut = { scope.launch { authRepository.signOut() } },
            modifier = modifier,
        )

        if (confirmReset) {
            GlassDialog(onDismiss = { confirmReset = false }) {
                GlassDialogHeader(
                    title = "Restablecer ajustes",
                    subtitle = "Se restaurarán el fondo, el cristal y el audio. " +
                        "Tu música y tu cuenta no se tocan.",
                    onClose = { confirmReset = false },
                )
                Spacer(Modifier.height(20.dp))
                GlassDialogActions {
                    GlassTextButton(
                        text = "Cancelar",
                        onClick = { confirmReset = false },
                        modifier = Modifier.weight(1f),
                    )
                    GlassPrimaryButton(
                        text = "Restablecer",
                        onClick = {
                            preferences.resetAppearanceAndAudio()
                            confirmReset = false
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (confirmClearDownloads) {
            GlassDialog(onDismiss = { confirmClearDownloads = false }) {
                GlassDialogHeader(
                    title = "Borrar descargas",
                    subtitle = "Se eliminarán ${songCount(library.downloads.size)} del almacenamiento.",
                    onClose = { confirmClearDownloads = false },
                )
                Spacer(Modifier.height(20.dp))
                GlassDialogActions {
                    GlassTextButton(
                        text = "Cancelar",
                        onClick = { confirmClearDownloads = false },
                        modifier = Modifier.weight(1f),
                    )
                    GlassPrimaryButton(
                        text = "Borrar todo",
                        onClick = {
                            library.downloads.forEach { libraryRepository.removeDownload(it.id) }
                            confirmClearDownloads = false
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        return
    }

    SettingsDetail(
        view = current,
        settings = settings,
        playback = playback,
        preferences = preferences,
        musicSync = musicSync,
        favoritesCount = library.favorites.size,
        playlistCount = library.playlists.size,
        downloadCount = library.downloads.size,
        downloadBytes = libraryRepository.downloadsDir.totalSize(),
        onOpenView = { view = it },
        onPickImage = { imagePicker.launch("image/*") },
        onExport = { exportLauncher.launch("popmusic-biblioteca.json") },
        onOpenDownloads = onOpenDownloads,
        onCheckUpdates = {
            onCheckUpdates { available ->
                Toast.makeText(
                    context,
                    if (available) "Hay una actualización disponible" else "Tienes la última versión",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        onRequestClearDownloads = { confirmClearDownloads = true },
        onBack = { view = null },
        modifier = modifier,
    )

    if (confirmClearDownloads) {
        GlassDialog(onDismiss = { confirmClearDownloads = false }) {
            GlassDialogHeader(
                title = "Borrar descargas",
                subtitle = "Se eliminarán ${songCount(library.downloads.size)} del almacenamiento.",
                onClose = { confirmClearDownloads = false },
            )
            Spacer(Modifier.height(20.dp))
            GlassDialogActions {
                GlassTextButton(
                    text = "Cancelar",
                    onClick = { confirmClearDownloads = false },
                    modifier = Modifier.weight(1f),
                )
                GlassPrimaryButton(
                    text = "Borrar todo",
                    onClick = {
                        library.downloads.forEach { libraryRepository.removeDownload(it.id) }
                        confirmClearDownloads = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SettingsRoot(
    filter: String,
    onFilterChange: (String) -> Unit,
    onBack: () -> Unit,
    groups: List<Pair<String, List<RootItem>>>,
    accountEmail: String?,
    accountAvatar: String?,
    onOpenAuth: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val term = filter.trim()
    val matches = if (term.isBlank()) {
        emptyList()
    } else {
        groups.flatMap { it.second }
            .filter { item ->
                item.label.contains(term, ignoreCase = true) ||
                    item.subtitle.contains(term, ignoreCase = true)
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 140.dp),
    ) {
        SettingsHeader(title = "Ajustes", onBack = onBack)

        AccountCard(
            email = accountEmail,
            avatarUrl = accountAvatar,
            onOpenAuth = onOpenAuth,
            onSignOut = onSignOut,
        )

        GlassTextField(
            value = filter,
            onValueChange = onFilterChange,
            placeholder = "Buscar ajustes…",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        if (term.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            if (matches.isEmpty()) {
                Text(
                    text = "Sin resultados para «$term».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                SettingsGroup(title = "Resultados", items = matches)
            }
            return@Column
        }

        groups.forEach { (title, items) ->
            SettingsGroup(title = title, items = items)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "PoP Music 0.1.0 · Música, letras y podcasts",
            style = MaterialTheme.typography.labelMedium,
            color = Faint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsDetail(
    view: SettingsView,
    settings: BackgroundSettings,
    playback: PlaybackSettings,
    preferences: AppPreferences,
    musicSync: MusicSyncManager,
    favoritesCount: Int,
    playlistCount: Int,
    downloadCount: Int,
    downloadBytes: Long,
    onOpenView: (SettingsView) -> Unit,
    onPickImage: () -> Unit,
    onExport: () -> Unit,
    onOpenDownloads: () -> Unit,
    onCheckUpdates: () -> Unit,
    onRequestClearDownloads: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 140.dp),
    ) {
        SettingsHeader(title = detailTitle(view), onBack = onBack)
        Spacer(Modifier.height(6.dp))

        when (view) {
            SettingsView.ACCOUNTS -> AccountsDetail(musicSync)
            SettingsView.PLAYBACK -> PlaybackDetail(playback, preferences)
            SettingsView.AUDIO -> AudioDetail(playback, preferences, onOpenView)
            SettingsView.EQUALIZER -> EqualizerSection(playback, preferences)
            SettingsView.DOWNLOADS -> DownloadsDetail(
                playback = playback,
                preferences = preferences,
                downloadCount = downloadCount,
                downloadBytes = downloadBytes,
                onOpenDownloads = onOpenDownloads,
                onRequestClearDownloads = onRequestClearDownloads,
            )

            SettingsView.APPEARANCE -> AppearanceDetail(settings, playback, preferences)
            SettingsView.CUSTOMIZATION -> {
                // Borrador: el usuario ve el cambio en la vista previa y confirma
                // con "Aplicar" (ítem 4). Las imágenes (galería) se aplican al
                // elegirlas porque el selector devuelve la URI.
                var draft by remember(settings) { mutableStateOf(settings) }
                SectionLabel("Fondos dinámicos")
                BackgroundChoices(
                    settings = draft,
                    onSelectPreset = { index ->
                        draft = draft.copy(mode = BackgroundMode.PRESET, presetId = index)
                    },
                    onSelectNone = { draft = draft.copy(mode = BackgroundMode.NONE) },
                    onPickImage = onPickImage,
                )
                Spacer(Modifier.height(18.dp))
                SectionLabel("Vista previa")
                AppScreenPreview(settings = draft, playback = playback, glassOpacity = draft.glassOpacity)
                Spacer(Modifier.height(12.dp))
                PrimaryAction(
                    text = if (draft != settings) "Aplicar" else "Aplicado",
                    onClick = { if (draft != settings) preferences.update { draft } },
                )
                Spacer(Modifier.height(20.dp))
            }

            SettingsView.EXPORT -> {
                SettingsCard(title = "Exportar biblioteca") {
                    Text(
                        text = "Se guardará un archivo JSON con tus canciones favoritas, " +
                            "tus playlists y la lista de descargas. Podrás usarlo como copia " +
                            "de seguridad o para importarlo en otro dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Faint,
                    )
                    Spacer(Modifier.height(14.dp))
                    InfoRow("Favoritos", songCount(favoritesCount))
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        "Playlists",
                        if (playlistCount == 1) "1 playlist" else "$playlistCount playlists",
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Descargas", songCount(downloadCount))
                    Spacer(Modifier.height(16.dp))
                    PrimaryAction(text = "Guardar archivo", onClick = onExport)
                }
            }

            SettingsView.STORAGE -> StorageDetail(preferences)
            SettingsView.ABOUT -> AboutDetail(onCheckUpdates = onCheckUpdates)
            SettingsView.DOCS -> SupportText(
                "PoP Music reproduce música desde YouTube, muestra letras sincronizadas con " +
                    "LRCLIB y permite importar playlists. Usa el buscador para encontrar " +
                    "canciones o el micrófono para reconocer la música que suena a tu alrededor.",
            )

            SettingsView.SUPPORT -> SupportText(
                "¿Necesitas ayuda? Escríbenos y te responderemos lo antes posible.",
            )
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun AccountsDetail(musicSync: MusicSyncManager) {
    val accounts by musicSync.state.collectAsState()

    MusicService.entries.forEach { service ->
        val state = accounts[service] ?: ServiceSyncState(service = service)
        SettingsCard(title = service.displayName()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            state.connected -> "Cuenta vinculada"
                            !state.configured -> "Sin configurar"
                            else -> "No conectada"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = when {
                            state.connected ->
                                if (state.working) "Sincronizando…" else "Sincronizada"
                            !state.configured ->
                                "Falta el Client ID en local.properties"
                            else -> "Toca Conectar para iniciar sesión"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Faint,
                    )
                }
                if (state.importedPlaylists > 0) {
                    Text(
                        text = "${state.importedPlaylists} playlists",
                        style = MaterialTheme.typography.labelSmall,
                        color = Faint,
                    )
                }
            }

            state.message?.let { message ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }

            Spacer(Modifier.height(12.dp))
            if (state.connected) {
                PrimaryAction(
                    text = if (state.working) "Sincronizando…" else "Importar playlists ahora",
                    onClick = { if (!state.working) musicSync.importFrom(service) },
                )
                Spacer(Modifier.height(8.dp))
                LinkRow(
                    title = "Subir mis playlists",
                    subtitle = "Crea en ${service.displayName()} las playlists y canciones que aún no están allí",
                    onClick = { musicSync.pushAll(service) },
                )
                LinkRow(
                    title = "Desvincular cuenta",
                    subtitle = "La biblioteca local se conserva",
                    onClick = { musicSync.disconnect(service) },
                )
            } else {
                PrimaryAction(
                    text = if (state.configured) "Conectar" else "Configurar credenciales",
                    onClick = { musicSync.connect(service) },
                )
            }
        }
    }
}

@Composable
private fun PlaybackDetail(playback: PlaybackSettings, preferences: AppPreferences) {
    SettingsCard(title = "Transición") {
        SwitchRow(
            title = "Transición suave",
            subtitle = "Fundido de entrada al empezar cada canción",
            checked = playback.fadeIn,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(fadeIn = enabled) }
            },
        )
        HorizontalDivider(color = Line)
        SliderRow(
            title = "Crossfade",
            value = playback.crossfadeSeconds / 10f,
            valueLabel = if (playback.crossfadeSeconds <= 0) {
                "Desactivado"
            } else {
                "${playback.crossfadeSeconds} s"
            },
            onValueChange = { sliderValue ->
                val seconds = (sliderValue * 10f + 0.5f).toInt().coerceIn(0, 10)
                preferences.updatePlayback { it.copy(crossfadeSeconds = seconds) }
            },
        )
        HorizontalDivider(color = Line)
        SwitchRow(
            title = "Omitir silencios",
            subtitle = "Salta automáticamente los tramos sin sonido",
            checked = playback.skipSilence,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(skipSilence = enabled) }
            },
        )
        HorizontalDivider(color = Line)
        SwitchRow(
            title = "Reproducción instantánea",
            subtitle = "Sin fundidos y precarga más canciones por adelantado. Empieza antes y el aleatorio es inmediato.",
            checked = playback.instantPlay,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(instantPlay = enabled) }
            },
        )
    }

    SettingsCard(title = "Reproducción inteligente") {
        SwitchRow(
            title = "Pausa al silenciar",
            subtitle = "Se pausa solo cuando el móvil pasa a vibrar o silencio",
            checked = playback.pauseOnMute,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(pauseOnMute = enabled) }
            },
        )
        HorizontalDivider(color = Line)
        SwitchRow(
            title = "Reanudar al conectar",
            subtitle = "Continúa la reproducción al reconectar auriculares o Bluetooth",
            checked = playback.resumeOnHeadset,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(resumeOnHeadset = enabled) }
            },
        )
    }

    SettingsCard(title = "Filtros de contenido") {
        SwitchRow(
            title = "Ocultar videos",
            subtitle = "Deja fuera resultados que son vídeos (video oficial, videoclip…)",
            checked = playback.hideVideos,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(hideVideos = enabled) }
            },
        )
        HorizontalDivider(color = Line)
        SwitchRow(
            title = "Ocultar Shorts",
            subtitle = "Deja fuera piezas cortas de 70 segundos o menos",
            checked = playback.hideShorts,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(hideShorts = enabled) }
            },
        )
    }

    SleepTimerSection(playback = playback, preferences = preferences)

    // Vista previa viva: los chips cambian al activar/desactivar ajustes.
    Spacer(Modifier.height(18.dp))
    SectionLabel("Vista previa")
    PlaybackPreview(playback = playback)
}

@Composable
private fun AudioDetail(
    playback: PlaybackSettings,
    preferences: AppPreferences,
    onOpenView: (SettingsView) -> Unit,
) {
    SettingsCard(title = "Calidad de audio") {
        Text(
            text = "Afecta al consumo de datos. La calidad alta usa más ancho de banda.",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QualityChip(
                label = "Baja",
                selected = playback.audioQuality == "baja",
                onClick = { preferences.updatePlayback { it.copy(audioQuality = "baja") } },
                modifier = Modifier.weight(1f),
            )
            QualityChip(
                label = "Media",
                selected = playback.audioQuality == "media",
                onClick = { preferences.updatePlayback { it.copy(audioQuality = "media") } },
                modifier = Modifier.weight(1f),
            )
            QualityChip(
                label = "Alta",
                selected = playback.audioQuality == "alta",
                onClick = { preferences.updatePlayback { it.copy(audioQuality = "alta") } },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Volumen") {
        SliderRow(
            title = "Refuerzo de volumen",
            valueLabel = if (playback.volumeBoostDb <= 0f) {
                "Desactivado"
            } else {
                "+${"%.0f".format(playback.volumeBoostDb)} dB"
            },
            value = playback.volumeBoostDb / MAX_BOOST_DB,
            onValueChange = { fraction ->
                val db = (fraction * MAX_BOOST_DB * 2f).toInt() / 2f
                preferences.updatePlayback { it.copy(volumeBoostDb = db) }
            },
        )
        Text(
            text = "Sube el nivel en auriculares o altavoces pequeños. Si distorsiona, bájalo.",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )
    }

    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Ecualizador") {
        SwitchRow(
            title = "Ecualizador",
            subtitle = "Ajusta bajos, voces y agudos con presets",
            checked = playback.equalizerEnabled,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(equalizerEnabled = enabled) }
            },
            padded = false,
        )
        Spacer(Modifier.height(4.dp))
        LinkRow(
            title = "Preset actual: ${playback.equalizerPreset}",
            subtitle = "Abrir ecualizador de 5 bandas",
            onClick = { onOpenView(SettingsView.EQUALIZER) },
        )
    }
}

@Composable
private fun DownloadsDetail(
    playback: PlaybackSettings,
    preferences: AppPreferences,
    downloadCount: Int,
    downloadBytes: Long,
    onOpenDownloads: () -> Unit,
    onRequestClearDownloads: () -> Unit,
) {
    SettingsCard(title = "Descargas") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                tint = Faint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = songCount(downloadCount),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = formatBytes(downloadBytes) + " en el dispositivo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassTextButton(
                text = "Ver descargas",
                onClick = onOpenDownloads,
                modifier = Modifier.weight(1f),
                enabled = downloadCount > 0,
            )
            GlassTextButton(
                text = "Borrar todo",
                onClick = onRequestClearDownloads,
                modifier = Modifier.weight(1f),
                enabled = downloadCount > 0,
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Datos") {
        SwitchRow(
            title = "Descargar solo por Wi-Fi",
            subtitle = "Evita gastar datos móviles al descargar música",
            checked = playback.downloadWifiOnly,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(downloadWifiOnly = enabled) }
            },
            icon = Icons.Rounded.Wifi,
        )
    }
}

@Composable
private fun AppearanceDetail(
    settings: BackgroundSettings,
    playback: PlaybackSettings,
    preferences: AppPreferences,
) {
    SettingsCard(title = "Cristal") {
        // Los ajustes del cristal solo aparecen con "Cristal líquido"
        // activado (como pidió el usuario): si está apagado, la tarjeta
        // solo muestra el interruptor y una nota.
        if (settings.liquidGlass) {
            SliderRow(
                title = "Desenfoque del fondo",
                value = settings.backgroundBlur,
                onValueChange = { value ->
                    preferences.update { it.copy(backgroundBlur = value) }
                },
            )
            Spacer(Modifier.height(8.dp))
            SliderRow(
                title = "Oscurecimiento",
                value = settings.backgroundDim,
                onValueChange = { value ->
                    preferences.update { it.copy(backgroundDim = value) }
                },
            )
            Spacer(Modifier.height(8.dp))
            SliderRow(
                title = "Transparencia del cristal",
                value = settings.glassOpacity,
                onValueChange = { value ->
                    preferences.update { it.copy(glassOpacity = value) }
                },
            )
            HorizontalDivider(color = Line)
        } else {
            Text(
                text = "Activa «Cristal líquido» para ajustar el desenfoque, " +
                    "el oscurecimiento y la transparencia del cristal.",
                style = MaterialTheme.typography.bodyMedium,
                color = Faint,
            )
            Spacer(Modifier.height(12.dp))
        }
        // Item 10 (tanda 2) + item 13 (tanda 3): la lente líquida queda
        // como opción y SOLO afecta a la barra de navegación.
        SwitchRow(
            title = "Cristal líquido",
            subtitle = "Lente líquida en la barra de navegación; apáralo para una nav sólida",
            checked = settings.liquidGlass,
            onCheckedChange = { enabled ->
                preferences.update { it.copy(liquidGlass = enabled) }
            },
            padded = false,
        )
    }
    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Reproductor") {
        SwitchRow(
            title = "Ocultar miniatura",
            subtitle = "Sin carátula en la pantalla de reproducción; solo controles",
            checked = playback.hideThumbnail,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(hideThumbnail = enabled) }
            },
            padded = false,
        )
        HorizontalDivider(color = Line)
        SwitchRow(
            title = "Canvas ambiental",
            subtitle = "Fondo desenfocado con movimiento lento en el reproductor",
            checked = playback.ambientMotion,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(ambientMotion = enabled) }
            },
            padded = false,
        )
    }
    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Descubre más") {
        SwitchRow(
            title = "Secciones de descubrimiento",
            subtitle = "Añade filas nuevas al final del inicio según exploras",
            checked = playback.showDiscoverSections,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(showDiscoverSections = enabled) }
            },
            padded = false,
        )
    }

    // Vista previa viva al fondo: pantalla de Inicio con el fondo, el
    // desenfoque, la transparencia, la miniatura y la nav (sólida o
    // liquid glass) ACTUALES — todo lo que muevas se ve aquí.
    Spacer(Modifier.height(18.dp))
    SectionLabel("Vista previa")
    AppScreenPreview(settings = settings, playback = playback, glassOpacity = settings.glassOpacity)
}

@Composable
private fun StorageDetail(preferences: AppPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableStateOf<Long?>(null) }
    var cleared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cacheBytes = withContext(Dispatchers.IO) { context.cacheDir.totalSize() }
    }

    SettingsCard(title = "Caché") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
                tint = Faint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (cleared) "Caché vaciada" else "Imágenes y datos temporales",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "No afecta a tu música ni a tus playlists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                )
            }
            Text(
                text = formatBytes(cacheBytes),
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
        }
        Spacer(Modifier.height(14.dp))
        GlassTextButton(
            text = "Vaciar caché",
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                    }
                    cacheBytes = 0L
                    cleared = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AboutDetail(onCheckUpdates: () -> Unit) {
    SettingsCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtworkPlaceholder()
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                PopLogo(
                    contentDescription = "PoP Music",
                    modifier = Modifier.height(19.dp),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Versión 0.1.0 · GPLv3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Faint,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryAction(text = "Buscar actualizaciones", onClick = onCheckUpdates)
    }
    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Creador") {
        InfoRow("Creador", "4lexzx")
        HorizontalDivider(color = Line)
        InfoRow("GitHub", "github.com/4lexzx")
        HorizontalDivider(color = Line)
        InfoRow("Nacionalidad", "Peruana")
        HorizontalDivider(color = Line)
        InfoRow("Contacto / soporte", "solisumboalexander@hotmail.com")
        HorizontalDivider(color = Line)
        InfoRow("Repositorio", "github.com/4lexzx/PoPMusic")
    }
    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Detalles técnicos") {
        InfoRow("Fuente de audio", "YouTube (NewPipe)")
        HorizontalDivider(color = Line)
        InfoRow("Letras", "LRCLIB")
        HorizontalDivider(color = Line)
        InfoRow("Reconocimiento", "AudD")
        HorizontalDivider(color = Line)
        InfoRow("Sincronización", "Supabase (Google/Spotify)")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Reproductor de música con letras sincronizadas, podcasts y " +
                "recomendaciones. Sin anuncios, sin rastreo, sin venta de datos.",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )
    }
    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Documentación") {
        Text(
            text = "Ajustes: los cambios de fondo, cristal y reproducción se " +
                "previsualizan en vivo antes de aplicarlos. Las playlists se " +
                "sincronizan con YouTube o Spotify si vinculas tu cuenta. Los " +
                "me gusta y la biblioteca se guardan en tu cuenta al iniciar " +
                "sesión, así los conservas al cambiar de dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )
    }
}

@Composable
private fun ArtworkPlaceholder() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(Accent, Surface2))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "P",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Black,
        )
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    onBack: () -> Unit,
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
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    items: List<RootItem>,
) {
    SectionLabel(title)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            // Cada elemento es su propia card (estilo Echo): la primera y la
            // última llevan 24 dp fuera y 4 dp dentro; las intermedias 4 dp.
            val shape = when {
                items.size == 1 -> RoundedCornerShape(24.dp)
                index == 0 -> RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp,
                )
                index == items.size - 1 -> RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp,
                )
                else -> RoundedCornerShape(4.dp)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
            ) {
                SettingsRow(item = item)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SettingsRow(item: RootItem) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = item.onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Ink.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Faint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsCard(
    title: String? = null,
    content: @Composable () -> Unit,
) {
    if (title != null) {
        SectionLabel(title)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            content()
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    padded: Boolean = true,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (padded) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Ink.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.98f)
            .padding(horizontal = 0.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Faint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun QualityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) Ink.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
        animationSpec = tween(180),
        label = "qualityBg",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(
                1.dp,
                if (selected) Ink.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Ink else Faint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Ink.copy(alpha = 0.92f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = Black),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.97f)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = BgElev,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
}

@Composable
private fun SupportText(text: String) {
    SettingsCard(title = "Información") {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueLabel: String = "${(value * 100).toInt()}%",
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = Ink,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f),
            ),
        )
    }
}

@Composable
private fun AccountCard(
    email: String?,
    avatarUrl: String?,
    onOpenAuth: () -> Unit,
    onSignOut: () -> Unit,
) {
    SectionLabel("Cuenta")
    // Sólida como el resto de las cards de Ajustes (sin cristal).
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (email == null) Modifier.clickable(onClick = onOpenAuth) else Modifier)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Ink.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                // Ítem 2: la foto de cuenta (la misma que en Perfil).
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(42.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = email ?: "Iniciar sesión o crear cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (email == null) {
                        "Guarda tus playlists y sincroniza tu música"
                    } else {
                        "Sesión iniciada en PoP Music"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }
            Spacer(Modifier.width(10.dp))
            if (email == null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Faint,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = "Salir",
                    style = MaterialTheme.typography.labelLarge,
                    color = Faint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSignOut)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

private val eqPresets: List<Pair<String, List<Float>>> = listOf(
    "Plano" to listOf(0f, 0f, 0f, 0f, 0f),
    "Bajos" to listOf(8f, 5f, 2f, 0f, -2f),
    "Voz" to listOf(1f, 3f, 4f, 3f, 1f),
    "Rock" to listOf(6f, 3f, -1f, 2f, 4f),
    "Electrónica" to listOf(7f, 3f, 1f, 2f, 6f),
)

private val eqBandLabels = listOf("60 Hz", "230 Hz", "980 Hz", "3.6 kHz", "14 kHz")

private const val EQ_MIN_DB = -12f
private const val EQ_MAX_DB = 12f
private const val MAX_BOOST_DB = 12f

@Composable
private fun EqualizerSection(
    playback: PlaybackSettings,
    preferences: AppPreferences,
) {
    SettingsCard(title = "Ecualizador") {
        SwitchRow(
            title = "Activar",
            subtitle = "Personaliza el sonido con 5 bandas",
            checked = playback.equalizerEnabled,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(equalizerEnabled = enabled) }
            },
            padded = false,
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "Presets", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(eqPresets) { _, (name, bands) ->
                val selected = playback.equalizerPreset == name
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) Ink.copy(alpha = 0.16f)
                            else Color.White.copy(alpha = 0.06f),
                        )
                        .border(
                            1.dp,
                            if (selected) Ink.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(50),
                        )
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(bounded = true),
                        ) {
                            preferences.updatePlayback {
                                it.copy(
                                    equalizerPreset = name,
                                    equalizerBands = bands,
                                    equalizerEnabled = true,
                                )
                            }
                        }
                        .pressScale(interaction, pressedScale = 0.94f)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Ink else Faint,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(6.dp))
        eqBandLabels.forEachIndexed { index, label ->
            EqSlider(
                title = label,
                value = playback.equalizerBands.getOrElse(index) { 0f },
                onValueChange = { value ->
                    preferences.updatePlayback { current ->
                        val bands = current.equalizerBands.toMutableList()
                        bands[index] = (value * 100).toInt() / 100f
                        current.copy(equalizerBands = bands, equalizerPreset = "Personalizado")
                    }
                },
            )
        }
        if (playback.equalizerPreset == "Personalizado") {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Preset personalizado",
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
        }
    }
}

@Composable
private fun EqSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (value >= 0f) "+${"%.1f".format(value)} dB"
                else "${"%.1f".format(value)} dB",
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
        }
        Slider(
            value = value.coerceIn(EQ_MIN_DB, EQ_MAX_DB),
            onValueChange = onValueChange,
            valueRange = EQ_MIN_DB..EQ_MAX_DB,
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = Ink,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f),
            ),
        )
    }
}

@Composable
private fun SleepTimerSection(
    playback: PlaybackSettings,
    preferences: AppPreferences,
) {
    SettingsCard(title = "Temporizador") {
        Text(
            text = "Detiene la música automáticamente",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SleepChip(15, playback, preferences)
            SleepChip(30, playback, preferences)
            SleepChip(45, playback, preferences)
            SleepChip(60, playback, preferences)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SleepChip(90, playback, preferences)
            val active = playback.sleepAtEpochMs > 0L
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) Color.White.copy(alpha = 0.06f) else Ink.copy(alpha = 0.16f),
                    )
                    .border(
                        1.dp,
                        if (active) Color.White.copy(alpha = 0.10f) else Ink.copy(alpha = 0.5f),
                        RoundedCornerShape(50),
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(bounded = true),
                    ) {
                        preferences.updatePlayback { it.copy(sleepAtEpochMs = 0L) }
                    }
                    .pressScale(interaction, pressedScale = 0.94f)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Desactivar",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Faint else Ink,
                )
            }
        }
        if (playback.sleepAtEpochMs > 0L) {
            Spacer(Modifier.height(10.dp))
            val remaining = playback.sleepAtEpochMs - System.currentTimeMillis()
            val message = if (remaining > 0L) {
                val minutes = remaining / 60_000L
                if (minutes >= 1) "Se detendrá en $minutes min" else "Deteniendo…"
            } else {
                "Detenido"
            }
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
        }
    }
}

@Composable
private fun SleepChip(
    minutes: Int,
    playback: PlaybackSettings,
    preferences: AppPreferences,
) {
    val interaction = remember { MutableInteractionSource() }
    val selected = playback.sleepAtEpochMs > 0L &&
        kotlin.math.abs(
            playback.sleepAtEpochMs - System.currentTimeMillis() - minutes * 60_000L,
        ) < 15_000L
    Text(
        text = "$minutes min",
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Ink else Faint,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) Ink.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
            )
            .border(
                1.dp,
                if (selected) Ink.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(50),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
            ) {
                val now = System.currentTimeMillis()
                preferences.updatePlayback {
                    it.copy(sleepAtEpochMs = now + minutes * 60_000L)
                }
            }
            .pressScale(interaction, pressedScale = 0.94f)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/**
 * Vista previa viva: una miniatura de la pantalla de Inicio de la MISMA
 * app que refleja los ajustes EN TIEMPO REAL — cambias el fondo → cambia
 * aquí; activas "Cristal líquido" → la barra de navegación pasa de sólida
 * a liquid glass; ocultas la miniatura → la carátula desaparece.
 */
@Composable
private fun AppScreenPreview(
    settings: BackgroundSettings,
    playback: PlaybackSettings,
    glassOpacity: Float = 0.65f,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(300.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
    ) {
        // Fondo real de la app (degradado/imagen + desenfoque + oscurecimiento)
        AppBackground(settings = settings, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // — Cabecera —
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PoP Music",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                PreviewHeaderIcon(Icons.Rounded.History)
                Spacer(Modifier.width(6.dp))
                PreviewHeaderIcon(Icons.Rounded.Person)
            }

            // — Chips de ánimo —
            Row(
                modifier = Modifier.padding(start = 14.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PreviewPill("Para estudiar")
                PreviewPill("Energía")
                PreviewPill("Triste")
            }

            // — Sección + tarjetas —
            Text(
                text = "PARA TI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Faint,
                modifier = Modifier.padding(start = 14.dp, top = 12.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PreviewCard(
                    modifier = Modifier.weight(1f),
                    brush = Brush.linearGradient(listOf(Accent, Color(0xFF7B2FF7))),
                )
                PreviewCard(
                    modifier = Modifier.weight(1f),
                    brush = Brush.linearGradient(listOf(Color(0xFFE04BC8), Color(0xFFFFA51E))),
                )
            }

            Spacer(Modifier.weight(1f))

            // — Mini reproductor (la carátula se oculta con "Ocultar miniatura") —
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (playback.hideThumbnail) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Accent, Surface2))),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Canción de ejemplo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Artista · 3:24",
                        style = MaterialTheme.typography.labelSmall,
                        color = Faint,
                        maxLines = 1,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(18.dp),
                )
            }

            // — Barra de navegación: sólida o liquid glass según el ajuste —
            PreviewNav(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                settings = settings,
            )
        }
    }
}

/**
 * Barra de navegación en miniatura: con "Cristal líquido" APAGADO es
 * sólida (Surface3 + borde fino, como la nav real); ACTIVADO usa
 * liquid glass — el cambio se ve al mover el interruptor.
 */
@Composable
private fun PreviewNav(modifier: Modifier, settings: BackgroundSettings) {
    val pill = RoundedCornerShape(24.dp)
    Box(modifier = modifier.fillMaxWidth()) {
        if (settings.liquidGlass) {
            LiquidGlass(
                modifier = Modifier.fillMaxWidth(),
                shape = pill,
                elevation = 6.dp,
            ) {
                PreviewNavTabs()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pill)
                    .background(Surface3)
                    .border(1.dp, Faint.copy(alpha = 0.30f), pill),
            ) {
                PreviewNavTabs()
            }
        }
    }
}

/** Pestañas de la nav en miniatura: Inicio activo, el resto atenuado. */
@Composable
private fun PreviewNavTabs() {
    val tabs = listOf(
        Icons.Rounded.Home to "Inicio",
        Icons.Rounded.Search to "Buscar",
        Icons.Rounded.Mic to "Reconocer",
        Icons.Rounded.LibraryMusic to "Biblioteca",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEachIndexed { index, (icon, label) ->
            val active = index == 0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) Ink else Faint,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) Ink else Faint,
                    fontSize = 8.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Icono circular de cabecera en la vista previa. */
@Composable
private fun PreviewHeaderIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** Chip de ánimo en la vista previa. */
@Composable
private fun PreviewPill(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Ink,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Tarjeta de contenido en la vista previa (con barra de título falsa). */
@Composable
private fun PreviewCard(modifier: Modifier, brush: Brush) {
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .width(46.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.55f)),
        )
    }
}

/**
 * Vista previa viva de Reproducción: un mini-reproductor de mentira con
 * los ajustes ACTIVOS como chips — cambia al mover los interruptores.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaybackPreview(playback: PlaybackSettings) {
    val active = buildList {
        if (playback.fadeIn) add("Transición suave")
        if (playback.crossfadeSeconds > 0) add("Crossfade ${playback.crossfadeSeconds} s")
        if (playback.skipSilence) add("Omitir silencios")
        if (playback.instantPlay) add("Inicio instantáneo")
        if (playback.pauseOnMute) add("Pausa al silenciar")
        if (playback.resumeOnHeadset) add("Reanudar al conectar")
        if (playback.equalizerEnabled) add("Ecualizador")
        if (playback.hideVideos) add("Sin videos")
        if (playback.hideShorts) add("Sin Shorts")
    }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(min = 168.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Surface3, Surface2)))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Vista previa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            Text(
                text = "Así se reproduce con tus ajustes",
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Accent, Surface2))),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Canción de ejemplo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Artista · 3:24",
                        style = MaterialTheme.typography.bodySmall,
                        color = Faint,
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (active.isEmpty()) {
                Text(
                    text = "Sin ajustes activos: sonará con los valores por defecto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    active.forEach { label -> PreviewChip(label) }
                }
            }
        }
    }
}

/** Chip de ajuste activo dentro de la vista previa. */
@Composable
private fun PreviewChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun BackgroundChoices(
    settings: BackgroundSettings,
    onSelectPreset: (Int) -> Unit,
    onSelectNone: () -> Unit,
    onPickImage: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ChoiceSwatch(
                selected = settings.mode == BackgroundMode.NONE,
                onClick = onSelectNone,
                label = "Sin fondo",
                brush = Brush.linearGradient(listOf(Black, Surface)),
                trailing = {
                    Text(
                        text = "A",
                        color = Faint,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
        itemsIndexed(BackgroundPresets) { index, preset ->
            ChoiceSwatch(
                selected = settings.mode == BackgroundMode.PRESET && settings.presetId == index,
                onClick = { onSelectPreset(index) },
                label = preset.name,
                brush = Brush.linearGradient(preset.colors),
            )
        }
        item {
            ChoiceSwatch(
                selected = settings.mode == BackgroundMode.IMAGE,
                onClick = onPickImage,
                label = "Galería",
                brush = Brush.linearGradient(listOf(Surface3, Surface)),
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun ChoiceSwatch(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    brush: Brush,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val ring by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "swatchRing",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp),
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(brush)
                .border(
                    width = (1f + 2f * ring).dp,
                    color = if (selected) Ink else Color.White.copy(alpha = 0.12f),
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(bounded = false, radius = 34.dp),
                    onClick = onClick,
                )
                .pressScale(interaction, pressedScale = 0.92f),
            contentAlignment = Alignment.Center,
        ) {
            if (trailing != null) {
                trailing()
            }
            if (selected && trailing == null) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Ink else Faint,
            maxLines = 1,
        )
    }
}

private fun detailTitle(view: SettingsView): String = when (view) {
    SettingsView.ACCOUNTS -> "Cuentas de música"
    SettingsView.PLAYBACK -> "Reproducción"
    SettingsView.AUDIO -> "Audio"
    SettingsView.EQUALIZER -> "Ecualizador"
    SettingsView.DOWNLOADS -> "Descargas"
    SettingsView.APPEARANCE -> "Cristal y desenfoque"
    SettingsView.CUSTOMIZATION -> "Fondos"
    SettingsView.EXPORT -> "Exportar biblioteca"
    SettingsView.STORAGE -> "Caché y almacenamiento"
    SettingsView.ABOUT -> "Acerca de"
    SettingsView.DOCS -> "Documentación"
    SettingsView.SUPPORT -> "Soporte"
}

private fun qualityLabel(quality: String): String = when (quality) {
    "baja" -> "Calidad baja"
    "media" -> "Calidad media"
    else -> "Calidad alta"
}

private fun backgroundLabel(settings: BackgroundSettings): String = when (settings.mode) {
    BackgroundMode.NONE -> "Sin fondo"
    BackgroundMode.IMAGE -> "Imagen de la galería"
    BackgroundMode.PRESET -> BackgroundPresets
        .getOrNull(settings.presetId)
        ?.let { "Degradado ${it.name}" }
        ?: "Degradado"
}

private fun tasteSummary(taste: TasteSettings): String {
    if (!taste.completed) return "Sin configurar"
    val genres = taste.genres.size
    val artists = taste.artists.size
    val parts = buildList {
        if (genres > 0) add(if (genres == 1) "1 género" else "$genres géneros")
        if (artists > 0) add(if (artists == 1) "1 artista" else "$artists artistas")
        if (taste.podcast) add("Podcasts")
    }
    return if (parts.isEmpty()) "Sin configurar" else parts.joinToString(" · ")
}

private fun songCount(count: Int): String = when (count) {
    0 -> "Sin canciones"
    1 -> "1 canción"
    else -> "$count canciones"
}

private fun File.totalSize(): Long =
    walkBottomUp()
        .filter { it.isFile }
        .sumOf { it.length() }

private fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "…"
    return when {
        bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
