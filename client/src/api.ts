import type { Album, Artist, LyricLine, SearchAll, SearchResponse, Settings, Track } from './types'
import { estimateWords, parseLyrics, parseLyricsfile, parseYrc } from './lyrics'
import { dzAlbum, dzArtist, dzArtistIdByName, dzSearchAll, isDz } from './catalog'

const API = import.meta.env.VITE_API_URL || 'https://backend-theta-one-73.vercel.app'

/**
 * Carátula en alta resolución. Las portadas del catálogo (iTunes) aceptan
 * cualquier tamaño cambiando el segmento `NxNbb` de la URL.
 */
export function hd(url: string | undefined, size = 1000): string {
  if (!url) return ''
  // Deezer: .../1000x1000-000000-80-0-0.jpg (sirve hasta 1400 px)
  if (url.includes('dzcdn.net')) {
    const px = size
    return url.replace(/\/\d+x\d+-(\d+-\d+-\d+-\d+)\.jpg/, `/${px}x${px}-$1.jpg`)
  }
  return url.replace(/\/\d+x\d+(bb|cc)?\.(jpg|png|webp)/, `/${size}x${size}bb.jpg`)
}

async function get<T>(path: string, params?: Record<string, string>): Promise<T> {
  const qs = params ? `?${new URLSearchParams(params)}` : ''
  const res = await fetch(`${API}${path}${qs}`)
  if (!res.ok) throw new Error(`Catálogo: ${res.status}`)
  return res.json() as Promise<T>
}

function mapTrack(raw: any): Track {
  return {
    id: String(raw.id),
    title: raw.title,
    artists: raw.artists,
    artistId: raw.artistId || raw.artistList?.[0]?.id || undefined,
    coverUrl: raw.coverUrl || raw.cover?.url || '',
    durationMs: raw.durationMs || 0,
    previewUrl: raw.previewUrl,
    releaseDate: raw.releaseDate,
    explicit: raw.explicit,
    album: raw.album,
  }
}

function mapAlbum(raw: any): Album {
  return {
    id: String(raw.id),
    title: raw.title,
    artist: raw.artists?.[0]?.name || '',
    artistId: raw.artists?.[0]?.id || undefined,
    year: raw.year,
    coverUrl: raw.cover?.url || '',
    tracks: Array.isArray(raw.tracks) ? raw.tracks.map(mapTrack) : undefined,
  }
}

/** Solo canciones: el catálogo nunca devuelve videos. */
export async function searchTracks(query: string, country = 'us'): Promise<SearchResponse> {
  const data = await get<any>('/v1/catalog/search', { q: query, type: 'track', country })
  return { items: (data.items || []).filter((i: any) => i.type === 'track').map(mapTrack) }
}

export async function searchAll(query: string, country = 'us'): Promise<SearchAll> {
  try {
    const res = await dzSearchAll(query)
    if (res.tracks.length || res.albums.length) return res
  } catch { /* iTunes de respaldo */ }
  return itunesSearchAll(query, country)
}

async function itunesSearchAll(query: string, country: string): Promise<SearchAll> {
  const data = await get<any>('/v1/catalog/search', { q: query, type: 'all', country })
  const items: any[] = data.items || []
  return {
    tracks: items.filter((i) => i.type === 'track').map(mapTrack),
    albums: items.filter((i) => i.type === 'album').map(mapAlbum),
    artists: dedupeArtists(items.filter((i) => i.type === 'artist').map((a) => ({ id: String(a.id), name: a.name }))),
  }
}

function dedupeArtists(list: Artist[]): Artist[] {
  const seen = new Set<string>()
  return list.filter((a) => {
    const key = a.name.toLowerCase()
    if (!a.id || seen.has(key)) return false
    seen.add(key)
    return true
  })
}

export async function getAlbum(id: string): Promise<Album> {
  if (isDz(id)) return dzAlbum(id)
  return mapAlbum(await get<any>(`/v1/catalog/albums/${encodeURIComponent(id)}`))
}

export async function getArtist(id: string, name?: string): Promise<Artist> {
  if (isDz(id)) return dzArtist(id)
  // Artista guardado desde iTunes: se busca su ficha en Deezer (foto, top, relacionados).
  const raw = await get<any>(`/v1/catalog/artists/${encodeURIComponent(id)}`).catch(() => ({}))
  const dzId = await dzArtistIdByName(raw.name || name || '')
  if (dzId) return dzArtist(dzId)
  const albums: Album[] = (raw.albums || []).map(mapAlbum)
  // Discografía de más nueva a más vieja y sin repetir el mismo título.
  const seen = new Set<string>()
  const unique = albums
    .sort((a, b) => (b.year || 0) - (a.year || 0))
    .filter((a) => {
      const key = a.title.toLowerCase().replace(/\s*[([].*?[)\]]\s*/g, '')
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
  return { id: String(raw.id || id), name: raw.name || '', albums: unique }
}

export async function explorePopular(country = 'us'): Promise<SearchResponse> {
  const data = await get<any>('/v1/explore/popular', { country })
  return { items: (data.items || []).map(mapTrack) }
}

export async function fetchPreview(track: string, artist: string): Promise<string | null> {
  try {
    const data = await get<any>('/v1/preview', { track, artist })
    return data.url ?? null
  } catch {
    return null
  }
}

const LRCLIB = 'https://lrclib.net/api'

/** Quita "(feat. X)", "- Remastered" y similares para que se encuentre la canción. */
function cleanTitle(title: string): string {
  return title
    .replace(/\s*[([](feat|ft|with|con)\.?[^)\]]*[)\]]/gi, '')
    .replace(/\s*-\s*(remaster(ed)?|live|en vivo|single|radio edit).*$/i, '')
    .trim()
}

