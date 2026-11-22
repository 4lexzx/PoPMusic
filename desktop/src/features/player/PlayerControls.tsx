import { useState } from 'react'
import {
  NextIcon,
  PauseIcon,
  PlayIcon,
  PrevIcon,
  RepeatIcon,
  RepeatOneIcon,
  ShuffleIcon,
  VolumeIcon,
  VolumeMuteIcon,
} from '@/components/Icons'
import { IconButton, Slider, Spinner } from '@/components/ui'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'
import { formatDuration } from '@/shared/utils/time'

interface ProgressBarProps {
  position: number
  duration: number
  onSeek: (seconds: number) => void
}

/** Barra de progreso con previsualización mientras se arrastra. */
export function ProgressBar({ position, duration, onSeek }: ProgressBarProps) {
  const [scrub, setScrub] = useState<number | null>(null)
  const value = duration > 0 ? position / duration : 0
  const shown = scrub ?? value

  return (
    <div className="w-full">
      <Slider
        value={shown}
        ariaLabel="Progreso de la canción"
        trackClassName="h-1.5 bg-[color:var(--overlay-3)]"
        fillClassName="progress-fill"
        thumbClassName="progress-thumb"
        onChange={(next) => setScrub(next)}
        onCommit={(next) => {
          setScrub(null)
          if (duration > 0) onSeek(next * duration)
        }}
      />
      <div className="mt-0.5 flex justify-between text-[11px] tabular-nums text-faint">
        <span>{formatDuration(shown * duration)}</span>
        <span>-{formatDuration(Math.max(0, duration - shown * duration))}</span>
      </div>
    </div>
  )
}

/** Controles de reproducción principales. */
export function PlayerControls() {
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const isBusy = usePlayerStore((state) => state.isLoadingTrack || state.isBuffering)
  const shuffle = usePlayerStore((state) => state.shuffle)
  const repeat = usePlayerStore((state) => state.repeat)
  const hasTrack = usePlayerStore((state) => state.queue.length > 0)

  const toggle = usePlayerStore((state) => state.toggle)
  const next = usePlayerStore((state) => state.next)
  const prev = usePlayerStore((state) => state.prev)
  const toggleShuffle = usePlayerStore((state) => state.toggleShuffle)
  const cycleRepeat = usePlayerStore((state) => state.cycleRepeat)

  return (
    <div className="flex items-center justify-between">
      <IconButton label="Aleatorio" onClick={toggleShuffle} active={shuffle}>
        <ShuffleIcon size={22} />
      </IconButton>

      <IconButton label="Anterior" onClick={() => prev()} disabled={!hasTrack} size="lg">
        <PrevIcon size={28} />
      </IconButton>

      <button
        type="button"
        onClick={toggle}
        disabled={!hasTrack}
        aria-label={isPlaying ? 'Pausar' : 'Reproducir'}
        className="flex h-16 w-16 items-center justify-center rounded-full bg-ink text-bg shadow-xl shadow-black/40 transition-transform active:scale-95 disabled:opacity-50"
      >
        {isBusy ? <Spinner size={26} className="text-bg" /> : isPlaying ? <PauseIcon size={30} /> : <PlayIcon size={30} />}
      </button>

      <IconButton label="Siguiente" onClick={() => next()} disabled={!hasTrack} size="lg">
        <NextIcon size={28} />
      </IconButton>

      <IconButton
        label="Repetir"
        onClick={cycleRepeat}
        active={repeat !== 'off'}
      >
        {repeat === 'one' ? <RepeatOneIcon size={22} /> : <RepeatIcon size={22} />}
      </IconButton>
    </div>
  )
}

/** Control de volumen compacto (usa el mismo store). */
export function VolumeControl() {
  const volume = usePlayerStore((state) => state.volume)
  const muted = usePlayerStore((state) => state.muted)
  const setVolume = usePlayerStore((state) => state.setVolume)
  const toggleMute = usePlayerStore((state) => state.toggleMute)
  const track = usePlayerStore(selectCurrentTrack)

  if (!track) return null

  const value = muted ? 0 : volume / 100

  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        aria-label={muted ? 'Activar sonido' : 'Silenciar'}
        onClick={toggleMute}
        className="text-muted"
      >
        {muted ? <VolumeMuteIcon size={20} /> : <VolumeIcon size={20} />}
      </button>
      <Slider
        value={value}
        ariaLabel="Volumen"
        className="flex-1"
        trackClassName="h-1 bg-[color:var(--overlay-3)]"
        fillClassName="bg-ink/60"
        onChange={(next) => setVolume(next * 100)}
      />
    </div>
  )
}
