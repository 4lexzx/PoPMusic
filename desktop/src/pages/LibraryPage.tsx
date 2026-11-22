import { useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { Artwork } from '@/components/Artwork'
import {
  GridIcon,
  HeartFilledIcon,
  ListIcon,
  MusicIcon,
  PlayIcon,
  PlusIcon,
  SearchIcon,
  TrashIcon,
} from '@/components/Icons'
import { Button } from '@/components/ui'
import { AddToLibraryDialog } from '@/features/library/AddToLibraryDialog'
import { TrackRow } from '@/features/library/TrackRow'
import { useLibraryArtists } from '@/features/library/useLibraryArtists'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import type { Playlist } from '@/shared/types/track'
import { formatDuration } from '@/shared/utils/time'

type Tab = 'lists' | 'favorites' | 'artists'
type ViewMode = 'grid' | 'list'

const TABS: Array<{ id: Tab; label: string }> = [
  { id: 'lists', label: 'Listas' },
  { id: 'favorites', label: 'Álbumes' },
  { id: 'artists', label: 'Artistas' },
]

const EASE = [0.22, 1, 0.36, 1] as const

/** Paleta cálida de la captura: negro cálido, superficies grafito, texto apagado. */
const WARM = {
  bg: '#0c0b0a',
  surface: '#1a1917',
  surfaceDeep: '#141312',
  text: '#ffffff',
  muted: '#8c8780',
  line: 'rgba(255, 255, 255, 0.06)',
} as const

function sourceLabel(source: Playlist['source']): string {
  return source === 'spotify' ? 'Spotify' : source === 'youtube' ? 'YouTube' : 'Local'
}

function coverSources(playlist: Playlist): string[] {
  return [playlist.thumbnail, ...playlist.tracks.map((track) => track.thumbnail)]
    .filter((src): src is string => Boolean(src))
    .slice(0, 4)
}

export function LibraryPage() {
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('lists')
  const [view, setView] = useState<ViewMode>('grid')
  const [query, setQuery] = useState('')
  const [addOpen, setAddOpen] = useState(false)

  const playlists = useLibraryStore((state) => state.playlists)
  const favorites = useLibraryStore((state) => state.favorites)
  const removePlaylist = useLibraryStore((state) => state.removePlaylist)
  const playQueue = usePlayerStore((state) => state.playQueue)

  const artists = useLibraryArtists()

  const term = query.trim().toLowerCase()
  const filteredPlaylists = useMemo(
    () => playlists.filter((playlist) => playlist.name.toLowerCase().includes(term)),
    [playlists, term],
  )
  const filteredFavorites = useMemo(
    () =>
      favorites.filter(
        (track) =>
          track.title.toLowerCase().includes(term) || track.artist.toLowerCase().includes(term),
      ),
    [favorites, term],
  )
  const filteredArtists = useMemo(
    () => artists.filter((artist) => artist.name.toLowerCase().includes(term)),
    [artists, term],
  )

  const placeholder =
    tab === 'favorites'
      ? 'Buscar canciones'
      : tab === 'artists'
        ? 'Buscar artistas'
        : 'Buscar listas'

  const showLibraryGrid = tab === 'lists'

  const renderPlaylists = () => {
    if (filteredPlaylists.length === 0 && playlists.length === 0 && favorites.length === 0) {
      return (
        <EmptyState
          text="Aún no tienes listas."
          action={
            <Button size="sm" icon={<PlusIcon size={16} />} onClick={() => setAddOpen(true)}>
              Crear lista
            </Button>
          }
        />
      )
    }
    if (filteredPlaylists.length === 0) return <EmptyState text="Sin resultados." />

    if (view === 'grid') {
      return (
        <div className="grid grid-cols-2 gap-x-5 gap-y-7 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          <FavoritesTile
            count={favorites.length}
            index={0}
            onOpen={() => setTab('favorites')}
          />
          {filteredPlaylists.map((playlist, index) => (
            <PlaylistTile
              key={playlist.id}
              playlist={playlist}
              index={index + 1}
              onOpen={(id) => void navigate(`/playlist/${encodeURIComponent(id)}`)}
            />
          ))}
        </div>
      )
    }

    return (
      <ul className="space-y-2">
        <li>
          <FavoritesListRow count={favorites.length} index={0} onOpen={() => setTab('favorites')} />
        </li>
        {filteredPlaylists.map((playlist, index) => (
          <li key={playlist.id}>
            <PlaylistListRow
              playlist={playlist}
              index={index + 1}
              onOpen={() => void navigate(`/playlist/${encodeURIComponent(playlist.id)}`)}
              onRemove={() => removePlaylist(playlist.id)}
            />
          </li>
        ))}
      </ul>
    )
  }

  return (
    <div
      className="relative"
      style={{
        // Sangra el padding de .desktop-page para que el fondo cálido cubra toda la página.
        margin: '-22px -34px -40px',
        padding: '22px 34px 40px',
        minHeight: 'calc(100% + 62px)',
        backgroundColor: WARM.bg,
      }}
    >
      <header className="pb-6 pt-1">
        <h1 className="text-2xl font-bold tracking-tight text-white">Biblioteca</h1>

        <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
          <div className="no-scrollbar flex flex-wrap items-center gap-2">
            {TABS.map((item) => {
              const active = item.id === tab
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    setTab(item.id)
                    setQuery('')
                  }}
                  className={`inline-flex h-9 items-center rounded-full px-4 text-sm font-medium transition-colors duration-200 ${
                    active ? 'bg-white text-black' : 'text-[#8c8780] hover:text-white'
                  }`}
                  style={{
                    border: `1px solid ${active ? 'transparent' : WARM.line}`,
                    backgroundColor: active ? undefined : WARM.surface,
                  }}
                >
                  {item.label}
                </button>
              )
            })}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <div
              className="flex h-9 w-64 items-center gap-2 rounded-full border px-3.5 transition-colors focus-within:border-white/20"
              style={{ backgroundColor: WARM.surface, borderColor: WARM.line }}
            >
              <SearchIcon size={16} className="shrink-0" style={{ color: WARM.muted }} />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={placeholder}
                className="h-full min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-[#8c8780]"
              />
            </div>

            {showLibraryGrid ? (
              <div
                className="flex items-center gap-0.5 rounded-full border p-0.5"
                style={{ backgroundColor: WARM.surface, borderColor: WARM.line }}
              >
                <button
                  type="button"
                  aria-label="Vista de cuadrícula"
                  onClick={() => setView('grid')}
                  className={`grid h-8 w-8 place-items-center rounded-full transition-colors duration-200 ${
                    view === 'grid'
                      ? 'bg-white text-black'
                      : 'text-[#8c8780] hover:text-white'
                  }`}
                >
                  <GridIcon size={17} />
                </button>
                <button
                  type="button"
                  aria-label="Vista de lista"
                  onClick={() => setView('list')}
                  className={`grid h-8 w-8 place-items-center rounded-full transition-colors duration-200 ${
                    view === 'list'
                      ? 'bg-white text-black'
                      : 'text-[#8c8780] hover:text-white'
                  }`}
                >
                  <ListIcon size={17} />
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </header>

      <AnimatePresence mode="wait">
        <motion.div
          key={tab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.2, ease: EASE }}
        >
          {showLibraryGrid ? renderPlaylists() : null}

          {tab === 'favorites' ? (
            filteredFavorites.length > 0 ? (
              <>
                <div className="mb-3 flex items-center justify-between">
                  <p className="text-xs" style={{ color: WARM.muted }}>
                    {filteredFavorites.length}{' '}
                    {filteredFavorites.length === 1 ? 'canción' : 'canciones'}
                  </p>
                  <Button
                    size="sm"
                    variant="subtle"
                    icon={<PlayIcon size={16} />}
                    onClick={() => void playQueue(filteredFavorites, 0)}
                  >
                    Reproducir
                  </Button>
                </div>
                <ul className="-mx-2 space-y-0.5">
                  {filteredFavorites.map((track, index) => (
                    <li key={`${track.source}-${track.id}-${index}`}>
                      <TrackRow track={track} queue={filteredFavorites} index={index} />
                    </li>
                  ))}
                </ul>
              </>
            ) : (
              <EmptyState
                text={
                  favorites.length === 0
                    ? 'Toca el corazón en cualquier canción para guardarla aquí.'
                    : 'Sin resultados.'
                }
              />
            )
          ) : null}

          {tab === 'artists' ? (
            filteredArtists.length > 0 ? (
              <ul className="space-y-1">
                {filteredArtists.map((artist, index) => (
                  <li key={artist.name}>
                    <motion.button
                      type="button"
                      onClick={() => void playQueue(artist.tracks, 0)}
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.035, duration: 0.3, ease: EASE }}
                      whileHover={{
                        x: 4,
                        transition: { duration: 0.18, ease: EASE },
                      }}
                      className="flex w-full items-center gap-3.5 rounded-2xl px-1 py-2 text-left"
                    >
                      <Artwork
                        src={artist.tracks[0]?.thumbnail}
                        className="h-12 w-12 shrink-0 rounded-full"
                        rounded="rounded-full"
                        iconSize={22}
                      />
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-[0.95rem] font-medium text-white">
                          {artist.name}
                        </span>
                        <span className="mt-0.5 block truncate text-xs" style={{ color: WARM.muted }}>
                          {artist.tracks.length}{' '}
                          {artist.tracks.length === 1 ? 'canción' : 'canciones'}
                        </span>
                      </span>
                      <PlayIcon size={18} className="shrink-0" style={{ color: WARM.muted }} />
                    </motion.button>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState
                text={
                  artists.length === 0
                    ? 'Escucha o guarda música para ver tus artistas.'
                    : 'Sin resultados.'
                }
              />
            )
          ) : null}
        </motion.div>
      </AnimatePresence>

      {/* Botón flotante circular blanco — abre crear/importar playlist. */}
      <motion.button
        type="button"
        aria-label="Añadir a la biblioteca"
        title="Añadir a la biblioteca"
        onClick={() => setAddOpen(true)}
        initial={{ opacity: 0, scale: 0.85 }}
        animate={{ opacity: 1, scale: 1 }}
        whileHover={{ scale: 1.06 }}
        whileTap={{ scale: 0.95 }}
        className="fixed z-40 grid h-14 w-14 place-items-center rounded-full bg-white text-black shadow-[0_12px_34px_-6px_rgba(0,0,0,0.85)]"
        style={{ bottom: 104, right: 34 }}
      >
        <PlusIcon size={26} strokeWidth={2.4} />
      </motion.button>

      <AddToLibraryDialog open={addOpen} onClose={() => setAddOpen(false)} />
    </div>
  )
}

