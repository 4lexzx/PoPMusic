import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Artwork } from '@/components/Artwork'
import {
  ChevronLeftIcon,
  DiscIcon,
  HeartFilledIcon,
  HeartIcon,
  ListMusicIcon,
  MicIcon,
  MoreIcon,
  NextIcon,
  PauseIcon,
  PlayIcon,
  PrevIcon,
  ShuffleIcon,
  RepeatIcon,
  RepeatOneIcon,
  WaveformIcon,
} from '@/components/Icons'
import { IconButton, Spinner } from '@/components/ui'
import { TimelineSlider } from '@/components/TimelineSlider'
import { DesktopLyrics } from '@/features/lyrics/DesktopLyrics'
import { useLyrics } from '@/features/lyrics/useLyrics'
import { ReactiveBackdrop } from '@/features/visuals/ReactiveBackdrop'
import { useArtworkColors } from '@/features/visuals/DynamicBackground'
import { TrackMenu } from '@/features/library/TrackRow'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'
import { VolumeControl } from '@/features/player/PlayerControls'

/** Reproductor a pantalla completa con letras sincronizadas y fondo reactivo. */
export function NowPlayingPage() {
  const navigate = useNavigate()
  const track = usePlayerStore(selectCurrentTrack)
  const queue = usePlayerStore((state) => state.queue)
  const position = usePlayerStore((state) => state.position)
  const duration = usePlayerStore((state) => state.duration)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const isBusy = usePlayerStore((state) => state.isLoadingTrack || state.isBuffering)
  const shuffle = usePlayerStore((state) => state.shuffle)
  const repeat = usePlayerStore((state) => state.repeat)
  const toggle = usePlayerStore((state) => state.toggle)
  const next = usePlayerStore((state) => state.next)
  const prev = usePlayerStore((state) => state.prev)
  const seek = usePlayerStore((state) => state.seek)
  const toggleShuffle = usePlayerStore((state) => state.toggleShuffle)
  const cycleRepeat = usePlayerStore((state) => state.cycleRepeat)
  const setQueueOpen = useUiStore((state) => state.setQueueOpen)

  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  const { lyrics, status } = useLyrics(track)
  const colors = useArtworkColors(track?.thumbnail)
  const [menuOpen, setMenuOpen] = useState(false)

  if (!track) {
    return (
      <div className="now-empty">
        <DiscIcon size={52} />
        <p>No hay ninguna canción reproduciéndose.</p>
        <Link to="/search" className="text-accent-soft">
          Buscar música
        </Link>
      </div>
    )
  }

  const isFavorite = favorites.some((item) => sameTrack(item, track))
  const total = duration || track.duration || 0
  const hasLyrics = status === 'ready' && !!lyrics

  return (
    <div className="now-playing">
      <ReactiveBackdrop colors={colors} image={track.thumbnail} />

      <header className="now-playing__header">
        <IconButton label="Volver" onClick={() => navigate(-1)}>
          <ChevronLeftIcon size={24} />
        </IconButton>
        <span className="now-playing__eyebrow">Reproduciendo</span>
        <div className="now-playing__header-actions">
          <IconButton
            label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
            onClick={() => toggleFavorite(track)}
            active={isFavorite}
          >
            {isFavorite ? <HeartFilledIcon size={20} className="text-accent" /> : <HeartIcon size={20} />}
          </IconButton>
          <IconButton label="Cola" onClick={() => setQueueOpen(true)}>
            <ListMusicIcon size={20} />
          </IconButton>
          <IconButton label="Más opciones" onClick={() => setMenuOpen(true)}>
            <MoreIcon size={20} />
          </IconButton>
        </div>
      </header>

      <div className="now-playing__grid">
        <motion.section
          initial={{ opacity: 0, y: 24, scale: 0.97 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.42, ease: [0.22, 1, 0.36, 1] }}
          className="now-playing__left"
        >
          <div className="now-playing__art-wrap">
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ duration: 26, repeat: Infinity, ease: 'linear' }}
              className="now-playing__ring"
            />
            <Artwork
              src={track.thumbnail}
              alt={track.title}
              className="now-playing__art"
              iconSize={64}
            />
          </div>

          <div className="now-playing__info">
            <h1 className="now-playing__title">{track.title}</h1>
            <p className="now-playing__artist">{track.artist}</p>
          </div>

          <div className="now-playing__progress">
            <TimelineSlider
              position={position}
              duration={total}
              onSeek={seek}
              size="md"
              showTime
            />
          </div>

          <div className="now-playing__controls">
            <IconButton label="Aleatorio" onClick={toggleShuffle} active={shuffle}>
              <ShuffleIcon size={20} />
            </IconButton>
            <button type="button" onClick={() => prev()} aria-label="Anterior" className="now-playing__skip">
              <PrevIcon size={30} />
            </button>
            <button type="button" onClick={toggle} aria-label={isPlaying ? 'Pausar' : 'Reproducir'} className="now-playing__play">
              {isBusy ? <Spinner size={25} /> : isPlaying ? <PauseIcon size={28} /> : <PlayIcon size={28} />}
            </button>
            <button type="button" onClick={() => next()} aria-label="Siguiente" className="now-playing__skip">
              <NextIcon size={30} />
            </button>
            <IconButton label="Repetir" onClick={cycleRepeat} active={repeat !== 'off'}>
              {repeat === 'one' ? <RepeatOneIcon size={20} /> : <RepeatIcon size={20} />}
            </IconButton>
          </div>

          <div className="now-playing__volume">
            <VolumeControl />
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, x: 28 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.42, delay: 0.08, ease: [0.22, 1, 0.36, 1] }}
          className="now-playing__right"
        >
          <div className="now-playing__lyrics-head">
            <span className="now-playing__eyebrow">
              <MicIcon size={14} /> Letra
            </span>
            <button
              type="button"
              onClick={() => window.popDesktop?.toggleOverlay()}
              className="now-playing__overlay-btn"
            >
              <WaveformIcon size={15} /> Flotante
            </button>
          </div>

          {hasLyrics && lyrics ? (
            <DesktopLyrics lyrics={lyrics} />
          ) : (
            <div className="now-playing__no-lyrics">
              <MicIcon size={30} />
              <p>{status === 'loading' ? 'Buscando la letra…' : 'Esta canción no tiene letra sincronizada.'}</p>
            </div>
          )}
        </motion.section>
      </div>

      {menuOpen ? (
        <TrackMenu track={track} queue={queue} onClose={() => setMenuOpen(false)} />
      ) : null}
    </div>
  )
}
