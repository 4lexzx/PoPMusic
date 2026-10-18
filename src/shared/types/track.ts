export type TrackSource = 'youtube' | 'spotify'

export interface Track {
  /** Identificador interno (videoId de YouTube o id de Spotify). */
  id: string
  title: string
  artist: string
  thumbnail: string
  /** Duración en segundos. */
  duration: number
  source: TrackSource
  /** videoId de YouTube resuelto (obligatorio para reproducir). */
  videoId?: string
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
