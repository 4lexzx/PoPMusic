import { qs } from './http'
import { parseClockTime } from '@/shared/utils/time'

/**
 * Cliente de la API interna de YouTube (InnerTube).
 *
 * Por qué esto y no la API oficial: la API oficial (YouTube Data API) necesita
 * una clave y NO entrega audio. InnerTube es la misma API que usan la web y las
 * apps oficiales, y devuelve las URLs de audio directas.
 *
 * Clientes: YouTube exige `po_token` (prueba de origen) para algunos clientes.
 * Se rotan varios perfiles hasta que uno responda, igual que hacen las apps
 * open source (NewPipe, LibreTube, etc.). Verificado: el cliente IOS entrega el
 * audio completo (itag 140, AAC) sin token.
 */

const YOUTUBE_BASE = 'https://www.youtube.com'
const PLAYER_URL = `${YOUTUBE_BASE}/youtubei/v1/player?prettyPrint=false`
const SEARCH_URL = `${YOUTUBE_BASE}/youtubei/v1/search?prettyPrint=false`
const BROWSE_URL = `${YOUTUBE_BASE}/youtubei/v1/browse?prettyPrint=false`

interface ClientProfile {
  name: string
  version: string
  /** X-Youtube-Client-Name */
  id: number
  userAgent: string
  extra?: Record<string, string | number>
}

/**
 * Orden de intentos para reproducir. Los que no exigen `po_token` van primero.
 */
const PLAYER_CLIENTS: ClientProfile[] = [
  {
    name: 'IOS',
    version: '20.10.4',
    id: 5,
    userAgent: 'com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X)',
    extra: { deviceMake: 'Apple', deviceModel: 'iPhone16,2', osName: 'iPhone', osVersion: '18.3.2.22D82' },
  },
  {
    name: 'ANDROID_VR',
    version: '1.60.19',
    id: 28,
    userAgent: 'com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12; GB) gzip',
    extra: {
      deviceMake: 'Oculus',
      deviceModel: 'Quest 3',
      osName: 'Android',
      osVersion: '12',
      androidSdkVersion: 32,
    },
  },
  {
    name: 'ANDROID',
    version: '19.35.36',
    id: 3,
    userAgent: 'com.google.android.youtube/19.35.36 (Linux; U; Android 14) gzip',
    extra: { osName: 'Android', osVersion: '14', androidSdkVersion: 34 },
  },
]

const WEB_CLIENT: ClientProfile = {
  name: 'WEB',
  version: '2.20250710.01.00',
  id: 1,
  userAgent:
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
}

async function postJson(
  url: string,
  client: ClientProfile,
  body: Record<string, unknown>,
  timeoutMs = 15000,
): Promise<Record<string, unknown>> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        'Accept-Language': 'es-ES,es;q=0.9',
        'X-Youtube-Client-Name': String(client.id),
        'X-Youtube-Client-Version': client.version,
        'User-Agent': client.userAgent,
        Origin: YOUTUBE_BASE,
      },
      body: JSON.stringify({
        context: {
          client: {
            clientName: client.name,
            clientVersion: client.version,
            hl: 'es',
            gl: 'US',
            ...client.extra,
          },
        },
        ...body,
      }),
      signal: controller.signal,
    })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    return (await response.json()) as Record<string, unknown>
  } finally {
    clearTimeout(timer)
  }
}

/** Recorre un árbol JSON y recoge los nodos que tengan la clave buscada. */
function collectNodes(node: unknown, key: string, out: Record<string, unknown>[] = []): Record<string, unknown>[] {
  if (!node || typeof node !== 'object') return out
  if (Array.isArray(node)) {
    for (const item of node) collectNodes(item, key, out)
    return out
  }
  for (const [name, value] of Object.entries(node as Record<string, unknown>)) {
    if (name === key && value && typeof value === 'object' && !Array.isArray(value)) {
      out.push(value as Record<string, unknown>)
    } else {
      collectNodes(value, key, out)
    }
  }
  return out
}

