import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { LyricLine, Settings, Track } from './types'
import { fetchLyrics, fetchPreview, hd } from './api'
import { similarTo } from './recommend'
import type { PrefetchPriority, RemoteCommand } from './electron.d'
import { applyTheme } from './theme'
import { hasFullAudio, resolveAudio } from './player'
import { parseLyrics } from './lyrics'
import {
  loadLibrary, saveLibrary, upsertRecent, toggleFavorite as toggleFavoriteInLibrary,
  loadSettings, saveSettings, loadQueue, saveQueue,
  type Library, type QueueState,
} from './store'
import { AppContext, ArtQualityContext, type AppActions, type Route } from './ui'
import HomeView from './components/HomeView'
import SearchView from './components/SearchView'
import LibraryView from './components/LibraryView'
import SettingsModal from './components/SettingsModal'
import { AlbumView, ArtistView, CollectionView, PlaylistView, RemotePlaylistView } from './components/EntityView'
import PlayerBar from './components/PlayerBar'
import FullPlayer from './components/FullPlayer'
import { QueuePanel, ResizeEdges, Sidebar, Toasts, WindowControls } from './components/Chrome'

/** Duración del audio que de verdad suena (la versión de YouTube puede diferir del catálogo). */
function realDuration(audio: HTMLAudioElement | null): number {
  return audio && Number.isFinite(audio.duration) && audio.src.includes('googlevideo') ? audio.duration * 1000 : 0
}

