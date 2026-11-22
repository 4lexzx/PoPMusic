package com.popmusic.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.popmusic.app.domain.Playlist

/**
 * Exportación a Spotify sin cuenta conectada.
 *
 * Escribir en la cuenta de Spotify exige OAuth con una app registrada y, desde
 * febrero de 2026, Premium del dueño y máximo 5 cuentas. Para que funcione para
 * cualquiera se usa la misma vía que ofrece el propio Spotify: TuneMyMusic,
 * que ya tiene cuota ampliada. Generamos la lista «Artista - Título», la
 * copiamos al portapapeles y abrimos su página de texto a Spotify.
 */
private const val TUNEMYMUSIC_URL = "https://tunemymusic.com/transfer/freetext-to-spotify"

/** Ruido típico de títulos de YouTube que estorba al buscar la canción. */
private val videoNoise = Regex(
    """\s*[\[(]\s*(official\s*)?(music\s*)?(video|audio|lyrics?|hd|hq|mv|visualizer|live\s*session)\s*[\])]""",
    RegexOption.IGNORE_CASE,
)

/** Una canción por línea, en el formato que acepta TuneMyMusic. */
fun spotifyExportText(playlist: Playlist): String = buildString {
    playlist.songs.forEach { song ->
        val artist = song.artist.replace(videoNoise, "").trim()
        val title = song.title.replace(videoNoise, "").trim()
        if (title.isBlank()) return@forEach
        append(if (artist.isBlank()) title else "$artist - $title").append('\n')
    }
}

/** Copia la lista y abre TuneMyMusic para crear la playlist en Spotify. */
fun copyAndOpenSpotifyExport(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("PoP Music", text))
    Toast.makeText(context, "Lista copiada. Pégala en TuneMyMusic.", Toast.LENGTH_LONG).show()
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(TUNEMYMUSIC_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
