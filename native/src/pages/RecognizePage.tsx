import { useCallback, useEffect, useRef, useState } from 'react'
import { Pressable, ScrollView, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import Svg, { Circle } from 'react-native-svg'
import { RecordingPresets, requestRecordingPermissionsAsync, useAudioRecorder } from 'expo-audio'
import { Artwork } from '@/components/Artwork'
import { HeartFilledIcon, HeartIcon, MicIcon, PlayIcon, SearchIcon } from '@/components/Icons'
import { Button, IconButton, Spinner } from '@/components/ui'
import { recognizeAudio, type RecognizedSong } from '@/shared/api/recognize'
import { searchMusic } from '@/shared/api/youtube'
import { HAS_RECOGNITION } from '@/shared/config'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { Track } from '@/shared/types/track'
import type { RootStackParamList } from '@/navigation/types'

type Phase = 'idle' | 'listening' | 'analyzing' | 'found' | 'notfound' | 'error'

const SAMPLE_MS = 7000
const RING_RADIUS = 48
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS

export function RecognizePage() {
  const colors = useThemeColors()
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>()
  const [phase, setPhase] = useState<Phase>('idle')
  const [song, setSong] = useState<RecognizedSong | null>(null)
  const [progress, setProgress] = useState(0)

  const notify = useUiStore((state) => state.notify)
  const playTrack = usePlayerStore((state) => state.playTrack)
  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const stopTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const clearTimers = useCallback(() => {
    if (timerRef.current) clearInterval(timerRef.current)
    if (stopTimeoutRef.current) clearTimeout(stopTimeoutRef.current)
    timerRef.current = null
    stopTimeoutRef.current = null
  }, [])

  useEffect(() => clearTimers, [clearTimers])

  const analyze = useCallback(
    async (uri: string) => {
      setPhase('analyzing')
      try {
        const result = await recognizeAudio({ uri, name: 'sample.m4a', type: 'audio/m4a' })
        if (result && result.title) {
          setSong(result)
          setPhase('found')
        } else {
          setPhase('notfound')
        }
      } catch {
        setPhase('error')
      }
    },
    [],
  )

  const finishRecording = useCallback(async () => {
    clearTimers()
    setProgress(1)
    try {
      await recorder.stop()
      const uri = recorder.uri
      if (!uri) {
        setPhase('error')
        return
      }
      await analyze(uri)
    } catch {
      setPhase('error')
    }
  }, [analyze, clearTimers, recorder])

  const startListening = useCallback(async () => {
    if (!HAS_RECOGNITION) {
      notify('Falta configurar EXPO_PUBLIC_AUDD_API_KEY para reconocer música.', 'error')
      return
    }
    try {
      const permission = await requestRecordingPermissionsAsync()
      if (!permission.granted) {
        notify('No se pudo acceder al micrófono.', 'error')
        return
      }

      setSong(null)
      setProgress(0)
      setPhase('listening')
      recorder.record()

      const started = Date.now()
      timerRef.current = setInterval(() => {
        setProgress(Math.min(1, (Date.now() - started) / SAMPLE_MS))
      }, 100)
      stopTimeoutRef.current = setTimeout(() => void finishRecording(), SAMPLE_MS)
    } catch {
      notify('No se pudo acceder al micrófono.', 'error')
      setPhase('idle')
    }
  }, [finishRecording, notify, recorder])

  const stopEarly = useCallback(() => {
    void finishRecording()
  }, [finishRecording])

  const playRecognized = useCallback(async () => {
    if (!song) return
    setPhase('analyzing')
    try {
      const results = await searchMusic(`${song.title} ${song.artist}`, 10)
      if (results.length === 0) {
        notify('No se encontró en YouTube.', 'error')
        setPhase('found')
        return
      }
      await playTrack(results[0], results)
      setPhase('found')
    } catch {
      notify('No se pudo reproducir la canción.', 'error')
      setPhase('found')
    }
  }, [notify, playTrack, song])

  const favoriteRecognized = useCallback(async () => {
    if (!song) return
    const results = await searchMusic(`${song.title} ${song.artist}`, 5)
    const track: Track =
      results[0] ??
      ({
        id: `recognized-${song.title}-${song.artist}`,
        title: song.title,
        artist: song.artist,
        thumbnail: song.cover ?? '',
        duration: 0,
        source: 'youtube',
      } as Track)
    toggleFavorite(track)
  }, [song, toggleFavorite])

  const isFavorite = song
    ? favorites.some(
        (item) =>
          item.title.toLowerCase() === song.title.toLowerCase() &&
          item.artist.toLowerCase() === song.artist.toLowerCase(),
      )
    : false

  return (
    <ScrollView contentContainerStyle={{ flexGrow: 1, paddingBottom: 24 }} showsVerticalScrollIndicator={false}>
      <View className="flex-1 px-6 pb-6 pt-6">
        <View className="items-center">
          <Text className="text-[1.6rem] font-semibold tracking-tight" style={{ color: colors.ink }}>
            Reconocer música
          </Text>
          <Text className="mt-1 text-center text-sm" style={{ color: colors.faint }}>
            Acerca el micrófono a la música y déjame escuchar unos segundos.
          </Text>
        </View>

        <View className="flex-1 items-center justify-center py-10">
          <View className="items-center justify-center">
            <Pressable
              onPress={phase === 'listening' ? stopEarly : startListening}
              disabled={phase === 'analyzing'}
              accessibilityLabel={phase === 'listening' ? 'Detener' : 'Escuchar'}
              className="h-32 w-32 items-center justify-center rounded-full"
              style={{
                backgroundColor: phase === 'listening' ? colors.accent : colors.overlay1,
                borderWidth: phase === 'listening' ? 0 : 1,
                borderColor: colors.hair,
                opacity: phase === 'analyzing' ? 0.6 : 1,
              }}
            >
              {phase === 'analyzing' ? (
                <Spinner size={40} color={colors.ink} />
              ) : (
                <MicIcon size={46} color={phase === 'listening' ? colors.accentInk : colors.ink} />
              )}
            </Pressable>

            {phase === 'listening' ? (
              <View style={{ position: 'absolute', width: 160, height: 160 }}>
                <Svg width={160} height={160} viewBox="0 0 100 100">
                  <Circle
                    cx="50"
                    cy="50"
                    r={RING_RADIUS}
                    fill="none"
                    stroke={colors.overlay3}
                    strokeWidth={2}
                  />
                  <Circle
                    cx="50"
                    cy="50"
                    r={RING_RADIUS}
                    fill="none"
                    stroke={colors.accent}
                    strokeWidth={2.5}
                    strokeLinecap="round"
                    strokeDasharray={`${RING_CIRCUMFERENCE}`}
                    strokeDashoffset={`${RING_CIRCUMFERENCE * (1 - progress)}`}
                    transform="rotate(-90 50 50)"
                  />
                </Svg>
              </View>
            ) : null}
          </View>

          <Text className="mt-8 h-6 text-sm font-medium" style={{ color: colors.muted }}>
            {phase === 'idle' && 'Toca para escuchar'}
            {phase === 'listening' && 'Escuchando…'}
            {phase === 'analyzing' && 'Analizando…'}
            {phase === 'notfound' && 'No reconocí la canción. Inténtalo de nuevo.'}
            {phase === 'error' && 'Hubo un problema al analizar el audio.'}
          </Text>
        </View>

        {phase === 'found' && song ? (
          <View
            className="mt-2 flex-row items-center gap-3.5 rounded-2xl p-3.5"
            style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
          >
            <Artwork src={song.cover} style={{ width: 64, height: 64 }} radius={12} iconSize={26} />
            <View className="min-w-0 flex-1">
              <Text numberOfLines={1} className="font-semibold" style={{ color: colors.ink }}>
                {song.title}
              </Text>
              <Text numberOfLines={1} className="text-sm" style={{ color: colors.faint }}>
                {song.artist}
                {song.album ? ` · ${song.album}` : ''}
              </Text>
              <View className="mt-2.5 flex-row items-center gap-2">
                <Button
                  size="sm"
                  icon={<PlayIcon size={16} color={colors.primaryInk} />}
                  onPress={() => void playRecognized()}
                >
                  Reproducir
                </Button>
                <IconButton
                  label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
                  size="sm"
                  active={isFavorite}
                  onPress={() => void favoriteRecognized()}
                >
                  {isFavorite ? (
                    <HeartFilledIcon size={18} color={colors.accent} />
                  ) : (
                    <HeartIcon size={18} color={colors.ink} />
                  )}
                </IconButton>
                <IconButton
                  label="Buscar en YouTube"
                  size="sm"
                  onPress={() =>
                    navigation.navigate('Tabs', {
                      screen: 'Search',
                      params: { q: `${song.title} ${song.artist}` },
                    })
                  }
                >
                  <SearchIcon size={18} color={colors.ink} />
                </IconButton>
              </View>
            </View>
          </View>
        ) : null}

        {!HAS_RECOGNITION ? (
          <View
            className="mt-3 rounded-2xl p-3.5"
            style={{ borderWidth: 1, borderColor: colors.accent + '40', backgroundColor: colors.accent + '14' }}
          >
            <Text className="text-xs leading-relaxed" style={{ color: colors.muted }}>
              <Text style={{ fontWeight: '600', color: colors.ink }}>Falta la clave de reconocimiento.</Text> Crea
              una cuenta gratuita en AudD y añade{' '}
              <Text style={{ color: colors.accentSoft }}>EXPO_PUBLIC_AUDD_API_KEY</Text> en tu archivo{' '}
              <Text style={{ color: colors.accentSoft }}>.env</Text> para activar esta función.
            </Text>
          </View>
        ) : null}
      </View>
    </ScrollView>
  )
}
