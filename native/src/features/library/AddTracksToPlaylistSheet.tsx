import { useState } from 'react'
import { Pressable, Text, TextInput, View } from 'react-native'
import { Artwork } from '@/components/Artwork'
import { CheckIcon, MusicIcon, PlusIcon, SearchIcon } from '@/components/Icons'
import { Sheet, Spinner } from '@/components/ui'
import { searchMusic } from '@/shared/api/youtube'
import { IS_DEMO } from '@/shared/config'
import { demoTracks } from '@/shared/data/demo'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { Track } from '@/shared/types/track'

interface AddTracksToPlaylistSheetProps {
  playlistId: string
  open: boolean
  onClose: () => void
}

/** Hoja para buscar y añadir canciones a una playlist existente. */
export function AddTracksToPlaylistSheet({ playlistId, open, onClose }: AddTracksToPlaylistSheetProps) {
  const colors = useThemeColors()
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
      const tracks = IS_DEMO
        ? demoTracks.filter((track) => track.title.toLowerCase().includes(value.toLowerCase()))
        : await searchMusic(value)
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
      <View className="pb-4">
        <View
          className="h-11 flex-row items-center gap-2 rounded-2xl px-4"
          style={{
            borderWidth: 1,
            borderColor: colors.hair,
            backgroundColor: colors.overlay2,
          }}
        >
          <SearchIcon size={18} color={colors.faint} />
          <TextInput
            value={query}
            onChangeText={setQuery}
            onSubmitEditing={() => void runSearch(query)}
            returnKeyType="search"
            placeholder="Busca canciones para añadir"
            placeholderTextColor={colors.faint}
            className="h-11 flex-1 text-sm"
            style={{ color: colors.ink }}
          />
        </View>

        <View className="mt-4">
          {status === 'loading' ? (
            <View className="items-center gap-3 py-10">
              <Spinner size={24} />
              <Text className="text-sm" style={{ color: colors.faint }}>
                Buscando…
              </Text>
            </View>
          ) : null}

          {status === 'error' ? (
            <Text className="py-10 text-center text-sm" style={{ color: colors.faint }}>
              No se pudo buscar. Inténtalo de nuevo.
            </Text>
          ) : null}

          {isEmpty && status !== 'loading' ? (
            <View className="items-center gap-2 py-10">
              <MusicIcon size={28} color={colors.faint} />
              <Text className="text-sm" style={{ color: colors.faint }}>
                Busca una canción para añadirla.
              </Text>
            </View>
          ) : null}

          {results.length > 0
            ? results.map((track, index) => {
                const already = playlist?.tracks.some((item) => sameTrack(item, track)) ?? false
                return (
                  <View
                    key={`${track.source}-${track.id}-${index}`}
                    className="flex-row items-center gap-3 rounded-2xl px-1 py-2"
                  >
                    <Artwork src={track.thumbnail} style={{ width: 44, height: 44 }} radius={12} />
                    <View className="min-w-0 flex-1">
                      <Text numberOfLines={1} className="text-sm font-medium" style={{ color: colors.ink }}>
                        {track.title}
                      </Text>
                      <Text numberOfLines={1} className="text-xs" style={{ color: colors.faint }}>
                        {track.artist}
                      </Text>
                    </View>
                    <Pressable
                      disabled={already}
                      onPress={() => handleAdd(track)}
                      accessibilityLabel={already ? 'Ya está en la playlist' : `Añadir ${track.title}`}
                      className="h-9 w-9 shrink-0 items-center justify-center rounded-full"
                      style={{ backgroundColor: already ? colors.overlay2 : colors.ink }}
                    >
                      {already ? (
                        <CheckIcon size={18} color={colors.accent} />
                      ) : (
                        <PlusIcon size={18} color={colors.bg} />
                      )}
                    </Pressable>
                  </View>
                )
              })
            : null}
        </View>
      </View>
    </Sheet>
  )
}
