import type { Track } from './types'
import type { ImportedList } from './electron.d'
import { dz, dzAlbum, dzPlaylist, mapTrack } from './catalog'

export const IMPORT_SOURCES = ['Spotify', 'YouTube', 'YouTube Music', 'SoundCloud', 'Apple Music', 'Deezer']

export interface ImportResult { name: string; tracks: Track[]; source: string; missing: number }

const norm = (s: string) => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
  .replace(/\s*[([].*?[)\]]\s*/g, ' ').replace(/feat\.?.*$/, '').replace(/[^a-z0-9]+/g, ' ').trim()

/** Parecido entre dos textos por palabras compartidas (0-1). */
function similar(a: string, b: string): number {
  const A = new Set(norm(a).split(' ').filter(Boolean))
  const B = new Set(norm(b).split(' ').filter(Boolean))
  if (!A.size || !B.size) return 0
  let common = 0
  A.forEach((w) => { if (B.has(w)) common++ })
  return common / Math.max(A.size, B.size)
}

/** Busca la canción en el catálogo: mismo título y artista, duración parecida. */
async function match(item: ImportedList['items'][number]): Promise<Track | null> {
  const score = (t: Track) => {
    const title = similar(t.title, item.title)
    const artist = Math.max(similar(t.artists, item.artist), similar(t.title + ' ' + t.artists, item.artist + ' ' + item.title))
    const dur = item.durationMs && t.durationMs ? Math.max(0, 1 - Math.abs(t.durationMs - item.durationMs) / 15000) : 0.5
    return title * 0.55 + artist * 0.3 + dur * 0.15
  }
  const queries = [
    `artist:"${item.artist}" track:"${item.title}"`,
    `${item.artist} ${item.title}`,
  ]
  for (const q of queries) {
    try {
      const res = await dz(`search?q=${encodeURIComponent(q)}&limit=6`)
      const tracks: Track[] = (res.data || []).map((t: any) => mapTrack(t))
      const best = tracks.map((t) => ({ t, s: score(t) })).sort((a, b) => b.s - a.s)[0]
      if (best && best.s >= 0.55) return best.t
    } catch { /* siguiente intento */ }
  }
  return null
}

/** Si no está en el catálogo, se guarda tal cual (suena igual: se busca por artista y título). */
function rawTrack(item: ImportedList['items'][number], i: number): Track {
  return {
    id: `imp:${norm(item.artist)}:${norm(item.title)}:${i}`,
    title: item.title,
    artists: item.artist,
    coverUrl: item.cover || '',
    durationMs: item.durationMs || 0,
  }
}

export async function importFromLink(link: string, onProgress: (done: number, total: number) => void): Promise<ImportResult> {
  const api = window.popApi?.importLink
  let list: ImportedList
  if (api) {
    const res = await api(link)
    if (!res.ok) throw new Error(res.error)
    list = res.data
  } else {
    // Sin la app de escritorio solo se puede leer Deezer (API del catálogo).
    const m = /deezer\.com\/(?:[a-z]{2}\/)?(playlist|album|track)\/(\d+)/.exec(link)
    if (!m) throw new Error('Importar desde este enlace solo funciona en la app de escritorio')
    list = { source: 'deezer', kind: m[1] as 'playlist', id: m[2], title: '', cover: '', items: [] }
  }

  if (list.source === 'deezer' && list.id) {
    onProgress(0, 1)
    if (list.kind === 'album') {
      const a = await dzAlbum(`dz:${list.id}`)
      onProgress(1, 1)
      return { name: a.title, tracks: a.tracks || [], source: link, missing: 0 }
    }
    if (list.kind === 'track') {
      const t = mapTrack(await dz(`track/${list.id}`))
      onProgress(1, 1)
      return { name: t.title, tracks: [t], source: link, missing: 0 }
    }
    const p = await dzPlaylist(`dz:${list.id}`)
    onProgress(1, 1)
    return { name: p.title, tracks: p.tracks || [], source: link, missing: 0 }
  }

  const items = list.items
  if (!items.length) throw new Error('La lista está vacía o es privada')
  const out: (Track | null)[] = new Array(items.length).fill(null)
  let done = 0
  let next = 0
  let missing = 0
  onProgress(0, items.length)
  // 6 a la vez: el limitador del catálogo reparte el resto.
  await Promise.all(Array.from({ length: Math.min(6, items.length) }, async () => {
    while (next < items.length) {
      const i = next++
      const found = await match(items[i])
      if (!found) missing++
      out[i] = found || rawTrack(items[i], i)
      onProgress(++done, items.length)
    }
  }))
  // Sin duplicados (la misma canción dos veces en la lista original).
  const seen = new Set<string>()
  const tracks = (out.filter(Boolean) as Track[]).filter((t) => (seen.has(t.id) ? false : (seen.add(t.id), true)))
  return { name: list.title || 'Lista importada', tracks, source: link, missing }
}
