/** En Electron hay yt-dlp y se puede sonar la canción completa. */
export const hasFullAudio = !!window.popApi?.resolveAudio

/**
 * Resuelve el audio completo de una canción con yt-dlp (proceso principal).
 * `fresh` ignora la caché, para cuando la URL anterior caducó.
 */
export async function resolveAudio(artist: string, title: string, fresh = false): Promise<string | null> {
  if (!window.popApi?.resolveAudio) return null
  try {
    return (await window.popApi.resolveAudio(artist, title, fresh)) || null
  } catch {
    return null
  }
}
