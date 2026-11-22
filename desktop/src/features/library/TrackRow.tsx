import { useState, type ReactNode } from 'react'
import { Artwork } from '@/components/Artwork'
import {
  HeartFilledIcon,
  HeartIcon,
  ListMusicIcon,
  MoreIcon,
  NextIcon,
  PauseIcon,
  PlayIcon,
  PlusIcon,
  TrashIcon,
} from '@/components/Icons'
import { Button, PlayingBars, Sheet } from '@/components/ui'
import { usePlayerStore } from '@/shared/store/playerStore'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { useUiStore } from '@/shared/store/uiStore'
import type { Track } from '@/shared/types/track'
import { formatDuration } from '@/shared/utils/time'

interface TrackRowProps {
  track: Track
  queue: Track[]
  index?: number
  showArtwork?: boolean
  showDuration?: boolean
  onRemove?: () => void
}

interface ActionProps {
  icon: ReactNode
  label: string
  onClick: () => void
  tone?: 'default' | 'danger'
}

function Action({ icon, label, onClick, tone = 'default' }: ActionProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex w-full items-center gap-3 rounded-xl px-2 py-3 text-left text-sm font-medium tap ${
        tone === 'danger' ? 'text-accent' : 'text-ink'
      }`}
    >
      <span className={tone === 'danger' ? 'text-accent' : 'text-muted'}>{icon}</span>
      {label}
    </button>
  )
}

/** Menú de acciones de una pista (⋮). */
export function TrackMenu({
  track,
  queue,
  onRemove,
  onClose,
}: {
  track: Track
  queue: Track[]
  onRemove?: () => void
  onClose: () => void
}) {
  const [view, setView] = useState<'root' | 'playlists'>('root')
  const [newName, setNewName] = useState('')

  const playTrack = usePlayerStore((state) => state.playTrack)
  const playNext = usePlayerStore((state) => state.playNext)
  const enqueue = usePlayerStore((state) => state.enqueue)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const currentTrack = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const toggle = usePlayerStore((state) => state.toggle)

  const playlists = useLibraryStore((state) => state.playlists)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)
  const addTracksToPlaylist = useLibraryStore((state) => state.addTracksToPlaylist)
  const createPlaylist = useLibraryStore((state) => state.createPlaylist)
  const favorites = useLibraryStore((state) => state.favorites)
  const notify = useUiStore((state) => state.notify)

  const isCurrent = currentTrack ? sameTrack(currentTrack, track) : false
  const isFavorite = favorites.some((item) => sameTrack(item, track))

  const run = (fn: () => void) => {
    fn()
    onClose()
  }

  return (
    <Sheet open onClose={onClose} title={view === 'root' ? track.title : 'Añadir a una lista'}>
      {view === 'root' ? (
        <div className="space-y-0.5 pb-2">
          <Action
            icon={isCurrent && isPlaying ? <PauseIcon size={20} /> : <PlayIcon size={20} />}
            label={isCurrent && isPlaying ? 'Pausar' : 'Reproducir'}
            onClick={() => run(() => (isCurrent ? toggle() : void playTrack(track, queue)))}
          />
          <Action
            icon={<NextIcon size={20} />}
            label="Reproducir a continuación"
            onClick={() => run(() => playNext(track))}
          />
          <Action
            icon={<ListMusicIcon size={20} />}
            label="Añadir a la cola"
            onClick={() => run(() => enqueue(track))}
          />
          <Action
            icon={isFavorite ? <HeartFilledIcon size={20} /> : <HeartIcon size={20} />}
            label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
            onClick={() => run(() => toggleFavorite(track))}
          />
          <Action
            icon={<PlusIcon size={20} />}
            label="Añadir a una lista"
            onClick={() => setView('playlists')}
          />
          {onRemove ? (
            <Action
              icon={<TrashIcon size={20} />}
              label="Eliminar de esta lista"
              tone="danger"
              onClick={() => run(onRemove)}
            />
          ) : null}
        </div>
      ) : (
        <div className="pb-2">
          <div className="flex items-center gap-2 pb-2">
            <input
              value={newName}
              maxLength={40}
              onChange={(event) => setNewName(event.target.value)}
              placeholder="Nueva lista…"
              className="hairline surface-1 h-11 flex-1 rounded-xl px-3.5 text-sm outline-none focus:border-accent/50"
            />
            <Button
              size="sm"
              disabled={!newName.trim()}
              onClick={() => {
                const playlist = createPlaylist(newName.trim())
                addTracksToPlaylist(playlist.id, [track])
                notify(`Añadido a «${playlist.name}»`)
                onClose()
              }}
            >
              Crear
            </Button>
          </div>
          {playlists.length > 0 ? (
            <div className="space-y-0.5">
              {playlists.map((playlist) => (
                <Action
                  key={playlist.id}
                  icon={<ListMusicIcon size={20} />}
                  label={`${playlist.name} · ${playlist.tracks.length}`}
                  onClick={() =>
                    run(() => {
                      addTracksToPlaylist(playlist.id, [track])
                      notify(`Añadido a «${playlist.name}»`)
                    })
                  }
                />
              ))}
            </div>
          ) : (
            <p className="px-2 py-3 text-sm text-faint">
              Aún no tienes listas. Crea una arriba.
            </p>
          )}
        </div>
      )}
    </Sheet>
  )
}

/** Fila de pista reutilizable: pulsa para reproducir, ⋮ para más acciones. */
export function TrackRow({
  track,
  queue,
  index,
  showArtwork = true,
  showDuration = true,
  onRemove,
}: TrackRowProps) {
  const [menuOpen, setMenuOpen] = useState(false)

  const playTrack = usePlayerStore((state) => state.playTrack)
  const currentTrack = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const toggle = usePlayerStore((state) => state.toggle)

  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  const isCurrent = currentTrack ? sameTrack(currentTrack, track) : false
  const isFavorite = favorites.some((item) => sameTrack(item, track))

  const handlePlay = () => {
    if (isCurrent) {
      toggle()
    } else {
      void playTrack(track, queue)
    }
  }

  return (
    <>
      <div
        className={`flex items-center gap-3 rounded-xl px-2 py-1.5 transition-colors ${
          isCurrent ? 'bg-[color:var(--overlay-2)]' : 'tap'
        }`}
      >
        <button
          type="button"
          onClick={handlePlay}
          className="flex min-w-0 flex-1 items-center gap-3 text-left"
        >
          {showArtwork ? (
            <span className="relative shrink-0">
              <Artwork src={track.thumbnail} className="h-12 w-12" rounded="rounded-lg" />
              {isCurrent ? (
                <span className="absolute inset-0 grid place-items-center rounded-lg bg-black/55 text-white">
                  {isPlaying ? <PlayingBars className="text-accent" /> : <PlayIcon size={18} />}
                </span>
              ) : null}
            </span>
          ) : (
            <span className="w-6 shrink-0 text-center text-sm text-faint">
              {index !== undefined ? index + 1 : ''}
            </span>
          )}

          <span className="min-w-0 flex-1">
            <span
              className={`block truncate text-[0.95rem] font-semibold ${
                isCurrent ? 'text-accent' : 'text-ink'
              }`}
            >
              {track.title}
            </span>
            <span className="mt-0.5 block truncate text-xs text-faint">{track.artist}</span>
          </span>
        </button>

        {showDuration && track.duration > 0 ? (
          <span className="shrink-0 text-xs tabular-nums text-faint">
            {formatDuration(track.duration)}
          </span>
        ) : null}

        <button
          type="button"
          aria-label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
          onClick={() => toggleFavorite(track)}
          className={`shrink-0 p-1.5 transition-colors ${
            isFavorite ? 'text-accent' : 'text-faint hover:text-muted'
          }`}
        >
          {isFavorite ? <HeartFilledIcon size={19} /> : <HeartIcon size={19} />}
        </button>

        <button
          type="button"
          aria-label="Más acciones"
          onClick={() => setMenuOpen(true)}
          className="shrink-0 p-1.5 text-faint transition-colors hover:text-muted"
        >
          <MoreIcon size={20} />
        </button>
      </div>

      {menuOpen ? (
        <TrackMenu
          track={track}
          queue={queue}
          onRemove={onRemove}
          onClose={() => setMenuOpen(false)}
        />
      ) : null}
    </>
  )
}
