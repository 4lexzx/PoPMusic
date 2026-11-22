import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import {
  ChevronRightIcon,
  CloseIcon,
  MusicIcon,
  SearchIcon,
  SparklesIcon,
} from '@/components/Icons'
import { Spinner } from '@/components/ui'
import { TrackRow } from '@/features/library/TrackRow'
import { AudiusRail, HomeSuggestions, SuggestionRail } from '@/features/home/SuggestionRail'
import { SearchResults } from '@/features/search/SearchResults'
import { searchAudius } from '@/shared/api/audius'
import { searchTracks } from '@/shared/api/youtube'
import { IS_DEMO } from '@/shared/config'
import { demoTracks } from '@/shared/data/demo'
import { useUiStore } from '@/shared/store/uiStore'
import { useSettingsStore } from '@/shared/store/settingsStore'
import type { Track } from '@/shared/types/track'

type SearchStatus = 'idle' | 'loading' | 'done' | 'error'

/** Easing suave compartido con el resto de la app (Liner). */
const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1]

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

/** Entrada escalonada y serena para filas, chips y tarjetas. */
function rowMotion(reduceMotion: boolean | null, index: number) {
  return {
    initial: reduceMotion ? false : { opacity: 0, y: 8 },
    animate: { opacity: 1, y: 0 },
    transition: {
      duration: reduceMotion ? 0 : 0.3,
      delay: reduceMotion ? 0 : Math.min(index * 0.04, 0.3),
      ease: EASE,
    },
  }
}

/** Panel de cristal con cabecera discreta, como en Liner. */
function ResultPanel({
  title,
  children,
  className = '',
}: {
  title?: string
  children: ReactNode
  className?: string
}) {
  const reduceMotion = useReducedMotion()
  return (
    <motion.section
      className={`glass-panel mx-4 overflow-hidden rounded-[20px] ${className}`}
      initial={reduceMotion ? false : { opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: reduceMotion ? 0 : 0.35, ease: EASE }}
    >
      {title ? (
        <div className="flex items-center justify-between px-5 pb-1.5 pt-4">
          <h2 className="text-xs font-semibold uppercase tracking-wider text-faint">{title}</h2>
        </div>
      ) : null}
      {children}
    </motion.section>
  )
}

/** Lista de pistas con filas animadas (mantiene TrackRow y su menú ⋮). */
function AnimatedResults({ results }: { results: Track[] }) {
  const reduceMotion = useReducedMotion()
  return (
    <div className="px-2 pb-2 pt-1">
      {results.map((track, index) => (
        <motion.div
          key={`${track.source}-${track.id}-${index}`}
          {...rowMotion(reduceMotion, index)}
        >
          <TrackRow track={track} queue={results} index={index} />
        </motion.div>
      ))}
    </div>
  )
}

