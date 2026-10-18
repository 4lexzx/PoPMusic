import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { zustandStorage } from '@/shared/store/storage'
import type { Playlist, Track } from '@/shared/types/track'

const MAX_HISTORY = 60

interface LibraryState {
  playlists: Playlist[]
  favorites: Track[]
  history: Track[]

  addPlaylist: (playlist: Playlist) => void
  createPlaylist: (name: string, description?: string) => Playlist
  removePlaylist: (id: string) => void
  addTracksToPlaylist: (playlistId: string, tracks: Track[]) => void
  removeTrackFromPlaylist: (playlistId: string, index: number) => void

  toggleFavorite: (track: Track) => void
  isFavorite: (track: Track) => boolean

  addToHistory: (track: Track) => void
  clearHistory: () => void
}

/** Dos pistas son la misma si comparten id y origen. */
export function sameTrack(a: Track, b: Track): boolean {
  return a.id === b.id && a.source === b.source
}

/** Genera un id único para playlists locales. */
function createLocalId(): string {
  const random = Math.random().toString(36).slice(2, 8)
  return `local-${Date.now().toString(36)}-${random}`
}

export const useLibraryStore = create<LibraryState>()(
  persist(
    (set, get) => ({
      playlists: [],
      favorites: [],
      history: [],

      addPlaylist: (playlist) =>
        set((state) => ({
          playlists: [playlist, ...state.playlists.filter((item) => item.id !== playlist.id)],
        })),

      createPlaylist: (name, description = '') => {
        const playlist: Playlist = {
          id: createLocalId(),
          name: name.trim() || 'Nueva lista',
          description: description.trim(),
          thumbnail: '',
          source: 'local',
          tracks: [],
          importedAt: Date.now(),
        }
        set((state) => ({ playlists: [playlist, ...state.playlists] }))
        return playlist
      },

      removePlaylist: (id) =>
        set((state) => ({ playlists: state.playlists.filter((item) => item.id !== id) })),

      addTracksToPlaylist: (playlistId, tracks) =>
        set((state) => ({
          playlists: state.playlists.map((playlist) => {
            if (playlist.id !== playlistId) return playlist
            const fresh = tracks.filter(
              (track) => !playlist.tracks.some((existing) => sameTrack(existing, track)),
            )
            return { ...playlist, tracks: [...playlist.tracks, ...fresh] }
          }),
        })),

      removeTrackFromPlaylist: (playlistId, index) =>
        set((state) => ({
          playlists: state.playlists.map((playlist) =>
            playlist.id === playlistId
              ? { ...playlist, tracks: playlist.tracks.filter((_, i) => i !== index) }
              : playlist,
          ),
        })),

      toggleFavorite: (track) =>
        set((state) => {
          const exists = state.favorites.some((item) => sameTrack(item, track))
          return {
            favorites: exists
              ? state.favorites.filter((item) => !sameTrack(item, track))
              : [track, ...state.favorites],
          }
        }),

      isFavorite: (track) => get().favorites.some((item) => sameTrack(item, track)),

      addToHistory: (track) =>
        set((state) => ({
          history: [track, ...state.history.filter((item) => !sameTrack(item, track))].slice(
            0,
            MAX_HISTORY,
          ),
        })),

      clearHistory: () => set({ history: [] }),
    }),
    {
      name: 'liner-library-v1',
      storage: zustandStorage,
      version: 1,
    },
  ),
)
