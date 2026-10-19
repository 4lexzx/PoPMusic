/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_YT_API_KEY?: string
  readonly VITE_SPOTIFY_CLIENT_ID?: string
  readonly VITE_AUDD_API_KEY?: string
  readonly VITE_AUDIO_API?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
