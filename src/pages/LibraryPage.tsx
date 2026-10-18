import { useMemo, useState, type ReactNode } from 'react'
import { Artwork } from '@/components/Artwork'
import {
  GridIcon,
  HeartFilledIcon,
  ListIcon,
  PlayIcon,
  PlusIcon,
  SearchIcon,
} from '@/components/Icons'
import { Button, Portal, Segmented } from '@/components/ui'
import { AddToLibraryDialog } from '@/features/library/AddToLibraryDialog'
import { PlaylistCard } from '@/features/library/PlaylistCard'
import { TrackRow } from '@/features/library/TrackRow'
import { useLibraryArtists } from '@/features/library/useLibraryArtists'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'

type Tab = 'lists' | 'favorites' | 'artists'
type ViewMode = 'grid' | 'list'

export function LibraryPage() {
  const [tab, setTab] = useState<Tab>('lists')
  const [view, setView] = useState<ViewMode>('grid')
  const [query, setQuery] = useState('')
  const [addOpen, setAddOpen] = useState(false)

  const playlists = useLibraryStore((state) => state.playlists)
  const favorites = useLibraryStore((state) => state.favorites)
  const removePlaylist = useLibraryStore((state) => state.removePlaylist)
  const playQueue = usePlayerStore((state) => state.playQueue)
  const hasTrack = usePlayerStore((state) => state.queue.length > 0)

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
    tab === 'lists' ? 'Buscar listas' : tab === 'favorites' ? 'Buscar canciones' : 'Buscar artistas'

  const fabBottom = hasTrack
    ? 'calc(9.25rem + env(safe-area-inset-bottom))'
    : 'calc(5.75rem + env(safe-area-inset-bottom))'

  return (
    <div className="pb-28">
      <header className="px-5 pb-4 pt-6">
        <h1 className="text-[1.7rem] font-semibold tracking-tight">Biblioteca</h1>
      </header>

      <div className="px-5">
        <Segmented
          value={tab}
          onChange={(next) => {
            setTab(next)
            setQuery('')
          }}
          tabs={[
            { id: 'lists', label: 'Listas', count: playlists.length },
            { id: 'favorites', label: 'Favoritos', count: favorites.length },
            { id: 'artists', label: 'Artistas', count: artists.length },
          ]}
        />
      </div>

      <div className="mt-3 flex items-center gap-2 px-5">
        <div className="flex flex-1 items-center gap-2 rounded-xl hairline surface-1 px-3.5 transition-colors focus-within:border-accent/50">
          <SearchIcon size={17} className="text-faint" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={placeholder}
            className="h-10 flex-1 bg-transparent text-sm outline-none placeholder:text-faint"
          />
        </div>

        {tab === 'lists' ? (
          <div className="flex items-center gap-0.5 rounded-xl hairline surface-1 p-0.5">
            <button
              type="button"
              aria-label="Vista de cuadrícula"
              onClick={() => setView('grid')}
              className={`grid h-9 w-9 place-items-center rounded-lg transition-colors ${
                view === 'grid' ? 'bg-accent text-accent-ink' : 'text-faint'
              }`}
            >
              <GridIcon size={18} />
            </button>
            <button
              type="button"
              aria-label="Vista de lista"
              onClick={() => setView('list')}
              className={`grid h-9 w-9 place-items-center rounded-lg transition-colors ${
                view === 'list' ? 'bg-accent text-accent-ink' : 'text-faint'
              }`}
            >
              <ListIcon size={18} />
            </button>
          </div>
        ) : null}
      </div>

      <div className="mt-5 px-5">
        {tab === 'lists' ? (
          filteredPlaylists.length === 0 && playlists.length === 0 ? (
            <EmptyState
              text="Aún no tienes listas."
              action={
                <Button size="sm" icon={<PlusIcon size={16} />} onClick={() => setAddOpen(true)}>
                  Crear lista
                </Button>
              }
            />
          ) : filteredPlaylists.length === 0 ? (
            <EmptyState text="Sin resultados." />
          ) : view === 'grid' ? (
            <div className="grid grid-cols-2 gap-x-4 gap-y-6 sm:grid-cols-3">
              <FavoritesCard count={favorites.length} onOpen={() => setTab('favorites')} />
              {filteredPlaylists.map((playlist) => (
                <PlaylistCard key={playlist.id} playlist={playlist} className="w-full" />
              ))}
            </div>
          ) : (
            <ul className="space-y-1.5">
              <li>
                <FavoritesRow count={favorites.length} onOpen={() => setTab('favorites')} />
              </li>
              {filteredPlaylists.map((playlist) => (
                <li key={playlist.id} className="flex items-center gap-2">
                  <div className="min-w-0 flex-1">
                    <PlaylistCard playlist={playlist} variant="wide" />
                  </div>
                  <button
                    type="button"
                    aria-label={`Eliminar ${playlist.name}`}
                    onClick={() => removePlaylist(playlist.id)}
                    className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-faint tap"
                  >
                    <span className="text-lg leading-none">×</span>
                  </button>
                </li>
              ))}
            </ul>
          )
        ) : null}

        {tab === 'favorites' ? (
          filteredFavorites.length > 0 ? (
            <>
              <div className="mb-3 flex items-center justify-between">
                <p className="text-xs text-faint">
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
              {filteredArtists.map((artist) => (
                <li key={artist.name}>
                  <button
                    type="button"
                    onClick={() => void playQueue(artist.tracks, 0)}
                    className="flex w-full items-center gap-3.5 rounded-2xl px-1 py-2 text-left tap"
                  >
                    <Artwork
                      src={artist.tracks[0]?.thumbnail}
                      className="h-12 w-12 shrink-0 rounded-full"
                      rounded="rounded-full"
                      iconSize={22}
                    />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-[0.95rem] font-medium">{artist.name}</span>
                      <span className="mt-0.5 block truncate text-xs text-faint">
                        {artist.tracks.length}{' '}
                        {artist.tracks.length === 1 ? 'canción' : 'canciones'}
                      </span>
                    </span>
                    <PlayIcon size={18} className="shrink-0 text-faint" />
                  </button>
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
      </div>

      <Portal>
        <button
          type="button"
          aria-label="Crear o importar lista"
          onClick={() => setAddOpen(true)}
          style={{ bottom: fabBottom }}
          className="fixed right-5 z-40 grid h-14 w-14 place-items-center rounded-full bg-ink text-black shadow-[0_16px_36px_-12px_rgba(0,0,0,0.9)] transition-transform active:scale-95"
        >
          <PlusIcon size={26} strokeWidth={2.4} />
        </button>
      </Portal>

      <AddToLibraryDialog open={addOpen} onClose={() => setAddOpen(false)} />
    </div>
  )
}

function EmptyState({ text, action }: { text: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 py-12 text-center">
      <p className="max-w-[16rem] text-sm text-faint">{text}</p>
      {action}
    </div>
  )
}

function FavoritesCard({ count, onOpen }: { count: number; onOpen: () => void }) {
  return (
    <button type="button" onClick={onOpen} className="group block w-full text-left">
      <div className="relative grid aspect-square w-full place-items-center rounded-2xl bg-gradient-to-br from-accent/30 to-accent/5 hairline">
        <HeartFilledIcon size={46} className="text-accent" />
      </div>
      <p className="mt-2.5 truncate text-sm font-semibold">Me gusta</p>
      <p className="truncate text-xs text-faint">
        {count} {count === 1 ? 'canción' : 'canciones'}
      </p>
    </button>
  )
}

function FavoritesRow({ count, onOpen }: { count: number; onOpen: () => void }) {
  return (
    <button
      type="button"
      onClick={onOpen}
      className="flex w-full items-center gap-3 rounded-2xl surface-1 p-2 text-left tap"
    >
      <span className="grid h-14 w-14 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-accent/30 to-accent/5">
        <HeartFilledIcon size={24} className="text-accent" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate font-medium">Me gusta</span>
        <span className="mt-0.5 block text-xs text-faint">
          {count} {count === 1 ? 'canción' : 'canciones'}
        </span>
      </span>
    </button>
  )
}
