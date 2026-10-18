import { searchCatalog } from './catalog'
import {
  fetchYouTubePlaylist,
  parseYouTubePlaylistId,
  resolveYouTubeAudio,
  searchYouTube,
} from './innertube'
import type { Playlist, Track } from '@/shared/types/track'
import { guessArtistAndTitle } from '@/shared/utils/text'

export { parseYouTubePlaylistId }

/** Convierte un resultado de búsqueda de YouTube en una pista de la app. */
function toTrack(result: {
  videoId: string
  title: string
  channel: string
  thumbnail: string
  duration: number
}): Track {
  const guessed = guessArtistAndTitle(result.title, result.channel)
  return {
    id: result.videoId,
    videoId: result.videoId,
    title: guessed.title,
    artist: guessed.artist,
    thumbnail: result.thumbnail,
    duration: result.duration,
    source: 'youtube',
  }
}

/**
 * Busca música: primero en YouTube (canciones completas) y, si YouTube no
 * responde desde este entorno, cae al catálogo legal sin claves.
 */
export async function searchMusic(query: string, limit = 25): Promise<Track[]> {
  try {
    const results = await searchYouTube(query, limit)
    const tracks = results.map(toTrack)
    if (tracks.length > 0) return tracks
  } catch {
    // Sin conexión a YouTube: probamos el catálogo legal.
  }
  return searchCatalog(query, limit)
}

/**
 * Importa una playlist de YouTube (solo metadatos, sin clave de API).
 * Al reproducir cada pista se resuelve su audio completo.
 */
export async function importYouTubePlaylist(
  input: string,
  onProgress?: (loaded: number) => void,
): Promise<Playlist> {
  const playlistId = parseYouTubePlaylistId(input)
  if (!playlistId) {
    throw new Error('No se pudo reconocer la URL o el ID de la playlist.')
  }

  const data = await fetchYouTubePlaylist(playlistId, onProgress)
  if (data.items.length === 0) {
    throw new Error('La playlist está vacía o no es accesible.')
  }

  const tracks = data.items.map(toTrack)

  return {
    id: playlistId,
    name: data.title,
    description: '',
    thumbnail: tracks[0]?.thumbnail ?? '',
    source: 'youtube',
    tracks,
    importedAt: Date.now(),
  }
}

export { resolveYouTubeAudio }
