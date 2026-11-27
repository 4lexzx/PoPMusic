import { useEffect, useRef, useState } from 'react'
import {
  Play, Pause, Heart, MoreVertical, Radio, ListPlus, ListEnd, Disc3, UserRound, Trash2, Plus, LoaderCircle,
} from '../icons'
import type { Track } from '../types'
import { formatTime, hoverCancel, hoverIntent, useApp } from '../ui'
import { Cover } from './Cover'

interface Props {
  tracks: Track[]
  /** Muestra número de pista en vez de carátula (vista de álbum). */
  numbered?: boolean
  showAlbum?: boolean
  /** Si se pasa, el menú ofrece quitar la canción de esta playlist. */
  playlistId?: string
}

export default function TrackList({ tracks, numbered, showAlbum = true, playlistId }: Props) {
  const app = useApp()
  const [menu, setMenu] = useState<{ track: Track; x: number; y: number } | null>(null)

  return (
    <div className={`trackList ${numbered ? 'numbered' : ''} ${!showAlbum && !numbered ? 'compact' : ''}`}>
      {tracks.map((track, i) => {
        const active = app.current?.id === track.id
        const liked = app.isFavorite(track.id)
        return (
          <div
            key={`${track.id}-${i}`}
            className={`trackRow ${active ? 'active' : ''}`}
            style={{ '--i': Math.min(i, 14) } as React.CSSProperties}
            onDoubleClick={() => app.activate(track, tracks)}
            onPointerEnter={() => hoverIntent(app, track)}
            onPointerLeave={hoverCancel}
            onContextMenu={(e) => { e.preventDefault(); setMenu({ track, x: e.clientX, y: e.clientY }) }}
          >
            <div className="trackLead">
              {numbered
                ? <span className="trackNum">{i + 1}</span>
                : <Cover src={track.coverUrl} size={44} />}
              <button
                className="trackPlay"
                onClick={() => (active ? app.toggle() : app.play(track, tracks))}
                title={active && app.playing ? 'Pausar' : 'Reproducir'}
              >
                {active && app.resolving
                  ? <LoaderCircle className="spin" size={16} />
                  : active && app.playing
                    ? <Pause size={16} fill="currentColor" />
                    : <Play size={16} fill="currentColor" />}
              </button>
              {active && app.playing && !app.resolving && <span className="eq"><i /><i /><i /></span>}
            </div>
            <div className="trackMeta">
              <span className="trackTitle">
                {track.explicit && <b className="explicit">E</b>}
                {track.title}
              </span>
              {!numbered && (
                <button
                  className="trackArtist link"
                  onClick={() => track.artistId && app.navigate({ name: 'artist', id: track.artistId, title: track.artists })}
                >{track.artists}</button>
              )}
            </div>
            {showAlbum && !numbered && (
              <button
                className="trackAlbum link"
                onClick={() => track.album?.id && app.navigate({ name: 'album', id: track.album.id })}
              >{track.album?.title || ''}</button>
            )}
            <button className={`iconBtn like ${liked ? 'on' : ''}`} onClick={() => app.fav(track)} title="Me gusta">
              <Heart size={16} fill={liked ? 'currentColor' : 'none'} />
            </button>
            <span className="trackTime">{track.durationMs ? formatTime(track.durationMs) : ''}</span>
            <button
              className="iconBtn more"
              title="Más opciones"
              onClick={(e) => {
                const r = (e.currentTarget as HTMLElement).getBoundingClientRect()
                setMenu({ track, x: r.right, y: r.bottom })
              }}
            ><MoreVertical size={17} /></button>
          </div>
        )
      })}
      {menu && <TrackMenu {...menu} playlistId={playlistId} onClose={() => setMenu(null)} />}
    </div>
  )
}

export function TrackMenu({ track, x, y, playlistId, onClose }: {
  track: Track; x: number; y: number; playlistId?: string; onClose: () => void
}) {
  const app = useApp()
  const ref = useRef<HTMLDivElement>(null)
  const [pos, setPos] = useState({ left: x, top: y })
  const [sub, setSub] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (el) {
      const r = el.getBoundingClientRect()
      setPos({
        left: Math.max(8, Math.min(x - r.width, window.innerWidth - r.width - 8)),
        top: y + r.height > window.innerHeight - 8 ? Math.max(8, y - r.height) : y,
      })
    }
    const close = (e: MouseEvent) => { if (!ref.current?.contains(e.target as Node)) onClose() }
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    setTimeout(() => document.addEventListener('mousedown', close))
    document.addEventListener('keydown', esc)
    return () => { document.removeEventListener('mousedown', close); document.removeEventListener('keydown', esc) }
  }, [x, y, onClose])

  const run = (fn: () => void) => () => { fn(); onClose() }

  return (
    <div className="menu" ref={ref} style={pos}>
      <button onClick={run(() => app.playNext(track))}><ListPlus size={16} />Reproducir a continuación</button>
      <button onClick={run(() => app.addToQueue([track]))}><ListEnd size={16} />Añadir a la cola</button>
      <button onClick={run(() => app.startRadio(track))}><Radio size={16} />Iniciar radio</button>
      <button onClick={() => setSub(!sub)}><Plus size={16} />Añadir a playlist</button>
      {sub && (
        <div className="menuSub">
          {app.playlists.map((p) => (
            <button key={p.id} onClick={run(() => app.addToPlaylist(p.id, track))}>{p.name}</button>
          ))}
          <button className="accent" onClick={run(() => app.createPlaylist(`Mi playlist ${app.playlists.length + 1}`, track))}>
            Nueva playlist
          </button>
        </div>
      )}
      <hr />
      {track.album?.id && (
        <button onClick={run(() => app.navigate({ name: 'album', id: track.album!.id }))}><Disc3 size={16} />Ir al álbum</button>
      )}
      {track.artistId && (
        <button onClick={run(() => app.navigate({ name: 'artist', id: track.artistId!, title: track.artists }))}>
          <UserRound size={16} />Ir al artista
        </button>
      )}
      {playlistId && (
        <>
          <hr />
          <button className="danger" onClick={run(() => app.removeFromPlaylist(playlistId, track.id))}>
            <Trash2 size={16} />Quitar de esta playlist
          </button>
        </>
      )}
    </div>
  )
}
