package com.popmusic.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.popmusic.app.BuildConfig
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.popmusic.app.R
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
import com.popmusic.app.ui.components.PopSlider
import com.popmusic.app.ui.components.animatedPreset
import com.popmusic.app.ui.components.BackgroundPresets
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassDialogHeader
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.theme.Accent
import com.popmusic.app.ui.theme.Black
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Line
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.OnInk
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.LocalGlassOpacity
import com.popmusic.app.ui.theme.LocalLiquidGlassEnabled
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
    val nickname by preferences.nickname.collectAsState()
    val syncAccounts by musicSync.state.collectAsState()
    val syncEvent by musicSync.events.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(syncEvent) {
        val message = syncEvent ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        musicSync.clearEvent()
    }

    val (view, setView) = remember { mutableStateOf<SettingsView?>(null) }
    var filter by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClearDownloads by remember { mutableStateOf(false) }
    // Borrador de Fondos: los cambios (incluida la foto) se ven en la vista
    // previa y solo se guardan al pulsar "Aplicar".
    var backgroundDraft by remember(settings) { mutableStateOf(settings) }

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
            // La foto entra en el borrador: se ve en la vista previa y se
            // guarda con "Aplicar" (no se aplica al elegirla).
            backgroundDraft = backgroundDraft.copy(
                mode = BackgroundMode.IMAGE,
                imageUri = uri.toString(),
            )
        }
    }

    BackHandler(enabled = view != null) { setView(null) }

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
                    onClick = { setView(SettingsView.ACCOUNTS) },
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
                    onClick = { setView(SettingsView.PLAYBACK) },
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
                    onClick = { setView(SettingsView.AUDIO) },
                ),
            ),
            "Apariencia" to listOf(
                RootItem(
                    label = "Cristal y desenfoque",
                    subtitle = "Nitidez, oscurecimiento y transparencia",
                    icon = Icons.Rounded.Palette,
                    onClick = { setView(SettingsView.APPEARANCE) },
                ),
                RootItem(
                    label = "Fondos",
                    subtitle = backgroundLabel(settings),
                    icon = Icons.Rounded.Image,
                    onClick = { setView(SettingsView.CUSTOMIZATION) },
                ),
            ),
            "Aplicación" to listOf(
                RootItem(
                    label = "Descargas",
                    subtitle = "${songCount(library.downloads.size)} · " +
                        if (playback.downloadWifiOnly) "Solo Wi-Fi" else "Wi-Fi y datos",
                    icon = Icons.Rounded.Download,
                    onClick = { setView(SettingsView.DOWNLOADS) },
                ),
                RootItem(
                    label = "Exportar biblioteca",
                    subtitle = "Copia de seguridad en un archivo",
                    icon = Icons.Rounded.FileDownload,
                    onClick = { setView(SettingsView.EXPORT) },
                ),
                RootItem(
                    label = "Caché y almacenamiento",
                    subtitle = "Libera espacio temporal",
                    icon = Icons.Rounded.Storage,
                    onClick = { setView(SettingsView.STORAGE) },
                ),
                RootItem(
                    label = "Restablecer ajustes",
                    subtitle = "Apariencia y audio de fábrica",
                    icon = Icons.Rounded.RestartAlt,
                    onClick = { confirmReset = true },
                ),
                RootItem(
                    label = "Acerca de PoP Music",
                    subtitle = "Versión ${BuildConfig.VERSION_NAME}",
                    icon = Icons.Rounded.Info,
                    onClick = { setView(SettingsView.ABOUT) },
                ),
            ),
            "Ayuda" to listOf(
                RootItem(
                    label = "Documentación",
                    subtitle = "Cómo funciona PoP Music",
                    icon = Icons.Rounded.MenuBook,
                    onClick = { setView(SettingsView.DOCS) },
                ),
                RootItem(
                    label = "Soporte",
                    subtitle = "Contacta con nosotros",
                    icon = Icons.Rounded.HelpOutline,
                    onClick = { setView(SettingsView.SUPPORT) },
                ),
            ),
        )

        SettingsRoot(
            filter = filter,
            onFilterChange = { filter = it },
            onBack = onBack,
            groups = groups,
            accountEmail = session?.let { nickname.ifBlank { it.email.ifBlank { "Cuenta conectada" } } },
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
        backgroundDraft = backgroundDraft,
        onDraftChange = { backgroundDraft = it },
        playback = playback,
        preferences = preferences,
        musicSync = musicSync,
        favoritesCount = library.favorites.size,
        playlistCount = library.playlists.size,
        downloadCount = library.downloads.size,
        downloadBytes = libraryRepository.downloadsDir.totalSize(),
        onOpenView = { setView(it) },
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
        onBack = { setView(null) },
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            leadingIcon = {
                Icon(Icons.Rounded.Search, null, tint = Faint, modifier = Modifier.size(19.dp))
            },
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
            text = "PoP Music ${BuildConfig.VERSION_NAME} · Música, letras y podcasts",
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
    backgroundDraft: BackgroundSettings,
    onDraftChange: (BackgroundSettings) -> Unit,
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
                // Borrador: fondo, foto, desenfoque y oscurecimiento se ven
                // en la vista previa y se guardan juntos al pulsar "Aplicar".
                // La transparencia del cristal no es un ajuste de fondo: se
                // cambia en "Cristal y desenfoque" y se aplica al instante.
                val draft = backgroundDraft
                var fullPreview by remember { mutableStateOf(false) }
                val changed = draft != settings
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BackgroundPhonePreview(settings = draft, onExpand = { fullPreview = true })
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (changed) {
                            "Vista previa · aún no se ha aplicado"
                        } else {
                            "Este es tu fondo actual"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlassTextButton(
                            text = "Pantalla completa",
                            onClick = { fullPreview = true },
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryAction(
                            text = if (changed) "Aplicar fondo" else "Aplicado",
                            onClick = { if (changed) preferences.update { draft } },
                            enabled = changed,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                BackgroundGallery(
                    draft = draft,
                    onChange = onDraftChange,
                    onPickImage = onPickImage,
                )
                if (draft.mode != BackgroundMode.NONE) {
                    Spacer(Modifier.height(14.dp))
                    SettingsCard(title = "Ajustar") {
                        if (draft.mode == BackgroundMode.IMAGE) {
                            SliderRow(
                                title = "Desenfoque",
                                value = draft.backgroundBlur,
                                onValueChange = { onDraftChange(draft.copy(backgroundBlur = it)) },
                            )
                        }
                        SliderRow(
                            title = "Oscurecimiento",
                            value = draft.backgroundDim,
                            onValueChange = { onDraftChange(draft.copy(backgroundDim = it)) },
                        )
                    }
                }
                if (fullPreview) {
                    BackgroundFullPreview(
                        settings = draft,
                        canApply = changed,
                        onApply = { preferences.update { draft } },
                        onClose = { fullPreview = false },
                    )
                }
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
            SettingsView.ABOUT -> AboutDetail(
                onCheckUpdates = onCheckUpdates,
                onOpenDocs = { onOpenView(SettingsView.DOCS) },
            )
            SettingsView.DOCS -> DocumentationDetail()
            SettingsView.SUPPORT -> SupportDetail(onOpenDocs = { onOpenView(SettingsView.DOCS) })
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun AccountsDetail(musicSync: MusicSyncManager) {
    val accounts by musicSync.state.collectAsState()

    // Spotify no admite conexión de cuenta en una app personal (Premium del
    // dueño + máximo 5 cuentas). Su importación va por enlace y su exportación
    // por TuneMyMusic, así que aquí solo se ofrece YouTube.
    listOf(MusicService.YOUTUBE).forEach { service ->
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
    PlayerPhonePreview(playback = playback)
    SettingsCard(title = "Transición") {
        SwitchRow(
            title = "Transición suave",
            subtitle = "Fundido de entrada al empezar cada canción",
            checked = playback.fadeIn,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(fadeIn = enabled) }
            },
        )
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
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
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        SwitchRow(
            title = "Omitir silencios",
            subtitle = "Salta automáticamente los tramos sin sonido",
            checked = playback.skipSilence,
            onCheckedChange = { enabled ->
                preferences.updatePlayback { it.copy(skipSilence = enabled) }
            },
        )
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
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
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
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
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
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
            HorizontalDivider(color = Ink.copy(alpha = 0.06f))
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
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
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

    // El cristal líquido se activa al instante desde el interruptor; la
    // vista previa de esta sección se elimina porque no aportaba cambio
    // visible. La previsualización de fondos está en "Fondos".
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
private fun AboutDetail(
    onCheckUpdates: () -> Unit,
    onOpenDocs: () -> Unit,
) {
    SettingsCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // La imagen de perfil de la app ES el logo; al lado va el nombre
            // en texto normal (antes se repetía el logo como wordmark).
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = "Logo de PoP Music",
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(15.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "PoP Music",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Versión ${BuildConfig.VERSION_NAME} · GPLv3",
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
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        InfoRow("GitHub", "github.com/4lexzx")
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        InfoRow("Nacionalidad", "Peruana")
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        InfoRow("Contacto / soporte", "solisumboalexander@hotmail.com")
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        InfoRow("Repositorio", "github.com/4lexzx/PoPMusic")
    }
    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Detalles técnicos") {
        InfoRow("Fuente de audio", "YouTube (NewPipe)")
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        InfoRow("Letras", "LRCLIB")
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        InfoRow("Reconocimiento", "AudD")
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
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
            text = "Guía de uso de la app: búsqueda, reproducción, letras, " +
                "biblioteca, sincronización, descargas y ajustes.",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )
        Spacer(Modifier.height(12.dp))
        PrimaryAction(text = "Abrir documentación", onClick = onOpenDocs)
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Surface2),
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Volver",
                tint = Ink,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, Ink.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
    ) {
        items.forEachIndexed { index, item ->
            SettingsRow(item = item)
            if (index != items.lastIndex) {
                HorizontalDivider(
                    color = Ink.copy(alpha = 0.06f),
                    modifier = Modifier.padding(start = 64.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Surface3),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
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
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Ink.copy(alpha = 0.05f)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
    @Suppress("UNUSED_PARAMETER") padded: Boolean = true,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnInk,
                checkedTrackColor = Ink,
                checkedBorderColor = Ink,
                uncheckedThumbColor = Muted,
                uncheckedTrackColor = Surface3,
                uncheckedBorderColor = Ink.copy(alpha = 0.10f),
            ),
        )
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
                color = Muted,
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
        targetValue = if (selected) Ink else Surface3,
        animationSpec = tween(180),
        label = "qualityBg",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(
                1.dp,
                Ink.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp),
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
            color = if (selected) OnInk else Muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Ink else Surface3)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = Black),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.97f)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) OnInk else Muted,
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
            color = Muted,
        )
    }
}

