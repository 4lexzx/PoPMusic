package com.popmusic.app

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.popmusic.app.player.PlaybackService

/**
 * Item 14: cierre automático a los 8 min en 2.º plano (lógica Spotify).
 *
 * El freezer de Android/Samsung congela el proceso ~10 s después de irse a
 * segundo plano (`am_freeze`), así que un temporizador en memoria (coroutine o
 * Handler) nunca llega a ejecutarse: el `delay` se queda congelado. Esta alarma
 * la entrega el sistema (descongela el proceso, o lo reinicia si murió) y:
 *
 *  - si hay música sonando → reprograma +60 s;
 *  - si hay descargas activas → reprograma +60 s (y es actividad);
 *  - si el usuario ya volvió → se ignora (onResume también la cancela);
 *  - si hay una pausa RECIENTE (asistente de voz, TikTok, audio…) → no
 *    cierra: reprograma para que pasen 8 min desde la última actividad;
 *  - si lleva ≥ 8 min sin ninguna actividad → cierra del todo: borra la
 *    cola guardada (sin mini reproductor ni música al volver), suelta el
 *    controlador, para el servicio (quita la notificación) y termina la
 *    tarea de recientes. NO mata el proceso: killProcess hacía que
 *    MediaSessionService (START_STICKY) lo reviviera con la notificación
 *    huérfana.
 */
class AppCloseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_CLOSE_CHECK) return
        val app = context.applicationContext as? PoPMusicApplication

        // Carrera con el regreso del usuario: si la actividad está en primer
        // plano, no cerrar (onResume cancela la alarma, pero por si acaso).
        MainActivity.instance?.let { activity ->
            if (!activity.appInBackground()) {
                Log.d(TAG, "Alarma de cierre ignorada: el usuario ya volvio")
                return
            }
        }

        val playing = runCatching {
            app?.playerConnection?.state?.value?.isPlaying == true
        }.getOrDefault(false)
        val downloading = runCatching {
            app?.downloadRepository?.progress?.value?.isNotEmpty() == true
        }.getOrDefault(false)
        val now = System.currentTimeMillis()
        val lastActive = runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PlaybackService.KEY_LAST_ACTIVE, 0L)
        }.getOrDefault(0L)
        val idleMs = if (lastActive > 0L) now - lastActive else -1L

        // Diagnóstico: con `adb logcat -s AppClose:D` se ve por qué la
        // alarma decidió reprogramar o cerrar en cada revisión.
        Log.d(
            TAG,
            "revision: musica=$playing descargas=$downloading " +
                "ultimaActividad=" + (if (lastActive > 0L) "hace ${idleMs / 1000}s" else "desconocida"),
        )

        if (playing || downloading) {
            Log.d(TAG, "en uso → reprograma +60 s")
            // Música o descarga = actividad: mantiene fresco el contador
            // (si el estado quedó desfasado, el cierre no puede dispararse
            // con una marca vieja mientras algo sigue en marcha).
            markActive(context)
            schedule(context, RETRY_MS)
            return
        }

        // Sin marca de actividad (proceso nuevo o preferencias perdidas):
        // NUNCA cerrar a ciegas; empezar la cuenta de 8 min desde ahora.
        if (lastActive <= 0L) {
            Log.d(TAG, "sin marca de actividad → cuenta de 8 min desde ahora")
            markActive(context)
            schedule(context, IDLE_CLOSE_MS)
            return
        }

        // Pausa reciente (Bixby, TikTok, audio de WhatsApp…): NO cerrar,
        // solo asegurar que pasen 8 min desde la última actividad.
        if (idleMs < IDLE_CLOSE_MS) {
            val restMs = IDLE_CLOSE_MS - idleMs + 1_000L
            Log.d(TAG, "pausa hace ${idleMs / 1000}s → cierre en ${restMs / 1000}s")
            schedule(context, restMs)
            return
        }

        Log.d(TAG, "Cierre automatico: ${idleMs / 1000}s sin actividad")
        closeForGood(context)
    }

    private fun markActive(context: Context) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(PlaybackService.KEY_LAST_ACTIVE, System.currentTimeMillis())
                .apply()
        }
    }

    companion object {
        private const val TAG = "AppClose"
        const val ACTION_CLOSE_CHECK = "com.popmusic.app.CLOSE_CHECK"
        private const val REQUEST_CODE = 78014
        private const val PREFS_NAME = "popmusic_prefs"
        private const val SNAPSHOT_KEY = "playback_snapshot"
        private const val RETRY_MS = 60_000L

        /** Ventana sin actividad (play/pausa/descarga) antes de cerrar. */
        private const val IDLE_CLOSE_MS = 8L * 60L * 1000L

        /** Id de la notificación de media3 (MediaNotification.DEFAULT). */
        private const val NOTIFICATION_ID = 1001

        /**
         * Cierre total, compartido con [PlaybackService.onTaskRemoved] (cuando el
         * usuario barre la app de las ventanas de recientes) y con la alarma de
         * los 8 min. Borra la cola guardada, suelta el controlador, quita la
         * notificación, para el servicio y elimina la tarea. Al volver a abrir es
         * como la primera vez: sin mini reproductor y sin retomar la última canción.
         */
        fun closeForGood(context: Context) {
            if (PlaybackService.closeForGood) return
            PlaybackService.closeForGood = true
            val app = context.applicationContext as? PoPMusicApplication

            // 1. Sin cola guardada: al volver es como la primera vez. La bandera
            //    evita que onDestroy del servicio re-guarde la cola borrada.
            runCatching {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(SNAPSHOT_KEY)
                    .commit()
            }
            // 2. Soltar el controlador y limpiar el estado en memoria (mini
            //    reproductor vacío al volver) y así dejar el servicio sin
            //    clientes para que pueda destruirse.
            runCatching { app?.playerConnection?.resetAfterClose() }
            // 3. Notificación fuera ya (refuerzo del borrado en onDestroy).
            runCatching {
                context.getSystemService(NotificationManager::class.java)
                    ?.cancel(NOTIFICATION_ID)
            }
            // 4. Parar el servicio: media3 destruye la sesión y quita su
            //    notificación en onDestroy (startService sin kill no se revive).
            runCatching { context.stopService(Intent(context, PlaybackService::class.java)) }
            // 5. Quitar la tarea de recientes (funciona aunque no haya Activity viva).
            runCatching {
                context.getSystemService(ActivityManager::class.java)
                    .appTasks
                    .forEach { it.finishAndRemoveTask() }
            }
        }

        private fun pendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, AppCloseReceiver::class.java).setAction(ACTION_CLOSE_CHECK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        /**
         * Programa (o reprograma) la revisión de cierre. `setAndAllowWhileIdle`
         * dispara ≈ a la hora programada incluso en Doze y no necesita el
         * permiso de alarmas exactas (denegado por defecto en targetSdk 37).
         */
        fun schedule(context: Context, delayMs: Long) {
            val alarm = context.getSystemService(AlarmManager::class.java) ?: return
            alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMs,
                pendingIntent(context),
            )
        }

        fun cancel(context: Context) {
            val alarm = context.getSystemService(AlarmManager::class.java) ?: return
            alarm.cancel(pendingIntent(context))
        }
    }
}
