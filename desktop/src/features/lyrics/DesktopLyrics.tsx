import { useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { MinusIcon, PlusIcon } from '@/components/Icons'
import { LyricsScroller } from '@/features/lyrics/LyricsSection'
import type { Lyrics } from '@/shared/types/lyrics'

const STEP = 0.5
const LIMIT = 10

/** Panel de letras grande, con control de retardo y transiciones suaves. */
export function DesktopLyrics({ lyrics }: { lyrics: Lyrics }) {
  const [offset, setOffset] = useState(0)
  const clamp = (value: number) => Math.max(-LIMIT, Math.min(LIMIT, Math.round(value * 10) / 10))

  return (
    <div className="desktop-lyrics">
      <AnimatePresence mode="wait">
        <motion.div
          key={`${lyrics.trackName}-${lyrics.lines.length}`}
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -18 }}
          transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
          className="desktop-lyrics__viewport"
        >
          <LyricsScroller lyrics={lyrics} offset={offset} size="lg" />
        </motion.div>
      </AnimatePresence>

      <div className="desktop-lyrics__offset">
        <button type="button" onClick={() => setOffset((value) => clamp(value - STEP))} aria-label="Atrasar letra">
          <MinusIcon size={15} />
        </button>
        <span>
          {offset > 0 ? '+' : ''}
          {offset.toFixed(1)}s
        </span>
        <button type="button" onClick={() => setOffset((value) => clamp(value + STEP))} aria-label="Adelantar letra">
          <PlusIcon size={15} />
        </button>
      </div>
    </div>
  )
}
