import { useEffect, useRef, useState } from 'react'
import {
  Play, Pause, SkipBack, SkipForward, Shuffle, Repeat, Repeat1, Heart, MicVocal, ListMusic,
  Volume2, Volume1, VolumeX, Maximize2, LoaderCircle,
} from '../icons'
import type { Track } from '../types'
import { useApp } from '../ui'
import { Cover } from './Cover'

interface Props {
  track: Track | null
  progress: number
  duration: number
  /** Posición exacta del audio en ms, para animar el avance a 60 fps. */
  getTime: () => number
  /** Duración real del audio que suena (0 si aún no se sabe). */
  getDuration: () => number
  preview: boolean
  volume: number
  shuffle: boolean
  repeat: 'off' | 'all' | 'one'
  lyricsOpen: boolean
  queueOpen: boolean
  onSeek: (ms: number) => void
  onPrev: () => void
  onNext: () => void
  onShuffle: () => void
  onRepeat: () => void
  onVolume: (v: number) => void
  onLyrics: () => void
  onQueue: () => void
  onExpand: () => void
}

/**
 * Reproductor flotante. No usa la clásica barra de progreso: la propia
 * tarjeta se va llenando de luz con el avance y se arrastra sobre ella para
 * buscar dentro de la canción.
 */
export default function PlayerBar(p: Props) {
  const app = useApp()
  const card = useRef<HTMLDivElement>(null)
  const [scrub, setScrub] = useState<number | null>(null)
  const fill = useRef<HTMLDivElement>(null)
  const edge = useRef<HTMLDivElement>(null)
  const scrubRef = useRef<number | null>(null)
  scrubRef.current = scrub
  const durRef = useRef(p.duration)
  durRef.current = p.duration
  const loadingRef = useRef(app.resolving)
  loadingRef.current = app.resolving
  const trackRef = useRef(p.track?.id)
  const tickRef = useRef<() => void>(() => {})
  // Al arrastrar para buscar, el relleno sigue al ratón al instante.
  useEffect(() => { if (scrub !== null) tickRef.current() }, [scrub])

  /**
   * Avance a 60 fps: toda la tarjeta se va llenando de luz de izquierda a
   * derecha según la duración real del audio (como Liner). Mientras carga la
   * siguiente canción se queda en 0 (antes leía el tiempo de la anterior).
   */
  useEffect(() => {
    let shown = 0
    let lastTrack = trackRef.current
    // El ancho se guarda aparte: leer clientWidth en cada actualización obligaba
    // a recalcular el diseño de toda la página.
    let w = card.current?.clientWidth || 0
    const ro = new ResizeObserver((entries) => { w = entries[0]?.contentRect.width || w })
    if (card.current) ro.observe(card.current)
    let lastPx = -1
    const paint = (value: number, jump: boolean) => {
      // Solo se pinta si el borde se mueve al menos 1 px (una canción avanza
      // ~1 px cada 150 ms): nada de animar 144 veces por segundo sin necesidad.
      const px = Math.round(value * w)
      if (px === lastPx && !jump) return
      lastPx = px
      const t = jump ? 'transform 0.35s cubic-bezier(0.22, 1, 0.36, 1)' : 'none'
      if (fill.current) { fill.current.style.transition = t; fill.current.style.transform = `scaleX(${value.toFixed(4)})` }
      if (edge.current) {
        edge.current.style.transition = jump ? `${t}, opacity 0.3s` : 'opacity 0.3s'
        edge.current.style.transform = `translateX(${px - 48}px)`
        edge.current.style.opacity = value > 0.002 && value < 0.998 ? '1' : '0'
      }
    }
    const tick = () => {
      if (trackRef.current !== lastTrack) { lastTrack = trackRef.current; shown = 0; paint(0, true) }
      const d = p.getDuration() || durRef.current
      const time = scrubRef.current ?? (loadingRef.current ? 0 : p.getTime())
      const target = d > 0 ? Math.min(1, Math.max(0, time / d)) : 0
      const jump = Math.abs(target - shown) > 0.02 && scrubRef.current === null
      shown = target
      paint(shown, jump)
    }
    tick()
    tickRef.current = tick
    const timer = window.setInterval(tick, 100)
    return () => { clearInterval(timer); ro.disconnect() }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  trackRef.current = p.track?.id

  const ratioAt = (clientX: number) => {
    const r = card.current!.getBoundingClientRect()
    return Math.min(1, Math.max(0, (clientX - r.left) / r.width))
  }

  const onPointerDown = (e: React.PointerEvent) => {
    // Solo el fondo de la tarjeta sirve para buscar; los botones siguen siendo botones.
    const d = p.getDuration() || p.duration
    if ((e.target as HTMLElement).closest('button, input, .noScrub') || !d) return
    e.currentTarget.setPointerCapture(e.pointerId)
    setScrub(ratioAt(e.clientX) * d)
  }
  const onPointerMove = (e: React.PointerEvent) => {
    if (scrub !== null) setScrub(ratioAt(e.clientX) * (p.getDuration() || p.duration))
  }
  const onPointerUp = () => {
    if (scrub !== null) p.onSeek(scrub)
    setScrub(null)
  }

  const liked = p.track ? app.isFavorite(p.track.id) : false
  const VolIcon = p.volume === 0 ? VolumeX : p.volume < 0.5 ? Volume1 : Volume2

  return (
    <div className="dock">
      <div
        ref={card}
        className={`playerCard ${scrub !== null ? 'scrubbing' : ''}`}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
      >
        <div className="playerFill" ref={fill} aria-hidden="true" />
        <div className="playerEdge" ref={edge} aria-hidden="true" />
        <div className="pcLeft">
          <button className="pcArt swapIn" key={`art-${p.track?.id}`} onClick={p.onExpand} title="Abrir reproductor" disabled={!p.track}>
            <Cover src={p.track?.coverUrl} size={64} />
            <span className="pcArtHover"><Maximize2 size={18} /></span>
          </button>
          <div className="pcMeta noScrub swapIn" key={p.track?.id || "none"}>
            <span className="pcTitle">
              {p.track?.title || 'Nada sonando'}
              {p.preview && <b className="previewTag" title="Fragmento de 30 s: la canción completa solo suena en la app de escritorio">Vista previa</b>}
            </span>
            <button
              className="pcArtist link"
              onClick={() => p.track?.artistId && app.navigate({ name: 'artist', id: p.track.artistId, title: p.track.artists })}
            >{p.track?.artists || 'Elige una canción para empezar'}</button>
          </div>
          {p.track && (
            <button className={`iconBtn like ${liked ? 'on' : ''}`} onClick={() => app.fav(p.track!)} title="Me gusta">
              <Heart size={18} fill={liked ? 'currentColor' : 'none'} />
            </button>
          )}
        </div>

        <div className="pcCenter">
          <button className="iconBtn" onClick={p.onPrev} title="Anterior"><SkipBack size={19} fill="currentColor" /></button>
          <button className="pcPlay" onClick={app.toggle} title={app.playing ? 'Pausar' : 'Reproducir'} disabled={!p.track}>
            {app.resolving
              ? <LoaderCircle className="spin" size={18} />
              : app.playing ? <Pause size={18} fill="currentColor" /> : <Play size={18} fill="currentColor" />}
          </button>
          <button className="iconBtn" onClick={p.onNext} title="Siguiente"><SkipForward size={19} fill="currentColor" /></button>
        </div>

        <div className="pcRight">
          <div className="segGroup">
            <button className={p.shuffle ? 'on' : ''} onClick={p.onShuffle} title="Aleatorio"><Shuffle size={16} /></button>
            <button className={p.repeat !== 'off' ? 'on' : ''} onClick={p.onRepeat}
              title={p.repeat === 'one' ? 'Repetir una' : p.repeat === 'all' ? 'Repetir todo' : 'Repetir'}>
              {p.repeat === 'one' ? <Repeat1 size={16} /> : <Repeat size={16} />}
            </button>
            <button className={p.lyricsOpen ? 'on' : ''} onClick={p.onLyrics} title="Letras"><MicVocal size={16} /></button>
            <button className={p.queueOpen ? 'on' : ''} onClick={p.onQueue} title="Cola"><ListMusic size={16} /></button>
          </div>
          <div className="volume noScrub">
            <button className="iconBtn" onClick={() => p.onVolume(p.volume > 0 ? 0 : 0.8)} title="Silenciar">
              <VolIcon size={17} />
            </button>
            <div className="volPop">
              <input
                type="range" min="0" max="1" step="0.01" value={p.volume}
                style={{ '--v': `${p.volume * 100}%` } as React.CSSProperties}
                onChange={(e) => p.onVolume(Number(e.target.value))}
              />
              <span>{Math.round(p.volume * 100)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
