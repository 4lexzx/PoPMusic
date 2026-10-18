import { Link } from 'react-router-dom'
import { Artwork } from '@/components/Artwork'
import { NextIcon, PauseIcon, PlayIcon } from '@/components/Icons'
import { IconButton, Spinner } from '@/components/ui'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'

/** Barra compacta de vidrio flotante sobre la navegación inferior. */
export function MiniPlayer() {
  const track = usePlayerStore(selectCurrentTrack)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const isBusy = usePlayerStore((state) => state.isLoadingTrack || state.isBuffering)
  const position = usePlayerStore((state) => state.position)
  const duration = usePlayerStore((state) => state.duration)
  const toggle = usePlayerStore((state) => state.toggle)
  const next = usePlayerStore((state) => state.next)

  if (!track) return null

  const progress = duration > 0 ? Math.min(100, (position / duration) * 100) : 0

  return (
    <div
      className="fixed inset-x-0 z-30 px-3"
      style={{ bottom: 'calc(4.75rem + env(safe-area-inset-bottom))' }}
    >
      <div className="mx-auto max-w-md">
        <div className="glass-panel group relative overflow-hidden rounded-[1.5rem] pr-24">
          <Link to="/now-playing" className="flex items-center gap-3 p-2.5">
            <Artwork src={track.thumbnail} className="h-11 w-11 shrink-0" rounded="rounded-xl" />
            <span className="min-w-0 flex-1">
              <span className="block truncate text-[0.9rem] font-medium">{track.title}</span>
              <span className="mt-0.5 block truncate text-xs text-faint">{track.artist}</span>
            </span>
          </Link>

          <div className="absolute right-2 top-1/2 flex -translate-y-1/2 items-center gap-1">
            <IconButton label={isPlaying ? 'Pausar' : 'Reproducir'} onClick={toggle}>
              {isBusy ? (
                <Spinner size={20} />
              ) : isPlaying ? (
                <PauseIcon size={22} />
              ) : (
                <PlayIcon size={22} />
              )}
            </IconButton>
            <IconButton label="Siguiente" onClick={() => next()}>
              <NextIcon size={22} />
            </IconButton>
          </div>

          <div className="absolute inset-x-0 bottom-0 h-[3px] bg-[color:var(--overlay-2)]">
            <div
              className="progress-fill h-full"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
