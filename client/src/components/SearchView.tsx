import { useEffect, useRef, useState } from 'react'
import { Search, X, LoaderCircle, Play, Clock3 } from '../icons'
import { RECENT_KEY, loadSearchHistory, useCatalogSearch } from '../search'
import { useApp } from '../ui'
import { Cover } from './Cover'
import TrackList from './TrackList'
import { AlbumCard, Shelf } from './Shelf'

type Filter = 'all' | 'tracks' | 'albums' | 'artists'

const GENRES = [
  { name: 'Pop', q: 'pop hits', c: ['#4a3a55', '#2a2533'] },
  { name: 'Reggaetón', q: 'reggaeton', c: ['#54442e', '#2e2620'] },
  { name: 'Rock', q: 'rock classics', c: ['#4a2f2f', '#24201f'] },
  { name: 'Hip-Hop', q: 'hip hop', c: ['#2f4a40', '#1f2624'] },
  { name: 'Indie', q: 'indie', c: ['#34405a', '#20232c'] },
  { name: 'Electrónica', q: 'electronic dance', c: ['#2c4650', '#1e2328'] },
  { name: 'Salsa', q: 'salsa', c: ['#553a33', '#2a2120'] },
  { name: 'K-pop', q: 'k-pop', c: ['#4d3a4f', '#262129'] },
  { name: 'Baladas', q: 'baladas romanticas', c: ['#4a3940', '#252024'] },
  { name: 'Regional', q: 'regional mexicano', c: ['#4a4430', '#25241e'] },
]

export default function SearchView({ country, initial = '' }: { country: string; initial?: string }) {
  const app = useApp()
  const input = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState(initial)
  const [filter, setFilter] = useState<Filter>('all')
  const { data, loading, error, artistCover } = useCatalogSearch(query, country)
  // El primer resultado es el más probable: se prepara su audio ya.
  const first = data.tracks[0]
  useEffect(() => { if (first) app.prefetch(first, 'hover') }, [first, app])
  const [history, setHistory] = useState<string[]>(loadSearchHistory)

  useEffect(() => { input.current?.focus() }, [])
  useEffect(() => { if (!loading) setHistory(loadSearchHistory()) }, [loading])

  const top = data.tracks[0]
  const hasResults = data.tracks.length + data.albums.length + data.artists.length > 0

  return (
    <div className="page search">
      <div className="searchHero">
        <div className="searchField">
          <Search size={19} />
          <input
            ref={input}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="¿Qué quieres escuchar?"
            spellCheck={false}
          />
          {loading && <LoaderCircle className="spin" size={17} />}
          {query && !loading && <button className="iconBtn" onClick={() => { setQuery(''); input.current?.focus() }}><X size={16} /></button>}
        </div>
        {query.trim().length >= 2 && (
          <div className="chips">
            {([['all', 'Todo'], ['tracks', 'Canciones'], ['albums', 'Álbumes'], ['artists', 'Artistas']] as const).map(([k, label]) => (
              <button key={k} className={`chip ${filter === k ? 'on' : ''}`} onClick={() => setFilter(k)}>{label}</button>
            ))}
          </div>
        )}
      </div>

      {error && <div className="notice">{error}</div>}

      {query.trim().length < 2 && (
        <>
          {history.length > 0 && (
            <section className="block">
              <h3 className="blockTitle">Búsquedas recientes</h3>
              <div className="chips">
                {history.map((h) => (
                  <button key={h} className="chip" onClick={() => setQuery(h)}><Clock3 size={13} />{h}</button>
                ))}
                <button className="chip ghost" onClick={() => { setHistory([]); localStorage.removeItem(RECENT_KEY) }}>Borrar</button>
              </div>
            </section>
          )}
          <section className="block">
            <h3 className="blockTitle">Explorar géneros</h3>
            <div className="genreGrid">
              {GENRES.map((g, i) => (
                <button
                  key={g.name}
                  className="genreTile"
                  style={{ background: `linear-gradient(135deg, ${g.c[0]}, ${g.c[1]})`, '--i': i } as React.CSSProperties}
                  onClick={() => { setQuery(g.q); setFilter('tracks') }}
                >{g.name}</button>
              ))}
            </div>
          </section>
        </>
      )}

      {query.trim().length >= 2 && !loading && !hasResults && !error && (
        <div className="emptyState"><Search size={30} /><p>No encontramos música para “{query}”.</p></div>
      )}

      {hasResults && filter === 'all' && (
        <>
          <div className="topGrid">
            {top && (
              <section className="block">
                <h3 className="blockTitle">Mejor resultado</h3>
                <div className="topCard" onDoubleClick={() => app.activate(top, data.tracks)}>
                  <Cover src={top.coverUrl} size={112} />
                  <h2>{top.title}</h2>
                  <p><span className="tag">Canción</span>{top.artists}</p>
                  <button className="topPlay" onClick={() => app.play(top, data.tracks)} title="Reproducir">
                    <Play size={20} fill="currentColor" />
                  </button>
                </div>
              </section>
            )}
            <section className="block">
              <h3 className="blockTitle">Canciones</h3>
              <TrackList tracks={data.tracks.slice(0, 5)} showAlbum={false} />
            </section>
          </div>
          {data.artists.length > 0 && (
            <Shelf title="Artistas">
              {data.artists.slice(0, 12).map((a) => (
                <ArtistBubble key={a.id} id={a.id} name={a.name} cover={a.picture || artistCover(a.name)} />
              ))}
            </Shelf>
          )}
          {data.albums.length > 0 && (
            <Shelf title="Álbumes">
              {data.albums.map((a) => <AlbumCard key={a.id} album={a} />)}
            </Shelf>
          )}
        </>
      )}

      {hasResults && filter === 'tracks' && <TrackList tracks={data.tracks} />}
      {hasResults && filter === 'albums' && (
        <div className="cardGrid">{data.albums.map((a) => <AlbumCard key={a.id} album={a} />)}</div>
      )}
      {hasResults && filter === 'artists' && (
        <div className="cardGrid">
          {data.artists.map((a) => <ArtistBubble key={a.id} id={a.id} name={a.name} cover={a.picture || artistCover(a.name)} />)}
        </div>
      )}
    </div>
  )
}

function ArtistBubble({ id, name, cover }: { id: string; name: string; cover?: string }) {
  const app = useApp()
  return (
    <button className="card artistCard" onClick={() => app.navigate({ name: 'artist', id, title: name })}>
      <div className="cardArt round">
        {cover ? <Cover src={cover} size={180} /> : <div className="cover initials">{name.slice(0, 1)}</div>}
      </div>
      <strong>{name}</strong>
      <span>Artista</span>
    </button>
  )
}
