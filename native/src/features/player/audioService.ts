import { createAudioPlayer, setAudioModeAsync, type AudioPlayer, type AudioStatus } from 'expo-audio'
import { resolveCatalogStream, type ResolvedStream } from '@/shared/api/catalog'
import { resolveYouTubeAudio, searchYouTube } from '@/shared/api/innertube'
import { API_BASE_URL } from '@/shared/config'
import type { Track } from '@/shared/types/track'

/**
 * Estados de reproducción (equivalentes a los de YouTube IFrame / TrackPlayer).
 * Se mantienen los mismos valores que usaba la app web para no cambiar la lógica.
 */
export enum YT_STATE {
  UNSTARTED = -1,
  ENDED = 0,
  PLAYING = 1,
  PAUSED = 2,
  BUFFERING = 3,
  CUED = 5,
}

/**
 * Resuelve la URL de audio reproducible para una pista. Es la pieza
 * intercambiable: añadir un proveedor nuevo no toca el resto de la app.
 */
export interface AudioResolver {
  resolve(track: Track): Promise<ResolvedStream | null>
}

export type StateListener = (state: YT_STATE) => void
export type ProgressListener = (position: number, duration: number) => void

export interface AudioService {
  /** Carga una pista y, si `autoplay`, empieza a sonar. */
  load(track: Track, autoplay: boolean): Promise<ResolvedStream | null>
  play(): void
  pause(): void
  seek(seconds: number): void
  setVolume(volume: number): void
  mute(): void
  unmute(): void
  /** Actualiza los controles de pantalla de bloqueo/notificación. */
  setNowPlaying(track: Track | null): void
  destroy(): void
  onState(listener: StateListener): () => void
  onProgress(listener: ProgressListener): () => void
}

/** Pistas que ya traen su propia URL de audio (catálogo legal). */
export const directResolver: AudioResolver = {
  async resolve(track) {
    if (!track.streamUrl) return null
    return {
      url: track.streamUrl,
      duration: track.preview ? Math.min(track.duration || 30, 30) : track.duration,
      preview: track.preview ?? false,
    }
  },
}

/**
 * YouTube (canciones completas). Si la pista trae `videoId` lo usa directo; si
 * no (por ejemplo una canción importada de Spotify) busca la mejor coincidencia
 * en YouTube. Ver `shared/api/innertube.ts`.
 */
export const youtubeResolver: AudioResolver = {
  async resolve(track) {
    let videoId = track.videoId ?? (track.source === 'youtube' ? track.id : '')
    if (!videoId) {
      const [best] = await searchYouTube(`${track.title} ${track.artist}`, 1)
      videoId = best?.videoId ?? ''
    }
    if (!videoId) return null
    const audio = await resolveYouTubeAudio(videoId)
    if (!audio) return null
    return { url: audio.url, duration: audio.duration || track.duration, preview: false }
  },
}

/** Pistas sin URL ni vídeo: se emparejan al catálogo legal sin claves. */
export const catalogResolver: AudioResolver = {
  resolve: (track) => resolveCatalogStream(track.title, track.artist),
}

/**
 * Proveedor opcional (desactivado salvo que definas EXPO_PUBLIC_API_BASE_URL).
 * Sirve para conectar un backend propio de streaming sin tocar la app.
 */
export const backendResolver: AudioResolver = {
  async resolve(track) {
    if (!API_BASE_URL || !track.videoId) return null
    return {
      url: `${API_BASE_URL}/api/stream?videoId=${encodeURIComponent(track.videoId)}`,
      duration: track.duration,
      preview: false,
    }
  },
}

const resolvers: AudioResolver[] = [
  directResolver,
  youtubeResolver,
  catalogResolver,
  backendResolver,
]

async function resolveAudio(track: Track): Promise<ResolvedStream | null> {
  for (const resolver of resolvers) {
    try {
      const resolved = await resolver.resolve(track)
      if (resolved?.url) return resolved
    } catch {
      // Probamos el siguiente resolutor.
    }
  }
  return null
}

/**
 * Reproductor real basado en `expo-audio`: soporta reproducción en segundo
 * plano y controles de pantalla de bloqueo/notificación (Android e iOS).
 */
class ExpoAudioService implements AudioService {
  private player: AudioPlayer | null = null
  private subscription: { remove: () => void } | null = null
  private track: Track | null = null
  private lastState: YT_STATE | null = null
  private finished = false
  private configured = false
  private stateListeners = new Set<StateListener>()
  private progressListeners = new Set<ProgressListener>()