/** Guía de uso completa (Ajustes → Ayuda → Documentación). */
@Composable
private fun DocumentationDetail() {
    DocSection(
        "Primeros pasos",
        "La barra inferior tiene Inicio, Buscar, Reconocer y Biblioteca. En Buscar " +
            "escribe un artista o canción y toca el resultado para reproducirlo. " +
            "El micrófono de Reconocer escucha unos segundos y te dice qué suena a " +
            "tu alrededor. Toca la carátula del reproductor para ver la letra " +
            "sincronizada y deslízala hacia abajo para abrir el panel de letras.",
    )
    DocSection(
        "Reproductor",
        "Los controles son anterior, play/pausa y siguiente. Aleatorio y Repetir " +
            "están en la fila pequeña junto a Letras y Temporizador. El botón ⋮ " +
            "de arriba abre Información, Compartir, Ver en YouTube, Establecer " +
            "como tono y Añadir a la cola. Añadir a la cola busca canciones y las " +
            "encola sin reproducirlas ni interrumpir la canción actual. El " +
            "Temporizador detiene la música a los 15, 30, 45, 60 o 90 minutos.",
    )
    DocSection(
        "Letras e información",
        "La letra sincronizada resalta la línea que suena. En la pantalla de " +
            "Letras puedes cambiar el estilo con el botón «Aa» y ajustar el desfase " +
            "si va adelantada o atrasada. El botón Información del reproductor " +
            "muestra foto del artista, biografía, álbum y datos de la canción.",
    )
    DocSection(
        "Biblioteca y sincronización",
        "En Biblioteca tienes Favoritos, Descargas, Recientes y tus playlists. " +
            "Puedes crear playlists, renombrarlas y ordenarlas. El botón «+» abre " +
            "el importador: pega un enlace de YouTube o de Spotify para traer una " +
            "playlist completa. Si vinculas tu cuenta en Ajustes → Cuentas, las " +
            "playlists se sincronizan con YouTube o Spotify; también puedes elegir " +
            "el destino desde el menú ⋮ de cada playlist.",
    )
    DocSection(
        "Descargas y reproducción offline",
        "En el menú ⋮ del reproductor, o en los botones de la tarjeta de canción, " +
            "tienes Descargar. La canción se guarda en el teléfono y aparece en " +
            "Biblioteca → Descargas. El reproductor prefiere el archivo local, así " +
            "que sigue funcionando sin internet. Compartir y Establecer como tono " +
            "también están en el menú ⋮.",
    )
    DocSection(
        "Salas y podcasts",
        "El icono de grupo de la cabecera del Inicio abre Salas: crea una sala con " +
            "código de 7 caracteres, compártelo y todos pueden controlar la cola " +
            "y añadir canciones desde su música o desde Buscar. La pestaña Podcasts " +
            "del Inicio busca programas reales, permite suscribirse y reproduce " +
            "sus episodios directamente.",
    )
    DocSection(
        "Apariencia y cuenta",
        "En Ajustes → Apariencia puedes cambiar el fondo (color, degradado o foto), " +
            "el desenfoque y el oscurecimiento. La vista previa muestra los cambios " +
            "en vivo y se guardan con Aplicar. En Cristal y desenfoque ajustas la " +
            "transparencia, que se aplica al instante. Si inicias sesión, tus " +
            "me gusta y tu biblioteca se guardan en la cuenta.",
    )
}

