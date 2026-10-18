export interface LyricWord {
  /** Segundo en el que empieza la palabra. */
  time: number
  text: string
}

export interface LyricLine {
  time: number
  /** Segundo en el que termina la línea (calculado). */
  end: number
  text: string
  words: LyricWord[]
}

export interface Lyrics {
  /** true si hay sincronización temporal (LRC). */
  synced: boolean
  lines: LyricLine[]
  /** Letra sin sincronizar, si existe. */
  plain: string | null
  trackName: string
  artistName: string
}
