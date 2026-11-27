import { useEffect, useState } from 'react'
import type { SearchAll } from './types'
import { searchAll } from './api'

export const RECENT_KEY = 'popmusic-client-searches-v1'
export const EMPTY_SEARCH: SearchAll = { tracks: [], albums: [], artists: [] }

export function loadSearchHistory(): string[] {
  try { return JSON.parse(localStorage.getItem(RECENT_KEY) || '[]') } catch { return [] }
}

function remember(term: string) {
  const next = [term, ...loadSearchHistory().filter((h) => h.toLowerCase() !== term.toLowerCase())].slice(0, 8)
  localStorage.setItem(RECENT_KEY, JSON.stringify(next))
}

/** Búsqueda en el catálogo con espera al teclear; solo música. */
export function useCatalogSearch(query: string, country: string) {
  const [data, setData] = useState<SearchAll>(EMPTY_SEARCH)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const term = query.trim()
    if (term.length < 2) { setData(EMPTY_SEARCH); setError(''); setLoading(false); return }
    let cancelled = false
    setLoading(true)
    const timer = setTimeout(async () => {
      setError('')
      try {
        const res = await searchAll(term, country)
        if (cancelled) return
        // iTunes devuelve artistas sueltos poco relacionados: solo los que coinciden con lo buscado.
        const t = term.toLowerCase()
        const artists = res.artists.filter((a) => {
          const n = a.name.toLowerCase()
          return n.includes(t) || t.includes(n) || res.tracks.some((x) => x.artists.toLowerCase() === n)
        })
        setData({ ...res, artists })
        remember(term)
      } catch {
        if (!cancelled) setError('No se pudo conectar con el catálogo. Inténtalo de nuevo.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }, 200)
    return () => { cancelled = true; clearTimeout(timer) }
  }, [query, country])

  const artistCover = (name: string) =>
    data.tracks.find((t) => t.artists.toLowerCase() === name.toLowerCase())?.coverUrl
    || data.albums.find((a) => a.artist.toLowerCase() === name.toLowerCase())?.coverUrl

  return { data, loading, error, artistCover }
}
