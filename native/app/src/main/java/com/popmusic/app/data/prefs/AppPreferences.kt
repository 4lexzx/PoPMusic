package com.popmusic.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BackgroundMode { NONE, PRESET, IMAGE, ANIMATED }

data class BackgroundSettings(
    val mode: BackgroundMode = BackgroundMode.NONE,
    val presetId: Int = 0,
    val imageUri: String? = null,
    val backgroundBlur: Float = 0.30f,
    val backgroundDim: Float = 0.42f,
    val glassOpacity: Float = 0.65f,
    /** Efecto "liquid glass" en superficies (opcional, como en Echo). */
    val liquidGlass: Boolean = false,
)

data class PlaybackSettings(
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: String = "Plano",
    val equalizerBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val skipSilence: Boolean = false,
    val sleepAtEpochMs: Long = 0L,
    val audioQuality: String = "alta",
    val volumeBoostDb: Float = 0f,
    val downloadWifiOnly: Boolean = false,
    val showDiscoverSections: Boolean = true,
    /** Fundido de entrada al empezar cada pista. */
    val fadeIn: Boolean = true,

    /** Pausa automática cuando el móvil se silencia (modo vibrar/silencio). */
    val pauseOnMute: Boolean = true,

    /** Reanuda la reproducción al reconectar auriculares o Bluetooth. */
    val resumeOnHeadset: Boolean = true,

    /** Oculta la carátula en la pantalla del reproductor (estilo Echo). */
    val hideThumbnail: Boolean = false,

    /** Fondo del reproductor con movimiento lento (Canvas/Ambient de Echo). */
    val ambientMotion: Boolean = true,

    /** Segundos de fundido de salida antes de cambiar de pista (0 = off). */
    val crossfadeSeconds: Int = 0,

    /** Estilo visual de la pantalla de letras: clasico | grande | serif | mono. */
    val lyricsStyle: String = "clasico",

    /** Filtra resultados que son vídeos (video oficial, videoclip…). */
    val hideVideos: Boolean = false,

    /** Filtra Shorts (piezas de 70 s o menos) de búsquedas y feeds. */
    val hideShorts: Boolean = false,
    /** Reproducción instantánea: salta fundidos y precarga más canciones por adelantado. */
    val instantPlay: Boolean = false,
)

data class TasteSettings(
    val genres: Set<String> = emptySet(),
    val artists: List<String> = emptyList(),
    val podcast: Boolean = false,
    val podcastTopics: Set<String> = emptySet(),
    val completed: Boolean = false,
)