/* ==========================================================================
   Piezas de la rejilla
   ========================================================================== */

function FavoritesTile({
  count,
  index,
  onOpen,
}: {
  count: number
  index: number
  onOpen: () => void
}) {
  return (
    <motion.button
      type="button"
      onClick={onOpen}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.035, duration: 0.3, ease: EASE }}
      whileHover={{ y: -4, scale: 1.02, transition: { duration: 0.18, ease: EASE } }}
      className="group block w-full text-left"
    >
      <div
        className="relative aspect-square w-full overflow-hidden rounded-[10px] border"
        style={{ backgroundColor: WARM.surfaceDeep, borderColor: WARM.line }}
      >
        <div className="absolute inset-0 grid place-items-center">
          <HeartFilledIcon
            size={64}
            className="text-white drop-shadow-[0_8px_18px_rgba(0,0,0,0.5)] transition-transform duration-300 group-hover:scale-110"
          />
        </div>
      </div>
      <p className="mt-2.5 truncate text-sm font-medium text-white">Canciones que te gustan</p>
      <p className="truncate text-xs" style={{ color: WARM.muted }}>
        {count} {count === 1 ? 'canción' : 'canciones'}
      </p>
    </motion.button>
  )
}

function PlaylistCollage({
  playlist,
  coversOverride,
  compact = false,
}: {
  playlist: Playlist
  coversOverride?: string[]
  compact?: boolean
}) {
  const covers = coversOverride ?? coverSources(playlist)

  if (covers.length === 0) {
    return (
      <div className="grid h-full w-full place-items-center" style={{ backgroundColor: WARM.surfaceDeep }}>
        <MusicIcon size={compact ? 18 : 44} style={{ color: WARM.muted }} />
      </div>
    )
  }

  if (covers.length === 1 || compact) {
    return (
      <Artwork
        src={covers[0]}
        className="h-full w-full"
        rounded="rounded-none"
        iconSize={compact ? 18 : 44}
      />
    )
  }

  return (
    <div className="grid h-full w-full grid-cols-2 grid-rows-2 gap-px bg-black/40">
      {covers.map((src, index) => (
        <Artwork
          key={`${src}-${index}`}
          src={src}
          className="h-full w-full"
          rounded="rounded-none"
          iconSize={20}
        />
      ))}
      {Array.from({ length: 4 - covers.length }, (_, index) => (
        <div key={`empty-${index}`} style={{ backgroundColor: WARM.surfaceDeep }} />
      ))}
    </div>
  )
}

