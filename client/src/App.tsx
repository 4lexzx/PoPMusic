import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Search as SearchIcon,
  Heart,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Mic2,
  Music2,
  LoaderCircle,
  X,
} from 'lucide-react'
import type { Track } from './types'
import { searchTracks, fetchLyrics } from './api'
import { resolveAudio } from './player'

type View = 'search' | 'favorites'

const FAV_KEY = 'popmusic-client-favorites'

function loadFavorites(): Track[] {
  try {
    return JSON.parse(localStorage.getItem(FAV_KEY) || '[]')
  } catch {
    return []
  }
}

function formatTime(ms: number): string {
  const total = Math.floor(ms / 1000)
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}

export default function App() {
  const [view, setView] = useState<View>('search')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Track[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [favorites, setFavorites] = useState<Track[]>(loadFavorites)
  const [current, setCurrent] = useState<Track | null>(null)
  const [playing, setPlaying] = useState(false)
  const [resolving, setResolving] = useState(false)
  const [progress, setProgress] = useState(0)
  const [lyrics, setLyrics] = useState<string | null>(null)
  const [showLyrics, setShowLyrics] = useState(false)
  const audioRef = useRef<HTMLAudioElement>(new Audio())

  useEffect(() => {
    localStorage.setItem(FAV_KEY, JSON.stringify(favorites))
  }, [favorites])

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
    try {
      const url = (await resolveAudio(track.artists, track.title)) || track.previewUrl
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

  const toggleFavorite = useCallback((track: Track) => {
    setFavorites((prev) =>
      prev.some((t) => t.id === track.id)
        ? prev.filter((t) => t.id !== track.id)
        : [...prev, track],
    )
  }, [])

  const openLyrics = useCallback(async () => {
    if (!current) return
    setShowLyrics(true)
    setLyrics(null)
    const text = await fetchLyrics(current.title, current.artists)
    setLyrics(text)
  }, [current])

  const list = view === 'favorites' ? favorites : results
  const isFavorite = useCallback(
    (id: string) => favorites.some((t) => t.id === id),
    [favorites],
  )

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
          className={`railBtn ${view === 'favorites' ? 'active' : ''}`}
          onClick={() => setView('favorites')}
          title="Favoritos"
        >
          <Heart size={22} />
        </button>
        <button className="railBtn" onClick={openLyrics} title="Letras">
          <Mic2 size={22} />
        </button>
      </aside>

      <main className="main">
        <header className="topbar">
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
        </header>

        <div className="content">
          <div className="sectionHead">
            <h1>{view === 'search' ? 'Resultados' : 'Tus favoritos'}</h1>
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
                  : 'Aún no guardaste canciones'}
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
                <img
                  className="art"
                  src={track.coverUrl}
                  alt=""
                  loading="lazy"
                />
                <div className="meta">
                  <span className="title">{track.title}</span>
                  <span className="artist">{track.artists}</span>
                </div>
                <span className="album">{track.album?.title || ''}</span>
                <span className="duration">{formatTime(track.durationMs)}</span>
                <button
                  className={`fav ${isFavorite(track.id) ? 'on' : ''}`}
                  onClick={() => toggleFavorite(track)}
                  title="Favorito"
                >
                  <Heart size={18} fill={isFavorite(track.id) ? 'currentColor' : 'none'} />
                </button>
              </div>
            ))}
          </div>
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
          <img className="nowArt" src={current?.coverUrl || ''} alt="" />
          <div className="nowMeta">
            <span className="nowTitle">{current?.title || 'Nada sonando'}</span>
            <span className="nowArtist">{current?.artists || ''}</span>
          </div>
          {current && (
            <button className={`fav ${isFavorite(current.id) ? 'on' : ''}`}
              onClick={() => toggleFavorite(current)}>
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
