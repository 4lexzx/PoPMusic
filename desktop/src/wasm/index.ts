export { parseLrc, paletteFromPixels } from './fallback'
export const USING_WASM = false

/** Sin Rust instalado no hay módulo WASM: la implementación JS ya está lista. */
export function initWasm(): Promise<void> {
  return Promise.resolve()
}
