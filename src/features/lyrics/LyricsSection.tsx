import { useEffect, useMemo, useRef, useState } from 'react'
import { CloseIcon, MinusIcon, MusicIcon, PlusIcon } from '@/components/Icons'
import { Portal, Spinner } from '@/components/ui'
import { DynamicBackground, useArtworkColors } from '@/features/visuals/DynamicBackground'
import type { Lyrics } from '@/shared/types/lyrics'
import type { Track } from '@/shared/types/track'
import { findActiveLine } from '@/shared/utils/lrc'

const OFFSET_STEP = 0.5
const OFFSET_LIMIT = 10

interface LyricsScrollerProps {
  lyrics: Lyrics
  time: number
  offset?: number
  size?: 'md' | 'lg'
  className?: string
}

/** Letras con resaltado tipo karaoke: línea activa nítida, resto difuminado. */
export function LyricsScroller({
  lyrics,
  time,
  offset = 0,
  size = 'md',
  className = '',
}: LyricsScrollerProps) {
  const adjusted = time + offset
  const activeIndex = useMemo(
    () => findActiveLine(lyrics.lines, adjusted),
    [lyrics, adjusted],
  )
  const activeRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const node = activeRef.current
    if (!node) return
    node.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }, [activeIndex])

  const baseText = size === 'lg' ? 'text-[1.6rem] leading-9' : 'text-lg leading-7'
  const activeText = size === 'lg' ? 'text-[1.75rem] leading-10' : 'text-xl leading-8'

  return (
    <div className={`no-scrollbar h-full overflow-y-auto ${className}`}>
      <div className={size === 'lg' ? 'space-y-4 py-1' : 'space-y-3 py-2'}>
        {lyrics.lines.map((line, index) => {
          const isActive = index === activeIndex
          const isPast = index < activeIndex

          if (!line.text.trim()) {
            return <div key={index} className="h-4" />
          }

          return (
            <div
              key={index}
              ref={isActive ? activeRef : undefined}
              className={`lyric-line font-semibold tracking-tight ${baseText} ${
                isActive ? `lyric-line--active ${activeText} text-white` : ''
              } ${isPast ? 'lyric-line--past text-muted' : ''} ${!isActive && !isPast ? 'text-muted' : ''}`}
            >
              {isActive && line.words.length > 0
                ? line.words.map((word, wordIndex) => {
                    const sung = adjusted >= word.time
                    return (
                      <span
                        key={wordIndex}
                        className="transition-opacity duration-150"
                        style={{ opacity: sung ? 1 : 0.38 }}
                      >
                        {word.text}
                        {wordIndex < line.words.length - 1 ? ' ' : ''}
                      </span>
                    )
                  })
                : line.text}
            </div>
          )
        })}
      </div>
    </div>
  )
}

interface LyricsFullscreenProps {
  track: Track
  lyrics: Lyrics
  time: number
  colors: ReturnType<typeof useArtworkColors>
  onClose: () => void
}

/** Vista de letra a pantalla completa con control de retardo. */
export function LyricsFullscreen({ track, lyrics, time, colors, onClose }: LyricsFullscreenProps) {
  const [offset, setOffset] = useState(0)

  const clampOffset = (value: number) =>
    Math.max(-OFFSET_LIMIT, Math.min(OFFSET_LIMIT, Math.round(value * 10) / 10))

  return (
    <Portal>
      <div className="fixed inset-0 z-[60] flex flex-col overflow-hidden">
        <DynamicBackground image={track.thumbnail} colors={colors} intensity="strong" />
        <div className="safe-top relative mx-auto flex w-full max-w-md items-center justify-between gap-3 px-5 py-4">
          <div className="flex min-w-0 items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              aria-label="Cerrar letra"
              className="grid h-10 w-10 shrink-0 place-items-center rounded-full glass-pill"
            >
              <CloseIcon size={20} />
            </button>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold">{track.title}</p>
              <p className="truncate text-xs text-faint">{track.artist}</p>
            </div>
          </div>

          <div className="glass-pill flex shrink-0 items-center gap-1 rounded-full py-1 pr-2 pl-1">
            <button
              type="button"
              aria-label="Atrasar letra"
              onClick={() => setOffset((value) => clampOffset(value - OFFSET_STEP))}
              className="grid h-8 w-8 place-items-center rounded-full tap"
            >
              <MinusIcon size={16} />
            </button>
            <span className="min-w-[3.2rem] text-center text-[11px] tabular-nums">
              {offset > 0 ? '+' : ''}
              {offset.toFixed(1)}s
            </span>
            <button
              type="button"
              aria-label="Adelantar letra"
              onClick={() => setOffset((value) => clampOffset(value + OFFSET_STEP))}
              className="grid h-8 w-8 place-items-center rounded-full tap"
            >
              <PlusIcon size={16} />
            </button>
          </div>
        </div>

        <div className="relative mx-auto w-full max-w-md flex-1 overflow-hidden px-6">
          <LyricsScroller lyrics={lyrics} time={time} offset={offset} size="lg" className="h-full" />
        </div>

        <div className="relative pb-[calc(1rem+env(safe-area-inset-bottom))]">
          <button type="button" onClick={onClose} className="mx-auto block text-xs text-faint">
            Toca para volver
          </button>
        </div>
      </div>
    </Portal>
  )
}

interface LyricsSectionProps {
  track: Track
  lyrics: Lyrics | null
  status: 'idle' | 'loading' | 'ready' | 'empty' | 'error'
  time: number
  colors: ReturnType<typeof useArtworkColors>
}

/** Panel de letras reutilizable dentro de una pantalla. */
export function LyricsSection({ track, lyrics, status, time, colors }: LyricsSectionProps) {
  const [fullscreen, setFullscreen] = useState(false)

  return (
    <>
      <section className="glass-panel flex min-h-0 flex-1 flex-col overflow-hidden rounded-3xl px-5 py-4">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-xs font-semibold uppercase tracking-wider text-faint">Letra</h2>
          {lyrics ? (
            <button
              type="button"
              onClick={() => setFullscreen(true)}
              className="text-xs font-medium text-accent-soft"
            >
              Pantalla completa
            </button>
          ) : null}
        </div>

        <div className="relative flex min-h-0 flex-1 flex-col">
          {status === 'loading' && (
            <div className="flex flex-1 flex-col items-center justify-center gap-3 text-faint">
              <Spinner size={26} />
              <p className="text-sm">Buscando la letra…</p>
            </div>
          )}

          {status === 'ready' && lyrics && (
            <LyricsScroller lyrics={lyrics} time={time} size="md" />
          )}

          {(status === 'empty' || status === 'error') && (
            <div className="flex flex-1 flex-col items-center justify-center gap-2 px-6 text-center text-faint">
              <MusicIcon size={30} />
              <p className="text-sm">
                {status === 'error'
                  ? 'No se pudieron cargar las letras.'
                  : 'Aún no hay letra para esta canción.'}
              </p>
            </div>
          )}

          {status === 'idle' && (
            <div className="flex flex-1 items-center justify-center text-sm text-faint">
              Reproduce una canción para ver la letra.
            </div>
          )}
        </div>
      </section>

      {fullscreen && lyrics ? (
        <LyricsFullscreen
          track={track}
          lyrics={lyrics}
          time={time}
          colors={colors}
          onClose={() => setFullscreen(false)}
        />
      ) : null}
    </>
  )
}
