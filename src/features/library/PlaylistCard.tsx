import { Link } from 'react-router-dom'
import { Artwork } from '@/components/Artwork'
import { PlayIcon } from '@/components/Icons'
import { usePlayerStore } from '@/shared/store/playerStore'
import type { Playlist } from '@/shared/types/track'

interface PlaylistCardProps {
  playlist: Playlist
  variant?: 'tile' | 'wide'
  className?: string
}

/** Portada de playlist: imagen propia o mosaico 2×2 con las canciones. */
export function PlaylistCover({ playlist, className = '' }: { playlist: Playlist; className?: string }) {
  const covers = playlist.tracks.map((track) => track.thumbnail).filter(Boolean).slice(0, 4)

  if (playlist.thumbnail) {
    return <Artwork src={playlist.thumbnail} className={className} rounded="rounded-2xl" iconSize={40} />
  }

  if (covers.length >= 4) {
    return (
      <div className={`grid grid-cols-2 gap-px overflow-hidden bg-black/30 ${className}`}>
        {covers.map((cover) => (
          <img
            key={cover}
            src={cover}
            alt=""
            loading="lazy"
            referrerPolicy="no-referrer"
            className="h-full w-full object-cover"
          />
        ))}
      </div>
    )
  }

  return <Artwork src={covers[0]} className={className} rounded="rounded-2xl" iconSize={40} />
}

/** Tarjeta de playlist con reproducción rápida. */
export function PlaylistCard({ playlist, variant = 'tile', className = '' }: PlaylistCardProps) {
  const playQueue = usePlayerStore((state) => state.playQueue)

  const handlePlay = (event: React.MouseEvent) => {
    event.preventDefault()
    event.stopPropagation()
    if (playlist.tracks.length > 0) void playQueue(playlist.tracks, 0)
  }

  if (variant === 'wide') {
    return (
      <Link
        to={`/playlist/${encodeURIComponent(playlist.id)}`}
        className={`flex items-center gap-3 rounded-2xl surface-1 p-2 tap ${className}`}
      >
        <PlaylistCover playlist={playlist} className="h-14 w-14 shrink-0" />
        <span className="min-w-0 flex-1">
          <span className="block truncate font-medium">{playlist.name}</span>
          <span className="mt-0.5 block text-xs text-faint">
            {playlist.tracks.length} canciones ·{' '}
            {playlist.source === 'spotify'
              ? 'Spotify'
              : playlist.source === 'youtube'
                ? 'YouTube'
                : 'Local'}
          </span>
        </span>
        {playlist.tracks.length > 0 ? (
          <button
            type="button"
            onClick={handlePlay}
            aria-label={`Reproducir ${playlist.name}`}
            className="mr-1 flex h-10 w-10 items-center justify-center rounded-full bg-ink text-bg"
          >
            <PlayIcon size={18} />
          </button>
        ) : null}
      </Link>
    )
  }

  return (
    <Link to={`/playlist/${encodeURIComponent(playlist.id)}`} className={`group block ${className}`}>
      <div className="relative">
        <PlaylistCover playlist={playlist} className="aspect-square w-full" />
        {playlist.tracks.length > 0 ? (
          <button
            type="button"
            onClick={handlePlay}
            aria-label={`Reproducir ${playlist.name}`}
            className="absolute bottom-2 right-2 flex h-11 w-11 translate-y-1 items-center justify-center rounded-full bg-ink text-bg opacity-0 shadow-lg shadow-black/50 transition-all group-hover:translate-y-0 group-hover:opacity-100"
          >
            <PlayIcon size={18} />
          </button>
        ) : null}
      </div>
      <p className="mt-2.5 truncate text-sm font-medium">{playlist.name}</p>
      <p className="truncate text-xs text-faint">
        {playlist.tracks.length} {playlist.tracks.length === 1 ? 'canción' : 'canciones'}
      </p>
    </Link>
  )
}
