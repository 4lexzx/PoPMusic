import { useEffect, useState } from 'react'
import { SpotifyIcon, YouTubeIcon } from '@/components/Icons'
import { Button, Dialog, Segmented } from '@/components/ui'
import { importSpotifyPlaylist } from '@/shared/api/spotify'
import { importYouTubePlaylist } from '@/shared/api/youtube'
import { IS_DEMO } from '@/shared/config'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { useUiStore } from '@/shared/store/uiStore'

const NAME_LIMIT = 20

type Mode = 'create' | 'import'
type Source = 'youtube' | 'spotify'

interface AddToLibraryDialogProps {
  open: boolean
  onClose: () => void
}

const inputClass =
  'w-full rounded-xl hairline surface-1 px-3.5 py-3 text-sm outline-none transition-colors placeholder:text-faint focus:border-accent/50'

/** Diálogo para crear una lista desde cero o importarla (botón «+»). */
export function AddToLibraryDialog({ open, onClose }: AddToLibraryDialogProps) {
  const [mode, setMode] = useState<Mode>('create')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [showDescription, setShowDescription] = useState(false)
  const [source, setSource] = useState<Source>('youtube')
  const [url, setUrl] = useState('')
  const [busy, setBusy] = useState(false)

  const addPlaylist = useLibraryStore((state) => state.addPlaylist)
  const createPlaylist = useLibraryStore((state) => state.createPlaylist)
  const notify = useUiStore((state) => state.notify)

  useEffect(() => {
    if (!open) {
      setMode('create')
      setName('')
      setDescription('')
      setShowDescription(false)
      setSource('youtube')
      setUrl('')
      setBusy(false)
    }
  }, [open])

  const handleCreate = () => {
    if (!name.trim()) return
    const playlist = createPlaylist(name, description)
    notify(`Lista «${playlist.name}» creada.`)
    onClose()
  }

  const handleImport = async () => {
    const input = url.trim()
    if (!input || busy) return
    setBusy(true)
    try {
      if (source === 'spotify') {
        const result = await importSpotifyPlaylist(input)
        if (!result.itemsAvailable) {
          notify(
            'Se importó la lista, pero Spotify no permitió listar sus canciones (restricción de la API).',
          )
        }
        if (result.playlist.tracks.length === 0) {
          throw new Error('La lista no contiene canciones accesibles.')
        }
        addPlaylist(result.playlist)
        notify(`Lista «${result.playlist.name}» importada.`)
      } else {
        const playlist = await importYouTubePlaylist(input)
        if (playlist.tracks.length === 0) {
          throw new Error('La lista no contiene canciones accesibles.')
        }
        addPlaylist(playlist)
        notify(`Lista «${playlist.name}» importada con ${playlist.tracks.length} canciones.`)
      }
      onClose()
    } catch (error) {
      notify(error instanceof Error ? error.message : 'No se pudo importar la lista.', 'error')
    } finally {
      setBusy(false)
    }
  }

  const title = 'Añadir a tu biblioteca'
  const subtitle =
    mode === 'create'
      ? 'Crea una lista vacía y añádele canciones cuando quieras'
      : 'Trae una lista de YouTube o Spotify'

  return (
    <Dialog open={open} onClose={onClose}>
      <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
      <p className="mt-0.5 text-[0.8rem] text-faint">{subtitle}</p>

      <Segmented
        className="mt-4"
        value={mode}
        onChange={setMode}
        tabs={[
          { id: 'create', label: 'Desde cero' },
          { id: 'import', label: 'Importar' },
        ]}
      />

      {mode === 'create' ? (
        <div className="mt-4 space-y-1">
          <div className="relative">
            <input
              autoFocus
              value={name}
              maxLength={NAME_LIMIT}
              onChange={(event) => setName(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') handleCreate()
              }}
              placeholder="Nombre de la lista"
              className={`${inputClass} pr-14`}
            />
            <span className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-xs tabular-nums text-faint">
              {name.length}/{NAME_LIMIT}
            </span>
          </div>

          {showDescription ? (
            <textarea
              value={description}
              rows={2}
              maxLength={120}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Añade una descripción"
              className={`${inputClass} resize-none`}
            />
          ) : (
            <button
              type="button"
              onClick={() => setShowDescription(true)}
              className="px-1 py-1 text-[0.8rem] text-faint transition-colors active:text-muted"
            >
              + Descripción (opcional)
            </button>
          )}
        </div>
      ) : (
        <div className="mt-4 space-y-2.5">
          <Segmented
            value={source}
            onChange={setSource}
            tabs={[
              { id: 'youtube', label: 'YouTube' },
              { id: 'spotify', label: 'Spotify' },
            ]}
          />
          <div className="flex items-center gap-2 rounded-xl hairline surface-1 px-3.5 transition-colors focus-within:border-accent/50">
            <span className="text-faint">
              {source === 'spotify' ? <SpotifyIcon size={18} /> : <YouTubeIcon size={18} />}
            </span>
            <input
              autoFocus
              type="url"
              inputMode="url"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              value={url}
              onChange={(event) => setUrl(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') void handleImport()
              }}
              placeholder={
                source === 'spotify'
                  ? 'https://open.spotify.com/playlist/…'
                  : 'https://youtube.com/playlist?list=…'
              }
              className="h-11 flex-1 bg-transparent text-sm outline-none placeholder:text-faint"
            />
          </div>
        </div>
      )}

      {IS_DEMO ? (
        <p className="mt-3 text-[0.72rem] leading-relaxed text-faint">
          Modo demo: sin clave de YouTube la importación usa datos de muestra.
        </p>
      ) : null}

      <div className="mt-5 flex items-center justify-end gap-2">
        <Button variant="ghost" size="sm" onClick={onClose}>
          Cancelar
        </Button>
        {mode === 'create' ? (
          <Button size="sm" disabled={!name.trim()} onClick={handleCreate}>
            Crear
          </Button>
        ) : (
          <Button size="sm" loading={busy} disabled={!url.trim()} onClick={() => void handleImport()}>
            Importar
          </Button>
        )}
      </div>
    </Dialog>
  )
}
