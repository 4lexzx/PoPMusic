import { useEffect, type ReactNode } from 'react'
import { selectCurrentTrack, usePlayerStore } from '@/shared/store/playerStore'
import type { Track } from '@/shared/types/track'
import { playerController, type YouTubePlayerLike } from './youtubeController'

const SCRIPT_ID = 'youtube-iframe-api'
const HOST_ID = 'yt-player-host'

interface YouTubeNamespace {
  Player: new (
    element: string,
    options: Record<string, unknown>,
  ) => YouTubePlayerLike & { destroy: () => void }
}

interface YouTubeWindow extends Window {
  YT?: YouTubeNamespace
  onYouTubeIframeAPIReady?: () => void
}

let apiPromise: Promise<void> | null = null

function loadYouTubeApi(): Promise<void> {
  if (apiPromise) return apiPromise

  apiPromise = new Promise<void>((resolve) => {
    const globalWindow = window as YouTubeWindow
    if (globalWindow.YT?.Player) {
      resolve()
      return
    }

    const previous = globalWindow.onYouTubeIframeAPIReady
    globalWindow.onYouTubeIframeAPIReady = () => {
      previous?.()
      resolve()
    }

    if (!document.getElementById(SCRIPT_ID)) {
      const script = document.createElement('script')
      script.id = SCRIPT_ID
      script.src = 'https://www.youtube.com/iframe_api'
      script.async = true
      document.head.appendChild(script)
    }
  })

  return apiPromise
}

/** Portadas de la pantalla de bloqueo con tamaños reales (YouTube permite varios). */
function buildArtwork(track: Track): MediaImage[] {
  if (track.videoId) {
    const base = `https://i.ytimg.com/vi/${track.videoId}`
    return [
      { src: `${base}/mqdefault.jpg`, sizes: '320x180', type: 'image/jpeg' },
      { src: `${base}/hqdefault.jpg`, sizes: '480x360', type: 'image/jpeg' },
      { src: `${base}/sddefault.jpg`, sizes: '640x480', type: 'image/jpeg' },
      { src: `${base}/maxresdefault.jpg`, sizes: '1280x720', type: 'image/jpeg' },
    ]
  }
  if (track.thumbnail) {
    return [{ src: track.thumbnail, sizes: '512x512', type: 'image/jpeg' }]
  }
  return []
}

/**
 * Crea el reproductor de YouTube oculto y sincroniza su estado con el store.
 * Toda la UI mostrada al usuario es personalizada.
 */
export function PlayerProvider({ children }: { children: ReactNode }) {
  useEffect(() => {
    let disposed = false

    void loadYouTubeApi().then(() => {
      if (disposed) return
      const globalWindow = window as YouTubeWindow
      if (!globalWindow.YT?.Player) return

      const player = new globalWindow.YT.Player(HOST_ID, {
        height: '1',
        width: '1',
        playerVars: {
          playsinline: 1,
          controls: 0,
          disablekb: 1,
          modestbranding: 1,
          rel: 0,
          fs: 0,
          iv_load_policy: 3,
          origin: window.location.origin,
        },
        events: {
          onReady: () => {
            const store = usePlayerStore.getState()
            playerController.attach(player)
            playerController.setVolume(store.muted ? 0 : store.volume)
            if (store.muted) playerController.mute()
          },
          onStateChange: (event: { data: number }) => {
            usePlayerStore.getState().applyPlayerState(event.data)
          },
        },
      })
    })

    return () => {
      disposed = true
      playerController.detach()
    }
  }, [])

  // Actualiza posición y duración mientras se reproduce.
  useEffect(() => {
    const interval = window.setInterval(() => {
      if (!playerController.isReady()) return
      const store = usePlayerStore.getState()

      const duration = playerController.duration()
      if (Number.isFinite(duration) && duration > 0 && Math.abs(duration - store.duration) > 0.5) {
        store.setDuration(duration)
      }

      const position = playerController.currentTime()
      if (Number.isFinite(position) && Math.abs(position - store.position) > 0.2) {
        store.setPosition(position)
      }
    }, 300)

    return () => window.clearInterval(interval)
  }, [])

  const currentTrack = usePlayerStore(selectCurrentTrack)
  const isPlaying = usePlayerStore((state) => state.isPlaying)

  // Media Session (1/4): portada, título y artista en pantalla de bloqueo / Centro de control.
  useEffect(() => {
    const mediaSession = navigator.mediaSession
    if (!mediaSession || !currentTrack) return

    try {
      if ('MediaMetadata' in window) {
        mediaSession.metadata = new MediaMetadata({
          title: currentTrack.title,
          artist: currentTrack.artist,
          album: 'PoP Music',
          artwork: buildArtwork(currentTrack),
        })
      }
    } catch {
      // MediaMetadata no disponible: se ignora.
    }
  }, [currentTrack])

  // Media Session (2/4): estado reproducir/pausar que muestra el sistema.
  useEffect(() => {
    const mediaSession = navigator.mediaSession
    if (!mediaSession) return
    try {
      mediaSession.playbackState = isPlaying ? 'playing' : 'paused'
    } catch {
      // Se ignora.
    }
  }, [isPlaying])

  // Media Session (3/4): botones de play/pausa, anterior, siguiente y avance/retroceso
  // (notificación, pantalla de bloqueo, Centro de control y auriculares Bluetooth).
  useEffect(() => {
    const mediaSession = navigator.mediaSession
    if (!mediaSession) return

    const state = () => usePlayerStore.getState()
    const handlers: Array<[MediaSessionAction, MediaSessionActionHandler]> = [
      ['play', () => { if (!state().isPlaying) state().toggle() }],
      ['pause', () => { if (state().isPlaying) state().toggle() }],
      ['nexttrack', () => state().next()],
      ['previoustrack', () => state().prev()],
      [
        'seekbackward',
        (details) => state().seek(Math.max(0, state().position - (details.seekOffset ?? 10))),
      ],
      [
        'seekforward',
        (details) => state().seek(state().position + (details.seekOffset ?? 10)),
      ],
      [
        'seekto',
        (details) => {
          if (details.seekTime != null) state().seek(details.seekTime)
        },
      ],
    ]

    for (const [action, handler] of handlers) {
      try {
        mediaSession.setActionHandler(action, handler)
      } catch {
        // Acción no soportada por el navegador.
      }
    }

    return () => {
      for (const [action] of handlers) {
        try {
          mediaSession.setActionHandler(action, null)
        } catch {
          // Se ignora.
        }
      }
    }
  }, [])

  // Media Session (4/4): barra de progreso precisa en pantalla de bloqueo, en tiempo real.
  useEffect(() => {
    const mediaSession = navigator.mediaSession
    if (!mediaSession || typeof mediaSession.setPositionState !== 'function') return

    const sync = () => {
      const { position, duration } = usePlayerStore.getState()
      if (!Number.isFinite(duration) || duration <= 0) return
      const clamped = Math.max(0, Math.min(position, duration))
      try {
        mediaSession.setPositionState({ duration, playbackRate: 1, position: clamped })
      } catch {
        // Posición/duración no válidas para este navegador.
      }
    }

    sync()
    const interval = window.setInterval(sync, 1000)
    return () => window.clearInterval(interval)
  }, [currentTrack, isPlaying])

  return (
    <>
      {children}
      {/* Contenedor oculto del iframe de YouTube (no usar display:none). */}
      <div
        aria-hidden="true"
        className="pointer-events-none fixed bottom-0 right-0 h-px w-px overflow-hidden opacity-[0.01]"
      >
        <div id={HOST_ID} />
      </div>
    </>
  )
}
