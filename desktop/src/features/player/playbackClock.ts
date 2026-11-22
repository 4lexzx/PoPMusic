import { audioEngine } from '@/features/player/audioEngine'
import { usePlayerStore } from '@/shared/store/playerStore'

let anchorTime = 0
let anchorStamp = 0
let lastEngineRead = 0
let frozenTime: number | null = null
let pausedPosition = Number.NaN

/**
 * Reloj de reproducción con resolución de frame (60 fps).
 *
 * El store solo actualiza `position` cada 300 ms, lo que hacía que el karaoke
 * avanzara a saltos. Aquí se interpola con `performance.now()` y se corrige
 * contra el tiempo real del motor de audio varias veces por segundo, de modo
 * que la letra sigue el ritmo aunque el store vaya por detrás.
 */
export function getPlaybackTime(now = performance.now()): number {
  const { isPlaying, position } = usePlayerStore.getState()

  if (!isPlaying) {
    if (anchorStamp === 0) {
      anchorTime = position
      anchorStamp = now
      frozenTime = position
      pausedPosition = position
      return position
    }
    // Al pausar se congela el último tiempo interpolado (el store puede ir
    // hasta 300 ms por detrás). Solo un cambio real de posición (seek) lo mueve.
    if (frozenTime === null) {
      frozenTime = anchorTime + (now - anchorStamp) / 1000
    }
    if (!Number.isNaN(pausedPosition) && Math.abs(position - pausedPosition) > 0.05) {
      frozenTime = position
    }
    pausedPosition = position
    return frozenTime
  }

  if (frozenTime !== null) {
    anchorTime = frozenTime
    anchorStamp = now
    lastEngineRead = now
    frozenTime = null
    pausedPosition = Number.NaN
  } else if (anchorStamp === 0) {
    anchorTime = position
    anchorStamp = now
    lastEngineRead = now
  }

  let time = anchorTime + (now - anchorStamp) / 1000

  // Corrección contra el motor real: también detecta seeks, sin depender del
  // store (que se actualiza con retraso y puede provocar saltos atrás).
  if (now - lastEngineRead >= 200) {
    lastEngineRead = now
    const engineTime = audioEngine.currentTime()
    if (Number.isFinite(engineTime) && Math.abs(engineTime - time) > 0.25) {
      anchorTime = engineTime
      anchorStamp = now
      time = engineTime
    }
  }

  return time
}
