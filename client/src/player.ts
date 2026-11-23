const API = import.meta.env.VITE_API_URL || 'https://backend-theta-one-73.vercel.app'

/**
 * Resuelve el audio completo de una canción.
 * 1) En Electron, yt-dlp local (sin CORS y sin servicios de terceros).
 * 2) En el navegador, el endpoint del backend propio.
 */
export async function resolveAudio(artist: string, title: string): Promise<string | null> {
  if (window.popApi?.resolveAudio) {
    try {
      const url = await window.popApi.resolveAudio(artist, title)
      if (url) return url
    } catch {
      // caemos al backend
    }
  }
  try {
    const params = new URLSearchParams({ artist, title })
    const res = await fetch(`${API}/v1/audio?${params}`)
    if (!res.ok) return null
    const data = await res.json()
    return data.url ?? null
  } catch {
    return null
  }
}
