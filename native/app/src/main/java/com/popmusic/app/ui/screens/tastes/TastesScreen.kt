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
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0C0C16), Color(0xFF06060C))),
            )
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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Ink,
                )
            }
            Spacer(Modifier.size(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tus gustos musicales",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Definen tu inicio y tus recomendaciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = Faint,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // — Géneros —
            SectionTitle(
                "Géneros",
                trailing = if (selectedGenres.isEmpty()) null else "${selectedGenres.size} elegidos",
                icon = Icons.Rounded.GraphicEq,
            )
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
                            selected = genre.label in selectedGenres,
                            onClick = {
                                selectedGenres = if (genre.label in selectedGenres) {
                                    selectedGenres - genre.label
                                } else {
                                    selectedGenres + genre.label
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // — Artistas —
            SectionTitle(
                "Artistas favoritos",
                trailing = "${artists.size} de $MAX_ARTISTS",
                icon = Icons.Rounded.Person,
            )
            Spacer(Modifier.height(10.dp))
            LiquidGlass(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
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
            }

            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ARTIST_SUGGESTIONS
                    .filter { suggestion -> artists.none { it.equals(suggestion, true) } }
                    .forEach { suggestion ->
                        TasteChip(
                            label = suggestion,
                            selected = false,
                            enabled = artists.size < MAX_ARTISTS,
                            onClick = { addArtist(suggestion) },
                        )
                    }
            }

            Spacer(Modifier.height(24.dp))

            // — Podcasts —
            SectionTitle("Podcasts", icon = Icons.Rounded.Podcasts)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TasteChip(
                    label = "Sí, escucho podcasts",
                    selected = podcast,
                    onClick = { podcast = true },
                )
                TasteChip(
                    label = "Solo música",
                    selected = !podcast,
                    onClick = { podcast = false },
                )
            }

            if (podcast) {
                Spacer(Modifier.height(16.dp))
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
                }

                // Tanda 9: escribir temas propios, igual que con los artistas.
                Spacer(Modifier.height(12.dp))
                LiquidGlass(shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Tus propios temas",
                            style = MaterialTheme.typography.labelMedium,
                            color = Faint,
                        )
                        Spacer(Modifier.height(10.dp))
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
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            GlassPrimaryButton(
                text = "Guardar cambios",
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
