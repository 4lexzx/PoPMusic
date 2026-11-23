export {}

declare global {
  interface Window {
    popApi?: {
      resolveAudio: (artist: string, title: string) => Promise<string | null>
      debug: (msg: string) => void
    }
  }
}