  private configure(): void {
    if (this.configured) return
    this.configured = true
    void setAudioModeAsync({
      playsInSilentMode: true,
      shouldPlayInBackground: true,
      interruptionMode: 'doNotMix',
      allowsRecording: false,
      shouldRouteThroughEarpiece: false,
    }).catch(() => {
      // En web la sesión de audio la gestiona el navegador.
    })
  }

  private ensurePlayer(): AudioPlayer {
    if (this.player) return this.player
    this.player = createAudioPlayer(null, { updateInterval: 250, keepAudioSessionActive: true })
    this.subscription = this.player.addListener('playbackStatusUpdate', (status) => {
      this.handleStatus(status)
    })
    return this.player
  }

  private handleStatus(status: AudioStatus): void {
    const reported = status.duration
    const duration =
      Number.isFinite(reported) && reported > 0 ? reported : this.track?.duration ?? 0
    this.emitProgress(Number.isFinite(status.currentTime) ? status.currentTime : 0, duration)

    if (status.didJustFinish && !this.finished) {
      this.finished = true
      this.emitState(YT_STATE.ENDED)
      return
    }
    if (!status.didJustFinish) this.finished = false

    let next: YT_STATE
    if (status.playing) next = YT_STATE.PLAYING
    else if (!status.isLoaded) next = YT_STATE.BUFFERING
    else if (status.isBuffering) next = YT_STATE.BUFFERING
    else next = YT_STATE.PAUSED

    this.emitState(next)
  }

  async load(track: Track, autoplay: boolean): Promise<ResolvedStream | null> {
    this.track = track
    this.finished = false

    this.emitState(YT_STATE.BUFFERING)
    this.emitProgress(0, track.duration || 0)

    const resolved = await resolveAudio(track)
    if (!resolved) {
      this.emitState(YT_STATE.CUED)
      return null
    }

    const player = this.ensurePlayer()
    this.configure()

    // Si cambia de pista resolviendo la misma URL, `replace` reinicia la carga.
    player.replace({ uri: resolved.url })
    this.emitProgress(0, resolved.duration || track.duration || 0)

    if (autoplay) player.play()
    return resolved
  }

  play(): void {
    this.finished = false
    this.player?.play()
  }

  pause(): void {
    this.player?.pause()
  }

  seek(seconds: number): void {
    void this.player?.seekTo(Math.max(0, seconds))
  }

  setVolume(volume: number): void {
    if (this.player) this.player.volume = Math.max(0, Math.min(1, volume / 100))
  }

  mute(): void {
    if (this.player) this.player.muted = true
  }

  unmute(): void {
    if (this.player) this.player.muted = false
  }

  setNowPlaying(track: Track | null): void {
    const player = this.player
    if (!player || typeof player.setActiveForLockScreen !== 'function') return
    try {
      if (!track) {
        player.clearLockScreenControls()
        return
      }
      player.setActiveForLockScreen(
        true,
        {
          title: track.title,
          artist: track.artist,
          albumTitle: 'PoP Music',
          artworkUrl: track.thumbnail || undefined,
        },
        {
          showSeekForward: true,
          showSeekBackward: true,
          showNextTrack: true,
          showPreviousTrack: true,
        },
      )
    } catch {
      // Algunos entornos (web) no soportan controles de bloqueo.
    }
  }

  destroy(): void {
    this.subscription?.remove()
    this.subscription = null
    this.player?.remove()
    this.player = null
    this.track = null
    this.stateListeners.clear()
    this.progressListeners.clear()
  }

  onState(listener: StateListener): () => void {
    this.stateListeners.add(listener)
    return () => this.stateListeners.delete(listener)
  }

  onProgress(listener: ProgressListener): () => void {
    this.progressListeners.add(listener)
    return () => this.progressListeners.delete(listener)
  }

  private emitState(state: YT_STATE): void {
    if (this.lastState === state) return
    this.lastState = state
    this.stateListeners.forEach((listener) => listener(state))
  }

  private emitProgress(position: number, duration: number): void {
    this.progressListeners.forEach((listener) => listener(position, duration))
  }
}

/** Instancia única del servicio de audio. */
export const audioService: AudioService = new ExpoAudioService()
