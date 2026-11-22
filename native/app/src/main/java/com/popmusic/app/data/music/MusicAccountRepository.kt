package com.popmusic.app.data.music

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/**
 * Almacena las cuentas de servicios de música vinculadas (YouTube, Spotify) en
 * SharedPreferences. Cada cuenta guarda sus tokens de acceso para poder importar
 * playlists y sincronizar en tiempo real.
 */
class MusicAccountRepository(private val context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("popmusic_music_accounts", Context.MODE_PRIVATE)

    private val _accounts = MutableStateFlow(loadAccounts())
    val accounts: StateFlow<List<MusicAccount>> = _accounts.asStateFlow()

    /** Vincula una cuenta de servicio (guarda o actualiza). */
    fun linkAccount(account: MusicAccount) {
        _accounts.update { current ->
            val withoutExisting = current.filter { it.service != account.service }
            withoutExisting + account
        }
        saveAccounts()
    }

    /** Desvincula un servicio. */
    fun unlink(service: MusicService) {
        _accounts.update { current ->
            current.filter { it.service != service }
        }
        saveAccounts()
    }

    /** Devuelve la cuenta vinculada para un servicio, si existe. */
    fun accountFor(service: MusicService): MusicAccount? =
        _accounts.value.firstOrNull { it.service == service }

    /** Verifica si hay una cuenta conectada y con token válido. */
    fun isConnected(service: MusicService): Boolean =
        accountFor(service)?.isConnected() == true

    private fun saveAccounts() {
        val array = JSONArray()
        _accounts.value.forEach { account ->
            array.put(
                JSONObject()
                    .put("service", account.service.name)
                    .put("userId", account.userId)
                    .put("userName", account.userName)
                    .put("accessToken", account.accessToken)
                    .put("refreshToken", account.refreshToken)
                    .put("expiresAt", account.expiresAt)
                    .put("connectedAt", account.connectedAt),
            )
        }
        prefs.edit().putString("music_accounts", array.toString()).apply()
    }

    private fun loadAccounts(): List<MusicAccount> {
        val json = prefs.getString("music_accounts", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val service = MusicService.valueOf(obj.getString("service"))
                    add(
                        MusicAccount(
                            service = service,
                            userId = obj.getString("userId"),
                            userName = obj.optString("userName", ""),
                            accessToken = obj.optString("accessToken", null).takeIf { it.isNotBlank() },
                            refreshToken = obj.optString("refreshToken", null).takeIf { it.isNotBlank() },
                            expiresAt = obj.optLong("expiresAt", 0L).takeIf { it > 0L },
                            connectedAt = obj.optLong("connectedAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_ACCOUNTS = "music_accounts"
    }
}
