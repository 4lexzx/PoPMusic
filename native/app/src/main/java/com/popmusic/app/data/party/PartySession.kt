package com.popmusic.app.data.party

import com.popmusic.app.BuildConfig
import com.popmusic.app.PoPMusicApplication
import com.popmusic.app.domain.Song
import com.popmusic.app.player.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la sala para la UI (conexión + cola + participantes + errores). */
data class PartyUiState(
    val status: PartyStatus = PartyStatus.IDLE,
    /** Código de la sala activa (en minúsculas para el topic, visible en mayúsculas). */
    val code: String = "",
    /** Participantes de la sala (yo incluido). */
    val participants: List<PartyParticipant> = emptyList(),
    val snapshot: PartySnapshot? = null,
    val error: String? = null,
    /** true cuando el código unido no corresponde a ninguna sala con gente. */
    val roomMissing: Boolean = false,
    /**
     * true mientras haya sala abierta (aunque esté reconectando): la UI
     * muestra la sala y solo el botón "Salir" la cierra.
     */
    val inRoom: Boolean = false,
    /** Ids recién añadidos a la cola (para el check temporizado). */
    val justAdded: Set<String> = emptySet(),
    // — Búsqueda para añadir (la rellena el ViewModel de la pantalla) —
    val searchQuery: String = "",
    val searching: Boolean = false,
    val searchResults: List<Song> = emptyList(),
)

/**
 * Sesión de sala "Todos controlan" a nivel de aplicación (tanda 9).
 *
 * La sala vive mientras la app esté viva: salir de la pantalla NO la cierra,
 * solo lo hace el botón "Salir" o cerrar la app. Cualquier acción local
 * (play/pausa/saltar/añadir) se comparte por broadcast y cualquier snapshot
 * remoto se aplica al reproductor local. No hay host: gana el último.
 */
class PartySession(private val app: PoPMusicApplication) {

