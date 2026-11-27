export {}

export interface NowPlaying {
  track: { title: string; artists: string; coverUrl: string } | null
  playing: boolean
  /** Posición en ms en el instante `at` (la píldora la extrapola). */
  position: number
  duration: number
  at: number
  liked: boolean
}

export type PrefetchPriority = 'next' | 'hover' | 'idle'
export type RemoteCommand = 'toggle' | 'next' | 'prev' | 'like'

declare global {
  interface Window {
    popApi?: {
      resolveAudio: (artist: string, title: string, fresh?: boolean) => Promise<string | null>
      prefetchAudio?: (artist: string, title: string, priority?: PrefetchPriority) => void
      debug: (msg: string) => void
      deezer?: (apiPath: string) => Promise<any>
      netease?: (apiPath: string) => Promise<any>
      cache?: (action: 'size' | 'clear') => Promise<number>
      windowAction?: (action: 'minimize' | 'maximize' | 'close') => void
      onWindowState?: (cb: (state: { maximized: boolean }) => void) => () => void
      getBounds?: () => Promise<{ x: number; y: number; width: number; height: number }>
      setBounds?: (b: { x: number; y: number; width: number; height: number }) => void
      sendNowPlaying?: (state: NowPlaying) => void
      overlayConfig?: (config: { enabled?: boolean; onMinimize?: boolean; anchor?: 'left' | 'center' | 'right' }) => void
      overlayCommand?: (cmd: RemoteCommand | 'open' | 'close') => void
      overlayReady?: () => void
      onOverlayState?: (cb: (state: NowPlaying) => void) => () => void
      onOverlayVisible?: (cb: (visible: boolean) => void) => () => void
      onRemoteCommand?: (cb: (cmd: RemoteCommand) => void) => () => void
    }
  }
}
