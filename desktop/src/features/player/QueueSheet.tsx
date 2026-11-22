import { Artwork } from '@/components/Artwork'
import { CloseIcon, ListMusicIcon, PauseIcon, PlayIcon } from '@/components/Icons'
import { Button, Sheet } from '@/components/ui'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'

/** Cola de reproducción en hoja deslizable (se abre por gesto o botón). */
export function QueueSheet() {
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
        <div className="flex flex-col items-center gap-2 py-12 text-muted">
          <ListMusicIcon size={30} />
          <p className="text-sm">La cola está vacía.</p>
        </div>
      ) : (
        <ul className="space-y-1 pb-4">
          {queue.map((track, index) => {
            const isCurrent = index === currentIndex
            return (
              <li key={`${track.source}-${track.id}-${index}`}>
                <div
                  className={`flex items-center gap-3 rounded-2xl px-2 py-2 ${
                    isCurrent ? 'bg-[color:var(--overlay-2)]' : ''
                  }`}
                >
                  <button
                    type="button"
                    className="flex min-w-0 flex-1 items-center gap-3 text-left"
                    onClick={() => {
                      if (isCurrent) {
                        toggle()
                      } else {
                        void jumpTo(index)
                      }
                    }}
                  >
                    <Artwork src={track.thumbnail} className="h-11 w-11 shrink-0" rounded="rounded-xl" />
                    <span className="min-w-0 flex-1">
                      <span
                        className={`block truncate text-sm font-medium ${
                          isCurrent ? 'text-accent-soft' : ''
                        }`}
                      >
                        {track.title}
                      </span>
                      <span className="block truncate text-xs text-faint">{track.artist}</span>
                    </span>
                    {isCurrent ? (
                      <span className="text-accent-soft">
                        {isPlaying ? <PauseIcon size={18} /> : <PlayIcon size={18} />}
                      </span>
                    ) : null}
                  </button>

                  <button
                    type="button"
                    aria-label="Quitar de la cola"
                    onClick={() => removeFromQueue(index)}
                    className="shrink-0 p-2 text-faint"
                  >
                    <CloseIcon size={18} />
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      {queue.length > 0 ? (
        <div className="pb-2">
          <Button variant="danger" fullWidth onClick={clearQueue}>
            Vaciar cola
          </Button>
        </div>
      ) : null}
    </Sheet>
  )
}
