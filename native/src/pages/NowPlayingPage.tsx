import { useState } from 'react'
import { Pressable, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Artwork } from '@/components/Artwork'
import {
  ChevronDownIcon,
  DiscIcon,
  HeartFilledIcon,
  HeartIcon,
  ListMusicIcon,
  MicIcon,
  MoreIcon,
  PlusIcon,
  VolumeIcon,
} from '@/components/Icons'
import { IconButton } from '@/components/ui'
import { LyricsFullscreen } from '@/features/lyrics/LyricsSection'
import { useLyrics } from '@/features/lyrics/useLyrics'
import { PlayerControls, ProgressBar, VolumeControl } from '@/features/player/PlayerControls'
import { TrackMenu } from '@/features/library/TrackRow'
import { DynamicBackground, useArtworkColors } from '@/features/visuals/DynamicBackground'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { RootStackParamList } from '@/navigation/types'

export function NowPlayingPage() {
  const colors = useThemeColors()
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>()
  const track = usePlayerStore(selectCurrentTrack)
  const queue = usePlayerStore((state) => state.queue)
  const position = usePlayerStore((state) => state.position)
  const duration = usePlayerStore((state) => state.duration)
  const seek = usePlayerStore((state) => state.seek)
  const setQueueOpen = useUiStore((state) => state.setQueueOpen)

  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  const { lyrics, status } = useLyrics(track)
  const artworkColors = useArtworkColors(track?.thumbnail)

  const [showLyrics, setShowLyrics] = useState(false)
  const [showVolume, setShowVolume] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  if (!track) {
    return (
      <View className="flex-1 items-center justify-center gap-3 px-8">
        <DiscIcon size={40} color={colors.faint} />
        <Text className="text-center text-sm" style={{ color: colors.faint }}>
          No hay ninguna canción reproduciéndose.
        </Text>
        <Pressable onPress={() => navigation.navigate('Tabs', { screen: 'Search' })}>
          <Text className="text-sm font-medium" style={{ color: colors.accent }}>
            Buscar música
          </Text>
        </Pressable>
      </View>
    )
  }

  const isFavorite = favorites.some((item) => sameTrack(item, track))
  const hasLyrics = status === 'ready' && !!lyrics

  return (
    <View className="flex-1 overflow-hidden">
      <DynamicBackground image={track.thumbnail} colors={artworkColors} intensity="strong" />

      <View className="flex-1 px-5 pb-3 pt-2">
        <View className="flex-row items-center justify-between">
          <IconButton label="Minimizar" onPress={() => navigation.goBack()}>
            <ChevronDownIcon size={26} color={colors.ink} />
          </IconButton>
          <Text className="text-[11px] font-medium uppercase tracking-[0.18em]" style={{ color: colors.faint }}>
            Reproduciendo
          </Text>
          <IconButton label="Más opciones" onPress={() => setMenuOpen(true)}>
            <MoreIcon size={22} color={colors.ink} />
          </IconButton>
        </View>

        <View className="flex-1 items-center justify-center">
          <Artwork
            src={track.thumbnail}
            style={{ width: 240, height: 240, borderRadius: 28 }}
            radius={28}
            iconSize={56}
          />
        </View>

        <View className="mt-5 flex-row items-center justify-between gap-4">
          <View className="min-w-0 flex-1">
            <Text numberOfLines={1} className="text-[1.35rem] font-semibold tracking-tight" style={{ color: colors.ink }}>
              {track.title}
            </Text>
            <Text numberOfLines={1} className="mt-0.5 text-sm" style={{ color: colors.faint }}>
              {track.artist}
            </Text>
          </View>
          <IconButton
            label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
            onPress={() => toggleFavorite(track)}
            active={isFavorite}
          >
            {isFavorite ? (
              <HeartFilledIcon size={26} color={colors.accent} />
            ) : (
              <HeartIcon size={26} color={colors.ink} />
            )}
          </IconButton>
        </View>

        <View className="mt-3">
          <ProgressBar position={position} duration={duration || track.duration} onSeek={seek} />
        </View>

        <View className="mt-2">
          <PlayerControls />
        </View>

        {showVolume ? (
          <View
            className="mt-3 rounded-2xl px-3.5 py-2"
            style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
          >
            <VolumeControl />
          </View>
        ) : null}

        <View
          className="mt-3 flex-row items-center justify-between rounded-2xl px-1.5 py-1"
          style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
        >
          <IconButton
            label={hasLyrics ? 'Ver letra' : 'Letra no disponible'}
            onPress={() => setShowLyrics(true)}
            disabled={!lyrics}
          >
            <MicIcon size={21} color={colors.ink} />
          </IconButton>
          <IconButton label="Cola de reproducción" onPress={() => setQueueOpen(true)}>
            <ListMusicIcon size={21} color={colors.ink} />
          </IconButton>
          <IconButton label="Volumen" active={showVolume} onPress={() => setShowVolume((value) => !value)}>
            <VolumeIcon size={21} color={colors.ink} />
          </IconButton>
          <IconButton label="Añadir a una lista" onPress={() => setMenuOpen(true)}>
            <PlusIcon size={21} color={colors.ink} />
          </IconButton>
        </View>
      </View>

      {showLyrics && lyrics ? (
        <LyricsFullscreen
          track={track}
          lyrics={lyrics}
          time={position}
          colors={artworkColors}
          onClose={() => setShowLyrics(false)}
        />
      ) : null}

      {menuOpen ? <TrackMenu track={track} queue={queue} onClose={() => setMenuOpen(false)} /> : null}
    </View>
  )
}
