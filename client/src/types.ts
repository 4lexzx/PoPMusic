export interface Track {
  id: string
  title: string
  artists: string
  coverUrl: string
  durationMs: number
  previewUrl?: string
  album?: { id: string; title: string }
}

export interface SearchResponse {
  items: Track[]
}
