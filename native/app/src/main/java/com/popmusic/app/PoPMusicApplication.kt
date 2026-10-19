package com.popmusic.app

import android.app.Application
import com.popmusic.app.data.youtube.NewPipeInitializer
import com.popmusic.app.player.PlayerConnection

class PoPMusicApplication : Application() {

    lateinit var playerConnection: PlayerConnection
        private set

    override fun onCreate() {
        super.onCreate()
        NewPipeInitializer.ensureInitialized()
        playerConnection = PlayerConnection(this)
    }
}
