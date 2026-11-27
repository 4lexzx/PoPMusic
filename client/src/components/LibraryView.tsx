import { useEffect, useMemo, useRef, useState } from 'react'
import { Plus, Search, LayoutGrid, List, X, Info, LoaderCircle, Music2 } from '../icons'
import type { Track } from '../types'
import type { Library } from '../store'
import { useApp } from '../ui'
import { IMPORT_SOURCES, importFromLink } from '../importer'
import { Collage, Cover, LikedCover } from './Cover'

type Tab = 'lists' | 'albums' | 'artists'

interface Item { key: string; title: string; sub: string; art: React.ReactNode; open: () => void; round?: boolean }

export default function LibraryView({ library }: { library: Library }) {
  const app = useApp()
  const [tab, setTab] = useState<Tab>(() => (localStorage.getItem('popmusic-client-libtab') as Tab) || 'lists')
  const [q, setQ] = useState('')
  const [creating, setCreating] = useState(false)
  const [layout, setLayout] = useState<'grid' | 'list'>(() =>
    (localStorage.getItem('popmusic-client-libview') as 'grid' | 'list') || 'grid')

  const items = useMemo(() => {
    const list: Item[] = []
    if (tab === 'lists') {
      list.push({
        key: 'liked', title: 'Canciones que te gustan', sub: count(library.favorites),
        art: <LikedCover />, open: () => app.navigate({ name: 'liked' }),
      })
      for (const p of library.playlists) list.push({
        key: p.id, title: p.name, sub: count(p.tracks),
        art: <Collage covers={covers(p.tracks)} />, open: () => app.navigate({ name: 'playlist', id: p.id }),
      })
      if (library.recent.length) list.push({
        key: 'recent', title: 'Escuchado hace poco', sub: count(library.recent),
        art: <Collage covers={covers(library.recent)} />, open: () => app.navigate({ name: 'recent' }),
      })
    } else if (tab === 'albums') {
      for (const a of library.albums || []) list.push({
        key: a.id, title: a.title, sub: [a.artist, a.year].filter(Boolean).join(' · '),
        art: <Cover src={a.coverUrl} />, open: () => app.navigate({ name: 'album', id: a.id }),
      })
    } else {
      for (const a of library.artists || []) list.push({
        key: a.id, title: a.name, sub: 'Artista', round: true,
        art: a.picture ? <Cover src={a.picture} className="round" /> : <div className="cover initials round">{a.name.slice(0, 1)}</div>,
        open: () => app.navigate({ name: 'artist', id: a.id, title: a.name }),
      })
    }
    const t = q.trim().toLowerCase()
    return t ? list.filter((i) => `${i.title} ${i.sub}`.toLowerCase().includes(t)) : list
  }, [library, tab, q, app])

  const setView = (v: 'grid' | 'list') => { setLayout(v); localStorage.setItem('popmusic-client-libview', v) }
  const pick = (t: Tab) => { setTab(t); setQ(''); localStorage.setItem('popmusic-client-libtab', t) }
  const placeholder = tab === 'lists' ? 'Buscar listas' : tab === 'albums' ? 'Buscar álbumes' : 'Buscar artistas'

  return (
    <div className="page library">
      <header className="pageHead"><h1 className="display">Biblioteca</h1></header>
      <div className="toolbar">
        <div className="segGroup labeled libTabs">
          {([['lists', 'Listas'], ['albums', 'Álbumes'], ['artists', 'Artistas']] as const).map(([k, l]) => (
            <button key={k} className={tab === k ? 'on' : ''} onClick={() => pick(k)}>{l}</button>
          ))}
        </div>
        <div className="toolbarRight">
          <label className="miniSearch"><Search size={14} /><input value={q} onChange={(e) => setQ(e.target.value)} placeholder={placeholder} /></label>
          <div className="segGroup">
            <button className={layout === 'grid' ? 'on' : ''} onClick={() => setView('grid')} title="Cuadrícula"><LayoutGrid size={15} /></button>
            <button className={layout === 'list' ? 'on' : ''} onClick={() => setView('list')} title="Lista"><List size={15} /></button>
          </div>
        </div>
      </div>

      {items.length === 0 && (
        <div className="emptyState">
          <Music2 size={30} />
          <p>{q ? 'Nada coincide con tu búsqueda.'
            : tab === 'albums' ? 'Guarda álbumes con el corazón de su página.'
              : tab === 'artists' ? 'Sigue a tus artistas desde su página.' : 'Crea o importa tu primera lista.'}</p>
        </div>
      )}

      <div className={layout === 'grid' ? 'cardGrid libGrid' : 'libList'}>
        {items.map((i, n) => (
          <button key={i.key} className={`${layout === 'grid' ? 'card' : 'libRow'} ${i.round ? 'artistCard' : ''}`} onClick={i.open}
            style={{ '--i': Math.min(n, 14) } as React.CSSProperties}>
            <div className={`cardArt ${i.round ? 'round' : ''}`}>{i.art}</div>
            <span className="libText"><strong>{i.title}</strong><span>{i.sub}</span></span>
          </button>
        ))}
      </div>

      <button className="fab" onClick={() => setCreating(true)} title="Nueva lista"><Plus size={22} /></button>
      {creating && <CreatePlaylistDialog onClose={() => setCreating(false)} />}
    </div>
  )
}

