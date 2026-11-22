export interface DesktopTrackInfo {
  title: string
  artist: string
  thumbnail: string
  playing: boolean
}

export interface DesktopApi {
  isElectron: boolean
  minimize: () => void
  maximize: () => void
  close: () => void
  toggleOverlay: () => void
  hideOverlay: () => void
  sendTrack: (track: DesktopTrackInfo) => void
  sendCommand: (command: 'play-pause' | 'next' | 'prev') => void
  requestDeepLink: () => void
  youtubeFetch: (options: {
    url: string
    method?: string
    headers?: Record<string, string>
    body?: string
  }) => Promise<{ status: number; text: string; cookie?: string }>
  onMaximized: (callback: (value: boolean) => void) => () => void
  onCommand: (callback: (command: string) => void) => () => void
  onTrack: (callback: (track: DesktopTrackInfo) => void) => () => void
  onDeepLink: (callback: (url: string) => void) => () => void
}

declare global {
  interface Window {
    popDesktop?: DesktopApi
  }
}

export {}
