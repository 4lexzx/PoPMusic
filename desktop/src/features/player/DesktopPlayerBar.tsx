import { Link } from 'react-router-dom'
import { Artwork } from '@/components/Artwork'
import {
  DiscIcon,
  HeartFilledIcon,
  HeartIcon,
  ListMusicIcon,
  NextIcon,
  PauseIcon,
  PlayIcon,
  PrevIcon,
  RepeatIcon,
  RepeatOneIcon,
  ShuffleIcon,
  VolumeIcon,
  VolumeMuteIcon,
  WaveformIcon,
} from '@/components/Icons'
import { TimelineSlider } from '@/components/TimelineSlider'
import { Slider, Spinner } from '@/components/ui'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'

/** Barra de reproductor fija, inspirada en los reproductores de escritorio. */
export function DesktopPlayerBar() {
  const track = usePlayerStore(selectCurrentTrack)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const isBusy = usePlayerStore((state) => state.isLoadingTrack || state.isBuffering)
  const position = usePlayerStore((state) => state.position)
  const duration = usePlayerStore((state) => state.duration)
  const volume = usePlayerStore((state) => state.volume)
  const muted = usePlayerStore((state) => state.muted)
  const shuffle = usePlayerStore((state) => state.shuffle)
  const repeat = usePlayerStore((state) => state.repeat)
  const toggle = usePlayerStore((state) => state.toggle)
  const next = usePlayerStore((state) => state.next)
  const prev = usePlayerStore((state) => state.prev)
  const seek = usePlayerStore((state) => state.seek)
  const setVolume = usePlayerStore((state) => state.setVolume)
  const toggleMute = usePlayerStore((state) => state.toggleMute)
  const toggleShuffle = usePlayerStore((state) => state.toggleShuffle)
  const cycleRepeat = usePlayerStore((state) => state.cycleRepeat)

  const setQueueOpen = useUiStore((state) => state.setQueueOpen)
  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  if (!track) {
    return (
      <footer className="playerbar playerbar--empty">
        <WaveformIcon size={18} />
        <span>Elige una canción para empezar</span>
      </footer>
    )
  }

  const isFavorite = favorites.some((item) => sameTrack(item, track))
  const total = duration || track.duration || 0

  return (
    <footer className="playerbar">
      <div className="playerbar__track">
        <Link to="/now-playing" className="playerbar__art" aria-label="Abrir reproductor">
          <Artwork src={track.thumbnail} alt={track.title} className="h-full w-full" iconSize={20} />
        </Link>
        <div className="playerbar__meta">
          <Link to="/now-playing" className="playerbar__title">
            {track.title}
          </Link>
          <span className="playerbar__artist">{track.artist}</span>
        </div>
        <button
          type="button"
          onClick={() => toggleFavorite(track)}
          aria-label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
          className={`playerbar__icon ${isFavorite ? 'playerbar__icon--active' : ''}`}
        >
          {isFavorite ? <HeartFilledIcon size={18} /> : <HeartIcon size={18} />}
        </button>
      </div>

      <div className="playerbar__center">
        <div className="playerbar__buttons">
          <button type="button" onClick={() => prev()} aria-label="Anterior" className="playerbar__icon">
            <PrevIcon size={21} />
          </button>
          <button
            type="button"
            onClick={toggle}
            aria-label={isPlaying ? 'Pausar' : 'Reproducir'}
            className="playerbar__play"
          >
            {isBusy ? <Spinner size={19} /> : isPlaying ? <PauseIcon size={21} /> : <PlayIcon size={21} />}
          </button>
          <button type="button" onClick={() => next()} aria-label="Siguiente" className="playerbar__icon">
            <NextIcon size={21} />
          </button>
        </div>
        <div className="playerbar__progress">
          <TimelineSlider
            position={position}
            duration={total}
            onSeek={seek}
            size="xs"
            className="w-full"
          />
        </div>
      </div>

      <div className="playerbar__right">
        <div className="playerbar__cluster">
          <button
            type="button"
            onClick={toggleShuffle}
            aria-label="Aleatorio"
            className={`playerbar__icon ${shuffle ? 'playerbar__icon--active' : ''}`}
          >
            <ShuffleIcon size={17} />
          </button>
          <button
            type="button"
            onClick={cycleRepeat}
            aria-label="Repetir"
            className={`playerbar__icon ${repeat !== 'off' ? 'playerbar__icon--active' : ''}`}
          >
            {repeat === 'one' ? <RepeatOneIcon size={17} /> : <RepeatIcon size={17} />}
          </button>
          <button
            type="button"
            onClick={() => window.popDesktop?.toggleOverlay()}
            aria-label="Reproductor flotante"
            className="playerbar__icon"
            title="Reproductor flotante"
          >
            <WaveformIcon size={17} />
          </button>
          <div className="playerbar__vol">
            <button
              type="button"
              onClick={toggleMute}
              aria-label={muted ? 'Activar sonido' : 'Silenciar'}
              className="playerbar__icon"
            >
              {muted ? <VolumeMuteIcon size={17} /> : <VolumeIcon size={17} />}
            </button>
            <Slider
              value={muted ? 0 : volume / 100}
              ariaLabel="Volumen"
              className="playerbar__volume"
              trackClassName="h-[3px] bg-[color:var(--overlay-3)]"
              fillClassName="bg-ink/60"
              onChange={(value) => setVolume(value * 100)}
            />
          </div>
          <Link to="/now-playing" aria-label="Reproductor" className="playerbar__icon">
            <DiscIcon size={17} />
          </Link>
          <button
            type="button"
            onClick={() => setQueueOpen(true)}
            aria-label="Cola de reproducción"
            className="playerbar__icon"
          >
            <ListMusicIcon size={17} />
          </button>
        </div>
      </div>
    </footer>
  )
}
