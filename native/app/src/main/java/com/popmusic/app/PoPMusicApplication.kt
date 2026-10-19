package com.popmusic.app

import android.app.Application
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.data.youtube.NewPipeInitializer
import com.popmusic.app.player.PlayerConnection

class PoPMusicApplication : Application() {

    lateinit var playerConnection: PlayerConnection
        private set

    lateinit var libraryRepository: LibraryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NewPipeInitializer.ensureInitialized()
        playerConnection = PlayerConnection(this)
        libraryRepository = LibraryRepository(this)
    }
}
