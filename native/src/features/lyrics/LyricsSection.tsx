import { useEffect, useMemo, useRef, useState, type ComponentRef } from 'react'
import { Modal, Pressable, ScrollView, Text, View } from 'react-native'
import { CloseIcon, MinusIcon, MusicIcon, PlusIcon } from '@/components/Icons'
import { Spinner } from '@/components/ui'
import { DynamicBackground, type useArtworkColors } from '@/features/visuals/DynamicBackground'
import { useThemeColors } from '@/shared/theme/theme'
import type { Lyrics } from '@/shared/types/lyrics'
import type { Track } from '@/shared/types/track'
import { findActiveLine } from '@/shared/utils/lrc'

const OFFSET_STEP = 0.5
const OFFSET_LIMIT = 10

interface LyricsScrollerProps {
  lyrics: Lyrics
  time: number
  offset?: number
  size?: 'md' | 'lg'
}

/** Letras con resaltado tipo karaoke: línea activa nítida, resto atenuado. */
export function LyricsScroller({ lyrics, time, offset = 0, size = 'md' }: LyricsScrollerProps) {
  const colors = useThemeColors()
  const adjusted = time + offset
  const activeIndex = useMemo(() => findActiveLine(lyrics.lines, adjusted), [lyrics, adjusted])

  const scrollRef = useRef<ComponentRef<typeof ScrollView> | null>(null)
  const offsets = useRef<Record<number, number>>({})
  const [viewportHeight, setViewportHeight] = useState(0)

  useEffect(() => {
    const y = offsets.current[activeIndex]
    if (y === undefined || viewportHeight === 0) return
    scrollRef.current?.scrollTo({ y: Math.max(0, y - viewportHeight / 2), animated: true })
  }, [activeIndex, viewportHeight])

  const baseSize = size === 'lg' ? 26 : 18
  const activeSize = size === 'lg' ? 28 : 20

  return (
    <ScrollView
      ref={scrollRef}
      showsVerticalScrollIndicator={false}
      onLayout={(event) => setViewportHeight(event.nativeEvent.layout.height)}
      contentContainerStyle={{ paddingVertical: size === 'lg' ? 16 : 8 }}
    >
      {lyrics.lines.map((line, index) => {
        const isActive = index === activeIndex
        const isPast = index < activeIndex

        if (!line.text.trim()) {
          return (
            <View
              key={index}
              onLayout={(event) => {
                offsets.current[index] = event.nativeEvent.layout.y
              }}
              style={{ height: 16 }}
            />
          )
        }

        return (
          <View
            key={index}
            onLayout={(event) => {
              offsets.current[index] = event.nativeEvent.layout.y
            }}
            style={{ marginVertical: size === 'lg' ? 8 : 6 }}
          >
            <Text
              style={{
                fontSize: isActive ? activeSize : baseSize,
                lineHeight: (isActive ? activeSize : baseSize) * 1.4,
                fontWeight: '600',
                color: isActive ? '#ffffff' : colors.muted,
                opacity: isActive ? 1 : isPast ? 0.55 : 0.4,
              }}
            >
              {isActive && line.words.length > 0
                ? line.words.map((word, wordIndex) => {
                    const sung = adjusted >= word.time
                    return (
                      <Text key={wordIndex} style={{ opacity: sung ? 1 : 0.38 }}>
                        {word.text}
                        {wordIndex < line.words.length - 1 ? ' ' : ''}
                      </Text>
                    )
                  })
                : line.text}
            </Text>
          </View>
        )
      })}
    </ScrollView>
  )
}

interface LyricsFullscreenProps {
  track: Track
  lyrics: Lyrics
  time: number
  colors: ReturnType<typeof useArtworkColors>
  onClose: () => void
}

