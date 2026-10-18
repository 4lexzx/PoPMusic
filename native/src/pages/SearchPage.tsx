import { useEffect, useMemo, useRef, useState, type ComponentRef } from 'react'
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native'
import { useRoute, type RouteProp } from '@react-navigation/native'
import { ChevronRightIcon, CloseIcon, MusicIcon, SearchIcon, SparklesIcon } from '@/components/Icons'
import { ScreenHeader, Spinner } from '@/components/ui'
import { HomeSuggestions, SuggestionRail } from '@/features/home/SuggestionRail'
import { SearchResults } from '@/features/search/SearchResults'
import { searchMusic } from '@/shared/api/youtube'
import { IS_DEMO } from '@/shared/config'
import { demoTracks } from '@/shared/data/demo'
import { useSettingsStore } from '@/shared/store/settingsStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { Track } from '@/shared/types/track'
import type { RootTabParamList } from '@/navigation/types'

type SearchStatus = 'idle' | 'loading' | 'done' | 'error'

const SUGGESTION_POOL = [
  'Rick Astley',
  'Queen',
  'The Weeknd',
  'Dua Lipa',
  'Coldplay',
  'Imagine Dragons',
  'Ed Sheeran',
  'Adele',
  'Bad Bunny',
  'Taylor Swift',
  'Maroon 5',
  'OneRepublic',
  'Katy Perry',
  'twenty one pilots',
  'Teddy Swims',
  'Macklemore',
  'Daft Punk',
  'Luis Fonsi',
]

