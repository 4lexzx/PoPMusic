import type { Track } from '@/shared/types/track'

/**
 * Cliente de Audius (catálogo público, sin clave).
 * Devuelve `stream` como URL de audio directa: al reproducirse con un
 * `<audio>` real, el audio SÍ continúa en segundo plano y en la pantalla
 * de bloqueo (a diferencia del iframe de YouTube).
 */

const APP_NAME = 'PoPMusic'
const NODES_URL = 'https://api.audius.co'
const FALLBACK_NODE = 'https://discoveryprovider.audius.co'

let cachedNodes: string[] = []
let nodePromise: Promise<string[]> | null = null

interface AudiusTrack {
  id?: string
  title?: string
  duration?: number
  artwork?: Record<string, string>
  user?: { name?: string; handle?: string }
}

function loadNodes(): Promise<string[]> {
  if (cachedNodes.length > 0) return Promise.resolve(cachedNodes)
  if (!nodePromise) {
    nodePromise = fetch(NODES_URL)
      .then((response) => response.json() as Promise<{ data?: string[] }>)
      .then((data) => {
        cachedNodes = (data.data ?? []).filter((url) => typeof url === 'string')
        return cachedNodes
      })
      .catch(() => {
        cachedNodes = [FALLBACK_NODE]
        return cachedNodes
      })
  }
  return nodePromise
}

async function pickNode(): Promise<string> {
  const nodes = await loadNodes()
  if (nodes.length === 0) return FALLBACK_NODE
  return nodes[Math.floor(Math.random() * nodes.length)]
}

function artworkUrl(artwork?: Record<string, string>): string {
  if (!artwork) return ''
  return artwork['480x480'] ?? artwork['1000x1000'] ?? artwork['150x150'] ?? ''
}

function toTrack(node: string, item: AudiusTrack): Track | null {
  if (!item.id || !item.title) return null
  return {
    id: `audius-${item.id}`,
    title: item.title,
    artist: item.user?.name ?? item.user?.handle ?? 'Audius',
    thumbnail: artworkUrl(item.artwork),
    duration: item.duration ?? 0,
    source: 'audius',
    stream: `${node}/v1/tracks/${item.id}/stream?app_name=${APP_NAME}`,
  }
}

/** Busca pistas en Audius. Devuelve `[]` ante cualquier fallo (nunca lanza). */
export async function searchAudius(query: string, limit = 25): Promise<Track[]> {
  const value = query.trim()
  if (!value) return []
  try {
    const node = await pickNode()
    const response = await fetch(
      `${node}/v1/tracks/search?query=${encodeURIComponent(value)}&app_name=${APP_NAME}`,
    )
    if (!response.ok) return []
    const data = (await response.json()) as { data?: AudiusTrack[] }
    return (data.data ?? [])
      .map((item) => toTrack(node, item))
      .filter((track): track is Track => track !== null)
      .slice(0, limit)
  } catch {
    return []
  }
}

/** Pistas en tendencia de Audius (para filas de recomendación). */
export async function fetchAudiusTrending(limit = 12): Promise<Track[]> {
  try {
    const node = await pickNode()
    const response = await fetch(
      `${node}/v1/tracks/trending?app_name=${APP_NAME}&limit=${limit}`,
    )
    if (!response.ok) return []
    const data = (await response.json()) as { data?: AudiusTrack[] }
    return (data.data ?? [])
      .map((item) => toTrack(node, item))
      .filter((track): track is Track => track !== null)
      .slice(0, limit)
  } catch {
    return []
  }
}

function normalize(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, '')
}

/** Comprueba que el artista coincide de forma laxa (se evita remix/versión ajena). */
function looseArtistMatch(target: string, candidate: string): boolean {
  const a = normalize(target)
  const b = normalize(candidate)
  if (!a || !b) return true
  return a.includes(b) || b.includes(a)
}

/**
 * Intenta emparejar una pista (de YouTube/Spotify) con una versión de Audius
 * para poder reproducirla en segundo plano. Solo acepta coincidencias de
 * título muy claras, para no reproducir una canción equivocada.
 */
export async function resolveAudiusStream(title: string, artist: string): Promise<string | null> {
  const target = normalize(title)
  if (target.length < 3) return null

  const lookup = searchAudius(`${title} ${artist}`.trim(), 20).then((results) => {
    const match = results.find(
      (track) =>
        normalize(track.title) === target && looseArtistMatch(artist, track.artist),
    )
    return match?.stream ?? null
  })

  const timeout = new Promise<null>((resolve) => {
    window.setTimeout(() => resolve(null), 4000)
  })

  return Promise.race([lookup, timeout])
}
