/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_YT_API_KEY?: string
  readonly VITE_SPOTIFY_CLIENT_ID?: string
  readonly VITE_AUDD_API_KEY?: string
  readonly VITE_AUDIO_API?: string
  readonly VITE_SUPABASE_URL?: string
  readonly VITE_SUPABASE_ANON_KEY?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
