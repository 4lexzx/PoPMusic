import { AUDD_API_KEY, HAS_RECOGNITION } from '@/shared/config'

export interface RecognizedSong {
  title: string
  artist: string
  album?: string
  cover?: string
}

interface AuddResponse {
  status?: string
  result?: {
    title?: string
    artist?: string
    album?: string
    song_link?: string
    spotify?: { images?: Array<{ url?: string }> }
    apple_music?: { artwork?: { url?: string } }
  } | null
  error?: { error_code?: number; error_message?: string }
}

/**
 * Reconoce la canción de un fragmento de audio usando AudD.
 * Requiere `VITE_AUDD_API_KEY`; si no existe, devuelve `null`.
 */
export async function recognizeAudio(blob: Blob): Promise<RecognizedSong | null> {
  if (!HAS_RECOGNITION) return null

  const form = new FormData()
  form.append('file', blob, 'sample.webm')
  form.append('api_token', AUDD_API_KEY)
  form.append('return', 'spotify,apple_music')

  const response = await fetch('https://api.audd.io/', { method: 'POST', body: form })
  if (!response.ok) {
    throw new Error(`AudD HTTP ${response.status}`)
  }

  const data = (await response.json()) as AuddResponse
  if (!data.result?.title) return null

  return {
    title: data.result.title,
    artist: data.result.artist ?? '',
    album: data.result.album,
    cover:
      data.result.spotify?.images?.[0]?.url ??
      data.result.apple_music?.artwork?.url?.replace('{w}', '640').replace('{h}', '640'),
  }
}
