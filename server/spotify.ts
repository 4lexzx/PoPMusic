/**
 * Lógica compartida de la integración con Spotify Web API.
 *
 * Se usa desde:
 *  - `api/spotify.ts`  -> Serverless Function de Vercel (producción)
 *  - `vite.config.ts`  -> middleware de desarrollo para `npm run dev`
 *
 * El Client Secret NUNCA se expone al navegador: solo se lee aquí (servidor).
 */

export interface NormalizedTrack {
  id: string
  title: string
  artist: string
  thumbnail: string
  duration: number
  source: 'spotify'
}

export interface NormalizedPlaylist {
  id: string
  name: string
  description: string
  thumbnail: string
  source: 'spotify'
  tracks: NormalizedTrack[]
}

export interface ApiResult {
  status: number
  body: unknown
}

interface SpotifyImage {
  url?: string
}

interface SpotifyArtist {
  name?: string
}

interface SpotifyTrack {
  id?: string | null
  name?: string
  duration_ms?: number
  artists?: SpotifyArtist[]
  album?: { images?: SpotifyImage[] }
}

interface SpotifyPlaylistItem {
  track?: SpotifyTrack | null
  item?: SpotifyTrack | null
}

function pickImage(images?: SpotifyImage[]): string {
  if (!images || images.length === 0) return ''
  return images[0]?.url ?? ''
}

function errorWithStatus(message: string, status: number): Error {
  const error = new Error(message) as Error & { status: number }
  error.status = status
  return error
}

async function getAccessToken(): Promise<string> {
  const clientId = process.env.SPOTIFY_CLIENT_ID || process.env.VITE_SPOTIFY_CLIENT_ID || ''
  const clientSecret = process.env.SPOTIFY_CLIENT_SECRET || ''

  if (!clientId || !clientSecret) {
    throw errorWithStatus('missing_spotify_credentials', 400)
  }

  const basic = Buffer.from(`${clientId}:${clientSecret}`).toString('base64')
  const response = await fetch('https://accounts.spotify.com/api/token', {
    method: 'POST',
    headers: {
      Authorization: `Basic ${basic}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: 'grant_type=client_credentials',
  })

  if (!response.ok) {
    throw errorWithStatus('spotify_token_failed', 502)
  }

  const data = (await response.json()) as { access_token?: string }
  if (!data.access_token) {
    throw errorWithStatus('spotify_token_failed', 502)
  }
  return data.access_token
}

async function fetchPlaylistItems(
  playlistId: string,
  headers: Record<string, string>,
): Promise<{ tracks: NormalizedTrack[]; itemsAvailable: boolean }> {
  const tracks: NormalizedTrack[] = []
  let offset = 0
  const maxItems = 500

  while (offset < maxItems) {
    const url =
      `https://api.spotify.com/v1/playlists/${playlistId}/items` +
      `?limit=50&offset=${offset}&market=US`

    const response = await fetch(url, { headers })

    if (!response.ok) {
      const fallback = await fetch(
        `https://api.spotify.com/v1/playlists/${playlistId}/tracks?limit=50&offset=${offset}&market=US`,
        { headers },
      )
      if (!fallback.ok) {
        return { tracks, itemsAvailable: false }
      }
      const page = (await fallback.json()) as { items?: SpotifyPlaylistItem[] }
      const items = page.items ?? []
      pushItems(items, tracks)
      if (items.length < 50) break
      offset += 50
      continue
    }

    const page = (await response.json()) as { items?: SpotifyPlaylistItem[] }
    const items = page.items ?? []
    pushItems(items, tracks)
    if (items.length < 50) break
    offset += 50
  }

  return { tracks, itemsAvailable: true }
}

function pushItems(items: SpotifyPlaylistItem[], target: NormalizedTrack[]): void {
  for (const entry of items) {
    const track = entry?.track ?? entry?.item ?? null
    if (!track || !track.id || !track.name) continue
    const artists = (track.artists ?? [])
      .map((artist) => artist.name)
      .filter(Boolean)
      .join(', ')
    target.push({
      id: track.id,
      title: track.name,
      artist: artists || 'Artista desconocido',
      thumbnail: pickImage(track.album?.images),
      duration: Math.round((track.duration_ms ?? 0) / 1000),
      source: 'spotify',
    })
  }
}

export async function handleSpotify(
  query: Record<string, string | string[] | undefined>,
): Promise<ApiResult> {
  const action = String(query.action ?? '')
  const id = String(query.id ?? '')

  if (action !== 'playlist' || !id) {
    return { status: 400, body: { error: 'bad_request' } }
  }

  try {
    const token = await getAccessToken()
    const headers = { Authorization: `Bearer ${token}` }

    const metaResponse = await fetch(
      `https://api.spotify.com/v1/playlists/${id}?fields=name,description,images`,
      { headers },
    )

    if (!metaResponse.ok) {
      const status = metaResponse.status === 404 ? 404 : 502
      return { status, body: { error: 'playlist_not_found' } }
    }

    const meta = (await metaResponse.json()) as {
      name?: string
      description?: string
      images?: SpotifyImage[]
    }

    const { tracks, itemsAvailable } = await fetchPlaylistItems(id, headers)

    const playlist: NormalizedPlaylist = {
      id,
      name: meta.name ?? 'Playlist de Spotify',
      description: meta.description ?? '',
      thumbnail: pickImage(meta.images) || tracks[0]?.thumbnail || '',
      source: 'spotify',
      tracks,
    }

    return { status: 200, body: { playlist, itemsAvailable } }
  } catch (error) {
    const status = (error as { status?: number }).status ?? 500
    return { status, body: { error: (error as Error).message } }
  }
}
