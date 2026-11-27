import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Play, Shuffle, Search, ListEnd, Music2, Pencil, Trash2, LoaderCircle, Check, Radio as RadioIcon } from '../icons'
import type { Album, Artist, RemotePlaylist, Track } from '../types'
import { getAlbum, getArtist } from '../api'
import { dzPlaylist } from '../catalog'
import { formatLength, useApp } from '../ui'
import type { Playlist } from '../store'
import { Collage, Cover, LikedCover } from './Cover'
import TrackList from './TrackList'
import { AlbumCard, ArtistCard, Shelf } from './Shelf'

/** Cabecera común: portada 168, metadatos y acciones. */
function EntityHeader({ art, kind, title, subtitle, onSubtitle, chips, tracks, extra, backdrop }: {
  art: ReactNode; kind: string; title: string; subtitle?: string; onSubtitle?: () => void
  chips: string[]; tracks: Track[]; extra?: ReactNode; backdrop?: string
}) {
  const app = useApp()
  const shuffled = () => {
    const copy = [...tracks]
    for (let i = copy.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [copy[i], copy[j]] = [copy[j], copy[i]]
    }
    return copy
  }
  return (
    <header className="entityHead">
      {backdrop && <div className="entityBackdrop" style={{ backgroundImage: `url(${backdrop})` }} />}
      <div className="entityArt">{art}</div>
      <div className="entityInfo">
        <small className="kind">{kind}</small>
        <h1>{title}</h1>
        {subtitle && (onSubtitle
          ? <button className="link entitySub" onClick={onSubtitle}>{subtitle}</button>
          : <span className="entitySub">{subtitle}</span>)}
        <div className="metaChips">{chips.filter(Boolean).map((c) => <span key={c}>{c}</span>)}</div>
        <div className="entityActions">
          <button className="btnPrimary" disabled={!tracks.length} onClick={() => app.play(tracks[0], tracks)}>
            <Play size={16} fill="currentColor" />Reproducir
          </button>
          <button className="btnGlass square" disabled={!tracks.length} title="Aleatorio" onClick={() => {
            const s = shuffled(); app.play(s[0], s)
          }}><Shuffle size={17} /></button>
          <button className="btnGlass" disabled={!tracks.length} onClick={() => app.addToQueue(tracks)}>
            <ListEnd size={16} />Añadir a la cola
          </button>
          {extra}
        </div>
      </div>
    </header>
  )
}

function FilterableList({ tracks, numbered, playlistId, empty }: {
  tracks: Track[]; numbered?: boolean; playlistId?: string; empty: string
}) {
  const [q, setQ] = useState('')
  const shown = useMemo(() => {
    const t = q.trim().toLowerCase()
    if (!t) return tracks
    return tracks.filter((x) => `${x.title} ${x.artists} ${x.album?.title || ''}`.toLowerCase().includes(t))
  }, [q, tracks])
  return (
    <div className="listPanel">
      {tracks.length > 6 && (
        <div className="listTools">
          <label className="miniSearch"><Search size={14} /><input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar en la lista" /></label>
        </div>
      )}
      {tracks.length === 0
        ? <div className="emptyState"><Music2 size={28} /><p>{empty}</p></div>
        : <TrackList tracks={shown} numbered={numbered && !q} playlistId={playlistId} />}
    </div>
  )
}

const total = (tracks: Track[]) => tracks.reduce((s, t) => s + (t.durationMs || 0), 0)
const countLabel = (n: number) => `${n} ${n === 1 ? 'canción' : 'canciones'}`

export function AlbumView({ id }: { id: string }) {
  const app = useApp()
  const [album, setAlbum] = useState<Album | null>(null)
  const [error, setError] = useState(false)
  useEffect(() => {
    let alive = true
    setAlbum(null); setError(false)
    getAlbum(id).then((a) => alive && setAlbum(a)).catch(() => alive && setError(true))
    return () => { alive = false }
  }, [id])
  if (error) return <div className="page"><div className="emptyState"><p>No se pudo cargar el álbum.</p></div></div>
  if (!album) return <div className="page center"><LoaderCircle className="spin" size={26} /></div>
  const tracks = album.tracks || []
  return (
    <div className="page entity">
      <EntityHeader
        art={<Cover src={album.coverUrl} size={168} />}
        backdrop={album.coverUrl}
        kind="Álbum"
        title={album.title}
        subtitle={album.artist}
        onSubtitle={album.artistId ? () => app.navigate({ name: 'artist', id: album.artistId!, title: album.artist }) : undefined}
        chips={[album.year ? String(album.year) : '', `${countLabel(tracks.length)} · ${formatLength(total(tracks))}`]}
        tracks={tracks}
      />
      <FilterableList tracks={tracks} numbered empty="Este álbum no tiene canciones disponibles." />
    </div>
  )
}

