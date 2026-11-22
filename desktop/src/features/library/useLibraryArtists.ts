import { useMemo } from 'react'
import { useLibraryStore } from '@/shared/store/libraryStore'
import type { Track } from '@/shared/types/track'

export interface ArtistGroup {
  name: string
  tracks: Track[]
}

/** Agrupa todas las canciones conocidas por artista (favoritos, historial y listas). */
export function useLibraryArtists(): ArtistGroup[] {
  const favorites = useLibraryStore((state) => state.favorites)
  const history = useLibraryStore((state) => state.history)
  const playlists = useLibraryStore((state) => state.playlists)

  return useMemo(() => {
    const map = new Map<string, Track[]>()
    const add = (track: Track) => {
      const key = track.artist?.trim() || 'Artista desconocido'
      const list = map.get(key) ?? []
      if (!list.some((item) => item.id === track.id && item.source === track.source)) {
        list.push(track)
      }
      map.set(key, list)
    }

    favorites.forEach(add)
    history.forEach(add)
    playlists.forEach((playlist) => playlist.tracks.forEach(add))

    return [...map.entries()]
      .map(([name, tracks]) => ({ name, tracks }))
      .sort((a, b) => b.tracks.length - a.tracks.length || a.name.localeCompare(b.name))
  }, [favorites, history, playlists])
}
