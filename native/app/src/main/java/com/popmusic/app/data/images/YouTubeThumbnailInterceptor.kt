package com.popmusic.app.data.images

import android.util.Log
import coil3.ImageLoader
import com.popmusic.app.BuildConfig
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.crossfade
import java.util.Collections
import java.util.regex.Pattern

/**
 * Sube las fotos (portadas, miniaturas y avatares) a la máxima resolución
 * disponible, con reserva.
 *
 * Miniaturas de YouTube: los resultados de NewPipe/YouTube suelen traer
 * `hqdefault` (480x360) o `mqdefault` (320x180), que se ven pixelados en la
 * carátula grande del reproductor. Este interceptor pide en su lugar
 * `maxresdefault` (1280x720) y, si ese archivo no existe (404), repite con
 * `sddefault` (640x480); si tampoco, usa la URL original, que siempre está
 * disponible. Las URLs con firma (`?sqp=…&rs=…`) también se suben de nivel.
 *
 * Avatares/carátulas de Google (`lh3`/`yt3.googleusercontent…`): se reescriben
 * a 1024 px cuadrados (`=w1024-h1024-l90-rj`), porque YouTube Music guarda
 * las carátulas a 120 px.
 *
 * Va DELANTE del motor de Coil, así que la caché (memoria y disco) sigue
 * siendo la nativa: cada URL se cachea bajo su propia clave, y un 404 no
 * deja residuos (además se recuerda para no repetir la petición).
 */
class YouTubeThumbnailInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = when (val data = request.data) {
            is String -> data
            is coil3.Uri -> data.toString()
            else -> return chain.proceed()
        }

        for (target in upgradeTargets(url)) {
            if (target in unavailable) continue
            val result = chain.withRequest(request.newBuilder().data(target).build()).proceed()
            if (result !is ErrorResult) {
                log(url, target, result, chain)
                return result
            }
            if (BuildConfig.DEBUG) Log.w(TAG, "fallo target=$target err=${result.throwable}")
            // Solo se recuerda un fallo HTTP explícito: un corte de red puntual
            // no debe desactivar la miniatura HD durante toda la sesión.
            val message = result.throwable.message.orEmpty()
            if ("404" in message || "HTTP" in message) {
                if (unavailable.size > 500) unavailable.clear()
                unavailable += target
            }
        }
        val result = chain.proceed()
        log(url, null, result, chain)
        return result
    }

    private fun log(url: String, used: String?, result: ImageResult, chain: Interceptor.Chain) {
        if (BuildConfig.DEBUG && result is SuccessResult) {
            Log.i(
                TAG,
                "ok=${used ?: "original"} px=${result.image.width}x${result.image.height} " +
                    "pantalla=${chain.size} src=$url",
            )
        }
    }

    private companion object {
        const val TAG = "ThumbHD"

        /** i.ytimg.com/vi/<id>/<algo>.jpg[?firma] → candidatas en orden de calidad. */
        private val YT_THUMB: Pattern = Pattern.compile(
            "^https?://(?:i\\.ytimg\\.com|img\\.youtube\\.com)/vi(?:_webp)?/([A-Za-z0-9_-]{6,})/([^/?#]+)",
        )

        /** Avatares de canal de Google (YouTube): sufijo de tamaño en la ruta. */
        private val G_AVATAR: Pattern = Pattern.compile(
            "^https?://(?:lh3\\.googleusercontent\\.com|yt3\\.googleusercontent\\.com|yt3\\.ggpht\\.com)/[^?#]+",
        )

        private val SIZE_W: Pattern = Pattern.compile("=w\\d+-h\\d+(?:-[A-Za-z0-9_-]+)*")
        private val SIZE_S: Pattern = Pattern.compile("=s\\d+(?:-[A-Za-z0-9_-]+)*")

        /** URLs descartadas (404) para no repetir la petición en cada carga. */
        private val unavailable: MutableSet<String> =
            Collections.synchronizedSet(mutableSetOf())

        fun upgradeTargets(url: String): List<String> {
            val yt = YT_THUMB.matcher(url)
            if (yt.lookingAt()) {
                val id = yt.group(1)
                val current = quality(yt.group(2))
                val candidates = if (current == 0) {
                    // Formato desconocido: solo se propone 720p, por si acaso
                    // la original ya es mejor (p. ej. un fotograma a 1080p).
                    listOf("maxresdefault.jpg")
                } else {
                    listOf("maxresdefault.jpg", "sddefault.jpg")
                        .filter { quality(it) > current }
                }
                return candidates.map { "https://i.ytimg.com/vi/$id/$it" }
            }
            googleHdTargets(url)?.let { return it }
            return emptyList()
        }

        /** Calidad relativa de una miniatura; solo se propone una mejora. */
        private fun quality(file: String): Int = when (file.lowercase()) {
            "default.jpg" -> 1
            "mqdefault.jpg" -> 2
            "hqdefault.jpg" -> 3
            "sddefault.jpg" -> 4
            "hq720.jpg" -> 5
            "maxresdefault.jpg" -> 6
            else -> 0
        }

        /**
         * `…=s88` / `…=w120-h120-l90-rj` → candidatas `=w1024…` y `=w512…`.
         *
         * YouTube Music guarda la carátula de las canciones como URL de
         * `yt3.googleusercontent` a 120 px, que en la carátula de 900 px salía
         * 7× pixelada; Google sirve hasta el tamaño original si se pide
         * (verificado: `=w1024` devuelve la imagen completa), y si es más
         * pequeña la devuelve tal cual (no escala hacia arriba). Devuelve
         * `null` si la URL no es de un host de Google.
         */
        fun googleHdTargets(url: String): List<String>? {
            if (!G_AVATAR.matcher(url).lookingAt()) return null
            val base = when {
                SIZE_W.matcher(url).find() -> url.substring(
                    0,
                    SIZE_W.matcher(url).also { it.find() }.start(),
                )
                SIZE_S.matcher(url).find() -> url.substring(
                    0,
                    SIZE_S.matcher(url).also { it.find() }.start(),
                )
                else -> url
            }
            val current = currentGoogleSize(url)
            // "=w0-h0" / "=s0" piden el original: no se toca.
            if (current == 0 && (url.contains("=w0-h0") || url.contains("=s0"))) return null
            return listOf(1024, 512)
                .filter { it > current }
                .map { base + "=w$it-h$it-l90-rj" }
        }

        /** Tamaño actual de una URL de Google (0 = sin sufijo o "original"). */
        private fun currentGoogleSize(url: String): Int {
            SIZE_W.matcher(url).let { m ->
                if (m.find()) {
                    val w = m.group().removePrefix("=w").substringBefore('-').toIntOrNull()
                    if (w != null) return w
                }
            }
            SIZE_S.matcher(url).let { m ->
                if (m.find()) {
                    val s = m.group().removePrefix("=s").substringBefore('-').toIntOrNull()
                    if (s != null) return s
                }
            }
            return 0
        }
    }
}

/** Carga de imágenes de la app con el interceptor de calidad de fotos. */
fun newImageLoader(context: android.content.Context): ImageLoader =
    ImageLoader.Builder(context)
        // Fundido al aparecer cada foto (no se aplica si sale de la caché
        // de memoria, así que el scroll no parpadea).
        .crossfade(200)
        .components { add(YouTubeThumbnailInterceptor()) }
        .build()
