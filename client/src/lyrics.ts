import type { LyricLine } from './types'

/** Convierte texto LRC (o letra plana) en líneas con timestamp. */
export function parseLyrics(raw: string | null): LyricLine[] {
  if (!raw) return []
  const lines: LyricLine[] = []
  const stamped = /\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]/g

  for (const line of raw.split(/\r?\n/)) {
    const match = stamped.exec(line)
    if (match) {
      const min = Number(match[1])
      const sec = Number(match[2])
      const frac = match[3] ? Number(`0.${match[3]}`) : 0
      const text = line.slice(match.index + match[0].length).trim()
      if (text) lines.push({ time: (min * 60 + sec + frac) * 1000, text })
    } else if (line.trim() && !line.startsWith('[')) {
      lines.push({ time: -1, text: line.trim() })
    }
  }

  lines.sort((a, b) => a.time - b.time)
  return lines
}

/** Índice de la línea activa para un tiempo en ms. */
export function activeLine(lines: LyricLine[], ms: number): number {
  let found = -1
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].time >= 0 && lines[i].time <= ms) found = i
    else break
  }
  return found
}
