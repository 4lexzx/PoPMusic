import { useEffect } from 'react'
import { usePlayerStore } from '@/shared/store/playerStore'
import { audioService } from './audioService'

/**
 * Puente entre el servicio de audio y el store del reproductor.
 * Sustituye al `PlayerProvider` de la web (que montaba el iframe de YouTube).
 */
export function AudioSync() {
  useEffect(() => {
    const store = usePlayerStore.getState()
    audioService.setVolume(store.muted ? 0 : store.volume)
    if (store.muted) audioService.mute()
    else audioService.unmute()

    const offState = audioService.onState((state) => {
      usePlayerStore.getState().applyPlayerState(state)
    })

    const offProgress = audioService.onProgress((position, duration) => {
      const state = usePlayerStore.getState()
      if (Math.abs(position - state.position) > 0.2) state.setPosition(position)
      if (duration > 0 && Math.abs(duration - state.duration) > 0.5) state.setDuration(duration)
    })

    return () => {
      offState()
      offProgress()
    }
  }, [])

  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const track = usePlayerStore((state) => state.queue[state.currentIndex] ?? null)
  const trackKey = track ? `${track.source}:${track.id}` : ''

  // Mantiene el servicio alineado si el estado se restaura desde el almacenamiento.
  useEffect(() => {
    if (isPlaying) audioService.play()
  }, [isPlaying])

  // Controles de pantalla de bloqueo y notificación (título, artista, portada).
  useEffect(() => {
    audioService.setNowPlaying(track)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trackKey])

  useEffect(() => {
    return () => {
      audioService.destroy()
    }
  }, [])

  return null
}
