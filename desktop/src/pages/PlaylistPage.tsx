import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import { Artwork } from '@/components/Artwork'
import {
  CheckIcon,
  ChevronDownIcon,
  ChevronLeftIcon,
  CloseIcon,
  HeartFilledIcon,
  HeartIcon,
  ListMusicIcon,
  MoreIcon,
  MusicIcon,
  PlayIcon,
  PlusIcon,
  SearchIcon,
  ShuffleIcon,
  TrashIcon,
} from '@/components/Icons'
import { Button, IconButton, PlayingBars } from '@/components/ui'
import { AddTracksToPlaylistSheet } from '@/features/library/AddTracksToPlaylistSheet'
import { PlaylistCover } from '@/features/library/PlaylistCard'
import { TrackMenu } from '@/features/library/TrackRow'
import { useArtworkColors } from '@/features/visuals/DynamicBackground'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'
import type { Track } from '@/shared/types/track'
import { formatDuration } from '@/shared/utils/time'

const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1]

type SortMode = 'default' | 'title' | 'artist' | 'duration'

const SORT_OPTIONS: Array<{ id: SortMode; label: string }> = [
  { id: 'default', label: 'Predeterminado' },
  { id: 'title', label: 'Título (A–Z)' },
  { id: 'artist', label: 'Artista (A–Z)' },
  { id: 'duration', label: 'Duración (mayor a menor)' },
]

interface TrackEntry {
  track: Track
  /** Índice original en la playlist (lo usa removeTrackFromPlaylist). */
  index: number
}

