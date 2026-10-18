import { useState, type ReactNode } from 'react'
import { Pressable, Text, TextInput, View } from 'react-native'
import { Artwork } from '@/components/Artwork'
import {
  HeartFilledIcon,
  HeartIcon,
  ListMusicIcon,
  MoreIcon,
  NextIcon,
  PauseIcon,
  PlayIcon,
  PlusIcon,
  TrashIcon,
} from '@/components/Icons'
import { Button, PlayingBars, Sheet } from '@/components/ui'
import { usePlayerStore } from '@/shared/store/playerStore'
import { sameTrack, useLibraryStore } from '@/shared/store/libraryStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'
import type { Track } from '@/shared/types/track'
import { formatDuration } from '@/shared/utils/time'

interface TrackRowProps {
  track: Track
  queue: Track[]
  index?: number
  showArtwork?: boolean
  showDuration?: boolean
  onRemove?: () => void
}

function Action({
  icon,
  label,
  onPress,
  tone = 'default',
}: {
  icon: ReactNode
  label: string
  onPress: () => void
  tone?: 'default' | 'danger'
}) {
  const colors = useThemeColors()
  return (
    <Pressable onPress={onPress} className="flex-row items-center gap-3 rounded-xl px-2 py-3">
      <View>{icon}</View>
      <Text style={{ color: tone === 'danger' ? colors.accent : colors.ink, fontSize: 14, fontWeight: '500' }}>
        {label}
      </Text>
    </Pressable>
  )
}

/** Menú de acciones de una pista (⋮). */
export function TrackMenu({
  track,
  queue,
  onRemove,
  onClose,
}: {
  track: Track
  queue: Track[]
  onRemove?: () => void
  onClose: () => void
}) {
  const [view, setView] = useState<'root' | 'playlists'>('root')
  const [newName, setNewName] = useState('')
  const colors = useThemeColors()

  const playTrack = usePlayerStore((state) => state.playTrack)
  const playNext = usePlayerStore((state) => state.playNext)
  const enqueue = usePlayerStore((state) => state.enqueue)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const currentTrack = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const toggle = usePlayerStore((state) => state.toggle)

  const playlists = useLibraryStore((state) => state.playlists)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)
  const addTracksToPlaylist = useLibraryStore((state) => state.addTracksToPlaylist)
  const createPlaylist = useLibraryStore((state) => state.createPlaylist)
  const favorites = useLibraryStore((state) => state.favorites)
  const notify = useUiStore((state) => state.notify)

  const isCurrent = currentTrack ? sameTrack(currentTrack, track) : false
  const isFavorite = favorites.some((item) => sameTrack(item, track))

  const run = (fn: () => void) => {
    fn()
    onClose()
  }

  return (
    <Sheet open onClose={onClose} title={view === 'root' ? track.title : 'Añadir a una lista'}>
      {view === 'root' ? (
        <View>
          <Action
            icon={
              isCurrent && isPlaying ? (
                <PauseIcon size={20} color={colors.muted} />
              ) : (
                <PlayIcon size={20} color={colors.muted} />
              )
            }
            label={isCurrent && isPlaying ? 'Pausar' : 'Reproducir'}
            onPress={() => run(() => (isCurrent ? toggle() : void playTrack(track, queue)))}
          />
          <Action
            icon={<NextIcon size={20} color={colors.muted} />}
            label="Reproducir a continuación"
            onPress={() => run(() => playNext(track))}
          />
          <Action
            icon={<ListMusicIcon size={20} color={colors.muted} />}
            label="Añadir a la cola"
            onPress={() => run(() => enqueue(track))}
          />
          <Action
            icon={
              isFavorite ? (
                <HeartFilledIcon size={20} color={colors.muted} />
              ) : (
                <HeartIcon size={20} color={colors.muted} />
              )
            }
            label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
            onPress={() => run(() => toggleFavorite(track))}
          />
          <Action
            icon={<PlusIcon size={20} color={colors.muted} />}
            label="Añadir a una lista"
            onPress={() => setView('playlists')}
          />
          {onRemove ? (
            <Action
              icon={<TrashIcon size={20} color={colors.accent} />}
              label="Eliminar de esta lista"
              tone="danger"
              onPress={() => run(onRemove)}
            />
          ) : null}
        </View>
      ) : (
        <View>
          <View className="flex-row items-center gap-2 pb-2">
            <TextInput
              value={newName}
              maxLength={40}
              onChangeText={setNewName}
              placeholder="Nueva lista…"
              placeholderTextColor={colors.faint}
              className="h-11 flex-1 rounded-xl border px-3.5 text-sm"
              style={{ borderColor: colors.hair, backgroundColor: colors.overlay1, color: colors.ink }}
            />
            <Button
              size="sm"
              disabled={!newName.trim()}
              onPress={() => {
                const playlist = createPlaylist(newName.trim())
                addTracksToPlaylist(playlist.id, [track])
                notify(`Añadido a «${playlist.name}»`)
                onClose()
              }}
            >
              Crear
            </Button>
          </View>
          {playlists.length > 0 ? (
            <View>
              {playlists.map((playlist) => (
                <Action
                  key={playlist.id}
                  icon={<ListMusicIcon size={20} color={colors.muted} />}
                  label={`${playlist.name} · ${playlist.tracks.length}`}
                  onPress={() =>
                    run(() => {
                      addTracksToPlaylist(playlist.id, [track])
                      notify(`Añadido a «${playlist.name}»`)
                    })
                  }
                />
              ))}
            </View>
          ) : (
            <Text className="px-2 py-3 text-sm" style={{ color: colors.faint }}>
              Aún no tienes listas. Crea una arriba.
            </Text>
          )}
        </View>
      )}
    </Sheet>
  )
}

