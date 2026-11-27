import { memo, useEffect, useRef, useState } from 'react'
import {
  ChevronDown, Play, Pause, SkipBack, SkipForward, Shuffle, Repeat, Repeat1, Heart, Volume2, VolumeX,
  LoaderCircle, Minus, Plus, Timer, Sparkles, Check,
} from '../icons'
import type { LyricLine, LyricsEffect, Settings, Track } from '../types'
import { activeLine } from '../lyrics'
import { formatTime, useApp } from '../ui'
import { hd } from '../api'
import { Cover } from './Cover'
import { WindowControls } from './Chrome'

interface Props {
  track: Track | null
  lines: LyricLine[]
  loading: boolean
  progress: number
  duration: number
  /** Posición exacta del audio en ms (para sincronizar la letra a 60 fps). */
  getTime: () => number
  volume: number
  shuffle: boolean
  repeat: 'off' | 'all' | 'one'
  settings: Settings
  onSettings: (s: Settings) => void
  onSeek: (ms: number) => void
  onPrev: () => void
  onNext: () => void
  onShuffle: () => void
  onRepeat: () => void
  onVolume: (v: number) => void
  onClose: () => void
}

/** Pantalla completa: carátula y controles a la izquierda, letra a la derecha. */
export default function FullPlayer(p: Props) {
  const app = useApp()
  const [closing, setClosing] = useState(false)
  const close = () => { setClosing(true); setTimeout(p.onClose, 220) }

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  })

  const synced = p.lines.some((l) => l.time >= 0)
  const hasLyrics = p.lines.length > 0
  const liked = p.track ? app.isFavorite(p.track.id) : false
  const pct = p.duration > 0 ? Math.min(100, (p.progress / p.duration) * 100) : 0

  return (
    <div className={`fullPlayer forceDark ${closing ? 'closing' : ''} ${hasLyrics || p.loading ? '' : 'noLyrics'}`}>
      <div className="fpBackdrop" style={p.track ? { backgroundImage: `url(${hd(p.track.coverUrl, 120)})` } : undefined} />
      <div className="fpShade" />

      <header className="fpTop">
        <button className="glassBtn" onClick={close} title="Cerrar (Esc)"><ChevronDown size={20} /></button>
        {synced && (
          <EffectPicker value={p.settings.lyricsEffect} onChange={(v) => p.onSettings({ ...p.settings, lyricsEffect: v })} />
        )}
        {synced && (
          <div className="offsetPill" title="Ajusta la sincronización de la letra">
            <Timer size={14} />
            <button onClick={() => p.onSettings({ ...p.settings, lyricsOffset: p.settings.lyricsOffset - 250 })}><Minus size={13} /></button>
            <span>{(p.settings.lyricsOffset / 1000).toFixed(2)} s</span>
            <button onClick={() => p.onSettings({ ...p.settings, lyricsOffset: p.settings.lyricsOffset + 250 })}><Plus size={13} /></button>
          </div>
        )}
        <WindowControls />
      </header>

      <section className="fpLeft">
        <Cover src={p.track?.coverUrl} className="fpArt" />
        <div className="fpHead">
          <div>
            <h2>{p.track?.title || 'Nada sonando'}</h2>
            <button
              className="link"
              onClick={() => { if (p.track?.artistId) { app.navigate({ name: 'artist', id: p.track.artistId, title: p.track.artists }); close() } }}
            >{p.track?.artists}</button>
          </div>
          {p.track && (
            <button className={`glassBtn round like ${liked ? 'on' : ''}`} onClick={() => app.fav(p.track!)} title="Me gusta">
              <Heart size={18} fill={liked ? 'currentColor' : 'none'} />
            </button>
          )}
        </div>
        <div className="fpProgress">
          <input
            type="range" min="0" max={Math.max(1, p.duration)} value={Math.min(p.progress, p.duration)}
            style={{ '--v': `${pct}%` } as React.CSSProperties}
            onChange={(e) => p.onSeek(Number(e.target.value))}
          />
          <div className="fpTimes"><span>{formatTime(p.progress)}</span><span>-{formatTime(p.duration - p.progress)}</span></div>
        </div>
        <div className="fpControls">
          <button className={`iconBtn ${p.shuffle ? 'on' : ''}`} onClick={p.onShuffle} title="Aleatorio"><Shuffle size={20} /></button>
          <button className="iconBtn big" onClick={p.onPrev} title="Anterior"><SkipBack size={28} fill="currentColor" /></button>
          <button className="fpPlay" onClick={app.toggle} title={app.playing ? 'Pausar' : 'Reproducir'}>
            {app.resolving ? <LoaderCircle className="spin" size={30} />
              : app.playing ? <Pause size={32} fill="currentColor" /> : <Play size={32} fill="currentColor" />}
          </button>
          <button className="iconBtn big" onClick={p.onNext} title="Siguiente"><SkipForward size={28} fill="currentColor" /></button>
          <button className={`iconBtn ${p.repeat !== 'off' ? 'on' : ''}`} onClick={p.onRepeat} title="Repetir">
            {p.repeat === 'one' ? <Repeat1 size={20} /> : <Repeat size={20} />}
          </button>
        </div>
        <div className="fpVolume">
          <button className="iconBtn" onClick={() => p.onVolume(p.volume > 0 ? 0 : 0.8)}>
            {p.volume === 0 ? <VolumeX size={17} /> : <Volume2 size={17} />}
          </button>
          <input
            type="range" min="0" max="1" step="0.01" value={p.volume}
            style={{ '--v': `${p.volume * 100}%` } as React.CSSProperties}
            onChange={(e) => p.onVolume(Number(e.target.value))}
          />
        </div>
      </section>

      {(hasLyrics || p.loading) && (
        <Lyrics
          lines={p.lines}
          loading={p.loading}
          getTime={p.getTime}
          offset={p.settings.lyricsOffset}
          synced={synced}
          size={p.settings.lyricsSize}
          blur={p.settings.lyricsBlur && !p.settings.reduceMotion}
          effect={p.settings.lyricsEffect || 'karaoke'}
          onSeek={p.onSeek}
        />
      )}
    </div>
  )
}

