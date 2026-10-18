import { type ReactNode } from 'react'
import { Pressable, ScrollView, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import {
  ChevronRightIcon,
  ClockIcon,
  HeartFilledIcon,
  PlayIcon,
  SettingsIcon,
  SparklesIcon,
  WaveformIcon,
} from '@/components/Icons'
import { Artwork } from '@/components/Artwork'
import { Logo } from '@/components/Logo'
import { Button, Reveal } from '@/components/ui'
import { HomeSuggestions, SuggestionRail } from '@/features/home/SuggestionRail'
import { PlaylistCard } from '@/features/library/PlaylistCard'
import { TrackCard } from '@/features/library/TrackCard'
import { TrackRow } from '@/features/library/TrackRow'
import { demoPlaylist, demoTracks } from '@/shared/data/demo'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useSettingsStore } from '@/shared/store/settingsStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { RootStackParamList } from '@/navigation/types'

function greeting(): string {
  const hour = new Date().getHours()
  if (hour < 6) return 'Buenas noches'
  if (hour < 13) return 'Buenos días'
  if (hour < 20) return 'Buenas tardes'
  return 'Buenas noches'
}

function Section({
  title,
  icon,
  action,
  children,
}: {
  title: string
  icon?: ReactNode
  action?: ReactNode
  children: ReactNode
}) {
  const colors = useThemeColors()
  return (
    <Reveal style={{ marginTop: 28 }}>
      <View className="mb-3 flex-row items-center justify-between px-5">
        <View className="flex-row items-center gap-2">
          {icon}
          <Text className="text-[1.05rem] font-semibold tracking-tight" style={{ color: colors.ink }}>
            {title}
          </Text>
        </View>
        {action}
      </View>
      {children}
    </Reveal>
  )
}