export interface LyricsResult {
  lines: LyricLine[]
  /** De dónde salen los tiempos por palabra. */
  source: 'lrclib-words' | 'netease-words' | 'estimated' | 'lines' | 'plain' | 'none'
}

const norm = (s: string) => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]/g, '')

async function netease(path: string): Promise<any> {
  const api = window.popApi?.netease
  if (api) return api(path)
  const res = await fetch(`/ne/${path}`)
  return res.ok ? res.json() : null
}

/** NetEase: busca la misma canción (artista y duración) y trae su letra YRC. */
async function neteaseWords(name: string, artist: string, durationMs: number): Promise<LyricLine[] | null> {
  try {
    const q = encodeURIComponent(`${name} ${artist}`)
    const found = await netease(`api/search/get?s=${q}&type=1&limit=8`)
    const songs: any[] = found?.result?.songs || []
    const a = norm(artist)
    const pick = songs.find((s) =>
      (s.artists || []).some((x: any) => norm(x.name) === a || norm(x.name).includes(a) || a.includes(norm(x.name)))
      && norm(s.name).startsWith(norm(name).slice(0, 12))
      && (!durationMs || Math.abs(s.duration - durationMs) < 4000))
    if (!pick) return null
    const lyr = await netease(`api/song/lyric/v1?id=${pick.id}&cp=false&tv=0&lv=0&rv=0&kv=0&yv=0&ytv=0&yrv=0`)
    const yrc: string = lyr?.yrc?.lyric || ''
    if (!yrc) return null
    const lines = parseYrc(yrc)
    return lines.length > 3 ? lines : null
  } catch {
    return null
  }
}

/**
 * Letra con los mejores tiempos disponibles:
 *  1. LrcLib con tiempos por palabra (`lyricsfile`).
 *  2. NetEase YRC (por palabra, con la duración de cada una).
 *  3. LrcLib por línea, con las palabras repartidas por sílabas.
 * Siempre se elige la versión cuya duración coincide con el audio que suena.
 */
export async function fetchLyrics(track: string, artist: string, durationMs = 0): Promise<LyricsResult> {
  const name = cleanTitle(track)
  const mainArtist = artist.split(/,|&| feat\.| ft\./i)[0].trim()
  const near = (r: any) => (durationMs ? Math.abs((r.duration || 0) * 1000 - durationMs) : 0)
  let rows: any[] = []
  try {
    const params = new URLSearchParams({ track_name: name, artist_name: mainArtist })
    const res = await fetch(`${LRCLIB}/search?${params}`)
    if (res.ok) rows = await res.json()
  } catch { /* sigue con NetEase */ }

  const close = (max: number) => (r: any) => near(r) < max
  const words = rows.filter((r) => r.hasWordSync && r.syncedLyrics).sort((x, y) => near(x) - near(y))
  const withWords = words.find(close(3000))
  if (withWords) {
    // La búsqueda no trae el lyricsfile: se pide la ficha completa.
    try {
      const full = await fetch(`${LRCLIB}/get/${withWords.id}`).then((r) => (r.ok ? r.json() : null))
      if (full?.lyricsfile) {
        const lines = parseLyricsfile(full.lyricsfile)
        if (lines.length > 3) return { lines, source: 'lrclib-words' }
      }
    } catch { /* sigue */ }
  }

  const ne = await neteaseWords(name, mainArtist, durationMs)
  if (ne) return { lines: ne, source: 'netease-words' }

  const synced = rows.filter((r) => r.syncedLyrics).sort((x, y) => near(x) - near(y))
  const plain = rows.filter((r) => r.plainLyrics).sort((x, y) => near(x) - near(y))
  const best = synced.find(close(3000)) || synced.find(close(8000)) || synced[0]
  if (best) return { lines: estimateWords(parseLyrics(best.syncedLyrics)), source: 'estimated' }
  const flat = plain.find(close(8000)) || plain[0]
  if (flat) return { lines: parseLyrics(flat.plainLyrics), source: 'plain' }
  try {
    const data = await get<any>('/v1/lyrics', { track: name, artist: mainArtist })
    if (data.lyrics) {
      const lines = parseLyrics(data.lyrics)
      const isSynced = lines.some((l) => l.time >= 0)
      return { lines: isSynced ? estimateWords(lines) : lines, source: isSynced ? 'estimated' : 'plain' }
    }
  } catch { /* nada */ }
  return { lines: [], source: 'none' }
}

export const DEFAULT_SETTINGS: Settings = {
  country: 'us',
  volume: 0.8,
  crossfade: 0,
  autoplay: true,
  autoRadio: true,
  doubleClick: 'play',
  startup: 'resume',
  outputDevice: '',
  overlay: true,
  overlayOnMinimize: true,
  overlayAnchor: 'center',
  artQuality: 1000,
  theme: 'dark',
  accent: '',
  tint: 0.55,
  reduceMotion: false,
  lyricsSize: 'l',
  lyricsOffset: 0,
  lyricsBlur: true,
  lyricsEffect: 'karaoke',
}
