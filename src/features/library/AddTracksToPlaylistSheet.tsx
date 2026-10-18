import { useState } from 'react'
import { CheckIcon, MusicIcon, PlusIcon, SearchIcon } from '@/components/Icons'
import { Artwork } from '@/components/Artwork'
import { Sheet, Spinner } from '@/components/ui'
import { searchTracks } from '@/shared/api/youtube'
import { IS_DEMO } from '@/shared/config'
import { demoTracks } from '@/shared/data/demo'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { useUiStore } from '@/shared/store/uiStore'
import type { Track } from '@/shared/types/track'

interface AddTracksToPlaylistSheetProps {
  playlistId: string
  open: boolean
  onClose: () => void
}

/** Hoja para buscar y añadir canciones a una playlist existente. */
export function AddTracksToPlaylistSheet({ playlistId, open, onClose }: AddTracksToPlaylistSheetProps) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Track[]>([])
  const [status, setStatus] = useState<'idle' | 'loading' | 'done' | 'error'>('idle')

  const playlist = useLibraryStore((state) => state.playlists.find((item) => item.id === playlistId))
  const addTracks = useLibraryStore((state) => state.addTracksToPlaylist)
  const notify = useUiStore((state) => state.notify)

  const runSearch = async (term: string) => {
    const value = term.trim()
    if (!value) return
    setStatus('loading')
    try {
      const tracks = IS_DEMO ? demoTracks.filter((track) => track.title.toLowerCase().includes(value.toLowerCase())) : await searchTracks(value)
      setResults(tracks)
      setStatus('done')
    } catch {
      setStatus('error')
    }
  }

  const handleAdd = (track: Track) => {
    addTracks(playlistId, [track])
    notify(`«${track.title}» añadida a la playlist.`)
  }

  const isEmpty = status === 'idle' || results.length === 0

  return (
    <Sheet open={open} onClose={onClose} title="Añadir canciones">
      <div className="pb-4">
        <form
          onSubmit={(event) => {
            event.preventDefault()
            void runSearch(query)
          }}
        >
          <div className="flex h-11 items-center gap-2 rounded-2xl hairline surface-2 px-4 transition-colors focus-within:border-accent/50">
            <SearchIcon size={18} className="text-faint" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Busca canciones para añadir"
              enterKeyHint="search"
              className="h-11 flex-1 bg-transparent text-sm outline-none placeholder:text-faint"
            />
          </div>
        </form>

        <div className="mt-4">
          {status === 'loading' ? (
            <div className="flex flex-col items-center gap-3 py-10 text-faint">
              <Spinner size={24} />
              <p className="text-sm">Buscando…</p>
            </div>
          ) : null}

          {status === 'error' ? (
            <p className="py-10 text-center text-sm text-faint">No se pudo buscar. Inténtalo de nuevo.</p>
          ) : null}

          {isEmpty && status !== 'loading' ? (
            <div className="flex flex-col items-center gap-2 py-10 text-center text-faint">
              <MusicIcon size={28} />
              <p className="text-sm">Busca una canción para añadirla.</p>
            </div>
          ) : null}

          {results.length > 0 ? (
            <ul className="space-y-0.5">
              {results.map((track, index) => {
                const already = playlist?.tracks.some((item) => sameTrack(item, track)) ?? false
                return (
                  <li key={`${track.source}-${track.id}-${index}`}>
                    <div className="flex items-center gap-3 rounded-2xl px-1 py-2">
                      <Artwork src={track.thumbnail} className="h-11 w-11 shrink-0" rounded="rounded-xl" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">{track.title}</p>
                        <p className="truncate text-xs text-faint">{track.artist}</p>
                      </div>
                      <button
                        type="button"
                        disabled={already}
                        onClick={() => handleAdd(track)}
                        aria-label={already ? 'Ya está en la playlist' : `Añadir ${track.title}`}
                        className={`grid h-9 w-9 shrink-0 place-items-center rounded-full ${
                          already ? 'surface-2 text-accent' : 'bg-ink text-bg'
                        }`}
                      >
                        {already ? <CheckIcon size={18} /> : <PlusIcon size={18} />}
                      </button>
                    </div>
                  </li>
                )
              })}
            </ul>
          ) : null}
        </div>
      </div>
    </Sheet>
  )
}
