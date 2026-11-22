package com.popmusic.app.data.music

import android.content.Context
import android.util.Log
import com.popmusic.app.data.library.LibraryRepository
import com.popmusic.app.data.library.LibraryState
import com.popmusic.app.data.youtube.YouTubeRepository
import com.popmusic.app.domain.Playlist
import com.popmusic.app.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Estado visible de la sincronización con un servicio externo. */
data class ServiceSyncState(
    val service: MusicService,
    val connected: Boolean = false,
    val configured: Boolean = false,
    val working: Boolean = false,
    val importedPlaylists: Int = 0,
    val pushedSongs: Int = 0,
    val message: String? = null,
    val lastSyncAt: Long = 0L,
)

/**
 * Motor de sincronización entre la biblioteca local y las cuentas vinculadas.
 *
 * - **Importación**: al conectar una cuenta (o al pulsar "Importar ahora") se
 *   traen las playlists del servicio y se crean/actualizan en la biblioteca.
 * - **Tiempo real**: observa la biblioteca; cuando se crea una playlist se crea
 *   también en el servicio y cuando se añade una canción se empuja al instante.
 */
class MusicSyncManager(
    private val context: Context,
    private val library: LibraryRepository,
    private val accounts: MusicAccountRepository,
    private val oauth: MusicOAuthManager,
    private val youtube: YouTubeRepository,
) {

    val services: Map<MusicService, MusicServiceRepository>

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watcher: Job? = null

    /** playlistId -> ids de canciones ya empujadas por servicio. */
    private val pushed = mutableMapOf<String, MutableMap<MusicService, MutableSet<String>>>()

    /** Playlists que el motor ya conocía (las de arranque no se re-suben solas). */
    private val known = mutableSetOf<String>()
    private var importing = false

    /** Últimas canciones vistas por playlist (para detectar borrados locales). */
    private val previousSongs = mutableMapOf<String, Map<String, Song>>()

    /** Borrados remotos que fallaron o esperan cuenta: se reintentan solos. */
    private val pendingRemovals = mutableMapOf<String, List<Song>>()

    /**
     * Serializa TODA escritura remota (watcher, "Sincronizar…", "Exportar a…",
     * "Sincronizar todo"): sin esto, el export manual y el watcher leían
     * `pushed` a la vez, ambos veían la canción como no subida y entraban
     * 2 copias idénticas en la playlist de YouTube (bug reportado en Tanda 12).
     * Mutex NO reentrante: solo se toma en los puntos de entrada (los
     * internos `pushPlaylist`/`createRemotePlaylist` se llaman ya con él).
     */
    private val syncMutex = Mutex()

    private val _state = MutableStateFlow(
        MusicService.entries.associateWith { service ->
            ServiceSyncState(
                service = service,
                connected = accounts.isConnected(service),
                configured = oauth.isConfigured(service),
            )
        },
    )
    val state: StateFlow<Map<MusicService, ServiceSyncState>> = _state.asStateFlow()

    private val _events = MutableStateFlow<String?>(null)

    /** Último mensaje para mostrar al usuario (toast/aviso). */
    val events: StateFlow<String?> = _events.asStateFlow()

    init {
        services = mapOf(
            MusicService.YOUTUBE to YouTubeMusicService(accounts, oauth, youtube),
            MusicService.SPOTIFY to SpotifyMusicService(accounts, oauth),
        )
        // Las playlists que ya existían al arrancar se consideran sincronizadas:
        // no se crean ni se re-suben solas. Solo los cambios nuevos se empujan.
        val initial = library.state.value.playlists
        initial.forEach { playlist ->
            known += playlist.id
            playlist.external.keys.forEach { serviceName ->
                val service = runCatching { MusicService.valueOf(serviceName) }.getOrNull()
                if (service != null) markPushed(playlist.id, service, playlist.songs.map { it.id })
            }
        }
        startWatching()
    }

    fun clearEvent() {
        _events.value = null
    }

    /** Abre el navegador para vincular la cuenta; al volver importa automáticamente. */
    fun connect(service: MusicService) {
        val started = oauth.startAuthorization(service) { ok, error ->
            Log.i(TAG, "connect(${service.name}) ok=$ok error=$error")
            if (ok) {
                refreshConnection(service)
                _events.value = "${service.displayName()} conectado. Importando playlists…"
                scope.launch {
                    delay(300)
                    importFrom(service)
                }
            } else {
                _events.value = error ?: "No se pudo conectar ${service.displayName()}"
                refreshConnection(service)
            }
        }
        if (!started) {
            _events.value = "Faltan credenciales de ${service.displayName()}. " +
                "Configúralas en local.properties y recompila."
        }
    }

    fun disconnect(service: MusicService) {
        accounts.unlink(service)
        library.unlinkAllExternal(service.name)
        refreshConnection(service)
        _events.value = "${service.displayName()} desvinculado"
    }

    fun refreshConnection(service: MusicService) {
        _state.update { current ->
            current + (service to (current[service] ?: ServiceSyncState(service))
                .copy(
                    connected = accounts.isConnected(service),
                    configured = oauth.isConfigured(service),
                ))
        }
    }

    /** Importa las playlists de un servicio y las refleja en la biblioteca. */
    fun importFrom(service: MusicService) {
        val repository = services[service] ?: return
        if (!repository.isConnected()) {
            _events.value = "Conecta ${service.displayName()} primero"
            return
        }
        scope.launch {
            importing = true
            update(service) { it.copy(working = true, message = null) }
            var created = 0
            var updated = 0
            var failure: String? = null
            runCatching {
                val externalPlaylists = repository.importPlaylists()
                externalPlaylists.forEach { external ->
                    val localPlaylists = library.state.value.playlists
                    // 1) ya vinculada  2) misma playlist local sin vincular (mismo nombre)
                    val existing = localPlaylists.firstOrNull {
                        it.external[service.name] == external.externalId
                    } ?: localPlaylists.firstOrNull {
                        it.external[service.name] == null &&
                            it.name.equals(external.name, ignoreCase = true)
                    }
                    val songs = toSongs(repository.importPlaylistSongs(external.externalId))
                    if (existing == null) {
                        val playlist = library.addPlaylist(
                            name = external.name,
                            songs = songs,
                            external = mapOf(service.name to external.externalId),
                        )
                        known += playlist.id
                        markPushed(playlist.id, service, playlist.songs.map { it.id })
                        created++
                    } else {
                        if (existing.external[service.name] == null) {
                            library.linkExternal(existing.id, service.name, external.externalId)
                        }
                        markPushed(existing.id, service, existing.songs.map { it.id })
                        library.mergeSongs(existing.id, songs)
                        known += existing.id
                        updated++
                    }
                }
            }.onFailure {
                failure = it.message ?: "Error al importar"
                Log.e(TAG, "Error importando ${service.displayName()}", it)
                update(service) { state -> state.copy(message = failure) }
            }
            importing = false
            if (failure != null) {
                update(service) { state ->
                    state.copy(working = false, message = failure, lastSyncAt = System.currentTimeMillis())
                }
                _events.value = failure
                return@launch
            }
            update(service) {
                it.copy(
                    working = false,
                    importedPlaylists = created + updated,
                    lastSyncAt = System.currentTimeMillis(),
                    message = if (created + updated == 0) {
                        "No se encontraron playlists (¿creaste alguna en ${service.displayName()}?)"
                    } else {
                        "$created nuevas · $updated actualizadas"
                    },
                )
            }
            _events.value = "Importadas ${created + updated} playlists de ${service.displayName()}"
        }
    }

    /** Crea en la biblioteca una playlist que ya existe en el servicio. */
    fun importSingle(service: MusicService, external: ExternalPlaylist) {
        val repository = services[service] ?: return
        scope.launch {
            update(service) { it.copy(working = true) }
            runCatching {
                val songs = toSongs(repository.importPlaylistSongs(external.externalId))
                val playlist = library.addPlaylist(
                    name = external.name,
                    songs = songs,
                    external = mapOf(service.name to external.externalId),
                )
                known += playlist.id
                markPushed(playlist.id, service, playlist.songs.map { it.id })
            }
            update(service) { it.copy(working = false) }
        }
    }

    /** Empuja a un servicio todo lo que falte (playlists y canciones). */
    fun pushAll(service: MusicService) {
        val repository = services[service] ?: return
        if (!repository.isConnected()) {
            _events.value = "Conecta ${service.displayName()} primero"
            return
        }
        scope.launch {
            update(service) { it.copy(working = true, message = null) }
            var count = 0
            runCatching {
                syncMutex.withLock {
                    library.state.value.playlists.forEach { playlist ->
                        count += pushPlaylist(repository, playlist)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error sincronizando ${service.displayName()}", it)
            }
            update(service) {
                it.copy(
                    working = false,
                    pushedSongs = count,
                    lastSyncAt = System.currentTimeMillis(),
                    message = if (count == 0) "Todo estaba sincronizado" else "$count canciones subidas",
                )
            }
            _events.value = "Sincronizado con ${service.displayName()} ($count canciones)"
        }
    }

    /**
     * Cambia el nombre de una playlist y lo propaga a las plataformas donde
     * está vinculada (ítem 1): YouTube y Spotify. Las que fallen se dejan
     * como están (el nombre local sí se guarda siempre).
     */
    fun renamePlaylist(playlistId: String, newName: String) {
        val playlist = library.state.value.playlists.firstOrNull { it.id == playlistId }
            ?: return
        scope.launch {
            syncMutex.withLock {
                var updated = 0
                playlist.external.forEach { (serviceName, externalId) ->
                    val service = runCatching { MusicService.valueOf(serviceName) }.getOrNull()
                        ?: return@forEach
                    val repository = services[service] ?: return@forEach
                    if (!repository.isConnected()) return@forEach
                    val ok = runCatching {
                        repository.renamePlaylist(externalId, newName)
                    }.getOrDefault(false)
                    if (ok) {
                        updated++
                    } else {
                        Log.w(
                            TAG,
                            "No se pudo renombrar «${playlist.name}» en " +
                                service.displayName(),
                        )
                    }
                }
                _events.value = when {
                    updated > 0 -> "Nombre actualizado en $updated plataforma(s)"
                    playlist.external.isNotEmpty() -> "Nombre guardado solo en el teléfono"
                    else -> null
                }
            }
        }
    }

    /**
     * Sube una playlist concreta a un servicio (export manual desde la
     * biblioteca, ítem "exportar a YouTube/Spotify"). Crea la playlist en el
     * servicio si aún no está vinculada y añade lo que falte.
     */
    fun exportPlaylist(service: MusicService, playlistId: String) {
        val repository = services[service] ?: return
        if (!repository.isConnected()) {
            _events.value = "Conecta ${service.displayName()} primero (Ajustes → Cuentas)"
            return
        }
        val playlist = library.state.value.playlists.firstOrNull { it.id == playlistId } ?: return
        scope.launch {
            update(service) { it.copy(working = true, message = null) }
            var count = 0
            runCatching {
                count += syncMutex.withLock { pushPlaylist(repository, playlist) }
            }.onFailure {
                Log.e(TAG, "Error exportando «${playlist.name}» a ${service.displayName()}", it)
            }
            update(service) {
                it.copy(
                    working = false,
                    pushedSongs = count,
                    lastSyncAt = System.currentTimeMillis(),
                    message = if (count == 0) "Ya estaba sincronizada" else "$count canciones subidas",
                )
            }
            _events.value = if (count == 0) {
                "«${playlist.name}» ya está en ${service.displayName()}"
            } else {
                "Exportada «${playlist.name}» a ${service.displayName()} ($count canciones)"
            }
        }
    }

    /**
     * Observa la biblioteca para replicar en tiempo real las playlists nuevas y
     * las canciones añadidas. Un pequeño retardo agrupa ráfagas de cambios.
     */
    private fun startWatching() {
        if (watcher != null) return
        watcher = scope.launch {
            library.state.collect { current ->
                if (importing) return@collect
                delay(400)
                if (importing) return@collect
                syncMutex.withLock { processLibrary(current) }
            }
        }
    }

    /**
     * Aplica una emisión de la biblioteca. Corre SIEMPRE con [syncMutex]
     * tomado: el export manual ("Sincronizar…"/"Exportar a…") y `pushAll`
     * usan el mismo candado, así que aquí `pushed` ya está al día y no se
     * suben canciones duplicadas (bug: playlist de 1 canción → 2 iguales
     * en YouTube).
     */
    private suspend fun processLibrary(current: LibraryState) {
        // Olvida playlists que ya no existen en la biblioteca.
        val liveIds = current.playlists.map { it.id }.toSet()
        previousSongs.keys.retainAll(liveIds)
        pendingRemovals.keys.retainAll(liveIds)
        for (playlist in current.playlists) {
            // Primera vez que vemos esta playlist: si es nueva de verdad
            // (creada durante la sesión) se replica; si ya existía, no.
            val justCreated = known.add(playlist.id)
            val currentIds = playlist.songs.map { it.id }.toSet()

            // Borrados en tiempo real: lo que había en la emisión anterior
            // (o lo que falló en pasadas) y ya no está en la local.
            val removed = LinkedHashMap<String, Song>()
            previousSongs[playlist.id]?.values?.forEach { song ->
                if (song.id !in currentIds) removed[song.id] = song
            }
            pendingRemovals[playlist.id]?.forEach { song ->
                if (song.id !in currentIds) removed[song.id] = song
            }
            previousSongs[playlist.id] = playlist.songs.associateBy { it.id }
            var keepPending = false

            for ((service, repository) in services) {
                val externalId = playlist.external[service.name]
                if (!repository.isConnected()) {
                    // Cuenta caída con borrados pendientes: se reintenta
                    // en cuanto vuelva a haber cambios (o al reconectar).
                    if (removed.isNotEmpty() && externalId != null) keepPending = true
                    continue
                }
                if (justCreated) {
                    // Ítem 3: respeta la elección al crear la
                    // playlist: "NONE" = no subirla; "YOUTUBE" o
                    // "SPOTIFY" = solo esa; null = automático.
                    val target = playlist.syncTarget
                    val wanted = target == null || target == service.name
                    if (wanted && playlist.external[service.name] == null) {
                        createRemotePlaylist(repository, playlist)
                    }
                }

                // Saca de la remota lo que se borró en la app (solo lo
                // que nosotros subimos; un fallo se reintenta después).
                if (removed.isNotEmpty() && externalId != null) {
                    val remotePushed = pushed[playlist.id]?.get(service)
                    var failed = false
                    for (song in removed.values) {
                        if (remotePushed?.contains(song.id) != true) continue
                        val ok = runCatching {
                            repository.removeFromPlaylist(
                                externalId,
                                song.toExternal(service),
                            )
                        }.getOrDefault(false)
                        if (ok) {
                            markUnpushed(playlist.id, service, song.id)
                        } else {
                            failed = true
                            Log.w(
                                TAG,
                                "No se pudo borrar «${song.title}» de " +
                                    "${service.displayName()}",
                            )
                        }
                    }
                    if (failed) keepPending = true
                }

                val alreadyPushed = pushed[playlist.id]?.get(service)
                val newSongs = playlist.songs.filter { song ->
                    alreadyPushed?.contains(song.id) != true
                }
                if (newSongs.isEmpty()) continue

                val pushedExternalId = playlist.external[service.name] ?: continue
                newSongs.forEach { song ->
                    val ok = runCatching {
                        repository.addToPlaylist(
                            pushedExternalId,
                            song.toExternal(service),
                        )
                    }.getOrDefault(false)
                    if (ok) {
                        markPushed(playlist.id, service, listOf(song.id))
                    }
                }
            }

            if (keepPending && removed.isNotEmpty()) {
                pendingRemovals[playlist.id] = removed.values.toList()
            } else {
                pendingRemovals.remove(playlist.id)
            }
        }
    }

    private suspend fun createRemotePlaylist(
        repository: MusicServiceRepository,
        playlist: Playlist,
    ): String? {
        val created = runCatching { repository.createPlaylist(playlist.name) }.getOrNull()
            ?: return null
        library.linkExternal(playlist.id, repository.service.name, created)
        markPushed(playlist.id, repository.service, emptyList())
        if (playlist.syncTarget != null) {
            _events.value = "«${playlist.name}» subida a ${repository.service.displayName()}"
        }
        return created
    }

    private suspend fun pushPlaylist(
        repository: MusicServiceRepository,
        playlist: Playlist,
    ): Int {
        val externalId = playlist.external[repository.service.name]
            ?: repository.createPlaylist(playlist.name)?.also {
                library.linkExternal(playlist.id, repository.service.name, it)
            }
            ?: return 0
        var count = 0
        val already = pushed[playlist.id]?.get(repository.service)
        playlist.songs.forEach { song ->
            if (already?.contains(song.id) == true) return@forEach
            val ok = runCatching {
                repository.addToPlaylist(externalId, song.toExternal(repository.service))
            }.getOrDefault(false)
            if (ok) {
                markPushed(playlist.id, repository.service, listOf(song.id))
                count++
            }
        }
        return count
    }

    private fun markPushed(playlistId: String, service: MusicService, songIds: List<String>) {
        val perPlaylist = pushed.getOrPut(playlistId) { mutableMapOf() }
        val set = perPlaylist.getOrPut(service) { mutableSetOf() }
        set.addAll(songIds)
    }

    /** Olvida que una canción estaba subida (tras borrarla de la remota). */
    private fun markUnpushed(playlistId: String, service: MusicService, songId: String) {
        pushed[playlistId]?.get(service)?.remove(songId)
    }

    private fun Song.toExternal(service: MusicService): ExternalSong = ExternalSong(
        service = service,
        externalId = id,
        title = title,
        artist = artist,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl,
        playbackId = id,
    )

    /**
     * Convierte las canciones externas en canciones reproducibles. Las de YouTube
     * ya traen `videoId`; las de Spotify se resuelven contra YouTube (con caché).
     */
    private suspend fun toSongs(external: List<ExternalSong>): List<Song> {
        if (external.isEmpty()) return emptyList()
        val semaphore = Semaphore(4)
        val songs = mutableListOf<Song>()
        external.map { item ->
            scope.launch {
                semaphore.withPermit {
                    val playbackId = item.playbackId ?: resolvePlaybackId(item)
                    if (playbackId != null) {
                        synchronized(songs) {
                            songs += Song(
                                id = playbackId,
                                title = item.title,
                                artist = item.artist,
                                durationSeconds = item.durationSeconds,
                                thumbnailUrl = item.thumbnailUrl,
                            )
                        }
                    }
                }
            }
        }.forEach { it.join() }
        return songs.distinctBy { it.id }
    }

    private suspend fun resolvePlaybackId(item: ExternalSong): String? =
        withContext(Dispatchers.IO) {
            synchronized(cache) { cache[item.externalId] }?.let { return@withContext it }
            val query = "${item.title} ${item.artist}".trim()
            val found = runCatching {
                youtube.search(query).firstOrNull { it.durationSeconds > 0 }?.id
            }.getOrNull()
            if (found != null) synchronized(cache) { cache[item.externalId] = found }
            found
        }

    private fun update(service: MusicService, block: (ServiceSyncState) -> ServiceSyncState) {
        _state.update { current ->
            val existing = current[service] ?: ServiceSyncState(service)
            current + (service to block(existing))
        }
    }

    private val cache = mutableMapOf<String, String>()

    private companion object {
        const val TAG = "MusicSync"
    }
}