/** Contacto y ayuda (Ajustes → Ayuda → Soporte). */
@Composable
private fun SupportDetail(onOpenDocs: () -> Unit) {
    val context = LocalContext.current
    SettingsCard(title = "Contacto") {
        ExternalLinkRow(
            label = "Correo",
            value = "solisumboalexander@hotmail.com",
            url = "mailto:solisumboalexander@hotmail.com",
        )
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        ExternalLinkRow(
            label = "Informes en GitHub",
            value = "github.com/4lexzx/PoPMusic/issues",
            url = "https://github.com/4lexzx/PoPMusic/issues",
        )
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        ExternalLinkRow(
            label = "Sitio web",
            value = "4lexzx.github.io/PoPMusic",
            url = "https://4lexzx.github.io/PoPMusic/",
        )
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        ExternalLinkRow(
            label = "Documentación",
            value = "Guía de uso dentro de la app",
            onClick = onOpenDocs,
        )
    }
    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Antes de escribirnos") {
        Text(
            text = "1. Comprueba que la app esté actualizada en Ajustes → " +
                "Acerca de PoP Music → Buscar actualizaciones.\n" +
                "2. Si es un problema de audio, prueba a reiniciar la canción y " +
                "comprueba tu conexión.\n" +
                "3. Indica qué pantalla usabas, qué esperabas que pasara y qué " +
                "ocurrió. Si puedes, adjunta una captura de pantalla.\n" +
                "4. Envíalo por correo o abre un informe en GitHub. Respondemos " +
                "lo antes posible.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
    }
    Spacer(Modifier.height(10.dp))
    SettingsCard(title = "Preguntas frecuentes") {
        FaqItem(
            question = "¿La música sigue sonando al cerrar la app?",
            answer = "No. Si cierras la ventana de recientes, la app se cierra del " +
                "todo: se borra la cola guardada y al volver se abre como la " +
                "primera vez, sin retomar la última canción.",
        )
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        FaqItem(
            question = "¿Se puede escuchar sin internet?",
            answer = "Sí, si has descargado antes las canciones. Aparecen en " +
                "Biblioteca → Descargas y se reproducen desde el archivo local.",
        )
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
        FaqItem(
            question = "¿Por qué no veo mis playlists de Spotify?",
            answer = "La importación por enlace funciona sin iniciar sesión. Para " +
                "la sincronización automática hay que completar la configuración " +
                "de Spotify en el proveedor de la app.",
        )
    }
}

