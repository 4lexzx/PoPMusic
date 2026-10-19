import http from 'node:http'
import { Readable } from 'node:stream'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'

/**
 * Backend de audio de PoP Music.
 *
 * Resuelve la URL de audio de un video de YouTube con yt-dlp y PROXYEA los bytes
 * hacia el navegador. Proxyear es obligatorio: las URLs de googlevideo estan
 * ligadas a la IP que las solicito, por lo que un redirect fallaria desde el
 * navegador. Ademas asi la app puede reproducir en segundo plano con <audio>.
 *
 * Endpoints:
 *   GET /health
 *   GET /stream?videoId=<id>   (soporta Range para poder buscar dentro de la pista)
 *
 * Variables de entorno:
 *   PORT          Puerto (por defecto 8080)
 *   ALLOW_ORIGIN  Origen permitido para CORS (por defecto *)
 *   YTDLP_PATH    Ruta al binario yt-dlp (por defecto "yt-dlp")
 */

const PORT = Number(process.env.PORT || 8080)
const ALLOW_ORIGIN = process.env.ALLOW_ORIGIN || '*'
const YTDLP = process.env.YTDLP_PATH || 'yt-dlp'
const CACHE_TTL_MS = 2 * 60 * 60 * 1000
const LOG_LEVEL = process.env.LOG_LEVEL || 'info'

const UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36'

const execFileAsync = promisify(execFile)

const urlCache = new Map()

async function resolveAudio(videoId) {
  const cached = urlCache.get(videoId)
  if (cached && cached.expiresAt > Date.now()) return cached

  const watchUrl = `https://www.youtube.com/watch?v=${videoId}`
  const { stdout } = await execFileAsync(
    YTDLP,
    [
      '--no-warnings',
      '--no-playlist',
      '-f',
      'bestaudio[ext=m4a]/bestaudio/best',
      '-g',
      watchUrl,
    ],
    { timeout: 45000, maxBuffer: 16 * 1024 * 1024 },
  )

  const url = String(stdout)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)[0]

  if (!url) throw new Error('no_audio_url')

  const entry = { url, expiresAt: Date.now() + CACHE_TTL_MS }
  urlCache.set(videoId, entry)
  return entry
}

function setCors(res) {
  res.setHeader('Access-Control-Allow-Origin', ALLOW_ORIGIN)
  res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Range, Content-Type')
  res.setHeader(
    'Access-Control-Expose-Headers',
    'Content-Length, Content-Range, Accept-Ranges, Content-Type',
  )
}

function sendJson(res, status, payload) {
  res.writeHead(status, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify(payload))
}

async function proxyStream(req, res, videoId) {
  const { url } = await resolveAudio(videoId)

  const headers = { 'user-agent': UA, accept: '*/*' }
  if (req.headers.range) headers.range = req.headers.range

  const upstream = await fetch(url, { method: req.method === 'HEAD' ? 'HEAD' : 'GET', headers })

  res.statusCode = upstream.status
  const passthrough = ['content-type', 'content-length', 'content-range', 'accept-ranges']
  for (const name of passthrough) {
    const value = upstream.headers.get(name)
    if (value) res.setHeader(name, value)
  }
  if (!res.getHeader('accept-ranges')) res.setHeader('Accept-Ranges', 'bytes')
  res.setHeader('Cache-Control', 'no-store')

  if (req.method === 'HEAD' || !upstream.body) {
    res.end()
    return
  }

  const nodeStream = Readable.fromWeb(upstream.body)
  nodeStream.on('error', () => res.destroy())
  res.on('close', () => nodeStream.destroy())
  nodeStream.pipe(res)
}

const server = http.createServer((req, res) => {
  setCors(res)

  if (req.method === 'OPTIONS') {
    res.writeHead(204)
    res.end()
    return
  }

  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`)

  if (url.pathname === '/health') {
    sendJson(res, 200, { ok: true, service: 'pop-music-audio' })
    return
  }

  if (url.pathname !== '/stream') {
    sendJson(res, 404, { error: 'not_found' })
    return
  }

  const videoId = url.searchParams.get('videoId') || ''
  if (!/^[A-Za-z0-9_-]{6,}$/.test(videoId)) {
    sendJson(res, 400, { error: 'bad_video_id' })
    return
  }

  proxyStream(req, res, videoId).catch((error) => {
    if (LOG_LEVEL !== 'silent') {
      console.error(`[stream] ${videoId} fallo:`, error?.message || error)
    }
    if (!res.headersSent) {
      sendJson(res, 502, {
        error: 'stream_failed',
        message: String(error?.message || error),
      })
    } else {
      res.end()
    }
  })
})

server.listen(PORT, () => {
  console.log(`pop-music audio backend escuchando en http://0.0.0.0:${PORT}`)
})
