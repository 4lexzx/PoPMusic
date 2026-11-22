import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import type { DesktopTrackInfo } from '@/types/desktop'

/** Ventana flotante pequeña: muestra la pista y permite play/pausa y salto. */
export function OverlayPage() {
  const [track, setTrack] = useState<DesktopTrackInfo>({
    title: 'PoP Music',
    artist: '',
    thumbnail: '',
    playing: false,
  })

  useEffect(() => {
    if (!window.popDesktop) return
    const off = window.popDesktop.onTrack(setTrack)
    return () => {
      off()
    }
  }, [])

  const command = (name: 'play-pause' | 'next' | 'prev') => window.popDesktop?.sendCommand(name)

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.94 }}
      animate={{ opacity: 1, scale: 1 }}
      className="overlay-card"
    >
      {track.thumbnail ? (
        <img className="overlay-card__art" src={track.thumbnail} alt="" />
      ) : (
        <div className="overlay-card__art overlay-card__art--empty" />
      )}
      <div className="overlay-card__meta">
        <span className="overlay-card__title">{track.title}</span>
        <span className="overlay-card__artist">{track.artist || 'PoP Music'}</span>
      </div>
      <div className="overlay-card__actions">
        <button type="button" onClick={() => command('prev')} aria-label="Anterior">
          ‹‹
        </button>
        <button type="button" onClick={() => command('play-pause')} aria-label="Reproducir o pausar">
          {track.playing ? '❚❚' : '▶'}
        </button>
        <button type="button" onClick={() => command('next')} aria-label="Siguiente">
          ››
        </button>
        <button type="button" onClick={() => window.popDesktop?.hideOverlay()} aria-label="Ocultar">
          ×
        </button>
      </div>
    </motion.div>
  )
}
