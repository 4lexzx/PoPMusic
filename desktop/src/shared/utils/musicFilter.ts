/** Filtros de contenido: solo música, nunca videoclips ni Shorts. */

const VIDEO_WORDS = [
  /\bvideo(clip)?\b/i,
  /\bmv\b/i,
  /\bofficial\s+video\b/i,
  /\blyric\s+video\b/i,
  /\bvisualizador\b/i,
  /\bvisualizer\b/i,
  /\bteaser\b/i,
  /\btrailer\b/i,
  /\bclip\s+oficial\b/i,
  /\bshorts?\b/i,
  /#shorts/i,
]

/** true si el título parece una pista de música y no un videoclip. */
export function isMusicTitle(title: string): boolean {
  const clean = title.trim()
  if (!clean) return false
  if (VIDEO_WORDS.some((pattern) => pattern.test(clean))) return false
  return true
}

/** Descarta Shorts y clips por título y duración (segundos). */
export function isMusicTrack(title: string, durationSeconds?: number): boolean {
  if (!isMusicTitle(title)) return false
  if (typeof durationSeconds === 'number' && durationSeconds > 0 && durationSeconds < 50) {
    return false
  }
  return true
}