function PlaylistTile({
  playlist,
  index,
  onOpen,
}: {
  playlist: Playlist
  index: number
  onOpen: (id: string) => void
}) {
  const playQueue = usePlayerStore((state) => state.playQueue)

  return (
    <motion.button
      type="button"
      onClick={() => onOpen(playlist.id)}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.035, duration: 0.3, ease: EASE }}
      whileHover={{ y: -4, scale: 1.02, transition: { duration: 0.18, ease: EASE } }}
      className="group block w-full text-left"
    >
      <div
        className="relative aspect-square w-full overflow-hidden rounded-[10px] border"
        style={{ borderColor: WARM.line }}
      >
        <PlaylistCollage playlist={playlist} />
        {playlist.tracks.length > 0 ? (
          <span
            role="button"
            tabIndex={0}
            aria-label={`Reproducir ${playlist.name}`}
            onClick={(event) => {
              event.stopPropagation()
              void playQueue(playlist.tracks, 0)
            }}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                event.stopPropagation()
                void playQueue(playlist.tracks, 0)
              }
            }}
            className="absolute bottom-2 right-2 grid h-10 w-10 translate-y-1 place-items-center rounded-full bg-white text-black opacity-0 shadow-lg shadow-black/50 transition-all duration-200 group-hover:translate-y-0 group-hover:opacity-100 focus:translate-y-0 focus:opacity-100 focus:outline-none"
          >
            <PlayIcon size={18} />
          </span>
        ) : null}
      </div>
      <p className="mt-2.5 truncate text-sm font-medium text-white">{playlist.name}</p>
      <p className="truncate text-xs" style={{ color: WARM.muted }}>
        {playlist.tracks.length} {playlist.tracks.length === 1 ? 'canción' : 'canciones'}
      </p>
    </motion.button>
  )
}

