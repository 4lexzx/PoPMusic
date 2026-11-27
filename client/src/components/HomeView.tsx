import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Play, Pause, Search, X, LoaderCircle, ArrowRight, Sparkles, TrendingUp, Disc3, ListMusic, UserRound, Shapes, Lightning, Fire,
} from '../icons'
import type { Track } from '../types'
import type { Playlist } from '../store'
import { FeedEngine, profile, type Section } from '../recommend'
import { hoverCancel, hoverIntent, useApp } from '../ui'
import { Collage, Cover, LikedCover } from './Cover'
import { AlbumCard, ArtistCard, MixCard, PlaylistCard, Shelf, TrackCard } from './Shelf'
import TrackList from './TrackList'
import { useCatalogSearch } from '../search'

interface Props {
  recent: Track[]
  favorites: Track[]
  playlists: Playlist[]
  country: string
}


/**
 * El feed se conserva entre visitas (como en una app nativa): volver al
 * inicio no lo recarga. Se rehace si cambian tus gustos o pasan 30 min.
 */
const FEED_TTL = 30 * 60 * 1000
let feedCache: { key: string; at: number; engine: FeedEngine; sections: Section[] } | null = null

/** El último feed se guarda en disco: al abrir la app se ve al instante. */
const DISK_KEY = 'popmusic-client-feed-v1'
function readDisk(): { key: string; at: number; sections: Section[] } | null {
  try { return JSON.parse(localStorage.getItem(DISK_KEY) || 'null') } catch { return null }
}
function writeDisk(key: string, sections: Section[]) {
  try { localStorage.setItem(DISK_KEY, JSON.stringify({ key, at: Date.now(), sections: sections.slice(0, 10) })) } catch { /* sin espacio */ }
}

