export class HttpError extends Error {
  status: number

  constructor(status: number, message?: string) {
    super(message ?? `HTTP ${status}`)
    this.name = 'HttpError'
    this.status = status
  }
}

/** Construye un query string compatible con Hermes (evita URLSearchParams). */
export function qs(params: Record<string, string | number | undefined | null>): string {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
}

/** GET JSON con timeout y errores tipados. */
export async function getJson<T>(url: string, init?: RequestInit, timeoutMs = 15000): Promise<T> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    const response = await fetch(url, { ...init, signal: controller.signal })
    if (!response.ok) {
      throw new HttpError(response.status, `HTTP ${response.status}`)
    }
    return (await response.json()) as T
  } finally {
    clearTimeout(timer)
  }
}
