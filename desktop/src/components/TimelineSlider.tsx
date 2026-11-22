import { memo, useCallback, useRef, useState } from 'react'
import { motion } from 'framer-motion'
import { formatDuration } from '@/shared/utils/time'

interface TimelineSliderProps {
  position: number
  duration: number
  onSeek: (seconds: number) => void
  size?: 'xs' | 'sm' | 'md' | 'lg'
  showTime?: boolean
  className?: string
  fillClassName?: string
  trackBg?: string
  trackActiveBg?: string
}

const CONTAINER_HEIGHT = { xs: 'h-[16px]', sm: 'h-[20px]', md: 'h-[24px]', lg: 'h-[28px]' }
const TRACK_HEIGHT = { xs: 'h-[4px]', sm: 'h-[5px]', md: 'h-[6px]', lg: 'h-[7px]' }
const SCALE_Y = { xs: 1.5, sm: 1.4, md: 1.35, lg: 1.3 }

/** Línea de tiempo estilo Liner: sin punto, se engrosa al pasar el ratón. */
export const TimelineSlider = memo(function TimelineSlider({
  position,
  duration,
  onSeek,
  size = 'md',
  showTime = false,
  className = '',
  fillClassName = '',
  trackBg,
  trackActiveBg,
}: TimelineSliderProps) {
  const [hovered, setHovered] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [scrub, setScrub] = useState<number | null>(null)
  const trackRef = useRef<HTMLDivElement>(null)

  const total = Math.max(0, duration)
  const current = scrub ?? Math.min(Math.max(0, position), total || Infinity)
  const shown = dragging ? current : Math.floor(current)
  const percentage = total > 0 ? Math.min(100, Math.max(0, (shown / total) * 100)) : 0

  const positionFromX = useCallback(
    (clientX: number) => {
      const rect = trackRef.current?.getBoundingClientRect()
      if (!rect || rect.width === 0) return current
      const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width))
      return ratio * total
    },
    [current, total],
  )

  const handlePointerDown = useCallback(
    (event: React.PointerEvent<HTMLDivElement>) => {
      event.stopPropagation()
      event.preventDefault()
      setDragging(true)
      const initial = positionFromX(event.clientX)
      setScrub(initial)

      const move = (ev: PointerEvent) => setScrub(positionFromX(ev.clientX))
      const up = (ev: PointerEvent) => {
        const final = positionFromX(ev.clientX)
        setDragging(false)
        setScrub(null)
        window.removeEventListener('pointermove', move)
        window.removeEventListener('pointerup', up)
        window.removeEventListener('pointercancel', up)
        onSeek(final)
      }
      window.addEventListener('pointermove', move)
      window.addEventListener('pointerup', up)
      window.addEventListener('pointercancel', up)
    },
    [positionFromX, onSeek],
  )

  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      if (total <= 0) return
      if (event.key === 'ArrowRight') {
        event.preventDefault()
        onSeek(Math.min(total, current + 5))
      } else if (event.key === 'ArrowLeft') {
        event.preventDefault()
        onSeek(Math.max(0, current - 5))
      } else if (event.key === 'Home') {
        event.preventDefault()
        onSeek(0)
      } else if (event.key === 'End') {
        event.preventDefault()
        onSeek(total)
      }
    },
    [current, total, onSeek],
  )

  const active = hovered || dragging

  return (
    <div
      className={`group relative flex flex-col ${CONTAINER_HEIGHT[size]} ${className}`}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <div
        role="slider"
        aria-label="Progreso"
        aria-valuemin={0}
        aria-valuemax={Math.round(total)}
        aria-valuenow={Math.round(current)}
        tabIndex={0}
        onKeyDown={handleKeyDown}
        onPointerDown={handlePointerDown}
        className="relative flex h-full flex-1 cursor-pointer items-center outline-none"
      >
        <motion.div
          ref={trackRef}
          initial={false}
          animate={{
            scaleY: active ? SCALE_Y[size] : 1,
            backgroundColor: active
              ? trackActiveBg ?? 'rgba(255,255,255,0.30)'
              : trackBg ?? 'rgba(255,255,255,0.16)',
          }}
          transition={{ type: 'spring', stiffness: 400, damping: 30, mass: 0.8 }}
          className={`relative w-full origin-center overflow-hidden rounded-full ${TRACK_HEIGHT[size]}`}
        >
          <div
            className={`absolute inset-0 rounded-full ${fillClassName || 'bg-white'}`}
            style={{
              transform: `translateX(${percentage - 100}%)`,
              transition: dragging
                ? 'none'
                : 'transform 600ms cubic-bezier(0.22, 1, 0.36, 1)',
            }}
          />
        </motion.div>
      </div>

      {showTime ? (
        <div className="mt-[2px] flex items-center justify-between font-mono text-[11px] font-medium tabular-nums text-white/70">
          <span>{formatDuration(scrub ?? position)}</span>
          <span>{formatDuration(duration)}</span>
        </div>
      ) : null}
    </div>
  )
})