export default function HomeView({ recent, favorites, playlists, country }: Props) {
  const app = useApp()
  const [query, setQuery] = useState('')
  const searching = query.trim().length >= 2

  // La clave cambia cuando cambian tus artistas principales, no con cada canción.
  const key = useMemo(
    () => `${country}|${profile(favorites, recent).slice(0, 6).map((s) => s.name).join(',')}`,
    [favorites, recent, country],
  )
  const fresh = !!feedCache && feedCache.key === key && Date.now() - feedCache.at < FEED_TTL
  const engine = useRef<FeedEngine | null>(null)
  const disk = useRef(fresh ? null : readDisk())
  if (!engine.current) {
    engine.current = fresh ? feedCache!.engine : new FeedEngine(favorites, recent, country)
    // Feed guardado de la sesión anterior: se muestra ya y no se repite.
    if (!fresh && disk.current?.key === key) engine.current.markSeen(disk.current.sections)
  }
  const [sections, setSections] = useState<Section[]>(
    fresh ? feedCache!.sections : disk.current?.key === key ? disk.current.sections : [],
  )
  const [loading, setLoading] = useState(false)
  const [retry, setRetry] = useState(0)
  const busy = useRef(false)
  const stalledAt = useRef(0)
  const sentinel = useRef<HTMLDivElement>(null)

  // Tus artistas principales cambiaron: se rehace el feed.
  useEffect(() => {
    if (feedCache && feedCache.key === key && feedCache.engine === engine.current) return
    if (feedCache && feedCache.key !== key) {
      engine.current = new FeedEngine(favorites, recent, country)
      setSections([])
    }
    feedCache = { key, at: Date.now(), engine: engine.current!, sections: [] }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  useEffect(() => {
    if (feedCache && feedCache.engine === engine.current) feedCache.sections = sections
    if (sections.length) writeDisk(key, sections)
  }, [sections, key])

  // Precarga en segundo plano lo más probable de escuchar: así suena al instante.
  useEffect(() => {
    const t = setTimeout(() => {
      recent.slice(0, 2).forEach((tr) => app.prefetch(tr, 'idle'))
      const mixes = sections.find((s) => s.kind === 'mixes')
      if (mixes?.kind === 'mixes') mixes.mixes.slice(0, 2).forEach((m) => m.tracks[0] && app.prefetch(m.tracks[0], 'idle'))
    }, 2500)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sections.length > 0])

  // La siguiente tanda se pide en cuanto se muestra la anterior: al bajar ya está lista.
  const ahead = useRef<{ engine: FeedEngine; job: Promise<Section[]> } | null>(null)
  const loadMore = useCallback(async () => {
    if (busy.current) return
    busy.current = true
    setLoading(true)
    const owner = engine.current!
    try {
      const ready = ahead.current?.engine === owner ? ahead.current.job : null
      ahead.current = null
      const next = await (ready || owner.next(2))
      ahead.current = { engine: owner, job: owner.next(2) }
      // Sin red o sin resultados: se deja de insistir unos segundos.
      if (!next.length) {
        stalledAt.current = Date.now()
        setTimeout(() => setRetry((r) => r + 1), 2600)
      }
      if (owner === engine.current) setSections((s) => [...s, ...next.filter((n) => !s.some((x) => x.key === n.key))])
    } finally {
      busy.current = false
      setLoading(false)
    }
  }, [])

  // Scroll infinito: cuando el final se acerca, se pide otra tanda.
  useEffect(() => {
    const el = sentinel.current
    if (!el) return
    const io = new IntersectionObserver((entries) => {
      if (entries.some((e) => e.isIntersecting)) void loadMore()
    }, { rootMargin: '900px 0px' })
    io.observe(el)
    return () => io.disconnect()
  }, [loadMore])
  // Si una tanda no llenó la pantalla, el sentinel sigue visible: se pide otra.
  useEffect(() => {
    if (loading || Date.now() - stalledAt.current < 2500) return
    const el = sentinel.current
    if (el && el.getBoundingClientRect().top < window.innerHeight + 900) void loadMore()
  }, [loading, sections.length, loadMore, retry])

  const quick = useMemo(() => {
    const items: { key: string; title: string; sub: string; art: React.ReactNode; onClick: () => void; track?: Track }[] = []
    if (favorites.length) items.push({ key: 'liked', title: 'Me gusta', sub: `${favorites.length} canciones`, art: <LikedCover size={52} />, onClick: () => app.navigate({ name: 'liked' }) })
    for (const p of playlists.slice(0, 3)) {
      items.push({ key: p.id, title: p.name, sub: `Playlist · ${p.tracks.length}`, art: <Collage covers={p.tracks.map((t) => t.coverUrl)} size={52} />, onClick: () => app.navigate({ name: 'playlist', id: p.id }) })
    }
    const seen = new Set<string>()
    for (const t of recent) {
      if (items.length >= 8) break
      if (seen.has(t.id)) continue
      seen.add(t.id)
      items.push({ key: t.id, title: t.title, sub: t.artists, art: <Cover src={t.coverUrl} size={52} />, onClick: () => app.play(t, recent), track: t })
    }
    return items
  }, [favorites.length, playlists, recent, app])

  return (
    <div className={`page home ${searching ? 'isSearching' : ''}`}>
      <header className="homeTop">
        <HomeSearch query={query} onQuery={setQuery} country={country} />
      </header>

      <div className="homeBody" aria-hidden={searching} onClick={() => searching && setQuery('')}>
        {quick.length > 0 && <h2 className="sectionTitle"><Lightning size={21} weight="fill" />Acceso rápido</h2>}

        {quick.length > 0 && (
          <div className="quickGrid">
            {quick.map((q, i) => {
              const active = q.track && app.current?.id === q.track.id
              return (
                <button key={q.key} className={`quickTile ${active ? 'active' : ''}`}
                  style={{ '--i': i } as React.CSSProperties}
                  onPointerEnter={() => hoverIntent(app, q.track)} onPointerLeave={hoverCancel}
                  onClick={() => (active ? app.toggle() : q.onClick())}>
                  {q.art}
                  <span className="quickText"><strong>{q.title}</strong><small>{q.sub}</small></span>
                  {q.track && (
                    <span className="quickPlay">
                      {active && app.playing ? <Pause size={15} fill="currentColor" /> : <Play size={15} fill="currentColor" />}
                    </span>
                  )}
                </button>
              )
            })}
          </div>
        )}

        {sections.map((s) => <FeedSection key={s.key} section={s} />)}

        <div ref={sentinel} className="feedSentinel">
          {sections.length ? (loading && <LoaderCircle className="spin" size={22} />) : <HomeSkeleton />}
        </div>
      </div>
    </div>
  )
}

const ICONS: Record<Section['kind'], React.ReactNode> = {
  tracks: <Sparkles size={20} weight="fill" />,
  mixes: <Shapes size={20} weight="fill" />,
  artists: <UserRound size={20} weight="fill" />,
  albums: <Disc3 size={20} weight="fill" />,
  playlists: <ListMusic size={20} weight="fill" />,
}

function FeedSection({ section: s }: { section: Section }) {
  const icon = s.key === 'trending' ? <Fire size={20} weight="fill" /> : s.key === 'country' ? <TrendingUp size={20} /> : ICONS[s.kind]
  return (
    <Shelf title={s.title} subtitle={s.subtitle} icon={icon}>
      {s.kind === 'tracks' && s.tracks.map((t) => <TrackCard key={t.id} track={t} context={s.tracks} />)}
      {s.kind === 'mixes' && s.mixes.map((m) => <MixCard key={m.id} mix={m} />)}
      {s.kind === 'artists' && s.artists.map((a) => <ArtistCard key={a.id} artist={a} />)}
      {s.kind === 'albums' && s.albums.map((a) => <AlbumCard key={a.id} album={a} />)}
      {s.kind === 'playlists' && s.playlists.map((p) => <PlaylistCard key={p.id} playlist={p} />)}
    </Shelf>
  )
}

/** Buscador del inicio: los resultados aparecen encima del inicio desenfocado. */
function HomeSearch({ query, onQuery, country }: { query: string; onQuery: (q: string) => void; country: string }) {
  const app = useApp()
  const input = useRef<HTMLInputElement>(null)
  const { data, loading, error } = useCatalogSearch(query, country)
  const open = query.trim().length >= 2
  const wrap = useRef<HTMLDivElement>(null)

  // Con la búsqueda abierta, la rueda no mueve el inicio que queda detrás.
  useEffect(() => {
    const scroller = wrap.current?.closest('.scroller') as HTMLElement | null
    if (!scroller) return
    scroller.classList.toggle('locked', open)
    return () => scroller.classList.remove('locked')
  }, [open])

  // El primer resultado es el más probable: se prepara su audio ya.
  const top = data.tracks[0]
  useEffect(() => { if (top) app.prefetch(top, 'hover') }, [top, app])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && open) { onQuery(''); input.current?.blur() }
      if (e.key === '/' && !(e.target instanceof Element && e.target.closest('input'))) { e.preventDefault(); input.current?.focus() }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onQuery])

  return (
    <div className={`homeSearch ${open ? 'open' : ''}`} ref={wrap}>
      <label className="searchField compact">
        <Search size={17} />
        <input
          ref={input}
          value={query}
          onChange={(e) => onQuery(e.target.value)}
          placeholder="Busca canciones, artistas o álbumes"
          spellCheck={false}
        />
        {loading && <LoaderCircle className="spin" size={16} />}
        {query && !loading && <button className="iconBtn" onClick={() => onQuery('')} title="Cerrar (Esc)"><X size={15} /></button>}
      </label>

      {open && (
        <div className="homeResults">
          {error && <div className="notice">{error}</div>}
          {!loading && !error && data.tracks.length === 0 && data.albums.length === 0 && (
            <div className="emptyState"><p>No encontramos música para “{query}”.</p></div>
          )}
          {data.artists.length > 0 && (
            <div className="resultArtists">
              {data.artists.slice(0, 6).map((a) => (
                <button key={a.id} className="chip artistChip" onClick={() => app.navigate({ name: 'artist', id: a.id, title: a.name })}>
                  {a.picture ? <img src={a.picture} alt="" /> : <UserRound size={14} />}{a.name}
                </button>
              ))}
            </div>
          )}
          {data.tracks.length > 0 && (
            <section>
              <h3 className="blockTitle">Canciones</h3>
              <TrackList tracks={data.tracks.slice(0, 6)} showAlbum={false} />
            </section>
          )}
          {data.albums.length > 0 && (
            <Shelf title="Álbumes">
              {data.albums.slice(0, 12).map((a) => <AlbumCard key={a.id} album={a} />)}
            </Shelf>
          )}
          {(data.tracks.length > 0 || data.albums.length > 0) && (
            <button className="btnGlass seeAll" onClick={() => app.navigate({ name: 'search', q: query })}>
              Ver todos los resultados <ArrowRight size={15} />
            </button>
          )}
        </div>
      )}
    </div>
  )
}

function HomeSkeleton() {
  return (
    <div className="skeleton">
      {[0, 1].map((r) => (
        <div key={r} className="skBlock">
          <div className="skTitle" />
          <div className="skRow">{Array.from({ length: 7 }, (_, i) => <div key={i} className="skCard" />)}</div>
        </div>
      ))}
    </div>
  )
}
