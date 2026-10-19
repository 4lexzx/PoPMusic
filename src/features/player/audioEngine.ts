import { playerController, YT_STATE, type YouTubePlayerLike } from './youtubeController'

/**
 * Motor de audio unificado. Usa dos motores según la pista:
 * - `youtube`: reproductor IFrame (no continúa en segundo plano en móvil).
 * - `html`: elemento `<audio>` con URL directa (Audius) → SÍ continúa en
 *   segundo plano y en la pantalla de bloqueo.
 *
 * Expone la misma forma que `playerController` para que el store no cambie.
 */

type EngineKind = 'youtube' | 'html'

let audioEl: HTMLAudioElement | null = null
let kind: EngineKind = 'youtube'
let stateListener: ((state: number) => void) | null = null
let errorListener: (() => void) | null = null

function ensureAudio(): HTMLAudioElement {
  if (audioEl) return audioEl

  const el = new Audio()
  el.preload = 'auto'
  el.addEventListener('play', () => stateListener?.(YT_STATE.PLAYING))
  el.addEventListener('playing', () => stateListener?.(YT_STATE.PLAYING))
  el.addEventListener('pause', () => stateListener?.(YT_STATE.PAUSED))
  el.addEventListener('waiting', () => stateListener?.(YT_STATE.BUFFERING))
  el.addEventListener('loadstart', () => stateListener?.(YT_STATE.BUFFERING))
  el.addEventListener('ended', () => stateListener?.(YT_STATE.ENDED))
  el.addEventListener('error', () => errorListener?.())
  audioEl = el
  return el
}

export const audioEngine = {
  /** Escucha los cambios de estado del motor HTML (emula onStateChange de YouTube). */
  onState(listener: ((state: number) => void) | null): void {
    stateListener = listener
  },

  /** Escucha errores del motor HTML (p. ej. el backend no responde). */
  onError(listener: (() => void) | null): void {
    errorListener = listener
  },

  attachYouTube(player: YouTubePlayerLike): void {
    kind = 'youtube'
    playerController.attach(player)
  },

  detach(): void {
    playerController.detach()
  },

  isReady(): boolean {
    if (kind === 'html') return audioEl !== null
    return playerController.isReady()
  },

  /** Carga un vídeo de YouTube (iframe) y detiene el audio HTML. */
  loadVideo(videoId: string, autoplay = true): void {
    audioEl?.pause()
    kind = 'youtube'
    playerController.load(videoId, autoplay)
  },

  /** Carga una URL de audio directa y detiene el iframe de YouTube. */
  loadUrl(url: string, autoplay = true): void {
    const el = ensureAudio()
    playerController.pause()
    kind = 'html'
    el.src = url
    el.load()
    if (autoplay) void el.play().catch(() => undefined)
  },

  play(): void {
    if (kind === 'html') {
      void audioEl?.play().catch(() => undefined)
      return
    }
    playerController.play()
  },

  pause(): void {
    if (kind === 'html') {
      audioEl?.pause()
      return
    }
    playerController.pause()
  },

  seek(seconds: number): void {
    if (kind === 'html') {
      if (audioEl) audioEl.currentTime = Math.max(0, seconds)
      return
    }
    playerController.seek(seconds)
  },

  setVolume(volume: number): void {
    playerController.setVolume(volume)
    if (audioEl) audioEl.volume = Math.max(0, Math.min(1, volume / 100))
  },

  mute(): void {
    playerController.mute()
    if (audioEl) audioEl.muted = true
  },

  unmute(): void {
    playerController.unmute()
    if (audioEl) audioEl.muted = false
  },

  currentTime(): number {
    if (kind === 'html') return audioEl?.currentTime ?? 0
    return playerController.currentTime()
  },

  duration(): number {
    if (kind === 'html') {
      const value = audioEl?.duration ?? 0
      return Number.isFinite(value) ? value : 0
    }
    return playerController.duration()
  },

  state(): number {
    if (kind === 'html') {
      if (!audioEl) return YT_STATE.UNSTARTED
      if (audioEl.ended) return YT_STATE.ENDED
      if (audioEl.paused) return YT_STATE.PAUSED
      return YT_STATE.PLAYING
    }
    return playerController.state()
  },

  /** ¿La pista actual suena vía `<audio>` (por tanto en segundo plano)? */
  isBackgroundCapable(): boolean {
    return kind === 'html'
  },
}