/** "Nueva lista": desde cero o importando un enlace de cualquier plataforma. */
export function CreatePlaylistDialog({ onClose, initialMode = 'new' }: { onClose: () => void; initialMode?: 'new' | 'import' }) {
  const app = useApp()
  const [mode, setMode] = useState<'new' | 'import'>(initialMode)
  const [name, setName] = useState('')
  const [link, setLink] = useState('')
  const [error, setError] = useState('')
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null)
  const [busy, setBusy] = useState(false)
  const [closing, setClosing] = useState(false)
  const input = useRef<HTMLInputElement>(null)
  const close = () => { if (busy) return; setClosing(true); setTimeout(onClose, 160) }

  useEffect(() => { input.current?.focus() }, [mode])
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  })

  const submit = async () => {
    setError('')
    if (mode === 'new') {
      const id = app.createPlaylist(name.trim() || 'Nueva lista')
      onClose()
      app.navigate({ name: 'playlist', id })
      return
    }
    if (!link.trim()) { setError('Pega un enlace de una lista.'); return }
    setBusy(true)
    try {
      const res = await importFromLink(link.trim(), (done, total) => setProgress({ done, total }))
      if (!res.tracks.length) throw new Error('No se encontraron canciones en esa lista')
      const id = app.createPlaylist(res.name, res.tracks, res.source)
      if (res.missing) app.toast(`${res.tracks.length - res.missing} canciones con carátula del catálogo`, `${res.missing} se guardaron tal cual`)
      onClose()
      app.navigate({ name: 'playlist', id })
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e)
      // Mensajes técnicos (JSON, red) → uno que se entienda.
      setError(/json|token|fetch|network|http|status/i.test(msg) ? 'No se pudo leer esa lista. Revisa el enlace y que la lista sea pública.' : msg)
    } finally {
      setBusy(false)
      setProgress(null)
    }
  }

  const pct = progress && progress.total ? Math.round((progress.done / progress.total) * 100) : 0

  return (
    <div className={`modalLayer ${closing ? 'closing' : ''}`} onMouseDown={(e) => { if (e.target === e.currentTarget) close() }}>
      <div className="dialog" role="dialog" aria-label="Nueva lista">
        <header>
          <div>
            <h2>{mode === 'new' ? 'Nueva lista de reproducción' : 'Importar lista de reproducción'}</h2>
            <p>{mode === 'new' ? 'Ponle un nombre y añade canciones cuando quieras.' : 'Importa una lista existente desde cualquier plataforma.'}</p>
          </div>
          <button className="iconBtn" onClick={close} disabled={busy}><X size={16} /></button>
        </header>
        <div className="segGroup labeled wide">
          <button className={mode === 'new' ? 'on' : ''} onClick={() => { setMode('new'); setError('') }} disabled={busy}>Desde cero</button>
          <button className={mode === 'import' ? 'on' : ''} onClick={() => { setMode('import'); setError('') }} disabled={busy}>Importar</button>
        </div>
        {mode === 'new' ? (
          <label className="field">
            <Music2 size={15} />
            <input ref={input} value={name} onChange={(e) => setName(e.target.value)} placeholder="Nombre de la lista"
              onKeyDown={(e) => e.key === 'Enter' && submit()} maxLength={80} />
          </label>
        ) : (
          <>
            <label className="field">
              <Plus size={15} />
              <input ref={input} value={link} onChange={(e) => setLink(e.target.value)} placeholder="Pega un enlace…" disabled={busy}
                onKeyDown={(e) => e.key === 'Enter' && submit()} spellCheck={false} />
              <span className="fieldInfo" tabIndex={0}>
                <Info size={15} />
                <span className="tip">Funciona con listas, álbumes y canciones de {IMPORT_SOURCES.join(', ')}. La lista debe ser pública.</span>
              </span>
            </label>
            {progress && (
              <div className="importProgress">
                <div className="bar"><i style={{ width: `${Math.max(4, pct)}%` }} /></div>
                <span>{progress.total > 1 ? `Buscando canciones… ${progress.done} de ${progress.total}` : 'Leyendo la lista…'}</span>
              </div>
            )}
            {busy && !progress && <div className="importProgress"><span><LoaderCircle className="spin" size={14} /> Leyendo la lista…</span></div>}
          </>
        )}
        {error && <div className="dialogError">{error}</div>}
        <footer>
          <button className="textBtn" onClick={close} disabled={busy}>Cancelar</button>
          <button className="btnPrimary" onClick={submit} disabled={busy}>
            {busy ? <LoaderCircle className="spin" size={15} /> : null}{mode === 'new' ? 'Crear' : 'Importar'}
          </button>
        </footer>
      </div>
    </div>
  )
}

const count = (t: Track[]) => `${t.length} ${t.length === 1 ? 'canción' : 'canciones'}`
const covers = (t: Track[]) => t.map((x) => x.coverUrl)
