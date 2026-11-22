import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ChevronLeftIcon, PlayIcon, PlusIcon, ShuffleIcon, TrashIcon } from '@/components/Icons'
import { Button, IconButton } from '@/components/ui'
import { AddTracksToPlaylistSheet } from '@/features/library/AddTracksToPlaylistSheet'
import { PlaylistCover } from '@/features/library/PlaylistCard'
import { TrackRow } from '@/features/library/TrackRow'
import { formatDuration } from '@/shared/utils/time'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'

export function PlaylistPage() {
  const params = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [addOpen, setAddOpen] = useState(false)

  const playlistId = params.id ? decodeURIComponent(params.id) : ''
  const playlists = useLibraryStore((state) => state.playlists)
  const removePlaylist = useLibraryStore((state) => state.removePlaylist)
  const removeTrackFromPlaylist = useLibraryStore((state) => state.removeTrackFromPlaylist)
  const playQueue = usePlayerStore((state) => state.playQueue)

  const playlist = playlists.find((item) => item.id === playlistId)

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

  const handleShuffle = () => {
    const shuffled = [...playlist.tracks]
    for (let index = shuffled.length - 1; index > 0; index -= 1) {
      const swap = Math.floor(Math.random() * (index + 1))
      ;[shuffled[index], shuffled[swap]] = [shuffled[swap], shuffled[index]]
    }
    void playQueue(shuffled, 0)
  }

  return (
    <div className="pb-4">
      <header className="flex items-center justify-between px-4 pt-[calc(0.75rem+env(safe-area-inset-top))]">
        <IconButton label="Volver" onClick={() => navigate(-1)}>
          <ChevronLeftIcon size={22} />
        </IconButton>
        <IconButton
          label="Eliminar playlist"
          onClick={() => {
            removePlaylist(playlist.id)
            navigate('/library')
          }}
        >
          <TrashIcon size={20} />
        </IconButton>
      </header>

      <section className="flex flex-col items-center px-5 pt-2 text-center">
        <PlaylistCover
          playlist={playlist}
          className="h-44 w-44 shadow-[0_25px_50px_-20px_rgba(0,0,0,0.9)]"
        />
        <h1 className="mt-5 text-xl font-semibold tracking-tight">{playlist.name}</h1>
        {playlist.description ? (
          <p className="mt-1 line-clamp-2 max-w-xs text-xs text-faint">{playlist.description}</p>
        ) : null}
        <p className="mt-1 text-xs text-faint">
          {playlist.tracks.length} {playlist.tracks.length === 1 ? 'canción' : 'canciones'}
          {totalDuration > 0 ? ` · ${formatDuration(totalDuration)}` : ''}
        </p>

        <div className="mt-5 flex w-full max-w-sm items-center gap-3">
          <Button
            fullWidth
            icon={<PlayIcon size={18} />}
            disabled={playlist.tracks.length === 0}
            onClick={() => void playQueue(playlist.tracks, 0)}
          >
            Reproducir
          </Button>
          <Button
            variant="subtle"
            icon={<ShuffleIcon size={18} />}
            disabled={playlist.tracks.length === 0}
            onClick={handleShuffle}
          >
            Aleatorio
          </Button>
          {isLocal ? (
            <IconButton label="Añadir canciones" onClick={() => setAddOpen(true)}>
              <PlusIcon size={22} />
            </IconButton>
          ) : null}
        </div>
      </section>

      <section className="mt-6 px-3">
        {playlist.tracks.length > 0 ? (
          <ul className="space-y-0.5">
            {playlist.tracks.map((track, index) => (
              <li key={`${track.source}-${track.id}-${index}`}>
                <TrackRow
                  track={track}
                  queue={playlist.tracks}
                  index={index}
                  onRemove={isLocal ? () => removeTrackFromPlaylist(playlist.id, index) : undefined}
                />
              </li>
            ))}
          </ul>
        ) : (
          <div className="flex flex-col items-center gap-3 py-10 text-center">
            <p className="text-sm text-faint">Esta playlist no tiene canciones.</p>
            {isLocal ? (
              <Button icon={<PlusIcon size={18} />} onClick={() => setAddOpen(true)}>
                Añadir canciones
              </Button>
            ) : null}
          </div>
        )}
      </section>

      <AddTracksToPlaylistSheet
        playlistId={playlist.id}
        open={addOpen}
        onClose={() => setAddOpen(false)}
      />
    </div>
  )
}
