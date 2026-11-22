import { ClientType, Innertube } from 'youtubei.js/web'
import { mintPoToken } from './youtubeIntegrity'

/** Stream de audio directo de YouTube (sin iframe), como en Liner. */

let sessionPromise: Promise<InstanceType<typeof Innertube>> | null = null

/** En Electron las peticiones a YouTube van por IPC para evitar CORS. */
const popFetch: typeof fetch = async (input, init) => {
  const url =
    typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
  if (window.popDesktop && !url.startsWith('http://127.0.0.1')) {
    const headers: Record<string, string> = {}
    if (init?.headers) {
      new Headers(init.headers).forEach((value, key) => {
        headers[key] = value
      })
    }
    const result = await window.popDesktop.youtubeFetch({
      url,
      method: init?.method ?? (init?.body ? 'POST' : 'GET'),
      headers,
      body: typeof init?.body === 'string' ? init.body : undefined,
    })
    return new Response(result.text, { status: result.status, headers: new Headers(headers) })
  }
  return fetch(input, init)
}

function getSession(): Promise<InstanceType<typeof Innertube>> {
  if (!sessionPromise) {
    sessionPromise = Innertube.create({
      client_type: ClientType.WEB,
      fetch: popFetch,
    }) as Promise<InstanceType<typeof Innertube>>
  }
  return sessionPromise
}

export interface DirectStream {
  url: string
  mimeType: string
  bitrate: number
  expiresAt: string | null
}

export async function resolveYoutubeStream(videoId: string): Promise<DirectStream> {
  const youtube = await getSession()
  const poToken = await mintPoToken()
  const info = await youtube.getInfo(videoId, poToken ? { po_token: poToken } : undefined)

  const formats = info.streaming_data?.adaptive_formats ?? []
  const audio = formats
    .filter((format) => format.has_audio && !format.has_video && format.url)
    .sort((a, b) => (b.bitrate ?? 0) - (a.bitrate ?? 0))[0]

  if (!audio?.url) {
    throw new Error('YouTube no devolvió una URL de audio reproducible')
  }

  return {
    url: audio.url,
    mimeType: audio.mime_type ?? '',
    bitrate: audio.bitrate ?? 0,
    expiresAt: info.streaming_data?.expires?.toISOString?.() ?? null,
  }
}
