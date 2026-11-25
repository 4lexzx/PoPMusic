package com.popmusic.app.data.youtube

import com.popmusic.app.domain.Song
import org.json.JSONArray
import org.json.JSONObject

/** Lee la cola de reproducción que devuelve el endpoint `next` de YouTube Music. */
internal object MusicRadioParser {

    fun parse(root: JSONObject): YouTubeRepository.MusicRadioPage {
        val panel = root.optJSONObject("continuationContents")
            ?.optJSONObject("playlistPanelContinuation")
            ?: findObject(root, "playlistPanelRenderer")
            ?: return YouTubeRepository.MusicRadioPage()
        val songs = panel.optJSONArray("contents").objects().mapNotNull(::songFrom)
        val continuation = panel.optJSONArray("continuations").objects()
            .firstNotNullOfOrNull { item ->
                item.optJSONObject("nextRadioContinuationData")
                    ?: item.optJSONObject("nextContinuationData")
            }
            ?.optString("continuation")
            ?.takeIf { it.isNotBlank() }
        return YouTubeRepository.MusicRadioPage(songs.distinctBy { it.id }, continuation)
    }

    private fun songFrom(item: JSONObject): Song? {
        val wrapper = item.optJSONObject("playlistPanelVideoWrapperRenderer")
        val options = if (wrapper == null) {
            listOfNotNull(item.optJSONObject("playlistPanelVideoRenderer"))
        } else {
            // El mismo tema puede venir como videoclip y como audio: vale el audio.
            listOfNotNull(
                wrapper.optJSONObject("primaryRenderer")
                    ?.optJSONObject("playlistPanelVideoRenderer"),
            ) + wrapper.optJSONArray("counterpart").objects().mapNotNull {
                it.optJSONObject("counterpartRenderer")
                    ?.optJSONObject("playlistPanelVideoRenderer")
            }
        }
        val video = options.firstOrNull { videoType(it) == AUDIO_TYPE } ?: return null
        val id = video.optString("videoId").takeIf { it.isNotBlank() } ?: return null
        val title = text(video.optJSONObject("title")).takeIf { it.isNotBlank() } ?: return null
        // El primer tramo es el artista principal; después van colaboradores,
        // álbum y año.
        val artist = video.optJSONObject("longBylineText")
            ?.optJSONArray("runs").objects()
            .firstOrNull()?.optString("text").orEmpty().trim()
        val thumbnail = video.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails").objects()
            .maxByOrNull { it.optInt("height") }
            ?.optString("url")?.takeIf { it.isNotBlank() }
        return Song(
            id = id,
            title = title,
            artist = artist,
            durationSeconds = seconds(text(video.optJSONObject("lengthText"))),
            thumbnailUrl = thumbnail ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg",
        )
    }

    /** Artista, álbum y año del primer tema (la propia semilla). */
    fun meta(root: JSONObject, videoId: String): YouTubeRepository.MusicMeta? {
        val panel = findObject(root, "playlistPanelRenderer") ?: return null
        val video = panel.optJSONArray("contents").objects().firstNotNullOfOrNull { item ->
            val direct = item.optJSONObject("playlistPanelVideoRenderer")
                ?: item.optJSONObject("playlistPanelVideoWrapperRenderer")
                    ?.optJSONObject("primaryRenderer")
                    ?.optJSONObject("playlistPanelVideoRenderer")
            direct?.takeIf { it.optString("videoId") == videoId }
        } ?: return null
        // Solo el audio oficial trae "artista • álbum • año"; un vídeo trae visitas.
        if (videoType(video) != AUDIO_TYPE) return null
        val parts = text(video.optJSONObject("longBylineText")).split(" • ").map { it.trim() }
        if (parts.size < 3) return null
        val artist = video.optJSONObject("longBylineText")?.optJSONArray("runs").objects()
            .firstOrNull()?.optString("text").orEmpty().trim()
        return YouTubeRepository.MusicMeta(
            artist = artist,
            album = parts[1],
            year = parts.last().takeIf { it.matches(Regex("(19|20)\\d{2}")) },
        )
    }

    private fun videoType(video: JSONObject): String =
        video.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optJSONObject("watchEndpointMusicSupportedConfigs")
            ?.optJSONObject("watchEndpointMusicConfig")
            ?.optString("musicVideoType").orEmpty()

    private fun text(node: JSONObject?): String =
        node?.optJSONArray("runs").objects().joinToString("") { it.optString("text") }.trim()

    private fun seconds(length: String): Long {
        val parts = length.split(':').map { it.trim().toLongOrNull() ?: return 0L }
        return parts.fold(0L) { total, part -> total * 60 + part }
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun findObject(node: Any?, key: String): JSONObject? {
        when (node) {
            is JSONObject -> {
                node.optJSONObject(key)?.let { return it }
                for (name in node.keys()) {
                    findObject(node.opt(name), key)?.let { return it }
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                findObject(node.opt(i), key)?.let { return it }
            }
        }
        return null
    }

    private const val AUDIO_TYPE = "MUSIC_VIDEO_TYPE_ATV"
}
