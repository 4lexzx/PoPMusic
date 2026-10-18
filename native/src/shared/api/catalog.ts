import type { Track } from '@/shared/types/track'
import { getJson, qs } from './http'

/**
 * Catálogo musical 100% legal y sin claves de API.
 *
 * - Audius: canciones COMPLETAS de música independiente (stream público).
 * - Deezer: catálogo mainstream con preview oficial de 30 s (MP3).
 * - iTunes: respaldo mainstream con preview oficial de 30 s (AAC).
 *
 * No se reproduce nada de YouTube ni de Spotify: sus TOS lo prohíben fuera
 * de sus reproductores oficiales. Esas plataformas solo se usan para leer
 * metadatos de playlists y emparejar cada pista con el catálogo legal.
 */

const AUDIUS_BASE = 'https://api.audius.co/v1'
const DEEZER_BASE = 'https://api.deezer.com'
const ITUNES_BASE = 'https://itunes.apple.com'
const APP_NAME = 'PoPMusic'

/** Duración del preview legal de Deezer/iTunes, en segundos. */
const PREVIEW_SECONDS = 30

export interface ResolvedStream {
  /** URL reproducible directamente por el reproductor. */
  url: string
  /** Duración real de lo que se va a reproducir (30 s en previews). */
  duration: number
  /** `true` si solo se puede reproducir un fragmento de 30 s. */
  preview: boolean
}

interface DeezerTrack {
  id?: number
  title?: string
  duration?: number
  preview?: string
  artist?: { name?: string }
  album?: { cover_medium?: string; cover_xl?: string; cover_big?: string }
}

interface AudiusTrack {
  id?: string
  title?: string
  duration?: number
  artwork?: Record<string, string>
  user?: { name?: string; handle?: string }
}

interface ITunesSong {
  trackId?: number
  trackName?: string
  artistName?: string
  previewUrl?: string
  artworkUrl100?: string
  trackTimeMillis?: number
}

/** Normaliza texto para comparar títulos y artistas entre proveedores. */
export function normalizeForMatch(value: string): string {
  return value
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\(.*?\)|\[.*?\]/g, ' ')
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function scoreMatch(candidateTitle: string, candidateArtist: string, title: string, artist: string) {
  const ct = normalizeForMatch(candidateTitle)
  const ca = normalizeForMatch(candidateArtist)
  const t = normalizeForMatch(title)
  const a = normalizeForMatch(artist)

  let titleScore = 0
  if (ct && ct === t) titleScore = 3
  else if (ct && t && (ct.includes(t) || t.includes(ct))) titleScore = 2

  let artistScore = 0
  if (ca && ca === a) artistScore = 2
  else if (ca && a && (ca.includes(a) || a.includes(ca))) artistScore = 1

  return titleScore + artistScore
}

function pickBest<T>(items: T[], toTitle: (item: T) => string, toArtist: (item: T) => string, title: string, artist: string): T | null {
  let best: T | null = null
  let bestScore = -1
  for (const item of items) {
    const score = scoreMatch(toTitle(item), toArtist(item), title, artist)
    if (score > bestScore) {
      bestScore = score
      best = item
    }
  }
  return best
}

function pickAudiusArtwork(artwork: Record<string, string> | undefined): string {
  if (!artwork) return ''
  return artwork['1000x1000'] ?? artwork['480x480'] ?? artwork['150x150'] ?? ''
}

function deezerArtwork(album: DeezerTrack['album']): string {
  return album?.cover_xl ?? album?.cover_big ?? album?.cover_medium ?? ''
}

function itunesArtwork(url: string | undefined): string {
  return url ? url.replace('100x100bb', '600x600bb') : ''
}

/** Convierte un resultado de Deezer en una pista del catálogo. */
function toTrackFromDeezer(item: DeezerTrack): Track | null {
  if (!item.id || !item.title || !item.preview) return null
  return {
    id: `deezer:${item.id}`,
    title: item.title,
    artist: item.artist?.name ?? 'Artista desconocido',
    thumbnail: deezerArtwork(item.album),
    duration: item.duration && item.duration > 0 ? item.duration : PREVIEW_SECONDS,
    source: 'deezer',
    streamUrl: item.preview,
    preview: true,
  }
}

/** Convierte un resultado de Audius en una pista del catálogo. */
function toTrackFromAudius(item: AudiusTrack): Track | null {
  if (!item.id || !item.title) return null
  return {
    id: `audius:${item.id}`,
    title: item.title,
    artist: item.user?.name ?? item.user?.handle ?? 'Artista desconocido',
    thumbnail: pickAudiusArtwork(item.artwork),
    duration: item.duration ?? 0,
    source: 'audius',
    streamUrl: `${AUDIUS_BASE}/tracks/${encodeURIComponent(item.id)}/stream?${qs({ app_name: APP_NAME })}`,
    preview: false,
  }
}

/** Convierte un resultado de iTunes en una pista del catálogo. */
function toTrackFromITunes(item: ITunesSong): Track | null {
  if (!item.trackId || !item.trackName || !item.previewUrl) return null
  return {
    id: `itunes:${item.trackId}`,
    title: item.trackName,
    artist: item.artistName ?? 'Artista desconocido',
    thumbnail: itunesArtwork(item.artworkUrl100),
    duration: item.trackTimeMillis ? Math.round(item.trackTimeMillis / 1000) : PREVIEW_SECONDS,
    source: 'itunes',
    streamUrl: item.previewUrl,
    preview: true,
  }
}