function readText(value: unknown): string {
  if (!value || typeof value !== 'object') return ''
  const node = value as Record<string, unknown>
  if (typeof node.simpleText === 'string') return node.simpleText
  const runs = node.runs
  if (Array.isArray(runs)) {
    return runs
      .map((run) => (run && typeof run === 'object' ? String((run as Record<string, unknown>).text ?? '') : ''))
      .join('')
  }
  return ''
}

function readThumbnail(value: unknown): string {
  if (!value || typeof value !== 'object') return ''
  const thumbnails = (value as Record<string, unknown>).thumbnails
  if (!Array.isArray(thumbnails) || thumbnails.length === 0) return ''
  const last = thumbnails[thumbnails.length - 1] as Record<string, unknown> | undefined
  return typeof last?.url === 'string' ? (last.url as string) : ''
}

export interface YouTubeAudio {
  url: string
  /** Duración real en segundos del audio entregado. */
  duration: number
  mimeType: string
}

/** Elige el mejor audio reproducible (prefiere AAC/mp4 por compatibilidad). */
function pickAudio(formats: Record<string, unknown>[]): YouTubeAudio | null {
  const playable = formats.filter((format) => typeof format.url === 'string' && format.url)
  if (playable.length === 0) return null

  const byBitrate = (a: Record<string, unknown>, b: Record<string, unknown>) =>
    Number(b.bitrate ?? 0) - Number(a.bitrate ?? 0)

  const mp4 = playable
    .filter((format) => String(format.mimeType ?? '').startsWith('audio/mp4'))
    .sort(byBitrate)
  const chosen = mp4[0] ?? playable.sort(byBitrate)[0]

  const approxMs = Number(chosen.approxDurationMs ?? 0)
  return {
    url: String(chosen.url),
    duration: approxMs > 0 ? Math.round(approxMs / 1000) : 0,
    mimeType: String(chosen.mimeType ?? ''),
  }
}

/**
 * Resuelve la URL de audio completa de un vídeo. Rota entre clientes hasta que
 * uno devuelva un formato reproducible.
 */
export async function resolveYouTubeAudio(videoId: string): Promise<YouTubeAudio | null> {
  for (const client of PLAYER_CLIENTS) {
    try {
      const data = await postJson(PLAYER_URL, client, {
        videoId,
        contentCheckOk: true,
        racyCheckOk: true,
      })
      const status = (data.playabilityStatus as Record<string, unknown> | undefined)?.status
      if (status !== 'OK' && status !== 'LIVE_STREAM_OFFLINE') continue
      const streamingData = data.streamingData as Record<string, unknown> | undefined
      const formats = Array.isArray(streamingData?.adaptiveFormats)
        ? (streamingData?.adaptiveFormats as Record<string, unknown>[])
        : []
      const audio = pickAudio(formats)
      if (audio) return audio
    } catch {
      // Probamos el siguiente cliente.
    }
  }
  return null
}

export interface YouTubeSearchResult {
  videoId: string
  title: string
  channel: string
  thumbnail: string
  duration: number
}

/** Busca vídeos en YouTube sin clave de API. */
export async function searchYouTube(query: string, limit = 25): Promise<YouTubeSearchResult[]> {
  const data = await postJson(SEARCH_URL, WEB_CLIENT, { query, params: 'EgIQAQ==' })
  const renderers = collectNodes(data, 'videoRenderer')
  const results: YouTubeSearchResult[] = []

  for (const renderer of renderers) {
    const videoId = typeof renderer.videoId === 'string' ? renderer.videoId : ''
    if (!videoId) continue
    const title = readText(renderer.title)
    if (!title) continue
    results.push({
      videoId,
      title,
      channel: readText(renderer.ownerText) || readText(renderer.longBylineText),
      thumbnail: readThumbnail(renderer.thumbnail) || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
      duration: parseClockTime(readText(renderer.lengthText)),
    })
    if (results.length >= limit) break
  }

  return results
}

export interface YouTubePlaylistData {
  title: string
  items: YouTubeSearchResult[]
}

function decodeHtml(value: string): string {
  return value
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
}

