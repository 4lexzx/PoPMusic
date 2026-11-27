/**
 * Motor de recomendaciones del inicio. Solo música, con la misma lógica que
 * la app móvil (RecommendationsRepository):
 *
 * - El perfil sale de lo que GUARDAS: "Me gusta" y canciones de tus playlists
 *   (pesan más) y, en menor medida, lo que escuchaste hace poco.
 * - Secciones por artista: "Éxitos de X" (sus propias canciones) y "Porque
 *   escuchas X" (artistas parecidos, a un solo salto: nunca se encadenan
 *   relacionados de relacionados, que es lo que hacía derivar a otros estilos).
 * - Los géneros que aparecen son SOLO los tuyos (deducidos de tus artistas);
 *   no se rotan géneros genéricos ni tendencias mundiales.
 * - Sin límite: se va bajando por tus artistas, luego "Más de X" con canciones
 *   menos conocidas y los artistas parecidos a cada uno. Nunca repite canción.
 */
import type { Album, Artist, RemotePlaylist, Track } from './types'
import {
  GENRES, artistIdFor, dz, dzAlbum, dzArtistRadio, dzChart, dzRelated, dzTop, isDz, mainArtist, mapArtist,
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

export interface SeedArtist { name: string; weight: number; sample: Track }

/** Artistas que más tienes guardados (como `topArtists` del móvil). */
export function profile(favorites: Track[], recent: Track[], playlistSongs: Track[] = []): SeedArtist[] {
  const map = new Map<string, SeedArtist>()
  const add = (t: Track, w: number) => {
    const name = mainArtist(t)
    if (!name) return
    const key = name.toLowerCase()
    const cur = map.get(key)
    if (cur) cur.weight += w
    else map.set(key, { name, weight: w, sample: t })
  }
  favorites.slice(0, 400).forEach((t) => add(t, 3))
  playlistSongs.slice(0, 1500).forEach((t) => add(t, 2))
  recent.slice(0, 100).forEach((t, i) => add(t, 1.2 * (1 - i / 140)))
  return [...map.values()].sort((a, b) => b.weight - a.weight)
}

type Task = () => Promise<Section | null>

export class FeedEngine {
  private seen = new Set<string>()
  /** Secciones que ya están en pantalla (p. ej. del feed guardado): no se vuelven a pedir. */
  private shown = new Set<string>()
  private known: Set<string>
  private tasks: Task[] = []
  /** Artistas parecidos a tus artistas (un solo salto), por explorar. */
  private nearby: { id: string; name: string; via: string }[] = []
  private nearbySeen = new Set<string>()
  private seeds: SeedArtist[]
  private seedIds = new Map<string, string>()
  private country: string
  private cold: boolean
  private round = 0
  private genres: { id: number; name: string }[] | null = null

  constructor(favorites: Track[], recent: Track[], country: string, playlistSongs: Track[] = []) {
    this.seeds = profile(favorites, recent, playlistSongs).slice(0, 24)
    this.cold = this.seeds.length === 0
    this.country = country
    this.known = new Set([...favorites, ...playlistSongs].map(songKey))
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

  private async idOf(seed: SeedArtist): Promise<string | null> {
    const cached = this.seedIds.get(seed.name)
    if (cached) return cached
    const id = await artistIdFor(seed.sample)
    if (id) this.seedIds.set(seed.name, id)
    return id
  }

  private plan() {
    const s = this.seeds
    const t = this.tasks
    if (this.cold) {
      // Sin biblioteca todavía: tops y tendencias, y desde ahí artistas parecidos.
      t.push(() => this.countryTop(), () => this.trending(), () => this.featuredPlaylists())
      return
    }
    t.push(() => this.mixes(s.slice(0, 6)))
    // Por cada artista tuyo: sus éxitos y luego parecidos. Se alterna para
    // que el inicio no sea un bloque de un mismo artista.
    s.slice(0, 3).forEach((seed) => {
      t.push(() => this.hitsOf(seed))
      t.push(() => this.becauseYouListen(seed))
    })
    t.push(() => this.artistsYouMayLike(s.slice(0, 10)))
    t.push(() => this.yourGenres(0))
    s.slice(3, 10).forEach((seed) => {
      t.push(() => this.hitsOf(seed))
      t.push(() => this.becauseYouListen(seed))
    })
    t.push(() => this.countryTop())
    t.push(() => this.yourGenres(1))
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
    }
  }

  /**
   * Cuando se acaba el plan: más canciones de TUS artistas (más allá de sus
   * éxitos) y artistas parecidos a ellos. Siempre cerca de tu estilo.
   */
  private refill() {
    this.round++
    const s = this.seeds
    if (this.cold) {
      const g = GENRES[(this.round - 1) % GENRES.length]
      this.tasks.push(() => this.genreTop(g.id, g.name, `cold-${this.round}`))
      for (let i = 0; i < 2; i++) {
        const n = this.nearby.shift()
        if (n) this.tasks.push(() => this.discoverArtist(n))
      }
      return
    }
    // Una ronda: 3 artistas parecidos + "Más de" de 2 de tus artistas.
    for (let i = 0; i < 3; i++) {
      const n = this.nearby.shift()
      if (n) this.tasks.push(() => this.discoverArtist(n))
    }
    const a = s[(this.round * 2 - 2) % s.length]
    const b = s[(this.round * 2 - 1) % s.length]
    const page = Math.floor((this.round * 2) / Math.max(1, s.length)) + 1
    if (a) this.tasks.push(() => this.moreOf(a, page))
    if (b && b !== a) this.tasks.push(() => this.moreOf(b, page))
    // Si se acabaron los parecidos conocidos, se piden los de más artistas tuyos.
    if (this.nearby.length < 3) {
      const seed = s[(this.round + 9) % s.length]
      if (seed) this.tasks.push(() => this.loadNearby(seed))
    }
    if (this.round % 4 === 0) this.tasks.push(() => this.yourGenres(this.round))
  }

  private addNearby(artists: Artist[], via: string) {
    const ownIds = new Set(this.seedIds.values())
    for (const a of artists) {
      if (this.nearbySeen.has(a.id) || ownIds.has(a.id)) continue
      if (this.seeds.some((s) => s.name.toLowerCase() === a.name.toLowerCase())) continue
      this.nearbySeen.add(a.id)
      this.nearby.push({ id: a.id, name: a.name, via })
    }
  }

  // ---------------- generadores ----------------

  private async mixes(seeds: SeedArtist[]): Promise<Section | null> {
    if (this.shown.has('mixes')) return null
    const mixes = (await Promise.all(seeds.map(async (seed) => {
      const id = await this.idOf(seed)
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

  /** "Éxitos de X": las canciones más escuchadas de un artista tuyo. */
  private async hitsOf(seed: SeedArtist): Promise<Section | null> {
    const id = await this.idOf(seed)
    if (!id || this.shown.has(`hits-${id}`)) return null
    const tracks = this.take(await dzTop(id, 25), 14, false)
    return tracks.length >= 4 ? { kind: 'tracks', key: `hits-${id}`, title: `Éxitos de ${seed.name}`, subtitle: 'De tus artistas', tracks } : null
  }

  /** "Más de X": canciones menos conocidas del artista (siguiente página de su top). */
  private async moreOf(seed: SeedArtist, page: number): Promise<Section | null> {
    const id = await this.idOf(seed)
    if (!id || this.shown.has(`more-${id}-${page}`)) return null
    const tracks = this.take(await dzTop(id, 25, page * 25), 14, false)
    return tracks.length >= 4 ? { kind: 'tracks', key: `more-${id}-${page}`, title: `Más de ${seed.name}`, subtitle: 'Para seguir escuchando', tracks } : null
  }

  private async loadNearby(seed: SeedArtist): Promise<Section | null> {
    const id = await this.idOf(seed)
    if (!id) return null
    this.addNearby(await dzRelated(id, 10).catch(() => []), seed.name)
    return null
  }

  /** "Porque escuchas X": artistas parecidos a X (a un salto), mezclados. */
  private async becauseYouListen(seed: SeedArtist): Promise<Section | null> {
    const id = await this.idOf(seed)
    if (!id || this.shown.has(`byl-${id}`)) return null
    const related = await dzRelated(id, 12)
    this.addNearby(related.slice(4), seed.name)
    const tops = await Promise.all(related.slice(0, 4).map((a) => dzTop(a.id, 6).catch(() => [])))
    const tracks = this.take(interleave(tops.map(shuffle)), 16)
    if (tracks.length < 5) return null
    return { kind: 'tracks', key: `byl-${id}`, title: seed.name, subtitle: 'Porque escuchas', tracks }
  }

  private async artistsYouMayLike(seeds: SeedArtist[]): Promise<Section | null> {
    if (this.shown.has('aymml')) return null
    const score = new Map<string, { a: Artist; n: number }>()
    const lists = await Promise.all(seeds.map(async (s) => {
      const id = await this.idOf(s)
      return id ? dzRelated(id, 15).catch(() => []) : []
    }))
    lists.forEach((list) => list.forEach((a, i) => {
      const cur = score.get(a.id)
      const w = 1 - i / 20
      if (cur) cur.n += w
      else score.set(a.id, { a, n: w })
    }))
    const own = new Set(seeds.map((s) => s.name.toLowerCase()))
    const artists = [...score.values()]
      .filter((x) => !own.has(x.a.name.toLowerCase()))
      .sort((a, b) => b.n - a.n)
      .map((x) => x.a)
      .slice(0, 16)
    return artists.length >= 4 ? { kind: 'artists', key: 'aymml', title: 'Artistas que te pueden gustar', subtitle: 'Parecidos a los que escuchas', artists } : null
  }

  /** Tus géneros: los de los álbumes de tus artistas principales (no genéricos). */
  private async detectGenres(): Promise<{ id: number; name: string }[]> {
    if (this.genres) return this.genres
    const count = new Map<number, { id: number; name: string; n: number }>()
    await Promise.all(this.seeds.slice(0, 6).map(async (seed, i) => {
      try {
        let albumId = isDz(seed.sample.album?.id) ? seed.sample.album!.id : ''
        if (!albumId) {
          const id = await this.idOf(seed)
          const top = id ? await dzTop(id, 1) : []
          albumId = top[0]?.album?.id || ''
        }
        if (!albumId) return
        const album = await dzAlbum(albumId)
        const genre = (album.genre || '').toLowerCase()
        const known = GENRES.find((g) => g.name.toLowerCase() === genre)
          || GENRES.find((g) => genre && genre.includes(g.name.toLowerCase().split(' ')[0]))
        if (!known) return
        const cur = count.get(known.id)
        const w = 6 - i
        if (cur) cur.n += w
        else count.set(known.id, { ...known, n: w })
      } catch { /* sin género */ }
    }))
    this.genres = [...count.values()].sort((a, b) => b.n - a.n).slice(0, 3)
    return this.genres
  }

  private async yourGenres(turn: number): Promise<Section | null> {
    const genres = await this.detectGenres()
    if (!genres.length) return null
    const g = genres[turn % genres.length]
    return this.genreTop(g.id, g.name, `yg-${g.id}-${turn}`)
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
    this.addNearby(c.artists, 'tendencias')
    const tracks = this.take(c.tracks, 30, false)
    return tracks.length >= 5 ? { kind: 'tracks', key: 'trending', title: 'Tendencias', tracks } : null
  }

  private async featuredPlaylists(): Promise<Section | null> {
    if (this.shown.has('pl')) return null
    const c = await dzChart(0)
    return c.playlists.length ? { kind: 'playlists', key: 'pl', title: 'Listas destacadas', playlists: c.playlists } : null
  }

  private async genreTop(genreId: number, name: string, key: string): Promise<Section | null> {
    if (this.shown.has(key)) return null
    const c = await dzChart(genreId)
    if (this.cold) this.addNearby(c.artists.slice(0, 6), name)
    const tracks = this.take(shuffle(c.tracks), 18)
    return tracks.length >= 5 ? { kind: 'tracks', key, title: `Lo mejor de ${name}`, subtitle: 'Tu género', tracks } : null
  }

  private async discoverArtist(next: { id: string; name: string; via: string }): Promise<Section | null> {
    if (this.shown.has(`disc-${next.id}`)) return null
    const tracks = this.take(await dzTop(next.id, 15), 12)
    return tracks.length >= 4
      ? { kind: 'tracks', key: `disc-${next.id}`, title: next.name, subtitle: `Parecido a ${next.via}`, tracks }
      : null
  }
}

/**
 * Canciones parecidas para seguir sonando cuando se acaba la cola. Como la
 * radio del móvil: se queda en el estilo de la semilla (radio del artista y
 * sus parecidos más cercanos), sin repetir lo que ya sonó.
 */
export async function similarTo(track: Track, exclude: Track[], count = 25): Promise<Track[]> {
  const id = await artistIdFor(track)
  if (!id || !isDz(id)) return []
  const [radio, related] = await Promise.all([
    dzArtistRadio(id, 60).catch(() => []),
    dzRelated(id, 4).catch(() => []),
  ])
  const tops = await Promise.all(related.slice(0, 3).map((a) => dzTop(a.id, 5).catch(() => [])))
  const skip = new Set(exclude.map(songKey))
  skip.add(songKey(track))
  const perArtist = new Map<string, number>()
  const out: Track[] = []
  for (const t of interleave([shuffle(radio), ...tops])) {
    const k = songKey(t)
    const artist = mainArtist(t).toLowerCase()
    // Como en el móvil: no más de 3 del mismo artista por tanda.
    if (skip.has(k) || (perArtist.get(artist) || 0) >= 3) continue
    skip.add(k)
    perArtist.set(artist, (perArtist.get(artist) || 0) + 1)
    out.push(t)
    if (out.length >= count) break
  }
  return out
}
