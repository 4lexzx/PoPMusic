/** Formatea segundos como m:ss o h:mm:ss. */
export function formatDuration(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00'
  const total = Math.floor(seconds)
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const secs = total % 60
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  }
  return `${minutes}:${String(secs).padStart(2, '0')}`
}

/** Convierte una duración ISO 8601 de YouTube (PT3M12S) a segundos. */
export function parseIsoDuration(iso: string | undefined): number {
  if (!iso) return 0
  const match = /PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/.exec(iso)
  if (!match) return 0
  const hours = Number(match[1] ?? 0)
  const minutes = Number(match[2] ?? 0)
  const seconds = Number(match[3] ?? 0)
  return hours * 3600 + minutes * 60 + seconds
}

/** Convierte un tiempo de reloj de YouTube ("5:55" o "1:02:03") a segundos. */
export function parseClockTime(clock: string | undefined): number {
  if (!clock) return 0
  const parts = clock
    .trim()
    .split(':')
    .map((part) => Number(part.replace(/[^\d]/g, '')))
  if (parts.length === 0 || parts.some((part) => !Number.isFinite(part))) return 0
  return parts.reduce((total, part) => total * 60 + part, 0)
}

/** Limita un número a un rango. */
export function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}
