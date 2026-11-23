export interface Track {
  id: string
  title: string
  artists: string
  coverUrl: string
  durationMs: number
  previewUrl?: string
  releaseDate?: string
  album?: { id: string; title: string }
}

export interface SearchResponse {
  items: Track[]
}

export interface Settings {
  country: string
  volume: number
  crossfade: number
  autoplay: boolean
}

export interface LyricLine {
  time: number
  text: string
}
