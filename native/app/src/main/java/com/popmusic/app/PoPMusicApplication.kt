package com.popmusic.app

import android.app.Application
import android.os.StrictMode
import android.os.SystemClock
import android.util.Log
import com.popmusic.app.data.auth.AuthRepository
import com.popmusic.app.data.downloads.DownloadRepository
import com.popmusic.app.data.images.newImageLoader
import com.popmusic.app.data.info.ArtistInfoRepository
import com.popmusic.app.data.info.TranslationRepository
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.data.music.MusicAccountRepository
import com.popmusic.app.data.music.MusicOAuthManager
import com.popmusic.app.data.music.MusicSyncManager
import com.popmusic.app.data.lyrics.LyricsRepository
import com.popmusic.app.data.prefs.AppPreferences
import com.popmusic.app.player.AudioUrlCache
import com.popmusic.app.player.PlayerConnection

class PoPMusicApplication : Application() {

    lateinit var playerConnection: PlayerConnection
        private set

    lateinit var libraryRepository: LibraryRepository
        private set

    lateinit var downloadRepository: DownloadRepository
        private set

    lateinit var preferences: AppPreferences
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var lyricsRepository: LyricsRepository
        private set

    lateinit var translationRepository: TranslationRepository
        private set

    lateinit var artistInfoRepository: ArtistInfoRepository
        private set

    lateinit var musicAccountRepository: MusicAccountRepository
        private set

    /**
     * Sesión de la sala "Todos controlan" (tanda 9): vive mientras la app
     * viva, para que navegar fuera de la pantalla no cierre la sala.
     */
    val partySession: com.popmusic.app.data.party.PartySession by lazy {
        com.popmusic.app.data.party.PartySession(this)
    }

    /** Caché en disco de URLs de audio resueltas (arranque perezoso, sin coste inicial). */
    val audioUrlCache: AudioUrlCache by lazy { AudioUrlCache(this) }

    /**
     * Motor de importación/sincronización con YouTube y Spotify. Es perezoso para
     * no gastar tiempo de arranque: se crea la primera vez que la app lo necesita.
     */
    val musicSync: MusicSyncManager by lazy {
        val youtube = com.popmusic.app.data.youtube.YouTubeRepository()
        MusicSyncManager(
            context = this,
            library = libraryRepository,
            accounts = musicAccountRepository,
            oauth = MusicOAuthManager(this, musicAccountRepository),
            youtube = youtube,
        )
    }

    @Volatile
    private var warmedImageLoader: coil3.ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        START_UPTIME = SystemClock.uptimeMillis()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build(),
            )
        }
        mark("playerConnection") { playerConnection = PlayerConnection(this) }
        mark("libraryRepository") { libraryRepository = LibraryRepository(this) }
        mark("preferences") { preferences = AppPreferences(this) }
        mark("downloadRepository") {
            downloadRepository = DownloadRepository(this, libraryRepository, preferences)
        }
        mark("authRepository") { authRepository = AuthRepository(this) }
        mark("lyricsRepository") { lyricsRepository = LyricsRepository(this) }
        mark("translationRepository") { translationRepository = TranslationRepository() }
        mark("artistInfoRepository") { artistInfoRepository = ArtistInfoRepository() }
        mark("musicAccountRepository") {
            musicAccountRepository = MusicAccountRepository(this)
        }
        coil3.SingletonImageLoader.setSafe { warmedImageLoader ?: newImageLoader(it) }
        Thread({ warmedImageLoader = newImageLoader(this) }, "coil-warmup").start()
        authRepository.restore()
        Log.d(STARTUP_TAG, "Application.onCreate total ${SystemClock.uptimeMillis() - START_UPTIME}ms")
    }

    private inline fun mark(name: String, block: () -> Unit) {
        val start = SystemClock.uptimeMillis()
        block()
        Log.d(STARTUP_TAG, "$name ${SystemClock.uptimeMillis() - start}ms")
    }

    companion object {
        const val STARTUP_TAG = "StartupPerf"
        var START_UPTIME = 0L
    }
}
