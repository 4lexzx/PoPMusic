import { useEffect, useRef, useState } from 'react'
import { Pause, Play, SkipBack, SkipForward, Heart, X } from '../icons'
import type { NowPlaying } from '../electron.d'
import { applyTheme } from '../theme'

/**
 * Píldora flotante de la superposición (ventana aparte, siempre encima).
 * Recibe el estado de la ventana principal y le manda los botones pulsados.
 */
export default function Overlay() {
  const api = window.popApi
  const [state, setState] = useState<NowPlaying | null>(null)
  const [visible, setVisible] = useState(true)
  const ring = useRef<SVGCircleElement>(null)

  useEffect(() => {
    document.documentElement.classList.add('overlayRoot')
    applyTheme('dark', '')
    const offState = api?.onOverlayState?.(setState)
    const offVisible = api?.onOverlayVisible?.(setVisible)
    api?.overlayReady?.()
    return () => { offState?.(); offVisible?.() }
  }, [api])

  // Anillo de progreso alrededor de la carátula, a 60 fps.
  useEffect(() => {
    let raf = 0
    const C = 2 * Math.PI * 21
    const tick = () => {
      if (state && ring.current) {
        const pos = state.position + (state.playing ? Date.now() - state.at : 0)
        const pct = state.duration > 0 ? Math.min(1, pos / state.duration) : 0
        ring.current.style.strokeDashoffset = String(C * (1 - pct))
      }
      raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [state])

  const cmd = (c: Parameters<NonNullable<typeof api>['overlayCommand'] & {}>[0]) => api?.overlayCommand?.(c)
  const t = state?.track

  return (
    <div className={`pill ${visible ? 'in' : 'out'}`}>
      <button className="pillArt" onClick={() => cmd('open')} title="Abrir PoP Music">
        {t && <img src={t.coverUrl} alt="" />}
        <svg viewBox="0 0 48 48" className="pillRing">
          <circle cx="24" cy="24" r="21" className="track" />
          <circle cx="24" cy="24" r="21" className="bar" ref={ring}
            style={{ strokeDasharray: 2 * Math.PI * 21 }} />
        </svg>
      </button>
      <div className="pillMeta">
        <strong title={t?.title}>{t?.title || 'Nada sonando'}</strong>
        <span>{t?.artists}</span>
      </div>
      <div className="pillControls">
        <button onClick={() => cmd('like')} className={state?.liked ? 'on' : ''} title="Me gusta">
          <Heart size={16} fill={state?.liked ? 'currentColor' : 'none'} />
        </button>
        <button onClick={() => cmd('prev')} title="Anterior"><SkipBack size={17} fill="currentColor" /></button>
        <button className="pillPlay" onClick={() => cmd('toggle')} title={state?.playing ? 'Pausar' : 'Reproducir'}>
          {state?.playing ? <Pause size={17} fill="currentColor" /> : <Play size={17} fill="currentColor" />}
        </button>
        <button onClick={() => cmd('next')} title="Siguiente"><SkipForward size={17} fill="currentColor" /></button>
        <button className="pillClose" onClick={() => cmd('close')} title="Ocultar (Alt+Shift+O)"><X size={13} /></button>
      </div>
    </div>
  )
}