export default function App() {
  const [history, setHistory] = useState<Route[]>([{ name: 'home' }])
  const route = history[history.length - 1]
  const [library, setLibrary] = useState<Library>(loadLibrary)
  const [settings, setSettings] = useState<Settings>(loadSettings)
  const [queue, setQueue] = useState<QueueState>(() => {
    const q = loadQueue()
    return loadSettings().startup === 'empty' ? { ...q, tracks: [], index: 0 } : q
  })
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [playing, setPlaying] = useState(false)
  const [resolving, setResolving] = useState(false)
  const resolvingRef = useRef(false)
  resolvingRef.current = resolving
  const [preview, setPreview] = useState(false)
  const [progress, setProgress] = useState(0)
  const [durationMs, setDurationMs] = useState(0)
  const [lyrics, setLyrics] = useState<LyricLine[]>([])
  const lyricsFor = useRef<string | null>(null)
  const [lyricsLoading, setLyricsLoading] = useState(false)
  const [fullOpen, setFullOpen] = useState(false)
  const [queueOpen, setQueueOpen] = useState(false)
  const [toasts, setToasts] = useState<{ id: number; title: string; detail?: string }[]>([])
  const audioRef = useRef<HTMLAudioElement>(null)
  const loadTokenRef = useRef(0)
  const fadeRef = useRef<number | null>(null)
  const scroller = useRef<HTMLDivElement>(null)

  const current = queue.tracks[queue.index] || null
  const currentRef = useRef(current)
  currentRef.current = current
  const retriedRef = useRef<string | null>(null)
  const favIdsRef = useRef<Set<string>>(new Set())
  /** Posición real del audio en ms (estable: no cambia entre renders). */
  const getAudioTime = useCallback(() => (audioRef.current ? audioRef.current.currentTime * 1000 : 0), [])

  /** Pide a yt-dlp la canción por adelantado para que suene al instante. */
  const prefetch = useCallback((t: Track, priority: PrefetchPriority = 'hover') => {
    window.popApi?.prefetchAudio?.(t.artists, t.title, priority)
  }, [])

  useEffect(() => saveLibrary(library), [library])
  useEffect(() => saveSettings(settings), [settings])
  useEffect(() => saveQueue(queue), [queue])
  useEffect(() => { if (audioRef.current) audioRef.current.volume = settings.volume }, [settings.volume])
  useEffect(() => { document.documentElement.classList.toggle('reduceMotion', settings.reduceMotion) }, [settings.reduceMotion])

  // Tema y color de énfasis (con contraste calculado) sobre :root.
  useEffect(() => {
    applyTheme(settings.theme, settings.accent)
    if (settings.theme !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const on = () => applyTheme('system', settings.accent)
    mq.addEventListener('change', on)
    return () => mq.removeEventListener('change', on)
  }, [settings.theme, settings.accent])

  // Salida de audio elegida en Ajustes → Audio.
  useEffect(() => {
    const audio = audioRef.current as (HTMLAudioElement & { setSinkId?: (id: string) => Promise<void> }) | null
    if (audio?.setSinkId) audio.setSinkId(settings.outputDevice || '').catch(() => {})
  }, [settings.outputDevice])

  // Precarga la siguiente canción en cuanto la actual ya suena (no a la vez,
  // para que dos yt-dlp no se frenen entre sí): así el salto es inmediato.
  useEffect(() => {
    const upcoming = queue.tracks.slice(queue.index + 1, queue.index + 3)
    if (!upcoming.length || resolving) return
    const timer = setTimeout(() => upcoming.forEach((t) => prefetch(t, 'next')), 300)
    return () => clearTimeout(timer)
  }, [queue.index, queue.tracks, resolving, prefetch])

  // ----- superposición y cartel multimedia de Windows -----
  useEffect(() => {
    window.popApi?.overlayConfig?.({ enabled: settings.overlay, onMinimize: settings.overlayOnMinimize, anchor: settings.overlayAnchor })
  }, [settings.overlay, settings.overlayOnMinimize, settings.overlayAnchor])

  useEffect(() => {
    const send = () => window.popApi?.sendNowPlaying?.({
      track: current ? { title: current.title, artists: current.artists, coverUrl: hd(current.coverUrl, 300) } : null,
      playing,
      position: (audioRef.current?.currentTime || 0) * 1000,
      duration: durationMs || current?.durationMs || 0,
      at: Date.now(),
      liked: current ? favIdsRef.current.has(current.id) : false,
    })
    send()
    const audio = audioRef.current
    audio?.addEventListener('seeked', send)
    return () => audio?.removeEventListener('seeked', send)
  }, [current, playing, durationMs])

  // Teclas multimedia, auriculares y pantalla de bloqueo de Windows.
  useEffect(() => {
    if (!('mediaSession' in navigator) || !current) return
    navigator.mediaSession.metadata = new MediaMetadata({
      title: current.title,
      artist: current.artists,
      album: current.album?.title || '',
      artwork: [{ src: hd(current.coverUrl, 512), sizes: '512x512', type: 'image/jpeg' }],
    })
  }, [current])
  useEffect(() => {
    if ('mediaSession' in navigator) navigator.mediaSession.playbackState = playing ? 'playing' : 'paused'
  }, [playing])

  // ----- navegación -----
  const navigate = useCallback((r: Route) => {
    if (r.name === 'settings') { setSettingsOpen(true); return }
    setHistory((h) => {
      const top = h[h.length - 1]
      if (JSON.stringify(top) === JSON.stringify(r)) return h
      const base = ['home', 'search', 'library', 'settings'].includes(r.name) ? [] : h
      return [...base, r].slice(-30)
    })
    setFullOpen(false)
    scroller.current?.scrollTo({ top: 0 })
  }, [])
  const back = useCallback(() => setHistory((h) => (h.length > 1 ? h.slice(0, -1) : h)), [])

  const toast = useCallback((title: string, detail?: string) => {
    const id = Date.now() + Math.random()
    setToasts((t) => [...t.slice(-2), { id, title, detail }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 2600)
  }, [])

  // ----- audio -----
  const fadeOut = useCallback((ms: number, done: () => void) => {
    if (fadeRef.current) clearTimeout(fadeRef.current)
    const audio = audioRef.current
    if (ms <= 0 || !audio) { done(); return }
    const from = audio.volume
    const start = Date.now()
    const step = () => {
      const ratio = Math.min(1, (Date.now() - start) / ms)
      audio.volume = from * (1 - ratio)
      if (ratio < 1) fadeRef.current = window.setTimeout(step, 80)
      else done()
    }
    step()
  }, [])

  const loadTrack = useCallback(async (track: Track, autoplay = true) => {
    const token = ++loadTokenRef.current
    const alive = () => token === loadTokenRef.current
    setProgress(0)
    setDurationMs(track.durationMs || 0)
    setLibrary((prev) => upsertRecent(prev, track))
    const audio = audioRef.current
    if (!audio) return
    audio.pause()
    setResolving(true)
    setPreview(false)

    const start = async (src: string, isPreview: boolean) => {
      audio.src = src
      audio.volume = settings.volume
      audio.load()
      setPreview(isPreview)
      if (autoplay) { await audio.play(); setPlaying(true) }
    }

    // Sin Electron no hay yt-dlp: solo queda el fragmento de 30 s de iTunes.
    if (!hasFullAudio) {
      const clip = track.previewUrl || (await fetchPreview(track.title, track.artists))
      if (!alive()) return
      setResolving(false)
      if (clip) await start(clip, true).catch(() => {})
      else toast('No se encontró audio', track.title)
      return
    }

    // Canción completa con yt-dlp (~7-9 s). Solo si tarda más de 10 s suena el
    // fragmento de relleno; al llegar la completa se empieza desde el principio.
    const fullJob = resolveAudio(track.artists, track.title)
    const filler = setTimeout(async () => {
      if (!alive()) return
      const clip = track.previewUrl || (await fetchPreview(track.title, track.artists))
      if (clip && alive() && resolvingRef.current) await start(clip, true).catch(() => {})
    }, 10000)

    let full = await fullJob
    clearTimeout(filler)
    if (!alive()) return
    if (!full) full = await resolveAudio(track.artists, track.title, true)
    if (!alive()) return
    setResolving(false)
    if (!full) {
      toast('No se encontró la canción completa', track.title)
      return
    }
    try {
      await start(full, false)
    } catch (e) {
      window.popApi?.debug(`fallo al reproducir: ${String(e instanceof Error ? e.message : e)}`)
    }
  }, [settings.volume, toast])

  const playAt = useCallback((index: number, tracks?: Track[]) => {
    const list = tracks || queue.tracks
    if (!list[index]) return
    setQueue((q) => ({ ...q, tracks: tracks || q.tracks, index }))
    void loadTrack(list[index])
  }, [queue.tracks, loadTrack])

  const play = useCallback((track: Track, context?: Track[]) => {
    if (context?.length) {
      const idx = context.findIndex((t) => t.id === track.id)
      playAt(idx >= 0 ? idx : 0, context)
    } else playAt(0, [track])
  }, [playAt])

  const advance = useCallback((direction: 1 | -1, auto = false) => {
    const { tracks, index, repeat } = queue
    if (!tracks.length) return
    const audio = audioRef.current
    if (direction === -1 && audio && audio.currentTime > 4) { audio.currentTime = 0; return }
    if (auto && repeat === 'one') { void loadTrack(tracks[index]); return }
    if (auto && !settings.autoplay) { setPlaying(false); return }
    let nextIndex = index + direction
    if (nextIndex >= tracks.length) {
      if (repeat === 'all') nextIndex = 0
      else { setPlaying(false); return }
    }
    if (nextIndex < 0) nextIndex = 0
    const go = () => playAt(nextIndex)
    if (settings.crossfade > 0 && playing && !auto) fadeOut(Math.min(1500, settings.crossfade * 250), go)
    else go()
  }, [queue, settings.crossfade, settings.autoplay, playing, fadeOut, playAt, loadTrack])

  // Sin fin: cuando quedan pocas canciones en la cola, se añaden parecidas.
  const extending = useRef(false)
  useEffect(() => {
    if (!settings.autoRadio || queue.repeat !== 'off' || extending.current) return
    const { tracks, index } = queue
    if (!tracks.length || index < tracks.length - 2) return
    extending.current = true
    const seed = tracks[tracks.length - 1]
    similarTo(seed, tracks.slice(-60), 25)
      .then((more) => {
        if (more.length) setQueue((q) => (q.tracks[q.tracks.length - 1]?.id === seed.id ? { ...q, tracks: [...q.tracks, ...more] } : q))
      })
      .catch(() => {})
      .finally(() => { extending.current = false })
  }, [queue, settings.autoRadio])

  // Transición: atenúa los últimos segundos antes de que termine sola.
  useEffect(() => {
    const audio = audioRef.current
    if (!audio || settings.crossfade <= 0 || !durationMs) return
    const left = durationMs - progress
    const span = settings.crossfade * 1000
    if (left < span && left > 0) audio.volume = settings.volume * Math.max(0, left / span)
    else if (audio.volume !== settings.volume && left >= span) audio.volume = settings.volume
  }, [progress, durationMs, settings.crossfade, settings.volume])

  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return
    const onTime = () => setProgress(audio.currentTime * 1000)
    const onMeta = () => { if (Number.isFinite(audio.duration) && audio.duration > 0) setDurationMs(audio.duration * 1000) }
    const onPlay = () => setPlaying(true)
    const onPause = () => setPlaying(false)
    const onErr = () => {
      setPlaying(false)
      if (!audio.error) return
      window.popApi?.debug(`error de audio ${audio.error.code} ${audio.error.message || ''}`)
      // La URL de googlevideo pudo caducar: se pide otra una sola vez.
      const track = currentRef.current
      if (track && hasFullAudio && retriedRef.current !== track.id) {
        retriedRef.current = track.id
        const at = audio.currentTime
        void resolveAudio(track.artists, track.title, true).then((url) => {
          if (!url || currentRef.current?.id !== track.id) return
          audio.src = url
          audio.addEventListener('loadedmetadata', () => { audio.currentTime = at }, { once: true })
          void audio.play()
        })
      }
    }
    // Si el fragmento termina mientras aún llega la completa, no se salta de canción.
    const onEnd = () => { if (!resolvingRef.current) advance(1, true) }
    audio.addEventListener('timeupdate', onTime)
    audio.addEventListener('loadedmetadata', onMeta)
    audio.addEventListener('durationchange', onMeta)
    audio.addEventListener('play', onPlay)
    audio.addEventListener('pause', onPause)
    audio.addEventListener('error', onErr)
    audio.addEventListener('ended', onEnd)
    return () => {
      audio.removeEventListener('timeupdate', onTime)
      audio.removeEventListener('loadedmetadata', onMeta)
      audio.removeEventListener('durationchange', onMeta)
      audio.removeEventListener('play', onPlay)
      audio.removeEventListener('pause', onPause)
      audio.removeEventListener('error', onErr)
      audio.removeEventListener('ended', onEnd)
    }
  }, [advance])

  const toggle = useCallback(() => {
    const audio = audioRef.current
    if (!current || !audio) return
    if (playing) { audio.pause(); setPlaying(false) }
    else if (audio.src) { void audio.play(); setPlaying(true) }
    else void loadTrack(current)
  }, [current, playing, loadTrack])

  const seek = useCallback((ms: number) => {
    if (audioRef.current) audioRef.current.currentTime = ms / 1000
    setProgress(ms)
  }, [])

  const toggleShuffle = useCallback(() => {
    setQueue((q) => {
      if (q.shuffle || q.tracks.length < 2) return { ...q, shuffle: !q.shuffle }
      // Mezcla lo que queda después de la actual; lo ya escuchado se queda atrás.
      const head = q.tracks.slice(0, q.index + 1)
      const rest = q.tracks.slice(q.index + 1)
      for (let i = rest.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [rest[i], rest[j]] = [rest[j], rest[i]]
      }
      return { ...q, shuffle: true, tracks: [...head, ...rest] }
    })
  }, [])

  const cycleRepeat = useCallback(() => {
    setQueue((q) => ({ ...q, repeat: q.repeat === 'off' ? 'all' : q.repeat === 'all' ? 'one' : 'off' }))
  }, [])

  // ----- letras (se piden al abrir la pantalla completa, una vez por canción) -----
  useEffect(() => {
    if (!current || !fullOpen || lyricsFor.current === current.id) return
    const id = current.id
    lyricsFor.current = id
    setLyricsLoading(true)
    setLyrics([])
    fetchLyrics(current.title, current.artists, realDuration(audioRef.current) || current.durationMs).then((res) => {
      if (lyricsFor.current !== id) return
      setLyrics(res.lines)
      setLyricsLoading(false)
    })
  }, [current, fullOpen])

  // ----- biblioteca -----
  const favIds = useMemo(() => new Set(library.favorites.map((t) => t.id)), [library.favorites])
  favIdsRef.current = favIds
  const fav = useCallback((track: Track) => {
    toast(favIds.has(track.id) ? 'Quitada de Me gusta' : 'Añadida a Me gusta', track.title)
    setLibrary((p) => toggleFavoriteInLibrary(p, track))
  }, [toast, favIds])
  const isFavorite = useCallback((id: string) => favIds.has(id), [favIds])

  const createPlaylist = useCallback((name: string, first?: Track) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
    setLibrary((p) => ({ ...p, playlists: [...p.playlists, { id, name, tracks: first ? [first] : [], createdAt: Date.now() }] }))
    toast('Playlist creada', name)
    return id
  }, [toast])

  const addToPlaylist = useCallback((playlistId: string, track: Track) => {
    setLibrary((p) => ({
      ...p,
      playlists: p.playlists.map((pl) =>
        pl.id === playlistId && !pl.tracks.some((t) => t.id === track.id) ? { ...pl, tracks: [...pl.tracks, track] } : pl),
    }))
    toast('Añadida a la playlist', track.title)
  }, [toast])

  const removeFromPlaylist = useCallback((playlistId: string, trackId: string) => {
    setLibrary((p) => ({
      ...p,
      playlists: p.playlists.map((pl) => (pl.id === playlistId ? { ...pl, tracks: pl.tracks.filter((t) => t.id !== trackId) } : pl)),
    }))
  }, [])

  const playNext = useCallback((track: Track) => {
    setQueue((q) => {
      if (!q.tracks.length) return { ...q, tracks: [track], index: 0 }
      const tracks = [...q.tracks]
      tracks.splice(q.index + 1, 0, track)
      return { ...q, tracks }
    })
    if (!current) void loadTrack(track)
    toast('Sonará a continuación', track.title)
  }, [current, loadTrack, toast])

  const addToQueue = useCallback((tracks: Track[]) => {
    if (!tracks.length) return
    setQueue((q) => ({ ...q, tracks: [...q.tracks, ...tracks], index: q.tracks.length ? q.index : 0 }))
    if (!current) void loadTrack(tracks[0])
    toast(tracks.length === 1 ? 'Añadida a la cola' : `${tracks.length} canciones añadidas a la cola`, tracks.length === 1 ? tracks[0].title : undefined)
  }, [current, loadTrack, toast])

  // ----- atajos de teclado -----
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const typing = e.target instanceof Element && e.target.closest('input, textarea, select')
      if (e.ctrlKey && e.key.toLowerCase() === 'k') { e.preventDefault(); navigate({ name: 'search' }); return }
      if (typing) return
      if (e.code === 'Space') { e.preventDefault(); toggle() }
      else if (e.ctrlKey && e.key === 'ArrowRight') advance(1)
      else if (e.ctrlKey && e.key === 'ArrowLeft') advance(-1)
      else if (e.key.toLowerCase() === 'l' && current) setFullOpen((v) => !v)
      else if (e.altKey && e.key === 'ArrowLeft') back()
    }
    const onMouse = (e: MouseEvent) => { if (e.button === 3) back() }
    document.addEventListener('keydown', onKey)
    document.addEventListener('mouseup', onMouse)
    return () => { document.removeEventListener('keydown', onKey); document.removeEventListener('mouseup', onMouse) }
  }, [toggle, advance, navigate, back, current])

  // Comandos de la superposición y teclas multimedia.
  const remote = useRef<(cmd: RemoteCommand) => void>(() => {})
  remote.current = (cmd) => {
    if (cmd === 'toggle') toggle()
    else if (cmd === 'next') advance(1)
    else if (cmd === 'prev') advance(-1)
    else if (cmd === 'like' && current) fav(current)
  }
  useEffect(() => {
    const off = window.popApi?.onRemoteCommand?.((cmd) => remote.current(cmd))
    if ('mediaSession' in navigator) {
      const ms = navigator.mediaSession
      ms.setActionHandler('play', () => remote.current('toggle'))
      ms.setActionHandler('pause', () => remote.current('toggle'))
      ms.setActionHandler('nexttrack', () => remote.current('next'))
      ms.setActionHandler('previoustrack', () => remote.current('prev'))
    }
    return () => off?.()
  }, [])

  // Doble clic: reproducir al instante o añadir a la cola (Ajustes → Reproducción).
  const activate = useCallback((track: Track, context?: Track[]) => {
    if (settings.doubleClick === 'queue') addToQueue([track])
    else play(track, context)
  }, [settings.doubleClick, addToQueue, play])

  const startRadio = useCallback(async (track: Track) => {
    toast('Creando radio…', track.title)
    const more = await similarTo(track, [], 40).catch(() => [])
    if (more.length) play(track, [track, ...more])
    else toast('No hay canciones parecidas disponibles')
  }, [play, toast])

  const actions: AppActions = useMemo(() => ({
    current, playing, resolving, playlists: library.playlists,
    play, activate, startRadio, prefetch, toggle, playNext, addToQueue, isFavorite, fav, addToPlaylist, removeFromPlaylist, createPlaylist, navigate, toast,
  }), [current, playing, resolving, library.playlists, play, activate, startRadio, prefetch, toggle, playNext, addToQueue, isFavorite, fav,
    addToPlaylist, removeFromPlaylist, createPlaylist, navigate, toast])

  const totalMs = durationMs || current?.durationMs || 0
  const routeKey = JSON.stringify(route)

  const page = (() => {
    switch (route.name) {
      case 'home': return <HomeView recent={library.recent} favorites={library.favorites} playlists={library.playlists} country={settings.country} />
      case 'search': return <SearchView country={settings.country} initial={route.q} />
      case 'library': return <LibraryView library={library} />
      case 'settings': return null
      case 'remote': return <RemotePlaylistView id={route.id} />
      case 'album': return <AlbumView id={route.id} />
      case 'artist': return <ArtistView id={route.id} title={route.title} />
      case 'liked': return <CollectionView kind="liked" tracks={library.favorites} />
      case 'recent': return <CollectionView kind="recent" tracks={library.recent} />
      case 'playlist': {
        const pl = library.playlists.find((p) => p.id === route.id)
        if (!pl) return <div className="page"><div className="emptyState"><p>Esta playlist ya no existe.</p></div></div>
        return (
          <PlaylistView
            playlist={pl}
            onRename={(name) => setLibrary((p) => ({ ...p, playlists: p.playlists.map((x) => (x.id === pl.id ? { ...x, name } : x)) }))}
            onDelete={() => {
              setLibrary((p) => ({ ...p, playlists: p.playlists.filter((x) => x.id !== pl.id) }))
              navigate({ name: 'library' })
              toast('Playlist eliminada', pl.name)
            }}
          />
        )
      }
    }
  })()

  return (
    <ArtQualityContext.Provider value={settings.artQuality}>
      <AppContext.Provider value={actions}>
        <div className="app" style={{ '--tint': settings.tint } as React.CSSProperties}>
          <audio ref={audioRef} preload="auto" />
          <div className="backdrop">
            {current && <div key={current.id} className="backdropArt" style={{ backgroundImage: `url(${hd(current.coverUrl, 300)})` }} />}
          </div>
          <ResizeEdges />

          <Sidebar route={route} favorites={library.favorites} playlists={library.playlists} />

          <main className="stage">
            <div className="dragBar" />
            <WindowControls />
            <div className="scroller" ref={scroller}>
              {history.length > 1 && (
                <button className="backBtn glassBtn" onClick={back} title="Atrás (Alt+←)">‹</button>
              )}
              <div className="routeView" key={routeKey}>{page}</div>
            </div>
          </main>

          <PlayerBar
              track={current}
              progress={progress}
              duration={totalMs}
              preview={preview}
              getTime={getAudioTime}
              volume={settings.volume}
              shuffle={queue.shuffle}
              repeat={queue.repeat}
              lyricsOpen={fullOpen}
              queueOpen={queueOpen}
              onSeek={seek}
              onPrev={() => advance(-1)}
              onNext={() => advance(1)}
              onShuffle={toggleShuffle}
              onRepeat={cycleRepeat}
              onVolume={(v) => setSettings((s) => ({ ...s, volume: v }))}
              onLyrics={() => current && setFullOpen(!fullOpen)}
              onQueue={() => setQueueOpen(!queueOpen)}
              onExpand={() => setFullOpen(true)}
            />

          {queueOpen && (
            <QueuePanel
              tracks={queue.tracks}
              index={queue.index}
              onPlay={(i) => playAt(i)}
              onRemove={(i) => setQueue((q) => ({ ...q, tracks: q.tracks.filter((_, n) => n !== i) }))}
              onClear={() => setQueue((q) => ({ ...q, tracks: q.tracks.slice(0, q.index + 1) }))}
              onClose={() => setQueueOpen(false)}
            />
          )}

          {fullOpen && current && (
            <FullPlayer
              track={current}
              lines={lyrics}
              loading={lyricsLoading}
              progress={progress}
              getTime={getAudioTime}
              duration={totalMs}
              volume={settings.volume}
              shuffle={queue.shuffle}
              repeat={queue.repeat}
              settings={settings}
              onSettings={setSettings}
              onSeek={seek}
              onPrev={() => advance(-1)}
              onNext={() => advance(1)}
              onShuffle={toggleShuffle}
              onRepeat={cycleRepeat}
              onVolume={(v) => setSettings((s) => ({ ...s, volume: v }))}
              onClose={() => setFullOpen(false)}
            />
          )}

          {settingsOpen && (
            <SettingsModal
              settings={settings}
              onChange={setSettings}
              onClose={() => setSettingsOpen(false)}
              onClearLibrary={() => { setLibrary({ favorites: [], recent: [], playlists: [] }); toast('Biblioteca borrada') }}
              onClearRecent={() => { setLibrary((l) => ({ ...l, recent: [] })); toast('Historial borrado') }}
              stats={{ favorites: library.favorites.length, recent: library.recent.length, playlists: library.playlists.length }}
            />
          )}

          <Toasts items={toasts} />
        </div>
      </AppContext.Provider>
    </ArtQualityContext.Provider>
  )
}
