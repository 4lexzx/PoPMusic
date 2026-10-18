import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { ScrollView, Text, View } from 'react-native'
import { ChevronRightIcon, SparklesIcon } from '@/components/Icons'
import { Reveal } from '@/components/ui'
import { TrackCard } from '@/features/library/TrackCard'
import { searchMusic } from '@/shared/api/youtube'
import { IS_DEMO } from '@/shared/config'
import { demoTracks } from '@/shared/data/demo'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { useSettingsStore } from '@/shared/store/settingsStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { Track } from '@/shared/types/track'

/** Resultados ya resueltos por consulta para no agotar la cuota de la API. */
const railCache = new Map<string, Track[]>()

interface SuggestionRailProps {
  title: string
  query?: string
  icon?: ReactNode
  offset?: number
  limit?: number
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
  const colors = useThemeColors()
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

  const resolvedQuery = query ?? algorithmQuery ?? (topArtist ? `${topArtist} mejores canciones` : null)

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

    searchMusic(resolvedQuery)
      .then((results) => {
        if (cancelled) return
        const known = new Set(
          [...favorites, ...history].map((track) => `${track.source}:${track.id}`),
        )
        const fresh = results.filter((track) => !known.has(`${track.source}:${track.id}`)).slice(0, limit)
        railCache.set(resolvedQuery, fresh)
        setItems(fresh)
      })
      .catch(() => {
        if (cancelled) return
        setItems(rotate(demoTracks, offset).slice(0, limit))
      })

    return () => {
      cancelled = true
    }
  }, [resolvedQuery, query, topArtist, favorites, history, offset, limit])

  if (items.length < 2) return null

  return (
    <Reveal style={{ marginTop: 28 }}>
      <View className="mb-3 flex-row items-center justify-between px-5">
        <View className="flex-row items-center gap-2">
          {icon}
          <Text className="text-[1.05rem] font-semibold tracking-tight" style={{ color: colors.ink }}>
            {title}
          </Text>
        </View>
        <ChevronRightIcon size={16} color={colors.faint} />
      </View>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={{ gap: 14, paddingHorizontal: 20, paddingBottom: 4 }}
      >
        {items.map((track, index) => (
          <TrackCard key={`${track.source}-${track.id}-${index}`} track={track} queue={items} index={index} />
        ))}
      </ScrollView>
    </Reveal>
  )
}

/** Compatibilidad: fila de sugerencias basada en el artista más escuchado. */
export function HomeSuggestions() {
  const history = useLibraryStore((state) => state.history)
  const favorites = useLibraryStore((state) => state.favorites)
  const playlists = useLibraryStore((state) => state.playlists)
  const favoriteArtists = useSettingsStore((state) => state.artists)
  const colors = useThemeColors()

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
      icon={<SparklesIcon size={18} color={colors.accent} />}
      query={topArtist ? `${topArtist} mejores canciones` : undefined}
      limit={10}
    />
  )
}
