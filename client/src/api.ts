import type { SearchResponse, Settings } from './types'

const API = import.meta.env.VITE_API_URL || 'https://backend-theta-one-73.vercel.app'

export async function searchTracks(query: string, country = 'us'): Promise<SearchResponse> {
  const params = new URLSearchParams({ q: query, type: 'track', country })
  const res = await fetch(`${API}/v1/catalog/search?${params}`)
  if (!res.ok) throw new Error(`Catálogo: ${res.status}`)
  const data = await res.json()
  return { items: (data.items || []).filter((i: any) => i.type === 'track') }
}

export async function explorePopular(country = 'us'): Promise<SearchResponse> {
  const params = new URLSearchParams({ country })
  const res = await fetch(`${API}/v1/explore/popular?${params}`)
  if (!res.ok) throw new Error(`Descubre: ${res.status}`)
  return res.json()
}

export async function fetchPreview(track: string, artist: string): Promise<string | null> {
  const params = new URLSearchParams({ track, artist })
  try {
    const res = await fetch(`${API}/v1/preview?${params}`)
    if (!res.ok) return null
    const data = await res.json()
    return data.url ?? null
  } catch {
    return null
  }
}

export async function fetchLyrics(track: string, artist: string): Promise<string | null> {
  const params = new URLSearchParams({ track, artist })
  const res = await fetch(`${API}/v1/lyrics?${params}`)
  if (!res.ok) return null
  const data = await res.json()
  return data.lyrics ?? null
}

export const DEFAULT_SETTINGS: Settings = {
  country: 'us',
  volume: 0.8,
  crossfade: 0,
  autoplay: true,
}