/** URL de audio completo de una pista de Audius por su identificador. */
export function audiusStreamUrl(trackId: string): string {
  return `${AUDIUS_BASE}/tracks/${encodeURIComponent(trackId)}/stream?${qs({ app_name: APP_NAME })}`
}

export async function searchDeezer(query: string, limit: number): Promise<Track[]> {
  const data = await getJson<{ data?: DeezerTrack[] }>(
    `${DEEZER_BASE}/search?${qs({ q: query, limit })}`,
  )
  return (data.data ?? [])
    .map(toTrackFromDeezer)
    .filter((track): track is Track => track !== null)
}

export async function searchAudius(query: string, limit: number): Promise<Track[]> {
  const data = await getJson<{ data?: AudiusTrack[] }>(
    `${AUDIUS_BASE}/tracks/search?${qs({ query, app_name: APP_NAME, limit })}`,
  )
  return (data.data ?? [])
    .map(toTrackFromAudius)
    .filter((track): track is Track => track !== null)
}

export async function searchITunes(query: string, limit: number): Promise<Track[]> {
  const data = await getJson<{ results?: ITunesSong[] }>(
    `${ITUNES_BASE}/search?${qs({ term: query, entity: 'song', limit })}`,
    undefined,
    12000,
  )
  return (data.results ?? [])
    .map(toTrackFromITunes)
    .filter((track): track is Track => track !== null)
}

/**
 * Busca en el catálogo legal y combina varios proveedores sin duplicados.
 * Cada proveedor es opcional: si uno falla (por ejemplo, Deezer bloqueado por
 * CORS en la vista web) los demás siguen respondiendo.
 */
export async function searchCatalog(query: string, limit = 25): Promise<Track[]> {
  const trimmed = query.trim()
  if (!trimmed) return []

  const [deezer, itunes, audius] = await Promise.allSettled([
    searchDeezer(trimmed, limit),
    searchITunes(trimmed, limit),
    searchAudius(trimmed, Math.min(limit, 10)),
  ])

  const results: Track[] = []
  const seen = new Set<string>()

  const push = (settled: PromiseSettledResult<Track[]>) => {
    if (settled.status !== 'fulfilled') return
    for (const track of settled.value) {
      const key = `${normalizeForMatch(track.artist)}|${normalizeForMatch(track.title)}`
      if (seen.has(key)) continue
      seen.add(key)
      results.push(track)
    }
  }

  push(deezer)
  push(itunes)
  push(audius)

  return results.slice(0, limit)
}

const streamCache = new Map<string, ResolvedStream | null>()

async function matchAudius(title: string, artist: string): Promise<ResolvedStream | null> {
  const data = await getJson<{ data?: AudiusTrack[] }>(
    `${AUDIUS_BASE}/tracks/search?${qs({ query: `${title} ${artist}`, app_name: APP_NAME, limit: 10 })}`,
  )
  const items = data.data ?? []
  const match = pickBest(items, (item) => item.title ?? '', (item) => item.user?.name ?? '', title, artist)
  if (!match?.id) return null
  // Solo aceptamos coincidencia fuerte para no reproducir algo parecido.
  if (scoreMatch(match.title ?? '', match.user?.name ?? '', title, artist) < 4) return null
  return { url: audiusStreamUrl(match.id), duration: match.duration ?? 0, preview: false }
}

async function matchDeezer(title: string, artist: string): Promise<ResolvedStream | null> {
  const data = await getJson<{ data?: DeezerTrack[] }>(
    `${DEEZER_BASE}/search?${qs({ q: `${title} ${artist}`, limit: 10 })}`,
  )
  const items = data.data ?? []
  const match = pickBest(items, (item) => item.title ?? '', (item) => item.artist?.name ?? '', title, artist)
  if (!match?.preview) return null
  if (scoreMatch(match.title ?? '', match.artist?.name ?? '', title, artist) < 2) return null
  return { url: match.preview, duration: PREVIEW_SECONDS, preview: true }
}

async function matchITunes(title: string, artist: string): Promise<ResolvedStream | null> {
  const data = await getJson<{ results?: ITunesSong[] }>(
    `${ITUNES_BASE}/search?${qs({ term: `${title} ${artist}`, entity: 'song', limit: 10 })}`,
    undefined,
    12000,
  )
  const items = data.results ?? []
  const match = pickBest(items, (item) => item.trackName ?? '', (item) => item.artistName ?? '', title, artist)
  if (!match?.previewUrl) return null
  if (scoreMatch(match.trackName ?? '', match.artistName ?? '', title, artist) < 2) return null
  return { url: match.previewUrl, duration: PREVIEW_SECONDS, preview: true }
}

/**
 * Resuelve la URL reproducible de una pista sin stream propio (por ejemplo,
 * una canción importada de Spotify). Prioriza canciones completas de Audius y
 * cae a los previews oficiales de Deezer e iTunes.
 */
export async function resolveCatalogStream(title: string, artist: string): Promise<ResolvedStream | null> {
  const key = `${normalizeForMatch(artist)}|${normalizeForMatch(title)}`
  if (streamCache.has(key)) return streamCache.get(key) ?? null

  const resolvers = [matchAudius, matchDeezer, matchITunes]
  for (const resolver of resolvers) {
    try {
      const resolved = await resolver(title, artist)
      if (resolved) {
        streamCache.set(key, resolved)
        return resolved
      }
    } catch {
      // Probamos el siguiente proveedor.
    }
  }

  streamCache.set(key, null)
  return null
}
