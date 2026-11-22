package com.popmusic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.core.app.ActivityCompat
import com.popmusic.app.player.PlayerOpenBus
import com.popmusic.app.ui.theme.PoPMusicTheme
import com.popmusic.app.ui.screens.home.HomeRefreshBus

class MainActivity : ComponentActivity() {

    private var lastAuthCallback: String? = null
    private var wasStopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        lastAuthCallback = savedInstanceState?.getString(KEY_LAST_AUTH_CALLBACK)
        handleAuthIntent(intent)
        handleOpenPlayerIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        requestNotificationPermission()

        setContent {
            LaunchedEffect(Unit) {
                withFrameNanos { }
                Log.d("StartupPerf", "first frame at ${SystemClock.uptimeMillis() - PoPMusicApplication.START_UPTIME}ms")
            }
            PoPMusicTheme {
                PopMusicApp()
            }
        }
        Log.d("StartupPerf", "setContent done at ${SystemClock.uptimeMillis() - PoPMusicApplication.START_UPTIME}ms")

        window.decorView.setPadding(0, 0, 0, 0)
        window.decorView.post { window.decorView.setPadding(0, 0, 0, 0) }
    }

    override fun reportFullyDrawn() {
        super.reportFullyDrawn()
        Log.d("StartupPerf", "reportFullyDrawn ${SystemClock.uptimeMillis() - PoPMusicApplication.START_UPTIME}ms")
    }

    override fun onResume() {
        super.onResume()
        // Volver a la app cancela el cierre automático de los 8 min.
        runCatching { AppCloseReceiver.cancel(this) }
        if (wasStopped) {
            wasStopped = false
            HomeRefreshBus.notifyAppResumed()
        }
    }

    override fun onStop() {
        super.onStop()
        wasStopped = true
        // Item 6: guarda cuándo se fue a segundo plano para decidir al volver.
        runCatching {
            (application as PoPMusicApplication)
                .preferences
                .markBackgrounded(System.currentTimeMillis())
        }
        // Item 14: alarma del sistema como temporizador fiable (el freezer
        // congela las corutinas en ~10 s, esta la entrega el sistema ~8 min).
        // El cierre y la lógica "8 min sin actividad" viven en AppCloseReceiver.
        runCatching { AppCloseReceiver.schedule(this, BACKGROUND_CLOSE_MS) }
        Log.d("AppClose", "app a segundo plano → revision de cierre en 8 min")
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** Item 14: true si la app está en 2.º plano (lo consulta [AppCloseReceiver]). */
    fun appInBackground(): Boolean = wasStopped

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
        handleOpenPlayerIntent(intent)
    }

    /**
     * Item 5: la notificación de media session abre la app con este extra;
     * avía al bus para que Compose navegue al reproductor.
     */
    private fun handleOpenPlayerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(PlayerOpenBus.EXTRA_OPEN_PLAYER, false) != true) return
        intent.removeExtra(PlayerOpenBus.EXTRA_OPEN_PLAYER)
        PlayerOpenBus.requestOpen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LAST_AUTH_CALLBACK, lastAuthCallback)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "popmusic" || data.host != "auth-callback") return
        val raw = data.toString()
        if (raw == lastAuthCallback) return
        lastAuthCallback = raw
        (application as PoPMusicApplication).authRepository.completeAuthCallback(data)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST,
            )
        }
    }

    companion object {
        /** Instance viva para que [AppCloseReceiver] detecte regresos del usuario. */
        @Volatile
        var instance: MainActivity? = null
            private set

        private const val NOTIFICATION_PERMISSION_REQUEST = 1001
        private const val KEY_LAST_AUTH_CALLBACK = "last_auth_callback"
        private const val BACKGROUND_CLOSE_MS = 8L * 60L * 1000L
    }
}
