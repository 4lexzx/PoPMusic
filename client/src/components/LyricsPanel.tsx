import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'
import { activeLine } from '../lyrics'
import type { LyricLine, Track } from '../types'

interface Props {
  track: Track | null
  lines: LyricLine[]
  loading: boolean
  positionMs: number
  onClose: () => void
}

export default function LyricsPanel({ track, lines, loading, positionMs, onClose }: Props) {
  const active = activeLine(lines, positionMs)
  const container = useRef<HTMLDivElement>(null)
  const activeRef = useRef<HTMLParagraphElement>(null)

  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }, [active])

  return (
    <div className="lyricsPanel">
      <div className="lyricsHead">
        <div>
          <span>Letras</span>
          {track && <small>{track.title}</small>}
        </div>
        <button onClick={onClose} title="Cerrar"><X size={18} /></button>
      </div>
      <div className="lyricsBody" ref={container}>
        {loading && <p className="lyricsHint">Buscando letra…</p>}
        {!loading && lines.length === 0 && (
          <p className="lyricsHint">No hay letra disponible para esta canción.</p>
        )}
        {lines.map((line, i) => (
          <p
            key={`${line.time}-${i}`}
            ref={i === active ? activeRef : undefined}
            className={`lyricLine${i === active ? ' active' : ''}${i < active ? ' past' : ''}`}
          >
            {line.text}
          </p>
        ))}
      </div>
    </div>
  )
}
