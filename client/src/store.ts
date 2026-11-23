import type { Track } from './types'

export interface Playlist {
  id: string
  name: string
  tracks: Track[]
  createdAt: number
}

export interface Library {
  favorites: Track[]
  recent: Track[]
  playlists: Playlist[]
}

const KEY = 'popmusic-client-library-v1'

const EMPTY: Library = { favorites: [], recent: [], playlists: [] }

export function loadLibrary(): Library {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return EMPTY
    const data = JSON.parse(raw)
    return {
      favorites: Array.isArray(data.favorites) ? data.favorites : [],
      recent: Array.isArray(data.recent) ? data.recent : [],
      playlists: Array.isArray(data.playlists) ? data.playlists : [],
    }
  } catch {
    return EMPTY
  }
}

export function saveLibrary(library: Library): void {
  try {
    localStorage.setItem(KEY, JSON.stringify(library))
  } catch {
    // Almacenamiento lleno o bloqueado: la sesión sigue funcionando en memoria.
  }
}

export function upsertRecent(library: Library, track: Track): Library {
  const recent = [track, ...library.recent.filter((t) => t.id !== track.id)].slice(0, 100)
  return { ...library, recent }
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
