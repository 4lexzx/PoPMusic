package com.popmusic.app.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.popmusic.app.data.auth.AuthRepository
import com.popmusic.app.ui.components.GlassDialog
import com.popmusic.app.ui.components.GlassDialogActions
import com.popmusic.app.ui.components.GlassDialogHeader
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import kotlinx.coroutines.launch

@Composable
fun PasswordRecoveryDialog(authRepository: AuthRepository) {
    val recovery by authRepository.recoveryMode.collectAsState()
    val session by authRepository.session.collectAsState()

    if (!recovery || session == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    GlassDialog(
        onDismiss = {
            if (!busy) authRepository.cancelRecovery()
        },
        dismissEnabled = !busy,
    ) {
        GlassDialogHeader(
            title = "Nueva contraseña",
            subtitle = "Verificamos tu enlace. Elige una contraseña nueva para tu cuenta.",
        )

        Spacer(Modifier.height(16.dp))

        GlassTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Nueva contraseña",
            enabled = !busy,
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            imeAction = ImeAction.Next,
        )

        Spacer(Modifier.height(10.dp))

        GlassTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            placeholder = "Repite la contraseña",
            enabled = !busy,
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            imeAction = ImeAction.Done,
        )

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = Danger,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Mínimo 6 caracteres.",
            style = MaterialTheme.typography.bodySmall,
            color = Faint,
        )

        Spacer(Modifier.height(14.dp))

        GlassDialogActions {
            GlassTextButton(
                text = "Cancelar",
                onClick = { authRepository.cancelRecovery() },
                modifier = Modifier.weight(1f),
                enabled = !busy,
            )
            GlassPrimaryButton(
                text = if (busy) "Guardando…" else "Guardar",
                onClick = {
                    error = null
                    when {
                        password.length < 6 -> {
                            error = "La contraseña debe tener al menos 6 caracteres."
                        }
                        password != confirmation -> {
                            error = "Las contraseñas no coinciden."
                        }
                        else -> scope.launch {
                            busy = true
                            val result = authRepository.updatePassword(password)
                            busy = false
                            if (result == null) {
                                Toast.makeText(
                                    context,
                                    "Contraseña actualizada.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                error = result
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !busy,
            )
        }
    }
}
