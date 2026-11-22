import { createColdStartToken } from 'bgutils-js/webpo'

/**
 * po_token ligero para el stream directo de YouTube, sin interpreter de
 * BotGuard. En Electron las peticiones van por IPC para evitar CORS.
 */

const CLIENT_VERSION = '2.20250925.01.00'
const API_KEY = 'AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw'

interface FetchResult {
  status: number
  text: string
}

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

let cachedVisitor = ''
let cachedAt = 0

async function visitorData(): Promise<string> {
  if (cachedVisitor && Date.now() - cachedAt < 12 * 60 * 60 * 1000) return cachedVisitor

  const result = await request({
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

  const data = JSON.parse(result.text) as { responseContext?: { visitorData?: string } }
  cachedVisitor = data.responseContext?.visitorData ?? ''
  cachedAt = Date.now()
  return cachedVisitor
}

export async function mintPoToken(): Promise<string> {
  const visitor = await visitorData()
  return createColdStartToken(visitor || 'popmusic-desktop')
}
