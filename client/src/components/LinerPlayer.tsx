import React from 'react'
import { Play, Pause } from 'lucide-react'

// Liner-identical player component - exact layout and function as Liner Music Player
export default function LinerPlayer({ current, playing, onPlayPrev, onPlayNext, onTogglePlay, onSeek, onVolumeChange, onRepeat, onShuffle, currentTime, duration }: {
  current: any
  playing: boolean
  onPlayPrev: () => void
  onPlayNext: () => void
  onTogglePlay: () => void
  onSeek: (ms: number) => void
  onVolumeChange: (v: number) => void
  onRepeat: () => void
  onShuffle: () => void
  currentTime: number
  duration: number
}) {
  const [isPlaying, setIsPlaying] = React.useState(playing)

  return (
    <div className="liner-player">
      {/* Portada y título */}
      <div className="liner-player-top">
        <img className="liner-cover" src={current?.coverUrl || 'https://placehold.co/56x56/161618/ffffff?text=+'} alt={current?.title || 'Tocando ahora'} loading="lazy" />
        <div className="liner-player-info">
          <div className="liner-title">{current?.title || 'Tocando ahora'}</div>
          <div className="liner-artist">{current?.artists || ''}</div>
        </div>
      </div>

      {/* Controles de reproducción */}
      <div className="liner-player-controls">
        <button className="liner-btn-prev" onClick={onPlayPrev} title="Anterior">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <button className="liner-btn-play pause-btn" onClick={onTogglePlay} title={isPlaying ? 'Pausar' : 'Reproducir'}>
          {isPlaying ? (
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>
          ) : (
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 18 12 12 18"/></svg>
          )}
        </button>
        <button className="liner-btn-next" onClick={onPlayNext} title="Siguiente">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </div>

      {/* Barra de progreso y tiempo */}
      <div className="liner-player-progress">
        <div className="liner-progress-bar">
          <div className="liner-progress-fill" style={{ width: `${(currentTime / duration) * 100}%` }} />
        </div>
        <div className="liner-time-code">
          <span>{formatTime(currentTime)}</span>
          <span>{formatTime(duration)}</span>
        </div>
      </div>

      {/* Controles inferiores: repetir, aleatorio, volumen */}
      <div className="liner-player-bottom">
        <button className="liner-btn-repeat" onClick={onRepeat} title="Repetir">
          {/* Icono dependiente de estado repeat */}
        </button>
        <button className="liner-btn-shuffle" onClick={onShuffle} title="Aleatorio">
          {/* Icono dependiente de estado shuffle */}
        </button>
        <button className="liner-btn-vol" title="Volumen">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polygon points="12 3 19 12 12 21"/><polygon points="12 3 5 12 12 21"/></svg>
        </button>
        <input
          type="range"
          min="0"
          max="1"
          step="0.01"
          value="0.8"
          onChange={(e) => onVolumeChange(Number(e.target.value))}
          className="liner-vol-slider"
        />
      </div>
    </div>
  )
}

// Helper function for time formatting - idéntico a Liner
function formatTime(ms: number): string {
  const total = Math.floor(ms / 1000)
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}