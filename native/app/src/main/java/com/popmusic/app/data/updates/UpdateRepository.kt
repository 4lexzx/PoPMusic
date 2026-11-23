package com.popmusic.app.data.updates

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.popmusic.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String?,
    val sizeBytes: Long,
    val changelog: List<String>,
    val mandatory: Boolean,
)

internal object UpdateManifest {

    fun parse(raw: String): UpdateInfo? = runCatching {
        val json = JSONObject(raw)
        if (!json.optBoolean("published", false)) return@runCatching null

        val versionCode = json.optLong("versionCode", 0L)
        val versionName = json.optString("versionName").trim()
        val apkUrl = json.optString("apkUrl").trim()
        if (versionCode <= 0L || versionName.isEmpty() || apkUrl.isEmpty()) {
            return@runCatching null
        }

        val changelog = buildList {
            val entries = json.optJSONArray("changelog") ?: return@buildList
            for (index in 0 until entries.length()) {
                entries.optString(index).trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let(::add)
            }
        }

        UpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            sha256 = json.optString("sha256").trim().takeIf { it.isNotEmpty() },
            sizeBytes = json.optLong("sizeBytes", 0L).coerceAtLeast(0L),
            changelog = changelog,
            mandatory = json.optBoolean("mandatory", false),
        )
    }.getOrNull()
}

class UpdateRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val base = BuildConfig.UPDATE_MANIFEST_URL
            val separator = if (base.contains("?")) "&" else "?"
            val request = Request.Builder()
                .url("$base${separator}t=${System.currentTimeMillis()}")
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("UpdateCheck", "manifiesto HTTP ${response.code}")
                    return@use null
                }
                val raw = response.body.string()
                val manifest = UpdateManifest.parse(raw)
                if (manifest == null) {
                    Log.w("UpdateCheck", "manifiesto no valido: ${raw.take(160)}")
                    return@use null
                }
                Log.d(
                    "UpdateCheck",
                    "remoto=${manifest.versionCode}/${manifest.versionName} " +
                        "instalado=${BuildConfig.VERSION_CODE}/${BuildConfig.VERSION_NAME}",
                )
                manifest.takeIf {
                    it.versionCode > BuildConfig.VERSION_CODE.toLong() &&
                        it.apkUrl.startsWith("https://", ignoreCase = true)
                }
            }
        }.onFailure { error ->
            Log.w("UpdateCheck", "no se pudo consultar el manifiesto", error)
        }.getOrNull()
    }

    suspend fun cachedApk(info: UpdateInfo): File? = withContext(Dispatchers.IO) {
        val file = apkFile(info)
        if (!file.isFile) return@withContext null
        if (info.sizeBytes > 0L && file.length() != info.sizeBytes) {
            file.delete()
            return@withContext null
        }
        if (!info.sha256.isNullOrBlank()) {
            val validHash = runCatching {
                sha256(file).equals(info.sha256, ignoreCase = true)
            }.getOrDefault(false)
            if (!validHash) {
                file.delete()
                return@withContext null
            }
        }
        file
    }

    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = apkFile(info)
            val partial = File(directory, "${target.name}.part")
            target.delete()
            partial.delete()

            val request = Request.Builder().url(info.apkUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("No se pudo descargar la actualización (${response.code}).")
                }

                val body = response.body
                val totalBytes = body.contentLength().takeIf { it > 0L } ?: info.sizeBytes
                body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastPercent = -1

                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            if (totalBytes > 0L) {
                                val percent = ((downloaded * 100L) / totalBytes).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(
                                        (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f),
                                    )
                                }
                            }
                        }

                        if (downloaded <= 0L) error("La descarga llegó vacía.")
                        if (totalBytes > 0L && downloaded != totalBytes) {
                            error("La descarga se interrumpió. Inténtalo de nuevo.")
                        }
                    }
                }
            }

            if (!info.sha256.isNullOrBlank() &&
                !sha256(partial).equals(info.sha256, ignoreCase = true)
            ) {
                partial.delete()
                error("El archivo descargado no es válido.")
            }

            try {
                validateApk(partial, info)
            } catch (error: Exception) {
                partial.delete()
                throw error
            }

            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            onProgress(1f)
            target
        }

    private fun apkFile(info: UpdateInfo): File =
        File(File(context.cacheDir, "updates"), "popmusic-${info.versionCode}.apk")

    @Suppress("DEPRECATION")
    private fun validateApk(file: File, info: UpdateInfo) {
        val manager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

        val archive = manager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: error("El archivo descargado no es un APK válido.")
        if (archive.packageName != context.packageName) {
            error("El APK descargado no pertenece a PoP Music.")
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            archive.versionCode.toLong()
        }
        if (versionCode != info.versionCode) {
            error("La versión descargada no coincide con la actualización.")
        }

        val installed = manager.getPackageInfo(context.packageName, flags)
        val installedSignature = signatureHash(installed)
        val archiveSignature = signatureHash(archive)
        if (
            installedSignature != null &&
            archiveSignature != null &&
            !installedSignature.equals(archiveSignature, ignoreCase = true)
        ) {
            error("El APK no está firmado con la misma clave que la app instalada.")
        }
    }

    @Suppress("DEPRECATION")
    private fun signatureHash(info: PackageInfo): String? {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        } ?: return null

        val signature = signatures.firstOrNull() ?: return null
        return MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
