package com.popmusic.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.prefs.AppPreferences
import com.popmusic.app.data.prefs.TasteSettings
import com.popmusic.app.data.recommendations.PODCAST_TOPICS
import com.popmusic.app.data.recommendations.TASTE_GENRES
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.components.PopLogo
import com.popmusic.app.ui.theme.Accent
import com.popmusic.app.ui.theme.BrandBlue
import com.popmusic.app.ui.theme.BrandPink
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.BgElev
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.pressScale
import kotlinx.coroutines.launch

private const val MAX_ARTISTS = 8

private val ARTIST_SUGGESTIONS = listOf(
    "Bad Bunny", "Karol G", "Feid", "Peso Pluma", "Shakira", "Taylor Swift",
    "Coldplay", "Rauw Alejandro", "Rosalía", "Grupo Frontera", "The Weeknd",
    "Dua Lipa", "Quevedo", "Romeo Santos", "Luis Miguel", "Juan Gabriel",
    "Adele", "Bruno Mars", "Manuel Turizo", "Myke Towers",
)

private enum class Step(val title: String, val subtitle: String) {
    GENRES(
        "¿Qué te gusta escuchar?",
        "Elige al menos un género. Con esto armamos tu inicio y tus recomendaciones.",
    ),
    ARTISTS(
        "Tus artistas favoritos",
        "Añade artistas que escuches a menudo. Puedes buscar o tocar una sugerencia.",
    ),
    PODCASTS(
        "¿También escuchas podcasts?",
        "Cuéntanos qué temas te interesan para recomendarte episodios.",
    ),
}

