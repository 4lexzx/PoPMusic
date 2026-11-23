import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Search as SearchIcon,
  Home as HomeIcon,
  Library as LibraryIcon,
  Settings as SettingsIcon,
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
  Shuffle,
  Repeat,
  Repeat1,
  ListMusic,
  Volume2,
} from 'lucide-react'
import type { LyricLine, Settings, Track } from './types'
import { explorePopular, searchTracks, fetchLyrics, fetchPreview } from './api'
import { resolveAudio } from './player'
import { parseLyrics } from './lyrics'
import {
  loadLibrary, saveLibrary, upsertRecent, toggleFavorite as toggleFavoriteInLibrary,
  loadSettings, saveSettings, loadQueue, saveQueue,
  type Library, type QueueState,
} from './store'
import LyricsPanel from './components/LyricsPanel'
import SettingsView from './components/SettingsView'

type View = 'home' | 'search' | 'library' | 'settings'
type LibraryTab = 'favorites' | 'recent' | 'playlists'

function formatTime(ms: number): string {
  const total = Math.floor(ms / 1000)
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}

export default function App() {
  const [view, setView] = useState<View>('home')
  const [libraryTab, setLibraryTab] = useState<LibraryTab>('favorites')
  const [openPlaylist, setOpenPlaylist] = useState<string | null>(null)
  const [library, setLibrary] = useState<Library>(loadLibrary)
  const [settings, setSettings] = useState<Settings>(loadSettings)
  const [queue, setQueue] = useState<QueueState>(loadQueue)
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Track[]>([])
  const [discover, setDiscover] = useState<Track[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [playing, setPlaying] = useState(false)
  const [resolving, setResolving] = useState(false)
  const [progress, setProgress] = useState(0)
  const [lyrics, setLyrics] = useState<LyricLine[]>([])
  const [lyricsLoading, setLyricsLoading] = useState(false)
  const [showLyrics, setShowLyrics] = useState(false)
  const [showQueue, setShowQueue] = useState(false)
  const [menuFor, setMenuFor] = useState<string | null>(null)
  const [newPlaylist, setNewPlaylist] = useState('')
  const audioRef = useRef<HTMLAudioElement>(new Audio())
  const fadeRef = useRef<number | null>(null)

  const current = queue.tracks[queue.index] || null

  useEffect(() => saveLibrary(library), [library])
  useEffect(() => saveSettings(settings), [settings])
  useEffect(() => saveQueue(queue), [queue])
  useEffect(() => { audioRef.current.volume = settings.volume }, [settings.volume])

  useEffect(() => {
    const audio = audioRef.current
    const onTime = () => setProgress(audio.currentTime * 1000)
    audio.addEventListener('timeupdate', onTime)
    return () => audio.removeEventListener('timeupdate', onTime)
  }, [])

  useEffect(() => {
    let alive = true
    explorePopular(settings.country)
      .then((r) => alive && setDiscover(r.items))
      .catch(() => alive && setDiscover([]))
    return () => { alive = false }
  }, [settings.country])

  useEffect(() => {
    const term = query.trim()
    if (term.length < 2) { setResults([]); return }
    let cancelled = false
    const timer = setTimeout(async () => {
      setLoading(true)
      setError('')
      try {
        const data = await searchTracks(term, settings.country)
        if (!cancelled) setResults(data.items)
      } catch (e) {
        if (!cancelled) setError(String(e))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }, 300)
    return () => { cancelled = true; clearTimeout(timer) }
  }, [query, settings.country])

  const fadeOut = useCallback((ms: number, done: () => void) => {
    if (fadeRef.current) clearTimeout(fadeRef.current)
    if (ms <= 0) { done(); return }
    const audio = audioRef.current
    const from = audio.volume
    const start = Date.now()
    const step = () => {
      const elapsed = Date.now() - start
      const ratio = Math.min(1, elapsed / ms)
      audio.volume = from * (1 - ratio)
      if (ratio < 1) fadeRef.current = window.setTimeout(step, 80)
      else done()
    }
    step()
  }, [])

  const loadTrack = useCallback(async (track: Track, autoplay = true) => {
    setResolving(true)
    setError('')
    setProgress(0)
    setLibrary((prev) => upsertRecent(prev, track))
    try {
      const url = (await resolveAudio(track.artists, track.title))
        || track.previewUrl
        || (await fetchPreview(track.title, track.artists))
      if (!url) throw new Error('No se encontró audio para esta canción')
      const audio = audioRef.current
      audio.src = url
      audio.volume = settings.volume
      if (autoplay) {
        await audio.play()
        setPlaying(true)
      }
    } catch (e) {
      setError(String(e))
      setPlaying(false)
    } finally {
      setResolving(false)
    }
  }, [settings.volume])

  const playAt = useCallback((index: number, tracks?: Track[]) => {
    const list = tracks || queue.tracks
    if (!list[index]) return
    setQueue((q) => ({ ...q, tracks: tracks || q.tracks, index }))
    void loadTrack(list[index])
  }, [queue.tracks, loadTrack])

  const playTrack = useCallback((track: Track, context?: Track[]) => {
    if (context) {
      const idx = context.findIndex((t) => t.id === track.id)
      playAt(idx >= 0 ? idx : 0, context)
    } else {
      playAt(0, [track])
    }
  }, [playAt])

  const advance = useCallback((direction: 1 | -1, auto = false) => {
    const { tracks, index, shuffle, repeat } = queue
    if (!tracks.length) return
    if (auto && repeat === 'one') {
      void loadTrack(tracks[index])
      return
    }
    let nextIndex = index + direction
    if (shuffle && tracks.length > 1) {
      nextIndex = Math.floor(Math.random() * tracks.length)
      if (nextIndex === index) nextIndex = (index + 1) % tracks.length
    }
    if (nextIndex >= tracks.length) {
      if (repeat === 'all') nextIndex = 0
      else { setPlaying(false); return }
    }
    if (nextIndex < 0) nextIndex = tracks.length - 1
    const go = () => playAt(nextIndex)
    if (settings.crossfade > 0 && playing) fadeOut(settings.crossfade * 1000, go)
    else go()
  }, [queue, settings.crossfade, playing, fadeOut, playAt, loadTrack])

  useEffect(() => {
    const audio = audioRef.current
    const onEnd = () => advance(1, true)
    audio.addEventListener('ended', onEnd)
    return () => audio.removeEventListener('ended', onEnd)
  }, [advance])

  const toggle = useCallback(() => {
    const audio = audioRef.current
    if (!current) return
    if (playing) { audio.pause(); setPlaying(false) }
    else if (audio.src) { void audio.play(); setPlaying(true) }
    else void loadTrack(current)
  }, [current, playing, loadTrack])

  const toggleShuffle = useCallback(() => {
    setQueue((q) => {
      if (!q.shuffle) {
        const others = q.tracks.filter((_, i) => i !== q.index)
        for (let i = others.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          ;[others[i], others[j]] = [others[j], others[i]]
        }
        return { ...q, shuffle: true, tracks: [q.tracks[q.index], ...others], index: 0 }
      }
      return { ...q, shuffle: false }
    })
  }, [])

  const cycleRepeat = useCallback(() => {
    setQueue((q) => ({ ...q, repeat: q.repeat === 'off' ? 'all' : q.repeat === 'all' ? 'one' : 'off' }))
  }, [])

  const fav = useCallback((track: Track) => setLibrary((p) => toggleFavoriteInLibrary(p, track)), [])
  const isFavorite = useCallback((id: string) => library.favorites.some((t) => t.id === id), [library.favorites])

  const createPlaylist = useCallback(() => {
    const name = newPlaylist.trim()
    if (!name) return
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
    setLibrary((p) => ({ ...p, playlists: [...p.playlists, { id, name, tracks: [], createdAt: Date.now() }] }))
    setNewPlaylist('')
    setOpenPlaylist(id)
  }, [newPlaylist])

  const addToPlaylist = useCallback((playlistId: string, track: Track) => {
    setLibrary((p) => ({
      ...p,
      playlists: p.playlists.map((pl) =>
        pl.id === playlistId && !pl.tracks.some((t) => t.id === track.id)
          ? { ...pl, tracks: [track, ...pl.tracks] } : pl,
      ),
    }))
    setMenuFor(null)
  }, [])

  const removeFromPlaylist = useCallback((playlistId: string, trackId: string) => {
    setLibrary((p) => ({ ...p, playlists: p.playlists.map((pl) =>
      pl.id === playlistId ? { ...pl, tracks: pl.tracks.filter((t) => t.id !== trackId) } : pl) }))
  }, [])

  const openLyrics = useCallback(async () => {
    if (!current) return
    setShowLyrics(true)
    setLyricsLoading(true)
    setLyrics([])
    setLyrics(parseLyrics(await fetchLyrics(current.title, current.artists)))
    setLyricsLoading(false)
  }, [current])

  const selected = library.playlists.find((p) => p.id === openPlaylist)

  const list = useMemo(() => {
    if (view === 'search') return results
    if (view === 'home') return discover
    if (libraryTab === 'favorites') return library.favorites
    if (libraryTab === 'recent') return library.recent
    return selected?.tracks || []
  }, [view, results, discover, libraryTab, library.favorites, library.recent, selected])

  const title = view === 'settings' ? 'Ajustes'
    : view === 'home' ? 'Descubre'
      : view === 'search' ? 'Resultados'
      : libraryTab === 'favorites' ? 'Favoritos'
        : libraryTab === 'recent' ? 'Recientes'
          : selected?.name || 'Playlists'

  return (
    <div className="app">
      <aside className="rail">
        <div className="mark">PM</div>
        <button className={`railBtn ${view === 'home' ? 'active' : ''}`} onClick={() => setView('home')} title="Inicio"><HomeIcon size={22} /></button>
        <button className={`railBtn ${view === 'search' ? 'active' : ''}`} onClick={() => setView('search')} title="Buscar"><SearchIcon size={22} /></button>
        <button className={`railBtn ${view === 'library' ? 'active' : ''}`} onClick={() => setView('library')} title="Biblioteca"><LibraryIcon size={22} /></button>
        <button className="railBtn" onClick={openLyrics} title="Letras"><Mic2 size={22} /></button>
        <div className="railSpacer" />
        <button className={`railBtn ${view === 'settings' ? 'active' : ''}`} onClick={() => setView('settings')} title="Ajustes"><SettingsIcon size={22} /></button>
      </aside>

      <main className="main">
        <header className="topbar">
          {view === 'search' ? (
            <>
              <div className="searchBox">
                <SearchIcon size={18} />
                <input value={query} onChange={(e) => setQuery(e.target.value)}
                  placeholder="Busca canciones, artistas o álbumes" spellCheck={false} />
                {query && <button className="clear" onClick={() => setQuery('')}><X size={16} /></button>}
              </div>
              <span className="badge">Solo música</span>
            </>
          ) : view === 'library' ? (
            <div className="libraryTabs">
              {libraryTab === 'playlists' && selected && (
                <button className="back" onClick={() => setOpenPlaylist(null)}><ChevronLeft size={18} /> Playlists</button>
              )}
              {!(libraryTab === 'playlists' && selected) && (
                <>
                  <button className={libraryTab === 'favorites' ? 'on' : ''} onClick={() => { setLibraryTab('favorites'); setOpenPlaylist(null) }}><Heart size={15} /> Favoritos</button>
                  <button className={libraryTab === 'recent' ? 'on' : ''} onClick={() => { setLibraryTab('recent'); setOpenPlaylist(null) }}><Music2 size={15} /> Recientes</button>
                  <button className={libraryTab === 'playlists' ? 'on' : ''} onClick={() => { setLibraryTab('playlists'); setOpenPlaylist(null) }}><ListPlus size={15} /> Playlists</button>
                </>
              )}
            </div>
          ) : <div className="topTitle">{title}</div>}
          <button className={`queueBtn ${showQueue ? 'on' : ''}`} onClick={() => setShowQueue(!showQueue)} title="Cola"><ListMusic size={18} /></button>
        </header>

        <div className="content">
          {view === 'settings' ? (
            <SettingsView settings={settings} onChange={setSettings}
              onClearLibrary={() => setLibrary({ favorites: [], recent: [], playlists: [] })} />
          ) : (
            <>
              {view === 'library' && libraryTab === 'playlists' && !selected && (
                <div className="playlistCreator">
                  <input value={newPlaylist} onChange={(e) => setNewPlaylist(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && createPlaylist()}
                    placeholder="Nombre de la playlist nueva" />
                  <button onClick={createPlaylist}><Plus size={16} /> Crear</button>
                </div>
              )}
              {view === 'library' && libraryTab === 'playlists' && !selected && (
                <div className="playlistGrid">
                  {library.playlists.map((p) => (
                    <button key={p.id} className="playlistCard" onClick={() => setOpenPlaylist(p.id)}>
                      <span className="playlistArt"><Music2 size={26} /></span>
                      <span className="playlistName">{p.name}</span>
                      <span className="playlistCount">{p.tracks.length} canciones</span>
                    </button>
                  ))}
                </div>
              )}
              {(view !== 'library' || libraryTab !== 'playlists' || selected) && (
                <>
                  <div className="sectionHead">
                    <h1>{title}</h1>
                    <span>{list.length} canciones</span>
                  </div>
                  {error && <div className="error">{error}</div>}
                  {loading && <div className="empty"><LoaderCircle className="spin" size={28} /><span>Buscando…</span></div>}
                  {!loading && list.length === 0 && (
                    <div className="empty"><Music2 size={36} /><span>Aún no hay canciones aquí</span></div>
                  )}
                  <div className="list">
                    {list.map((track) => (
                      <div key={track.id} className={`row ${current?.id === track.id ? 'current' : ''}`}
                        onDoubleClick={() => playTrack(track, list)}>
                        <button className="playCell" onClick={() => playTrack(track, list)}>
                          {resolving && current?.id === track.id ? <LoaderCircle className="spin" size={18} />
                            : current?.id === track.id && playing ? <Pause size={18} fill="currentColor" />
                              : <Play size={18} fill="currentColor" />}
                        </button>
                        <img className="art" src={track.coverUrl} alt="" loading="lazy" />
                        <div className="meta"><span className="title">{track.title}</span><span className="artist">{track.artists}</span></div>
                        <span className="album">{track.album?.title || ''}</span>
                        <span className="duration">{formatTime(track.durationMs)}</span>
                        <button className={`fav ${isFavorite(track.id) ? 'on' : ''}`} onClick={() => fav(track)} title="Favorito">
                          <Heart size={18} fill={isFavorite(track.id) ? 'currentColor' : 'none'} />
                        </button>
                        <div className="menuWrap">
                          <button className="fav" onClick={() => setMenuFor(menuFor === track.id ? null : track.id)} title="Añadir a playlist"><Plus size={18} /></button>
                          {menuFor === track.id && (
                            <div className="menu">
                              {library.playlists.length === 0 && <span className="menuEmpty">Crea una playlist primero</span>}
                              {library.playlists.map((p) => <button key={p.id} onClick={() => addToPlaylist(p.id, track)}>{p.name}</button>)}
                              {selected && <button className="danger" onClick={() => { removeFromPlaylist(selected.id, track.id); setMenuFor(null) }}><Trash2 size={14} /> Quitar de esta playlist</button>}
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </>
          )}
        </div>
      </main>

      {showQueue && (
        <div className="queuePanel">
          <div className="lyricsHead"><span>Cola · {queue.tracks.length}</span><button onClick={() => setShowQueue(false)}><X size={18} /></button></div>
          <div className="queueBody">
            {queue.tracks.map((t, i) => (
              <button key={`${t.id}-${i}`} className={`queueItem${i === queue.index ? ' active' : ''}`} onClick={() => playAt(i)}>
                <img src={t.coverUrl} alt="" /><span>{t.title}<small>{t.artists}</small></span>
              </button>
            ))}
            {queue.tracks.length === 0 && <p className="lyricsHint">La cola está vacía.</p>}
          </div>
        </div>
      )}

      {showLyrics && <LyricsPanel track={current} lines={lyrics} loading={lyricsLoading} positionMs={progress} onClose={() => setShowLyrics(false)} />}

      <footer className="player">
        <div className="now">
          <img className="nowArt" src={current?.coverUrl || undefined} alt="" />
          <div className="nowMeta">
            <span className="nowTitle">{current?.title || 'Nada sonando'}</span>
            <span className="nowArtist">{current?.artists || ''}</span>
          </div>
          {current && <button className={`fav ${isFavorite(current.id) ? 'on' : ''}`} onClick={() => fav(current)}><Heart size={18} fill={isFavorite(current.id) ? 'currentColor' : 'none'} /></button>}
        </div>
        <div className="playerCenter">
          <div className="controls">
            <button className={queue.shuffle ? 'on' : ''} onClick={toggleShuffle} title="Aleatorio"><Shuffle size={17} /></button>
            <button onClick={() => advance(-1)}><SkipBack size={18} /></button>
            <button className="mainPlay" onClick={toggle}>
              {playing ? <Pause size={20} fill="currentColor" /> : <Play size={20} fill="currentColor" />}
            </button>
            <button onClick={() => advance(1)}><SkipForward size={18} /></button>
            <button className={queue.repeat !== 'off' ? 'on' : ''} onClick={cycleRepeat} title="Repetir">
              {queue.repeat === 'one' ? <Repeat1 size={17} /> : <Repeat size={17} />}
            </button>
          </div>
          <div className="progress">
            <span>{formatTime(progress)}</span>
            <input type="range" min="0" max={Math.max(1, current?.durationMs || 0)} value={Math.min(progress, current?.durationMs || 0)}
              onChange={(e) => { const ms = Number(e.target.value); audioRef.current.currentTime = ms / 1000; setProgress(ms) }} />
            <span>{formatTime(current?.durationMs || 0)}</span>
          </div>
        </div>
        <div className="playerRight">
          <Volume2 size={16} />
          <input type="range" min="0" max="1" step="0.01" value={settings.volume}
            onChange={(e) => setSettings({ ...settings, volume: Number(e.target.value) })} />
        </div>
      </footer>
    </div>
  )
}