@Composable
private fun DocSection(title: String, body: String) {
    SettingsCard(title = title) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = question,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
    }
}

@Composable
private fun ExternalLinkRow(
    label: String,
    value: String,
    url: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (onClick != null) {
                    onClick()
                } else if (url != null) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Ink.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = Muted,
        modifier = Modifier.padding(start = 22.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueLabel: String = "${(value * 100).toInt()}%",
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        SliderHeader(title, valueLabel)
        PopSlider(value = value, onValueChange = onValueChange, label = title)
    }
}

/** Título a la izquierda y valor en una pastilla a la derecha. */
@Composable
private fun SliderHeader(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Surface3)
                .padding(horizontal = 10.dp, vertical = 4.dp),
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
    // Sólida como el resto de las cards de Ajustes (sin cristal).
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Surface3),
                contentAlignment = Alignment.Center,
            ) {
                // Ítem 2: la foto de cuenta (la misma que en Perfil).
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp),
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
                    color = Muted,
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
                            if (selected) Ink else Surface3,
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
                        color = if (selected) OnInk else Muted,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Ink.copy(alpha = 0.06f))
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SliderHeader(
            title,
            if (value >= 0f) "+${"%.1f".format(value)} dB" else "${"%.1f".format(value)} dB",
        )
        PopSlider(
            value = value.coerceIn(EQ_MIN_DB, EQ_MAX_DB),
            onValueChange = onValueChange,
            range = EQ_MIN_DB..EQ_MAX_DB,
            step = 0.5f,
            centered = true,
            label = title,
        )
    }
}