/**
 * Cuestionario inicial de gustos musicales en tres pasos.
 * La primera vez se muestra como overlay al abrir la app; luego se puede
 * editar desde Ajustes (editMode = true).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    isEditMode: Boolean,
    onDone: () -> Unit,
    preferences: AppPreferences? = null,
) {
    val context = LocalContext.current
    val prefs = preferences
        ?: (context.applicationContext as PoPMusicApplication).preferences
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val initial = remember { prefs.taste.value }
    var step by remember { mutableIntStateOf(0) }
    var selectedGenres by remember { mutableStateOf(initial.genres) }
    var artists by remember { mutableStateOf(initial.artists) }
    var podcast by remember { mutableStateOf(initial.podcast) }
    var podcastTopics by remember { mutableStateOf(initial.podcastTopics) }
    var artistInput by remember { mutableStateOf("") }
    var artistInputFocused by remember { mutableStateOf(false) }
    val artistRequester = remember { BringIntoViewRequester() }
    val listState = rememberScrollState()

    LaunchedEffect(artistInputFocused) {
        if (artistInputFocused) {
            // Deja que el IME termine de abrirse antes de desplazar.
            kotlinx.coroutines.delay(220)
            runCatching { artistRequester.bringIntoView() }
        }
    }

    fun save() {
        keyboard?.hide()
        scope.launch {
            prefs.updateTaste {
                TasteSettings(
                    genres = selectedGenres,
                    artists = artists,
                    podcast = podcast,
                    podcastTopics = if (podcast) podcastTopics else emptySet(),
                    completed = true,
                )
            }
            onDone()
        }
    }

    fun next() {
        keyboard?.hide()
        if (step < Step.entries.lastIndex) {
            step++
        } else {
            save()
        }
    }

    fun back() {
        keyboard?.hide()
        if (step > 0) step-- else onDone()
    }

    BackHandler(enabled = isEditMode || step > 0) { back() }

    val current = Step.entries[step]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C0C16), Color(0xFF06060C)),
                ),
            )
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Ítem 4: logo en vez de "PoP Music" (labelLarge 14sp
                // ≈ 11 dp); "Tus gustos musicales" sigue siendo texto.
                if (isEditMode) {
                    Text(
                        text = "Tus gustos musicales",
                        style = MaterialTheme.typography.labelLarge,
                        color = Accent,
                        letterSpacing = 1.2.sp,
                    )
                } else {
                    PopLogo(
                        contentDescription = "PoP Music",
                        modifier = Modifier.height(13.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                StepDots(current = step, total = Step.entries.size)
            }
            if (isEditMode) {
                RoundIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Cerrar",
                    onClick = onDone,
                )
            } else {
                Text(
                    text = "Paso ${step + 1} de ${Step.entries.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Faint,
                )
            }
        }

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val offset = if (forward) 1 else -1
                (slideInHorizontally(tween(260)) { full -> offset * full / 4 } + fadeIn(tween(200)))
                    .togetherWith(
                        slideOutHorizontally(tween(200)) { full -> -offset * full / 6 } +
                            fadeOut(tween(160)),
                    )
            },
            modifier = Modifier.weight(1f),
            label = "onboardingStep",
        ) { active ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            ) {
                Text(
                    text = active.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = active.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Faint,
                )
                Spacer(Modifier.height(20.dp))

                when (active) {
                    Step.GENRES -> GenresStep(
                        selected = selectedGenres,
                        onToggle = { genre ->
                            selectedGenres = if (genre in selectedGenres) {
                                selectedGenres - genre
                            } else {
                                selectedGenres + genre
                            }
                        },
                    )

                    Step.ARTISTS -> ArtistsStep(
                        artists = artists,
                        input = artistInput,
                        onInputChange = { artistInput = it },
                        onAdd = {
                            val name = artistInput.trim()
                            if (name.isNotBlank() && artists.size < MAX_ARTISTS &&
                                artists.none { it.equals(name, ignoreCase = true) }
                            ) {
                                artists = artists + name
                                artistInput = ""
                            }
                        },
                        onRemove = { artists = artists - it },
                        onSuggestion = { name ->
                            if (artists.size < MAX_ARTISTS &&
                                artists.none { it.equals(name, ignoreCase = true) }
                            ) {
                                artists = artists + name
                            }
                        },
                        onFocusChange = { artistInputFocused = it },
                        modifier = Modifier.bringIntoViewRequester(artistRequester),
                    )

                    Step.PODCASTS -> PodcastsStep(
                        podcast = podcast,
                        onPodcastChange = { podcast = it },
                        topics = podcastTopics,
                        onToggleTopic = { label ->
                            podcast = true
                            podcastTopics = if (label in podcastTopics) {
                                podcastTopics - label
                            } else {
                                podcastTopics + label
                            }
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (step > 0) {
                GlassTextAction(
                    text = "Atrás",
                    onClick = { back() },
                    modifier = Modifier.weight(1f),
                )
            } else if (!isEditMode) {
                GlassTextAction(
                    text = "Omitir",
                    onClick = { save() },
                    modifier = Modifier.weight(1f),
                )
            } else {
                GlassTextAction(
                    text = "Cancelar",
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                )
            }
            PrimaryAction(
                text = when {
                    step < Step.entries.lastIndex -> "Continuar"
                    isEditMode -> "Guardar cambios"
                    else -> "Empezar"
                },
                onClick = { next() },
                modifier = Modifier.weight(1.2f),
            )
        }
    }
}

@Composable
private fun GenresStep(
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    SectionTitle("Géneros", trailing = if (selected.isEmpty()) null else "${selected.size} elegidos")
    Spacer(Modifier.height(10.dp))
    LiquidGlass(shape = RoundedCornerShape(24.dp)) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TASTE_GENRES.forEach { genre ->
                TasteChip(
                    label = genre.label,
                    selected = genre.label in selected,
                    onClick = { onToggle(genre.label) },
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Elige 3 o más para mejores resultados.",
        style = MaterialTheme.typography.bodyMedium,
        color = Faint,
    )
}

@Composable
private fun ArtistsStep(
    artists: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlass(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = "Escribe un artista…",
                    modifier = modifier
                        .weight(1f)
                        .onFocusChanged { onFocusChange(it.isFocused) },
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                    capitalization = KeyboardCapitalization.Words,
                )
                Spacer(Modifier.size(8.dp))
                AddChip(
                    enabled = input.isNotBlank() && artists.size < MAX_ARTISTS,
                    onClick = onAdd,
                )
            }
            if (artists.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    artists.forEach { artist ->
                        SelectedArtistChip(
                            artist = artist,
                            onRemove = { onRemove(artist) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${artists.size} de $MAX_ARTISTS artistas",
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
        }
    }

    Spacer(Modifier.height(22.dp))
    SectionTitle("Sugerencias")
    Spacer(Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ARTIST_SUGGESTIONS
            .filter { suggestion -> artists.none { it.equals(suggestion, ignoreCase = true) } }
            .forEach { suggestion ->
                TasteChip(
                    label = suggestion,
                    selected = false,
                    enabled = artists.size < MAX_ARTISTS,
                    onClick = { onSuggestion(suggestion) },
                )
            }
    }
}

@Composable
private fun PodcastsStep(
    podcast: Boolean,
    onPodcastChange: (Boolean) -> Unit,
    topics: Set<String>,
    onToggleTopic: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TasteChip(
            label = "Sí, escucho podcasts",
            selected = podcast,
            onClick = { onPodcastChange(true) },
        )
        TasteChip(
            label = "Solo música",
            selected = !podcast,
            onClick = { onPodcastChange(false) },
        )
    }

    if (podcast) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Podcasts,
                contentDescription = null,
                tint = Faint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            SectionTitle(
                "Temas de podcast",
                trailing = if (topics.isEmpty()) null else "${topics.size} elegidos",
            )
        }
        Spacer(Modifier.height(10.dp))
        LiquidGlass(shape = RoundedCornerShape(24.dp)) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PODCAST_TOPICS.forEach { topic ->
                    TasteChip(
                        label = topic.label,
                        selected = topic.label in topics,
                        onClick = { onToggleTopic(topic.label) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Aparecerán secciones de podcasts en tu inicio.",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )
    } else {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Perfecto. Tu inicio será solo música.",
            style = MaterialTheme.typography.bodyLarge,
            color = Faint,
        )
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        repeat(total) { index ->
            val active = index <= current
            Box(
                modifier = Modifier
                    .size(width = if (active) 22.dp else 8.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Ink else Color.White.copy(alpha = 0.16f)),
            )
        }
    }
}

@Composable
internal fun SectionTitle(text: String, trailing: String? = null, icon: ImageVector? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Faint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Faint,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
            )
        }
    }
}

@Composable
internal fun TasteChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    // Tanda 9: seleccionado = degradado de marca (azul→magenta).
                    selected -> Brush.horizontalGradient(
                        listOf(BrandBlue, BrandPink),
                    )
                    enabled -> SolidColor(Color.White.copy(alpha = 0.06f))
                    else -> SolidColor(Color.White.copy(alpha = 0.03f))
                },
            )
            .border(
                1.dp,
                when {
                    selected -> Color.Transparent
                    enabled -> Color.White.copy(alpha = 0.10f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
                RoundedCornerShape(50),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.94f)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.size(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                selected -> Ink
                enabled -> Muted
                else -> Faint.copy(alpha = 0.45f)
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
internal fun SelectedArtistChip(artist: String, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Accent.copy(alpha = 0.16f))
            .border(1.dp, Accent.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Text(
            text = artist,
            style = MaterialTheme.typography.labelLarge,
            color = Ink,
        )
        Spacer(Modifier.size(6.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 11.dp),
                    onClick = onRemove,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Quitar $artist",
                tint = Ink.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
internal fun AddChip(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) Ink.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (enabled) Ink.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(50),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.94f)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text(
            text = "Añadir",
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Ink else Faint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Ink,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun GlassTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.97f)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Faint,
            textAlign = TextAlign.Center,
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
            .clip(RoundedCornerShape(16.dp))
            .background(Ink.copy(alpha = 0.92f))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = Color.Black),
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
            color = BgElev,
            textAlign = TextAlign.Center,
        )
    }
}
