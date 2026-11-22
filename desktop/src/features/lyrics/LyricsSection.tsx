import { memo, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { CloseIcon, MinusIcon, MusicIcon, PlusIcon } from '@/components/Icons'
import { Portal, Spinner } from '@/components/ui'
import { getPlaybackTime } from '@/features/player/playbackClock'
import { DynamicBackground, useArtworkColors } from '@/features/visuals/DynamicBackground'
import type { LyricLine, Lyrics } from '@/shared/types/lyrics'
import type { Track } from '@/shared/types/track'
import { findActiveLine } from '@/shared/utils/lrc'

const OFFSET_STEP = 0.5
const OFFSET_LIMIT = 10
const SCROLL_TRANSITION = 'transform 480ms cubic-bezier(0.22, 1, 0.36, 1)'

interface LyricsScrollerProps {
  lyrics: Lyrics
  offset?: number
  size?: 'md' | 'lg'
  className?: string
}

/**
 * Palabras de la línea activa. La opacidad se actualiza directamente en el DOM
 * en cada frame, sin re-renderizar React, para que el karaoke siga el ritmo.
 */
function KaraokeWords({ line, offset }: { line: LyricLine; offset: number }) {
  const wordRefs = useRef<Array<HTMLSpanElement | null>>([])

  useEffect(() => {
    let frame = 0

    const tick = () => {
      const time = getPlaybackTime() + offset
      for (let index = 0; index < line.words.length; index += 1) {
        const node = wordRefs.current[index]
        if (!node) continue
        const opacity = time >= line.words[index].time ? '1' : '0.32'
        if (node.style.opacity !== opacity) node.style.opacity = opacity
      }
      frame = requestAnimationFrame(tick)
    }

    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [line, offset])

  return (
    <>
      {line.words.map((word, wordIndex) => (
        <span
          key={wordIndex}
          ref={(node) => {
            wordRefs.current[wordIndex] = node
          }}
          className="opacity-[0.32] transition-opacity duration-100"
        >
          {word.text}
          {wordIndex < line.words.length - 1 ? ' ' : ''}
        </span>
      ))}
    </>
  )
}

/**
 * Letras con resaltado tipo karaoke: línea activa nítida, resto difuminado.
 * El desplazamiento se hace con `transform` (acelerado por GPU) en lugar de
 * scroll nativo suave, que llegaba tarde y a tirones.
 */
function LyricsScrollerBase({
  lyrics,
  offset = 0,
  size = 'md',
  className = '',
}: LyricsScrollerProps) {
  const [activeIndex, setActiveIndex] = useState(-1)
  const [shift, setShift] = useState(0)
  const [viewportHeight, setViewportHeight] = useState(0)

  const viewportRef = useRef<HTMLDivElement | null>(null)
  const lineRefs = useRef<Array<HTMLDivElement | null>>([])

  // Índice activo a 60 fps; solo provoca render cuando cambia de línea.
  useEffect(() => {
    let frame = 0
    let current = -1

    const tick = () => {
      const index = findActiveLine(lyrics.lines, getPlaybackTime() + offset)
      if (index !== current) {
        current = index
        setActiveIndex(index)
      }
      frame = requestAnimationFrame(tick)
    }

    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [lyrics, offset])

  useLayoutEffect(() => {
    const viewport = viewportRef.current
    if (!viewport) return

    const update = () =>
      setViewportHeight((current) =>
        current === viewport.clientHeight ? current : viewport.clientHeight,
      )

    update()
    const observer = new ResizeObserver(update)
    observer.observe(viewport)
    return () => observer.disconnect()
  }, [])

  useLayoutEffect(() => {
    if (viewportHeight === 0) return

    const line = lineRefs.current[activeIndex]
    if (!line) {
      setShift(0)
      return
    }

    const target = line.offsetTop + line.offsetHeight / 2 - viewportHeight / 2
    setShift(Math.max(0, target))
  }, [activeIndex, lyrics, size, viewportHeight])

  const baseText = size === 'lg' ? 'text-[1.6rem] leading-9' : 'text-lg leading-7'
  const gap = size === 'lg' ? 'space-y-4' : 'space-y-3'

  return (
    <div ref={viewportRef} className={`relative h-full overflow-hidden ${className}`}>
      <div
        className={`relative ${gap}`}
        style={{
          paddingTop: viewportHeight / 2,
          paddingBottom: viewportHeight / 2,
          transform: `translate3d(0, ${-shift}px, 0)`,
          transition: SCROLL_TRANSITION,
          willChange: 'transform',
        }}
      >
        {lyrics.lines.map((line, index) => {
          const isActive = index === activeIndex
          const isPast = index < activeIndex

          return (
            <div
              key={index}
              ref={(node) => {
                lineRefs.current[index] = node
              }}
              className={`lyric-line font-semibold tracking-tight ${baseText} ${
                isActive ? 'lyric-line--active text-white' : ''
              } ${isPast ? 'lyric-line--past text-muted' : ''} ${!isActive && !isPast ? 'text-muted' : ''}`}
            >
              {isActive && line.words.length > 0 ? (
                <KaraokeWords line={line} offset={offset} />
              ) : (
                line.text
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}

export const LyricsScroller = memo(LyricsScrollerBase)

interface LyricsFullscreenProps {
  track: Track
  lyrics: Lyrics
  colors: ReturnType<typeof useArtworkColors>
  onClose: () => void
}

/** Vista de letra a pantalla completa con control de retardo. */
export function LyricsFullscreen({ track, lyrics, colors, onClose }: LyricsFullscreenProps) {
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
          <LyricsScroller lyrics={lyrics} offset={offset} size="lg" className="h-full" />
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
  colors: ReturnType<typeof useArtworkColors>
}

/** Panel de letras reutilizable dentro de una pantalla. */
export function LyricsSection({ track, lyrics, status, colors }: LyricsSectionProps) {
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

          {status === 'ready' && lyrics && <LyricsScroller lyrics={lyrics} size="md" />}

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
          colors={colors}
          onClose={() => setFullscreen(false)}
        />
      ) : null}
    </>
  )
}