export function HomePage() {
  const colors = useThemeColors()
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>()
  const playlists = useLibraryStore((state) => state.playlists)
  const history = useLibraryStore((state) => state.history)
  const favorites = useLibraryStore((state) => state.favorites)
  const playQueue = usePlayerStore((state) => state.playQueue)
  const algoGenres = useSettingsStore((state) => state.genres)
  const algoArtists = useSettingsStore((state) => state.artists)
  const algorithmReady = algoGenres.length > 0 || algoArtists.length > 0

  const isEmpty = playlists.length === 0 && history.length === 0 && favorites.length === 0
  const quickTiles = [
    ...(favorites.length > 0 ? [{ kind: 'favorites' as const }] : []),
    ...playlists.slice(0, 6).map((playlist) => ({ kind: 'playlist' as const, playlist })),
  ].slice(0, 6)

  return (
    <ScrollView
      showsVerticalScrollIndicator={false}
      contentContainerStyle={{ paddingBottom: 16 }}
      style={{ backgroundColor: 'transparent' }}
    >
      <View className="flex-row items-start justify-between gap-3 px-5 pb-2 pt-5">
        <View className="min-w-0 flex-1">
          <Logo size={16} style={{ marginBottom: 12 }} />
          <Text className="text-[1.6rem] font-semibold tracking-tight" style={{ color: colors.ink }}>
            {greeting()}
          </Text>
          <Text className="mt-1 text-sm" style={{ color: colors.faint }}>
            Tu música, con letras sincronizadas
          </Text>
        </View>
        <Pressable
          onPress={() => navigation.navigate('Settings')}
          accessibilityLabel="Ajustes"
          className="h-11 w-11 items-center justify-center rounded-full"
        >
          <SettingsIcon size={22} color={colors.muted} />
        </Pressable>
      </View>

      {isEmpty ? (
        <View className="mt-5 px-5">
          <View
            className="relative overflow-hidden rounded-3xl p-5"
            style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
          >
            <View
              pointerEvents="none"
              style={{
                position: 'absolute',
                right: -40,
                top: -40,
                width: 160,
                height: 160,
                borderRadius: 999,
                backgroundColor: colors.accent,
                opacity: 0.28,
              }}
            />
            <Text className="text-[11px] uppercase tracking-wider" style={{ color: colors.muted }}>
              Empieza ahora
            </Text>
            <Text className="mt-1 text-xl font-bold" style={{ color: colors.ink }}>
              {demoPlaylist.name}
            </Text>
            <Text className="mt-1 text-sm" style={{ color: colors.muted }}>
              {demoPlaylist.description}
            </Text>
            <Button
              style={{ marginTop: 16, alignSelf: 'flex-start' }}
              icon={<PlayIcon size={18} color={colors.primaryInk} />}
              onPress={() => void playQueue(demoPlaylist.tracks, 0)}
            >
              Reproducir
            </Button>
          </View>
        </View>
      ) : null}

      {quickTiles.length > 0 ? (
        <View className="mt-4 flex-row flex-wrap gap-2.5 px-5">
          {quickTiles.map((tile) =>
            tile.kind === 'favorites' ? (
              <Pressable
                key="favorites"
                onPress={() => navigation.navigate('Tabs', { screen: 'Library' })}
                className="flex-row items-center gap-3 overflow-hidden rounded-xl pr-3"
                style={{
                  width: '48%',
                  backgroundColor: colors.overlay2,
                  borderWidth: 1,
                  borderColor: colors.hair,
                }}
              >
                <View
                  className="h-14 w-14 items-center justify-center"
                  style={{ backgroundColor: colors.accent }}
                >
                  <HeartFilledIcon size={22} color={colors.accentInk} />
                </View>
                <View className="min-w-0 flex-1 py-2">
                  <Text numberOfLines={1} className="text-[0.85rem] font-medium" style={{ color: colors.ink }}>
                    Tus favoritos
                  </Text>
                  <Text numberOfLines={1} className="mt-0.5 text-[0.7rem]" style={{ color: colors.faint }}>
                    {favorites.length} canciones
                  </Text>
                </View>
              </Pressable>
            ) : (
              <Pressable
                key={tile.playlist.id}
                onPress={() => navigation.navigate('Playlist', { id: tile.playlist.id })}
                className="flex-row items-center gap-3 overflow-hidden rounded-xl pr-3"
                style={{
                  width: '48%',
                  backgroundColor: colors.overlay2,
                  borderWidth: 1,
                  borderColor: colors.hair,
                }}
              >
                <View className="h-14 w-14" style={{ backgroundColor: colors.surface3 }}>
                  {tile.playlist.thumbnail || tile.playlist.tracks[0]?.thumbnail ? (
                    <Artwork
                      src={tile.playlist.thumbnail || tile.playlist.tracks[0]?.thumbnail}
                      style={{ width: 56, height: 56 }}
                      radius={0}
                      iconSize={20}
                    />
                  ) : null}
                </View>
                <View className="min-w-0 flex-1 py-2">
                  <Text numberOfLines={1} className="text-[0.85rem] font-medium" style={{ color: colors.ink }}>
                    {tile.playlist.name}
                  </Text>
                  <Text numberOfLines={1} className="mt-0.5 text-[0.7rem]" style={{ color: colors.faint }}>
                    {tile.playlist.tracks.length} canciones
                  </Text>
                </View>
              </Pressable>
            ),
          )}
        </View>
      ) : null}

      {isEmpty ? (
        <Section title="Canciones de muestra" icon={<SparklesIcon size={18} color={colors.accent} />}>
          <View className="px-3">
            {demoTracks.slice(0, 6).map((track, index) => (
              <TrackRow key={track.id} track={track} queue={demoTracks} index={index} />
            ))}
          </View>
        </Section>
      ) : null}

      <HomeSuggestions />

      {algorithmReady ? (
        <SuggestionRail
          algorithm
          title="Hecho para ti"
          icon={<SparklesIcon size={18} color={colors.accent} />}
          offset={1}
        />
      ) : null}

      <SuggestionRail
        title="Éxitos del momento"
        query="pop en español"
        icon={<SparklesIcon size={18} color={colors.accent} />}
        offset={2}
      />

      <SuggestionRail
        title="Clásicos que nunca fallan"
        query="rock clásicos"
        icon={<WaveformIcon size={18} color={colors.accent} />}
        offset={4}
      />

      <SuggestionRail
        title="Para descubrir"
        query="música pop 2024"
        icon={<SparklesIcon size={18} color={colors.accent} />}
        offset={6}
      />

      {history.length > 0 ? (
        <Section
          title="Escuchando ahora"
          icon={<ClockIcon size={18} color={colors.accent} />}
          action={
            <Pressable onPress={() => navigation.navigate('Tabs', { screen: 'Library' })}>
              <Text className="text-xs font-medium" style={{ color: colors.faint }}>
                Ver todo
              </Text>
            </Pressable>
          }
        >
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={{ gap: 14, paddingHorizontal: 20, paddingBottom: 4 }}
          >
            {history.slice(0, 8).map((track, index) => (
              <TrackCard key={`${track.source}-${track.id}-${index}`} track={track} queue={history} index={index} />
            ))}
          </ScrollView>
        </Section>
      ) : null}

      <Section
        title="Tus playlists"
        action={
          playlists.length > 0 ? (
            <Pressable onPress={() => navigation.navigate('Tabs', { screen: 'Library' })}>
              <Text className="text-xs font-medium" style={{ color: colors.faint }}>
                Ver todas
              </Text>
            </Pressable>
          ) : null
        }
      >
        {playlists.length > 0 ? (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={{ gap: 16, paddingHorizontal: 20, paddingBottom: 4 }}
          >
            {playlists.map((playlist) => (
              <PlaylistCard key={playlist.id} playlist={playlist} size={144} />
            ))}
          </ScrollView>
        ) : (
          <Text className="px-5 text-sm" style={{ color: colors.faint }}>
            Aún no has importado playlists. Añádelas desde la Biblioteca.
          </Text>
        )}
      </Section>

      {history.length > 0 ? (
        <Section
          title="Vuelve a escuchar"
          action={<ChevronRightIcon size={16} color={colors.faint} />}
        >
          <View className="px-3">
            {history.slice(0, 6).map((track, index) => (
              <TrackRow key={`${track.source}-${track.id}-${index}`} track={track} queue={history} index={index} />
            ))}
          </View>
        </Section>
      ) : null}
    </ScrollView>
  )
}
