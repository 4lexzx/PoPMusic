import type { LyricLine, LyricWord } from './types'

const STAMP = /\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]/g

/**
 * Convierte texto LRC (o letra plana) en líneas con tiempo en ms.
 *
 * - Una línea puede tener varias marcas ([00:12.00][01:30.50]) en estribillos.
 * - Respeta la etiqueta [offset:+/-ms] del propio LRC.
 * - Las líneas vacías con marca se guardan como pausas instrumentales.
 * - Cada línea se analiza con una expresión nueva (una regex global
 *   compartida arrastra `lastIndex` entre líneas y se saltaba marcas).
 */
export function parseLyrics(raw: string | null): LyricLine[] {
  if (!raw) return []
  const lines: LyricLine[] = []
  let offset = 0
  const off = /\[offset:\s*([+-]?\d+)\s*\]/i.exec(raw)
  if (off) offset = Number(off[1])

  for (const rawLine of raw.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line) continue
    const stamps = [...line.matchAll(new RegExp(STAMP.source, 'g'))]
    if (stamps.length) {
      const text = line.replace(new RegExp(STAMP.source, 'g'), '').trim()
      for (const m of stamps) {
        const min = Number(m[1])
        const sec = Number(m[2])
        const frac = m[3] ? Number(m[3].padEnd(3, '0')) / 1000 : 0
        // En LRC, un offset positivo adelanta la letra.
        const time = Math.max(0, Math.round((min * 60 + sec + frac) * 1000) - offset)
        lines.push({ time, text })
      }
    } else if (!/^\[[a-z]+:/i.test(line)) {
      lines.push({ time: -1, text: line })
    }
  }

  lines.sort((a, b) => a.time - b.time)
  return cleanPauses(lines)
}

/** Pausas: se quitan las vacías seguidas y las que no duran nada. */
function cleanPauses(lines: LyricLine[]): LyricLine[] {
  return lines.filter((l, i) => {
    if (l.text) return true
    const prev = lines[i - 1]
    const next = lines[i + 1]
    if (prev && !prev.text) return false
    return !!next && next.time - l.time > 2500
  })
}

// ---------------------------------------------------------------------------
// Letra palabra por palabra
// ---------------------------------------------------------------------------

/** Quita las comillas de un escalar YAML ('it''s' → it's). */
function yamlScalar(v: string): string {
  const s = v.trim()
  if (s.startsWith("'") && s.endsWith("'")) return s.slice(1, -1).replace(/''/g, "'")
  if (s.startsWith('"') && s.endsWith('"')) {
    try { return JSON.parse(s) } catch { return s.slice(1, -1) }
  }
  return s
}

/**
 * Formato `lyricsfile` de LrcLib (YAML) con tiempos por palabra:
 *   lines: - text, start_ms, end_ms, words: - text, start_ms
 */
export function parseLyricsfile(yaml: string): LyricLine[] {
  const lines: LyricLine[] = []
  let line: LyricLine | null = null
  let word: LyricWord | null = null
  let inWords = false
  for (const raw of yaml.split(/\r?\n/)) {
    const lineItem = /^- text:\s*(.*)$/.exec(raw)
    if (lineItem) {
      line = { time: 0, text: yamlScalar(lineItem[1]), words: [] }
      lines.push(line)
      inWords = false
      word = null
      continue
    }
    if (!line) continue
    if (/^\s{2}words:\s*$/.test(raw)) { inWords = true; continue }
    const wordItem = /^\s{2}- text:\s*(.*)$/.exec(raw)
    if (wordItem && inWords) {
      word = { t: 0, text: yamlScalar(wordItem[1]) }
      line.words!.push(word)
      continue
    }
    const field = /^(\s*)(start_ms|end_ms):\s*(\d+)/.exec(raw)
    if (field) {
      const value = Number(field[3])
      const isWord = inWords && field[1].length >= 4 && word
      if (isWord) {
        if (field[2] === 'start_ms') word!.t = value
        else word!.d = value - word!.t
      } else if (field[2] === 'start_ms') line.time = value
      else line.end = value
    }
  }
  return finishWords(lines.filter((l) => l.text || l.words?.length))
}

