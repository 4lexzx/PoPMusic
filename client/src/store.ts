import type { Album, Artist, Settings, Track } from './types'
import { DEFAULT_SETTINGS } from './api'

export interface Playlist {
  id: string
  name: string
  tracks: Track[]
  createdAt: number
  /** Enlace de origen si se importó (Spotify, YouTube…). */
  source?: string
}

export interface Library {
  favorites: Track[]
  recent: Track[]
  playlists: Playlist[]
  /** Álbumes guardados y artistas seguidos (pestañas de Biblioteca). */
  albums: Album[]
  artists: Artist[]
}

export interface QueueState {
  tracks: Track[]
  index: number
  shuffle: boolean
  repeat: 'off' | 'all' | 'one'
}

const LIB_KEY = 'popmusic-client-library-v1'
const SET_KEY = 'popmusic-client-settings-v1'
const QUEUE_KEY = 'popmusic-client-queue-v1'

const EMPTY: Library = { favorites: [], recent: [], playlists: [], albums: [], artists: [] }
const EMPTY_QUEUE: QueueState = { tracks: [], index: 0, shuffle: false, repeat: 'off' }

function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? { ...fallback, ...JSON.parse(raw) } : fallback
  } catch {
    return fallback
  }
}

function write(key: string, value: unknown): void {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // Sin espacio o bloqueado: la sesión continúa en memoria.
  }
}

export const loadLibrary = (): Library => read(LIB_KEY, EMPTY)
export const saveLibrary = (l: Library) => write(LIB_KEY, l)

export const loadSettings = (): Settings => read(SET_KEY, DEFAULT_SETTINGS)
export const saveSettings = (s: Settings) => write(SET_KEY, s)

export const loadQueue = (): QueueState => {
  const q = read(QUEUE_KEY, EMPTY_QUEUE)
  return { ...q, index: Math.min(q.index, Math.max(0, q.tracks.length - 1)) }
}
export const saveQueue = (q: QueueState) => write(QUEUE_KEY, q)

export function upsertRecent(library: Library, track: Track): Library {
  return {
    ...library,
    recent: [track, ...library.recent.filter((t) => t.id !== track.id)].slice(0, 100),
  }
}

export function toggleFavorite(library: Library, track: Track): Library {
  const exists = library.favorites.some((t) => t.id === track.id)
  return {
    ...library,
    favorites: exists
      ? library.favorites.filter((t) => t.id !== track.id)
      : [track, ...library.favorites],
  }
}
