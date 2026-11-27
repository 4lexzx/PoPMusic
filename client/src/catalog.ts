/**
 * Catálogo musical (Deezer): búsqueda, álbumes, artistas con foto, artistas
 * relacionados, radios por artista, listas y tops. Todo es música: el
 * catálogo no contiene videos.
 *
 * Los ids llevan el prefijo `dz:` para distinguirlos de los de iTunes que ya
 * hay guardados en bibliotecas antiguas.
 */
import type { Album, Artist, RemotePlaylist, SearchAll, Track } from './types'

export const DZ = 'dz:'
export const isDz = (id?: string) => !!id && id.startsWith(DZ)
const raw = (id: string) => id.replace(DZ, '')

const memo = new Map<string, { at: number; data: Promise<any> }>()
const MEMO_MS = 10 * 60 * 1000

/**
 * Deezer permite ~50 peticiones cada 5 s. Se reparten con una cola (máx. 40
 * por ventana de 5 s y 6 a la vez) y se reintenta si responde "quota".
 */
const WINDOW_MS = 5000
const MAX_PER_WINDOW = 45
const MAX_PARALLEL = 10
const stamps: number[] = []
let running = 0
const waiting: (() => void)[] = []

function pump() {
  const now = Date.now()
  while (stamps.length && now - stamps[0] > WINDOW_MS) stamps.shift()
  while (waiting.length && running < MAX_PARALLEL && stamps.length < MAX_PER_WINDOW) {
    stamps.push(Date.now())
    running++
    waiting.shift()!()
  }
  if (waiting.length) setTimeout(pump, stamps.length >= MAX_PER_WINDOW ? WINDOW_MS - (now - stamps[0]) + 20 : 50)
}

/** `urgent`: lo que pidió el usuario (buscar, abrir un álbum) pasa delante del feed. */
function slot<T>(fn: () => Promise<T>, urgent = false): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const run = () => { fn().then(resolve, reject).finally(() => { running--; pump() }) }
    if (urgent) waiting.unshift(run)
    else waiting.push(run)
    pump()
  })
}

async function request(path: string, urgent: boolean, attempt = 0): Promise<any> {
  const api = window.popApi?.deezer
  const data = await slot(() => (api ? api(path) : fetch(`/dz/${path}`).then((r) => (r.ok ? r.json() : null))), urgent)
  // code 4 = cuota superada: se espera y se reintenta.
  if (data?.error?.code === 4 && attempt < 3) {
    await new Promise((r) => setTimeout(r, 1200 * (attempt + 1)))
    return request(path, urgent, attempt + 1)
  }
  if (!data || data.error) throw new Error(data?.error?.message || 'Catálogo no disponible')
  return data
}

/** GET a la API de Deezer: por IPC en Electron o por el proxy de Vite en el navegador. */
export function dz<T = any>(path: string, urgent = false): Promise<T> {
  const hit = memo.get(path)
  if (hit && Date.now() - hit.at < MEMO_MS) return hit.data
  const job = request(path, urgent)
  memo.set(path, { at: Date.now(), data: job })
  job.catch(() => memo.delete(path))
  return job
}

export function mapTrack(t: any, album?: any): Track {
  const al = t.album || album || {}
  const contributors: string[] = Array.isArray(t.contributors) ? t.contributors.map((c: any) => c.name) : []
  const artists = contributors.length > 1 ? contributors.join(', ') : t.artist?.name || ''
  return {
    id: DZ + t.id,
    title: t.title_short && t.title_version ? `${t.title_short} ${t.title_version}`.trim() : t.title,
    artists,
    artistId: t.artist?.id ? DZ + t.artist.id : undefined,
    coverUrl: al.cover_xl || al.cover_big || '',
    durationMs: (t.duration || 0) * 1000,
    previewUrl: t.preview || undefined,
    explicit: !!t.explicit_lyrics,
    album: al.id ? { id: DZ + al.id, title: al.title || '' } : undefined,
  }
}

export function mapAlbum(a: any): Album {
  return {
    id: DZ + a.id,
    title: a.title,
    artist: a.artist?.name || '',
    artistId: a.artist?.id ? DZ + a.artist.id : undefined,
    year: a.release_date ? Number(String(a.release_date).slice(0, 4)) : undefined,
    coverUrl: a.cover_xl || a.cover_big || '',
    kind: a.record_type,
  }
}

export function mapArtist(a: any): Artist {
  return {
    id: DZ + a.id,
    name: a.name,
    picture: a.picture_xl || a.picture_big || undefined,
    fans: a.nb_fan,
  }
}

const tracksOf = (data: any): Track[] =>
  (data?.data || []).filter((t: any) => t.readable !== false && t.type !== 'podcast' && t.type !== 'episode').map((t: any) => mapTrack(t))

const enc = encodeURIComponent

export async function dzSearchAll(q: string): Promise<SearchAll> {
  const [tr, al, ar] = await Promise.all([
    dz(`search?q=${enc(q)}&limit=40`, true),
    dz(`search/album?q=${enc(q)}&limit=24`, true).catch(() => ({ data: [] })),
    dz(`search/artist?q=${enc(q)}&limit=12`, true).catch(() => ({ data: [] })),
  ])
  return {
    tracks: tracksOf(tr),
    albums: (al.data || []).map(mapAlbum),
    artists: (ar.data || []).map(mapArtist),
  }
}

