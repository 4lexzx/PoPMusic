import { Pressable, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Artwork } from '@/components/Artwork'
import { NextIcon, PauseIcon, PlayIcon } from '@/components/Icons'
import { IconButton, Spinner } from '@/components/ui'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { RootStackParamList } from '@/navigation/types'

/** Barra compacta flotante sobre la navegación inferior. */
export function MiniPlayer() {
  const colors = useThemeColors()
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>()
  const track = usePlayerStore(selectCurrentTrack)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const isBusy = usePlayerStore((state) => state.isLoadingTrack || state.isBuffering)
  const position = usePlayerStore((state) => state.position)
  const duration = usePlayerStore((state) => state.duration)
  const toggle = usePlayerStore((state) => state.toggle)
  const next = usePlayerStore((state) => state.next)

  if (!track) return null

  const progress = duration > 0 ? Math.min(100, (position / duration) * 100) : 0

  return (
    <View className="px-3">
      <View
        className="relative flex-row items-center overflow-hidden rounded-3xl"
        style={{
          paddingRight: 96,
          backgroundColor: colors.bgElev + 'e6',
          borderWidth: 1,
          borderColor: colors.hair,
        }}
      >
        <Pressable
          onPress={() => navigation.navigate('NowPlaying')}
          className="flex-1 flex-row items-center gap-3 p-2.5"
        >
          <Artwork src={track.thumbnail} style={{ width: 44, height: 44 }} radius={12} />
          <View className="min-w-0 flex-1">
            <Text numberOfLines={1} className="text-[0.9rem] font-medium" style={{ color: colors.ink }}>
              {track.title}
            </Text>
            <Text numberOfLines={1} className="mt-0.5 text-xs" style={{ color: colors.faint }}>
              {track.artist}
            </Text>
          </View>
        </Pressable>

        <View className="absolute right-2 flex-row items-center gap-1">
          <IconButton label={isPlaying ? 'Pausar' : 'Reproducir'} onPress={toggle}>
            {isBusy ? (
              <Spinner size={20} />
            ) : isPlaying ? (
              <PauseIcon size={22} color={colors.ink} />
            ) : (
              <PlayIcon size={22} color={colors.ink} />
            )}
          </IconButton>
          <IconButton label="Siguiente" onPress={() => next()}>
            <NextIcon size={22} color={colors.ink} />
          </IconButton>
        </View>

        <View
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            bottom: 0,
            height: 3,
            backgroundColor: colors.overlay2,
          }}
        >
          <View style={{ height: '100%', width: `${progress}%`, backgroundColor: colors.accent }} />
        </View>
      </View>
    </View>
  )
}
