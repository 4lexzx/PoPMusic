const path = require('node:path')
const fs = require('node:fs')
const { YtDlp } = require('ytdlp-nodejs')

let binaryPath = null
try {
  const { app } = require('electron')
  if (app && app.isPackaged) {
    const candidate = path.join(
      process.resourcesPath,
      'app.asar.unpacked',
      'node_modules', 'ytdlp-nodejs', 'bin', 'yt-dlp.exe',
    )
    if (fs.existsSync(candidate)) binaryPath = candidate
  }
} catch {}

let ytdlp = null
function getYtdlp() {
  if (!ytdlp) ytdlp = binaryPath ? new YtDlp({ binaryPath }) : new YtDlp()
  return ytdlp
}

// Las URLs de googlevideo caducan (~6 h), así que se guardan poco tiempo.
const CACHE_TTL_MS = 90 * 60 * 1000
const cache = new Map()
const pending = new Map()

function cached(key) {
  const hit = cache.get(key)
  if (!hit) return null
  if (Date.now() - hit.at > CACHE_TTL_MS) { cache.delete(key); return null }
  return hit.url
}

async function resolveOnce(artist, title) {
  const y = getYtdlp()
  const query = `ytsearch1:${artist} ${title} official audio`.trim()
  const search = await y.getInfoAsync(query)
  const entry = Array.isArray(search.entries) ? search.entries[0] : search
  if (!entry) return null

  // La entrada de búsqueda solo trae metadatos: hace falta pedir el vídeo.
  const info = entry.formats ? entry : await y.getInfoAsync(entry.url)
  const formats = (info.formats || []).filter(
    (f) =>
      f.url &&
      f.protocol === 'https' &&
      String(f.vcodec || '') === 'none' &&
      String(f.acodec || '') !== 'none',
  )
  if (!formats.length) return null

  formats.sort((a, b) => (b.tbr || 0) - (a.tbr || 0))
  return formats[0].url
}

/** Devuelve la mejor URL de audio HTTPS para una canción. */
async function resolveAudio(artist, title) {
  const key = `${artist}|${title}`.toLowerCase()
  const hit = cached(key)
  if (hit) return hit
  if (pending.has(key)) return pending.get(key)

  const job = resolveOnce(artist, title)
    .then((url) => {
      if (url) cache.set(key, { url, at: Date.now() })
      return url
    })
    .finally(() => pending.delete(key))

  pending.set(key, job)
  return job
}

module.exports = { resolveAudio, binaryPath }
