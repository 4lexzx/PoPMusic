export interface Track {
  id: string
  title: string
  artists: string
  artistId?: string
  coverUrl: string
  durationMs: number
  previewUrl?: string
  releaseDate?: string
  explicit?: boolean
  album?: { id: string; title: string }
}

export interface Album {
  id: string
  title: string
  artist: string
  artistId?: string
  year?: number
  coverUrl: string
  tracks?: Track[]
  /** album | single | ep | compile */
  kind?: string
  genre?: string
}

export interface RemotePlaylist {
  id: string
  title: string
  description: string
  coverUrl: string
  count: number
  author?: string
  tracks?: Track[]
}

export interface Artist {
  id: string
  name: string
  picture?: string
  fans?: number
  albums?: Album[]
  top?: Track[]
  related?: Artist[]
}

export interface SearchResponse {
  items: Track[]
}

export interface SearchAll {
  tracks: Track[]
  albums: Album[]
  artists: Artist[]
}

export type ArtQuality = 600 | 1000 | 1400

export type ThemeMode = 'system' | 'light' | 'dark'
export type LyricsEffect = 'karaoke' | 'glow' | 'bounce' | 'fade' | 'line'

export interface Settings {
  country: string
  volume: number
  crossfade: number
  autoplay: boolean
  /** Al acabar la cola, sigue con canciones parecidas. */
  autoRadio: boolean
  /** Qué hace el doble clic sobre una canción. */
  doubleClick: 'play' | 'queue'
  /** Al abrir la app: retomar la cola anterior o empezar vacío. */
  startup: 'resume' | 'empty'
  /** Dispositivo de salida de audio ('' = el del sistema). */
  outputDevice: string
  /** Superposición flotante en segundo plano. */
  overlay: boolean
  overlayOnMinimize: boolean
  overlayAnchor: 'left' | 'center' | 'right'
  /** Tamaño de las carátulas pedidas al catálogo (px). */
  artQuality: ArtQuality
  /** Intensidad del tinte de la portada sobre el fondo (0-1). */
  tint: number
  /** Animaciones reducidas. */
  reduceMotion: boolean
  theme: ThemeMode
  /** Color de énfasis en hex; '' = monocromo (blanco en oscuro, negro en claro). */
  accent: string
  /** Tamaño de la letra en pantalla completa. */
  lyricsSize: 'm' | 'l' | 'xl'
  /** Desfase manual de la letra en ms (+ adelanta, - atrasa). */
  lyricsOffset: number
  /** Animación de la letra: karaoke, brillo, rebote, aparecer o por línea. */
  lyricsEffect: LyricsEffect
  /** Desenfoca las líneas lejanas de la letra activa. */
  lyricsBlur: boolean
}

export interface LyricWord {
  /** Inicio en ms. */
  t: number
  /** Duración en ms (cuánto se sostiene la palabra). */
  d?: number
  text: string
}

export interface LyricLine {
  time: number
  /** Fin de la línea en ms (si se conoce). */
  end?: number
  text: string
  /** Tiempos por palabra (reales de LrcLib/NetEase o estimados). */
  words?: LyricWord[]
  /** true si los tiempos de palabra son estimados por sílabas. */
  estimated?: boolean
}
