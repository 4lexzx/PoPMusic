package com.popmusic.app.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.popmusic.app.R
import com.popmusic.app.ui.components.GlassPrimaryButton
import com.popmusic.app.ui.components.GlassTextButton
import com.popmusic.app.ui.components.GlassTextField
import com.popmusic.app.ui.components.PopLogo
import com.popmusic.app.ui.theme.Danger
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Surface
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.pressScale

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
) {
    val session by viewModel.session.collectAsState()
    val googleBusy by viewModel.busy.collectAsState()
    val remoteError by viewModel.lastError.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val errorText = viewModel.localError ?: remoteError

    // Tanda 9: entrada animada (fundido + desplazamiento) al abrir la pantalla.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(460),
        label = "authEnter",
    )

    LaunchedEffect(session) {
        if (session != null) onAuthenticated()
    }

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = enterProgress
                translationY = (1f - enterProgress) * 48.dp.toPx()
            }
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 32.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Volver",
                tint = Ink,
            )
        }

        Spacer(Modifier.height(6.dp))

        // Marca centrada: solo el wordmark (sin caja blanca; la caja con el
        // glifo negro sobraba y apelmazaba el logo a escala pequeña).
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PopLogo(
                modifier = Modifier.height(40.dp),
                contentDescription = "PoP Music",
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tu música, siempre contigo",
                style = MaterialTheme.typography.bodyMedium,
                color = Faint,
            )
        }

        Spacer(Modifier.height(26.dp))

        Text(
            text = when (viewModel.mode) {
                AuthMode.LOGIN -> "Inicia sesión"
                AuthMode.REGISTER -> "Crea tu cuenta"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Guarda tus playlists y sincroniza tu música en todos tus dispositivos.",
            style = MaterialTheme.typography.bodyMedium,
            color = Faint,
        )

        Spacer(Modifier.height(18.dp))

        // Acceso rápido con cuentas (primero lo social, después el correo).
        SocialButton(
            label = "Continuar con Google",
            enabled = viewModel.configured && !viewModel.submitting &&
                !viewModel.resending && !viewModel.sendingReset && !googleBusy,
            loading = googleBusy,
            onClick = {
                val url = viewModel.googleAuthUrl() ?: return@SocialButton
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            leading = {
                Image(
                    painter = painterResource(R.drawable.google_g),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.10f)),
            )
            Text(
                text = "o con tu correo",
                style = MaterialTheme.typography.labelMedium,
                color = Faint,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.10f)),
            )
        }

        Spacer(Modifier.height(14.dp))

        ModeSelector(
            mode = viewModel.mode,
            onChange = viewModel::setMode,
        )

        Spacer(Modifier.height(14.dp))

        if (!viewModel.configured) {
            LiquidGlass(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Supabase no está configurado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Falta la URL o la clave pública del proyecto. Revisa local.properties y vuelve a compilar la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Faint,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        LiquidGlass(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassTextField(
                    value = viewModel.email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = "Correo electrónico",
                    enabled = !viewModel.submitting && !viewModel.resending &&
                        !viewModel.sendingReset && !googleBusy,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            tint = Faint,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )

                GlassTextField(
                    value = viewModel.password,
                    onValueChange = viewModel::onPasswordChange,
                    placeholder = "Contraseña",
                    enabled = !viewModel.submitting && !viewModel.resending &&
                        !viewModel.sendingReset && !googleBusy,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    imeAction = if (viewModel.mode == AuthMode.REGISTER) {
                        ImeAction.Next
                    } else {
                        ImeAction.Done
                    },
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { viewModel.submit() },
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Faint,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )

                if (viewModel.mode == AuthMode.LOGIN) {
                    GlassTextButton(
                        text = if (viewModel.sendingReset) {
                            "Enviando enlace…"
                        } else {
                            "¿Olvidaste tu contraseña?"
                        },
                        onClick = viewModel::requestPasswordReset,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.configured && !viewModel.submitting &&
                            !viewModel.resending && !viewModel.sendingReset && !googleBusy,
                    )
                }

                if (viewModel.mode == AuthMode.REGISTER) {
                    GlassTextField(
                        value = viewModel.passwordConfirm,
                        onValueChange = viewModel::onPasswordConfirmChange,
                        placeholder = "Repite tu contraseña",
                        enabled = !viewModel.submitting && !viewModel.resending &&
                            !viewModel.sendingReset && !googleBusy,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = PasswordVisualTransformation(),
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.submit() },
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = Faint,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }

                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                    )
                }

                if (viewModel.notice != null) {
                    Text(
                        text = viewModel.notice.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Faint,
                    )
                }

                AuthPrimaryButton(
                    text = when {
                        viewModel.submitting -> "Un momento…"
                        viewModel.mode == AuthMode.LOGIN -> "Entrar"
                        else -> "Crear cuenta"
                    },
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.configured && !viewModel.submitting &&
                        !viewModel.resending && !viewModel.sendingReset && !googleBusy,
                )

                if (viewModel.mode == AuthMode.REGISTER) {
                    GlassTextButton(
                        text = if (viewModel.resending) {
                            "Enviando correo…"
                        } else {
                            "Reenviar correo de confirmación"
                        },
                        onClick = viewModel::resendConfirmation,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.configured && !viewModel.submitting &&
                            !viewModel.resending && !viewModel.sendingReset && !googleBusy,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = if (viewModel.mode == AuthMode.LOGIN) {
                "¿No tienes cuenta? Cambia a Crear cuenta."
            } else {
                "Te enviaremos un correo para confirmar la cuenta. Revisa también la carpeta de spam."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Botón primario del login (ítem 4): blanco sólido con texto negro
 * (paleta negra/blanca/gris; sin degradados de marca) y feedback de
 * escala al tocar.
 */
@Composable
private fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) {
                    Color.White
                } else {
                    Color.White.copy(alpha = 0.10f)
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.96f)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.Black else Faint,
        )
    }
}

@Composable
private fun ModeSelector(
    mode: AuthMode,
    onChange: (AuthMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(4.dp),
    ) {
        ModePill(
            text = "Iniciar sesión",
            selected = mode == AuthMode.LOGIN,
            onClick = { onChange(AuthMode.LOGIN) },
            modifier = Modifier.weight(1f),
        )
        ModePill(
            text = "Crear cuenta",
            selected = mode == AuthMode.REGISTER,
            onClick = { onChange(AuthMode.REGISTER) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.97f)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Surface else Faint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Botón social de login: fondo claro, logo original a la izquierda y
 * etiqueta centrada (patrón profesional tipo "Continuar con Google").
 */
@Composable
private fun SocialButton(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.97f)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (loading) "Conectando…" else label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) {
                Surface
            } else {
                Surface.copy(alpha = 0.40f)
            },
        )
    }
}