/** Adelanto con el que se ilumina cada línea (el ojo la ve justo cuando suena). */
const LEAD_MS = 120
/** Adelanto por palabra: muy pequeño, para que no se adelante al canto. */
const WORD_LEAD_MS = 40
/** Silencio mínimo entre líneas para mostrar los puntos de pausa. */
const GAP_MS = 3500

export const EFFECTS: { id: LyricsEffect; name: string; hint: string }[] = [
  { id: 'karaoke', name: 'Karaoke', hint: 'Cada palabra se rellena mientras se canta' },
  { id: 'glow', name: 'Brillo', hint: 'Las palabras se encienden y brillan al sonar' },
  { id: 'bounce', name: 'Rebote', hint: 'Cada palabra salta un poco al cantarse' },
  { id: 'fade', name: 'Aparecer', hint: 'Las palabras aparecen de la nada' },
  { id: 'line', name: 'Por línea', hint: 'Se resalta la frase completa' },
]

/** Selector compacto de efecto de letra (un botón que despliega la lista). */
function EffectPicker({ value, onChange }: { value: LyricsEffect; onChange: (v: LyricsEffect) => void }) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!open) return
    const close = (e: MouseEvent) => { if (!ref.current?.contains(e.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])
  const current = EFFECTS.find((e) => e.id === value) || EFFECTS[0]
  return (
    <div className="fxPicker" ref={ref}>
      <button className={`fxBtn ${open ? 'on' : ''}`} onClick={() => setOpen(!open)} title="Efecto de la letra">
        <Sparkles size={15} weight="fill" /><span>{current.name}</span><ChevronDown size={12} />
      </button>
      {open && (
        <div className="fxMenu">
          {EFFECTS.map((e) => (
            <button key={e.id} className={e.id === value ? 'on' : ''} onClick={() => { onChange(e.id); setOpen(false) }}>
              <span className={`fxSample fx-${e.id}`}><i>A</i><i>a</i></span>
              <span className="fxText"><strong>{e.name}</strong><small>{e.hint}</small></span>
              {e.id === value && <Check size={14} />}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

const Lyrics = memo(function Lyrics({ lines, loading, getTime, offset, synced, size, blur, effect, onSeek }: {
  lines: LyricLine[]; loading: boolean; getTime: () => number; offset: number; synced: boolean
  size: Settings['lyricsSize']; blur: boolean; effect: LyricsEffect; onSeek: (ms: number) => void
}) {
  const [active, setActive] = useState(-1)
  const [gap, setGap] = useState(-1)
  const box = useRef<HTMLDivElement>(null)
  const refs = useRef<(HTMLElement | null)[]>([])
  const gapRefs = useRef<(HTMLElement | null)[]>([])
  const intro = useRef<HTMLDivElement>(null)
  const userScroll = useRef(0)
  const activeRef = useRef(-1)
  const gapRef = useRef(-1)
  const scrollAnim = useRef(0)

  // Desplazamiento suave propio (más natural que scroll-behavior: smooth).
  const glideTo = (top: number, instant = false) => {
    const el = box.current
    if (!el) return
    cancelAnimationFrame(scrollAnim.current)
    if (instant) { el.scrollTop = top; return }
    const from = el.scrollTop
    const t0 = performance.now()
    const step = (now: number) => {
      const k = Math.min(1, (now - t0) / 650)
      el.scrollTop = from + (top - from) * (1 - Math.pow(1 - k, 4))
      if (k < 1) scrollAnim.current = requestAnimationFrame(step)
    }
    scrollAnim.current = requestAnimationFrame(step)
  }

  // Reloj de la letra: se lee el audio en cada fotograma (60 fps).
  useEffect(() => {
    if (!synced) return
    let raf = 0
    let lastPos = 0
    let lastWordsLine = -1
    let spanCache: { line: number; spans: HTMLElement[] } = { line: -1, spans: [] }
    const tick = () => {
      const pos = getTime() + offset
      const jumped = Math.abs(pos - lastPos) > 1500
      lastPos = pos
      const idx = activeLine(lines, pos + LEAD_MS)
      const line = lines[idx]
      const next = lines[idx + 1]
      // Pausa larga después de una línea con final conocido → puntos.
      const g = line && line.end && next && next.time - line.end > GAP_MS && pos > line.end + 400 ? idx : -1

      if (idx !== activeRef.current || g !== gapRef.current) {
        const changedLine = idx !== activeRef.current
        activeRef.current = idx
        gapRef.current = g
        setActive(idx)
        setGap(g)
        const target = g >= 0 ? gapRefs.current[g] : refs.current[Math.max(0, idx)]
        if (target && box.current && Date.now() - userScroll.current > 2500 && (changedLine || g >= 0)) {
          glideTo(target.offsetTop - box.current.clientHeight * 0.34, jumped)
        }
      }

      if (line) {
        const el = refs.current[idx]
        // Palabra por palabra: progreso de cada una según su inicio y su duración
        // (si una nota se sostiene, su palabra tarda más en llenarse).
        if (el && line.words?.length) {
          if (spanCache.line !== idx) spanCache = { line: idx, spans: Array.from(el.querySelectorAll<HTMLElement>('.w')) }
          const spans = spanCache.spans
          const wpos = pos + WORD_LEAD_MS
          line.words.forEach((w, k) => {
            const p = Math.min(1, Math.max(0, (wpos - w.t) / Math.max(1, w.d || 300)))
            const sp = spans[k]
            if (sp && sp.dataset.p !== p.toFixed(3)) {
              sp.dataset.p = p.toFixed(3)
              sp.style.setProperty('--wp', p.toFixed(3))
              sp.classList.toggle('sung', p >= 1)
              sp.classList.toggle('singing', p > 0 && p < 1)
            }
          })
          // La línea anterior queda completa al pasar a la siguiente.
          if (lastWordsLine !== idx && lastWordsLine >= 0) {
            refs.current[lastWordsLine]?.querySelectorAll<HTMLElement>('.w').forEach((sp) => {
              sp.style.setProperty('--wp', '1'); sp.dataset.p = '1'; sp.classList.add('sung'); sp.classList.remove('singing')
            })
          }
          lastWordsLine = idx
        }
        // Progreso de la línea entera (efecto "por línea" y pausas).
        const end = line.end || (next ? next.time : line.time + 5000)
        el?.style.setProperty('--p', Math.min(1, Math.max(0, (pos - line.time) / Math.max(1, end - line.time))).toFixed(4))
        if (g >= 0 && next) {
          gapRefs.current[g]?.style.setProperty('--p', Math.min(1, Math.max(0, (pos - line.end!) / (next.time - line.end!))).toFixed(4))
        }
      }
      if (intro.current && lines[0]) {
        intro.current.style.setProperty('--p', Math.min(1, Math.max(0, pos / lines[0].time)).toFixed(4))
      }
      raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [lines, synced, offset, getTime])

  if (loading) {
    return <section className="fpLyrics"><div className="lyricsDots"><i /><i /><i /></div></section>
  }

  const showIntro = synced && lines[0] && lines[0].time > 2500 && active < 0
  const byWord = synced && effect !== 'line'

  return (
    <section
      className={`fpLyrics size-${size} ${synced ? 'synced' : 'plain'} fx-${effect}`}
      ref={box}
      onWheel={() => { userScroll.current = Date.now() }}
    >
      <div className="lyricsPad" />
      {synced && lines[0] && lines[0].time > 2500 && (
        <div ref={intro} className={`interlude ${showIntro ? 'on' : ''}`}><i /><i /><i /></div>
      )}
      {lines.map((line, i) => {
        const dist = i - active
        const state = i === active && gap !== i ? 'now' : i <= active ? 'past' : 'next'
        const style: React.CSSProperties = synced
          ? {
            transitionDelay: `${Math.min(6, Math.max(0, dist)) * 45}ms`,
            filter: blur && active >= 0 && state !== 'now' && Math.abs(dist) <= 5 ? `blur(${Math.min(2.5, Math.abs(dist) * 0.6)}px)` : undefined,
          }
          : {}
        const next = lines[i + 1]
        const hasGap = synced && line.end && next && next.time - line.end > GAP_MS
        if (!line.text) {
          return (
            <div key={`${line.time}-${i}`} ref={(el) => { refs.current[i] = el }}
              className={`interlude ${i === active ? 'on' : ''}`}><i /><i /><i /></div>
          )
        }
        return (
          <div key={`${line.time}-${i}`} className="lyricBlock">
            <p
              ref={(el) => { refs.current[i] = el }}
              className={`lyric ${synced ? state : ''} ${byWord && line.words?.length ? 'words' : ''}`}
              style={style}
              onClick={() => synced && line.time >= 0 && onSeek(Math.max(0, line.time - offset))}
            >
              {byWord && line.words?.length
                ? line.words.map((w, k) => <span key={k} className="w" data-text={w.text}>{w.text}</span>)
                : <span className="lyricText">{line.text}</span>}
            </p>
            {hasGap && (
              <div ref={(el) => { gapRefs.current[i] = el }} className={`interlude ${gap === i ? 'on' : ''}`}><i /><i /><i /></div>
            )}
          </div>
        )
      })}
      <div className="lyricsPad" />
    </section>
  )
})
