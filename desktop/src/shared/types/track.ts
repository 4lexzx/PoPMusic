export type TrackSource = 'youtube' | 'spotify' | 'audius'

export interface Track {
  /** Identificador interno (videoId de YouTube o id de Spotify/Audius). */
  id: string
  title: string
  artist: string
  thumbnail: string
  /** Duración en segundos. */
  duration: number
  source: TrackSource
  /** videoId de YouTube resuelto (obligatorio para reproducir). */
  videoId?: string
  /** URL de audio directa (Audius): permite reproducir en segundo plano. */
  stream?: string
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