/* ==========================================================================
   Filas de la vista lista
   ========================================================================== */

function FavoritesListRow({
  count,
  index,
  onOpen,
}: {
  count: number
  index: number
  onOpen: () => void
}) {
  return (
    <motion.button
      type="button"
      onClick={onOpen}
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.035, duration: 0.25, ease: EASE }}
      className="flex h-16 w-full items-center gap-3 rounded-xl border px-3 text-left transition-colors hover:brightness-125"
      style={{ backgroundColor: WARM.surface, borderColor: WARM.line }}
    >
      <span className="w-5 shrink-0 text-center text-xs tabular-nums" style={{ color: WARM.muted }}>
        {index + 1}
      </span>
      <span
        className="grid h-10 w-10 shrink-0 place-items-center rounded-lg"
        style={{ backgroundColor: WARM.surfaceDeep }}
      >
        <HeartFilledIcon size={19} className="text-white" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-medium text-white">
          Canciones que te gustan
        </span>
        <span className="mt-0.5 block truncate text-xs" style={{ color: WARM.muted }}>
          {count} {count === 1 ? 'canción' : 'canciones'}
        </span>
      </span>
    </motion.button>
  )
}

function PlaylistListRow({
  playlist,
  index,
  onOpen,
  onRemove,
}: {
  playlist: Playlist
  index: number
  onOpen: () => void
  onRemove: () => void
}) {
  const playQueue = usePlayerStore((state) => state.playQueue)
  const covers = coverSources(playlist)
  const totalDuration = playlist.tracks.reduce((sum, track) => sum + (track.duration || 0), 0)

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.035, duration: 0.25, ease: EASE }}
      className="flex h-16 items-center gap-3 rounded-xl border px-3 transition-colors hover:brightness-125"
      style={{ backgroundColor: WARM.surface, borderColor: WARM.line }}
    >
      <button type="button" onClick={onOpen} className="flex min-w-0 flex-1 items-center gap-3 text-left">
        <span className="w-5 shrink-0 text-center text-xs tabular-nums" style={{ color: WARM.muted }}>
          {index + 1}
        </span>
        <span className="h-10 w-10 shrink-0 overflow-hidden rounded-lg">
          <PlaylistCollage playlist={playlist} coversOverride={covers} compact />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-white">{playlist.name}</span>
          <span className="mt-0.5 block truncate text-xs" style={{ color: WARM.muted }}>
            {playlist.tracks.length} {playlist.tracks.length === 1 ? 'canción' : 'canciones'} ·{' '}
            {sourceLabel(playlist.source)}
          </span>
        </span>
      </button>

      {totalDuration > 0 ? (
        <span className="shrink-0 text-xs tabular-nums" style={{ color: WARM.muted }}>
          {formatDuration(totalDuration)}
        </span>
      ) : null}

      {playlist.tracks.length > 0 ? (
        <button
          type="button"
          aria-label={`Reproducir ${playlist.name}`}
          onClick={() => void playQueue(playlist.tracks, 0)}
          className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-white text-black transition-transform hover:scale-105"
        >
          <PlayIcon size={16} />
        </button>
      ) : null}

      <button
        type="button"
        aria-label={`Eliminar ${playlist.name}`}
        onClick={onRemove}
        className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-[#8c8780] transition-colors hover:text-white"
      >
        <TrashIcon size={17} />
      </button>
    </motion.div>
  )
}

/* ==========================================================================
   Utilidades
   ========================================================================== */

function EmptyState({ text, action }: { text: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 py-12 text-center">
      <p className="max-w-[16rem] text-sm" style={{ color: WARM.muted }}>
        {text}
      </p>
      {action}
    </div>
  )
}
