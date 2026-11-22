package com.popmusic.app.ui.updates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popmusic.app.data.updates.UpdateInfo
import com.popmusic.app.data.updates.UpdateInstaller
import com.popmusic.app.data.updates.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class UpdatePhase {
    Hidden,
    Available,
    Downloading,
    Ready,
    NeedsPermission,
    Error,
}

data class UpdateUiState(
    val info: UpdateInfo? = null,
    val phase: UpdatePhase = UpdatePhase.Hidden,
    val progress: Float = 0f,
    val file: File? = null,
    val error: String? = null,
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UpdateRepository(application)
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var checked = false
    private var installLaunched = false

    init {
        check()
    }

    private fun check() {
        if (checked) return
        checked = true

        viewModelScope.launch {
            val info = repository.checkForUpdate() ?: return@launch
            val cached = runCatching { repository.cachedApk(info) }.getOrNull()
            _state.value = UpdateUiState(
                info = info,
                phase = if (cached != null) UpdatePhase.Ready else UpdatePhase.Available,
                file = cached,
            )
        }
    }

    /** Vuelve a consultar el manifiesto (botón "Buscar actualizaciones"). */
    fun checkAgain(onResult: (UpdateInfo?) -> Unit = {}) {
        if (_state.value.phase == UpdatePhase.Downloading) {
            onResult(_state.value.info)
            return
        }
        checked = true
        viewModelScope.launch {
            val info = repository.checkForUpdate()
            if (info == null) {
                onResult(null)
                return@launch
            }
            val cached = runCatching { repository.cachedApk(info) }.getOrNull()
            _state.value = UpdateUiState(
                info = info,
                phase = if (cached != null) UpdatePhase.Ready else UpdatePhase.Available,
                file = cached,
            )
            onResult(info)
        }
    }

    fun download() {
        val info = _state.value.info ?: return
        if (_state.value.phase == UpdatePhase.Downloading) return

        _state.update {
            it.copy(
                phase = UpdatePhase.Downloading,
                progress = 0f,
                error = null,
                file = null,
            )
        }

        viewModelScope.launch {
            try {
                val file = repository.download(info) { progress ->
                    _state.update { current ->
                        if (current.phase == UpdatePhase.Downloading) {
                            current.copy(progress = progress)
                        } else {
                            current
                        }
                    }
                }
                _state.update {
                    it.copy(
                        phase = UpdatePhase.Ready,
                        progress = 1f,
                        file = file,
                        error = null,
                    )
                }
                install()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        phase = UpdatePhase.Error,
                        file = null,
                        error = error.message
                            ?: "No se pudo descargar la actualización.",
                    )
                }
            }
        }
    }

    fun install() {
        val current = _state.value
        val file = current.file ?: return
        val context = getApplication<Application>()

        if (!UpdateInstaller.canInstall(context)) {
            _state.update { it.copy(phase = UpdatePhase.NeedsPermission) }
            return
        }

        installLaunched = true
        val launched = UpdateInstaller.install(context, file)
        if (!launched) {
            installLaunched = false
            _state.update {
                it.copy(
                    phase = UpdatePhase.NeedsPermission,
                    error = "No se pudo abrir el instalador de Android.",
                )
            }
        }
    }

    fun onReturnedFromSettings() {
        val current = _state.value
        if (current.phase != UpdatePhase.NeedsPermission || current.file == null) return
        if (installLaunched) return
        if (!UpdateInstaller.canInstall(getApplication())) return
        install()
    }

    fun dismiss() {
        if (_state.value.phase == UpdatePhase.Downloading) return
        _state.value = UpdateUiState()
    }
}
