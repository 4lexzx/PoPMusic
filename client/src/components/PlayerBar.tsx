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
  const scrubRef = useRef<number | null>(null)
  scrubRef.current = scrub
  const durRef = useRef(p.duration)
  durRef.current = p.duration

  // El avance se pinta en cada frame leyendo el audio, no a saltos de timeupdate.
  useEffect(() => {
    let raf = 0
    let shown = 0
    const tick = () => {
      const d = durRef.current
      const target = d > 0 ? Math.min(1, (scrubRef.current ?? p.getTime()) / d) : 0
      // Suavizado: al buscar o cambiar de canción se desliza en vez de saltar.
      shown += (target - shown) * (Math.abs(target - shown) > 0.002 ? 0.18 : 1)
      if (fill.current) fill.current.style.transform = shown > 0 ? `translateX(calc(${(shown - 1) * 100}% + 70px))` : 'translateX(-100%)'
      raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const ratioAt = (clientX: number) => {
    const r = card.current!.getBoundingClientRect()
    return Math.min(1, Math.max(0, (clientX - r.left) / r.width))
  }

  const onPointerDown = (e: React.PointerEvent) => {
    // Solo el fondo de la tarjeta sirve para buscar; los botones siguen siendo botones.
    if ((e.target as HTMLElement).closest('button, input, .noScrub') || !p.duration) return
    e.currentTarget.setPointerCapture(e.pointerId)
    setScrub(ratioAt(e.clientX) * p.duration)
  }
  const onPointerMove = (e: React.PointerEvent) => {
    if (scrub !== null) setScrub(ratioAt(e.clientX) * p.duration)
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
        <div className="playerTint" style={p.track ? { backgroundImage: `url(${p.track.coverUrl})` } : undefined} />
        <div className="playerFill" ref={fill} />
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
