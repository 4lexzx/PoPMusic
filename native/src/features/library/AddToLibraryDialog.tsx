import { useEffect, useState } from 'react'
import { Pressable, Text, TextInput, View } from 'react-native'
import { SpotifyIcon, YouTubeIcon } from '@/components/Icons'
import { Button, Dialog, Segmented } from '@/components/ui'
import { importSpotifyPlaylist } from '@/shared/api/spotify'
import { importYouTubePlaylist } from '@/shared/api/youtube'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'

const NAME_LIMIT = 20

type Mode = 'create' | 'import'
type Source = 'youtube' | 'spotify'

interface AddToLibraryDialogProps {
  open: boolean
  onClose: () => void
}

/** Diálogo para crear una lista desde cero o importarla (botón «+»). */
export function AddToLibraryDialog({ open, onClose }: AddToLibraryDialogProps) {
  const colors = useThemeColors()
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
          notify('Se importó la lista, pero Spotify no permitió listar sus canciones (restricción de la API).')
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

  const subtitle =
    mode === 'create'
      ? 'Crea una lista vacía y añádele canciones cuando quieras'
      : 'Trae una lista de YouTube o Spotify'

  const inputStyle = {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.hair,
    backgroundColor: colors.overlay1,
    color: colors.ink,
    paddingHorizontal: 14,
    fontSize: 14,
  }

  return (
    <Dialog open={open} onClose={onClose}>
      <Text className="text-lg font-semibold tracking-tight" style={{ color: colors.ink }}>
        Añadir a tu biblioteca
      </Text>
      <Text className="mt-0.5 text-[0.8rem]" style={{ color: colors.faint }}>
        {subtitle}
      </Text>

      <Segmented
        style={{ marginTop: 16 }}
        value={mode}
        onChange={setMode}
        tabs={[
          { id: 'create', label: 'Desde cero' },
          { id: 'import', label: 'Importar' },
        ]}
      />

      {mode === 'create' ? (
        <View className="mt-4 gap-1">
          <View className="relative justify-center">
            <TextInput
              autoFocus
              value={name}
              maxLength={NAME_LIMIT}
              onChangeText={setName}
              placeholder="Nombre de la lista"
              placeholderTextColor={colors.faint}
              onSubmitEditing={handleCreate}
              returnKeyType="done"
              style={[inputStyle, { height: 48, paddingRight: 56 }]}
            />
            <Text
              className="absolute right-3.5 text-xs"
              style={{ color: colors.faint }}
            >
              {name.length}/{NAME_LIMIT}
            </Text>
          </View>

          {showDescription ? (
            <TextInput
              value={description}
              multiline
              maxLength={120}
              onChangeText={setDescription}
              placeholder="Añade una descripción"
              placeholderTextColor={colors.faint}
              style={[inputStyle, { minHeight: 64, paddingTop: 12, textAlignVertical: 'top' }]}
            />
          ) : (
            <Pressable onPress={() => setShowDescription(true)} className="px-1 py-1">
              <Text className="text-[0.8rem]" style={{ color: colors.faint }}>
                + Descripción (opcional)
              </Text>
            </Pressable>
          )}
        </View>
      ) : (
        <View className="mt-4 gap-2.5">
          <Segmented
            value={source}
            onChange={setSource}
            tabs={[
              { id: 'youtube', label: 'YouTube' },
              { id: 'spotify', label: 'Spotify' },
            ]}
          />
          <View
            className="flex-row items-center gap-2 px-3.5"
            style={{
              borderRadius: 12,
              borderWidth: 1,
              borderColor: colors.hair,
              backgroundColor: colors.overlay1,
            }}
          >
            {source === 'spotify' ? <SpotifyIcon size={18} color={colors.faint} /> : <YouTubeIcon size={18} color={colors.faint} />}
            <TextInput
              autoFocus
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="url"
              value={url}
              onChangeText={setUrl}
              onSubmitEditing={() => void handleImport()}
              returnKeyType="go"
              placeholder={
                source === 'spotify' ? 'https://open.spotify.com/playlist/…' : 'https://youtube.com/playlist?list=…'
              }
              placeholderTextColor={colors.faint}
              className="h-11 flex-1 text-sm"
              style={{ color: colors.ink }}
            />
          </View>
        </View>
      )}

      <View className="mt-5 flex-row items-center justify-end gap-2">
        <Button variant="ghost" size="sm" onPress={onClose}>
          Cancelar
        </Button>
        {mode === 'create' ? (
          <Button size="sm" disabled={!name.trim()} onPress={handleCreate}>
            Crear
          </Button>
        ) : (
          <Button size="sm" loading={busy} disabled={!url.trim()} onPress={() => void handleImport()}>
            Importar
          </Button>
        )}
      </View>
    </Dialog>
  )
}
