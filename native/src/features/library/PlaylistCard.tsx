import { Pressable, Text, View, type StyleProp, type ViewStyle } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Artwork } from '@/components/Artwork'
import { PlayIcon } from '@/components/Icons'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { Playlist } from '@/shared/types/track'
import type { RootStackParamList } from '@/navigation/types'
import { SOURCE_LABELS } from '@/navigation/types'

/** Portada de playlist: imagen propia o mosaico 2×2 con las canciones. */
export function PlaylistCover({
  playlist,
  size,
  radius = 16,
}: {
  playlist: Playlist
  size?: number
  radius?: number
}) {
  const colors = useThemeColors()
  const covers = playlist.tracks.map((track) => track.thumbnail).filter(Boolean).slice(0, 4)
  const dimension = size ? { width: size, height: size } : undefined

  if (playlist.thumbnail) {
    return <Artwork src={playlist.thumbnail} style={dimension} radius={radius} iconSize={40} />
  }

  if (covers.length >= 4) {
    const half = size ? size / 2 - 0.5 : undefined
    return (
      <View
        style={[{ borderRadius: radius, overflow: 'hidden', flexDirection: 'row', flexWrap: 'wrap', gap: 1, backgroundColor: 'rgba(0,0,0,0.3)' }, dimension]}
      >
        {covers.map((cover, index) => (
          <Artwork
            key={`${cover}-${index}`}
            src={cover}
            style={half ? { width: half, height: half } : undefined}
            radius={0}
            iconSize={20}
          />
        ))}
      </View>
    )
  }

  return <Artwork src={covers[0]} style={dimension} radius={radius} iconSize={40} />
}

interface PlaylistCardProps {
  playlist: Playlist
  variant?: 'tile' | 'wide'
  style?: StyleProp<ViewStyle>
  size?: number
}

/** Tarjeta de playlist con reproducción rápida. */
export function PlaylistCard({ playlist, variant = 'tile', style, size = 148 }: PlaylistCardProps) {
  const colors = useThemeColors()
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>()
  const playQueue = usePlayerStore((state) => state.playQueue)

  const open = () => navigation.navigate('Playlist', { id: playlist.id })
  const handlePlay = () => {
    if (playlist.tracks.length > 0) void playQueue(playlist.tracks, 0)
  }

  if (variant === 'wide') {
    return (
      <Pressable
        onPress={open}
        className="flex-row items-center gap-3 rounded-2xl p-2"
        style={[{ backgroundColor: colors.overlay1 }, style]}
      >
        <PlaylistCover playlist={playlist} size={56} radius={14} />
        <View className="min-w-0 flex-1">
          <Text numberOfLines={1} className="font-medium" style={{ color: colors.ink }}>
            {playlist.name}
          </Text>
          <Text numberOfLines={1} className="mt-0.5 text-xs" style={{ color: colors.faint }}>
            {playlist.tracks.length} canciones · {SOURCE_LABELS[playlist.source ?? 'local'] ?? 'Local'}
          </Text>
        </View>
        {playlist.tracks.length > 0 ? (
          <Pressable
            onPress={handlePlay}
            accessibilityLabel={`Reproducir ${playlist.name}`}
            className="mr-1 h-10 w-10 items-center justify-center rounded-full"
            style={{ backgroundColor: colors.ink }}
          >
            <PlayIcon size={18} color={colors.bg} />
          </Pressable>
        ) : null}
      </Pressable>
    )
  }

  return (
    <Pressable onPress={open} style={[{ width: size }, style]}>
      <View style={{ width: size, height: size }}>
        <PlaylistCover playlist={playlist} size={size} />
        {playlist.tracks.length > 0 ? (
          <Pressable
            onPress={handlePlay}
            accessibilityLabel={`Reproducir ${playlist.name}`}
            className="absolute items-center justify-center"
            style={{
              bottom: 8,
              right: 8,
              width: 44,
              height: 44,
              borderRadius: 999,
              backgroundColor: colors.ink,
            }}
          >
            <PlayIcon size={18} color={colors.bg} />
          </Pressable>
        ) : null}
      </View>
      <Text numberOfLines={1} className="mt-2.5 text-sm font-medium" style={{ color: colors.ink }}>
        {playlist.name}
      </Text>
      <Text numberOfLines={1} className="text-xs" style={{ color: colors.faint }}>
        {playlist.tracks.length} {playlist.tracks.length === 1 ? 'canción' : 'canciones'}
      </Text>
    </Pressable>
  )
}
