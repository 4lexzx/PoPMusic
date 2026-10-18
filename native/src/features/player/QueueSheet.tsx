import { Pressable, Text, View } from 'react-native'
import { Artwork } from '@/components/Artwork'
import { CloseIcon, ListMusicIcon, PauseIcon, PlayIcon } from '@/components/Icons'
import { Button, Sheet } from '@/components/ui'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'

/** Cola de reproducción en hoja deslizable. */
export function QueueSheet() {
  const colors = useThemeColors()
  const open = useUiStore((state) => state.queueOpen)
  const setOpen = useUiStore((state) => state.setQueueOpen)

  const queue = usePlayerStore((state) => state.queue)
  const currentIndex = usePlayerStore((state) => state.currentIndex)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const jumpTo = usePlayerStore((state) => state.jumpTo)
  const removeFromQueue = usePlayerStore((state) => state.removeFromQueue)
  const clearQueue = usePlayerStore((state) => state.clearQueue)
  const toggle = usePlayerStore((state) => state.toggle)

  return (
    <Sheet open={open} onClose={() => setOpen(false)} title="Cola de reproducción">
      {queue.length === 0 ? (
        <View className="items-center gap-2 py-12">
          <ListMusicIcon size={30} color={colors.muted} />
          <Text className="text-sm" style={{ color: colors.muted }}>
            La cola está vacía.
          </Text>
        </View>
      ) : (
        <View className="gap-1 pb-4">
          {queue.map((track, index) => {
            const isCurrent = index === currentIndex
            return (
              <View
                key={`${track.source}-${track.id}-${index}`}
                className="flex-row items-center gap-3 rounded-2xl px-2 py-2"
                style={{ backgroundColor: isCurrent ? colors.overlay2 : 'transparent' }}
              >
                <Pressable
                  onPress={() => (isCurrent ? toggle() : void jumpTo(index))}
                  className="min-w-0 flex-1 flex-row items-center gap-3"
                >
                  <Artwork src={track.thumbnail} style={{ width: 44, height: 44 }} radius={12} />
                  <View className="min-w-0 flex-1">
                    <Text
                      numberOfLines={1}
                      className="text-sm font-medium"
                      style={{ color: isCurrent ? colors.accent : colors.ink }}
                    >
                      {track.title}
                    </Text>
                    <Text numberOfLines={1} className="text-xs" style={{ color: colors.faint }}>
                      {track.artist}
                    </Text>
                  </View>
                  {isCurrent ? (
                    isPlaying ? (
                      <PauseIcon size={18} color={colors.accent} />
                    ) : (
                      <PlayIcon size={18} color={colors.accent} />
                    )
                  ) : null}
                </Pressable>

                <Pressable
                  onPress={() => removeFromQueue(index)}
                  accessibilityLabel="Quitar de la cola"
                  className="p-2"
                >
                  <CloseIcon size={18} color={colors.faint} />
                </Pressable>
              </View>
            )
          })}
        </View>
      )}

      {queue.length > 0 ? (
        <View className="pb-2">
          <Button variant="danger" fullWidth onPress={clearQueue}>
            Vaciar cola
          </Button>
        </View>
      ) : null}
    </Sheet>
  )
}