/** Vista de letra a pantalla completa con control de retardo. */
export function LyricsFullscreen({ track, lyrics, time, colors, onClose }: LyricsFullscreenProps) {
  const theme = useThemeColors()
  const [offset, setOffset] = useState(0)

  const clampOffset = (value: number) =>
    Math.max(-OFFSET_LIMIT, Math.min(OFFSET_LIMIT, Math.round(value * 10) / 10))

  return (
    <Modal visible animationType="fade" onRequestClose={onClose}>
      <View style={{ flex: 1 }}>
        <DynamicBackground image={track.thumbnail} colors={colors} intensity="strong" />
        <View className="flex-row items-center justify-between gap-3 px-5 py-4">
          <View className="min-w-0 flex-1 flex-row items-center gap-3">
            <Pressable
              onPress={onClose}
              accessibilityLabel="Cerrar letra"
              className="h-10 w-10 items-center justify-center rounded-full"
              style={{ backgroundColor: theme.overlay2 }}
            >
              <CloseIcon size={20} color={theme.ink} />
            </Pressable>
            <View className="min-w-0 flex-1">
              <Text numberOfLines={1} className="text-sm font-semibold" style={{ color: theme.ink }}>
                {track.title}
              </Text>
              <Text numberOfLines={1} className="text-xs" style={{ color: theme.faint }}>
                {track.artist}
              </Text>
            </View>
          </View>

          <View
            className="flex-row items-center gap-1 rounded-full py-1 pl-1 pr-2"
            style={{ backgroundColor: theme.overlay2 }}
          >
            <Pressable
              onPress={() => setOffset((value) => clampOffset(value - OFFSET_STEP))}
              accessibilityLabel="Atrasar letra"
              className="h-8 w-8 items-center justify-center rounded-full"
            >
              <MinusIcon size={16} color={theme.ink} />
            </Pressable>
            <Text className="text-center text-[11px]" style={{ minWidth: 52, color: theme.ink }}>
              {offset > 0 ? '+' : ''}
              {offset.toFixed(1)}s
            </Text>
            <Pressable
              onPress={() => setOffset((value) => clampOffset(value + OFFSET_STEP))}
              accessibilityLabel="Adelantar letra"
              className="h-8 w-8 items-center justify-center rounded-full"
            >
              <PlusIcon size={16} color={theme.ink} />
            </Pressable>
          </View>
        </View>

        <View className="flex-1 px-6">
          <LyricsScroller lyrics={lyrics} time={time} offset={offset} size="lg" />
        </View>

        <Pressable onPress={onClose} className="items-center pb-6">
          <Text className="text-xs" style={{ color: theme.faint }}>
            Toca para volver
          </Text>
        </Pressable>
      </View>
    </Modal>
  )
}

interface LyricsSectionProps {
  track: Track
  lyrics: Lyrics | null
  status: 'idle' | 'loading' | 'ready' | 'empty' | 'error'
  time: number
  colors: ReturnType<typeof useArtworkColors>
}

/** Panel de letras reutilizable dentro de una pantalla. */
export function LyricsSection({ track, lyrics, status, time, colors }: LyricsSectionProps) {
  const theme = useThemeColors()
  const [fullscreen, setFullscreen] = useState(false)

  return (
    <>
      <View
        className="flex-1 overflow-hidden rounded-3xl px-5 py-4"
        style={{ backgroundColor: theme.overlay1, borderWidth: 1, borderColor: theme.hair }}
      >
        <View className="mb-2 flex-row items-center justify-between">
          <Text className="text-xs font-semibold uppercase tracking-wider" style={{ color: theme.faint }}>
            Letra
          </Text>
          {lyrics ? (
            <Pressable onPress={() => setFullscreen(true)}>
              <Text className="text-xs font-medium" style={{ color: theme.accent }}>
                Pantalla completa
              </Text>
            </Pressable>
          ) : null}
        </View>

        <View className="flex-1">
          {status === 'loading' ? (
            <View className="flex-1 items-center justify-center gap-3">
              <Spinner size={26} />
              <Text className="text-sm" style={{ color: theme.faint }}>
                Buscando la letra…
              </Text>
            </View>
          ) : null}

          {status === 'ready' && lyrics ? <LyricsScroller lyrics={lyrics} time={time} size="md" /> : null}

          {status === 'empty' || status === 'error' ? (
            <View className="flex-1 items-center justify-center gap-2 px-6">
              <MusicIcon size={30} color={theme.faint} />
              <Text className="text-center text-sm" style={{ color: theme.faint }}>
                {status === 'error' ? 'No se pudieron cargar las letras.' : 'Aún no hay letra para esta canción.'}
              </Text>
            </View>
          ) : null}

          {status === 'idle' ? (
            <View className="flex-1 items-center justify-center">
              <Text className="text-sm" style={{ color: theme.faint }}>
                Reproduce una canción para ver la letra.
              </Text>
            </View>
          ) : null}
        </View>
      </View>

      {fullscreen && lyrics ? (
        <LyricsFullscreen
          track={track}
          lyrics={lyrics}
          time={time}
          colors={colors}
          onClose={() => setFullscreen(false)}
        />
      ) : null}
    </>
  )
}