export function PlaylistPage() {
  const params = useParams<{ id: string }>()
  const navigate = useNavigate()
  const reduceMotion = useReducedMotion()

  const [query, setQuery] = useState('')
  const [addOpen, setAddOpen] = useState(false)
  const [sort, setSort] = useState<SortMode>('default')
  const [openMenu, setOpenMenu] = useState<'sort' | 'more' | null>(null)
  const [menuIndex, setMenuIndex] = useState<number | null>(null)

  const playlistId = params.id ? decodeURIComponent(params.id) : ''
  const playlists = useLibraryStore((state) => state.playlists)
  const removePlaylist = useLibraryStore((state) => state.removePlaylist)
  const removeTrackFromPlaylist = useLibraryStore((state) => state.removeTrackFromPlaylist)
  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  const playQueue = usePlayerStore((state) => state.playQueue)
  const playTrack = usePlayerStore((state) => state.playTrack)
  const toggle = usePlayerStore((state) => state.toggle)
  const enqueue = usePlayerStore((state) => state.enqueue)
  const currentTrack = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const isPlaying = usePlayerStore((state) => state.isPlaying)

  const notify = useUiStore((state) => state.notify)

  const playlist = playlists.find((item) => item.id === playlistId)
  const colors = useArtworkColors(playlist?.thumbnail)

  // Ordenamos conservando el índice original para poder eliminar la pista correcta.
  const sortedEntries = useMemo<TrackEntry[]>(() => {
    const entries = (playlist?.tracks ?? []).map((track, index) => ({ track, index }))
    if (sort === 'title') {
      entries.sort((a, b) => a.track.title.localeCompare(b.track.title, 'es'))
    } else if (sort === 'artist') {
      entries.sort(
        (a, b) =>
          a.track.artist.localeCompare(b.track.artist, 'es') ||
          a.track.title.localeCompare(b.track.title, 'es'),
      )
    } else if (sort === 'duration') {
      entries.sort((a, b) => b.track.duration - a.track.duration)
    }
    return entries
  }, [playlist, sort])

  const visibleEntries = useMemo(() => {
    const term = query.trim().toLocaleLowerCase('es')
    if (!term) return sortedEntries
    return sortedEntries.filter(({ track }) =>
      `${track.title} ${track.artist}`.toLocaleLowerCase('es').includes(term),
    )
  }, [sortedEntries, query])

  const sortedTracks = sortedEntries.map((entry) => entry.track)

  if (!playlist) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-8 text-center text-faint">
        <p className="text-sm">No se encontró esta playlist.</p>
        <Button variant="subtle" onClick={() => navigate('/library')}>
          Volver a la biblioteca
        </Button>
      </div>
    )
  }

  const totalDuration = playlist.tracks.reduce((sum, track) => sum + (track.duration || 0), 0)
  const isLocal = playlist.source === 'local'
  const year = playlist.importedAt > 0 ? new Date(playlist.importedAt).getFullYear() : null
  const sourceLabel =
    playlist.source === 'youtube' ? 'YouTube' : playlist.source === 'spotify' ? 'Spotify' : 'Local'
  const sortLabel = SORT_OPTIONS.find((option) => option.id === sort)?.label ?? 'Predeterminado'
  const menuEntry = menuIndex !== null ? sortedEntries[menuIndex] : undefined

  const handleShuffle = () => {
    const shuffled = [...sortedTracks]
    for (let index = shuffled.length - 1; index > 0; index -= 1) {
      const swap = Math.floor(Math.random() * (index + 1))
      ;[shuffled[index], shuffled[swap]] = [shuffled[swap], shuffled[index]]
    }
    void playQueue(shuffled, 0)
  }

  const handleAddToQueue = () => {
    if (sortedTracks.length === 0) return
    sortedTracks.forEach((track) => enqueue(track))
    notify(
      `${sortedTracks.length} ${sortedTracks.length === 1 ? 'canción añadida' : 'canciones añadidas'} a la cola`,
    )
  }

  const handlePlayRow = (track: Track) => {
    if (currentTrack && sameTrack(currentTrack, track)) {
      toggle()
    } else {
      void playTrack(track, sortedTracks)
    }
  }

  const handleRemovePlaylist = () => {
    removePlaylist(playlist.id)
    navigate('/library')
  }

  return (
    <div className="relative isolate min-h-[70vh] pb-10">
      {/* El color de la portada tiñe el fondo del hero ("content is the colour"). */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-[520px] overflow-hidden"
      >
        <motion.div
          className="absolute -left-24 -top-28 h-[360px] w-[360px] rounded-full blur-[100px]"
          style={{ opacity: 0.34 }}
          animate={{ backgroundColor: colors.primary }}
          transition={{ duration: reduceMotion ? 0 : 0.7, ease: EASE }}
        />
        <motion.div
          className="absolute -right-20 -top-10 h-[320px] w-[320px] rounded-full blur-[110px]"
          style={{ opacity: 0.26 }}
          animate={{ backgroundColor: colors.secondary }}
          transition={{ duration: reduceMotion ? 0 : 0.7, ease: EASE }}
        />
        <div
          className="absolute inset-0"
          style={{
            background: `linear-gradient(180deg, color-mix(in srgb, ${colors.primary} 20%, transparent) 0%, transparent 70%), linear-gradient(180deg, transparent 55%, var(--color-bg) 100%)`,
          }}
        />
      </div>

      {openMenu ? (
        <button
          type="button"
          aria-label="Cerrar menú"
          className="fixed inset-0 z-20 cursor-default"
          onClick={() => setOpenMenu(null)}
        />
      ) : null}

      {/* Barra superior */}
      <header className="relative z-10 flex items-center justify-between px-4 pt-[calc(0.75rem+env(safe-area-inset-top))]">
        <motion.div
          initial={{ opacity: 0, x: -12 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: reduceMotion ? 0 : 0.3, ease: EASE }}
        >
          <IconButton label="Volver" onClick={() => navigate(-1)}>
            <ChevronLeftIcon size={22} />
          </IconButton>
        </motion.div>
        <div className="flex items-center gap-1">
          <IconButton label="Eliminar playlist" onClick={handleRemovePlaylist}>
            <TrashIcon size={19} />
          </IconButton>
          <div className="relative">
            <IconButton
              label="Más opciones"
              onClick={() => setOpenMenu((value) => (value === 'more' ? null : 'more'))}
            >
              <MoreIcon size={20} />
            </IconButton>
            {openMenu === 'more' ? (
              <div className="absolute right-0 top-full z-30 mt-2 w-56 glass-panel rounded-2xl p-1.5 shadow-2xl">
                <button
                  type="button"
                  onClick={() => {
                    setAddOpen(true)
                    setOpenMenu(null)
                  }}
                  className="flex w-full items-center gap-3 rounded-xl px-2.5 py-2.5 text-left text-[13px] text-ink tap"
                >
                  <PlusIcon size={17} className="text-faint" /> Añadir canciones
                </button>
                <button
                  type="button"
                  onClick={handleRemovePlaylist}
                  className="flex w-full items-center gap-3 rounded-xl px-2.5 py-2.5 text-left text-[13px] text-accent tap"
                >
                  <TrashIcon size={17} /> Eliminar playlist
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="relative z-10 px-5 pb-2 pt-5 sm:px-8">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-end">
          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: reduceMotion ? 0 : 0.45, ease: EASE }}
            className="h-[168px] w-[168px] shrink-0 overflow-hidden rounded-xl shadow-[0_25px_55px_-18px_rgba(0,0,0,0.9)] ring-1 ring-white/10"
          >
            <PlaylistCover playlist={playlist} className="h-full w-full" />
          </motion.div>

          <motion.div
            className="min-w-0 flex-1"
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: reduceMotion ? 0 : 0.42, delay: reduceMotion ? 0 : 0.08, ease: EASE }}
          >
            <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-white/55">
              Playlist · {sourceLabel}
            </p>
            <h1 className="mt-1.5 text-3xl font-semibold leading-tight tracking-tight text-ink">
              {playlist.name}
            </h1>
            {playlist.description ? (
              <p className="mt-2 line-clamp-2 max-w-xl text-[13px] leading-relaxed text-muted">
                {playlist.description}
              </p>
            ) : null}
            <div className="mt-3 flex flex-wrap items-center gap-2">
              {year !== null ? (
                <span className="glass-pill rounded-full px-2.5 py-1 text-[11px] font-medium">{year}</span>
              ) : null}
              <span className="glass-pill rounded-full px-2.5 py-1 text-[11px] font-medium">
                {playlist.tracks.length} {playlist.tracks.length === 1 ? 'canción' : 'canciones'}
              </span>
              {totalDuration > 0 ? (
                <span className="glass-pill rounded-full px-2.5 py-1 text-[11px] font-medium">
                  {formatDuration(totalDuration)}
                </span>
              ) : null}
            </div>

            <div className="mt-5 flex flex-wrap items-center gap-2.5">
              <Button
                icon={<PlayIcon size={17} />}
                disabled={playlist.tracks.length === 0}
                onClick={() => void playQueue(sortedTracks, 0)}
                className="h-11 rounded-full px-5"
              >
                Reproducir todo
              </Button>
              <IconButton
                label="Aleatorio"
                disabled={playlist.tracks.length === 0}
                onClick={handleShuffle}
                className="glass h-11 w-11"
                style={{ color: 'var(--color-ink)' }}
              >
                <ShuffleIcon size={18} />
              </IconButton>
              <div className="mx-1 hidden h-6 w-px bg-white/10 sm:block" />
              <div className="glass flex h-10 min-w-0 flex-1 items-center gap-2 rounded-full px-3.5 sm:max-w-[240px]">
                <SearchIcon size={16} className="shrink-0 text-faint" />
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Buscar en la playlist"
                  aria-label="Buscar en la playlist"
                  className="h-10 min-w-0 flex-1 bg-transparent text-[13px] text-ink outline-none placeholder:text-faint"
                />
                {query ? (
                  <button
                    type="button"
                    aria-label="Limpiar búsqueda"
                    onClick={() => setQuery('')}
                    className="shrink-0 text-faint transition-colors hover:text-ink"
                  >
                    <CloseIcon size={15} />
                  </button>
                ) : null}
              </div>
              <button
                type="button"
                disabled={playlist.tracks.length === 0}
                onClick={handleAddToQueue}
                className="glass flex h-10 items-center gap-2 rounded-full px-3.5 text-xs font-medium text-ink transition-colors hover:bg-white/10 disabled:opacity-40 tap"
              >
                <ListMusicIcon size={16} /> Añadir a la cola
              </button>
              <div className="relative">
                <button
                  type="button"
                  aria-label="Ordenar canciones"
                  onClick={() => setOpenMenu((value) => (value === 'sort' ? null : 'sort'))}
                  className="glass flex h-10 items-center gap-2 rounded-full px-3.5 text-xs font-medium text-ink tap"
                >
                  Ordenar
                  <ChevronDownIcon
                    size={14}
                    className={openMenu === 'sort' ? 'rotate-180 transition-transform' : 'transition-transform'}
                  />
                </button>
                {openMenu === 'sort' ? (
                  <div className="absolute left-0 top-full z-30 mt-2 w-60 glass-panel rounded-2xl p-1.5 shadow-2xl">
                    <p className="px-2.5 pb-1 pt-1.5 text-[10px] font-semibold uppercase tracking-[0.14em] text-faint">
                      Ordenar por
                    </p>
                    {SORT_OPTIONS.map((option) => (
                      <button
                        key={option.id}
                        type="button"
                        onClick={() => {
                          setSort(option.id)
                          setOpenMenu(null)
                        }}
                        className={`flex w-full items-center justify-between rounded-xl px-2.5 py-2.5 text-left text-[13px] tap ${
                          sort === option.id ? 'bg-white/10 text-ink' : 'text-muted'
                        }`}
                      >
                        {option.label}
                        {sort === option.id ? <CheckIcon size={15} className="text-white/70" /> : null}
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>
            </div>
            <p className="mt-2 text-[11px] text-faint">Orden actual: {sortLabel}</p>
          </motion.div>
        </div>
      </section>

      {/* Lista de canciones */}
      <section className="relative z-10 mt-5 px-3 sm:px-8">
        {playlist.tracks.length === 0 ? (
          <div className="glass-panel flex flex-col items-center gap-3 rounded-[20px] px-6 py-14 text-center">
            <span className="grid h-14 w-14 place-items-center rounded-2xl surface-2 text-faint">
              <MusicIcon size={26} />
            </span>
            <div>
              <p className="text-sm font-medium text-ink">Esta playlist no tiene canciones.</p>
              <p className="mt-1 text-xs text-faint">Añade tu primera canción para empezar.</p>
            </div>
            <Button icon={<PlusIcon size={17} />} onClick={() => setAddOpen(true)}>
              Añadir canciones
            </Button>
          </div>
        ) : visibleEntries.length === 0 ? (
          <div className="glass-panel flex flex-col items-center gap-3 rounded-[20px] px-6 py-14 text-center">
            <SearchIcon size={26} className="text-faint" />
            <p className="text-sm text-muted">No se encontraron canciones para «{query}».</p>
            <Button variant="subtle" size="sm" onClick={() => setQuery('')}>
              Limpiar búsqueda
            </Button>
          </div>
        ) : (
          <div className="glass-panel overflow-hidden rounded-[20px] px-1.5 py-1.5 sm:px-2">
            <ul>
              {visibleEntries.map(({ track, index }, rowIndex) => {
                const isCurrent = currentTrack ? sameTrack(currentTrack, track) : false
                const isFavorite = favorites.some((item) => sameTrack(item, track))
                return (
                  <motion.li
                    key={`${track.source}-${track.id}-${index}`}
                    layout="position"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{
                      duration: reduceMotion ? 0 : 0.32,
                      delay: reduceMotion ? 0 : Math.min(rowIndex * 0.025, 0.25),
                      ease: EASE,
                    }}
                  >
                    <div
                      className={`group flex h-16 items-center gap-3 rounded-2xl px-2 transition-colors sm:px-3 ${
                        isCurrent ? 'bg-white/[0.07]' : 'tap'
                      }`}
                    >
                      <button
                        type="button"
                        aria-label={`Reproducir ${track.title}`}
                        onClick={() => handlePlayRow(track)}
                        className="flex w-6 shrink-0 items-center justify-center text-xs tabular-nums text-faint"
                      >
                        {isCurrent && isPlaying ? (
                          <PlayingBars className="text-accent-soft" />
                        ) : (
                          <span className={isCurrent ? 'text-white/75' : ''}>{String(index + 1).padStart(2, '0')}</span>
                        )}
                      </button>
                      <button
                        type="button"
                        onClick={() => handlePlayRow(track)}
                        className="flex min-w-0 flex-1 items-center gap-3 text-left"
                      >
                        <Artwork
                          src={track.thumbnail}
                          alt=""
                          className="h-9 w-9 shrink-0"
                          rounded="rounded-lg"
                          iconSize={16}
                        />
                        <span className="min-w-0 flex-1">
                          <span
                            className={`block truncate text-sm font-medium ${
                              isCurrent ? 'text-white/85' : 'text-ink'
                            }`}
                          >
                            {track.title}
                          </span>
                          <span className="mt-0.5 block truncate text-xs text-faint">{track.artist}</span>
                        </span>
                      </button>
                      <span className="w-10 shrink-0 text-right text-xs tabular-nums text-faint">
                        {track.duration > 0 ? formatDuration(track.duration) : '—'}
                      </span>
                      <button
                        type="button"
                        aria-label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
                        onClick={() => toggleFavorite(track)}
                        className={`grid h-8 w-8 shrink-0 place-items-center rounded-full transition-colors ${
                          isFavorite ? 'text-accent' : 'text-faint hover:text-ink'
                        }`}
                      >
                        {isFavorite ? <HeartFilledIcon size={17} /> : <HeartIcon size={17} />}
                      </button>
                      <button
                        type="button"
                        aria-label="Más acciones"
                        onClick={() => setMenuIndex(index)}
                        className="grid h-8 w-8 shrink-0 place-items-center rounded-full text-faint transition-colors hover:text-ink"
                      >
                        <MoreIcon size={18} />
                      </button>
                    </div>
                  </motion.li>
                )
              })}
            </ul>
          </div>
        )}
      </section>

      {menuEntry ? (
        <TrackMenu
          track={menuEntry.track}
          queue={sortedTracks}
          onRemove={isLocal ? () => removeTrackFromPlaylist(playlist.id, menuEntry.index) : undefined}
          onClose={() => setMenuIndex(null)}
        />
      ) : null}

      <AddTracksToPlaylistSheet
        playlistId={playlist.id}
        open={addOpen}
        onClose={() => setAddOpen(false)}
      />
    </div>
  )
}
