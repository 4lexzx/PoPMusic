import { Pressable, Text, View } from 'react-native'
import { Artwork } from '@/components/Artwork'
import { PauseIcon, PlayIcon } from '@/components/Icons'
import { PlayingBars } from '@/components/ui'
import { sameTrack } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { Track } from '@/shared/types/track'

interface TrackCardProps {
  track: Track
  queue: Track[]
  index: number
  size?: number
}

/** Tarjeta grande para filas horizontales de sugerencias. */
export function TrackCard({ track, queue, size = 148 }: TrackCardProps) {
  const colors = useThemeColors()
  const playTrack = usePlayerStore((state) => state.playTrack)
  const currentTrack = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const toggle = usePlayerStore((state) => state.toggle)

  const isCurrent = currentTrack ? sameTrack(currentTrack, track) : false
  const active = isCurrent && isPlaying

  const handlePlay = () => {
    if (isCurrent) toggle()
    else void playTrack(track, queue)
  }

  return (
    <View style={{ width: size }} className="shrink-0">
      <Pressable onPress={handlePlay} accessibilityLabel={active ? `Pausar ${track.title}` : `Reproducir ${track.title}`}>
        <View style={{ width: size, height: size, borderRadius: 16, overflow: 'hidden' }}>
          <Artwork src={track.thumbnail} style={{ width: size, height: size }} radius={16} iconSize={36} />
          <View
            pointerEvents="none"
            style={{
              position: 'absolute',
              left: 0,
              right: 0,
              bottom: 0,
              height: '50%',
              backgroundColor: 'rgba(0,0,0,0.35)',
            }}
          />
          <View
            className="items-center justify-center"
            style={{
              position: 'absolute',
              bottom: 10,
              right: 10,
              width: 44,
              height: 44,
              borderRadius: 999,
              backgroundColor: colors.accent,
              opacity: isCurrent ? 1 : 0.92,
            }}
          >
            {active ? <PauseIcon size={17} color={colors.accentInk} /> : <PlayIcon size={17} color={colors.accentInk} />}
          </View>
          {active ? (
            <View
              className="items-center justify-center"
              style={{
                position: 'absolute',
                top: 10,
                left: 10,
                width: 26,
                height: 26,
                borderRadius: 999,
                backgroundColor: 'rgba(0,0,0,0.5)',
              }}
            >
              <PlayingBars color={colors.accent} size={12} />
            </View>
          ) : null}
        </View>
      </Pressable>

      <View className="mt-2.5 flex-row items-center gap-1.5">
        {isCurrent ? <PlayingBars color={colors.accent} size={12} /> : null}
        <Text
          numberOfLines={1}
          className="flex-1 text-sm font-semibold"
          style={{ color: isCurrent ? colors.accent : colors.ink }}
        >
          {track.title}
        </Text>
      </View>
      <Text numberOfLines={1} className="text-xs" style={{ color: colors.faint }}>
        {track.artist}
      </Text>
    </View>
  )
}
