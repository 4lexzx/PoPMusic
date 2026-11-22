/**
 * Controlador del reproductor de YouTube IFrame.
 * Mantiene una única instancia del player y encola comandos hasta que está listo.
 */

export interface YouTubePlayerLike {
  loadVideoById: (options: string | { videoId: string; startSeconds?: number }) => void
  playVideo: () => void
  pauseVideo: () => void
  seekTo: (seconds: number, allowSeekAhead: boolean) => void
  setVolume: (volume: number) => void
  mute: () => void
  unMute: () => void
  getCurrentTime: () => number
  getDuration: () => number
  getPlayerState: () => number
  destroy: () => void
}

/** Estados devueltos por la IFrame Player API. */
export const YT_STATE = {
  UNSTARTED: -1,
  ENDED: 0,
  PLAYING: 1,
  PAUSED: 2,
  BUFFERING: 3,
  CUED: 5,
} as const

let player: YouTubePlayerLike | null = null
let pendingLoad: { videoId: string; autoplay: boolean } | null = null

export const playerController = {
  attach(instance: YouTubePlayerLike): void {
    player = instance
    if (pendingLoad) {
      const { videoId, autoplay } = pendingLoad
      pendingLoad = null
      player.loadVideoById({ videoId })
      if (!autoplay) player.pauseVideo()
    }
  },

  detach(): void {
    player = null
  },

  isReady(): boolean {
    return player !== null
  },

  load(videoId: string, autoplay = true): void {
    if (!player) {
      pendingLoad = { videoId, autoplay }
      return
    }
    player.loadVideoById({ videoId })
    if (!autoplay) player.pauseVideo()
  },

  play(): void {
    player?.playVideo()
  },

  pause(): void {
    player?.pauseVideo()
  },

  seek(seconds: number): void {
    player?.seekTo(seconds, true)
  },

  setVolume(volume: number): void {
    player?.setVolume(Math.max(0, Math.min(100, volume)))
  },

  mute(): void {
    player?.mute()
  },

  unmute(): void {
    player?.unMute()
  },

  currentTime(): number {
    return player ? player.getCurrentTime() : 0
  },

  duration(): number {
    return player ? player.getDuration() : 0
  },

  state(): number {
    return player ? player.getPlayerState() : YT_STATE.UNSTARTED
  },
}
