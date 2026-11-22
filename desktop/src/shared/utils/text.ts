const NOISE =
  /[([][^)\]]*(official|oficial|video|audio|lyric|letra|lyrics|hd|hq|4k|8k|remaster|remastered|visualizer|music video|en vivo|live|mv|m\/v)[^)\]]*[)\]]/gi

/** Limpia títulos de YouTube eliminando ruido habitual (Official Video, [Lyrics]...). */
export function cleanYouTubeTitle(raw: string): string {
  return raw
    .replace(NOISE, '')
    .replace(/\s*[-–—]\s*topic$/i, '')
    .replace(/\s{2,}/g, ' ')
    .replace(/\s*[-–—]\s*$/, '')
    .trim()
}

/** Limpia el nombre del canal para usarlo como artista. */
export function cleanChannelName(raw: string): string {
  return raw
    .replace(/\s*-\s*Topic$/i, '')
    .replace(/\s*VEVO$/i, '')
    .replace(/\s{2,}/g, ' ')
    .trim()
}

/**
 * Deduce artista y título. Si el título sigue el patrón "Artista - Canción"
 * se usa el lado izquierdo como artista; si no, se usa el canal.
 */
export function guessArtistAndTitle(
  rawTitle: string,
  channel: string,
): { title: string; artist: string } {
  const title = cleanYouTubeTitle(rawTitle)
  const artist = cleanChannelName(channel)

  const separator = title.match(/^(.{2,60}?)\s+[-–—]\s+(.+)$/)
  if (separator) {
    const left = separator[1].trim()
    const right = separator[2].trim()
    const leftLooksLikeArtist = left.length <= 40 && !/[.!?]$/.test(left)
    if (leftLooksLikeArtist && right.length > 1) {
      return { title: right, artist: left }
    }
  }

  return { title: title || rawTitle, artist: artist || 'Artista desconocido' }
}

/** Construye la consulta de búsqueda en YouTube para una pista de Spotify. */
export function buildYouTubeQuery(title: string, artist: string): string {
  const artistPart = artist && artist !== 'Artista desconocido' ? ` ${artist}` : ''
  return `${title}${artistPart} official`
}
