import { useRef, type ReactNode } from 'react'
import { ChevronLeft, ChevronRight, Play, Pause, UserRound } from '../icons'
import type { Album, Artist, RemotePlaylist, Track } from '../types'
import type { Mix } from '../recommend'
import { hoverCancel, hoverIntent, useApp } from '../ui'
import { Cover } from './Cover'

/** Fila horizontal con flechas, como las estanterías del inicio. */
export function Shelf({ title, subtitle, icon, children }: {
  title: string; subtitle?: string; icon?: ReactNode; children: ReactNode
}) {
  const row = useRef<HTMLDivElement>(null)
  const move = (dir: 1 | -1) => row.current?.scrollBy({ left: dir * row.current.clientWidth * 0.8, behavior: 'smooth' })
  return (
    <section className="shelf">
      <header className="shelfHead">
        <div>
          {subtitle && <small>{subtitle}</small>}
          <h2>{icon}{title}</h2>
        </div>
        <div className="shelfArrows">
          <button className="glassBtn" onClick={() => move(-1)}><ChevronLeft size={18} /></button>
          <button className="glassBtn" onClick={() => move(1)}><ChevronRight size={18} /></button>
        </div>
      </header>
      <div className="shelfRow" ref={row}>{children}</div>
    </section>
  )
}

export function TrackCard({ track, context }: { track: Track; context: Track[] }) {
  const app = useApp()
  const active = app.current?.id === track.id
  return (
    <div className={`card ${active ? 'active' : ''}`} onDoubleClick={() => app.activate(track, context)}
      onPointerEnter={() => hoverIntent(app, track)} onPointerLeave={hoverCancel}>
      <div className="cardArt">
        <Cover src={track.coverUrl} size={180} />
        <button
          className="cardPlay"
          onClick={() => (active ? app.toggle() : app.play(track, context))}
          title="Reproducir"
        >
          {active && app.playing ? <Pause size={18} fill="currentColor" /> : <Play size={18} fill="currentColor" />}
        </button>
      </div>
      <strong title={track.title}>{track.title}</strong>
      <button
        className="link"
        onClick={() => track.artistId && app.navigate({ name: 'artist', id: track.artistId, title: track.artists })}
      >{track.artists}</button>
    </div>
  )
}

export function AlbumCard({ album }: { album: Album }) {
  const app = useApp()
  return (
    <div className="card" onClick={() => app.navigate({ name: 'album', id: album.id })} role="button">
      <div className="cardArt"><Cover src={album.coverUrl} size={180} /></div>
      <strong title={album.title}>{album.title}</strong>
      <span>{[album.year, album.artist].filter(Boolean).join(' · ')}</span>
    </div>
  )
}

export function ArtistCard({ artist }: { artist: Artist }) {
  const app = useApp()
  return (
    <button className="card artistCard" onClick={() => app.navigate({ name: 'artist', id: artist.id, title: artist.name })}>
      <div className="cardArt round">
        {artist.picture ? <Cover src={artist.picture} size={180} /> : <div className="cover initials"><UserRound size={40} /></div>}
      </div>
      <strong title={artist.name}>{artist.name}</strong>
      <span>Artista</span>
    </button>
  )
}

/** Mezcla generada: portada con la foto del artista y un velo de color. */
export function MixCard({ mix }: { mix: Mix }) {
  const app = useApp()
  const active = !!app.current && mix.tracks.some((t) => t.id === app.current!.id)
  return (
    <div className="card mixCard" onDoubleClick={() => app.play(mix.tracks[0], mix.tracks)}
      onPointerEnter={() => hoverIntent(app, mix.tracks[0])} onPointerLeave={hoverCancel}>
      <div className="cardArt">
        <Cover src={mix.cover} size={180} />
        <span className="mixShade" />
        <span className="mixLabel">{mix.title.replace('Mezcla de ', '')}</span>
        <button className="cardPlay" title="Reproducir mezcla"
          onClick={() => (active ? app.toggle() : app.play(mix.tracks[0], mix.tracks))}>
          {active && app.playing ? <Pause size={18} fill="currentColor" /> : <Play size={18} fill="currentColor" />}
        </button>
      </div>
      <strong title={mix.title}>{mix.title}</strong>
      <span title={mix.subtitle}>{mix.subtitle}</span>
    </div>
  )
}

export function PlaylistCard({ playlist }: { playlist: RemotePlaylist }) {
  const app = useApp()
  return (
    <button className="card" onClick={() => app.navigate({ name: 'remote', id: playlist.id })}>
      <div className="cardArt"><Cover src={playlist.coverUrl} size={180} /></div>
      <strong title={playlist.title}>{playlist.title}</strong>
      <span>{playlist.count} canciones</span>
    </button>
  )
}