export function SearchPage() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Track[]>([])
  const [audiusResults, setAudiusResults] = useState<Track[]>([])
  const [status, setStatus] = useState<SearchStatus>('idle')
  const inputRef = useRef<HTMLInputElement>(null)
  const requestIdRef = useRef(0)
  const skipLiveRef = useRef<string | null>(null)
  const [searchParams, setSearchParams] = useSearchParams()
  const reduceMotion = useReducedMotion()

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

  const executeSearch = useCallback(
    async (value: string, record = false) => {
      const requestId = ++requestIdRef.current
      if (record) addSearch(value)
      setStatus('loading')
      try {
        const [tracks, audius] = await Promise.all([
          searchTracks(value).catch(() => [] as Track[]),
          searchAudius(value, 12),
        ])
        if (requestId !== requestIdRef.current) return
        setResults(tracks)
        setAudiusResults(audius)
        setStatus('done')
      } catch {
        if (requestId !== requestIdRef.current) return
        setStatus('error')
        notify('No se pudo realizar la búsqueda. Revisa tu clave de YouTube.', 'error')
      }
    },
    [addSearch, notify],
  )

  const runSearch = (term: string) => {
    const value = term.trim()
    if (!value) return
    setQuery(value)
    skipLiveRef.current = value
    inputRef.current?.blur()
    void executeSearch(value, true)
  }

  // Búsqueda automática mientras se escribe (con debounce y descarte de respuestas viejas).
  useEffect(() => {
    const value = query.trim()

    if (!value) {
      requestIdRef.current += 1
      setResults([])
      setAudiusResults([])
      setStatus('idle')
      return
    }

    if (skipLiveRef.current === value) {
      skipLiveRef.current = null
      return
    }

    if (value.length < 2) {
      requestIdRef.current += 1
      setStatus('idle')
      return
    }

    // Invalida respuestas de la consulta anterior en cuanto cambia el texto.
    requestIdRef.current += 1

    const timeout = window.setTimeout(() => {
      void executeSearch(value)
    }, 400)

    return () => window.clearTimeout(timeout)
  }, [query, executeSearch])

  const reset = () => {
    requestIdRef.current += 1
    skipLiveRef.current = null
    setQuery('')
    setResults([])
    setAudiusResults([])
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

  const showSuggestions = query.trim().length > 0 && status !== 'done' && status !== 'error'
  const hasNoResults = status === 'done' && results.length === 0 && audiusResults.length === 0

  return (
    <div className="pb-10">
      <header className="flex items-end justify-between gap-3 px-5 pb-4 pt-5">
        <div className="min-w-0">
          <motion.h1
            className="truncate text-2xl font-semibold tracking-tight"
            initial={reduceMotion ? false : { opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: reduceMotion ? 0 : 0.35, ease: EASE }}
          >
            Buscar
          </motion.h1>
          {IS_DEMO ? (
            <p className="mt-1 text-xs text-faint">Modo demo · datos locales</p>
          ) : null}
        </div>
      </header>

      <form
        onSubmit={(event) => {
          event.preventDefault()
          void runSearch(query)
        }}
        className="px-5"
      >
        <motion.div
          className="glass-panel flex h-12 items-center gap-3 rounded-[12px] px-4 transition-colors focus-within:border-[color:var(--hair-strong)]"
          initial={reduceMotion ? false : { opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: reduceMotion ? 0 : 0.35, delay: 0.04, ease: EASE }}
        >
          <SearchIcon size={20} className="shrink-0 text-faint" />
          <input
            ref={inputRef}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Busca canciones, artistas o álbumes"
            enterKeyHint="search"
            aria-label="Buscar música"
            className="h-12 min-w-0 flex-1 bg-transparent text-sm text-ink outline-none placeholder:text-faint"
          />
          {query ? (
            <motion.button
              type="button"
              aria-label="Borrar búsqueda"
              onClick={reset}
              whileTap={{ scale: 0.88 }}
              className="grid h-7 w-7 shrink-0 place-items-center rounded-full text-faint transition-colors hover:text-ink"
            >
              <CloseIcon size={18} />
            </motion.button>
          ) : null}
        </motion.div>
      </form>

      {showSuggestions ? (
        <ResultPanel className="mt-3">
          {liveMatches.length > 0 ? (
            <div className="px-2 pb-2 pt-1">
              {liveMatches.map((term, index) => (
                <motion.button
                  key={term}
                  type="button"
                  onClick={() => void runSearch(term)}
                  {...rowMotion(reduceMotion, index)}
                  className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm text-muted tap"
                >
                  <SearchIcon size={17} className="shrink-0 text-faint" />
                  <span className="min-w-0 flex-1 truncate">{term}</span>
                  <ChevronRightIcon size={16} className="shrink-0 text-faint" />
                </motion.button>
              ))}
            </div>
          ) : (
            <p className="px-5 pb-4 pt-1 text-sm text-faint">
              Escribe al menos 2 letras para buscar «{query.trim()}».
            </p>
          )}
        </ResultPanel>
      ) : null}

      {status === 'idle' && query.trim().length === 0 ? (
        <div>
          {searchHistory.length > 0 ? (
            <ResultPanel title="Búsquedas recientes" className="mt-5">
              <div className="flex items-center justify-between px-5 pb-1">
                <span />
                <button
                  type="button"
                  onClick={clearSearchHistory}
                  className="text-xs font-medium text-accent-soft transition-opacity hover:opacity-80"
                >
                  Borrar
                </button>
              </div>
              <div className="flex flex-wrap gap-2 px-5 pb-4 pt-1">
                {searchHistory.map((term, index) => (
                  <motion.button
                    key={term}
                    type="button"
                    onClick={() => void runSearch(term)}
                    {...rowMotion(reduceMotion, index)}
                    className="rounded-full surface-2 px-3.5 py-1.5 text-sm text-muted transition-colors hover:text-ink tap"
                  >
                    {term}
                  </motion.button>
                ))}
              </div>
            </ResultPanel>
          ) : null}

          <ResultPanel title="Sugerencias para ti" className="mt-4">
            <div className="flex flex-wrap gap-2 px-5 pb-4 pt-2">
              {SUGGESTION_POOL.slice(IS_DEMO ? 0 : 4, 12).map((term, index) => (
                <motion.button
                  key={term}
                  type="button"
                  onClick={() => void runSearch(term)}
                  {...rowMotion(reduceMotion, index)}
                  className="rounded-full surface-2 px-3.5 py-1.5 text-sm text-muted transition-colors hover:text-ink tap"
                >
                  {term}
                </motion.button>
              ))}
            </div>
          </ResultPanel>

          <div className="glass-panel mx-4 mt-4 overflow-hidden rounded-[20px] pb-3">
            <div className="px-5 pt-4">
              <h2 className="text-xs font-semibold uppercase tracking-wider text-faint">
                Descubre
              </h2>
            </div>
            <HomeSuggestions />
            <AudiusRail />
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
          </div>

          {IS_DEMO ? (
            <ResultPanel title="Canciones de muestra" className="mt-4">
              <SearchResults results={demoTracks} />
            </ResultPanel>
          ) : null}
        </div>
      ) : null}

      {status === 'loading' ? (
        <ResultPanel className="mt-4">
          <div className="flex items-center gap-3 px-5 py-4 text-muted">
            <Spinner size={18} />
            <p className="text-sm">Buscando «{query.trim()}»…</p>
          </div>
          <div className="space-y-2 px-5 pb-5">
            <div className="skeleton h-3 w-3/5 rounded-full" />
            <div className="skeleton h-3 w-2/5 rounded-full" />
          </div>
        </ResultPanel>
      ) : null}

      {status === 'done' && results.length > 0 ? (
        <ResultPanel title="Canciones" className="mt-4">
          <AnimatedResults results={results} />
        </ResultPanel>
      ) : null}

      {status === 'done' && audiusResults.length > 0 ? (
        <ResultPanel title="Suena en segundo plano · Audius" className="mt-4">
          <AnimatedResults results={audiusResults} />
        </ResultPanel>
      ) : null}

      {hasNoResults ? (
        <ResultPanel className="mt-4">
          <div className="flex flex-col items-center gap-2 px-8 py-12 text-center text-muted">
            <MusicIcon size={30} className="text-faint" />
            <p className="text-sm">Sin resultados para «{query}».</p>
            <p className="text-xs text-faint">Prueba con otro título, artista o álbum.</p>
          </div>
        </ResultPanel>
      ) : null}

      {status === 'error' ? (
        <ResultPanel className="mt-4">
          <div className="flex flex-col items-center gap-2 px-8 py-12 text-center text-muted">
            <MusicIcon size={30} className="text-faint" />
            <p className="text-sm">Algo salió mal. Inténtalo de nuevo.</p>
          </div>
        </ResultPanel>
      ) : null}
    </div>
  )
}
