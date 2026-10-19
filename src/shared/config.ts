/**
 * Configuración central leída desde variables de entorno.
 * Las claves con prefijo VITE_ se incrustan en el bundle del navegador.
 */

export const YT_API_KEY: string = import.meta.env.VITE_YT_API_KEY?.trim() ?? ''
export const SPOTIFY_CLIENT_ID: string = import.meta.env.VITE_SPOTIFY_CLIENT_ID?.trim() ?? ''
export const AUDD_API_KEY: string = import.meta.env.VITE_AUDD_API_KEY?.trim() ?? ''

/** ¿Hay clave de YouTube Data API configurada? */
export const HAS_YT_KEY: boolean = YT_API_KEY.length > 0

/** ¿Hay servicio de reconocimiento de música (AudD) configurado? */
export const HAS_RECOGNITION: boolean = AUDD_API_KEY.length > 0

/** Modo demo: la app funciona con datos de muestra si no hay clave de YouTube. */
export const IS_DEMO: boolean = !HAS_YT_KEY

/** Ruta relativa a la Serverless Function de Spotify. */
export const SPOTIFY_API_PATH = '/api/spotify'

/**
 * URL base del backend de audio (extrae el audio de YouTube con yt-dlp).
 * Si está configurado, las canciones de YouTube se reproducen como audio
 * directo → suenan en segundo plano y en la pantalla de bloqueo.
 * Ejemplo: https://audio.tudominio.com  (sin barra final)
 */
export const AUDIO_API: string = (import.meta.env.VITE_AUDIO_API?.trim() ?? '').replace(/\/$/, '')

/** ¿Hay backend de audio configurado? */
export const HAS_AUDIO_BACKEND: boolean = AUDIO_API.length > 0