/** Fila de pista reutilizable: pulsa para reproducir, ⋮ para más acciones. */
export function TrackRow({
  track,
  queue,
  index,
  showArtwork = true,
  showDuration = true,
  onRemove,
}: TrackRowProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const colors = useThemeColors()

  const playTrack = usePlayerStore((state) => state.playTrack)
  const currentTrack = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const toggle = usePlayerStore((state) => state.toggle)

  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  const isCurrent = currentTrack ? sameTrack(currentTrack, track) : false
  const isFavorite = favorites.some((item) => sameTrack(item, track))

  const handlePlay = () => {
    if (isCurrent) {
      toggle()
    } else {
      void playTrack(track, queue)
    }
  }

  return (
    <>
      <View
        className="flex-row items-center gap-3 rounded-xl px-2 py-1.5"
        style={{ backgroundColor: isCurrent ? colors.overlay2 : 'transparent' }}
      >
        <Pressable onPress={handlePlay} className="min-w-0 flex-1 flex-row items-center gap-3">
          {showArtwork ? (
            <View className="relative">
              <Artwork src={track.thumbnail} style={{ width: 48, height: 48 }} radius={8} />
              {isCurrent ? (
                <View
                  className="absolute inset-0 items-center justify-center"
                  style={{ borderRadius: 8, backgroundColor: 'rgba(0,0,0,0.55)' }}
                >
                  {isPlaying ? (
                    <PlayingBars color={colors.accent} size={16} />
                  ) : (
                    <PlayIcon size={18} color="#ffffff" />
                  )}
                </View>
              ) : null}
            </View>
          ) : (
            <Text className="w-6 text-center text-sm" style={{ color: colors.faint }}>
              {index !== undefined ? index + 1 : ''}
            </Text>
          )}

          <View className="min-w-0 flex-1">
            <Text
              numberOfLines={1}
              style={{
                color: isCurrent ? colors.accent : colors.ink,
                fontSize: 15,
                fontWeight: '600',
              }}
            >
              {track.title}
            </Text>
            <Text numberOfLines={1} className="mt-0.5 text-xs" style={{ color: colors.faint }}>
              {track.artist}
            </Text>
          </View>
        </Pressable>

        {showDuration && track.duration > 0 ? (
          <Text className="text-xs" style={{ color: colors.faint }}>
            {formatDuration(track.duration)}
          </Text>
        ) : null}

        <Pressable onPress={() => toggleFavorite(track)} className="p-1.5">
          {isFavorite ? (
            <HeartFilledIcon size={19} color={colors.accent} />
          ) : (
            <HeartIcon size={19} color={colors.faint} />
          )}
        </Pressable>

        <Pressable onPress={() => setMenuOpen(true)} className="p-1.5">
          <MoreIcon size={20} color={colors.faint} />
        </Pressable>
      </View>

      {menuOpen ? (
        <TrackMenu track={track} queue={queue} onRemove={onRemove} onClose={() => setMenuOpen(false)} />
      ) : null}
    </>
  )
}
