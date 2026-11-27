/**
 * Motor de recomendaciones del inicio. Solo música.
 *
 * - Perfil: artistas de tus "Me gusta" (pesan más) y de lo que escuchaste
 *   hace poco (pesa más lo reciente).
 * - Fuentes: radios por artista, artistas relacionados, top de cada artista,
 *   top del país, tendencias, listas destacadas y tops por género.
 * - Sin límite: el feed se genera por tandas mientras bajas. Cuando se acaban
 *   tus artistas, se exploran los relacionados de los relacionados.
 * - Sin repetir: nunca sale dos veces la misma canción en el feed y no se
 *   recomienda como "descubrimiento" lo que ya está en tu biblioteca.
 */
import type { Album, Artist, RemotePlaylist, Track } from './types'
import {
  GENRES, artistIdFor, dz, dzArtistRadio, dzChart, dzRelated, dzTop, isDz, mainArtist, mapArtist,
} from './catalog'
import { explorePopular } from './api'

export interface Mix { id: string; title: string; subtitle: string; cover: string; tracks: Track[] }

export type Section =
  | { kind: 'tracks'; key: string; title: string; subtitle?: string; tracks: Track[]; layout?: 'cards' | 'list' }
  | { kind: 'mixes'; key: string; title: string; subtitle?: string; mixes: Mix[] }
  | { kind: 'artists'; key: string; title: string; subtitle?: string; artists: Artist[] }
  | { kind: 'albums'; key: string; title: string; subtitle?: string; albums: Album[] }
  | { kind: 'playlists'; key: string; title: string; subtitle?: string; playlists: RemotePlaylist[] }

export function songKey(t: Track): string {
  const title = t.title.toLowerCase().replace(/\s*[([].*?[)\]]\s*/g, '').replace(/\s*-\s*.*$/, '').trim()
  return `${title}|${mainArtist(t).toLowerCase()}`
}

