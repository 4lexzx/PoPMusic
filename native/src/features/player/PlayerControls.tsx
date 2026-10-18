import { useState } from 'react'
import { Pressable, Text, View } from 'react-native'
import {
  NextIcon,
  PauseIcon,
  PlayIcon,
  PrevIcon,
  RepeatIcon,
  RepeatOneIcon,
  ShuffleIcon,
  VolumeIcon,
  VolumeMuteIcon,
} from '@/components/Icons'
import { IconButton, Slider, Spinner } from '@/components/ui'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'
import { useThemeColors } from '@/shared/theme/theme'
import { formatDuration } from '@/shared/utils/time'

interface ProgressBarProps {
  position: number
  duration: number
  onSeek: (seconds: number) => void
}

/** Barra de progreso con previsualización mientras se arrastra. */
export function ProgressBar({ position, duration, onSeek }: ProgressBarProps) {
  const colors = useThemeColors()
  const [scrub, setScrub] = useState<number | null>(null)
  const value = duration > 0 ? position / duration : 0
  const shown = scrub ?? value

  return (
    <View className="w-full">
      <Slider
        value={shown}
        trackColor={colors.overlay3}
        fillColor={colors.accent}
        thumbColor={colors.ink}
        onChange={(next) => setScrub(next)}
        onCommit={(next) => {
          setScrub(null)
          if (duration > 0) onSeek(next * duration)
        }}
      />
      <View className="flex-row justify-between" style={{ marginTop: 2 }}>
        <Text className="text-[11px]" style={{ color: colors.faint }}>
          {formatDuration(shown * duration)}
        </Text>
        <Text className="text-[11px]" style={{ color: colors.faint }}>
          -{formatDuration(Math.max(0, duration - shown * duration))}
        </Text>
      </View>
    </View>
  )
}

/** Controles de reproducción principales. */
export function PlayerControls() {
  const colors = useThemeColors()
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const isBusy = usePlayerStore((state) => state.isLoadingTrack || state.isBuffering)
  const shuffle = usePlayerStore((state) => state.shuffle)
  const repeat = usePlayerStore((state) => state.repeat)
  const hasTrack = usePlayerStore((state) => state.queue.length > 0)

  const toggle = usePlayerStore((state) => state.toggle)
  const next = usePlayerStore((state) => state.next)
  const prev = usePlayerStore((state) => state.prev)
  const toggleShuffle = usePlayerStore((state) => state.toggleShuffle)
  const cycleRepeat = usePlayerStore((state) => state.cycleRepeat)

  return (
    <View className="flex-row items-center justify-between">
      <IconButton label="Aleatorio" onPress={toggleShuffle} active={shuffle}>
        <ShuffleIcon size={22} color={shuffle ? colors.accent : colors.muted} />
      </IconButton>

      <IconButton label="Anterior" onPress={() => prev()} disabled={!hasTrack} size="lg">
        <PrevIcon size={28} color={colors.ink} />
      </IconButton>

      <Pressable
        onPress={hasTrack ? toggle : undefined}
        disabled={!hasTrack}
        accessibilityLabel={isPlaying ? 'Pausar' : 'Reproducir'}
        className="items-center justify-center rounded-full"
        style={{ width: 64, height: 64, backgroundColor: colors.ink, opacity: hasTrack ? 1 : 0.5 }}
      >
        {isBusy ? (
          <Spinner size={26} color={colors.bg} />
        ) : isPlaying ? (
          <PauseIcon size={30} color={colors.bg} />
        ) : (
          <PlayIcon size={30} color={colors.bg} />
        )}
      </Pressable>

      <IconButton label="Siguiente" onPress={() => next()} disabled={!hasTrack} size="lg">
        <NextIcon size={28} color={colors.ink} />
      </IconButton>

      <IconButton label="Repetir" onPress={cycleRepeat} active={repeat !== 'off'}>
        {repeat === 'one' ? (
          <RepeatOneIcon size={22} color={colors.accent} />
        ) : (
          <RepeatIcon size={22} color={repeat === 'all' ? colors.accent : colors.muted} />
        )}
      </IconButton>
    </View>
  )
}

/** Control de volumen compacto (usa el mismo store). */
export function VolumeControl() {
  const colors = useThemeColors()
  const volume = usePlayerStore((state) => state.volume)
  const muted = usePlayerStore((state) => state.muted)
  const setVolume = usePlayerStore((state) => state.setVolume)
  const toggleMute = usePlayerStore((state) => state.toggleMute)
  const track = usePlayerStore(selectCurrentTrack)

  if (!track) return null

  const value = muted ? 0 : volume / 100

  return (
    <View className="flex-row items-center gap-3">
      <Pressable onPress={toggleMute} accessibilityLabel={muted ? 'Activar sonido' : 'Silenciar'}>
        {muted ? (
          <VolumeMuteIcon size={20} color={colors.muted} />
        ) : (
          <VolumeIcon size={20} color={colors.muted} />
        )}
      </Pressable>
      <View className="flex-1">
        <Slider
          value={value}
          trackColor={colors.overlay3}
          fillColor={colors.ink}
          onChange={(next) => setVolume(next * 100)}
        />
      </View>
    </View>
  )
}
