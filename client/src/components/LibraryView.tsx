import { useMemo, useState } from 'react'
import { Plus, Search, LayoutGrid, List } from '../icons'
import type { Track } from '../types'
import type { Library } from '../store'
import { useApp } from '../ui'
import { Collage, LikedCover } from './Cover'

type Filter = 'all' | 'playlists' | 'collections'

export default function LibraryView({ library }: { library: Library }) {
  const app = useApp()
  const [filter, setFilter] = useState<Filter>('all')
  const [q, setQ] = useState('')
  const [layout, setLayout] = useState<'grid' | 'list'>(() =>
    (localStorage.getItem('popmusic-client-libview') as 'grid' | 'list') || 'grid')

  const items = useMemo(() => {
    const list: { key: string; title: string; sub: string; art: React.ReactNode; open: () => void; kind: Filter }[] = []
    list.push({
      key: 'liked', title: 'Me gusta', sub: count(library.favorites), kind: 'collections',
      art: <LikedCover />, open: () => app.navigate({ name: 'liked' }),
    })
    if (library.recent.length) list.push({
      key: 'recent', title: 'Escuchado hace poco', sub: count(library.recent), kind: 'collections',
      art: <Collage covers={covers(library.recent)} />, open: () => app.navigate({ name: 'recent' }),
    })
    for (const p of library.playlists) list.push({
      key: p.id, title: p.name, sub: `Playlist · ${count(p.tracks)}`, kind: 'playlists',
      art: <Collage covers={covers(p.tracks)} />, open: () => app.navigate({ name: 'playlist', id: p.id }),
    })
    const t = q.trim().toLowerCase()
    return list.filter((i) => (filter === 'all' || i.kind === filter) && (!t || i.title.toLowerCase().includes(t)))
  }, [library, filter, q, app])

  const setView = (v: 'grid' | 'list') => { setLayout(v); localStorage.setItem('popmusic-client-libview', v) }

  return (
    <div className="page library">
      <header className="pageHead">
        <h1 className="display">Biblioteca</h1>
      </header>
      <div className="toolbar">
        <div className="chips">
          {([['all', 'Todo'], ['playlists', 'Playlists'], ['collections', 'Colecciones']] as const).map(([k, l]) => (
            <button key={k} className={`chip ${filter === k ? 'on' : ''}`} onClick={() => setFilter(k)}>{l}</button>
          ))}
        </div>
        <div className="toolbarRight">
          <label className="miniSearch"><Search size={14} /><input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar en la biblioteca" /></label>
          <div className="segGroup">
            <button className={layout === 'grid' ? 'on' : ''} onClick={() => setView('grid')} title="Cuadrícula"><LayoutGrid size={15} /></button>
            <button className={layout === 'list' ? 'on' : ''} onClick={() => setView('list')} title="Lista"><List size={15} /></button>
          </div>
          <button className="btnPrimary small" onClick={() => {
            const id = app.createPlaylist(`Mi playlist ${library.playlists.length + 1}`)
            app.navigate({ name: 'playlist', id })
          }}><Plus size={15} />Crear playlist</button>
        </div>
      </div>

      <div className={layout === 'grid' ? 'cardGrid' : 'libList'}>
        {items.map((i, n) => (
          <button key={i.key} className={layout === 'grid' ? 'card' : 'libRow'} onClick={i.open}
            style={{ '--i': Math.min(n, 14) } as React.CSSProperties}>
            <div className="cardArt">{i.art}</div>
            <span className="libText"><strong>{i.title}</strong><span>{i.sub}</span></span>
          </button>
        ))}
      </div>
    </div>
  )
}

const count = (t: Track[]) => `${t.length} ${t.length === 1 ? 'canción' : 'canciones'}`
const covers = (t: Track[]) => t.map((x) => x.coverUrl)