    private val connection: PlayerConnection = app.playerConnection
    private val client = PartyClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        anonKey = BuildConfig.SUPABASE_ANON_KEY,
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PartyUiState())
    val state: StateFlow<PartyUiState> = _state.asStateFlow()

    private var addClearJob: Job? = null
    /** Epoch ms hasta el que los cambios locales NO se reenvían (evita bucles). */
    private var applyingUntil = 0L
    private var lastSentAt = 0L
    /** Epoch ms en el que se conectó la sala (espera inicial al snapshot). */
    private var joinedAt = 0L

    init {
        client.onStatus = { status ->
            _state.value = _state.value.copy(
                status = status,
                error = if (status == PartyStatus.ERROR) _state.value.error else null,
                roomMissing = false,
                // La sala sigue abierta aunque la conexión parpadee
                // (CONNECTING de reconexión): solo la sale el botón
                // "Salir" (IDLE) o un error real de entrada (ERROR).
                inRoom = when (status) {
                    PartyStatus.JOINED -> true
                    PartyStatus.ERROR, PartyStatus.IDLE -> false
                    else -> _state.value.inRoom
                },
            )
            // Al entrar, publica la lista conmigo incluido (si estoy solo,
            // no llegará ningún "hello" ajeno y quedaría en 0).
            if (status == PartyStatus.JOINED) {
                publishParticipants(client.currentParticipants())
            }
        }
        client.onSnapshot = { snapshot -> scope.launch { applyRemote(snapshot) } }
        client.onPresence = { participants -> publishParticipants(participants) }
        client.onRoomMissing = {
            _state.value = _state.value.copy(
                roomMissing = true,
                status = PartyStatus.ERROR,
                error = "No se encontró esa sala. Revisa el código.",
                inRoom = false,
            )
        }
        client.onError = { message ->
            // Solo avisa: quien expulsa de la sala es onStatus(ERROR) (rechazo
            // del join). Un error puntual no debe sacar de una sala abierta.
            _state.value = _state.value.copy(error = message, status = PartyStatus.ERROR)
        }

        // Cambios locales (play/pausa, cola de la radio, adds desde otras
        // pantallas) → se comparten con la sala mientras esté abierta.
        scope.launch {
            connection.state.collect { playerState ->
                val snapshot = _state.value.snapshot
                if (_state.value.status != PartyStatus.JOINED) return@collect
                if (System.currentTimeMillis() < applyingUntil) return@collect
                if (!playerState.hasMedia) return@collect
                val localQueue = connection.currentQueue()
                if (localQueue.isEmpty()) return@collect
                if (snapshot == null) {
                    // Sala aún sin estado: comparte mi música (con calma,
                    // para dar tiempo a recibir el snapshot de los demás).
                    if (System.currentTimeMillis() - joinedAt < 2_500) return@collect
                    if (System.currentTimeMillis() - lastSentAt < 600) return@collect
                    sendCurrent(playerState.isPlaying)
                    return@collect
                }
                val sameQueue = localQueue.map { it.id } == snapshot.queue.map { it.id }
                val playingChanged = playerState.isPlaying != snapshot.playing
                if (sameQueue && !playingChanged) return@collect
                if (System.currentTimeMillis() - lastSentAt < 600) return@collect
                sendCurrent(playerState.isPlaying)
            }
        }
    }

    // ————————————————— Acciones de sala —————————————————

    fun createRoom() {
        val code = PartyClient.randomCode()
        joinedAt = System.currentTimeMillis()
        _state.value = _state.value.copy(
            code = code,
            roomMissing = false,
            error = null,
            // Sala nueva = SIN canciones: se limpia el snapshot anterior (si
            // no, la cola de la sala vieja seguía visible en la nueva).
            snapshot = null,
            participants = emptyList(),
            justAdded = emptySet(),
        )
        client.connect(
            code,
            displayName(),
            isCreate = true,
            avatarUrl = myAvatarUrl(),
        )
        // La cola de la sala ES la cola del reproductor local: se vacía
        // también para que la sala nueva arranque de cero (petición del
        // usuario: "al crear una nueva sala aún se quedan las canciones
        // anteriores"). El watcher ya no publica nada con la cola vacía.
        if (connection.currentQueue().isNotEmpty()) {
            connection.clearQueue()
        }
    }

    fun joinRoom(code: String) {
        val clean = code.trim()
        if (clean.length < 4) {
            _state.value = _state.value.copy(error = "El código debe tener al menos 4 caracteres.")
            return
        }
        _state.value = _state.value.copy(
            code = clean.lowercase(),
            roomMissing = false,
            error = null,
            snapshot = null,
            participants = emptyList(),
        )
        joinedAt = System.currentTimeMillis()
        client.connect(
            clean,
            displayName(),
            isCreate = false,
            avatarUrl = myAvatarUrl(),
        )
    }

    fun leave() {
        client.disconnect()
        _state.value = _state.value.copy(
            status = PartyStatus.IDLE,
            code = "",
            participants = emptyList(),
            snapshot = null,
            error = null,
            roomMissing = false,
            justAdded = emptySet(),
            inRoom = false,
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null, roomMissing = false)
        if (_state.value.code.isBlank()) {
            _state.value = _state.value.copy(status = PartyStatus.IDLE)
        }
    }

    /** Actualiza el código escrito sin intentar conectar todavía. */
    fun onCodeChange(code: String) {
        if (code.length > 8) return
        _state.value = _state.value.copy(code = code, error = null, roomMissing = false)
    }

    // ————————————————— Controles compartidos —————————————————

    fun togglePlay() {
        val playing = connection.state.value.isPlaying
        connection.setPlaying(!playing)
        sendCurrent(!playing)
    }

    fun next() {
        connection.next()
        scope.launch {
            delay(150)
            sendCurrent(connection.state.value.isPlaying)
        }
    }

    fun previous() {
        connection.previous()
        scope.launch {
            delay(150)
            sendCurrent(connection.state.value.isPlaying)
        }
    }

    /** Salta a una canción de la cola compartida. */
    fun jumpTo(index: Int) {
        connection.skipTo(index)
        applyingUntil = System.currentTimeMillis() + 1_500
        scope.launch {
            delay(250)
            sendCurrent(connection.state.value.isPlaying)
        }
    }

    /** Añade canciones (de la sala o de mi biblioteca) a la cola compartida. */
    fun addSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        val local = connection.currentQueue()
        val snapshot = _state.value.snapshot
        val base = local.ifEmpty { snapshot?.queue ?: emptyList() }
        val baseIndex = when {
            // Sala nueva sin nada (base vacía): la canción entra en la 0.
            // (coerceIn(0, -1) lanzaba IllegalArgumentException y crasheaba
            // al añadir la primera canción a una sala recién creada.)
            base.isEmpty() -> 0
            local.isNotEmpty() -> (connection.currentIndex()).coerceIn(0, base.lastIndex)
            else -> (snapshot?.index ?: 0).coerceIn(0, base.lastIndex)
        }
        val newQueue = base.toMutableList().apply { addAll(songs) }
        val wasPlaying = connection.state.value.isPlaying ||
            (local.isEmpty() && snapshot?.playing == true)

        applyingUntil = System.currentTimeMillis() + 2_000
        if (local.isEmpty()) {
            connection.play(newQueue, baseIndex)
            if (!wasPlaying) {
                scope.launch { delay(400); connection.setPlaying(false) }
            }
        } else {
            connection.addToQueue(songs)
        }
        scope.launch {
            delay(500)
            sendCurrent(connection.state.value.isPlaying)
        }
        markJustAdded(songs.map { it.id })
    }

    // ————————————————— Interno —————————————————

    /** Participantes (los demás + yo) para mostrar "N escuchando". */
    private fun publishParticipants(others: List<PartyParticipant>) {
        val withMe = if (_state.value.status == PartyStatus.JOINED) {
            listOf(
                PartyParticipant(
                    id = MY_ID,
                    name = displayName(),
                    avatarUrl = myAvatarUrl(),
                ),
            ) + others
        } else {
            others
        }
        _state.value = _state.value.copy(participants = withMe)
    }

    private fun sendCurrent(playing: Boolean) {
        val queue = connection.currentQueue()
        if (queue.isEmpty()) return
        val snapshot = PartySnapshot(
            queue = queue,
            index = connection.currentIndex().coerceIn(0, queue.lastIndex),
            playing = playing,
            positionMs = connection.currentPositionMs(),
            updatedAt = System.currentTimeMillis(),
            updatedBy = displayName(),
        )
        lastSentAt = System.currentTimeMillis()
        _state.value = _state.value.copy(snapshot = snapshot)
        client.sendSnapshot(snapshot)
    }

    /** Aplica un snapshot remoto al reproductor local (solo si aporta cola). */
    private fun applyRemote(snapshot: PartySnapshot) {
        _state.value = _state.value.copy(snapshot = snapshot)
        if (snapshot.queue.isEmpty()) return
        if (_state.value.status != PartyStatus.JOINED) return

        applyingUntil = System.currentTimeMillis() + 1_500
        val local = connection.currentQueue()
        val localIds = local.map { it.id }
        val remoteIds = snapshot.queue.map { it.id }
        val index = snapshot.index.coerceIn(0, snapshot.queue.lastIndex)
        val estimated = snapshot.positionMs +
            if (snapshot.playing) {
                (System.currentTimeMillis() - snapshot.updatedAt).coerceAtLeast(0L)
            } else {
                0L
            }

        when {
            localIds != remoteIds -> {
                connection.play(snapshot.queue, index)
                scope.launch {
                    delay(500)
                    if (estimated > 1_000) connection.seekTo(estimated)
                    connection.setPlaying(snapshot.playing)
                }
            }

            else -> {
                val current = connection.currentIndex()
                if (current != index && index in 0 until snapshot.queue.size) {
                    connection.skipTo(index)
                }
                val drift = kotlin.math.abs(connection.currentPositionMs() - estimated)
                if (estimated > 1_000 && drift > 4_000) {
                    connection.seekTo(estimated)
                }
                if (connection.state.value.isPlaying != snapshot.playing) {
                    connection.setPlaying(snapshot.playing)
                }
            }
        }
    }

    private fun markJustAdded(ids: List<String>) {
        addClearJob?.cancel()
        _state.value = _state.value.copy(justAdded = _state.value.justAdded + ids)
        addClearJob = scope.launch {
            delay(1_600)
            _state.value = _state.value.copy(justAdded = emptySet())
        }
    }

    fun displayName(): String {
        val email = app.authRepository.session.value?.email.orEmpty()
        val raw = email.substringBefore("@").ifBlank { "Invitado" }
        return raw.take(16)
    }

    /** Foto de la cuenta actual, para mostrarla en las burbujas de la sala. */
    private fun myAvatarUrl(): String? =
        app.authRepository.session.value?.avatarUrl?.takeIf { it.isNotBlank() }

    companion object {
        /** Id sintético de "yo" en la lista de participantes. */
        private const val MY_ID = "me"
    }
}
