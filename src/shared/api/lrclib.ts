import type { Lyrics } from '@/shared/types/lyrics'
import { parseLrc } from '@/shared/utils/lrc'
import { getJson, HttpError } from './http'

const BASE = 'https://lrclib.net/api'
const CLIENT = 'LinerMusic/1.0.0 (https://github.com/tryliner/desktop)'

interface LrclibRecord {
  trackName?: string
  artistName?: string
  instrumental?: boolean
  plainLyrics?: string | null
  syncedLyrics?: string | null
}

/**
 * LRCLIB permite CORS. Algunos navegadores rechazan el preflight cuando se
 * envían cabeceras personalizadas, así que primero intentamos con la cabecera
 * de identificación y, si falla, reintentamos sin ella.
 */
async function lrclibFetch(path: string): Promise<unknown | null> {
  const url = `${BASE}${path}`
  try {
    return await getJson<unknown>(url, { headers: { 'Lrclib-Client': CLIENT } })
  } catch (error) {
    if (error instanceof HttpError && error.status === 404) return null
    try {
      return await getJson<unknown>(url)
    } catch (retryError) {
      if (retryError instanceof HttpError && retryError.status === 404) return null
      return null
    }
  }
}

function toLyrics(record: LrclibRecord): Lyrics | null {
  const syncedLyrics = record.syncedLyrics?.trim() ?? ''
  const plainLyrics = record.plainLyrics?.trim() ?? ''

  if (syncedLyrics) {
    const lines = parseLrc(syncedLyrics)
    if (lines.length > 0) {
      return {
        synced: true,
        lines,
        plain: plainLyrics || null,
        trackName: record.trackName ?? '',
        artistName: record.artistName ?? '',
      }
    }
  }

  if (plainLyrics) {
    return {
      synced: false,
      lines: plainLyrics
        .split(/\r?\n/)
        .map((text, index) => ({ time: index, end: index + 1, text, words: [] })),
      plain: plainLyrics,
      trackName: record.trackName ?? '',
      artistName: record.artistName ?? '',
    }
  }

  return null
}

/** Busca letras por firma de pista (título, artista y duración). */
export async function fetchLyrics(input: {
  title: string
  artist: string
  duration?: number
}): Promise<Lyrics | null> {
  const title = input.title.trim()
  const artist = input.artist.trim()
  if (!title) return null

  const params = new URLSearchParams({ track_name: title })
  if (artist && artist !== 'Artista desconocido') params.set('artist_name', artist)
  if (input.duration && input.duration > 0) {
    params.set('duration', String(Math.round(input.duration)))
  }

  const exact = (await lrclibFetch(`/get?${params.toString()}`)) as LrclibRecord | null
  if (exact) {
    const lyrics = toLyrics(exact)
    if (lyrics) return lyrics
  }

  const searchParams = new URLSearchParams({ track_name: title })
  if (artist && artist !== 'Artista desconocido') searchParams.set('artist_name', artist)

  const results = (await lrclibFetch(`/search?${searchParams.toString()}`)) as
    | LrclibRecord[]
    | null

  if (!Array.isArray(results) || results.length === 0) return null

  const withSync = results.find((record) => record.syncedLyrics?.trim())

  const normalize = (value: string): string =>
    value
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, '')
      .replace(/\s+/g, ' ')
      .trim()

  const targetTitle = normalize(title)
  const best =
    withSync ??
    results.find((record) => normalize(record.trackName ?? '').includes(targetTitle)) ??
    results[0]

  return toLyrics(best)
}
