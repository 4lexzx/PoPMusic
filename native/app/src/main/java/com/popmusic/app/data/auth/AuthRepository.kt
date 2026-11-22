package com.popmusic.app.data.auth

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.popmusic.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val userId: String,
    val email: String,
    /** Foto de perfil del proveedor (Google/Spotify) si lo compartió. */
    val avatarUrl: String? = null,
)

data class SignUpOutcome(
    val needsConfirmation: Boolean,
    val error: String?,
)

/**
 * Cliente mínimo de Supabase Auth para Android.
 * Usa la API REST oficial con la publishable/anon key (nunca la service_role).
 */
class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("popmusic_auth", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY.trim()
    private val redirectUrl = BuildConfig.SUPABASE_REDIRECT_URL

    val configured: Boolean = baseUrl.isNotBlank() && apiKey.isNotBlank()

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val _recoveryMode = MutableStateFlow(false)
    val recoveryMode: StateFlow<Boolean> = _recoveryMode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun restore() {
        if (!configured || _session.value == null) return
        scope.launch { refreshIfNeeded() }
    }

    suspend fun signIn(email: String, password: String): String? {
        if (!configured) return "Supabase no está configurado en esta app."

        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)

        val response = runCatching {
            call("/token?grant_type=password", "POST", body)
        }.getOrElse {
            return "No se pudo conectar. Revisa tu conexión a internet."
        }

        if (response.code !in 200..299) {
            return friendlyError(response)
        }

        val session = parseSession(JSONObject(response.body))
            ?: return "Supabase devolvió una respuesta inesperada."
        saveSession(session)
        _lastError.value = null
        return null
    }

    suspend fun signUp(email: String, password: String): SignUpOutcome {
        if (!configured) return SignUpOutcome(false, "Supabase no está configurado en esta app.")

        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("redirect_to", redirectUrl)

        val response = runCatching {
            call("/signup", "POST", body)
        }.getOrElse {
            return SignUpOutcome(false, "No se pudo conectar. Revisa tu conexión a internet.")
        }

        if (response.code !in 200..299) {
            return SignUpOutcome(false, friendlyError(response))
        }

        val json = JSONObject(response.body)
        val sessionJson = json.optJSONObject("session") ?: json
        val session = parseSession(sessionJson)
        if (session != null) {
            saveSession(session)
            _lastError.value = null
            return SignUpOutcome(false, null)
        }

        return SignUpOutcome(true, null)
    }

    suspend fun resendConfirmation(email: String): String? {
        if (!configured) return "Supabase no está configurado en esta app."

        val body = JSONObject()
            .put("type", "signup")
            .put("email", email.trim())
            .put("redirect_to", redirectUrl)

        val response = runCatching {
            call("/resend", "POST", body)
        }.getOrElse {
            return "No se pudo conectar. Revisa tu conexión a internet."
        }

        if (response.code !in 200..299) {
            return friendlyError(response)
        }

        _lastError.value = null
        return null
    }

    suspend fun requestPasswordReset(email: String): String? {
        if (!configured) return "Supabase no está configurado en esta app."

        val body = JSONObject()
            .put("email", email.trim())
            .put("redirect_to", redirectUrl)

        val response = runCatching {
            call("/recover", "POST", body)
        }.getOrElse {
            return "No se pudo conectar. Revisa tu conexión a internet."
        }

        if (response.code !in 200..299) {
            return friendlyError(response)
        }

        _lastError.value = null
        return null
    }

    suspend fun updatePassword(password: String): String? {
        if (password.length < 6) {
            return "La contraseña debe tener al menos 6 caracteres."
        }

        val current = _session.value
            ?: return "El enlace de recuperación expiró. Solicita uno nuevo."

        val response = runCatching {
            call(
                "/user",
                "PUT",
                JSONObject().put("password", password),
                current.accessToken,
            )
        }.getOrElse {
            return "No se pudo conectar. Revisa tu conexión a internet."
        }

        if (response.code !in 200..299) {
            return friendlyError(response)
        }

        _lastError.value = null
        _recoveryMode.value = false
        return null
    }

    fun cancelRecovery() {
        _recoveryMode.value = false
        scope.launch { signOut() }
    }

    suspend fun signOut() {
        _recoveryMode.value = false
        val current = _session.value
        if (current != null && configured) {
            runCatching {
                call("/logout", "POST", null, current.accessToken)
            }
        }
        clearSession()
    }

    fun clearError() {
        _lastError.value = null
    }

    /** Genera la URL de OAuth con PKCE. Debe abrirse en el navegador del sistema. */
    fun buildGoogleAuthUrl(): String? {
        if (!configured) return null

        val verifier = generateCodeVerifier()
        prefs.edit().putString(KEY_GOOGLE_VERIFIER, verifier).apply()
        val challenge = generateCodeChallenge(verifier)
        val redirect = Uri.encode(redirectUrl)

        return "$baseUrl/auth/v1/authorize?provider=google" +
            "&redirect_to=$redirect" +
            "&code_challenge=${Uri.encode(challenge)}" +
            "&code_challenge_method=s256"
    }

    /** Llamar desde MainActivity para cualquier callback de Supabase. */
    fun completeAuthCallback(uri: Uri) {
        val queryError = uri.getQueryParameter("error_description")
            ?: uri.getQueryParameter("error")
        if (!queryError.isNullOrBlank()) {
            _lastError.value = friendlyMessage(queryError, 400)
            return
        }

        val fragmentParams = parseFragment(uri.fragment)
        val fragmentError = fragmentParams["error_description"] ?: fragmentParams["error"]
        if (!fragmentError.isNullOrBlank()) {
            _lastError.value = friendlyMessage(fragmentError, 400)
            return
        }

        val fragmentToken = fragmentParams["access_token"]
        if (!fragmentToken.isNullOrBlank()) {
            val session = AuthSession(
                accessToken = fragmentToken,
                refreshToken = fragmentParams["refresh_token"].orEmpty(),
                expiresAt = System.currentTimeMillis() +
                    (fragmentParams["expires_in"]?.toLongOrNull() ?: 3600L) * 1000L,
                userId = "",
                email = "",
            )
            saveSession(session)
            _lastError.value = null
            _recoveryMode.value = fragmentParams["type"] == "recovery"
            scope.launch { hydrateUser(session) }
            return
        }

        val tokenHash = uri.getQueryParameter("token_hash")
        if (!tokenHash.isNullOrBlank()) {
            scope.launch {
                _busy.value = true
                val errorMessage = verifyTokenHash(
                    tokenHash,
                    uri.getQueryParameter("type") ?: "signup",
                )
                _busy.value = false
                _lastError.value = errorMessage
            }
            return
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            _lastError.value = "No se recibió la respuesta de autenticación."
            return
        }

        scope.launch {
            _busy.value = true
            val errorMessage = exchangeCode(code)
            _busy.value = false
            _lastError.value = errorMessage
        }
    }

    private suspend fun hydrateUser(session: AuthSession) {
        val response = runCatching {
            call("/user", "GET", null, session.accessToken)
        }.getOrNull() ?: return
        if (response.code !in 200..299) return

        val json = runCatching { JSONObject(response.body) }.getOrNull() ?: return
        val email = json.optString("email")
        val userId = json.optString("id")
        val avatar = avatarFrom(json)
        val current = _session.value ?: return
        if (current.accessToken != session.accessToken) return
        if (email.isBlank() && userId.isBlank()) return

        saveSession(
            current.copy(
                email = email.ifBlank { current.email },
                userId = userId.ifBlank { current.userId },
                avatarUrl = avatar?.ifBlank { null } ?: current.avatarUrl,
            ),
        )
    }

    /** Extrae la foto de perfil de la respuesta /user de Supabase. */
    private fun avatarFrom(json: JSONObject): String? {
        val metadata = json.optJSONObject("user_metadata") ?: return null
        return listOf("avatar_url", "picture", "photo")
            .asSequence()
            .mapNotNull { key -> metadata.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull()
    }

    private fun parseFragment(fragment: String?): Map<String, String> {
        if (fragment.isNullOrBlank()) return emptyMap()
        return fragment.split("&").mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                Uri.decode(part.substring(0, separator)) to
                    Uri.decode(part.substring(separator + 1))
            }
        }.toMap()
    }

    private suspend fun verifyTokenHash(tokenHash: String, type: String): String? {
        val body = JSONObject()
            .put("token_hash", tokenHash)
            .put("type", type)
            .put("redirect_to", redirectUrl)

        val response = runCatching {
            call("/verify", "POST", body)
        }.getOrElse {
            return "No se pudo conectar. Revisa tu conexión a internet."
        }

        if (response.code !in 200..299) {
            return friendlyError(response)
        }

        val json = runCatching { JSONObject(response.body) }.getOrNull()
        val session = json?.let { parseSession(it.optJSONObject("session") ?: it) }
            ?: return "El enlace no es válido o ya fue usado. Solicita uno nuevo."

        saveSession(session)
        _lastError.value = null
        _recoveryMode.value = type == "recovery"
        scope.launch { hydrateUser(session) }
        return null
    }

    private suspend fun exchangeCode(code: String): String? {
        val verifier = prefs.getString(KEY_GOOGLE_VERIFIER, null)
        prefs.edit().remove(KEY_GOOGLE_VERIFIER).apply()

        if (verifier.isNullOrBlank()) {
            return "La sesión de Google expiró. Inténtalo de nuevo."
        }

        val body = JSONObject()
            .put("auth_code", code)
            .put("code_verifier", verifier)

        val response = runCatching {
            call("/token?grant_type=pkce", "POST", body)
        }.getOrElse {
            return "No se pudo conectar. Revisa tu conexión a internet."
        }

        if (response.code !in 200..299) {
            return friendlyError(response)
        }

        val session = parseSession(JSONObject(response.body))
            ?: return "Supabase devolvió una respuesta inesperada."
        saveSession(session)
        return null
    }

    private suspend fun refreshIfNeeded() {
        val current = _session.value ?: return
        if (current.expiresAt - System.currentTimeMillis() > 60_000L) return
        refresh(current)
    }

    private suspend fun refresh(current: AuthSession): Boolean {
        if (current.refreshToken.isBlank()) {
            clearSession()
            return false
        }

        val response = runCatching {
            call(
                "/token?grant_type=refresh_token",
                "POST",
                JSONObject().put("refresh_token", current.refreshToken),
            )
        }.getOrNull() ?: return false

        if (response.code in 200..299) {
            val next = parseSession(JSONObject(response.body))
            if (next != null) {
                saveSession(next)
                return true
            }
        }

        if (response.code == 400 || response.code == 401 || response.code == 403) {
            clearSession()
        }
        return false
    }

    private suspend fun call(
        path: String,
        method: String,
        body: JSONObject? = null,
        accessToken: String? = null,
    ): ApiResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url("$baseUrl/auth/v1$path")
            .header("apikey", apiKey)
            .header("Content-Type", "application/json")

        if (accessToken != null) {
            builder.header("Authorization", "Bearer $accessToken")
        }

        when (method) {
            "POST" -> builder.post((body?.toString() ?: "{}").toRequestBody(JSON))
            "PUT" -> builder.put((body?.toString() ?: "{}").toRequestBody(JSON))
        }

        client.newCall(builder.build()).execute().use { response ->
            ApiResponse(
                code = response.code,
                body = response.body?.string().orEmpty(),
            )
        }
    }

    private fun loadSession(): AuthSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (accessToken.isNullOrBlank()) return null

        return AuthSession(
            accessToken = accessToken,
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "").orEmpty(),
            expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L),
            userId = prefs.getString(KEY_USER_ID, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, null),
        )
    }

    private fun saveSession(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAt)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_AVATAR_URL, session.avatarUrl)
            .apply()
        _session.value = session
    }

    private fun clearSession() {
        _recoveryMode.value = false
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_GOOGLE_VERIFIER)
            .apply()
        _session.value = null
    }

    private fun parseSession(json: JSONObject): AuthSession? {
        val accessToken = json.optString("access_token")
        if (accessToken.isBlank()) return null

        val expiresIn = json.optLong("expires_in", 3600L)
        val expiresAt = json.optLong("expires_at", 0L).let { seconds ->
            if (seconds > 0L) seconds * 1000L else System.currentTimeMillis() + expiresIn * 1000L
        }
        val user = json.optJSONObject("user")

        return AuthSession(
            accessToken = accessToken,
            refreshToken = json.optString("refresh_token"),
            expiresAt = expiresAt,
            userId = user?.optString("id").orEmpty(),
            email = user?.optString("email").orEmpty(),
            avatarUrl = user?.let { avatarFrom(it) },
        )
    }

    private fun friendlyError(response: ApiResponse): String {
        val raw = runCatching {
            val json = JSONObject(response.body)
            json.optString("msg")
                .ifBlank { json.optString("error_description") }
                .ifBlank { json.optString("message") }
                .ifBlank { json.optString("error") }
        }.getOrDefault("")
        return friendlyMessage(raw, response.code)
    }

    private fun friendlyMessage(raw: String, status: Int): String {
        val message = raw.trim()
        val normalized = message.lowercase()

        return when {
            normalized.contains("invalid login credentials") ->
                "Correo o contraseña incorrectos."
            normalized.contains("email not confirmed") ||
                normalized.contains("email_not_confirmed") ->
                "Confirma tu correo antes de iniciar sesión."
            normalized.contains("already registered") ||
                normalized.contains("user_already_exists") ->
                "Ese correo ya tiene una cuenta. Inicia sesión."
            normalized.contains("password should be at least") ||
                normalized.contains("weak_password") ->
                "La contraseña debe tener al menos 6 caracteres."
            normalized.contains("unable to validate email") ||
                normalized.contains("invalid email") ->
                "Escribe un correo válido."
            normalized.contains("rate limit") || status == 429 ->
                "Demasiados intentos. Espera un momento e inténtalo de nuevo."
            normalized.contains("provider is not enabled") ->
                "Google no está activado en Supabase."
            normalized.contains("signup disabled") ->
                "El registro está desactivado en Supabase."
            normalized.contains("access_denied") ||
                normalized.contains("user cancelled") ->
                "Cancelaste el inicio de sesión."
            normalized.contains("redirect") &&
                (normalized.contains("not allowed") || normalized.contains("invalid")) ->
                "La URL popmusic://auth-callback no está autorizada en Supabase."
            message.isBlank() ->
                "No se pudo completar la operación."
            else -> message
        }
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private data class ApiResponse(val code: Int, val body: String)

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()

        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_GOOGLE_VERIFIER = "google_code_verifier"
    }
}
