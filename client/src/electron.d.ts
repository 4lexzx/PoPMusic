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

export interface ImportedList {
  source: 'spotify' | 'youtube' | 'soundcloud' | 'apple' | 'deezer'
  title: string
  cover: string
  /** Solo Deezer: tipo e id para leerlo con la API del catálogo. */
  kind?: 'playlist' | 'album' | 'track'
  id?: string
  items: { title: string; artist: string; durationMs?: number; cover?: string }[]
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
      appVersion?: () => Promise<string>
      updateCheck?: () => Promise<{ available: boolean; version?: string; notes?: string; page?: string; asset?: { name: string; url: string; size: number } }>
      updateDownload?: (url: string, size: number) => Promise<string>
      updateInstall?: (file: string) => void
      onUpdateProgress?: (cb: (p: { done: number; total: number }) => void) => () => void
      importLink?: (link: string) => Promise<{ ok: true; data: ImportedList } | { ok: false; error: string }>
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
