import { SPOTIFY_API_PATH } from '@/shared/config'
import type { Playlist } from '@/shared/types/track'
import { getJson, HttpError } from './http'

/** Extrae el ID de una playlist de Spotify desde una URL o un ID/URI directo. */
export function parseSpotifyPlaylistId(input: string): string | null {
  const value = input.trim()
  if (!value) return null

  const uriMatch = /spotify:playlist:([A-Za-z0-9]+)/.exec(value)
  if (uriMatch) return uriMatch[1]

  const urlMatch = /playlist\/([A-Za-z0-9]+)/.exec(value)
  if (urlMatch) return urlMatch[1]

  if (/^[A-Za-z0-9]{16,}$/.test(value)) return value

  return null
}

interface SpotifyPlaylistResponse {
  playlist: {
    id: string
    name: string
    description: string
    thumbnail: string
    tracks: Array<{
      id: string
      title: string
      artist: string
      thumbnail: string
      duration: number
    }>
  }
  itemsAvailable: boolean
}

export interface SpotifyImportResult {
  playlist: Playlist
  itemsAvailable: boolean
}

function friendlyError(error: unknown): Error {
  if (error instanceof HttpError) {
    if (error.status === 400) {
      return new Error(
        'Faltan las credenciales de Spotify. Configura SPOTIFY_CLIENT_SECRET en el servidor (.env o Vercel).',
      )
    }
    if (error.status === 404) return new Error('No se encontró la playlist de Spotify.')
    if (error.status === 403) {
      return new Error(
        'Spotify no permite importar esta playlist con una app nueva (restricción de su API desde 2024). Prueba una playlist pública creada por un usuario.',
      )
    }
    if (error.status === 502) return new Error('Spotify rechazó la petición. Revisa tus credenciales.')
  }
  return new Error(
    'No se pudo conectar con Spotify. En local, la función /api/spotify la emula el servidor de Vite.',
  )
}

/** Importa los metadatos de una playlist de Spotify a través de la función serverless. */
export async function importSpotifyPlaylist(input: string): Promise<SpotifyImportResult> {
  const playlistId = parseSpotifyPlaylistId(input)
  if (!playlistId) {
    throw new Error('No se pudo reconocer la URL o el ID de la playlist de Spotify.')
  }

  try {
    const data = await getJson<SpotifyPlaylistResponse>(
      `${SPOTIFY_API_PATH}?action=playlist&id=${encodeURIComponent(playlistId)}`,
    )

    const playlist: Playlist = {
      id: `spotify:${data.playlist.id}`,
      name: data.playlist.name,
      description: data.playlist.description,
      thumbnail: data.playlist.thumbnail,
      source: 'spotify',
      importedAt: Date.now(),
      tracks: data.playlist.tracks.map((track) => ({
        id: track.id,
        title: track.title,
        artist: track.artist,
        thumbnail: track.thumbnail,
        duration: track.duration,
        source: 'spotify' as const,
      })),
    }

    return { playlist, itemsAvailable: data.itemsAvailable }
  } catch (error) {
    throw friendlyError(error)
  }
}