export function ArtistView({ id, title }: { id: string; title?: string }) {
  const app = useApp()
  const [artist, setArtist] = useState<Artist | null>(null)
  const [failed, setFailed] = useState(false)
  const [all, setAll] = useState(false)
  const [filter, setFilter] = useState<'all' | 'album' | 'single'>('all')
  useEffect(() => {
    let alive = true
    setArtist(null); setFailed(false); setAll(false); setFilter('all')
    getArtist(id, title).then((a) => alive && setArtist(a)).catch(() => alive && setFailed(true))
    return () => { alive = false }
  }, [id, title])

  if (failed) return <div className="page"><div className="emptyState"><p>No se pudo cargar el artista.</p></div></div>
  if (!artist) return <div className="page center"><LoaderCircle className="spin" size={26} /></div>
  const top = artist.top || []
  const cover = artist.picture || top[0]?.coverUrl
  const albums = (artist.albums || []).filter((a) =>
    filter === 'all' || (filter === 'album' ? a.kind === 'album' || a.kind === 'compile' : a.kind === 'single' || a.kind === 'ep'))
  return (
    <div className="page entity">
      <EntityHeader
        art={cover ? <Cover src={cover} size={168} className="round" /> : <div className="cover initials round">{artist.name.slice(0, 1)}</div>}
        backdrop={cover}
        kind="Artista"
        title={artist.name}
        chips={[artist.fans ? `${artist.fans.toLocaleString('es')} fans` : '', artist.albums?.length ? `${artist.albums.length} lanzamientos` : '']}
        tracks={top}
        extra={top[0] && (
          <button className="btnGlass" onClick={() => app.startRadio(top[0])}><RadioIcon size={16} />Radio</button>
        )}
      />
      {top.length > 0 && (
        <section className="block">
          <h3 className="blockTitle">Populares</h3>
          <div className="listPanel"><TrackList tracks={all ? top : top.slice(0, 5)} showAlbum /></div>
          {top.length > 5 && <button className="textBtn" onClick={() => setAll(!all)}>{all ? 'Ver menos' : 'Ver más'}</button>}
        </section>
      )}
      {!!artist.albums?.length && (
        <section className="block">
          <div className="blockHead">
            <h3 className="blockTitle">Discografía</h3>
            <div className="chips">
              {([['all', 'Todo'], ['album', 'Álbumes'], ['single', 'Sencillos y EP']] as const).map(([k, l]) => (
                <button key={k} className={`chip ${filter === k ? 'on' : ''}`} onClick={() => setFilter(k)}>{l}</button>
              ))}
            </div>
          </div>
          <div className="cardGrid">{albums.map((a) => <AlbumCard key={a.id} album={a} />)}</div>
        </section>
      )}
      {!!artist.related?.length && (
        <Shelf title="Artistas similares">
          {artist.related.map((a) => <ArtistCard key={a.id} artist={a} />)}
        </Shelf>
      )}
    </div>
  )
}

export function RemotePlaylistView({ id }: { id: string }) {
  const [pl, setPl] = useState<RemotePlaylist | null>(null)
  const [failed, setFailed] = useState(false)
  useEffect(() => {
    let alive = true
    setPl(null); setFailed(false)
    dzPlaylist(id).then((p) => alive && setPl(p)).catch(() => alive && setFailed(true))
    return () => { alive = false }
  }, [id])
  if (failed) return <div className="page"><div className="emptyState"><p>No se pudo cargar la lista.</p></div></div>
  if (!pl) return <div className="page center"><LoaderCircle className="spin" size={26} /></div>
  const tracks = pl.tracks || []
  return (
    <div className="page entity">
      <EntityHeader
        art={<Cover src={pl.coverUrl} size={168} />}
        backdrop={pl.coverUrl}
        kind="Lista"
        title={pl.title}
        subtitle={pl.description || pl.author}
        chips={[`${countLabel(tracks.length)} · ${formatLength(total(tracks))}`]}
        tracks={tracks}
      />
      <FilterableList tracks={tracks} empty="Esta lista no tiene canciones disponibles." />
    </div>
  )
}

export function PlaylistView({ playlist, onRename, onDelete }: {
  playlist: Playlist; onRename: (name: string) => void; onDelete: () => void
}) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(playlist.name)
  useEffect(() => setName(playlist.name), [playlist.name])
  const tracks = playlist.tracks
  const save = () => { if (name.trim()) onRename(name.trim()); setEditing(false) }
  return (
    <div className="page entity">
      <EntityHeader
        art={<Collage covers={tracks.map((t) => t.coverUrl)} size={168} />}
        backdrop={tracks[0]?.coverUrl}
        kind="Playlist"
        title={playlist.name}
        chips={[`${countLabel(tracks.length)} · ${formatLength(total(tracks))}`]}
        tracks={tracks}
        extra={
          <>
            {editing ? (
              <span className="renameBox">
                <input autoFocus value={name} onChange={(e) => setName(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') save(); if (e.key === 'Escape') setEditing(false) }} />
                <button className="btnGlass square" onClick={save}><Check size={16} /></button>
              </span>
            ) : (
              <button className="btnGlass square" title="Renombrar" onClick={() => setEditing(true)}><Pencil size={16} /></button>
            )}
            <button className="btnGlass square danger" title="Eliminar playlist" onClick={() => {
              if (window.confirm(`¿Eliminar "${playlist.name}"? No se puede deshacer.`)) onDelete()
            }}><Trash2 size={16} /></button>
          </>
        }
      />
      <FilterableList tracks={tracks} playlistId={playlist.id}
        empty="Añade canciones desde el menú ⋮ de cualquier canción." />
    </div>
  )
}

export function CollectionView({ kind, tracks }: { kind: 'liked' | 'recent'; tracks: Track[] }) {
  return (
    <div className="page entity">
      <EntityHeader
        art={kind === 'liked' ? <LikedCover size={168} /> : <Collage covers={tracks.map((t) => t.coverUrl)} size={168} />}
        kind="Colección"
        title={kind === 'liked' ? 'Me gusta' : 'Escuchado hace poco'}
        chips={[countLabel(tracks.length), tracks.length ? formatLength(total(tracks)) : '']}
        tracks={tracks}
      />
      <FilterableList tracks={tracks}
        empty={kind === 'liked' ? 'Toca el corazón de una canción para guardarla aquí.' : 'Lo que escuches aparecerá aquí.'} />
    </div>
  )
}
