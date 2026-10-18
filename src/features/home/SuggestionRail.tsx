import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { ChevronRightIcon, SparklesIcon } from '@/components/Icons'
import { Reveal } from '@/components/ui'
import { TrackCard } from '@/features/library/TrackCard'
import { searchTracks } from '@/shared/api/youtube'
import { IS_DEMO } from '@/shared/config'
import { demoTracks } from '@/shared/data/demo'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { useSettingsStore } from '@/shared/store/settingsStore'
import type { Track } from '@/shared/types/track'

/** Resultados ya resueltos por consulta para no agotar la cuota de la API. */
const railCache = new Map<string, Track[]>()

interface SuggestionRailProps {
  title: string
  /** Consulta de búsqueda en YouTube. Si se omite, se usa el artista top o «Mi algoritmo». */
  query?: string
  icon?: ReactNode
  /** Desplazamiento del catálogo de muestra en modo demo (para variar filas). */
  offset?: number
  limit?: number
  /** Usa los géneros y artistas de «Mi algoritmo» para construir la consulta. */
  algorithm?: boolean
}

/** Rota una lista para obtener una porción distinta y estable. */
function rotate(list: Track[], offset: number): Track[] {
  if (list.length === 0) return list
  const start = offset % list.length
  return [...list.slice(start), ...list.slice(0, start)]
}

/** Fila horizontal de recomendaciones con portadas grandes. */
export function SuggestionRail({
  title,
  query,
  icon,
  offset = 0,
  limit = 10,
  algorithm = false,
}: SuggestionRailProps) {
  const history = useLibraryStore((state) => state.history)
  const favorites = useLibraryStore((state) => state.favorites)
  const playlists = useLibraryStore((state) => state.playlists)

  const algoArtists = useSettingsStore((state) => state.artists)
  const algoGenres = useSettingsStore((state) => state.genres)
  const discovery = useSettingsStore((state) => state.discovery)

  const [items, setItems] = useState<Track[]>([])

  const topArtist = useMemo(() => {
    const counts = new Map<string, number>()
    const bump = (track: Track) => {
      const artist = track.artist?.trim()
      if (!artist) return
      counts.set(artist, (counts.get(artist) ?? 0) + 1)
    }
    favorites.forEach(bump)
    history.forEach(bump)
    playlists.forEach((playlist) => playlist.tracks.forEach(bump))
    return [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? null
  }, [favorites, history, playlists])

  const algorithmQuery = useMemo(() => {
    if (!algorithm) return null
    if (algoArtists.length > 0) {
      const artist = algoArtists[offset % algoArtists.length]
      return `${artist} ${algoGenres[0] ?? ''} mejores canciones`.trim()
    }
    if (algoGenres.length > 0) {
      const genre = algoGenres[offset % algoGenres.length]
      return discovery > 0.55 ? `${genre} nuevos lanzamientos` : `${genre} mejores canciones`
    }
    return null
  }, [algorithm, algoArtists, algoGenres, discovery, offset])

  const resolvedQuery =
    query ?? algorithmQuery ?? (topArtist ? `${topArtist} mejores canciones` : null)

  useEffect(() => {
    let cancelled = false

    if (IS_DEMO) {
      if (query) {
        setItems(rotate(demoTracks, offset).slice(0, limit))
      } else if (topArtist) {
        const byArtist = demoTracks.filter((track) => track.artist === topArtist)
        setItems((byArtist.length > 0 ? byArtist : rotate(demoTracks, offset)).slice(0, limit))
      } else {
        setItems([])
      }
      return
    }

    if (!resolvedQuery) {
      setItems(rotate(demoTracks, offset).slice(0, limit))
      return
    }

    const cached = railCache.get(resolvedQuery)
    if (cached) {
      setItems(cached)
      return
    }

    searchTracks(resolvedQuery)
      .then((results) => {
        if (cancelled) return
        const known = new Set(
          [...favorites, ...history].map((track) => `${track.source}:${track.id}`),
        )
        const fresh = results
          .filter((track) => !known.has(`${track.source}:${track.id}`))
          .slice(0, limit)
        railCache.set(resolvedQuery, fresh)
        setItems(fresh)
      })
      .catch(() => {
        if (cancelled) return
        /* Si la API falla (sin clave o restringida), mostramos catálogo de muestra. */
        setItems(rotate(demoTracks, offset).slice(0, limit))
      })

    return () => {
      cancelled = true
    }
  }, [resolvedQuery, query, topArtist, favorites, history, offset, limit])

  if (items.length < 2) return null

  return (
    <Reveal>
      <section className="mt-7">
        <div className="mb-3 flex items-center justify-between px-5">
          <h2 className="flex items-center gap-2 text-[1.05rem] font-semibold tracking-tight">
            {icon}
            {title}
          </h2>
          <ChevronRightIcon size={16} className="text-faint" />
        </div>
        <div className="no-scrollbar flex gap-3.5 overflow-x-auto px-5 pb-1">
          {items.map((track, index) => (
            <TrackCard
              key={`${track.source}-${track.id}-${index}`}
              track={track}
              queue={items}
              index={index}
            />
          ))}
        </div>
      </section>
    </Reveal>
  )
}

/** Compatibilidad: fila de sugerencias basada en el artista más escuchado. */
export function HomeSuggestions() {
  const history = useLibraryStore((state) => state.history)
  const favorites = useLibraryStore((state) => state.favorites)
  const playlists = useLibraryStore((state) => state.playlists)
  const favoriteArtists = useSettingsStore((state) => state.artists)

  const topArtist = useMemo(() => {
    if (favoriteArtists.length > 0) return favoriteArtists[0]
    const counts = new Map<string, number>()
    const bump = (track: Track) => {
      const artist = track.artist?.trim()
      if (!artist) return
      counts.set(artist, (counts.get(artist) ?? 0) + 1)
    }
    favorites.forEach(bump)
    history.forEach(bump)
    playlists.forEach((playlist) => playlist.tracks.forEach(bump))
    return [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? null
  }, [favorites, history, playlists, favoriteArtists])

  return (
    <SuggestionRail
      title={topArtist ? `Porque escuchas a ${topArtist}` : 'Quizá te guste'}
      icon={<SparklesIcon size={18} className="text-accent" />}
      query={topArtist ? `${topArtist} mejores canciones` : undefined}
      limit={10}
    />
  )
}
