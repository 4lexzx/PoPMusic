import { AUDIO_API, HAS_AUDIO_BACKEND } from '@/shared/config'

/**
 * Backend de audio: extrae el audio de YouTube (yt-dlp) y lo sirve como stream
 * directo. Al reproducir con un `<audio>` real, la música continúa al salir de
 * la app y funciona la pantalla de bloqueo / notificación multimedia.
 */

/** URL del stream de audio directo para un video de YouTube. */
export function resolveAudioStream(videoId: string): string {
  return `${AUDIO_API}/stream?videoId=${encodeURIComponent(videoId)}`
}

/** Comprueba si el backend responde. Útil para avisar si está caído. */
export async function checkAudioBackend(signal?: AbortSignal): Promise<boolean> {
  if (!HAS_AUDIO_BACKEND) return false
  try {
    const response = await fetch(`${AUDIO_API}/health`, { signal })
    return response.ok
  } catch {
    return false
  }
}
