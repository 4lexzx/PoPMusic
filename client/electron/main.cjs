const { app, BrowserWindow, ipcMain, shell, session, screen, globalShortcut } = require('electron')
const path = require('node:path')
const fs = require('node:fs')
const { resolveAudio } = require('./audio.cjs')
const overlay = require('./overlay.cjs')

function debug(msg) {
  try {
    const dir = app.getPath('userData')
    fs.appendFileSync(path.join(dir, 'audio-debug.log'), `${new Date().toISOString()} ${msg}\n`)
  } catch {}
  console.log('[PoP Music]', msg)
}

const DEV = process.env.NODE_ENV === 'development' || process.env.POP_DEV === '1'
const DEV_URL = 'http://localhost:5180'

/** Carga la interfaz (con `hash` para la superposición). */
function loadUi(target, hash = '') {
  if (DEV) target.loadURL(DEV_URL + (hash ? `/#${hash}` : ''))
  else target.loadFile(path.join(__dirname, '..', 'dist', 'index.html'), hash ? { hash } : undefined)
}

let win = null

function createWindow() {
  win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 960,
    minHeight: 620,
    // Ventana transparente y sin marco: la propia app dibuja esquinas
    // redondeadas (Windows 10 no las redondea por sí mismo).
    frame: false,
    transparent: true,
    backgroundColor: '#00000000',
    hasShadow: true,
    icon: path.join(__dirname, '..', 'icons', 'icon.png'),
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      // Que el audio siga sonando con la ventana minimizada.
      backgroundThrottling: false,
    },
  })

  win.once('ready-to-show', () => win.show())
  const sendState = () => win && !win.isDestroyed() &&
    win.webContents.send('window-state', { maximized: win.isMaximized() })
  win.on('maximize', sendState)
  win.on('unmaximize', sendState)
  win.on('minimize', () => overlay.onMainMinimized())
  win.on('restore', () => overlay.onMainRestored())
  win.on('focus', () => overlay.onMainRestored())
  win.on('closed', () => { win = null; overlay.destroy() })

  win.webContents.on('did-fail-load', (_e, code, desc, url) => debug(`did-fail-load ${code} ${desc} ${url}`))
  win.webContents.on('render-process-gone', (_e, details) => debug(`render-process-gone ${JSON.stringify(details)}`))

  loadUi(win)
  overlay.init({ getMain: () => win, loadUi, preload: path.join(__dirname, 'preload.cjs') })

  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })
}

app.whenReady().then(() => {
  ipcMain.on('debug', (_event, msg) => debug(`renderer: ${msg}`))

  // Catálogo musical de Deezer: su API no admite CORS, así que se consulta
  // desde aquí. Solo se permite api.deezer.com y solo lectura (GET).
  ipcMain.handle('deezer', async (_event, apiPath) => {
    if (typeof apiPath !== 'string' || !/^[a-z0-9/_?=&%.,:+-]+$/i.test(apiPath)) return null
    try {
      const res = await fetch(`https://api.deezer.com/${apiPath.replace(/^\/+/, '')}`, {
        headers: { 'Accept-Language': app.getLocale() || 'es' },
      })
      if (!res.ok) return null
      // Se devuelve también el error de cuota ({ error: { code: 4 } }) para que el cliente reintente.
      return await res.json()
    } catch (error) {
      debug(`deezer ${apiPath}: ${error && error.message}`)
      return null
    }
  })

  // Letras palabra por palabra de NetEase (tampoco admite CORS). Solo lectura
  // de búsqueda y letras.
  ipcMain.handle('netease', async (_event, apiPath) => {
    if (typeof apiPath !== 'string' || !/^api\/(search\/get|song\/lyric\/v1)\?[a-z0-9_=&%.,:+-]+$/i.test(apiPath)) return null
    try {
      const res = await fetch(`https://music.163.com/${apiPath}`, { headers: { Referer: 'https://music.163.com/' } })
      return res.ok ? await res.json() : null
    } catch {
      return null
    }
  })

  // Caché de imágenes y audio del navegador interno (Ajustes → Almacenamiento).
  ipcMain.handle('cache', async (_event, action) => {
    const ses = session.defaultSession
    if (action === 'clear') { await ses.clearCache(); return 0 }
    return ses.getCacheSize()
  })

  ipcMain.on('window', (event, action) => {
    const target = BrowserWindow.fromWebContents(event.sender)
    if (!target) return
    if (action === 'minimize') target.minimize()
    else if (action === 'maximize') target.isMaximized() ? target.unmaximize() : target.maximize()
    else if (action === 'close') target.close()
  })

  // Redimensionar desde los bordes (las ventanas transparentes de Windows no
  // tienen borde nativo para hacerlo).
  ipcMain.handle('get-bounds', (event) => BrowserWindow.fromWebContents(event.sender)?.getBounds())
  ipcMain.on('set-bounds', (event, b) => {
    const target = BrowserWindow.fromWebContents(event.sender)
    if (!target || target.isMaximized() || !b) return
    const [minW, minH] = target.getMinimumSize()
    const area = screen.getDisplayMatching(b).workArea
    target.setBounds({
      x: Math.round(b.x),
      y: Math.round(Math.max(area.y - 10, b.y)),
      width: Math.round(Math.max(minW, b.width)),
      height: Math.round(Math.max(minH, b.height)),
    })
  })

  ipcMain.handle('resolve-audio', async (_event, artist, title, opts) => {
    const options = typeof opts === 'object' && opts ? opts : { fresh: !!opts }
    try {
      const url = await resolveAudio(artist, title, options)
      if (options.priority === 'play' || !options.priority) debug(`audio ${url ? 'ok' : 'sin URL'}: ${artist} - ${title}`)
      return url
    } catch (error) {
      debug(`ERROR ${error && error.message ? error.message : String(error)}`)
      return null
    }
  })
  // Precarga sin esperar respuesta (al pasar el ratón, siguiente de la cola…).
  ipcMain.on('prefetch-audio', (_event, artist, title, priority) => {
    resolveAudio(artist, title, { priority: priority || 'hover' }).catch(() => null)
  })

  overlay.registerIpc()
  createWindow()

  // Atajo global para mostrar/ocultar la superposición.
  globalShortcut.register('Alt+Shift+O', () => overlay.toggle())

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('will-quit', () => globalShortcut.unregisterAll())
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
