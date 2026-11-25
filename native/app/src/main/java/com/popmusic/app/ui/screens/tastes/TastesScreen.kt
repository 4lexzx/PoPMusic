package com.popmusic.app.ui.screens.tastes

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.data.prefs.TasteSettings
import com.popmusic.app.data.recommendations.PODCAST_TOPICS
import com.popmusic.app.data.recommendations.TASTE_GENRES
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.screens.onboarding.AddChip
import com.popmusic.app.ui.screens.onboarding.SectionTitle
import com.popmusic.app.ui.screens.onboarding.SelectedArtistChip
import com.popmusic.app.ui.screens.onboarding.TasteChip
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Muted
import com.popmusic.app.ui.theme.OnInk
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import com.popmusic.app.ui.theme.BgElev
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.LiquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_ARTISTS = 8

private val ARTIST_SUGGESTIONS = listOf(
    "Bad Bunny", "Karol G", "Feid", "Peso Pluma", "Shakira", "Taylor Swift",
    "Coldplay", "Rauw Alejandro", "Rosalía", "Grupo Frontera", "The Weeknd",
    "Dua Lipa", "Quevedo", "Romeo Santos", "Luis Miguel", "Juan Gabriel",
    "Adele", "Bruno Mars", "Manuel Turizo", "Myke Towers",
)

/**
 * Editor de gustos musicales desde Ajustes: una sola pantalla ordenada
 * (géneros → artistas → podcasts) en lugar del asistente de 3 pasos.
 * El asistente sigue existiendo para la primera vez que se abre la app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TastesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as PoPMusicApplication).preferences
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val initial = remember { prefs.taste.value }
    var selectedGenres by remember { mutableStateOf(initial.genres) }
    var artists by remember { mutableStateOf(initial.artists) }
    var podcast by remember { mutableStateOf(initial.podcast) }
    var podcastTopics by remember { mutableStateOf(initial.podcastTopics) }
    var artistInput by remember { mutableStateOf("") }
    var artistInputFocused by remember { mutableStateOf(false) }
    val artistRequester = remember { BringIntoViewRequester() }
    // Tanda 9: temas de podcast escritos a mano ("como en artistas").
    var topicInput by remember { mutableStateOf("") }
    val topicRequester = remember { BringIntoViewRequester() }
    var topicInputFocused by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(artistInputFocused, topicInputFocused) {
        if (artistInputFocused || topicInputFocused) {
            delay(220)
            runCatching {
                if (topicInputFocused) topicRequester.bringIntoView()
                else artistRequester.bringIntoView()
            }
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
            Toast.makeText(context, "Gustos guardados", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    fun addArtist(name: String) {
        val clean = name.trim()
        if (
            clean.isNotBlank() && artists.size < MAX_ARTISTS &&
            artists.none { it.equals(clean, ignoreCase = true) }
        ) {
            artists = artists + clean
            artistInput = ""
        }
    }

    fun addTopic(name: String) {
        val clean = name.trim()
        if (clean.isNotBlank() && podcastTopics.none { it.equals(clean, ignoreCase = true) }) {
            podcastTopics = podcastTopics + clean
            topicInput = ""
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Surface2),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Ink,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tus gustos musicales",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Definen tu inicio y tus recomendaciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            TasteCard(
                title = "Géneros",
                icon = Icons.Rounded.GraphicEq,
                trailing = if (selectedGenres.isEmpty()) null else "${selectedGenres.size} elegidos",
            ) {
                GenreGrid(
                    selected = selectedGenres,
                    onToggle = { label ->
                        selectedGenres = if (label in selectedGenres) {
                            selectedGenres - label
                        } else {
                            selectedGenres + label
                        }
                    },
                )
            }

            Spacer(Modifier.height(12.dp))

            TasteCard(
                title = "Artistas favoritos",
                icon = Icons.Rounded.Person,
                trailing = "${artists.size} de $MAX_ARTISTS",
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassTextField(
                            value = artistInput,
                            onValueChange = { artistInput = it },
                            placeholder = "Escribe un artista…",
                            modifier = Modifier
                                .weight(1f)
                                .bringIntoViewRequester(artistRequester)
                                .onFocusChanged { artistInputFocused = it.isFocused },
                            imeAction = ImeAction.Done,
                            keyboardActions = KeyboardActions(onDone = { addArtist(artistInput) }),
                            capitalization = KeyboardCapitalization.Words,
                        )
                        Spacer(Modifier.size(8.dp))
                        AddChip(
                            enabled = artistInput.isNotBlank() && artists.size < MAX_ARTISTS,
                            onClick = { addArtist(artistInput) },
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
                                    onRemove = { artists = artists - artist },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                ArtistGrid(
                    suggestions = ARTIST_SUGGESTIONS,
                    selected = artists,
                    canAdd = artists.size < MAX_ARTISTS,
                    onToggle = { name ->
                        val current = artists.firstOrNull { it.equals(name, ignoreCase = true) }
                        if (current != null) artists = artists - current else addArtist(name)
                    },
                )
            }

            Spacer(Modifier.height(12.dp))

            TasteCard(title = "Podcasts", icon = Icons.Rounded.Podcasts) {
                PodcastSwitch(podcast = podcast, onChange = { podcast = it })
                if (podcast) {
                    Spacer(Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PODCAST_TOPICS.forEach { topic ->
                            TasteChip(
                                label = topic.label,
                                selected = topic.label in podcastTopics,
                                onClick = {
                                    podcastTopics = if (topic.label in podcastTopics) {
                                        podcastTopics - topic.label
                                    } else {
                                        podcastTopics + topic.label
                                    }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassTextField(
                                value = topicInput,
                                onValueChange = { topicInput = it },
                                placeholder = "Escribe un tema…",
                                modifier = Modifier
                                    .weight(1f)
                                    .bringIntoViewRequester(topicRequester)
                                    .onFocusChanged { topicInputFocused = it.isFocused },
                                imeAction = ImeAction.Done,
                                keyboardActions = KeyboardActions(onDone = { addTopic(topicInput) }),
                                capitalization = KeyboardCapitalization.Sentences,
                            )
                            Spacer(Modifier.size(8.dp))
                            AddChip(
                                enabled = topicInput.isNotBlank(),
                                onClick = { addTopic(topicInput) },
                            )
                        }
                        val customTopics = podcastTopics
                            .filter { own -> PODCAST_TOPICS.none { it.label == own } }
                            .sorted()
                        if (customTopics.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                customTopics.forEach { topic ->
                                    SelectedArtistChip(
                                        artist = topic,
                                        onRemove = { podcastTopics = podcastTopics - topic },
                                        showPhoto = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ink)
                    .clickable { save() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Guardar cambios",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnInk,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