/** Lee el título de la playlist desde la página pública (og:title). */
export async function fetchYouTubePlaylistTitle(playlistId: string): Promise<string> {
  try {
    const response = await fetch(
      `${YOUTUBE_BASE}/playlist?${qs({ list: playlistId })}`,
      {
        headers: {
          'User-Agent': WEB_CLIENT.userAgent,
          'Accept-Language': 'es-ES,es;q=0.9',
        },
      },
    )
    if (!response.ok) return ''
    const html = await response.text()
    const match = /<meta property="og:title" content="([^"]+)"/.exec(html)
    return match ? decodeHtml(match[1]) : ''
  } catch {
    return ''
  }
}

/**
 * Lee una playlist de YouTube (paginando con el token de continuación).
 * Solo se usan los metadatos: el audio se resuelve vídeo a vídeo al reproducir.
 */
export async function fetchYouTubePlaylist(
  playlistId: string,
  onProgress?: (loaded: number) => void,
): Promise<YouTubePlaylistData> {
  const browseId = playlistId.startsWith('VL') ? playlistId : `VL${playlistId}`

  const items: YouTubeSearchResult[] = []
  const seen = new Set<string>()
  let title = ''

  const push = (nodes: Record<string, unknown>[]) => {
    for (const node of nodes) {
      const videoId = typeof node.videoId === 'string' ? node.videoId : ''
      if (!videoId || seen.has(videoId)) continue
      const itemTitle = readText(node.title)
      if (!itemTitle) continue
      seen.add(videoId)
      items.push({
        videoId,
        title: itemTitle,
        channel: readText(node.shortBylineText) || readText(node.longBylineText),
        thumbnail: readThumbnail(node.thumbnail) || youtubeThumbnail(videoId),
        duration: parseClockTime(readText(node.lengthText)),
      })
    }
  }

  // Igual que el player: se rota entre clientes hasta que uno responda.
  let firstPage: Record<string, unknown> | null = null
  for (const client of [...PLAYER_CLIENTS, WEB_CLIENT]) {
    try {
      const page = await postJson(BROWSE_URL, client, { browseId })
      const metadata = collectNodes(page, 'playlistMetadataRenderer')[0]
      const pageTitle = readText(metadata?.title)
      const found = collectNodes(page, 'playlistVideoRenderer')
      if (found.length > 0 || pageTitle) {
        firstPage = page
        title = pageTitle || title
        push(found)
        break
      }
    } catch {
      // Probamos el siguiente cliente.
    }
  }

  if (!firstPage) throw new Error('No se pudo leer la playlist.')
  onProgress?.(items.length)

  if (!title) title = await fetchYouTubePlaylistTitle(playlistId)

  let token = readContinuationToken(collectNodes(firstPage, 'continuationItemRenderer')[0])
  let guard = 0

  while (token && guard < 20) {
    guard += 1
    try {
      const page = await postJson(BROWSE_URL, PLAYER_CLIENTS[0], { continuation: token })
      push(collectNodes(page, 'playlistVideoRenderer'))
      onProgress?.(items.length)
      token = readContinuationToken(collectNodes(page, 'continuationItemRenderer')[0])
    } catch {
      break
    }
  }

  return { title: title || 'Playlist de YouTube', items }
}

function readContinuationToken(renderer: Record<string, unknown> | undefined): string {
  if (!renderer) return ''
  const endpoint = renderer.continuationEndpoint as Record<string, unknown> | undefined
  const command = endpoint?.continuationCommand as Record<string, unknown> | undefined
  return typeof command?.token === 'string' ? command.token : ''
}

/** URL pública de la miniatura de un vídeo. */
export function youtubeThumbnail(videoId: string): string {
  return `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`
}

/** Extrae el ID de una playlist a partir de una URL o de un ID directo. */
export function parseYouTubePlaylistId(input: string): string | null {
  const value = input.trim()
  if (!value) return null

  const urlMatch = /[?&]list=([A-Za-z0-9_-]+)/.exec(value)
  if (urlMatch) return urlMatch[1]

  if (/^(PL|UU|OLAK|FL|RD|LL)[A-Za-z0-9_-]{10,}$/.test(value)) return value
  if (/^[A-Za-z0-9_-]{18,}$/.test(value)) return value

  return null
}
