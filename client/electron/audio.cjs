/**
 * Audio completo con yt-dlp.
 *
 * Cada resolución cuesta ~6 s (2 s solo en arrancar el ejecutable), así que lo
 * importante es tenerla hecha ANTES del clic:
 *  - cola con prioridad: "play" (el usuario quiere oírla ya) adelanta a las
 *    precargas ("hover" = ratón encima, "next" = siguiente de la cola,
 *    "idle" = probables primeras reproducciones);
 *  - como mucho 2 yt-dlp a la vez para no saturar la CPU ni la red;
 *  - caché en disco que sobrevive a reinicios (hasta que caduca la URL).
 */
const path = require('node:path')
const fs = require('node:fs')
const { execFile } = require('node:child_process')

// El ratón encima es la señal más clara de que vas a hacer clic: va antes que la cola.
const PRIORITY = { play: 0, hover: 1, next: 2, idle: 3 }
const MAX_PARALLEL = 2

let binaryPath = null
let cacheFile = null
try {
  const { app } = require('electron')
  if (app && app.isPackaged) {
    const candidate = path.join(process.resourcesPath, 'app.asar.unpacked', 'node_modules', 'ytdlp-nodejs', 'bin', 'yt-dlp.exe')
    if (fs.existsSync(candidate)) binaryPath = candidate
  }
  if (app) cacheFile = path.join(app.getPath('userData'), 'audio-urls.json')
} catch {}

function ytdlpBinary() {
  if (binaryPath) return binaryPath
  const exe = process.platform === 'win32' ? 'yt-dlp.exe' : 'yt-dlp'
  const candidates = [path.join(__dirname, '..', 'node_modules', 'ytdlp-nodejs', 'bin', exe)]
  try {
    candidates.push(path.join(path.dirname(require.resolve('ytdlp-nodejs')), '..', 'bin', exe))
  } catch {}
  return candidates.find((c) => fs.existsSync(c)) || null
}

// ---------- caché (memoria + disco) ----------
/** @type {Map<string, {url: string, until: number}>} */
const cache = new Map()

/** Las URLs de googlevideo traen `expire=<segundos>`: se usan hasta 20 min antes. */
function expiryOf(url) {
  const m = /[?&]expire=(\d+)/.exec(url)
  const at = m ? Number(m[1]) * 1000 : Date.now() + 5 * 3600 * 1000
  return at - 20 * 60 * 1000
}

function loadDisk() {
  if (!cacheFile) return
  try {
    const data = JSON.parse(fs.readFileSync(cacheFile, 'utf8'))
    const now = Date.now()
    for (const [k, v] of Object.entries(data)) if (v && v.until > now) cache.set(k, v)
  } catch {}
}
let saveTimer = null
function saveDisk() {
  if (!cacheFile || saveTimer) return
  saveTimer = setTimeout(() => {
    saveTimer = null
    const now = Date.now()
    const out = {}
    for (const [k, v] of cache) if (v.until > now) out[k] = v
    fs.promises.writeFile(cacheFile, JSON.stringify(out)).catch(() => {})
  }, 1500)
}
loadDisk()

function cached(key) {
  const hit = cache.get(key)
  if (!hit) return null
  if (Date.now() > hit.until) { cache.delete(key); return null }
  return hit.url
}

// ---------- cola con prioridad ----------
/** @type {{key: string, artist: string, title: string, prio: number, waiters: Function[]}[]} */
const queue = []
const running = new Map()
let active = 0

function runYtdlp(artist, title) {
  const bin = ytdlpBinary()
  if (!bin) return Promise.reject(new Error('yt-dlp no encontrado'))
  const query = `ytsearch1:${artist} ${title} official audio`.trim()
  const args = ['-f', 'bestaudio[protocol=https]/bestaudio', '-g', '--no-warnings', '--no-playlist', '--socket-timeout', '10', query]
  return new Promise((resolve, reject) => {
    execFile(bin, args, { timeout: 45000, windowsHide: true, maxBuffer: 1 << 20 }, (err, stdout) => {
      if (err) return reject(err)
      resolve(String(stdout).split(/\s+/).find((l) => l.startsWith('https://')) || null)
    })
  })
}

function pump() {
  queue.sort((a, b) => a.prio - b.prio)
  while (active < MAX_PARALLEL && queue.length) {
    const job = queue.shift()
    active++
    const work = runYtdlp(job.artist, job.title)
      .catch((error) => { console.error('[PoP Music] yt-dlp falló:', String(error && error.message).slice(0, 200)); return null })
      .then((url) => {
        if (url) { cache.set(job.key, { url, until: expiryOf(url) }); saveDisk() }
        return url
      })
      .finally(() => { active--; running.delete(job.key); pump() })
    running.set(job.key, work)
    work.then((url) => job.waiters.forEach((w) => w(url)))
  }
}

/**
 * URL del mejor audio de una canción.
 * @param {string} artist
 * @param {string} title
 * @param {{ fresh?: boolean, priority?: 'play' | 'next' | 'hover' | 'idle' }} [opts]
 */
function resolveAudio(artist, title, opts = {}) {
  const key = `${artist}|${title}`.toLowerCase()
  const prio = PRIORITY[opts.priority || 'play'] ?? 0
  if (opts.fresh) cache.delete(key)
  const hit = cached(key)
  if (hit) return Promise.resolve(hit)
  if (running.has(key)) return running.get(key)
  const waiting = queue.find((j) => j.key === key)
  if (waiting) {
    waiting.prio = Math.min(waiting.prio, prio)
    pump()
    return new Promise((r) => waiting.waiters.push(r))
  }
  // Las precargas viejas sobran: se limita la cola para no acumular trabajo.
  if (prio !== PRIORITY.play) {
    const stale = queue.filter((j) => j.prio === prio)
    if (stale.length >= 3) {
      const drop = stale[0]
      queue.splice(queue.indexOf(drop), 1)
      drop.waiters.forEach((w) => w(null))
    }
  }
  return new Promise((resolve) => {
    queue.push({ key, artist, title, prio, waiters: [resolve] })
    pump()
  })
}

/** ¿Ya está resuelta? (para que la interfaz sepa si sonará al instante). */
function isCached(artist, title) {
  return !!cached(`${artist}|${title}`.toLowerCase())
}

module.exports = { resolveAudio, isCached, binaryPath, ytdlpBinary }
