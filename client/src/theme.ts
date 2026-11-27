import type { ThemeMode } from './types'

/**
 * Tema claro/oscuro y color de énfasis.
 *
 * Todo el CSS usa variables (`--fg`, `--bg`, `--accent`, `--on-accent`…), así
 * que cambiar de tema invierte texto y fondos a la vez. El color de énfasis se
 * corrige si se confunde con el fondo (contraste mínimo 3:1) y el texto que va
 * encima se elige blanco o negro según cuál se lea mejor (WCAG).
 */

type RGB = [number, number, number]

const PALETTES = {
  // Oscuro = negro de verdad (fondo #000, paneles casi negros).
  dark: { fg: [255, 255, 255], bg: [0, 0, 0], panel: [14, 14, 16], solid: [236, 236, 241] },
  light: { fg: [22, 22, 28], bg: [244, 244, 247], panel: [253, 253, 255], solid: [24, 24, 30] },
} satisfies Record<string, Record<string, RGB>>

export const ACCENTS: { name: string; value: string }[] = [
  { name: 'Predeterminado', value: '' },
  { name: 'Grafito', value: '#8e8e99' },
  { name: 'Rosa', value: '#e8799f' },
  { name: 'Lavanda', value: '#a993e0' },
  { name: 'Azul', value: '#6f9bea' },
  { name: 'Cian', value: '#5cbcc9' },
  { name: 'Menta', value: '#6cc59a' },
  { name: 'Oliva', value: '#a7b56a' },
  { name: 'Ámbar', value: '#e0aa5a' },
  { name: 'Coral', value: '#e6866f' },
  { name: 'Granate', value: '#b4566a' },
  { name: 'Índigo', value: '#5c63c9' },
]

export function hexToRgb(hex: string): RGB | null {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim())
  if (!m) return null
  const n = parseInt(m[1], 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

function luminance([r, g, b]: RGB): number {
  const c = [r, g, b].map((v) => {
    const s = v / 255
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
}

export function contrast(a: RGB, b: RGB): number {
  const [x, y] = [luminance(a), luminance(b)].sort((p, q) => q - p)
  return (x + 0.05) / (y + 0.05)
}

function mix(a: RGB, b: RGB, t: number): RGB {
  return [0, 1, 2].map((i) => Math.round(a[i] + (b[i] - a[i]) * t)) as RGB
}

/** Acerca el color al del texto hasta que se distinga del fondo. */
export function readableOn(color: RGB, bg: RGB, fg: RGB, min = 3): RGB {
  let out = color
  for (let t = 0.1; contrast(out, bg) < min && t <= 1; t += 0.1) out = mix(color, fg, t)
  return out
}

/** Texto blanco o casi negro, el que más contraste tenga sobre `color`. */
export function textOn(color: RGB): RGB {
  const white: RGB = [255, 255, 255]
  const black: RGB = [18, 18, 22]
  return contrast(color, white) >= contrast(color, black) ? white : black
}

export function resolvedMode(mode: ThemeMode): 'dark' | 'light' {
  if (mode !== 'system') return mode
  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

const css = (c: RGB) => c.join(', ')
const hex = (c: RGB) => `#${c.map((v) => v.toString(16).padStart(2, '0')).join('')}`

export function applyTheme(mode: ThemeMode, accentHex: string) {
  const name = resolvedMode(mode)
  const p = PALETTES[name]
  const root = document.documentElement
  const raw = hexToRgb(accentHex)
  const accent = raw ? readableOn(raw, p.bg as RGB, p.fg as RGB) : (p.solid as RGB)
  const onAccent = textOn(accent)
  const vars: Record<string, string> = {
    '--fg': css(p.fg as RGB),
    '--bg': css(p.bg as RGB),
    '--panel': css(p.panel as RGB),
    '--base': hex(p.bg as RGB),
    '--solid': hex(p.solid as RGB),
    '--on-solid': hex(textOn(p.solid as RGB)),
    '--accent': hex(accent),
    '--accent-rgb': css(accent),
    '--on-accent': hex(onAccent),
  }
  for (const [k, v] of Object.entries(vars)) root.style.setProperty(k, v)
  root.classList.toggle('light', name === 'light')
  root.classList.toggle('dark', name === 'dark')
  root.style.colorScheme = name
}
