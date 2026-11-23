import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Search as SearchIcon,
  Library as LibraryIcon,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Mic2,
  Music2,
  LoaderCircle,
  X,
  Heart,
  Plus,
  Trash2,
  ChevronLeft,
  ListPlus,
} from 'lucide-react'
import type { Track } from './types'
import { searchTracks, fetchLyrics } from './api'
import { resolveAudio } from './player'
import {
  loadLibrary,
  saveLibrary,
  upsertRecent,
  toggleFavorite as toggleFavoriteInLibrary,
  type Library,
} from './store'

type View = 'search' | 'library'
type LibraryTab = 'favorites' | 'recent' | 'playlists'

function formatTime(ms: number): string {
  const total = Math.floor(ms / 1000)
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}

export default function App() {
  const [view, setView] = useState<View>('search')
  const [libraryTab, setLibraryTab] = useState<LibraryTab>('favorites')
  const [openPlaylist, setOpenPlaylist] = useState<string | null>(null)
  const [library, setLibrary] = useState<Library>(loadLibrary)
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Track[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [current, setCurrent] = useState<Track | null>(null)
  const [playing, setPlaying] = useState(false)
  const [resolving, setResolving] = useState(false)
  const [progress, setProgress] = useState(0)
  const [lyrics, setLyrics] = useState<string | null>(null)
  const [showLyrics, setShowLyrics] = useState(false)
  const [menuFor, setMenuFor] = useState<string | null>(null)
  const [newPlaylist, setNewPlaylist] = useState('')
  const audioRef = useRef<HTMLAudioElement>(new Audio())

  useEffect(() => saveLibrary(library), [library])

  useEffect(() => {
    const audio = audioRef.current
    const onTime = () => setProgress(audio.currentTime * 1000)
    const onEnd = () => setPlaying(false)
    audio.addEventListener('timeupdate', onTime)
    audio.addEventListener('ended', onEnd)
    return () => {
      audio.removeEventListener('timeupdate', onTime)
      audio.removeEventListener('ended', onEnd)
    }
  }, [])

  useEffect(() => {
    const term = query.trim()
    if (term.length < 2) {
      setResults([])
      return
    }
    let cancelled = false
    const timer = setTimeout(async () => {
      setLoading(true)
      setError('')
      try {
        const data = await searchTracks(term)
        if (!cancelled) setResults(data.items)
      } catch (e) {
        if (!cancelled) setError(String(e))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }, 300)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [query])

  const play = useCallback(async (track: Track) => {
    setCurrent(track)
    setResolving(true)
    setError('')
    setLibrary((prev) => upsertRecent(prev, track))
    try {
      const url =
        (await resolveAudio(track.artists, track.title)) || track.previewUrl
      if (!url) throw new Error('No se encontró audio para esta canción')
      const audio = audioRef.current
      audio.src = url
      await audio.play()
      setPlaying(true)
    } catch (e) {
      setError(String(e))
      setPlaying(false)
    } finally {
      setResolving(false)
    }
  }, [])

  const toggle = useCallback(() => {
    const audio = audioRef.current
    if (!current) return
    if (playing) {
      audio.pause()
      setPlaying(false)
    } else if (audio.src) {
      void audio.play()
      setPlaying(true)
    } else {
      void play(current)
    }
  }, [current, playing, play])

  const fav = useCallback((track: Track) => {
    setLibrary((prev) => toggleFavoriteInLibrary(prev, track))
  }, [])

  const isFavorite = useCallback(
    (id: string) => library.favorites.some((t) => t.id === id),
    [library.favorites],
  )

  const createPlaylist = useCallback(() => {
    const name = newPlaylist.trim()
    if (!name) return
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
    setLibrary((prev) => ({
      ...prev,
      playlists: [...prev.playlists, { id, name, tracks: [], createdAt: Date.now() }],
    }))
    setNewPlaylist('')
    setOpenPlaylist(id)
  }, [newPlaylist])

  const addToPlaylist = useCallback((playlistId: string, track: Track) => {
    setLibrary((prev) => ({
      ...prev,
      playlists: prev.playlists.map((p) =>
        p.id === playlistId && !p.tracks.some((t) => t.id === track.id)
          ? { ...p, tracks: [track, ...p.tracks] }
          : p,
      ),
    }))
    setMenuFor(null)
  }, [])

  const removeFromPlaylist = useCallback((playlistId: string, trackId: string) => {
    setLibrary((prev) => ({
      ...prev,
      playlists: prev.playlists.map((p) =>
        p.id === playlistId
          ? { ...p, tracks: p.tracks.filter((t) => t.id !== trackId) }
          : p,
      ),
    }))
  }, [])

  const openLyrics = useCallback(async () => {
    if (!current) return
    setShowLyrics(true)
    setLyrics(null)
    setLyrics(await fetchLyrics(current.title, current.artists))
  }, [current])

  const selected = library.playlists.find((p) => p.id === openPlaylist)

  const list = useMemo(() => {
    if (view === 'search') return results
    if (libraryTab === 'favorites') return library.favorites
    if (libraryTab === 'recent') return library.recent
    return selected?.tracks || []
  }, [view, results, libraryTab, library.favorites, library.recent, selected])

  const title =
    view === 'search'
      ? 'Resultados'
      : libraryTab === 'favorites'
        ? 'Favoritos'
        : libraryTab === 'recent'
          ? 'Recientes'
          : selected?.name || 'Playlists'

  return (
    <div className="app">
      <aside className="rail">
        <div className="mark">PM</div>
        <button
          className={`railBtn ${view === 'search' ? 'active' : ''}`}
          onClick={() => setView('search')}
          title="Buscar"
        >
          <SearchIcon size={22} />
        </button>
        <button
          className={`railBtn ${view === 'library' ? 'active' : ''}`}
          onClick={() => setView('library')}
          title="Biblioteca"
        >
          <LibraryIcon size={22} />
        </button>
        <button className="railBtn" onClick={openLyrics} title="Letras">
          <Mic2 size={22} />
        </button>
      </aside>

      <main className="main">
        <header className="topbar">
          {view === 'search' ? (
            <>
              <div className="searchBox">
                <SearchIcon size={18} />
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Busca canciones, artistas o álbumes"
                  spellCheck={false}
                />
                {query && (
                  <button className="clear" onClick={() => setQuery('')}>
                    <X size={16} />
                  </button>
                )}
              </div>
              <span className="badge">Solo música</span>
            </>
          ) : (
            <div className="libraryTabs">
              {libraryTab === 'playlists' && selected && (
                <button className="back" onClick={() => setOpenPlaylist(null)}>
                  <ChevronLeft size={18} /> Playlists
                </button>
              )}
              {!(libraryTab === 'playlists' && selected) && (
                <>
                  <button
                    className={libraryTab === 'favorites' ? 'on' : ''}
                    onClick={() => { setLibraryTab('favorites'); setOpenPlaylist(null) }}
                  >
                    <Heart size={15} /> Favoritos
                  </button>
                  <button
                    className={libraryTab === 'recent' ? 'on' : ''}
                    onClick={() => { setLibraryTab('recent'); setOpenPlaylist(null) }}
                  >
                    <Music2 size={15} /> Recientes
                  </button>
                  <button
                    className={libraryTab === 'playlists' ? 'on' : ''}
                    onClick={() => { setLibraryTab('playlists'); setOpenPlaylist(null) }}
                  >
                    <ListPlus size={15} /> Playlists
                  </button>
                </>
              )}
            </div>
          )}
        </header>

        <div className="content">
          {view === 'library' && libraryTab === 'playlists' && !selected && (
            <div className="playlistCreator">
              <input
                value={newPlaylist}
                onChange={(e) => setNewPlaylist(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && createPlaylist()}
                placeholder="Nombre de la playlist nueva"
              />
              <button onClick={createPlaylist}><Plus size={16} /> Crear</button>
            </div>
          )}

          {view === 'library' && libraryTab === 'playlists' && !selected && (
            <div className="playlistGrid">
              {library.playlists.map((p) => (
                <button
                  key={p.id}
                  className="playlistCard"
                  onClick={() => setOpenPlaylist(p.id)}
                >
                  <span className="playlistArt"><Music2 size={26} /></span>
                  <span className="playlistName">{p.name}</span>
                  <span className="playlistCount">{p.tracks.length} canciones</span>
                </button>
              ))}
            </div>
          )}

          {(view === 'search' || libraryTab !== 'playlists' || selected) && (
            <>
              <div className="sectionHead">
                <h1>{title}</h1>
                <span>{list.length} canciones</span>
              </div>

              {error && <div className="error">{error}</div>}
              {loading && (
                <div className="empty">
                  <LoaderCircle className="spin" size={28} />
                  <span>Buscando…</span>
                </div>
              )}
              {!loading && list.length === 0 && (
                <div className="empty">
                  <Music2 size={36} />
                  <span>
                    {view === 'search'
                      ? 'Escribe algo para empezar'
                      : 'Aún no hay canciones aquí'}
                  </span>
                </div>
              )}

              <div className="list">
                {list.map((track) => (
                  <div
                    key={track.id}
                    className={`row ${current?.id === track.id ? 'current' : ''}`}
                    onDoubleClick={() => void play(track)}
                  >
                    <button className="playCell" onClick={() => void play(track)}>
                      {resolving && current?.id === track.id ? (
                        <LoaderCircle className="spin" size={18} />
                      ) : current?.id === track.id && playing ? (
                        <Pause size={18} fill="currentColor" />
                      ) : (
                        <Play size={18} fill="currentColor" />
                      )}
                    </button>
                    <img className="art" src={track.coverUrl} alt="" loading="lazy" />
                    <div className="meta">
                      <span className="title">{track.title}</span>
                      <span className="artist">{track.artists}</span>
                    </div>
                    <span className="album">{track.album?.title || ''}</span>
                    <span className="duration">{formatTime(track.durationMs)}</span>
                    <button
                      className={`fav ${isFavorite(track.id) ? 'on' : ''}`}
                      onClick={() => fav(track)}
                      title="Favorito"
                    >
                      <Heart size={18} fill={isFavorite(track.id) ? 'currentColor' : 'none'} />
                    </button>
                    <div className="menuWrap">
                      <button
                        className="fav"
                        onClick={() => setMenuFor(menuFor === track.id ? null : track.id)}
                        title="Añadir a playlist"
                      >
                        <Plus size={18} />
                      </button>
                      {menuFor === track.id && (
                        <div className="menu">
                          {library.playlists.length === 0 && (
                            <span className="menuEmpty">Crea una playlist primero</span>
                          )}
                          {library.playlists.map((p) => (
                            <button key={p.id} onClick={() => addToPlaylist(p.id, track)}>
                              {p.name}
                            </button>
                          ))}
                          {selected && (
                            <button
                              className="danger"
                              onClick={() => {
                                removeFromPlaylist(selected.id, track.id)
                                setMenuFor(null)
                              }}
                            >
                              <Trash2 size={14} /> Quitar de esta playlist
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </main>

      {showLyrics && (
        <div className="lyricsPanel">
          <div className="lyricsHead">
            <span>Letras</span>
            <button onClick={() => setShowLyrics(false)}><X size={18} /></button>
          </div>
          <pre>{lyrics ?? 'Buscando letra…'}</pre>
        </div>
      )}

      <footer className="player">
        <div className="now">
          <img className="nowArt" src={current?.coverUrl || undefined} alt="" />
          <div className="nowMeta">
            <span className="nowTitle">{current?.title || 'Nada sonando'}</span>
            <span className="nowArtist">{current?.artists || ''}</span>
          </div>
          {current && (
            <button className={`fav ${isFavorite(current.id) ? 'on' : ''}`}
              onClick={() => fav(current)}>
              <Heart size={18} fill={isFavorite(current.id) ? 'currentColor' : 'none'} />
            </button>
          )}
        </div>
        <div className="controls">
          <button onClick={() => list[0] && void play(list[0])}><SkipBack size={18} /></button>
          <button className="mainPlay" onClick={toggle}>
            {playing ? <Pause size={20} fill="currentColor" /> : <Play size={20} fill="currentColor" />}
          </button>
          <button onClick={() => list[1] && void play(list[1])}><SkipForward size={18} /></button>
        </div>
        <div className="progress">
          <span>{formatTime(progress)}</span>
          <div className="bar"><div style={{ width: `${Math.min(100, (progress / (current?.durationMs || 1)) * 100)}%` }} /></div>
          <span>{formatTime(current?.durationMs || 0)}</span>
        </div>
      </footer>
    </div>
  )
}
