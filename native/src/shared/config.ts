/**
 * Configuración central leída desde variables de entorno de Expo.
 * Las variables con prefijo EXPO_PUBLIC_ se incrustan en el bundle.
 */

export const AUDD_API_KEY: string = (process.env.EXPO_PUBLIC_AUDD_API_KEY ?? '').trim()

/** Base del backend (Vercel) donde viven las Serverless Functions. */
export const API_BASE_URL: string = (process.env.EXPO_PUBLIC_API_BASE_URL ?? '')
  .trim()
  .replace(/\/$/, '')

/** ¿Hay servicio de reconocimiento de música (AudD) configurado? */
export const HAS_RECOGNITION: boolean = AUDD_API_KEY.length > 0

/**
 * Todo el catálogo (búsqueda, reproducción e importación) funciona sin claves:
 * YouTube se lee por InnerTube y el respaldo legal (Audius/Deezer/iTunes) tampoco
 * necesita clave. Por eso ya no existe modo demo.
 */
export const IS_DEMO = false as boolean

/** Ruta absoluta a la Serverless Function de Spotify. */
export const SPOTIFY_API_PATH = `${API_BASE_URL}/api/spotify`
