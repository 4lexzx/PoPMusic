import { useEffect, useRef, useState } from 'react'
import { Home, Search, Library, Settings as Gear, Minus, Square, Copy, X } from '../icons'
import type { Track } from '../types'
import type { Playlist } from '../store'
import type { Route } from '../ui'
import { useApp } from '../ui'
import { Collage, LikedCover } from './Cover'
import { Logo } from './Logo'

const isElectron = !!window.popApi?.windowAction
if (isElectron) document.documentElement.classList.add('electron')

/** Controles de ventana propios (la ventana no tiene marco del sistema). */
export function WindowControls() {
  const [max, setMax] = useState(false)
  const api = window.popApi
  useEffect(() => api?.onWindowState?.((s) => setMax(s.maximized)), [api])
  useEffect(() => { document.documentElement.classList.toggle('maximized', max) }, [max])
  if (!api?.windowAction) return null
  return (
    <div className="winControls">
      <button onClick={() => api.windowAction!('minimize')} title="Minimizar"><Minus size={14} /></button>
      <button onClick={() => api.windowAction!('maximize')} title={max ? 'Restaurar' : 'Maximizar'}>
        {max ? <Copy size={13} /> : <Square size={12} />}
      </button>
      <button className="close" onClick={() => api.windowAction!('close')} title="Cerrar"><X size={14} /></button>
    </div>
  )
}

type Edge = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw'
const EDGES: Edge[] = ['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw']

/**
 * Bordes para redimensionar: la ventana es transparente (para tener esquinas
 * redondeadas en Windows 10) y así no tiene borde nativo.
 */
export function ResizeEdges() {
  const drag = useRef<{ edge: Edge; x: number; y: number; b: { x: number; y: number; width: number; height: number } } | null>(null)
  const frame = useRef(0)
  if (!window.popApi?.getBounds) return null

  const down = async (edge: Edge, e: React.PointerEvent) => {
    if (document.documentElement.classList.contains('maximized')) return
    const target = e.currentTarget as HTMLElement
    target.setPointerCapture(e.pointerId)
    const b = await window.popApi!.getBounds!()
    drag.current = { edge, x: e.screenX, y: e.screenY, b }
  }
  const move = (e: React.PointerEvent) => {
    const d = drag.current
    if (!d) return
    const dx = e.screenX - d.x
    const dy = e.screenY - d.y
    const b = { ...d.b }
    if (d.edge.includes('e')) b.width = d.b.width + dx
    if (d.edge.includes('s')) b.height = d.b.height + dy
    if (d.edge.includes('w')) { b.x = d.b.x + dx; b.width = d.b.width - dx }
    if (d.edge.includes('n')) { b.y = d.b.y + dy; b.height = d.b.height - dy }
    cancelAnimationFrame(frame.current)
    frame.current = requestAnimationFrame(() => window.popApi!.setBounds!(b))
  }
  const up = () => { drag.current = null }

  return (
    <>
      {EDGES.map((edge) => (
        <div key={edge} className={`resize r-${edge}`} onPointerDown={(e) => void down(edge, e)} onPointerMove={move} onPointerUp={up} />
      ))}
    </>
  )
}

export function Sidebar({ route, favorites, playlists }: { route: Route; favorites: Track[]; playlists: Playlist[] }) {
  const app = useApp()
  const is = (...names: Route['name'][]) => names.includes(route.name)
  const items: [Route['name'], string, typeof Home, boolean][] = [
    ['home', 'Inicio', Home, is('home')],
    ['search', 'Buscar', Search, is('search')],
    ['library', 'Biblioteca', Library, is('library', 'playlist', 'liked', 'recent')],
  ]
  return (
    <aside className="rail">
      <button className="brand" title="PoP Music" onClick={() => app.navigate({ name: 'home' })}><Logo height={20} /></button>
      <nav>
        {items.map(([name, label, Icon, on]) => (
          <button key={name} className={`railBtn ${on ? 'on' : ''}`} onClick={() => app.navigate({ name } as Route)} title={label}>
            <Icon size={23} weight={on ? 'fill' : 'bold'} />
          </button>
        ))}
      </nav>
      {(favorites.length > 0 || playlists.length > 0) && (
        <div className="railPins">
          <button className={`pin ${is('liked') ? 'on' : ''}`} onClick={() => app.navigate({ name: 'liked' })}
            title={`Me gusta · ${favorites.length}`}><LikedCover size={40} /></button>
          {playlists.slice(0, 6).map((p) => (
            <button key={p.id} className={`pin ${route.name === 'playlist' && route.id === p.id ? 'on' : ''}`}
              onClick={() => app.navigate({ name: 'playlist', id: p.id })} title={p.name}>
              <Collage covers={p.tracks.map((t) => t.coverUrl)} size={40} />
            </button>
          ))}
        </div>
      )}
      <button className="railBtn gear" onClick={() => app.navigate({ name: 'settings' })} title="Ajustes">
        <Gear size={23} />
      </button>
    </aside>
  )
}

export function QueuePanel({ tracks, index, onPlay, onRemove, onClear, onClose }: {
  tracks: Track[]; index: number; onPlay: (i: number) => void; onRemove: (i: number) => void
  onClear: () => void; onClose: () => void
}) {
  const app = useApp()
  const now = tracks[index]
  const next = tracks.slice(index + 1)
  return (
    <aside className="queuePanel">
      <header>
        <h3>Cola</h3>
        <div>
          {next.length > 0 && <button className="textBtn" onClick={onClear}>Vaciar</button>}
          <button className="iconBtn" onClick={onClose}><X size={17} /></button>
        </div>
      </header>
      <div className="queueBody">
        {now && (
          <>
            <small className="qLabel">Sonando</small>
            <QueueItem track={now} active onClick={() => app.toggle()} />
          </>
        )}
        <small className="qLabel">A continuación · {next.length}</small>
        {next.map((t, i) => (
          <QueueItem key={`${t.id}-${i}`} track={t} onClick={() => onPlay(index + 1 + i)} onRemove={() => onRemove(index + 1 + i)} />
        ))}
        {next.length === 0 && <p className="qEmpty">Nada más en la cola. Usa “Añadir a la cola” en cualquier canción.</p>}
      </div>
    </aside>
  )
}

function QueueItem({ track, active, onClick, onRemove }: { track: Track; active?: boolean; onClick: () => void; onRemove?: () => void }) {
  const app = useApp()
  return (
    <div className={`qItem ${active ? 'active' : ''}`} onClick={onClick} role="button">
      <img src={track.coverUrl} alt="" />
      <span><strong>{track.title}</strong><small>{track.artists}</small></span>
      {active && app.playing && <span className="eq"><i /><i /><i /></span>}
      {onRemove && <button className="iconBtn" onClick={(e) => { e.stopPropagation(); onRemove() }} title="Quitar"><X size={14} /></button>}
    </div>
  )
}

export function Toasts({ items }: { items: { id: number; title: string; detail?: string }[] }) {
  return (
    <div className="toasts">
      {items.map((t) => (
        <div key={t.id} className="toast"><strong>{t.title}</strong>{t.detail && <small>{t.detail}</small>}</div>
      ))}
    </div>
  )
}
