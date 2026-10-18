import { useState } from 'react'
import { ScrollView, Text, View } from 'react-native'
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { ChevronLeftIcon, PlayIcon, PlusIcon, ShuffleIcon, TrashIcon } from '@/components/Icons'
import { Button, IconButton } from '@/components/ui'
import { AddTracksToPlaylistSheet } from '@/features/library/AddTracksToPlaylistSheet'
import { PlaylistCover } from '@/features/library/PlaylistCard'
import { TrackRow } from '@/features/library/TrackRow'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useThemeColors } from '@/shared/theme/theme'
import { formatDuration } from '@/shared/utils/time'
import type { RootStackParamList } from '@/navigation/types'

export function PlaylistPage() {
  const colors = useThemeColors()
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>()
  const route = useRoute<RouteProp<RootStackParamList, 'Playlist'>>()
  const [addOpen, setAddOpen] = useState(false)

  const playlistId = route.params?.id ?? ''
  const playlists = useLibraryStore((state) => state.playlists)
  const removePlaylist = useLibraryStore((state) => state.removePlaylist)
  const removeTrackFromPlaylist = useLibraryStore((state) => state.removeTrackFromPlaylist)
  const playQueue = usePlayerStore((state) => state.playQueue)

  const playlist = playlists.find((item) => item.id === playlistId)

  if (!playlist) {
    return (
      <View className="flex-1 items-center justify-center gap-4 px-8">
        <Text className="text-center text-sm" style={{ color: colors.faint }}>
          No se encontró esta playlist.
        </Text>
        <Button variant="subtle" onPress={() => navigation.navigate('Tabs', { screen: 'Library' })}>
          Volver a la biblioteca
        </Button>
      </View>
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
    <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 24 }}>
      <View className="flex-row items-center justify-between px-4 pt-3">
        <IconButton label="Volver" onPress={() => navigation.goBack()}>
          <ChevronLeftIcon size={22} color={colors.ink} />
        </IconButton>
        <IconButton
          label="Eliminar playlist"
          onPress={() => {
            removePlaylist(playlist.id)
            navigation.navigate('Tabs', { screen: 'Library' })
          }}
        >
          <TrashIcon size={20} color={colors.ink} />
        </IconButton>
      </View>

      <View className="items-center px-5 pt-2">
        <PlaylistCover playlist={playlist} size={176} />
        <Text className="mt-5 text-xl font-semibold tracking-tight" style={{ color: colors.ink }}>
          {playlist.name}
        </Text>
        {playlist.description ? (
          <Text numberOfLines={2} className="mt-1 max-w-xs text-center text-xs" style={{ color: colors.faint }}>
            {playlist.description}
          </Text>
        ) : null}
        <Text className="mt-1 text-xs" style={{ color: colors.faint }}>
          {playlist.tracks.length} {playlist.tracks.length === 1 ? 'canción' : 'canciones'}
          {totalDuration > 0 ? ` · ${formatDuration(totalDuration)}` : ''}
        </Text>

        <View className="mt-5 flex-row items-center gap-3" style={{ alignSelf: 'stretch' }}>
          <View style={{ flex: 1 }}>
            <Button
              fullWidth
              icon={<PlayIcon size={18} color={colors.primaryInk} />}
              disabled={playlist.tracks.length === 0}
              onPress={() => void playQueue(playlist.tracks, 0)}
            >
              Reproducir
            </Button>
          </View>
          <Button
            variant="subtle"
            icon={<ShuffleIcon size={18} color={colors.ink} />}
            disabled={playlist.tracks.length === 0}
            onPress={handleShuffle}
          >
            Aleatorio
          </Button>
          {isLocal ? (
            <IconButton label="Añadir canciones" onPress={() => setAddOpen(true)}>
              <PlusIcon size={22} color={colors.ink} />
            </IconButton>
          ) : null}
        </View>
      </View>

      <View className="mt-6 px-3">
        {playlist.tracks.length > 0 ? (
          playlist.tracks.map((track, index) => (
            <TrackRow
              key={`${track.source}-${track.id}-${index}`}
              track={track}
              queue={playlist.tracks}
              index={index}
              onRemove={isLocal ? () => removeTrackFromPlaylist(playlist.id, index) : undefined}
            />
          ))
        ) : (
          <View className="items-center gap-3 py-10">
            <Text className="text-sm" style={{ color: colors.faint }}>
              Esta playlist no tiene canciones.
            </Text>
            {isLocal ? (
              <Button icon={<PlusIcon size={18} color={colors.primaryInk} />} onPress={() => setAddOpen(true)}>
                Añadir canciones
              </Button>
            ) : null}
          </View>
        )}
      </View>

      <AddTracksToPlaylistSheet playlistId={playlist.id} open={addOpen} onClose={() => setAddOpen(false)} />
    </ScrollView>
  )
}
