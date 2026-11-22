package com.popmusic.app.data.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.popmusic.app.BuildConfig
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Flujo OAuth 2.0 + PKCE para conectar cuentas de YouTube y Spotify.
 *
 * Usa el redirect de loopback (`http://127.0.0.1:5599/callback`), que es el que
 * ambos proveedores aceptan para aplicaciones instaladas. El navegador del
 * sistema completa el consentimiento y el `code` vuelve a un pequeño servidor
 * local que escucha una única petición.
 */
class MusicOAuthManager(
    private val context: Context,
    private val accounts: MusicAccountRepository,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val prefs = context.applicationContext
        .getSharedPreferences("popmusic_music_oauth", Context.MODE_PRIVATE)

    /** Servidor de callback activo (uno solo a la vez para el puerto 5599). */
    private var callbackServer: LoopbackCallbackServer? = null

    /** Indica si el servicio tiene credenciales configuradas en local.properties. */
    fun isConfigured(service: MusicService): Boolean = when (service) {
        MusicService.YOUTUBE -> BuildConfig.YOUTUBE_CLIENT_ID.isNotBlank()
        MusicService.SPOTIFY -> BuildConfig.SPOTIFY_CLIENT_ID.isNotBlank()
    }

    /** Redirige al navegador para autorizar el servicio. Devuelve false si falta configurar. */
    fun startAuthorization(service: MusicService, onResult: (Boolean, String?) -> Unit): Boolean {
        if (!isConfigured(service)) return false

        val verifier = randomToken(64)
        val challenge = sha256Base64(verifier)
        val state = when (service) {
            MusicService.YOUTUBE -> "yt"
            MusicService.SPOTIFY -> "sp"
        } + "." + randomToken(16)

        prefs.edit()
            .putString(KEY_VERIFIER + service.name, verifier)
            .putString(KEY_STATE + service.name, state)
            .apply()

        val redirect = redirectUri(service)
        val port = portFrom(redirect)
        Log.i(TAG, "Iniciando ${service.displayName()}: redirect=$redirect")

        // Escucha una única llamada de vuelta antes de abrir el navegador.
        // Cierra el intento anterior para no dejar el puerto 5599 tomado.
        callbackServer?.close()
        val server = LoopbackCallbackServer(
            port = port,
            onError = { message -> onResult(false, message) },
            onCallback = { uri -> completeAuthorization(service, uri, onResult) },
        )
        // Bind SINCRÓNICO con reintentos: si el puerto sigue tomado NO se
        // abre el navegador (antes se abría igual y el flujo moría al
        // volver: "no me redirecciona a la app y no me conecta").
        val bindError = server.bind()
        if (bindError != null) {
            Log.e(TAG, bindError)
            onResult(false, bindError)
            return true
        }
        callbackServer = server

        val authorizeUrl = when (service) {
            MusicService.YOUTUBE -> buildString {
                append("https://accounts.google.com/o/oauth2/v2/auth?")
                append("client_id=").append(Uri.encode(BuildConfig.YOUTUBE_CLIENT_ID))
                append("&redirect_uri=").append(Uri.encode(redirect))
                append("&response_type=code")
                append("&scope=").append(Uri.encode(YOUTUBE_SCOPES))
                append("&code_challenge=").append(Uri.encode(challenge))
                append("&code_challenge_method=S256")
                append("&access_type=offline&prompt=consent")
                append("&state=").append(Uri.encode(state))
            }

            MusicService.SPOTIFY -> buildString {
                append("https://accounts.spotify.com/authorize?")
                append("client_id=").append(Uri.encode(BuildConfig.SPOTIFY_CLIENT_ID))
                append("&redirect_uri=").append(Uri.encode(redirect))
                append("&response_type=code")
                append("&scope=").append(Uri.encode(SPOTIFY_SCOPES))
                append("&code_challenge=").append(Uri.encode(challenge))
                append("&code_challenge_method=S256")
                append("&state=").append(Uri.encode(state))
            }
        }

        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            Log.e(TAG, "No se pudo abrir el navegador para OAuth", it)
            onResult(false, "No se pudo abrir el navegador")
            return true
        }
        return true
    }

    private fun completeAuthorization(
        service: MusicService,
        uri: Uri,
        onResult: (Boolean, String?) -> Unit,
    ) {
        val expectedState = prefs.getString(KEY_STATE + service.name, null)
        val returnedState = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")
        Log.i(
            TAG,
            "Callback recibido: stateOk=${expectedState == returnedState} " +
                "code=${!code.isNullOrBlank()} error=$error",
        )
        if (expectedState != null && returnedState != expectedState) {
            Log.e(TAG, "state inválido: esperaba $expectedState, llegó $returnedState")
            onResult(false, "La respuesta de autorización no coincide (state inválido)")
            return
        }
        if (error != null) {
            Log.e(TAG, "El proveedor devolvió error=$error")
            onResult(false, "Autorización cancelada: $error")
            return
        }
        if (code.isNullOrBlank()) {
            Log.e(TAG, "El proveedor no devolvió código")
            onResult(false, "La respuesta de autorización no trajo código")
            return
        }

        val verifier = prefs.getString(KEY_VERIFIER + service.name, null)
        if (verifier.isNullOrBlank()) {
            Log.e(TAG, "No hay verificador PKCE guardado para ${service.name}")
            onResult(false, "Se perdió el verificador PKCE")
            return
        }

        Thread {
            Log.i(TAG, "Intercambiando código por token…")
            val result = runCatching { exchangeCode(service, code, verifier) }
            val failure = result.exceptionOrNull()
            failure?.let { Log.e(TAG, "Fallo el intercambio de código", it) }
            // Un corte de DNS del operador no debe matar el flujo: el error
            // se traduce al español para que el usuario sepa qué hacer.
            val message = failure?.let { friendlyNetworkError(it) } ?: failure?.message
            onResult(result.isSuccess, if (result.isSuccess) null else message)
        }.start()
    }

    private fun exchangeCode(service: MusicService, code: String, verifier: String) {
        val (tokenUrl, body) = when (service) {
            MusicService.YOUTUBE -> "https://oauth2.googleapis.com/token" to FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("client_id", BuildConfig.YOUTUBE_CLIENT_ID)
                .add("redirect_uri", redirectUri(service))
                .add("code_verifier", verifier)
                .apply {
                    if (BuildConfig.YOUTUBE_CLIENT_SECRET.isNotBlank()) {
                        add("client_secret", BuildConfig.YOUTUBE_CLIENT_SECRET)
                    }
                }
                .build()

            MusicService.SPOTIFY -> "https://accounts.spotify.com/api/token" to FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("client_id", BuildConfig.SPOTIFY_CLIENT_ID)
                .add("redirect_uri", redirectUri(service))
                .add("code_verifier", verifier)
                .build()
        }

        // Solo se reintenta mientras el POST no haya salido: en cuanto
        // Google devuelve 200 el código queda consumido y reenviarlo
        // daría invalid_grant.
        val json = retryNetwork("Token") { postForm(tokenUrl, body) }
        val access = json.optString("access_token")
        if (access.isBlank()) error("El proveedor no devolvió token de acceso")

        val profile = retryNetwork("Perfil") { fetchProfile(service, access) }
        accounts.linkAccount(
            MusicAccount(
                service = service,
                userId = profile.first,
                userName = profile.second,
                accessToken = access,
                refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
                expiresAt = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L,
            ),
        )
        prefs.edit()
            .remove(KEY_VERIFIER + service.name)
            .remove(KEY_STATE + service.name)
            .apply()
        Log.i(TAG, "${service.displayName()} conectado como ${profile.second}")
    }

    /** Renueva el token si está vencido y devuelve uno válido (o null si no hay cuenta). */
    suspend fun freshToken(service: MusicService): String? {
        val account = accounts.accountFor(service) ?: return null
        val token = account.accessToken
        if (token != null && !account.isExpired()) return token
        // Sin refresh_token no hay forma de renovarlo: devolver el token
        // vencido hacía que cada llamada fallara en silencio (401).
        val refresh = account.refreshToken ?: return null
        return runCatching { refreshToken(service, account, refresh) }
            .getOrElse {
                Log.e(TAG, "No se pudo renovar el token de ${service.displayName()}", it)
                null
            }
    }

    private suspend fun refreshToken(
        service: MusicService,
        account: MusicAccount,
        refresh: String,
    ): String? = withContextIo {
        val (tokenUrl, body) = when (service) {
            MusicService.YOUTUBE -> "https://oauth2.googleapis.com/token" to FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refresh)
                .add("client_id", BuildConfig.YOUTUBE_CLIENT_ID)
                .apply {
                    if (BuildConfig.YOUTUBE_CLIENT_SECRET.isNotBlank()) {
                        add("client_secret", BuildConfig.YOUTUBE_CLIENT_SECRET)
                    }
                }
                .build()

            MusicService.SPOTIFY -> "https://accounts.spotify.com/api/token" to FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refresh)
                .add("client_id", BuildConfig.SPOTIFY_CLIENT_ID)
                .build()
        }
        val json = retryNetwork("Renovación") { postForm(tokenUrl, body) }
        val access = json.optString("access_token").takeIf { it.isNotBlank() } ?: return@withContextIo null
        accounts.linkAccount(
            account.copy(
                accessToken = access,
                refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() } ?: refresh,
                expiresAt = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L,
            ),
        )
        access
    }

    private fun fetchProfile(service: MusicService, token: String): Pair<String, String> {
        val (url, idKey, nameKey) = when (service) {
            MusicService.YOUTUBE -> Triple(
                "https://openidconnect.googleapis.com/v1/userinfo",
                "sub",
                "name",
            )

            MusicService.SPOTIFY -> Triple("https://api.spotify.com/v1/me", "id", "display_name")
        }
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Perfil no disponible (${response.code})")
            val json = JSONObject(text)
            val id = json.optString(idKey).takeIf { it.isNotBlank() } ?: "desconocido"
            val name = json.optString(nameKey)
                .takeIf { it.isNotBlank() }
                ?: json.optString("email").takeIf { it.isNotBlank() }
                ?: id
            return id to name
        }
    }

    private fun postForm(url: String, body: FormBody): JSONObject {
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    JSONObject(text).optString("error_description")
                        .ifBlank { JSONObject(text).optString("error") }
                }.getOrDefault("")
                error("Error ${response.code}: $detail")
            }
            return JSONObject(text)
        }
    }

    /**
     * Reintenta una llamada de red ante fallos transitorios (DNS caído,
     * timeout, corte de datos): en pruebas reales el sistema BLOQUEA el
     * DNS de la app mientras está en segundo plano
     * (`isBlocked=true, 0ms` — el navegador va en primer plano y por eso
     * él sí funciona). La ventana cubre el regreso del usuario a la app
     * (~10 s): al volver a primer plano el bloqueo desaparece y el
     * siguiente intento entra. El código de autorización es válido ~10 min.
     */
    private fun <T> retryNetwork(label: String, block: () -> T): T {
        var last: Throwable? = null
        for (attempt in 0..NETWORK_ATTEMPTS) {
            try {
                return block()
            } catch (t: Throwable) {
                if (!isTransientNetwork(t)) throw t
                last = t
                Log.w(
                    TAG,
                    "$label falló (intento ${attempt + 1}/" +
                        "${NETWORK_ATTEMPTS + 1}): ${t.message}",
                )
                if (attempt < NETWORK_ATTEMPTS) Thread.sleep(RETRY_DELAYS_MS[attempt])
            }
        }
        throw last ?: IllegalStateException("$label sin resultado")
    }

    /** Solo las fallas de conectividad se reintentan; un 4xx no. */
    private fun isTransientNetwork(t: Throwable): Boolean {
        var current: Throwable? = t
        while (current != null) {
            if (current is java.io.IOException) return true
            current = current.cause
        }
        return false
    }

    /** Mensaje en español cuando el fallo fue de red (si no, null). */
    private fun friendlyNetworkError(t: Throwable): String? {
        var current: Throwable? = t
        while (current != null) {
            if (current is java.net.UnknownHostException) {
                return "Sin conexión a internet: no se pudo resolver el servidor. " +
                    "Revisa tus datos móviles y vuelve a intentarlo."
            }
            current = current.cause
        }
        return null
    }

    private fun redirectUri(service: MusicService): String = when (service) {
        MusicService.YOUTUBE -> BuildConfig.YOUTUBE_REDIRECT_URI
        MusicService.SPOTIFY -> BuildConfig.MUSIC_REDIRECT_URI
    }

    private fun portFrom(uri: String): Int =
        runCatching { Uri.parse(uri).port }.getOrDefault(-1).takeIf { it > 0 } ?: 5599

    private fun randomToken(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun sha256Base64(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private suspend fun <T> withContextIo(block: () -> T?): T? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }

    /**
     * Servidor local mínimo que espera la redirección OAuth y responde con una
     * página de confirmación.
     *
     * Protecciones (aprendidas en pruebas reales del 20/11):
     *  - `bind()` es **síncrono y reintenta**: si el intento anterior aún
     *    tenía el puerto tomado (EADDRINUSE), NO se abre el navegador para
     *    no lanzar un flujo que nadie podrá completar.
     *  - **SO_REUSEADDR** + cerrar el listener justo al aceptar: el puerto
     *    queda libre en cuanto llega la petición.
     *  - **Timeout de lectura**: si el navegador conectaba y no mandaba
     *    datos, `readLine()` se colgaba para siempre con el puerto tomado
     *    (fue la causa del EADDRINUSE que rompió la prueba).
     */
    private class LoopbackCallbackServer(
        private val port: Int,
        private val onError: (String) -> Unit,
        private val onCallback: (Uri) -> Unit,
    ) {
        private val executor = Executors.newSingleThreadExecutor()
        private var socket: ServerSocket? = null

        @Volatile
        private var closed = false

        /** Enlaza el puerto y empieza a escuchar. Devuelve null si quedó listo. */
        fun bind(): String? {
            var lastError: String? = null
            repeat(BIND_ATTEMPTS) { attempt ->
                try {
                    val server = ServerSocket()
                    runCatching { server.reuseAddress = true }
                    server.bind(InetSocketAddress(port))
                    socket = server
                    if (closed) {
                        runCatching { server.close() }
                        return "Flujo cancelado"
                    }
                    server.soTimeout = SERVER_TIMEOUT_MS
                    executor.execute { listen(server) }
                    Log.i(TAG, "Escuchando en el puerto $port")
                    return null
                } catch (e: IOException) {
                    lastError = e.message
                    Log.w(
                        TAG,
                        "Bind del puerto $port " +
                            "(intento ${attempt + 1}/$BIND_ATTEMPTS): ${e.message}",
                    )
                    if (attempt < BIND_ATTEMPTS - 1) runCatching { Thread.sleep(400) }
                }
            }
            return "No se pudo escuchar en el puerto $port ($lastError)"
        }

        private fun listen(server: ServerSocket) {
            var handled = false
            try {
                // Acepta UNA petición y cierra el listener de inmediato: el
                // puerto queda libre aunque el manejo se tarde o cuelgue.
                val connection = try {
                    server.accept()
                } finally {
                    runCatching { server.close() }
                }
                connection.use { c ->
                    c.soTimeout = READ_TIMEOUT_MS
                    handle(c)
                }
                handled = true
            } catch (t: Throwable) {
                Log.w(TAG, "Servidor de callback detenido: ${t.message}")
                if (!closed && !handled) {
                    onError("No se pudo recibir la respuesta en el puerto $port (${t.message})")
                }
            } finally {
                runCatching { server.close() }
            }
        }

        fun close() {
            closed = true
            runCatching { socket?.close() }
            executor.shutdown()
        }

        private fun handle(connection: Socket) {
            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
            val requestLine = reader.readLine().orEmpty()
            val path = requestLine.split(" ").getOrNull(1).orEmpty()
            val uri = Uri.parse("http://127.0.0.1:$port$path")

            val body = """
                <html><head><meta charset="utf-8"><title>PoP Music</title></head>
                <body style="font-family:sans-serif;background:#0c0c16;color:#fff;text-align:center;padding-top:20vh">
                <h2>Cuenta conectada</h2><p>Ya puedes volver a PoP Music.</p></body></html>
            """.trimIndent().toByteArray()

            val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
            runCatching {
                connection.getOutputStream().apply {
                    write(header.toByteArray())
                    write(body)
                    flush()
                }
            }
            onCallback(uri)
        }
    }

    private companion object {
        const val TAG = "MusicOAuth"
        const val KEY_VERIFIER = "verifier_"
        const val KEY_STATE = "state_"

        /**
         * Intentos de red adicionales y sus esperas (ms): ~90 s en total,
         * para cubrir el tiempo en que el usuario vuelve del navegador a
         * la app (mientras tanto el sistema bloquea el DNS en segundo plano).
         */
        const val NETWORK_ATTEMPTS = 8
        val RETRY_DELAYS_MS = longArrayOf(
            1_500, 3_000, 5_000, 8_000, 12_000, 15_000, 20_000, 25_000,
        )

        /** Reintentos de bind si el puerto 5599 sigue tomado. */
        const val BIND_ATTEMPTS = 4

        /** Espera máxima del navegador a completar el flujo (5 min). */
        const val SERVER_TIMEOUT_MS = 300_000

        /** Espera máxima de la petición del navegador dentro del socket. */
        const val READ_TIMEOUT_MS = 20_000

        const val YOUTUBE_SCOPES =
            "https://www.googleapis.com/auth/youtube " +
                "https://www.googleapis.com/auth/userinfo.profile " +
                "https://www.googleapis.com/auth/userinfo.email"

        const val SPOTIFY_SCOPES =
            "playlist-read-private playlist-read-collaborative " +
                "playlist-modify-public playlist-modify-private " +
                "user-read-private"
    }
}
