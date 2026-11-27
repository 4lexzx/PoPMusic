import { createContext, useContext } from 'react'
import type { ArtQuality, Track } from './types'
import type { Playlist } from './store'

export function formatTime(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000))
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}

export function formatLength(ms: number): string {
  const min = Math.round(ms / 60000)
  if (min < 60) return `${min} min`
  return `${Math.floor(min / 60)} h ${min % 60} min`
}

export type Route =
  | { name: 'home' }
  | { name: 'search'; q?: string }
  | { name: 'library' }
  | { name: 'settings' }
  | { name: 'album'; id: string }
  | { name: 'artist'; id: string; title?: string }
  | { name: 'playlist'; id: string }
  | { name: 'remote'; id: string }
  | { name: 'liked' }
  | { name: 'recent' }

/** Acciones y estado del reproductor que necesitan casi todas las vistas. */
export interface AppActions {
  current: Track | null
  playing: boolean
  resolving: boolean
  playlists: Playlist[]
  play: (track: Track, context?: Track[]) => void
  /** Doble clic: según Ajustes reproduce o añade a la cola. */
  activate: (track: Track, context?: Track[]) => void
  /** Radio a partir de una canción (parecidas, sin fin). */
  startRadio: (track: Track) => void
  /** Precarga el audio (al pasar el ratón por encima, etc.). */
  prefetch: (track: Track, priority?: 'next' | 'hover' | 'idle') => void
  toggle: () => void
  playNext: (track: Track) => void
  addToQueue: (tracks: Track[]) => void
  isFavorite: (id: string) => boolean
  fav: (track: Track) => void
  addToPlaylist: (playlistId: string, track: Track) => void
  removeFromPlaylist: (playlistId: string, trackId: string) => void
  createPlaylist: (name: string, first?: Track) => string
  navigate: (route: Route) => void
  toast: (title: string, detail?: string) => void
}

export const AppContext = createContext<AppActions | null>(null)

export function useApp(): AppActions {
  const ctx = useContext(AppContext)
  if (!ctx) throw new Error('AppContext sin proveedor')
  return ctx
}

/**
 * Precarga al pasar el ratón: si el puntero se queda 220 ms sobre una canción,
 * su audio se resuelve en segundo plano y al hacer clic suena al instante.
 */
let hoverTimer = 0
export function hoverIntent(app: AppActions, track: Track | undefined) {
  window.clearTimeout(hoverTimer)
  if (track) hoverTimer = window.setTimeout(() => app.prefetch(track, 'hover'), 220)
}
export function hoverCancel() { window.clearTimeout(hoverTimer) }

export const ArtQualityContext = createContext<ArtQuality>(1000)
export const useArtQuality = () => useContext(ArtQualityContext)
