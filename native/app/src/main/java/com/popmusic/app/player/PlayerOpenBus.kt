package com.popmusic.app.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Item 5: abre el reproductor cuando se toca la notificación de media
 * session. `MainActivity` recibe el extra EXTRA_OPEN_PLAYER (PendingIntent
 * de sesión) y llama a [requestOpen]; `PopMusicApp` consume el flujo y
 * navega a la pantalla del reproductor.
 */
object PlayerOpenBus {

    /**
     * Extra del PendingIntent de la sesión de media (lo lee MainActivity);
     * cuando la app se abre con él, hay que navegar al reproductor.
     */
    const val EXTRA_OPEN_PLAYER = "com.popmusic.app.OPEN_PLAYER"

    private val _pending = MutableStateFlow(false)

    val pending: StateFlow<Boolean> = _pending

    /** Instante de la última petición (para no pisar el reinicio del item 6). */
    @Volatile
    var requestedAtMs: Long = 0L
        private set

    fun requestOpen() {
        requestedAtMs = System.currentTimeMillis()
        _pending.value = true
    }

    fun consume() {
        _pending.value = false
    }
}