export function SearchPage() {
  const colors = useThemeColors()
  const route = useRoute<RouteProp<RootTabParamList, 'Search'>>()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Track[]>([])
  const [status, setStatus] = useState<SearchStatus>('idle')
  const inputRef = useRef<ComponentRef<typeof TextInput> | null>(null)

  const searchHistory = useUiStore((state) => state.searchHistory)
  const addSearch = useUiStore((state) => state.addSearch)
  const clearSearchHistory = useUiStore((state) => state.clearSearchHistory)
  const notify = useUiStore((state) => state.notify)

  const algoGenres = useSettingsStore((state) => state.genres)
  const algoArtists = useSettingsStore((state) => state.artists)
  const algorithmReady = algoGenres.length > 0 || algoArtists.length > 0

  const liveMatches = useMemo(() => {
    const term = query.trim().toLowerCase()
    if (!term) return []
    const pool = new Set<string>([
      ...searchHistory,
      ...SUGGESTION_POOL,
      ...demoTracks.map((track) => track.artist),
      ...demoTracks.map((track) => track.title),
    ])
    return [...pool]
      .filter((item) => item.toLowerCase().includes(term))
      .sort((a, b) => a.toLowerCase().indexOf(term) - b.toLowerCase().indexOf(term))
      .slice(0, 7)
  }, [query, searchHistory])

  const runSearch = async (term: string) => {
    const value = term.trim()
    if (!value) return
    setQuery(value)
    setStatus('loading')
    addSearch(value)
    inputRef.current?.blur()
    try {
      const tracks = await searchMusic(value)
      setResults(tracks)
      setStatus('done')
    } catch {
      setStatus('error')
      notify('No se pudo realizar la búsqueda. Revisa tu clave de YouTube.', 'error')
    }
  }

  const reset = () => {
    setQuery('')
    setResults([])
    setStatus('idle')
    inputRef.current?.focus()
  }

  const initialQuery = route.params?.q
  const handledRef = useRef<string | null>(null)
  useEffect(() => {
    if (!initialQuery || !initialQuery.trim()) return
    if (handledRef.current === initialQuery) return
    handledRef.current = initialQuery
    void runSearch(initialQuery)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialQuery])

  const showSuggestions = status === 'idle' && query.trim().length > 0

  return (
    <ScrollView
      showsVerticalScrollIndicator={false}
      keyboardShouldPersistTaps="handled"
      contentContainerStyle={{ paddingBottom: 16 }}
    >
      <ScreenHeader title="Buscar" subtitle={IS_DEMO ? 'Modo demo: datos locales' : undefined} />

      <View className="px-5">
        <View
          className="h-12 flex-row items-center gap-2 rounded-2xl px-4"
          style={{ backgroundColor: colors.overlay2, borderWidth: 1, borderColor: colors.hair }}
        >
          <SearchIcon size={20} color={colors.faint} />
          <TextInput
            ref={inputRef}
            value={query}
            onChangeText={(text) => {
              setQuery(text)
              if (status !== 'idle') setStatus('idle')
            }}
            onSubmitEditing={() => void runSearch(query)}
            returnKeyType="search"
            placeholder="Canciones, artistas o álbumes"
            placeholderTextColor={colors.faint}
            className="h-12 flex-1 text-sm"
            style={{ color: colors.ink }}
          />
          {query ? (
            <Pressable accessibilityLabel="Borrar" onPress={reset}>
              <CloseIcon size={18} color={colors.faint} />
            </Pressable>
          ) : null}
        </View>
      </View>

      {showSuggestions ? (
        <View className="mt-3 px-5">
          <View
            className="overflow-hidden rounded-2xl"
            style={{ borderWidth: 1, borderColor: colors.hair, backgroundColor: colors.overlay1 }}
          >
            {liveMatches.length > 0 ? (
              liveMatches.map((term, index) => (
                <Pressable
                  key={term}
                  onPress={() => void runSearch(term)}
                  className="flex-row items-center gap-3 px-4 py-3"
                  style={{
                    borderTopWidth: index > 0 ? 1 : 0,
                    borderTopColor: colors.hair,
                  }}
                >
                  <SearchIcon size={17} color={colors.faint} />
                  <Text numberOfLines={1} className="min-w-0 flex-1 text-sm" style={{ color: colors.ink }}>
                    {term}
                  </Text>
                  <ChevronRightIcon size={16} color={colors.faint} />
                </Pressable>
              ))
            ) : (
              <Text className="px-4 py-3 text-sm" style={{ color: colors.faint }}>
                Pulsa «Buscar» para ver resultados de «{query.trim()}».
              </Text>
            )}
          </View>
        </View>
      ) : null}

      {status === 'idle' && query.trim().length === 0 ? (
        <View className="mt-5 gap-6">
          {searchHistory.length > 0 ? (
            <View className="px-5">
              <View className="mb-2.5 flex-row items-center justify-between">
                <Text className="text-xs font-semibold uppercase tracking-wider" style={{ color: colors.faint }}>
                  Búsquedas recientes
                </Text>
                <Pressable onPress={clearSearchHistory}>
                  <Text className="text-xs font-medium" style={{ color: colors.accent }}>
                    Borrar
                  </Text>
                </Pressable>
              </View>
              <View className="flex-row flex-wrap gap-2">
                {searchHistory.map((term) => (
                  <Pressable
                    key={term}
                    onPress={() => void runSearch(term)}
                    className="rounded-full px-3.5 py-1.5"
                    style={{ backgroundColor: colors.overlay2 }}
                  >
                    <Text className="text-sm" style={{ color: colors.muted }}>
                      {term}
                    </Text>
                  </Pressable>
                ))}
              </View>
            </View>
          ) : null}

          <View className="px-5">
            <Text className="mb-2.5 text-xs font-semibold uppercase tracking-wider" style={{ color: colors.faint }}>
              Sugerencias para ti
            </Text>
            <View className="flex-row flex-wrap gap-2">
              {SUGGESTION_POOL.slice(IS_DEMO ? 0 : 4, 12).map((term) => (
                <Pressable
                  key={term}
                  onPress={() => void runSearch(term)}
                  className="rounded-full px-3.5 py-1.5"
                  style={{ backgroundColor: colors.overlay2 }}
                >
                  <Text className="text-sm" style={{ color: colors.muted }}>
                    {term}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>

          <HomeSuggestions />

          {algorithmReady ? (
            <SuggestionRail
              algorithm
              title="Hecho para ti"
              icon={<SparklesIcon size={18} color={colors.accent} />}
              offset={1}
            />
          ) : null}

          <SuggestionRail
            title="Éxitos del momento"
            query="grandes éxitos pop del momento"
            icon={<SparklesIcon size={18} color={colors.accent} />}
            offset={2}
          />

          <SuggestionRail
            title="Para descubrir"
            query="nueva música recomendada 2024"
            icon={<SparklesIcon size={18} color={colors.accent} />}
            offset={5}
          />

          {IS_DEMO ? (
            <View>
              <Text
                className="mb-2 px-5 text-xs font-semibold uppercase tracking-wider"
                style={{ color: colors.faint }}
              >
                Canciones de muestra
              </Text>
              <SearchResults results={demoTracks} />
            </View>
          ) : null}
        </View>
      ) : null}

      {status === 'loading' ? (
        <View className="mt-16 items-center gap-3">
          <Spinner size={28} />
          <Text className="text-sm" style={{ color: colors.muted }}>
            Buscando…
          </Text>
        </View>
      ) : null}

      {status === 'done' && results.length > 0 ? (
        <View className="mt-4">
          <SearchResults results={results} />
        </View>
      ) : null}

      {status === 'done' && results.length === 0 ? (
        <View className="mt-16 items-center gap-2 px-8">
          <MusicIcon size={30} color={colors.muted} />
          <Text className="text-center text-sm" style={{ color: colors.muted }}>
            Sin resultados para «{query}».
          </Text>
        </View>
      ) : null}

      {status === 'error' ? (
        <View className="mt-16 items-center gap-2 px-8">
          <Text className="text-center text-sm" style={{ color: colors.muted }}>
            Algo salió mal. Inténtalo de nuevo.
          </Text>
        </View>
      ) : null}
    </ScrollView>
  )
}