function shuffle<T>(list: T[]): T[] {
  const a = [...list]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

/** Intercala listas: a1, b1, c1, a2, b2… (evita bloques del mismo artista). */
function interleave<T>(lists: T[][]): T[] {
  const out: T[] = []
  const max = Math.max(0, ...lists.map((l) => l.length))
  for (let i = 0; i < max; i++) for (const l of lists) if (l[i]) out.push(l[i])
  return out
}

interface SeedArtist { name: string; weight: number; sample: Track }

/** Artistas que más escuchas, ordenados por peso. */
export function profile(favorites: Track[], recent: Track[]): SeedArtist[] {
  const map = new Map<string, SeedArtist>()
  const add = (t: Track, w: number) => {
    const name = mainArtist(t)
    if (!name) return
    const key = name.toLowerCase()
    const cur = map.get(key)
    if (cur) cur.weight += w
    else map.set(key, { name, weight: w, sample: t })
  }
  favorites.slice(0, 200).forEach((t, i) => add(t, 3 * (1 - i / 400)))
  recent.slice(0, 100).forEach((t, i) => add(t, 2 * (1 - i / 120)))
  return [...map.values()].sort((a, b) => b.weight - a.weight)
}

type Task = () => Promise<Section | null>

export class FeedEngine {
  private seen = new Set<string>()
  /** Secciones que ya están en pantalla (p. ej. del feed guardado): no se vuelven a pedir. */
  private shown = new Set<string>()
  private known: Set<string>
  private tasks: Task[] = []
  private frontier: { id: string; name: string; via: string }[] = []
  private visitedArtists = new Set<string>()
  private genreCursor = 0
  private seeds: SeedArtist[]
  private country: string
  private cold: boolean

  constructor(favorites: Track[], recent: Track[], country: string) {
    this.seeds = profile(favorites, recent)
    this.cold = this.seeds.length === 0
    this.country = country
    this.known = new Set([...favorites, ...recent].map(songKey))
    this.plan()
  }

  /** Quita repetidas y, si `fresh`, lo que ya está en tu biblioteca. */
  private take(tracks: Track[], max: number, fresh = true): Track[] {
    const out: Track[] = []
    for (const t of tracks) {
      const k = songKey(t)
      if (this.seen.has(k) || (fresh && this.known.has(k))) continue
      this.seen.add(k)
      out.push(t)
      if (out.length >= max) break
    }
    return out
  }

  private plan() {
    const s = this.seeds
    const t = this.tasks
    if (!this.cold) t.push(() => this.mixes(s.slice(0, 6)))
    if (s[0]) t.push(() => this.becauseYouListen(s[0]))
    t.push(() => this.countryTop())
    if (s[1]) t.push(() => this.becauseYouListen(s[1]))
    t.push(() => this.artistsYouMayLike(s.slice(0, 8)))
    if (s[2]) t.push(() => this.becauseYouListen(s[2]))
    t.push(() => this.trending())
    t.push(() => this.featuredPlaylists())
    for (const seed of s.slice(3, 10)) t.push(() => this.becauseYouListen(seed))
    t.push(() => this.newAlbums())
  }

  /**
   * Siguiente tanda del feed. Las secciones de una tanda se piden a la vez
   * (no una tras otra) y se devuelven en orden. Nunca se agota.
   */
  async next(count = 3): Promise<Section[]> {
    const out: Section[] = []
    let guard = 0
    while (out.length < count && guard++ < 12) {
      while (this.tasks.length < count) this.refill()
      const batch = this.tasks.splice(0, count - out.length)
      const results = await Promise.all(batch.map((task) => task().catch(() => null)))
      for (const r of results) if (r) out.push(r)
    }
    return out
  }

  /** Las canciones de un feed guardado ya se vieron: no se repiten. */
  markSeen(sections: Section[]) {
    for (const s of sections) {
      this.shown.add(s.key)
      if (s.kind === 'tracks') s.tracks.forEach((t) => this.seen.add(songKey(t)))
      if (s.kind === 'artists') s.artists.forEach((a) => this.visitedArtists.add(a.id))
    }
  }

  /** Cuando se acaba el plan: explorar relacionados y alternar con géneros. */
  private refill() {
    for (let i = 0; i < 3; i++) {
      const next = this.frontier.shift()
      if (next) this.tasks.push(() => this.discoverArtist(next))
    }
    const g = GENRES[this.genreCursor++ % GENRES.length]
    this.tasks.push(() => this.genreTop(g.id, g.name))
    if (!this.frontier.length) this.tasks.push(() => this.expandFromChart())
  }

  private remember(artists: Artist[], via: string) {
    for (const a of artists) {
      if (this.visitedArtists.has(a.id)) continue
      this.visitedArtists.add(a.id)
      this.frontier.push({ id: a.id, name: a.name, via })
    }
  }

  // ---------------- generadores ----------------

  private async mixes(seeds: SeedArtist[]): Promise<Section | null> {
    if (this.shown.has('mixes')) return null
    const mixes = (await Promise.all(seeds.map(async (seed) => {
      const id = await artistIdFor(seed.sample)
      if (!id) return null
      const [radio, info] = await Promise.all([dzArtistRadio(id, 50), dz(`artist/${id.slice(3)}`)])
      const tracks = shuffle(radio).filter((t, i, a) => a.findIndex((x) => songKey(x) === songKey(t)) === i)
      if (tracks.length < 8) return null
      const others = [...new Set(tracks.map(mainArtist).filter((n) => n.toLowerCase() !== seed.name.toLowerCase()))].slice(0, 3)
      return {
        id: `mix-${id}`,
        title: `Mezcla de ${seed.name}`,
        subtitle: others.length ? `${others.join(', ')} y más` : 'Canciones parecidas',
        cover: mapArtist(info).picture || seed.sample.coverUrl,
        tracks,
      } as Mix
    }))).filter(Boolean) as Mix[]
    return mixes.length ? { kind: 'mixes', key: 'mixes', title: 'Tus mezclas', subtitle: 'Hechas con lo que más escuchas', mixes } : null
  }

  private async becauseYouListen(seed: SeedArtist): Promise<Section | null> {
    const id = await artistIdFor(seed.sample)
    if (!id || this.shown.has(`byl-${id}`)) return null
    this.visitedArtists.add(id)
    const related = await dzRelated(id, 12)
    this.remember(related.slice(4), seed.name)
    const picks = related.slice(0, 4)
    const tops = await Promise.all(picks.map((a) => dzTop(a.id, 6).catch(() => [])))
    const tracks = this.take(interleave(tops.map(shuffle)), 16)
    if (tracks.length < 5) return null
    return { kind: 'tracks', key: `byl-${id}`, title: seed.name, subtitle: 'Porque escuchas', tracks }
  }

  private async artistsYouMayLike(seeds: SeedArtist[]): Promise<Section | null> {
    if (this.shown.has('aymml')) return null
    const seedIds = new Set<string>()
    const score = new Map<string, { a: Artist; n: number }>()
    const lists = await Promise.all(seeds.map(async (s) => {
      const id = await artistIdFor(s.sample)
      if (!id) return []
      seedIds.add(id)
      return dzRelated(id, 15).catch(() => [])
    }))
    lists.forEach((list) => list.forEach((a, i) => {
      const cur = score.get(a.id)
      const w = 1 - i / 20
      if (cur) cur.n += w
      else score.set(a.id, { a, n: w })
    }))
    const artists = [...score.values()]
      .filter((x) => !seedIds.has(x.a.id) && !seeds.some((s) => s.name.toLowerCase() === x.a.name.toLowerCase()))
      .sort((a, b) => b.n - a.n)
      .map((x) => x.a)
      .slice(0, 16)
    this.remember(artists, 'tus gustos')
    return artists.length >= 4 ? { kind: 'artists', key: 'aymml', title: 'Artistas que te pueden gustar', artists } : null
  }

  private async countryTop(): Promise<Section | null> {
    if (this.shown.has('country')) return null
    const { items } = await explorePopular(this.country)
    const tracks = this.take(items, 50, false)
    return tracks.length ? { kind: 'tracks', key: 'country', title: 'Lo más escuchado', subtitle: 'Top de tu país', tracks } : null
  }

  private async trending(): Promise<Section | null> {
    if (this.shown.has('trending')) return null
    const c = await dzChart(0)
    if (this.cold) this.remember(c.artists, 'tendencias')
    const tracks = this.take(c.tracks, 30, false)
    return tracks.length >= 5 ? { kind: 'tracks', key: 'trending', title: 'Tendencias mundiales', tracks } : null
  }

  private async featuredPlaylists(): Promise<Section | null> {
    if (this.shown.has('pl')) return null
    const c = await dzChart(0)
    return c.playlists.length ? { kind: 'playlists', key: 'pl', title: 'Listas destacadas', playlists: c.playlists } : null
  }

  private async newAlbums(): Promise<Section | null> {
    if (this.shown.has('albums')) return null
    const c = await dzChart(0)
    return c.albums.length ? { kind: 'albums', key: 'albums', title: 'Álbumes del momento', albums: c.albums } : null
  }

  private async genreTop(genreId: number, name: string): Promise<Section | null> {
    const c = await dzChart(genreId)
    this.remember(c.artists.slice(0, 6), name)
    const tracks = this.take(shuffle(c.tracks), 18)
    return tracks.length >= 5 ? { kind: 'tracks', key: `genre-${genreId}-${this.genreCursor}`, title: `Lo mejor de ${name}`, subtitle: 'Género', tracks } : null
  }

  private async discoverArtist(next: { id: string; name: string; via: string }): Promise<Section | null> {
    if (this.shown.has(`disc-${next.id}`)) return null
    const [top, related] = await Promise.all([dzTop(next.id, 15), dzRelated(next.id, 10).catch(() => [])])
    this.remember(related, next.name)
    const tracks = this.take(top, 12)
    return tracks.length >= 4
      ? { kind: 'tracks', key: `disc-${next.id}`, title: next.name, subtitle: `Descubre · por ${next.via}`, tracks }
      : null
  }

  private async expandFromChart(): Promise<Section | null> {
    const g = GENRES[Math.floor(Math.random() * GENRES.length)]
    const c = await dzChart(g.id)
    this.remember(shuffle(c.artists), g.name)
    return null
  }
}

/**
 * Canciones parecidas para seguir sonando cuando se acaba la cola: radio del
 * artista + top de sus relacionados, sin repetir lo que ya sonó.
 */
export async function similarTo(track: Track, exclude: Track[], count = 25): Promise<Track[]> {
  const id = await artistIdFor(track)
  if (!id || !isDz(id)) return []
  const [radio, related] = await Promise.all([
    dzArtistRadio(id, 60).catch(() => []),
    dzRelated(id, 6).catch(() => []),
  ])
  const tops = await Promise.all(related.slice(0, 4).map((a) => dzTop(a.id, 5).catch(() => [])))
  const skip = new Set(exclude.map(songKey))
  skip.add(songKey(track))
  const out: Track[] = []
  for (const t of interleave([shuffle(radio), ...tops])) {
    const k = songKey(t)
    if (skip.has(k)) continue
    skip.add(k)
    out.push(t)
    if (out.length >= count) break
  }
  return out
}
