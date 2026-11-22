import { Artwork } from '@/components/Artwork'
import { PauseIcon, PlayIcon } from '@/components/Icons'
import { PlayingBars } from '@/components/ui'
import { sameTrack } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import type { Track } from '@/shared/types/track'

interface TrackCardProps {
  track: Track
  queue: Track[]
  index: number
  width?: string
}

/** Tarjeta grande para filas horizontales de sugerencias. */
export function TrackCard({ track, queue, index, width = 'w-[9.5rem]' }: TrackCardProps) {
  const playTrack = usePlayerStore((state) => state.playTrack)
  const currentTrack = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const toggle = usePlayerStore((state) => state.toggle)

  const isCurrent = currentTrack ? sameTrack(currentTrack, track) : false
  const active = isCurrent && isPlaying

  const handlePlay = () => {
    if (isCurrent) toggle()
    else void playTrack(track, queue)
  }

  return (
    <div className={`group relative shrink-0 ${width}`}>
      <button
        type="button"
        onClick={handlePlay}
        aria-label={active ? `Pausar ${track.title}` : `Reproducir ${track.title}`}
        className="relative block w-full text-left"
      >
        <span className="relative block overflow-hidden rounded-2xl ring-1 ring-inset ring-white/5">
          <Artwork
            src={track.thumbnail}
            alt={track.title}
            className="aspect-square w-full transition-transform duration-300 group-hover:scale-[1.04]"
            rounded="rounded-2xl"
            iconSize={36}
          />
          <span className="pointer-events-none absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-black/55 to-transparent" />
          <span
            className={`absolute bottom-2.5 right-2.5 grid h-11 w-11 place-items-center rounded-full bg-[color:var(--color-accent)] text-[color:var(--color-accent-ink)] ring-1 ring-inset ring-white/25 shadow-[0_10px_24px_-8px_rgba(0,0,0,0.85)] backdrop-blur transition-all duration-200 active:scale-90 ${
              isCurrent ? 'opacity-100' : 'opacity-0 group-hover:opacity-100 max-md:opacity-100'
            }`}
          >
            {active ? <PauseIcon size={17} /> : <PlayIcon size={17} />}
          </span>
          {active ? (
            <span className="absolute left-2.5 top-2.5 grid h-6 w-6 place-items-center rounded-full bg-black/45 text-accent backdrop-blur">
              <PlayingBars className="scale-[0.6]" />
            </span>
          ) : null}
        </span>
      </button>

      <div className="mt-2.5 flex items-center gap-1.5">
        {isCurrent ? (
          <span className="shrink-0 text-accent">
            <PlayingBars className={active ? '' : 'opacity-40'} />
          </span>
        ) : null}
        <p className={`min-w-0 truncate text-sm font-semibold ${isCurrent ? 'text-accent' : ''}`}>
          {track.title}
        </p>
      </div>
      <p className="truncate text-xs text-faint">{track.artist}</p>
    </div>
  )
}
