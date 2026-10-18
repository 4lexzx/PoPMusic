import { AUDD_API_KEY, HAS_RECOGNITION } from '@/shared/config'

export interface RecognizedSong {
  title: string
  artist: string
  album?: string
  cover?: string
}

/** Archivo de audio listo para subir (formato de React Native). */
export interface AudioSample {
  uri: string
  name: string
  type: string
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
 * Requiere `EXPO_PUBLIC_AUDD_API_KEY`; si no existe, devuelve `null`.
 */
export async function recognizeAudio(sample: AudioSample): Promise<RecognizedSong | null> {
  if (!HAS_RECOGNITION) return null

  const form = new FormData()
  form.append('file', {
    uri: sample.uri,
    name: sample.name,
    type: sample.type,
  } as unknown as Blob)
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
