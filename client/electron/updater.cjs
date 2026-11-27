/**
 * Actualizaciones de la app de escritorio.
 *
 * Se buscan en las releases del repositorio: las de PC llevan la etiqueta
 * `pc-vX.Y.Z` y un instalador .exe. Si hay una versión mayor que la actual,
 * la interfaz muestra el aviso; al aceptar se descarga el instalador (con
 * progreso), se ejecuta en silencio y la app se vuelve a abrir sola.
 */
const { app, ipcMain } = require('electron')
const fs = require('node:fs')
const path = require('node:path')
const os = require('node:os')
const { spawn } = require('node:child_process')

const REPO = '4lexzx/PoPMusic'
const ALLOWED_HOSTS = ['github.com', 'objects.githubusercontent.com', 'release-assets.githubusercontent.com']

function parse(v) {
  return String(v || '').replace(/^pc-v|^v/, '').split('.').map((n) => parseInt(n, 10) || 0)
}
function newer(a, b) {
  const x = parse(a)
  const y = parse(b)
  for (let i = 0; i < 3; i++) if ((x[i] || 0) !== (y[i] || 0)) return (x[i] || 0) > (y[i] || 0)
  return false
}

async function check() {
  const res = await fetch(`https://api.github.com/repos/${REPO}/releases?per_page=30`, {
    headers: { 'User-Agent': 'PoPMusic-Desktop', Accept: 'application/vnd.github+json' },
  })
  if (!res.ok) throw new Error(`GitHub ${res.status}`)
  const releases = await res.json()
  // POP_FAKE_VERSION (solo en desarrollo) permite probar el aviso sin publicar nada.
  const current = (!app.isPackaged && process.env.POP_FAKE_VERSION) || app.getVersion()
  const pc = releases
    .filter((r) => !r.draft && !r.prerelease && /^pc-v\d+\.\d+\.\d+$/.test(r.tag_name))
    .sort((a, b) => (newer(a.tag_name, b.tag_name) ? -1 : 1))[0]
  if (!pc || !newer(pc.tag_name, current)) return { available: false, current }
  const asset = (pc.assets || []).find((a) => /\.exe$/i.test(a.name))
  if (!asset) return { available: false, current }
  return {
    available: true,
    current,
    version: pc.tag_name.replace(/^pc-v/, ''),
    notes: pc.body || '',
    page: pc.html_url,
    asset: { name: asset.name, url: asset.browser_download_url, size: asset.size },
  }
}

async function download(url, size, onProgress) {
  const u = new URL(url)
  if (!ALLOWED_HOSTS.includes(u.hostname)) throw new Error('Origen de descarga no permitido')
  const res = await fetch(url, { headers: { 'User-Agent': 'PoPMusic-Desktop' }, redirect: 'follow' })
  if (!res.ok || !res.body) throw new Error(`Descarga ${res.status}`)
  const total = Number(res.headers.get('content-length')) || size || 0
  const file = path.join(os.tmpdir(), `PoPMusic-Setup-${Date.now()}.exe`)
  const out = fs.createWriteStream(file)
  let done = 0
  const reader = res.body.getReader()
  for (;;) {
    const { value, done: end } = await reader.read()
    if (end) break
    done += value.length
    if (!out.write(Buffer.from(value))) await new Promise((r) => out.once('drain', r))
    onProgress(done, total)
  }
  await new Promise((r, j) => out.end((err) => (err ? j(err) : r())))
  return file
}

function register() {
  ipcMain.handle('app-version', () => app.getVersion())
  ipcMain.handle('update-check', async () => {
    try { return await check() } catch (e) { return { available: false, error: String(e && e.message) } }
  })
  ipcMain.handle('update-download', async (event, url, size) => {
    const file = await download(url, size, (done, total) => {
      if (!event.sender.isDestroyed()) event.sender.send('update-progress', { done, total })
    })
    return file
  })
  ipcMain.on('update-install', (_e, file) => {
    if (typeof file !== 'string' || !file.startsWith(os.tmpdir()) || !fs.existsSync(file)) return
    // Instalación silenciosa; --force-run vuelve a abrir la app al terminar.
    spawn(file, ['/S', '--force-run'], { detached: true, stdio: 'ignore' }).unref()
    setTimeout(() => app.quit(), 400)
  })
}

module.exports = { register, newer }