/**
 * Preferencias de personalización (fondo y cristal) y de reproducción/audio.
 * Se guardan en SharedPreferences y se exponen como [StateFlow].
 */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("popmusic_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<BackgroundSettings> = _settings.asStateFlow()

    private val _playback = MutableStateFlow(loadPlayback())
    val playback: StateFlow<PlaybackSettings> = _playback.asStateFlow()

    private val _taste = MutableStateFlow(loadTaste())
    val taste: StateFlow<TasteSettings> = _taste.asStateFlow()

    fun update(transform: (BackgroundSettings) -> BackgroundSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putString(KEY_MODE, next.mode.name)
            .putInt(KEY_PRESET, next.presetId)
            .putString(KEY_IMAGE, next.imageUri)
            .putFloat(KEY_BLUR, next.backgroundBlur)
            .putFloat(KEY_DIM, next.backgroundDim)
            .putFloat(KEY_GLASS, next.glassOpacity)
            .putBoolean(KEY_LIQUID_GLASS, next.liquidGlass)
            .apply()
    }

    /**
     * Guarda cuándo se llevó la app a segundo plano (item: tras >30 min sin
     * uso y sin música sonando, al volver se abre como la primera vez).
     */
    fun markBackgrounded(atMillis: Long) {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_AT, atMillis).apply()
    }

    /** Instante (ms) del último `onStop`; 0 si nunca ha ido a segundo plano. */
    fun lastBackgroundedAt(): Long = prefs.getLong(KEY_LAST_BACKGROUND_AT, 0L)

    fun updatePlayback(transform: (PlaybackSettings) -> PlaybackSettings) {
        val next = transform(_playback.value)
        _playback.value = next
        prefs.edit()
            .putBoolean(KEY_EQ_ENABLED, next.equalizerEnabled)
            .putString(KEY_EQ_PRESET, next.equalizerPreset)
            .putString(
                KEY_EQ_BANDS,
                next.equalizerBands.joinToString(",") { String.format(java.util.Locale.US, "%.1f", it) },
            )
            .putBoolean(KEY_SKIP_SILENCE, next.skipSilence)
            .putLong(KEY_SLEEP_AT, next.sleepAtEpochMs)
            .putString(KEY_AUDIO_QUALITY, next.audioQuality)
            .putFloat(KEY_VOLUME_BOOST, next.volumeBoostDb)
            .putBoolean(KEY_DOWNLOAD_WIFI, next.downloadWifiOnly)
            .putBoolean(KEY_SHOW_DISCOVER, next.showDiscoverSections)
            .putBoolean(KEY_FADE_IN, next.fadeIn)
            .putBoolean(KEY_INSTANT_PLAY, next.instantPlay)
            .putBoolean(KEY_PAUSE_ON_MUTE, next.pauseOnMute)
            .putBoolean(KEY_RESUME_ON_HEADSET, next.resumeOnHeadset)
            .putBoolean(KEY_HIDE_THUMBNAIL, next.hideThumbnail)
            .putBoolean(KEY_AMBIENT_MOTION, next.ambientMotion)
            .putBoolean(KEY_HIDE_VIDEOS, next.hideVideos)
            .putBoolean(KEY_HIDE_SHORTS, next.hideShorts)
            .putInt(KEY_CROSSFADE, next.crossfadeSeconds.coerceIn(0, 10))
            .putString(KEY_LYRICS_STYLE, next.lyricsStyle)
            .apply()
    }

    /** Vuelve a los valores de fábrica de apariencia y audio (conserva cuenta y biblioteca). */
    fun resetAppearanceAndAudio() {
        _settings.value = BackgroundSettings()
        _playback.value = PlaybackSettings()
        prefs.edit()
            .putString(KEY_MODE, BackgroundSettings().mode.name)
            .putInt(KEY_PRESET, BackgroundSettings().presetId)
            .remove(KEY_IMAGE)
            .putFloat(KEY_BLUR, BackgroundSettings().backgroundBlur)
            .putFloat(KEY_DIM, BackgroundSettings().backgroundDim)
            .putFloat(KEY_GLASS, BackgroundSettings().glassOpacity)
            .putBoolean(KEY_LIQUID_GLASS, BackgroundSettings().liquidGlass)
            .putBoolean(KEY_EQ_ENABLED, false)
            .putString(KEY_EQ_PRESET, PlaybackSettings().equalizerPreset)
            .putString(KEY_EQ_BANDS, "0.0,0.0,0.0,0.0,0.0")
            .putBoolean(KEY_SKIP_SILENCE, false)
            .putLong(KEY_SLEEP_AT, 0L)
            .putString(KEY_AUDIO_QUALITY, PlaybackSettings().audioQuality)
            .putFloat(KEY_VOLUME_BOOST, 0f)
            .putBoolean(KEY_DOWNLOAD_WIFI, false)
            .putBoolean(KEY_SHOW_DISCOVER, true)
            .putBoolean(KEY_FADE_IN, true)
            .putBoolean(KEY_INSTANT_PLAY, false)
            .apply()
    }

    private fun load(): BackgroundSettings {
        // Migración única: las instalaciones que ya arrancaron con el fondo
        // azul y el liquid glass activo pasan una sola vez al fondo negro y
        // al cristal normal. Después el usuario puede elegir lo que quiera.
        if (!prefs.contains(KEY_DEFAULTS_MIGRATED)) {
            prefs.edit()
                .putString(KEY_MODE, BackgroundMode.NONE.name)
                .putBoolean(KEY_LIQUID_GLASS, false)
                .putBoolean(KEY_DEFAULTS_MIGRATED, true)
                .apply()
        }
        val defaults = BackgroundSettings()
        val mode = runCatching {
            BackgroundMode.valueOf(prefs.getString(KEY_MODE, null) ?: defaults.mode.name)
        }.getOrDefault(defaults.mode)

        return defaults.copy(
            mode = mode,
            presetId = prefs.getInt(KEY_PRESET, defaults.presetId),
            imageUri = prefs.getString(KEY_IMAGE, defaults.imageUri),
            backgroundBlur = prefs.getFloat(KEY_BLUR, defaults.backgroundBlur),
            backgroundDim = prefs.getFloat(KEY_DIM, defaults.backgroundDim),
            glassOpacity = prefs.getFloat(KEY_GLASS, defaults.glassOpacity),
            liquidGlass = prefs.getBoolean(KEY_LIQUID_GLASS, defaults.liquidGlass),
        )
    }

    fun updateTaste(transform: (TasteSettings) -> TasteSettings) {
        val next = transform(_taste.value)
        _taste.value = next
        prefs.edit()
            .putString(KEY_TASTE_GENRES, next.genres.joinToString("|"))
            .putString(KEY_TASTE_ARTISTS, next.artists.joinToString("|"))
            .putBoolean(KEY_TASTE_PODCAST, next.podcast)
            .putString(KEY_TASTE_PODCAST_TOPICS, next.podcastTopics.joinToString("|"))
            .putBoolean(KEY_TASTE_DONE, next.completed)
            .apply()
    }

    private fun loadTaste(): TasteSettings {
        return TasteSettings(
            genres = prefs.getString(KEY_TASTE_GENRES, null)
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet(),
            artists = prefs.getString(KEY_TASTE_ARTISTS, null)
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty(),
            podcast = prefs.getBoolean(KEY_TASTE_PODCAST, false),
            podcastTopics = prefs.getString(KEY_TASTE_PODCAST_TOPICS, null)
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet(),
            completed = prefs.getBoolean(KEY_TASTE_DONE, false),
        )
    }

    private fun loadPlayback(): PlaybackSettings {
        return PlaybackSettings(
            equalizerEnabled = prefs.getBoolean(KEY_EQ_ENABLED, false),
            equalizerPreset = prefs.getString(KEY_EQ_PRESET, null) ?: "Plano",
            equalizerBands = prefs.getString(KEY_EQ_BANDS, null)
                ?.split(",")
                ?.mapNotNull { it.trim().toFloatOrNull() }
                ?.takeIf { it.size == 5 }
                ?: listOf(0f, 0f, 0f, 0f, 0f),
            skipSilence = prefs.getBoolean(KEY_SKIP_SILENCE, false),
            sleepAtEpochMs = prefs.getLong(KEY_SLEEP_AT, 0L),
            audioQuality = prefs.getString(KEY_AUDIO_QUALITY, null) ?: "alta",
            volumeBoostDb = prefs.getFloat(KEY_VOLUME_BOOST, 0f),
            downloadWifiOnly = prefs.getBoolean(KEY_DOWNLOAD_WIFI, false),
            showDiscoverSections = prefs.getBoolean(KEY_SHOW_DISCOVER, true),
            fadeIn = prefs.getBoolean(KEY_FADE_IN, true),
            instantPlay = prefs.getBoolean(KEY_INSTANT_PLAY, false),
            pauseOnMute = prefs.getBoolean(KEY_PAUSE_ON_MUTE, true),
            resumeOnHeadset = prefs.getBoolean(KEY_RESUME_ON_HEADSET, true),
            hideThumbnail = prefs.getBoolean(KEY_HIDE_THUMBNAIL, false),
            ambientMotion = prefs.getBoolean(KEY_AMBIENT_MOTION, true),
            hideVideos = prefs.getBoolean(KEY_HIDE_VIDEOS, false),
            hideShorts = prefs.getBoolean(KEY_HIDE_SHORTS, false),
            crossfadeSeconds = prefs.getInt(KEY_CROSSFADE, 0).coerceIn(0, 10),
            lyricsStyle = prefs.getString(KEY_LYRICS_STYLE, null) ?: "clasico",
        )
    }

    private companion object {
        const val KEY_MODE = "bg_mode"
        const val KEY_PRESET = "bg_preset"
        const val KEY_IMAGE = "bg_image"
        const val KEY_BLUR = "bg_blur"
        const val KEY_DIM = "bg_dim"
        const val KEY_GLASS = "glass_opacity"
        const val KEY_LIQUID_GLASS = "liquid_glass"
        const val KEY_DEFAULTS_MIGRATED = "defaults_black_v1"
        const val KEY_LAST_BACKGROUND_AT = "last_background_at"
        const val KEY_EQ_ENABLED = "eq_enabled"
        const val KEY_EQ_PRESET = "eq_preset"
        const val KEY_EQ_BANDS = "eq_bands"
        const val KEY_SKIP_SILENCE = "skip_silence"
        const val KEY_SLEEP_AT = "sleep_at"
        const val KEY_AUDIO_QUALITY = "audio_quality"
        const val KEY_VOLUME_BOOST = "volume_boost"
        const val KEY_DOWNLOAD_WIFI = "download_wifi_only"
        const val KEY_SHOW_DISCOVER = "show_discover_sections"
        const val KEY_FADE_IN = "fade_in"
        const val KEY_INSTANT_PLAY = "instant_play"
        const val KEY_PAUSE_ON_MUTE = "pause_on_mute"
        const val KEY_RESUME_ON_HEADSET = "resume_on_headset"
        const val KEY_HIDE_THUMBNAIL = "hide_thumbnail"
        const val KEY_AMBIENT_MOTION = "ambient_motion"
        const val KEY_HIDE_VIDEOS = "hide_videos"
        const val KEY_HIDE_SHORTS = "hide_shorts"
        const val KEY_CROSSFADE = "crossfade_seconds"
        const val KEY_LYRICS_STYLE = "lyrics_style"
        const val KEY_TASTE_GENRES = "taste_genres"
        const val KEY_TASTE_ARTISTS = "taste_artists"
        const val KEY_TASTE_PODCAST = "taste_podcast"
        const val KEY_TASTE_PODCAST_TOPICS = "taste_podcast_topics"
        const val KEY_TASTE_DONE = "taste_done"
    }
}