/**
 * Formato YRC de NetEase: `[inicio,duración](inicioPalabra,duración,0)palabra…`.
 * Trae la duración de cada palabra, así que sabe cuándo se alarga una nota.
 */
export function parseYrc(yrc: string): LyricLine[] {
  const lines: LyricLine[] = []
  for (const raw of yrc.split(/\r?\n/)) {
    const head = /^\[(\d+),(\d+)\](.*)$/.exec(raw.trim())
    if (!head) continue
    const words: LyricWord[] = []
    for (const m of head[3].matchAll(/\((\d+),(\d+),\d+\)([^(]*)/g)) {
      if (m[3]) words.push({ t: Number(m[1]), d: Number(m[2]), text: m[3] })
    }
    const text = words.map((w) => w.text).join('').trim()
    if (!text || /^(作词|作曲|编曲|制作|混音|母带|和声|吉他|贝斯|鼓|录音)/.test(text)) continue
    lines.push({ time: Number(head[1]), end: Number(head[1]) + Number(head[2]), text, words })
  }
  return finishWords(lines)
}

/** Completa las duraciones que faltan (hasta la palabra siguiente o el final de la línea). */
function finishWords(lines: LyricLine[]): LyricLine[] {
  lines.sort((a, b) => a.time - b.time)
  lines.forEach((line, i) => {
    const next = lines[i + 1]
    if (!line.end) line.end = next ? next.time : line.time + 4000
    const ws = line.words || []
    ws.forEach((w, j) => {
      const after = ws[j + 1]?.t ?? line.end!
      const gap = Math.max(60, after - w.t)
      // Sin duración real: lo que se tarda en cantarla (por sílabas), sin pasar
      // del inicio de la siguiente. Antes rellenaba también el silencio de después.
      if (!w.d || w.d <= 0) w.d = Math.min(gap, Math.max(200, syllables(w.text) * 210 + 90))
    })
  })
  return lines
}

/** Sílabas aproximadas (grupos de vocales): pesan las palabras largas. */
function syllables(word: string): number {
  const groups = word.toLowerCase().match(/[aeiouyáéíóúüàèìòùâêîôûäëïöå]+/g)
  return Math.max(1, groups ? groups.length : 1)
}

/**
 * Sin tiempos por palabra: se reparten dentro de la línea según sus sílabas,
 * en el tramo en que de verdad se canta (no en las pausas de después).
 */
export function estimateWords(lines: LyricLine[]): LyricLine[] {
  return lines.map((line, i) => {
    if (!line.text || line.words?.length || line.time < 0) return line
    const next = lines[i + 1]
    const span = (next && next.time > line.time ? next.time : line.time + 5000) - line.time
    const parts = line.text.split(/(\s+)/).filter(Boolean)
    const tokens: string[] = []
    for (const p of parts) {
      if (/^\s+$/.test(p) && tokens.length) tokens[tokens.length - 1] += p
      else tokens.push(p)
    }
    const weights = tokens.map((t) => syllables(t) + 0.35)
    const totalW = weights.reduce((a, b) => a + b, 0)
    const sung = Math.min(span * 0.9, Math.max(800, totalW * 260))
    let t = line.time
    const words: LyricWord[] = tokens.map((text, k) => {
      const d = (weights[k] / totalW) * sung
      const w = { t: Math.round(t), d: Math.round(d), text }
      t += d
      return w
    })
    return { ...line, end: line.time + sung, words, estimated: true }
  })
}

/** Índice de la línea activa para un tiempo en ms (búsqueda binaria). */
export function activeLine(lines: LyricLine[], ms: number): number {
  let lo = 0
  let hi = lines.length - 1
  let found = -1
  while (lo <= hi) {
    const mid = (lo + hi) >> 1
    if (lines[mid].time >= 0 && lines[mid].time <= ms) { found = mid; lo = mid + 1 } else hi = mid - 1
  }
  return found
}
