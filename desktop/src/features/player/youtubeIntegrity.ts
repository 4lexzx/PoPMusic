import { BotGuardClient } from 'bgutils-js/botguard'
import { WebPoMinter } from 'bgutils-js/webpo'
import { buildURL, GOOG_API_KEY } from 'bgutils-js/utils'

/**
 * Genera un po_token de YouTube con bgutils-js, igual que hace el reproductor
 * de Liner. En Electron las peticiones van por IPC (sin CORS); en el navegador
 * de desarrollo se intenta con fetch directo.
 */

const CLIENT_VERSION = '2.20250925.01.00'
const API_KEY = GOOG_API_KEY ?? 'AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8'
const REQUEST_KEY = 'O43z0dpjhgX20SCx4KAo'

interface FetchResult {
  status: number
  text: string
  cookie?: string
}

let lastCookie = ''

async function request(options: {
  url: string
  method?: string
  headers?: Record<string, string>
  body?: string
}): Promise<FetchResult> {
  if (window.popDesktop) return window.popDesktop.youtubeFetch(options)
  const response = await fetch(options.url, {
    method: options.method ?? 'GET',
    headers: options.headers,
    body: options.body,
  })
  return { status: response.status, text: await response.text() }
}

async function requestJson<T>(options: {
  url: string
  method?: string
  headers?: Record<string, string>
  body?: string
}): Promise<T> {
  const result = await request(options)
  if (result.status < 200 || result.status >= 300) {
    throw new Error(`YouTube devolvió ${result.status}`)
  }
  if (result.cookie) lastCookie = result.cookie.split(';')[0]
  return JSON.parse(result.text) as T
}

interface RawChallenge {
  program?: string
  globalName?: string
  interpreterUrl?: { privateDoNotAccessOrElseTrustedResourceUrlWrappedValue?: string }
  interpreterJavascript?: { privateDoNotAccessOrElseSafeScriptWrappedValue?: string }
}

interface CachedMinter {
  minter: WebPoMinter
  expiresAt: number
  visitorData: string
}

let cached: CachedMinter | null = null
let pending: Promise<CachedMinter> | null = null

async function createMinter(): Promise<CachedMinter> {
  const challenge = await requestJson<{
    bgChallenge?: RawChallenge
    responseContext?: { visitorData?: string }
  }>({
    url: 'https://www.youtube.com/youtubei/v1/att/get?prettyPrint=false&alt=json',
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-youtube-client-name': '1',
      'x-youtube-client-version': CLIENT_VERSION,
      'x-goog-api-key': API_KEY,
      origin: 'https://www.youtube.com',
    },
    body: JSON.stringify({
      context: {
        client: { clientName: 'WEB', clientVersion: CLIENT_VERSION, hl: 'en', gl: 'US' },
      },
      engagementType: 'ENGAGEMENT_TYPE_UNBOUND',
    }),
  })

  const raw = challenge.bgChallenge
  const interpreterScript =
    raw?.interpreterJavascript?.privateDoNotAccessOrElseSafeScriptWrappedValue
  const rawUrl = raw?.interpreterUrl?.privateDoNotAccessOrElseTrustedResourceUrlWrappedValue
  const interpreterUrl = rawUrl?.startsWith('//') ? `https:${rawUrl}` : rawUrl
  if (!raw?.program || !raw.globalName || !(interpreterScript || interpreterUrl)) {
    throw new Error('YouTube no devolvió el challenge de integridad')
  }

  const code =
    interpreterScript ??
    (await request({ url: interpreterUrl! })).text
  // El intérprete de BotGuard necesita un `window` real; se ejecuta en el renderer.
  new Function(code)()

  const botguard = await BotGuardClient.create({
    program: raw.program,
    globalName: raw.globalName,
    globalObject: globalThis,
  })
  const webPoSignalOutput: Parameters<typeof WebPoMinter.create>[1] = []
  const snapshot = await botguard.snapshot({ webPoSignalOutput })

  const integrity = await requestJson<Array<string | number | null>>({
    url: buildURL('GenerateIT', true),
    method: 'POST',
    headers: {
      'content-type': 'application/json+protobuf',
      'x-goog-api-key': API_KEY,
      'x-user-agent': 'grpc-web-javascript/0.1',
      origin: 'https://www.youtube.com',
      ...(lastCookie ? { cookie: lastCookie } : {}),
    },
    body: JSON.stringify([REQUEST_KEY, snapshot]),
  })
  // YouTube devuelve [null, ttl, null, token] o [token, ttl] según versión.
  const token = integrity.find(
    (item): item is string => typeof item === 'string' && item.length > 40,
  )
  const ttl = integrity.find((item): item is number => typeof item === 'number' && item > 0)
  if (!token) throw new Error('YouTube no devolvió el integrity token')

  const minter = await WebPoMinter.create(
    { integrityToken: token.replace(/\+/g, '-').replace(/\//g, '_') },
    webPoSignalOutput,
  )
  const ttlMs = ttl ? ttl * 1000 : 6 * 60 * 60 * 1000
  return {
    minter,
    expiresAt: Date.now() + Math.max(ttlMs - 10 * 60 * 1000, 60_000),
    visitorData: challenge.responseContext?.visitorData ?? '',
  }
}

export async function mintPoToken(identifier?: string): Promise<string> {
  if (!cached || Date.now() >= cached.expiresAt) {
    pending ??= createMinter()
      .then((entry) => {
        cached = entry
        return entry
      })
      .finally(() => {
        pending = null
      })
  }
  const entry = cached ?? (await pending!)
  return entry.minter.mintAsWebsafeString(identifier || entry.visitorData)
}
