package com.popmusic.app.player

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.popmusic.app.MainActivity
import com.popmusic.app.AppCloseReceiver
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import android.net.Uri
import com.popmusic.app.data.downloads.DownloadRepository
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.data.youtube.NewPipeDownloader
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.PoPMusicApplication
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Pausa hecha por las funciones inteligentes (silencio o auriculares retirados). */
    private var smartPaused = false
    private lateinit var audioManager: AudioManager

    /**
     * "Pausa al silenciar" (ringer mode) y marca de pausa cuando se
     * desconectan los auriculares, para poder reanudarlos al reconectar.
     */
    private val smartReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    if (!sharedPrefs.getBoolean("pause_on_mute", true)) return
                    val player = mediaSession?.player ?: return
                    if (
                        audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL &&
                        player.isPlaying
                    ) {
                        smartPaused = true
                        player.pause()
                        Log.d(TAG, "Pausa automatica: dispositivo silenciado")
                    }
                }

                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    smartPaused = true
                }
            }
        }
    }

    /** "Reanudar al conectar": retoma cuando aparece un nuevo destino de audio. */
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (!sharedPrefs.getBoolean("resume_on_headset", true)) return
            if (!smartPaused) return
            val player = mediaSession?.player ?: return
            val isOutput = addedDevices.any { it.isSink }
            if (!isOutput || player.mediaItemCount == 0 || player.isPlaying) return
            smartPaused = false
            player.play()
            Log.d(TAG, "Reproduccion reanudada al reconectar auriculares")
        }
    }

    private val repository = YouTubeRepository()
    private val audioUrlCache by lazy {
        (application as PoPMusicApplication).audioUrlCache
    }
    private val downloadRepository by lazy {
        DownloadRepository(this, LibraryRepository(this), com.popmusic.app.data.prefs.AppPreferences(this))
    }
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private val resolvedUrls = ConcurrentHashMap<String, String>()
    private val resolving = ConcurrentHashMap.newKeySet<String>()
    private val prefetchPermits = kotlinx.coroutines.sync.Semaphore(6)
    private val errorRetries = ConcurrentHashMap<String, Int>()
    private var lastTimelineCount = 0
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var fadeAnimator: android.animation.ValueAnimator? = null
    private val sharedPrefs: SharedPreferences by lazy {
        getSharedPreferences("popmusic_prefs", Context.MODE_PRIVATE)
    }
    private val instantPlay: Boolean by lazy {
        sharedPrefs.getBoolean("instant_play", false)
    }
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "eq_enabled", "eq_bands" -> applyEqualizer()
            "skip_silence" -> applySkipSilence()
            "sleep_at" -> scheduleSleep()
            "volume_boost" -> applyBoost()
            "fade_in" -> {
                if (!sharedPrefs.getBoolean("fade_in", true)) {
                    fadeAnimator?.cancel()
                    fadeAnimator = null
                    runCatching { mediaSession?.player?.volume = 1f }
                }
            }
        }
    }
    private val sleepRunnable = Runnable {
        mediaSession?.player?.pause()
        sharedPrefs.edit().putLong("sleep_at", 0L).apply()
    }
    private val snapshotRunnable = Runnable { saveSnapshot() }
    private val positionSyncRunnable = object : Runnable {
        override fun run() {
            // Cierre a los 8 min: mientras suena música, la marca de
            // actividad se refresca aquí (si el estado quedara desfasado,
            // la alarma no podría cerrar con una marca vieja).
            markActiveIfStale()
            scheduleSnapshotSave(0L)
            main.postDelayed(this, 5_000L)
        }
    }

    /** Hora (elapsedRealtime) del último cambio de pista, para la ventana de fade-in. */
    private var lastTransitionAt = 0L

    /**
     * Crossfade: atenúa el volumen durante los últimos segundos de la pista
     * (proporcional al tiempo restante). El fade-in de la transición sube el
     * volumen al empezar la siguiente. Se ignora mientras corre el fade-in
     * para no pisarlo, y se restaura el volumen si el usuario hace seek.
     */
    private val crossfadeRunnable = object : Runnable {
        override fun run() {
            val player = mediaSession?.player
            if (player != null && player.isPlaying && !instantPlay) {
                val windowMs = sharedPrefs.getInt("crossfade_seconds", 0).coerceIn(0, 10) * 1000L
                val now = android.os.SystemClock.elapsedRealtime()
                val fadeInWindow = sharedPrefs.getBoolean("fade_in", true) &&
                    now - lastTransitionAt < FADE_IN_MS + 80L
                val duration = player.duration
                val remaining =
                    if (duration > 0L) duration - player.currentPosition else Long.MAX_VALUE
                when {
                    fadeInWindow -> Unit // manda el fade-in de la transición

                    windowMs > 0L && remaining in 1..windowMs -> {
                        player.volume = (remaining.toFloat() / windowMs).coerceIn(0f, 1f)
                    }

                    player.volume < 0.999f -> player.volume = 1f
                }
            }
            main.postDelayed(this, CROSSFADE_TICK_MS)
        }
    }

    /**
     * La precarga masiva (20 pistas) usa NewPipe y compite por la red con la
     * canción que acaba de entrar: por eso el audio tardaba ~0,8 s en arrancar.
     * Se retrasa unos segundos, cuando la pista ya está sonando.
     */
    private val deferredPrefetch = Runnable {
        mediaSession?.player?.let { prefetchUpcoming(it, prefetchAhead) }
    }

    override fun onCreate() {
        super.onCreate()
        // Tras un cierre a los 8 min (AppCloseReceiver puso la bandera para
        // que onDestroy no re-guarde la cola), un arranque nuevo vuelve a
        // permitir guardar snapshots.
        closeForGood = false

        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(NewPipeDownloader.USER_AGENT)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            // Cadena de audio estilo Echo: recorta el silencio también al
            // terminar la pista (el procesador por defecto de Media3 deja
            // hasta 2 s de cola muda antes de pasar a la siguiente).
            .setRenderersFactory(EchoRenderersFactory(this))
            .build()

        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
        // Media3 precarga la pista siguiente con su propio pipeline: al saltar,
        // el audio ya está en buffering y entra sin el hueco de ~0,8 s.
        runCatching {
            player.setPreloadConfiguration(ExoPlayer.PreloadConfiguration(10_000_000L))
        }.onFailure {
            Log.w(TAG, "Precarga nativa no disponible", it)
        }

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Error de reproduccion (${error.errorCodeName})", error)
                handlePlaybackError(player)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    Log.d("SkipPerf", "ready ${player.currentMediaItem?.mediaId} @${android.os.SystemClock.elapsedRealtime()}")
                    // Si se saltó a una pista aún sin resolver, cargarla ya.
                    resolveCurrentIfNeeded(player)
                    main.removeCallbacks(deferredPrefetch)
                    main.postDelayed(deferredPrefetch, 3_000L)
                    // Con aleatorio activo la siguiente pista es impredecible: mantener
                    // resuelta toda la cola evita saltos y esperas.
                    if (player.shuffleModeEnabled) prefetchAllQueue()
                    // La siguiente pista en orden de reproducción manda: resolverla ya.
                    prefetchNextImmediate(player)
                }
                scheduleSnapshotSave()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Cierre a los 8 min: dar play/pausa (desde app o notificación)
                // es actividad del usuario; el cierre se mide desde aquí.
                markActive()
                Log.d(CLOSE_TAG, "actividad: playWhenReady=$playWhenReady (cierre en 8 min desde ahora)")
                // Cola restaurada sin preparar (URI sin resolver por falta de red
                // al arrancar): si el usuario dio play con el player en IDLE,
                // resolver y cargar la pista ahora, sin saltar canciones.
                if (playWhenReady) prepareWhenIdle(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                markActive()
                applyEqualizer()
                applyBoost()
                if (isPlaying) {
                    smartPaused = false
                    main.removeCallbacks(positionSyncRunnable)
                    main.postDelayed(positionSyncRunnable, 5_000L)
                } else {
                    main.removeCallbacks(positionSyncRunnable)
                    scheduleSnapshotSave()
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                val count = player.mediaItemCount
                val grew = count > lastTimelineCount
                lastTimelineCount = count
                if (!grew) return
                // La radio acaba de añadir canciones: resolver ya las primeras
                // de la cola evita que el primer "siguiente" espere a YouTube.
                if (count > 0 && player.playbackState != Player.STATE_IDLE) {
                    prefetchNextImmediate(player)
                    // Mientras carga la pista elegida, solo la siguiente: el
                    // resto se precarga al pasar a READY (deferredPrefetch).
                    if (player.playbackState == Player.STATE_READY) {
                        prefetchUpcoming(player, prefetchAhead)
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d("SkipPerf", "transition ${mediaItem?.mediaId} @${android.os.SystemClock.elapsedRealtime()}")
                markActive()
                mediaItem?.mediaId?.let { errorRetries.remove(it) }
                lastTransitionAt = android.os.SystemClock.elapsedRealtime()
                applyFadeIn(player)
                scheduleSnapshotSave()
                // La pista que ACABA de entrar manda: si el usuario saltó a una
                // canción todavía sin resolver, se resuelve aquí mismo para que
                // empiece a sonar sin esperar a la red dentro del gesto.
                resolveCurrentIfNeeded(player)
                // Después, la SIGUIENTE (la que sonará en unos minutos), para
                // que el próximo salto natural o manual sea también instantáneo.
                prefetchNextImmediate(player)
            }

            override fun onShuffleModeEnabledChanged(shuffleEnabled: Boolean) {
                if (shuffleEnabled) {
                    // El aleatorio reordena la cola: precargar TODO y resolver
                    // ya la nueva "siguiente" (es la que sonará en segundos).
                    prefetchAllQueue()
                    prefetchNextImmediate(player)
                }
                // Item 5: el botón de aleatorio de la notificación refleja el
                // estado (icono on/off) y se reconstruye la notificación.
                updateMediaButtons()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                // Item 2 (tanda 3): el botón de REPETIR de la notificación
                // refleja el modo actual (off/todo/una) y se reconstruye.
                updateMediaButtons()
            }
        })

        // Nota: esto corre ANTES de crear mediaSession, así que hay que
        // aplicarlo sobre el player directamente (antes nunca se activaba
        // al arrancar la app; solo al cambiar la preferencia).
        runCatching { player.skipSilenceEnabled = sharedPrefs.getBoolean("skip_silence", false) }
            .onFailure { Log.w(TAG, "No se pudo activar omision de silencios", it) }
        scheduleSleep()
        restorePlaybackSnapshot(player)
        main.postDelayed(crossfadeRunnable, CROSSFADE_TICK_MS)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(QueueCallback())
            // Item 2 (tanda 3): tocar la notificación abre la app en el reproductor
            // (MainActivity es singleTask → onNewIntent) y la notificación
            // incluye el botón de ALEATORIO junto a los controles. El comando
            // custom de REPETIR se declara en QueueCallback.onConnect.
            .setSessionActivity(playerSessionActivity())
            .setMediaButtonPreferences(
                listOf(
                    shuffleMediaButton(player.shuffleModeEnabled),
                    repeatMediaButton(player.repeatMode),
                ),
            )
            .build()

        applyBoost()

        registerSmartPlayback()
    }

    /**
     * PendingIntent de la sesión de media (item 5): es el contentIntent de la
     * notificación; al tocarla, MainActivity recibe el extra y abre el
     * reproductor vía [PlayerOpenBus].
     */
    private fun playerSessionActivity(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(PlayerOpenBus.EXTRA_OPEN_PLAYER, true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_OPEN_PLAYER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Botón de ALEATORIO de la notificación (item 5): `COMMAND_SET_SHUFFLE_MODE`
     * alterna el modo aleatorio al pulsarlo (y actualiza su icono on/off).
     */
    private fun shuffleMediaButton(shuffleOn: Boolean): CommandButton =
        CommandButton.Builder(
            if (shuffleOn) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF,
        )
            .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
            .setDisplayName("Aleatorio")
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()

    /**
     * Botón de REPETIR de la notificación (item 2, tanda 3): acción custom
     * [COMMAND_CYCLE_REPEAT] que alterna off → todo → una → off.
     */
    private fun repeatMediaButton(repeatMode: Int): CommandButton =
        CommandButton.Builder(
            when (repeatMode) {
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                else -> CommandButton.ICON_REPEAT_OFF
            },
        )
            .setSessionCommand(SessionCommand(COMMAND_CYCLE_REPEAT, Bundle()))
            .setDisplayName("Repetir")
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()

    /** Reconstruye los botones de acción de la notificación con el estado real. */
    private fun updateMediaButtons() {
        val player = mediaSession?.player ?: return
        runCatching {
            mediaSession?.setMediaButtonPreferences(
                listOf(
                    shuffleMediaButton(player.shuffleModeEnabled),
                    repeatMediaButton(player.repeatMode),
                ),
            )
        }.onFailure { Log.w(TAG, "No se pudieron actualizar los botones de notificacion", it) }
    }

    /** Escucha silencio y conexiones de auriculares (features "smart playback"). */
    private fun registerSmartPlayback() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                registerReceiver(smartReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(smartReceiver, filter)
            }
        }.onFailure {
            Log.w(TAG, "No se pudo registrar el receiver de audio inteligente", it)
        }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, main)
    }

    private fun applyEqualizer() {
        val player = mediaSession?.player ?: run {
            equalizer?.enabled = false
            return
        }
        val enabled = sharedPrefs.getBoolean("eq_enabled", false)
        val bands = sharedPrefs.getString("eq_bands", null)
            ?.split(",")
            ?.mapNotNull { it.trim().toFloatOrNull() }
            ?.takeIf { it.size == 5 }
            ?: listOf(0f, 0f, 0f, 0f, 0f)

        if (!enabled) {
            runCatching { equalizer?.setEnabled(false) }
                .onFailure { Log.w(TAG, "No se pudo desactivar el equalizador", it) }
            return
        }

        val eq = equalizer ?: runCatching {
            val sessionId = (player as? ExoPlayer)?.audioSessionId ?: return
            if (sessionId == 0) return
            Equalizer(0, sessionId).also { equalizer = it }
        }.getOrNull()

        if (eq == null) {
            Log.w(TAG, "Equalizador no disponible en esta sesion de audio")
            return
        }

        runCatching {
            val deviceBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange ?: shortArrayOf(-1500, 1500)
            val maxDb = range[1] / 100f
            val covered = bands.map { it.coerceIn(-maxDb, maxDb) }
            for (band in 0 until deviceBands) {
                val index =
                    if (deviceBands == 1) 4 else (band * 4 / (deviceBands - 1)).coerceIn(0, 4)
                eq.setBandLevel(band.toShort(), (covered[index] * 100).toInt().toShort())
            }
            eq.setEnabled(true)
        }.onFailure { Log.e(TAG, "Error aplicando niveles del equalizador", it) }
    }

    private fun applySkipSilence() {
        val player = mediaSession?.player ?: return
        (player as? ExoPlayer)?.skipSilenceEnabled =
            sharedPrefs.getBoolean("skip_silence", false)
    }

    /** Fundido de entrada suave al cambiar de pista (preferencia "Transición suave"). */
    private fun applyFadeIn(player: Player) {
        val crossfade = sharedPrefs.getInt("crossfade_seconds", 0)
        // Con crossfade activo sí hace falta subir el volumen aunque el
        // fade-in esté desactivado: la salida del anterior dejó volumen 0.
        if (instantPlay || (!sharedPrefs.getBoolean("fade_in", true) && crossfade <= 0)) {
            runCatching { player.volume = 1f }
            return
        }
        fadeAnimator?.cancel()
        runCatching { player.volume = 0f }
        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FADE_IN_MS
            addUpdateListener { animation ->
                runCatching { player.volume = animation.animatedValue as Float }
            }
        }
        fadeAnimator = animator
        runCatching { animator.start() }
    }

    private fun applyBoost() {
        val player = mediaSession?.player ?: run {
            runCatching<Unit> { loudnessEnhancer?.setEnabled(false) }
            return
        }
        val boostDb = sharedPrefs.getFloat("volume_boost", 0f)

        if (boostDb <= 0f) {
            runCatching<Unit> { loudnessEnhancer?.setEnabled(false) }
                .onFailure { Log.w(TAG, "No se pudo desactivar boost de volumen", it) }
            return
        }

        val sessionId = (player as? ExoPlayer)?.audioSessionId ?: return
        if (sessionId == 0) return

        val enh = loudnessEnhancer ?: runCatching<LoudnessEnhancer?> {
            LoudnessEnhancer(sessionId).also { loudnessEnhancer = it }
        }.getOrNull()

        if (enh == null) {
            Log.w(TAG, "LoudnessEnhancer no disponible en esta sesion")
            return
        }

        runCatching<Unit> {
            enh.setTargetGain((boostDb * 100).toInt())
            enh.setEnabled(true)
        }.onFailure { Log.e(TAG, "Error aplicando boost de volumen", it) }
    }

    private fun scheduleSleep() {
        val at = sharedPrefs.getLong("sleep_at", 0L)
        main.removeCallbacks(sleepRunnable)
        if (at <= System.currentTimeMillis()) return
        main.postDelayed(sleepRunnable, at - System.currentTimeMillis())
    }

    private fun scheduleSnapshotSave(delayMs: Long = 300L) {
        main.removeCallbacks(snapshotRunnable)
        if (delayMs > 0L) {
            main.postDelayed(snapshotRunnable, delayMs)
        } else {
            main.post(snapshotRunnable)
        }
    }

    /**
     * Guarda cuándo hubo última actividad de reproducción (play/pausa/cambio
     * de pista). El cierre a los 8 min ([com.popmusic.app.AppCloseReceiver])
     * se mide desde aquí: una pausa momentánea (asistente de voz, TikTok,
     * audio de WhatsApp…) NO cierra la app, solo empieza la cuenta de 8 min.
     */
    private fun markActive() {
        lastMarkAt = System.currentTimeMillis()
        runCatching {
            sharedPrefs.edit()
                .putLong(PlaybackService.KEY_LAST_ACTIVE, lastMarkAt)
                .apply()
        }
    }

    /** Como [markActive] pero solo si la marca tiene ≥ 30 s (tick de 5 s). */
    private fun markActiveIfStale() {
        if (System.currentTimeMillis() - lastMarkAt >= 30_000L) markActive()
    }

    @Volatile private var lastMarkAt = 0L

    private fun saveSnapshot() {
        // Cierre total en curso: la cola ya se borró, no re-guardarla.
        if (closeForGood) return
        val player = mediaSession?.player ?: return
        val editor = sharedPrefs.edit()
        if (player.mediaItemCount == 0) {
            editor.remove(SNAPSHOT_KEY).apply()
            return
        }

        val queue = JSONArray()
        val limit = minOf(player.mediaItemCount, SNAPSHOT_MAX_ITEMS)
        for (i in 0 until limit) {
            val item = player.getMediaItemAt(i)
            val meta = item.mediaMetadata
            queue.put(
                JSONObject().apply {
                    put("id", item.mediaId)
                    put("title", meta.title?.toString().orEmpty())
                    put("artist", meta.artist?.toString().orEmpty())
                    put("thumb", meta.artworkUri?.toString().orEmpty())
                    // URI directa (mp3 de podcast) o vacío si es YouTube sin resolver.
                    val uri = item.localConfiguration?.uri?.toString().orEmpty()
                    put("uri", if (uri.contains("youtube.com/watch")) "" else uri)
                },
            )
        }

        val snapshot = JSONObject().apply {
            put("savedAt", System.currentTimeMillis())
            put("queue", queue)
            put("index", player.currentMediaItemIndex)
            put("positionMs", player.currentPosition.coerceAtLeast(0L))
            put("shuffle", player.shuffleModeEnabled)
            put("repeatMode", player.repeatMode)
        }
        editor.putString(SNAPSHOT_KEY, snapshot.toString()).apply()
    }

    private fun restorePlaybackSnapshot(player: ExoPlayer) {
        if (player.mediaItemCount != 0) return
        val raw = sharedPrefs.getString(SNAPSHOT_KEY, null) ?: return
        val snapshot = runCatching { JSONObject(raw) }.getOrNull() ?: return
        val savedAt = snapshot.optLong("savedAt", 0L)
        val fresh = savedAt > 0L && System.currentTimeMillis() - savedAt <= SNAPSHOT_TTL_MS
        if (!fresh) {
            sharedPrefs.edit().remove(SNAPSHOT_KEY).apply()
            return
        }

        val queueJson = snapshot.optJSONArray("queue") ?: return
        val items = buildList {
            for (i in 0 until queueJson.length()) {
                val obj = queueJson.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                if (id.isBlank()) continue
                val savedUri = obj.optString("uri")
                add(
                    MediaItem.Builder()
                        .setMediaId(id)
                        .setUri(
                            savedUri.ifBlank { "https://www.youtube.com/watch?v=$id" },
                        )
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(obj.optString("title"))
                                .setArtist(obj.optString("artist"))
                                .setArtworkUri(obj.optString("thumb").takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) })
                                .setIsPlayable(true)
                                .build(),
                        )
                        .build(),
                )
            }
        }
        if (items.isEmpty()) return

        val index = snapshot.optInt("index", 0).coerceIn(0, items.lastIndex)
        val positionMs = snapshot.optLong("positionMs", 0L).coerceAtLeast(0L)
        val shuffle = snapshot.optBoolean("shuffle", false)
        val repeatMode = snapshot.optInt("repeatMode", Player.REPEAT_MODE_OFF)

        player.setMediaItems(items, index, positionMs)
        player.shuffleModeEnabled = shuffle
        player.repeatMode = repeatMode
        player.playWhenReady = false
        Log.d(TAG, "Sesion restaurada: ${items.size} pistas, index $index")
        prefetchUpcoming(player, PREFETCH_AHEAD)
        // NO prepare() con URIs sin resolver: cargarlas en pausa provoca una
        // cadena de errores que hace saltar canciones solas al abrir la app.
        // Se resuelve primero la pista actual (caché/archivo local es
        // instantáneo) y solo entonces se prepara; sin red queda en IDLE y se
        // cargará cuando el usuario pida reproducir (onPlayWhenReadyChanged).
        val restoredId = items[index].mediaId
        scope.launch {
            val resolved = resolveItem(items[index])
            main.post {
                runCatching {
                    if (
                        player.mediaItemCount > index &&
                        player.getMediaItemAt(index).mediaId == restoredId &&
                        player.playbackState == Player.STATE_IDLE
                    ) {
                        if (resolved != null && isPlayableNow(resolved)) {
                            player.replaceMediaItem(index, resolved)
                            player.prepare()
                            Log.d(TAG, "Sesion restaurada preparada (pista $index)")
                        } else {
                            Log.w(TAG, "Sesion restaurada sin cargar (sin red/URL): se cargara al reproducir")
                        }
                    }
                }
            }
        }
    }

    /**
     * ¿Hay red saliendo? Sin ella ninguna URL remota es utilizable: los
     * reintentos solo generarían una cadena de errores que salta canciones.
     */
    private fun hasNetwork(): Boolean {
        val cm = runCatching {
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }.getOrNull() ?: return true
        val net = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(net)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /** La pista puede cargarse ya: archivo local (sin red) o con red disponible. */
    private fun isPlayableNow(item: MediaItem): Boolean {
        val uri = item.localConfiguration?.uri?.toString().orEmpty()
        return uri.startsWith("file:") || hasNetwork()
    }

    /**
     * Carga la pista actual cuando el player queda en IDLE con playWhenReady
     * (cola restaurada que no pudo resolverse al arrancar por falta de red).
     * Con reintentos acotados; si no hay URL, deja la cola en pausa.
     */
    private fun prepareWhenIdle(player: Player, attempt: Int = 0) {
        if (!player.playWhenReady || player.playbackState != Player.STATE_IDLE) return
        val index = player.currentMediaItemIndex
        val item = player.currentMediaItem ?: return
        scope.launch {
            val resolved = resolveItem(item)
            main.post {
                runCatching {
                    if (!player.playWhenReady || player.playbackState != Player.STATE_IDLE) return@post
                    if (player.currentMediaItemIndex != index) return@post
                    if (player.currentMediaItem?.mediaId != item.mediaId) return@post
                    if (resolved != null && isPlayableNow(resolved)) {
                        player.replaceMediaItem(index, resolved)
                        player.prepare()
                        Log.d(TAG, "Pista $index cargada al reproducir")
                    } else if (attempt < 3) {
                        main.postDelayed({ prepareWhenIdle(player, attempt + 1) }, 2_000L)
                    } else {
                        Log.w(TAG, "Sin URL para la pista restaurada tras reintentos: en pausa")
                        player.pause()
                    }
                }
            }
        }
    }

    private inner class QueueCallback : MediaSession.Callback {

        override fun onConnect(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            // Item 2 (tanda 3): sin declarar el comando custom de REPETIR como
            // disponible, media3 no expone su botón en la notificación (el
            // resultado de onConnect alimenta los comandos de la sesión legacy).
            // Ojo con super.onConnect(): devuelve SessionCommands.EMPTY + extras
            // NOT_IMPLEMENTED; si se copian, media3 lo trata como implementación
            // por defecto y NO registra los comandos en la sesión legacy. Se parte
            // de AcceptedResultBuilder(session, controller), que aplica los
            // comandos por defecto según la confianza del controller.
            val base = MediaSession.ConnectionResult.AcceptedResultBuilder(mediaSession, controller).build()
            val sessionCommands = SessionCommands.Builder()
                .addSessionCommands(base.availableSessionCommands.commands)
                .add(SessionCommand(COMMAND_CYCLE_REPEAT, Bundle()))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(mediaSession, controller)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            if (mediaItems.isEmpty()) {
                future.set(mediaItems)
                return future
            }

            val requestedIndex = mediaItems.first().mediaMetadata.extras
                ?.getInt(PlayerConnection.EXTRA_START_INDEX, 0)
                ?: 0
            if (requestedIndex == PlayerConnection.APPEND_INDEX) {
                // Añadir a la cola responde YA. Si se espera a resolver la URL,
                // Media3 congela el estado del MediaController: la canción
                // sonaba y la pantalla seguía cargando. La siguiente la
                // resuelve onTimelineChanged; el resto se calienta después.
                future.set(mediaItems)
                scope.launch {
                    kotlinx.coroutines.delay(APPEND_WARM_DELAY_MS)
                    mediaItems.take(MAX_QUEUE_WARM).forEach { item ->
                        scope.launch { prefetchPermits.withPermit { resolveItem(item) } }
                    }
                }
                return future
            }
            val startIndex = requestedIndex.coerceIn(0, mediaItems.lastIndex)

        scope.launch {
            val items = mediaItems.toMutableList()

            // 1. Resolver solo el item de inicio con máxima prioridad para empezar INMEDIATAMENTE.
            // En modo instant-play no esperamos a las siguientes; se precargan en background.
            val firstResolved = resolveItem(items[startIndex]) ?: items[startIndex]
            items[startIndex] = firstResolved

            // Retornar INMEDIATAMENTE cuando el item de inicio está listo (no bloqueamos en las demás).
            future.set(items)

            // 2. Calentar TODA la cola en segundo plano (no bloquea el retorno):
            // las primeras van primero y el resto se reparte con un límite de
            // concurrencia. Así, saltar a una canción lejana encuentra la URL
            // ya resuelta en caché y arranca sin esperar a YouTube.
            val last = min(startIndex + MAX_QUEUE_WARM, items.lastIndex)
            if (startIndex + 1 <= last) {
                val nearEnd = min(startIndex + prefetchAhead, last)
                val order = buildList {
                    addAll(startIndex + 1..nearEnd)
                    if (nearEnd < last) addAll((nearEnd + 1..last).shuffled())
                }
                order.forEach { index ->
                    scope.launch {
                        prefetchPermits.withPermit { resolveItem(items[index]) }
                    }
                }
            }
        }
        return future
        }

        override fun onCustomCommand(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            // Item 2 (tanda 3): botón REPETIR de la notificación → ciclo
            // desactivado → repetir todo → repetir una → desactivado.
            if (customCommand.customAction == COMMAND_CYCLE_REPEAT) {
                val player = mediaSession.player
                player.repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                updateMediaButtons()
                Log.d(TAG, "Repetir alterado desde la notificacion: modo ${player.repeatMode}")
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

private suspend fun resolveItem(item: MediaItem): MediaItem? {
        if (!item.isUnresolved()) return item

        resolvedUrls[item.mediaId]?.let { url ->
            return item.buildUpon().setUri(url).build()
        }

        // 1. Verificar archivo local descargado PRIMERO (instantáneo, sin red)
        val localFile = downloadRepository.localFileFor(item.mediaId)
        if (localFile != null) {
            val localUri = Uri.fromFile(localFile).toString()
            resolvedUrls[item.mediaId] = localUri
            return item.buildUpon().setUri(localUri).build()
        }

        // 2. URL ya resuelta en una sesión anterior (instantáneo, sin red)
        audioUrlCache.get(item.mediaId)?.let { cached ->
            resolvedUrls[item.mediaId] = cached
            return item.buildUpon().setUri(cached).build()
        }

        // Sin red no se intenta YouTube: evita reintentos lentos y cadenas de
        // errores (los archivos locales y la caché ya se resolvieron arriba).
        if (!hasNetwork()) {
            Log.w(TAG, "Sin URL de audio para ${item.mediaId}: sin red")
            return null
        }

        if (!resolving.add(item.mediaId)) return null

        val quality = sharedPrefs.getString("audio_quality", "alta") ?: "alta"
        val url = try {
            runCatching { repository.resolveAudioUrl(item.mediaId, quality) }
                .onFailure { Log.e(TAG, "No se pudo resolver el audio de ${item.mediaId}", it) }
                .getOrNull()
        } finally {
            resolving.remove(item.mediaId)
        }

        if (url.isNullOrBlank()) {
            Log.w(TAG, "Sin URL de audio para ${item.mediaId}, se omite")
            return null
        }

        resolvedUrls[item.mediaId] = url
        audioUrlCache.put(item.mediaId, url)
        return item.buildUpon().setUri(url).build()
    }

    /**
     * Resuelve la pista que acaba de entrar si su URI sigue siendo el watch de
     * YouTube. Antes el salto del usuario esperaba a esta resolución dentro del
     * propio gesto (2-4 s con la canción anterior sonando); ahora el salto es
     * inmediato y esta resolución corre en segundo plano al cambiar de pista.
     */
    private fun resolveCurrentIfNeeded(player: Player) {
        val index = player.currentMediaItemIndex
        if (index < 0) return
        val item = runCatching { player.getMediaItemAt(index) }.getOrNull() ?: return
        if (!item.isUnresolved() || resolving.contains(item.mediaId)) return
        scope.launch {
            val resolved = resolveItem(item) ?: return@launch
            main.post {
                runCatching {
                    if (index >= player.mediaItemCount) return@post
                    val current = player.getMediaItemAt(index)
                    if (current.mediaId != item.mediaId || !current.isUnresolved()) return@post
                    val position = player.currentPosition
                    player.replaceMediaItem(index, resolved)
                    if (position > 0L) player.seekTo(position)
                    Log.d(TAG, "Pista entrante resuelta al instante: ${item.mediaId}")
                }
            }
        }
    }

    /**
     * Resuelve con máxima prioridad la siguiente pista en orden de
     * reproducción (respeta el aleatorio: `nextMediaItemIndex` ya apunta a la
     * pista que sonará a continuación). Es lo que evita el silencio de varios
     * segundos al saltar de canción.
     */
    private fun prefetchNextImmediate(player: Player) {
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex < 0) return
        val item = runCatching { player.getMediaItemAt(nextIndex) }.getOrNull() ?: return
        if (!item.isUnresolved() || resolving.contains(item.mediaId)) return

        scope.launch {
            // SIN semáforo: la siguiente pista tiene prioridad ABSOLUTA sobre
            // cualquier otra precarga en vuelo (nunca espera a un permit).
            val resolvedItem = resolveItem(item) ?: return@launch
            main.post {
                runCatching {
                    if (
                        nextIndex < player.mediaItemCount &&
                        player.getMediaItemAt(nextIndex).mediaId == item.mediaId &&
                        player.getMediaItemAt(nextIndex).isUnresolved()
                    ) {
                        player.replaceMediaItem(nextIndex, resolvedItem)
                    }
                }
            }
        }
    }

    private fun prefetchUpcoming(player: Player, count: Int) {
        val total = player.mediaItemCount
        val current = player.currentMediaItemIndex
        val indices = if (player.shuffleModeEnabled) {
            (0 until total).filter { it != current }.shuffled().take(count)
        } else {
            (current + 1 until total).take(count)
        }
        indices.forEach { index ->
            val item = runCatching { player.getMediaItemAt(index) }.getOrNull() ?: return@forEach
            if (!item.isUnresolved() || resolving.contains(item.mediaId)) return@forEach

            scope.launch {
                prefetchPermits.withPermit {
                    val resolvedItem = resolveItem(item) ?: return@withPermit
                    main.post {
                        runCatching {
                            if (
                                index < player.mediaItemCount &&
                                player.getMediaItemAt(index).mediaId == item.mediaId &&
                                player.getMediaItemAt(index).isUnresolved()
                            ) {
                                player.replaceMediaItem(index, resolvedItem)
                            }
                        }
                    }
                }
            }
        }
    }

    /** Cuantas canciones precargar por adelantado (instant-play, más). */
    private val prefetchAhead: Int get() = 20

    /** Precarga TODAS las canciones no resueltas de la cola actual. Útil al activar aleatorio. */
    fun prefetchAllQueue() {
        val player = mediaSession?.player ?: return
        val count = player.mediaItemCount
        val currentIndex = player.currentMediaItemIndex
        // La siguiente pista en orden de reproducción va PRIMERO; el resto en
        // orden aleatorio (así el shuffle nunca espera a resolver lo que sonará).
        val nextIndex = player.nextMediaItemIndex
        val order = buildList {
            if (nextIndex in 0 until count && nextIndex != currentIndex) add(nextIndex)
            addAll((0 until count).filter { it != currentIndex && it != nextIndex }.shuffled())
        }
        for (index in order) {
            val item = runCatching { player.getMediaItemAt(index) }.getOrNull() ?: continue
            if (!item.isUnresolved() || resolving.contains(item.mediaId)) continue
            scope.launch {
                prefetchPermits.withPermit {
                    val resolvedItem = resolveItem(item) ?: return@withPermit
                    main.post {
                        runCatching {
                            if (
                                index < player.mediaItemCount &&
                                player.getMediaItemAt(index).mediaId == item.mediaId &&
                                player.getMediaItemAt(index).isUnresolved()
                            ) {
                                player.replaceMediaItem(index, resolvedItem)
                            }
                        }
                    }
                }
            }
        }
    }

private fun handlePlaybackError(player: Player) {
        val index = player.currentMediaItemIndex
        val item = player.currentMediaItem ?: return
        val mediaId = item.mediaId

        val attempts = (errorRetries[mediaId] ?: 0)
        if (attempts >= 2) {
            Log.w(TAG, "Max reintentos para $mediaId, saltando")
            errorRetries.remove(mediaId)
            skipOrStop(player)
            return
        }

        // Sin red no hay nada que reintentar: preparar de nuevo solo provoca
        // otra cadena de errores que termina saltando canciones. La cola queda
        // en pausa y se reintentará cuando el usuario vuelva a dar play.
        if (!hasNetwork()) {
            Log.w(TAG, "Sin red: no se reintenta $mediaId, cola en pausa")
            player.pause()
            return
        }

        scope.launch {
            val quality = sharedPrefs.getString("audio_quality", "alta") ?: "alta"
            // URL directa (mp3 de podcast): no existe resolutor de YouTube.
            // Reintenta preparar y, si vuelve a fallar, salta a la siguiente.
            val directUri = item.localConfiguration?.uri?.toString().orEmpty()
            if (directUri.isNotBlank() && !directUri.contains("youtube.com/watch")) {
                errorRetries[mediaId] = attempts + 1
                main.post {
                    if (player.currentMediaItemIndex != index) return@post
                    if (player.currentMediaItem?.mediaId != mediaId) return@post
                    player.prepare()
                    if (player.playWhenReady) player.play()
                }
                return@launch
            }
            // La URL anterior falló (caducada, limitada o servida como HTML):
            // hay que olvidarla también en las cachés para no repetirla.
            resolvedUrls.remove(mediaId)
            audioUrlCache.invalidate(mediaId)
            val resolvedItem = runCatching {
                val url = repository.resolveAudioUrl(mediaId, quality)
                if (url.isNullOrBlank()) null
                else item.buildUpon().setUri(url).build()
            }.getOrNull()

            main.post {
                if (player.currentMediaItemIndex != index) return@post
                if (player.currentMediaItem?.mediaId != mediaId) return@post

                if (resolvedItem != null) {
                    errorRetries[mediaId] = attempts + 1
                    val newUrl = resolvedItem.localConfiguration?.uri?.toString().orEmpty()
                    resolvedUrls[mediaId] = newUrl
                    audioUrlCache.put(mediaId, newUrl)
                    Log.d(TAG, "URL re-resuelta para $mediaId (intento ${attempts + 1})")
                    player.replaceMediaItem(index, resolvedItem)
                    player.prepare()
                    // Solo reanudar si el usuario estaba reproduciendo: si dejó
                    // la cola en pausa, un reintento no debe arrancarla solo.
                    if (player.playWhenReady) player.play()
                    prefetchUpcoming(player, prefetchAhead)
                } else {
                    errorRetries.remove(mediaId)
                    // Sin red la URL no se resuelve: pausar sin mover la cola
                    // (saltar ahora recorrería las 26 pistas una a una).
                    if (!hasNetwork()) {
                        Log.w(TAG, "Sin URL de audio para $mediaId (sin red): cola en pausa")
                        player.pause()
                    } else {
                        skipOrStop(player)
                    }
                }
            }
        }
    }

    private fun skipOrStop(player: Player) {
        // Sin red TODAS las pistas fallan: saltar una haría saltar todas
        // (el "mal aleatorio" al abrir). En ese caso, pausar sin mover la cola.
        if (!hasNetwork()) {
            Log.w(TAG, "Sin red: no se avanza la cola, en pausa")
            player.pause()
            return
        }
        // En pausa NUNCA se avanza la cola: los errores en cadena (pista sin
        // resolver / sin red) saltaban canciones una y otra vez aunque el
        // usuario no hubiera pedido reproducir nada al abrir la app.
        if (player.playWhenReady && player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.prepare()
            // Tras un error solo avanza "solo" si estaba reproduciendo; si estaba
            // en pausa, se prepara la siguiente pero no se arranca nada.
            if (player.playWhenReady) player.play()
            prefetchUpcoming(player, prefetchAhead)
            // Tras un error, la nueva siguiente es la prioridad (evita
            // encadenar varios saltos "música por música").
            prefetchNextImmediate(player)
        } else {
            player.pause()
        }
    }

    private fun MediaItem.isUnresolved(): Boolean =
        localConfiguration?.uri?.toString()?.contains("youtube.com/watch") == true

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (closeForGood) return
        Log.d(CLOSE_TAG, "onTaskRemoved: ventana de recientes cerrada → cierre total")
        // Quitar la notificación antes de parar el servicio: MediaSessionService
        // en foreground mantiene la notificación viva si solo se llama a stopSelf.
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        // Borra la cola guardada, suelta el controlador y termina el servicio.
        // Al volver a abrir la app no se retoma la última canción ni aparece
        // el mini reproductor: es como si se acabara de instalar.
        AppCloseReceiver.closeForGood(applicationContext)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(CLOSE_TAG, "servicio destruido (closeForGood=$closeForGood)")
        super.onDestroy()
        if (!closeForGood) runCatching { saveSnapshot() }
        runCatching { unregisterReceiver(smartReceiver) }
        runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        main.removeCallbacksAndMessages(null)
        runCatching<Unit> { equalizer?.release() }
        equalizer = null
        runCatching<Unit> { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        fadeAnimator?.cancel()
        fadeAnimator = null
        scopeJob.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
    }

    companion object {
        /**
         * Cierre total desde [com.popmusic.app.AppCloseReceiver] (8 min): el
         * servicio se detiene antes de matar el proceso (MediaSessionService es
         * START_STICKY y si muere con el servicio vivo el sistema lo revive y
         * re-publica la notificación). Con la bandera activa, onDestroy NO
         * re-guarda la cola que el receptor acaba de borrar.
         */
        @Volatile
        var closeForGood = false

        const val TAG = "PlaybackService"
        /** Tag compartido con AppCloseReceiver para ver el cierre 8 min en un solo filtro. */
        const val CLOSE_TAG = "AppClose"
        const val PREFETCH_AHEAD = 3
        /**
         * Tope de canciones que se calientan al cargar una cola. Antes eran 80
         * con 6 permisos: saturaban la red y la "siguiente" tardaba en resolverse.
         * Con pocas basta (el resto se resuelve al acercarse su turno).
         */
        const val MAX_QUEUE_WARM = 10
        const val APPEND_WARM_DELAY_MS = 3_000L
        /** Acción custom del botón de repetir de la notificación. */
        const val COMMAND_CYCLE_REPEAT = "popmusic.CYCLE_REPEAT"
        const val MIN_BUFFER_MS = 15_000
        const val MAX_BUFFER_MS = 50_000
        const val BUFFER_FOR_PLAYBACK_MS = 200
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 1_000
        const val SNAPSHOT_KEY = "playback_snapshot"
        const val SNAPSHOT_MAX_ITEMS = 100
        const val SNAPSHOT_TTL_MS = 6L * 60L * 60L * 1000L
        /** Pref con el timestamp de la última actividad de reproducción. */
        const val KEY_LAST_ACTIVE = "last_active_at"
        const val REQUEST_OPEN_PLAYER = 4401
        const val FADE_IN_MS = 220L
        const val CROSSFADE_TICK_MS = 100L
    }
}