export async function dzSearchTracks(q: string, limit = 25): Promise<Track[]> {
  return tracksOf(await dz(`search?q=${enc(q)}&limit=${limit}`))
}

export async function dzAlbum(id: string): Promise<Album> {
  const a = await dz(`album/${raw(id)}`, true)
  const album = mapAlbum(a)
  album.tracks = (a.tracks?.data || []).map((t: any) => mapTrack(t, a))
  album.genre = a.genres?.data?.[0]?.name
  return album
}

export async function dzArtist(id: string): Promise<Artist> {
  const r = raw(id)
  const [a, top, albums, related] = await Promise.all([
    dz(`artist/${r}`, true),
    dz(`artist/${r}/top?limit=25`, true).catch(() => ({ data: [] })),
    dz(`artist/${r}/albums?limit=60`, true).catch(() => ({ data: [] })),
    dz(`artist/${r}/related?limit=16`, true).catch(() => ({ data: [] })),
  ])
  const artist = mapArtist(a)
  artist.top = tracksOf(top)
  const seen = new Set<string>()
  artist.albums = (albums.data || [])
    .map((x: any) => ({ ...mapAlbum(x), artist: artist.name, artistId: artist.id }))
    .filter((x: Album) => {
      const key = x.title.toLowerCase().replace(/\s*[([].*?[)\]]\s*/g, '')
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    .sort((x: Album, y: Album) => (y.year || 0) - (x.year || 0))
  artist.related = (related.data || []).map(mapArtist)
  return artist
}

export async function dzRelated(id: string, limit = 20): Promise<Artist[]> {
  const data = await dz(`artist/${raw(id)}/related?limit=${limit}`)
  return (data.data || []).map(mapArtist)
}

export async function dzTop(id: string, limit = 10, index = 0): Promise<Track[]> {
  return tracksOf(await dz(`artist/${raw(id)}/top?limit=${limit}${index ? `&index=${index}` : ''}`))
}

/** Radio del artista: canciones suyas y de artistas parecidos. */
export async function dzArtistRadio(id: string, limit = 40): Promise<Track[]> {
  return tracksOf(await dz(`artist/${raw(id)}/radio?limit=${limit}`))
}

export async function dzChart(genre = 0) {
  const c = await dz(`chart/${genre}?limit=50`)
  return {
    tracks: tracksOf(c.tracks),
    artists: (c.artists?.data || []).map(mapArtist) as Artist[],
    albums: (c.albums?.data || []).map(mapAlbum) as Album[],
    playlists: (c.playlists?.data || []).map(mapPlaylist) as RemotePlaylist[],
  }
}

function mapPlaylist(p: any): RemotePlaylist {
  return {
    id: DZ + p.id,
    title: p.title,
    description: p.description || '',
    coverUrl: p.picture_xl || p.picture_big || '',
    count: p.nb_tracks || 0,
    author: p.user?.name || p.creator?.name,
  }
}

export async function dzPlaylist(id: string): Promise<RemotePlaylist> {
  const p = await dz(`playlist/${raw(id)}`, true)
  const pl = mapPlaylist(p)
  pl.tracks = tracksOf(p.tracks)
  return pl
}

export const GENRES = [
  { id: 132, name: 'Pop' },
  { id: 116, name: 'Rap / Hip-Hop' },
  { id: 197, name: 'Latina' },
  { id: 152, name: 'Rock' },
  { id: 113, name: 'Dance' },
  { id: 85, name: 'Alternativa' },
  { id: 165, name: 'R&B' },
  { id: 106, name: 'Electro' },
  { id: 98, name: 'Clásica' },
  { id: 129, name: 'Jazz' },
  { id: 84, name: 'Country' },
  { id: 466, name: 'Folk' },
  { id: 464, name: 'Metal' },
  { id: 144, name: 'Reggae' },
  { id: 153, name: 'Blues' },
  { id: 173, name: 'Cine y juegos' },
]

/** Busca el artista de Deezer que corresponde a un nombre (para bibliotecas antiguas de iTunes). */
const ARTIST_KEY = 'popmusic-client-dz-artists-v1'
let artistIndex: Record<string, string> | null = null
export async function dzArtistIdByName(name: string): Promise<string | null> {
  if (!artistIndex) {
    try { artistIndex = JSON.parse(localStorage.getItem(ARTIST_KEY) || '{}') } catch { artistIndex = {} }
  }
  const key = name.toLowerCase().trim()
  if (!key) return null
  if (artistIndex![key]) return artistIndex![key]
  try {
    const res = await dz(`search/artist?q=${enc(name)}&limit=1`)
    const hit = res.data?.[0]
    if (!hit) return null
    artistIndex![key] = DZ + hit.id
    localStorage.setItem(ARTIST_KEY, JSON.stringify(artistIndex))
    return DZ + hit.id
  } catch {
    return null
  }
}

/** Artista principal de una canción (sin "feat.", "&", etc.). */
export function mainArtist(t: Track): string {
  return t.artists.split(/,|&| feat\.| ft\.| x /i)[0].trim()
}

/** Id de artista de Deezer para una canción, venga de donde venga. */
export async function artistIdFor(t: Track): Promise<string | null> {
  if (isDz(t.artistId)) return t.artistId!
  return dzArtistIdByName(mainArtist(t))
}
