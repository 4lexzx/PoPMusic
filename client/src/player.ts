/**
 * Resuelve el audio de una canción.
 * En Electron usa yt-dlp local con la canción completa; si algo falla,
 * el llamador cae al preview de iTunes.
 */
export async function resolveAudio(artist: string, title: string): Promise<string | null> {
  if (!window.popApi?.resolveAudio) return null
  try {
    return (await window.popApi.resolveAudio(artist, title)) || null
  } catch {
    return null
  }
}
