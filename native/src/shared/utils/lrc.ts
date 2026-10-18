import type { LyricLine, LyricWord } from '@/shared/types/lyrics'

const LINE_TIME = /\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]/g
const WORD_TAG = /<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>/g

function toSeconds(minutes: string, seconds: string, fraction?: string): number {
  const mins = Number(minutes)
  const secs = Number(seconds)
  let frac = 0
  if (fraction) {
    const padded =
      fraction.length === 1 ? `${fraction}00` : fraction.length === 2 ? `${fraction}0` : fraction
    frac = Number(padded) / 1000
  }
  return mins * 60 + secs + frac
}

/** Extrae palabras con marca de tiempo del formato LRC enriquecido (<mm:ss.xx>). */
function parseWordTags(content: string): LyricWord[] {
  const parts = content.split(/(<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>)/g)
  if (parts.length <= 1) return []

  const words: LyricWord[] = []
  let pending: number | null = null

  for (const part of parts) {
    const tag = /^<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>$/.exec(part)
    if (tag) {
      pending = toSeconds(tag[1], tag[2], tag[3])
    } else if (pending !== null && part.trim()) {
      words.push({ time: pending, text: part })
      pending = null
    }
  }
  return words
}

/**
 * Reparte las palabras de una línea sin sincronización por palabra dentro de
 * su duración, para poder animar el karaoke aunque el LRC sea por líneas.
 */
function interpolate(lines: LyricLine[]): void {
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
    const next = lines[index + 1]
    const end = next ? next.time : line.time + 4
    line.end = Math.max(end, line.time + 0.6)

    if (line.words.length === 0 && line.text) {
      const tokens = line.text.split(/\s+/).filter((token) => token.length > 0)
      const span = line.end - line.time
      const step = tokens.length > 0 ? span / tokens.length : 0
      line.words = tokens.map((token, tokenIndex) => ({
        time: line.time + step * tokenIndex,
        text: token,
      }))
    }
  }
}

/** Convierte texto LRC (con o sin timings por palabra) en líneas tipadas. */
export function parseLrc(lrc: string): LyricLine[] {
  const lines: LyricLine[] = []

  for (const raw of lrc.split(/\r?\n/)) {
    LINE_TIME.lastIndex = 0
    const stamps: number[] = []
    let match: RegExpExecArray | null
    while ((match = LINE_TIME.exec(raw)) !== null) {
      stamps.push(toSeconds(match[1], match[2], match[3]))
    }
    if (stamps.length === 0) continue

    const content = raw.replace(LINE_TIME, '')
    const text = content.replace(WORD_TAG, '').trim()
    if (!text) continue

    const words = parseWordTags(content)

    for (const time of stamps) {
      lines.push({
        time,
        end: 0,
        text,
        words: words.map((word) => ({ time: time + word.time, text: word.text })),
      })
    }
  }

  lines.sort((a, b) => a.time - b.time)
  interpolate(lines)
  return lines
}

/** Devuelve el índice de la línea activa para un segundo dado. */
export function findActiveLine(lines: LyricLine[], time: number): number {
  let low = 0
  let high = lines.length - 1
  let result = -1
  while (low <= high) {
    const mid = (low + high) >> 1
    if (lines[mid].time <= time) {
      result = mid
      low = mid + 1
    } else {
      high = mid - 1
    }
  }
  return result
}
