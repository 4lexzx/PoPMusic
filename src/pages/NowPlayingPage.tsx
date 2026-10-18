import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Artwork } from '@/components/Artwork'
import {
  ChevronDownIcon,
  DiscIcon,
  HeartFilledIcon,
  HeartIcon,
  ListMusicIcon,
  MicIcon,
  MoreIcon,
  PlusIcon,
  VolumeIcon,
} from '@/components/Icons'
import { IconButton } from '@/components/ui'
import { LyricsFullscreen } from '@/features/lyrics/LyricsSection'
import { useLyrics } from '@/features/lyrics/useLyrics'
import { PlayerControls, ProgressBar, VolumeControl } from '@/features/player/PlayerControls'
import { TrackMenu } from '@/features/library/TrackRow'
import { DynamicBackground, useArtworkColors } from '@/features/visuals/DynamicBackground'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'

export function NowPlayingPage() {
  const navigate = useNavigate()
  const track = usePlayerStore(selectCurrentTrack)
  const queue = usePlayerStore((state) => state.queue)
  const position = usePlayerStore((state) => state.position)
  const duration = usePlayerStore((state) => state.duration)
  const seek = usePlayerStore((state) => state.seek)
  const setQueueOpen = useUiStore((state) => state.setQueueOpen)

  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  const { lyrics, status } = useLyrics(track)
  const colors = useArtworkColors(track?.thumbnail)

  const [showLyrics, setShowLyrics] = useState(false)
  const [showVolume, setShowVolume] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  if (!track) {
    return (
      <div className="flex min-h-[70vh] flex-col items-center justify-center gap-3 px-8 text-center text-faint">
        <DiscIcon size={40} />
        <p className="text-sm">No hay ninguna canción reproduciéndose.</p>
        <Link to="/search" className="text-sm font-medium text-accent-soft">
          Buscar música
        </Link>
      </div>
    )
  }

  const isFavorite = favorites.some((item) => sameTrack(item, track))
  const hasLyrics = status === 'ready' && !!lyrics

  return (
    <div className="relative flex h-dvh flex-col overflow-hidden">
      <DynamicBackground image={track.thumbnail} colors={colors} intensity="strong" />

      <div className="relative flex min-h-0 flex-1 flex-col px-5 pb-[calc(0.75rem+env(safe-area-inset-bottom))] pt-[calc(0.5rem+env(safe-area-inset-top))]">
        <header className="flex shrink-0 items-center justify-between">
          <IconButton label="Minimizar" onClick={() => navigate(-1)}>
            <ChevronDownIcon size={26} />
          </IconButton>
          <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-faint">
            Reproduciendo
          </p>
          <IconButton label="Más opciones" onClick={() => setMenuOpen(true)}>
            <MoreIcon size={22} />
          </IconButton>
        </header>

        <div className="mt-4 flex min-h-0 flex-1 flex-col items-center justify-center">
          <Artwork
            src={track.thumbnail}
            alt={track.title}
            className="aspect-square w-full max-w-[15rem] shadow-[0_30px_70px_-20px_rgba(0,0,0,0.9)]"
            rounded="rounded-[1.75rem]"
            iconSize={56}
          />
        </div>

        <div className="mt-5 flex shrink-0 items-center justify-between gap-4">
          <div className="min-w-0">
            <h1 className="truncate text-[1.35rem] font-semibold tracking-tight">{track.title}</h1>
            <p className="mt-0.5 truncate text-sm text-faint">{track.artist}</p>
          </div>
          <IconButton
            label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
            onClick={() => toggleFavorite(track)}
            active={isFavorite}
          >
            {isFavorite ? (
              <HeartFilledIcon size={26} className="text-accent" />
            ) : (
              <HeartIcon size={26} />
            )}
          </IconButton>
        </div>

        <div className="mt-3 shrink-0">
          <ProgressBar position={position} duration={duration || track.duration} onSeek={seek} />
        </div>

        <div className="mt-2 shrink-0">
          <PlayerControls />
        </div>

        {showVolume ? (
          <div className="mt-3 shrink-0 animate-fade-in rounded-2xl hairline surface-1 px-3.5 py-2">
            <VolumeControl />
          </div>
        ) : null}

        <div className="mt-3 flex shrink-0 items-center justify-between rounded-2xl hairline surface-1 px-1.5 py-1">
          <IconButton
            label={hasLyrics ? 'Ver letra' : 'Letra no disponible'}
            onClick={() => setShowLyrics(true)}
            disabled={!lyrics}
          >
            <MicIcon size={21} />
          </IconButton>
          <IconButton label="Cola de reproducción" onClick={() => setQueueOpen(true)}>
            <ListMusicIcon size={21} />
          </IconButton>
          <IconButton
            label="Volumen"
            active={showVolume}
            onClick={() => setShowVolume((value) => !value)}
          >
            <VolumeIcon size={21} />
          </IconButton>
          <IconButton label="Añadir a una lista" onClick={() => setMenuOpen(true)}>
            <PlusIcon size={21} />
          </IconButton>
        </div>
      </div>

      {showLyrics && lyrics ? (
        <LyricsFullscreen
          track={track}
          lyrics={lyrics}
          time={position}
          colors={colors}
          onClose={() => setShowLyrics(false)}
        />
      ) : null}

      {menuOpen ? (
        <TrackMenu track={track} queue={queue} onClose={() => setMenuOpen(false)} />
      ) : null}
    </div>
  )
}
