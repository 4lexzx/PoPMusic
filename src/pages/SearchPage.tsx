import { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ChevronRightIcon, CloseIcon, MusicIcon, SearchIcon, SparklesIcon } from '@/components/Icons'
import { ScreenHeader, Spinner } from '@/components/ui'
import { HomeSuggestions, SuggestionRail } from '@/features/home/SuggestionRail'
import { SearchResults } from '@/features/search/SearchResults'
import { searchTracks } from '@/shared/api/youtube'
import { IS_DEMO } from '@/shared/config'
import { demoTracks } from '@/shared/data/demo'
import { useUiStore } from '@/shared/store/uiStore'
import { useSettingsStore } from '@/shared/store/settingsStore'
import type { Track } from '@/shared/types/track'

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
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Track[]>([])
  const [status, setStatus] = useState<SearchStatus>('idle')
  const inputRef = useRef<HTMLInputElement>(null)
  const [searchParams, setSearchParams] = useSearchParams()

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
      const tracks = await searchTracks(value)
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

  const initialQuery = searchParams.get('q')
  useEffect(() => {
    if (initialQuery && initialQuery.trim()) {
      void runSearch(initialQuery)
      setSearchParams({}, { replace: true })
    }
    // Se ejecuta solo cuando llega un término por URL.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialQuery])

  const showSuggestions = status === 'idle' && query.trim().length > 0

  return (
    <div className="pb-4">
      <ScreenHeader
        title="Buscar"
        subtitle={IS_DEMO ? 'Modo demo: datos locales' : undefined}
      />

      <form
        onSubmit={(event) => {
          event.preventDefault()
          void runSearch(query)
        }}
        className="px-5"
      >
        <div className="flex h-12 items-center gap-2 rounded-2xl hairline surface-2 px-4 transition-colors focus-within:border-accent/50">
          <SearchIcon size={20} className="text-faint" />
          <input
            ref={inputRef}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value)
              if (status !== 'idle') setStatus('idle')
            }}
            placeholder="Canciones, artistas o álbumes"
            enterKeyHint="search"
            className="h-12 flex-1 bg-transparent text-sm outline-none placeholder:text-faint"
          />
          {query ? (
            <button type="button" aria-label="Borrar" onClick={reset} className="text-faint">
              <CloseIcon size={18} />
            </button>
          ) : null}
        </div>
      </form>

      {showSuggestions ? (
        <section className="mt-3 px-5">
          <div className="overflow-hidden rounded-2xl hairline surface-1">
            {liveMatches.length > 0 ? (
              liveMatches.map((term, index) => (
                <button
                  key={term}
                  type="button"
                  onClick={() => void runSearch(term)}
                  className={`flex w-full items-center gap-3 px-4 py-3 text-left text-sm tap ${
                    index > 0 ? 'border-t border-[color:var(--hair)]' : ''
                  }`}
                >
                  <SearchIcon size={17} className="shrink-0 text-faint" />
                  <span className="min-w-0 flex-1 truncate">{term}</span>
                  <ChevronRightIcon size={16} className="shrink-0 text-faint" />
                </button>
              ))
            ) : (
              <p className="px-4 py-3 text-sm text-faint">
                Pulsa «Buscar» para ver resultados de «{query.trim()}».
              </p>
            )}
          </div>
        </section>
      ) : null}

      {status === 'idle' && query.trim().length === 0 ? (
        <div className="mt-5 space-y-6">
          {searchHistory.length > 0 ? (
            <section className="px-5">
              <div className="mb-2.5 flex items-center justify-between">
                <h2 className="text-xs font-semibold uppercase tracking-wider text-faint">
                  Búsquedas recientes
                </h2>
                <button
                  type="button"
                  onClick={clearSearchHistory}
                  className="text-xs font-medium text-accent-soft"
                >
                  Borrar
                </button>
              </div>
              <div className="flex flex-wrap gap-2">
                {searchHistory.map((term) => (
                  <button
                    key={term}
                    type="button"
                    onClick={() => void runSearch(term)}
                    className="rounded-full surface-2 px-3.5 py-1.5 text-sm text-muted tap"
                  >
                    {term}
                  </button>
                ))}
              </div>
            </section>
          ) : null}

          <section className="px-5">
            <h2 className="mb-2.5 text-xs font-semibold uppercase tracking-wider text-faint">
              Sugerencias para ti
            </h2>
            <div className="flex flex-wrap gap-2">
              {SUGGESTION_POOL.slice(IS_DEMO ? 0 : 4, 12).map((term) => (
                <button
                  key={term}
                  type="button"
                  onClick={() => void runSearch(term)}
                  className="rounded-full surface-2 px-3.5 py-1.5 text-sm text-muted tap"
                >
                  {term}
                </button>
              ))}
            </div>
          </section>

          <HomeSuggestions />

          {algorithmReady ? (
            <SuggestionRail
              algorithm
              title="Hecho para ti"
              icon={<SparklesIcon size={18} className="text-accent" />}
              offset={1}
            />
          ) : null}

          <SuggestionRail
            title="Éxitos del momento"
            query="grandes éxitos pop del momento"
            icon={<SparklesIcon size={18} className="text-accent" />}
            offset={2}
          />

          <SuggestionRail
            title="Para descubrir"
            query="nueva música recomendada 2024"
            icon={<SparklesIcon size={18} className="text-accent" />}
            offset={5}
          />

          {IS_DEMO ? (
            <section>
              <h2 className="mb-2 px-5 text-xs font-semibold uppercase tracking-wider text-faint">
                Canciones de muestra
              </h2>
              <SearchResults results={demoTracks} />
            </section>
          ) : null}
        </div>
      ) : null}

      {status === 'loading' ? (
        <div className="mt-16 flex flex-col items-center gap-3 text-muted">
          <Spinner size={28} />
          <p className="text-sm">Buscando…</p>
        </div>
      ) : null}

      {status === 'done' && results.length > 0 ? (
        <div className="mt-4">
          <SearchResults results={results} />
        </div>
      ) : null}

      {status === 'done' && results.length === 0 ? (
        <div className="mt-16 flex flex-col items-center gap-2 px-8 text-center text-muted">
          <MusicIcon size={30} />
          <p className="text-sm">Sin resultados para «{query}».</p>
        </div>
      ) : null}

      {status === 'error' ? (
        <div className="mt-16 flex flex-col items-center gap-2 px-8 text-center text-muted">
          <p className="text-sm">Algo salió mal. Inténtalo de nuevo.</p>
        </div>
      ) : null}
    </div>
  )
}