@Composable
private fun SleepTimerSection(
    playback: PlaybackSettings,
    preferences: AppPreferences,
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(playback.sleepAtEpochMs) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(10_000L)
        }
    }
    val remaining = if (playback.sleepAtEpochMs > now) {
        ((playback.sleepAtEpochMs - now + 59_999L) / 60_000L).toInt()
    } else {
        0
    }
    // Mientras se arrastra manda el dedo; al soltar se programa.
    var dragged by remember { mutableStateOf<Float?>(null) }
    val shown = dragged ?: remaining.toFloat().coerceAtMost(MAX_SLEEP_MIN)
    SettingsCard(title = "Temporizador") {
        SliderHeader(
            title = "Detener la música",
            value = if (shown < 1f) "Desactivado" else "${shown.toInt()} min",
        )
        PopSlider(
            value = shown,
            onValueChange = { dragged = it },
            range = 0f..MAX_SLEEP_MIN,
            step = 5f,
            label = "Temporizador en minutos",
            onValueChangeFinished = { minutes ->
                val startedAt = System.currentTimeMillis()
                now = startedAt
                preferences.updatePlayback {
                    it.copy(
                        sleepAtEpochMs = if (minutes < 1f) {
                            0L
                        } else {
                            startedAt + minutes.toLong() * 60_000L
                        },
                    )
                }
                dragged = null
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("No", "30", "60", "90", "120").forEach { mark ->
                Text(mark, style = MaterialTheme.typography.labelSmall, color = Faint)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (remaining > 0 && dragged == null) {
                "Se detendrá en $remaining min. Lleva la barra a «No» para cancelarlo."
            } else {
                "Desliza para elegir cuánto tiempo más quieres escuchar."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
    }
}

private const val MAX_SLEEP_MIN = 120f

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
    BackgroundMode.ANIMATED -> "Animado · ${animatedPreset(settings.presetId).name}"
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
