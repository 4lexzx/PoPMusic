import { HAS_YT_KEY, YT_API_KEY } from '@/shared/config'
import { searchDemo } from '@/shared/data/demo'
import type { Playlist, Track } from '@/shared/types/track'
import type {
  YouTubePlaylistItem,
  YouTubeSearchItem,
  YouTubeVideoItem,
} from '@/shared/types/youtube'
import { parseIsoDuration } from '@/shared/utils/time'
import { guessArtistAndTitle } from '@/shared/utils/text'
import { getJson } from './http'

const BASE = 'https://www.googleapis.com/youtube/v3'

function thumbnailsToUrl(
  thumbnails: Record<string, { url?: string }> | undefined,
): string {
  if (!thumbnails) return ''
  const preferred = ['maxres', 'standard', 'high', 'medium', 'default']
  for (const key of preferred) {
    const candidate = thumbnails[key]?.url
    if (candidate) return candidate
  }
  const first = Object.values(thumbnails)[0]?.url
  return first ?? ''
}

/** Busca vídeos en YouTube. En modo demo filtra datos locales. */
export async function searchTracks(query: string, maxResults = 25): Promise<Track[]> {
  if (!HAS_YT_KEY) {
    return searchDemo(query)
  }

  const searchParams = new URLSearchParams({
    part: 'snippet',
    type: 'video',
    videoEmbeddable: 'true',
    videoCategoryId: '10',
    maxResults: String(maxResults),
    q: query,
    key: YT_API_KEY,
  })

  const searchData = await getJson<{ items?: YouTubeSearchItem[] }>(
    `${BASE}/search?${searchParams.toString()}`,
  )

  const ids = (searchData.items ?? [])
    .map((item) => item.id?.videoId)
    .filter((id): id is string => Boolean(id))

  if (ids.length === 0) return []

  const videoData = await getJson<{ items?: YouTubeVideoItem[] }>(
    `${BASE}/videos?part=snippet,contentDetails&id=${ids.join(',')}&key=${YT_API_KEY}`,
  )

  const byId = new Map((videoData.items ?? []).map((item) => [item.id, item]))

  return ids.map((id) => {
    const item = byId.get(id)
    const snippet = item?.snippet
    const guessed = guessArtistAndTitle(snippet?.title ?? 'Vídeo', snippet?.channelTitle ?? '')
    return {
      id,
      videoId: id,
      title: guessed.title,
      artist: guessed.artist,
      thumbnail: thumbnailsToUrl(snippet?.thumbnails) || `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
      duration: parseIsoDuration(item?.contentDetails?.duration),
      source: 'youtube' as const,
    }
  })
}

/** Extrae el ID de una playlist a partir de una URL o de un ID directo. */
export function parseYouTubePlaylistId(input: string): string | null {
  const value = input.trim()
  if (!value) return null

  const urlMatch = /[?&]list=([A-Za-z0-9_-]+)/.exec(value)
  if (urlMatch) return urlMatch[1]

  if (/^(PL|UU|OLAK|FL|RD)[A-Za-z0-9_-]{10,}$/.test(value)) return value
  if (/^[A-Za-z0-9_-]{18,}$/.test(value)) return value

  return null
}

/** Metadatos de una playlist de YouTube. */
export async function getPlaylistMeta(playlistId: string): Promise<{
  name: string
  description: string
  thumbnail: string
}> {
  const data = await getJson<{
    items?: Array<{
      snippet?: { title?: string; description?: string; thumbnails?: Record<string, { url?: string }> }
    }>
  }>(`${BASE}/playlists?part=snippet&id=${playlistId}&key=${YT_API_KEY}`)

  const snippet = data.items?.[0]?.snippet
  return {
    name: snippet?.title ?? 'Playlist de YouTube',
    description: snippet?.description ?? '',
    thumbnail: thumbnailsToUrl(snippet?.thumbnails),
  }
}

/** Descarga una playlist completa de YouTube (con paginación). */
export async function importYouTubePlaylist(
  input: string,
  onProgress?: (loaded: number) => void,
): Promise<Playlist> {
  const playlistId = parseYouTubePlaylistId(input)
  if (!playlistId) {
    throw new Error('No se pudo reconocer la URL o el ID de la playlist.')
  }

  if (!HAS_YT_KEY) {
    throw new Error('Necesitas configurar VITE_YT_API_KEY para importar playlists de YouTube.')
  }

  const meta = await getPlaylistMeta(playlistId)

  const videoIds: string[] = []
  let pageToken = ''

  do {
    const params = new URLSearchParams({
      part: 'snippet,contentDetails',
      playlistId,
      maxResults: '50',
      key: YT_API_KEY,
    })
    if (pageToken) params.set('pageToken', pageToken)

    const page = await getJson<{
      items?: YouTubePlaylistItem[]
      nextPageToken?: string
    }>(`${BASE}/playlistItems?${params.toString()}`)

    for (const item of page.items ?? []) {
      const videoId = item.snippet?.resourceId?.videoId
      if (videoId) videoIds.push(videoId)
    }

    onProgress?.(videoIds.length)
    pageToken = page.nextPageToken ?? ''
  } while (pageToken)

  const tracks: Track[] = []
  for (let index = 0; index < videoIds.length; index += 50) {
    const chunk = videoIds.slice(index, index + 50)
    const data = await getJson<{ items?: YouTubeVideoItem[] }>(
      `${BASE}/videos?part=snippet,contentDetails&id=${chunk.join(',')}&key=${YT_API_KEY}`,
    )
    const byId = new Map((data.items ?? []).map((item) => [item.id, item]))

    for (const id of chunk) {
      const item = byId.get(id)
      const snippet = item?.snippet
      // Vídeos privados o eliminados no devuelven snippet.
      if (!snippet) continue
      const guessed = guessArtistAndTitle(snippet.title ?? 'Vídeo', snippet.channelTitle ?? '')
      tracks.push({
        id,
        videoId: id,
        title: guessed.title,
        artist: guessed.artist,
        thumbnail:
          thumbnailsToUrl(snippet.thumbnails) || `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
        duration: parseIsoDuration(item?.contentDetails?.duration),
        source: 'youtube',
      })
    }
  }

  return {
    id: playlistId,
    name: meta.name,
    description: meta.description,
    thumbnail: meta.thumbnail || tracks[0]?.thumbnail || '',
    source: 'youtube',
    tracks,
    importedAt: Date.now(),
  }
}

/** Devuelve el primer videoId que coincida con una pista de Spotify. */
export async function resolveYouTubeVideoId(title: string, artist: string): Promise<string | null> {
  if (!HAS_YT_KEY) return null
  const results = await searchTracks(`${title} ${artist} official`, 1)
  return results[0]?.videoId ?? null
}
