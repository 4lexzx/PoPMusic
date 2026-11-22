import init, { parse_lrc, palette } from './pkg/pop_wasm.js'
import * as fallback from './fallback'
import type { LyricLine } from '@/shared/types/lyrics'
import type { ArtworkColors } from '@/shared/utils/color'

/** Plantilla que el script de build copia a index.ts cuando hay WASM. */
export const USING_WASM = true

let started: Promise<void> | null = null

export function initWasm(): Promise<void> {
  if (!started) {
    started = init(new URL('./pkg/pop_wasm_bg.wasm', import.meta.url)).then(() => undefined)
  }
  return started
}

export function parseLrc(lrc: string): LyricLine[] {
  if (!started) return fallback.parseLrc(lrc)
  return JSON.parse(parse_lrc(lrc)) as LyricLine[]
}

export function paletteFromPixels(data: Uint8ClampedArray): ArtworkColors {
  if (!started) return fallback.paletteFromPixels(data)
  return JSON.parse(
    palette(new Uint8Array(data.buffer, data.byteOffset, data.length)),
  ) as ArtworkColors
}
