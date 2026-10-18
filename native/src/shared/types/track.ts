/** Origen de la pista: catálogo legal o plataformas usadas solo para importar. */
export type TrackSource = 'youtube' | 'spotify' | 'audius' | 'deezer' | 'itunes' | 'local'

export interface Track {
  /** Identificador interno (`proveedor:id` en el catálogo legal). */
  id: string
  title: string
  artist: string
  thumbnail: string
  /** Duración en segundos. */
  duration: number
  source: TrackSource
  /** videoId de YouTube (solo pistas de origen YouTube). */
  videoId?: string
  /** URL de audio ya resuelta: permite reproducir sin una segunda búsqueda. */
  streamUrl?: string
  /** `true` si solo hay disponible un fragmento legal de 30 s. */
  preview?: boolean
}

export interface Playlist {
  id: string
  name: string
  description: string
  thumbnail: string
  source: 'youtube' | 'spotify' | 'local'
  tracks: Track[]
  importedAt: number
}

export type RepeatMode = 'off' | 'all' | 'one'
