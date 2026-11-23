const API = import.meta.env.VITE_API_URL || 'https://backend-theta-one-73.vercel.app'

/**
 * Pide al backend propio la URL de audio de una canción.
 * La resolución se hace en Node (sin CORS), no en el navegador.
 */
export async function resolveAudio(artist: string, title: string): Promise<string | null> {
  const params = new URLSearchParams({ artist, title })
  const res = await fetch(`${API}/v1/audio?${params}`)
  if (!res.ok) return null
  const data = await res.json()
  return data.url ?? null
}
