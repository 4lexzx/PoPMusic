package com.popmusic.app.ui.screens.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.PoPMusicApplication
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PoPMusicApplication).authRepository

    val session = repository.session
    val busy = repository.busy
    val lastError = repository.lastError
    val configured = repository.configured

    private var authMode by mutableStateOf(AuthMode.LOGIN)
    val mode: AuthMode get() = authMode

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var passwordConfirm by mutableStateOf("")
        private set

    var localError by mutableStateOf<String?>(null)
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    var submitting by mutableStateOf(false)
        private set

    var resending by mutableStateOf(false)
        private set

    var sendingReset by mutableStateOf(false)
        private set

    init {
        repository.clearError()
    }

    fun setMode(next: AuthMode) {
        authMode = next
        passwordConfirm = ""
        localError = null
        notice = null
        repository.clearError()
    }

    fun onEmailChange(value: String) {
        email = value
    }

    fun onPasswordChange(value: String) {
        password = value
    }

    fun onPasswordConfirmChange(value: String) {
        passwordConfirm = value
    }

    fun clearMessages() {
        localError = null
        notice = null
        repository.clearError()
    }

    fun submit() {
        if (submitting) return

        localError = null
        notice = null

        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            localError = "Escribe un correo válido."
            return
        }
        if (password.length < 6) {
            localError = "La contraseña debe tener al menos 6 caracteres."
            return
        }
        if (mode == AuthMode.REGISTER && password != passwordConfirm) {
            localError = "Las contraseñas no coinciden."
            return
        }

        viewModelScope.launch {
            submitting = true
            if (mode == AuthMode.LOGIN) {
                localError = repository.signIn(cleanEmail, password)
            } else {
                val outcome = repository.signUp(cleanEmail, password)
                localError = outcome.error
                if (outcome.needsConfirmation) {
                    notice = "Te enviamos un correo para confirmar tu cuenta. Revisa tu bandeja de entrada."
                    password = ""
                    passwordConfirm = ""
                }
            }
            submitting = false
        }
    }

    fun resendConfirmation() {
        if (resending) return

        localError = null
        notice = null

        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            localError = "Escribe un correo válido."
            return
        }

        viewModelScope.launch {
            resending = true
            val error = repository.resendConfirmation(cleanEmail)
            localError = error
            if (error == null) {
                notice = "Te enviamos otro correo de confirmación."
            }
            resending = false
        }
    }

    fun requestPasswordReset() {
        if (sendingReset) return

        localError = null
        notice = null

        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            localError = "Escribe el correo de tu cuenta para enviarte el enlace."
            return
        }

        viewModelScope.launch {
            sendingReset = true
            val error = repository.requestPasswordReset(cleanEmail)
            localError = error
            if (error == null) {
                notice = "Te enviamos un enlace para restablecer tu contraseña. Revisa tu correo."
            }
            sendingReset = false
        }
    }

    fun googleAuthUrl(): String? {
        localError = null
        notice = null
        val url = repository.buildGoogleAuthUrl()
        if (url == null) {
            localError = "Supabase no está configurado en esta app."
        }
        return url
    }
}
