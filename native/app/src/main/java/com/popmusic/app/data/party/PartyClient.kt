package com.popmusic.app.data.party

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Cliente Supabase Realtime para la sala "Todos controlan".
 *
 * Protocolo Phoenix sobre WebSocket (vsn=1.0.0, frames JSON objeto):
 *  - join:  {topic, event:"phx_join", payload:{config:{broadcast…, private:false}}, ref, join_ref}
 *  - push:  {topic, event:"broadcast", payload:{event, type:"broadcast", payload}, ref, join_ref}
 *  - heart: {topic:"phoenix", event:"heartbeat", payload:{}, ref} cada 25 s
 *
 * No hay host: el último broadcast recibido gana (cola en vivo compartida).
 */
class PartyClient(
    private val supabaseUrl: String,
    private val anonKey: String,
) {

    var onStatus: ((PartyStatus) -> Unit)? = null
    var onSnapshot: ((PartySnapshot) -> Unit)? = null
    var onPresence: ((List<PartyParticipant>) -> Unit)? = null
    var onRoomMissing: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var topic = ""
    private val myId: String = UUID.randomUUID().toString().take(8)
    private var myName = ""
    private var myAvatarUrl: String? = null
    private var shouldRun = false
    private var joined = false
    private var joinRef = "1"
    private var refCounter = 1
    private var reconnectAttempt = 0
    private var heartbeatJob: java.util.concurrent.ScheduledFuture<*>? = null
    private var presenceJob: java.util.concurrent.ScheduledFuture<*>? = null
    private var joinWatchdog: java.util.concurrent.ScheduledFuture<*>? = null
    private val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
    private val participants = ConcurrentHashMap<String, PartyParticipant>()
    private var lastSnapshot: PartySnapshot? = null
    /** Solo para quien se une: exige respuesta de alguien (evita salas fantasma). */
    private var requirePresence = false

    // ————————————————— API pública —————————————————

    /** Crea o une la sala [roomCode] con nombre [displayName] y foto [avatarUrl]. */
    fun connect(
        roomCode: String,
        displayName: String,
        isCreate: Boolean,
        avatarUrl: String? = null,
    ) {
        val clean = roomCode.trim().lowercase()
        if (clean.length < 4) {
            onError?.invoke("El código debe tener al menos 4 caracteres.")
            return
        }
        disconnectInternal(notify = false)
        shouldRun = true
        myName = displayName.ifBlank { "Invitado" }
        myAvatarUrl = avatarUrl?.takeIf { it.isNotBlank() }
        topic = "realtime:party-$clean"
        requirePresence = !isCreate
        joined = false
        reconnectAttempt = 0
        participants.clear()
        onStatus?.invoke(PartyStatus.CONNECTING)
        openSocket()
    }

    /** Estado compartido: lo envía quien tiene la cola real (todos pueden). */
    fun sendSnapshot(snapshot: PartySnapshot) {
        lastSnapshot = snapshot
        broadcast("state", snapshot.toJson())
    }

    fun announcePresence() = sendHello()

    fun disconnect() = disconnectInternal(notify = true)

    fun currentParticipants(): List<PartyParticipant> =
        participants.values.sortedBy { it.name.lowercase() }

    fun currentSnapshot(): PartySnapshot? = lastSnapshot

    // ————————————————— WebSocket —————————————————

    private fun openSocket() {
        val base = supabaseUrl.trim().trimEnd('/')
            .replaceFirst("http://", "wss://")
            .replaceFirst("https://", "wss://")
        val url = "$base/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"
        val request = Request.Builder().url(url).build()
        webSocket = http.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    sendJoin()
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    ws.close(code, reason)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    handleDisconnect("closed: $reason")
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WebSocket fallo: ${t.message}")
                    handleDisconnect(t.message ?: "connection failed")
                }
            },
        )
    }

    private fun sendJoin() {
        val payload = JSONObject().apply {
            put(
                "config",
                JSONObject().apply {
                    put(
                        "broadcast",
                        JSONObject().apply {
                            put("ack", false)
                            put("self", false)
                        },
                    )
                    put("presence", JSONObject().apply { put("enabled", false) })
                    put("private", false)
                },
            )
        }
        push("phx_join", payload, useJoinRef = true)
        // Watchdog: si en 4 s nadie responde (sala inexistente/invitado solo),
        // se avisa para no dejar al usuario esperando para siempre.
        joinWatchdog = scheduler.schedule(
            {
                if (shouldRun && !joined && requirePresence) {
                    onRoomMissing?.invoke()
                }
            },
            4,
            TimeUnit.SECONDS,
        )
    }

    private fun handleMessage(text: String) {
        val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (msg.optString("event")) {
            "phx_reply" -> {
                val payload = msg.optJSONObject("payload") ?: return
                val status = payload.optString("status")
                if (status == "ok") {
                    joined = true
                    reconnectAttempt = 0
                    joinWatchdog?.cancel(false)
                    joinWatchdog = null
                    onStatus?.invoke(PartyStatus.JOINED)
                    startTimers()
                    sendHello()
                    // Comparte el estado actual con quien ya esté en la sala
                    // y, si somos los primeros, lo adoptamos al llegar.
                    lastSnapshot?.let { broadcast("state", it.toJson()) }
                } else {
                    val reason = payload.optJSONObject("response")
                        ?.optString("reason")
                        ?: payload.optString("reason")
                        ?: "error"
                    Log.w(TAG, "Join rechazado: $reason")
                    onError?.invoke(reason)
                    onStatus?.invoke(PartyStatus.ERROR)
                }
            }

            "broadcast" -> {
                val payload = msg.optJSONObject("payload") ?: return
                when (payload.optString("event")) {
                    "state" -> {
                        val snap = PartySnapshot.fromJson(
                            payload.optJSONObject("payload") ?: return,
                        ) ?: return
                        lastSnapshot = snap
                        onSnapshot?.invoke(snap)
                    }

                    "hello" -> {
                        val body = payload.optJSONObject("payload") ?: return
                        val id = body.optString("id")
                        if (id.isBlank() || id == myId) return
                        val isNew = !participants.containsKey(id)
                        participants[id] = PartyParticipant(
                            id = id,
                            name = body.optString("name").ifBlank { "Invitado" },
                            avatarUrl = body.optString("avatar").ifBlank { null },
                        )
                        if (isNew) {
                            // Presentación cruzada (termina: cada uno solo
                            // responde a quien aún no conoce) + estado actual.
                            sendHello()
                            lastSnapshot?.let { broadcast("state", it.toJson()) }
                        }
                        onPresence?.invoke(currentParticipants())
                    }

                    "bye" -> {
                        val id = payload.optJSONObject("payload")?.optString("id").orEmpty()
                        if (id.isNotBlank() && participants.remove(id) != null) {
                            onPresence?.invoke(currentParticipants())
                        }
                    }
                }
            }

            "phx_error", "phx_close" -> {
                joined = false
                if (shouldRun) handleDisconnect(msg.optString("event"))
            }
        }
    }

    private fun handleDisconnect(why: String) {
        stopTimers()
        val wasRun = shouldRun
        joined = false
        webSocket = null
        if (!wasRun) return
        onStatus?.invoke(PartyStatus.CONNECTING)
        val delayMs = RECONNECT_BACKOFF_MS[
            reconnectAttempt.coerceAtMost(RECONNECT_BACKOFF_MS.lastIndex),
        ]
        reconnectAttempt++
        Log.d(TAG, "Reconexion en ${delayMs}ms ($why)")
        scheduler.schedule(
            { if (shouldRun) openSocket() },
            delayMs,
            TimeUnit.MILLISECONDS,
        )
    }

    private fun disconnectInternal(notify: Boolean) {
        shouldRun = false
        stopTimers()
        if (joined) broadcast("bye", JSONObject().put("id", myId))
        val ws = webSocket
        webSocket = null
        joined = false
        participants.clear()
        ws?.close(1000, "leave")
        if (notify) {
            lastSnapshot = null
            onStatus?.invoke(PartyStatus.IDLE)
        }
    }

    // ————————————————— Timers —————————————————

    private fun startTimers() {
        stopTimers()
        heartbeatJob = scheduler.scheduleAtFixedRate(
            {
                if (shouldRun) {
                    push(
                        "heartbeat",
                        JSONObject(),
                        topicName = "phoenix",
                        useJoinRef = false,
                    )
                }
            },
            25, 25, TimeUnit.SECONDS,
        )
        presenceJob = scheduler.scheduleAtFixedRate(
            {
                if (!shouldRun || !joined) return@scheduleAtFixedRate
                sendHello()
                val cutoff = System.currentTimeMillis() - PRESENCE_TTL_MS
                val stale = participants.values.filter { it.lastSeen < cutoff }
                if (stale.isNotEmpty()) {
                    stale.forEach { participants.remove(it.id) }
                    onPresence?.invoke(currentParticipants())
                }
            },
            15, 15, TimeUnit.SECONDS,
        )
    }

    private fun stopTimers() {
        heartbeatJob?.cancel(false)
        presenceJob?.cancel(false)
        joinWatchdog?.cancel(false)
        heartbeatJob = null
        presenceJob = null
        joinWatchdog = null
    }

    // ————————————————— Frames —————————————————

    private fun sendHello() {
        if (!joined) return
        val body = JSONObject().put("id", myId).put("name", myName)
        myAvatarUrl?.let { body.put("avatar", it) }
        broadcast("hello", body)
    }

    private fun broadcast(event: String, payload: JSONObject) {
        if (!joined) return
        val body = JSONObject().apply {
            put("event", event)
            put("type", "broadcast")
            put("payload", payload)
        }
        push("broadcast", body, useJoinRef = true)
    }

    private fun push(
        event: String,
        payload: JSONObject,
        topicName: String = topic,
        useJoinRef: Boolean,
    ) {
        val ref = (++refCounter).toString()
        val frame = JSONObject().apply {
            put("topic", topicName)
            put("event", event)
            put("payload", payload)
            put("ref", ref)
            if (useJoinRef) put("join_ref", joinRef)
        }
        val ws = webSocket ?: return
        ws.send(frame.toString())
    }

    companion object {
        private const val TAG = "PartyClient"
        private val RECONNECT_BACKOFF_MS = longArrayOf(1_000, 2_000, 5_000, 10_000)
        private const val PRESENCE_TTL_MS = 45_000L

        /**
         * Código de sala legible (sin caracteres confusos). Tanda 12: de 5 a
         * 7 caracteres = 31^7 ≈ 27.500 millones de combinaciones, así la
         * probabilidad de chocar con una sala viva es prácticamente cero
         * (antes 31^5 ≈ 28 millones ya era baja, pero el usuario pidió
         * reforzarlo para no entrar en salas ajenas).
         */
        fun randomCode(): String {
            val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
            return (1..7).map { alphabet.random() }.joinToString("")
        }
    }
}
