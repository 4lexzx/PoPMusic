package com.popmusic.app.ui.screens.home

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Avisa al Home cuando la app vuelve a primer plano para refrescar
 * únicamente en ese momento, no al cambiar de pestaña dentro de la app.
 */
object HomeRefreshBus {

    private val _resumedAt = MutableStateFlow(0L)

    val resumedAt: StateFlow<Long> = _resumedAt

    fun notifyAppResumed() {
        _resumedAt.value = SystemClock.elapsedRealtime()
    }
}
