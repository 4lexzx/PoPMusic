const { app, BrowserWindow, Menu, Tray, globalShortcut, ipcMain, nativeImage, shell } = require('electron')
const http = require('node:http')
const fs = require('node:fs')
const path = require('node:path')

const DEV_URL = process.env.VITE_DEV_SERVER_URL || 'http://localhost:5174'
const isDev = !app.isPackaged

let mainWindow = null
let overlayWindow = null
let tray = null
let staticServer = null
let localUrl = ''
let lastTrack = { title: 'PoP Music', artist: '', thumbnail: '', playing: false }
let lastDeepLink = null

const iconPath = path.join(__dirname, '..', 'icons', 'icon.ico')

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.wasm': 'application/wasm',
  '.ico': 'image/x-icon',
}

/**
 * La app se sirve desde localhost en lugar de file://: así el iframe de
 * YouTube recibe un origen válido y el reproductor funciona con normalidad.
 */
function startStaticServer() {
  const root = path.join(__dirname, '..', 'dist')
  return new Promise((resolve, reject) => {
    const server = http.createServer((request, response) => {
      let pathname = '/'
      try {
        pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname)
      } catch {
        response.writeHead(400).end()
        return
      }
      let file = path.join(root, pathname === '/' ? 'index.html' : pathname)
      if (!file.startsWith(root)) {
        response.writeHead(403).end()
        return
      }
      if (!fs.existsSync(file) || fs.statSync(file).isDirectory()) file = path.join(root, 'index.html')
      response.setHeader('Content-Type', MIME[path.extname(file)] || 'application/octet-stream')
      fs.createReadStream(file).pipe(response)
    })
    // Puerto fijo para que localStorage (biblioteca y ajustes) persista entre
    // ejecuciones. Si está ocupado, se usa uno libre como último recurso.
    const onListening = () => {
      staticServer = server
      localUrl = `http://127.0.0.1:${server.address().port}`
      resolve(localUrl)
    }
    server.on('error', (error) => {
      if (error.code === 'EADDRINUSE') {
        server.listen(0, '127.0.0.1', onListening)
      } else {
        reject(error)
      }
    })
    server.listen(41730, '127.0.0.1', onListening)
  })
}

function sendToRenderer(channel, payload) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send(channel, payload)
  }
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1360,
    height: 860,
    minWidth: 940,
    minHeight: 600,
    backgroundColor: '#0a0a0f',
    frame: false,
    show: false,
    icon: iconPath,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: true,
    },
  })

  mainWindow.loadURL(isDev ? DEV_URL : `${localUrl}/`)

  mainWindow.once('ready-to-show', () => mainWindow.show())

  mainWindow.on('maximize', () => sendToRenderer('window:maximized', true))
  mainWindow.on('unmaximize', () => sendToRenderer('window:maximized', false))

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/.test(url)) shell.openExternal(url)
    return { action: 'deny' }
  })

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

function createOverlay() {
  if (overlayWindow && !overlayWindow.isDestroyed()) {
    overlayWindow.show()
    return
  }

  overlayWindow = new BrowserWindow({
    width: 340,
    height: 104,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    backgroundColor: '#00000000',
    icon: iconPath,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  })

  overlayWindow.loadURL(isDev ? `${DEV_URL}/#/overlay` : `${localUrl}/#/overlay`)
  overlayWindow.setIgnoreMouseEvents(false)

  overlayWindow.webContents.on('did-finish-load', () => {
    overlayWindow.webContents.send('overlay:track', lastTrack)
  })

  overlayWindow.on('closed', () => {
    overlayWindow = null
  })
}

function handleDeepLink(url) {
  if (!url) return
  lastDeepLink = url
  sendToRenderer('deep-link', url)
  if (mainWindow) mainWindow.show()
}

function registerShortcuts() {
  const send = (command) => () => sendToRenderer('player:command', command)
  const candidates =
    process.platform === 'win32'
      ? // Windows no expone las teclas multimedia a globalShortcut.
        [
          ['CmdOrCtrl+Alt+P', 'play-pause'],
          ['CmdOrCtrl+Alt+Right', 'next'],
          ['CmdOrCtrl+Alt+Left', 'prev'],
        ]
      : [
          ['MediaPlayPause', 'play-pause'],
          ['MediaTrackNext', 'next'],
          ['MediaTrackPrevious', 'prev'],
          ['CmdOrCtrl+Alt+P', 'play-pause'],
          ['CmdOrCtrl+Alt+Right', 'next'],
          ['CmdOrCtrl+Alt+Left', 'prev'],
        ]
  const registered = new Set()
  for (const [accelerator, command] of candidates) {
    if (registered.has(command)) continue
    try {
      if (globalShortcut.register(accelerator, send(command))) registered.add(command)
    } catch {
      // Accelerator no soportado en este sistema: se prueba el siguiente.
    }
  }
}

function createTray() {
  try {
    const image = nativeImage.createFromPath(iconPath)
    tray = new Tray(image.isEmpty() ? nativeImage.createEmpty() : image)
    tray.setToolTip('PoP Music')
    tray.setContextMenu(
      Menu.buildFromTemplate([
        { label: 'Mostrar PoP Music', click: () => mainWindow?.show() },
        { label: 'Reproducir / pausa', click: () => sendToRenderer('player:command', 'play-pause') },
        { label: 'Siguiente', click: () => sendToRenderer('player:command', 'next') },
        { label: 'Anterior', click: () => sendToRenderer('player:command', 'prev') },
        { type: 'separator' },
        { label: 'Reproductor flotante', click: () => createOverlay() },
        { type: 'separator' },
        { label: 'Salir', click: () => app.quit() },
      ]),
    )
    tray.on('double-click', () => mainWindow?.show())
  } catch {
    // El icono del tray es opcional en algunos entornos.
  }
}

if (!app.requestSingleInstanceLock()) {
  app.quit()
} else {
  app.on('second-instance', (_event, argv) => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore()
      mainWindow.show()
      mainWindow.focus()
    }
    const protocolArg = argv.find((arg) => arg.startsWith('popmusic://'))
    if (protocolArg) handleDeepLink(protocolArg)
  })

  app.whenReady().then(async () => {
    if (process.platform === 'win32') app.setAsDefaultProtocolClient('popmusic')
    if (!isDev) await startStaticServer()
    createMainWindow()
    createTray()
    registerShortcuts()

    if (process.platform === 'darwin') {
      app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) createMainWindow()
      })
    }
  })

  app.on('open-url', (_event, url) => handleDeepLink(url))

  app.on('will-quit', () => {
    globalShortcut.unregisterAll()
    staticServer?.close()
  })

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit()
  })
}

ipcMain.on('window:minimize', () => mainWindow?.minimize())
ipcMain.on('window:maximize', () => {
  if (!mainWindow) return
  if (mainWindow.isMaximized()) mainWindow.unmaximize()
  else mainWindow.maximize()
})
ipcMain.on('window:close', () => mainWindow?.close())
ipcMain.on('overlay:toggle', () => {
  if (overlayWindow && !overlayWindow.isDestroyed()) overlayWindow.close()
  else createOverlay()
})
ipcMain.on('overlay:hide', () => overlayWindow?.hide())

ipcMain.on('track:update', (_event, track) => {
  lastTrack = { ...lastTrack, ...track }
  if (overlayWindow && !overlayWindow.isDestroyed()) {
    overlayWindow.webContents.send('overlay:track', lastTrack)
  }
})

/**
 * Proxy de peticiones a YouTube/Google: el renderer no puede hacerlas por CORS,
 * pero el proceso principal sí. Igual que hace Liner con su worker oculto.
 */
ipcMain.handle('youtube:fetch', async (_event, options) => {
  const url = new URL(String(options?.url ?? ''))
  const hosts = ['www.youtube.com', 'www.google.com', 'www.gstatic.com', 'youtube.com']
  if (url.protocol !== 'https:' || !hosts.includes(url.hostname)) {
    throw new Error('Dominio no permitido')
  }
  // youtubei.js envía el body sin method explícito: con body, es POST.
  const method = options.method || (options.body ? 'POST' : 'GET')
  const response = await fetch(url, {
    method,
    headers: options.headers ?? {},
    body: method === 'GET' || method === 'HEAD' ? undefined : options.body,
    signal: AbortSignal.timeout(25_000),
  })
  return {
    status: response.status,
    text: await response.text(),
    cookie: response.headers.get('set-cookie') ?? '',
  }
})

ipcMain.on('player:command', (_event, command) => sendToRenderer('player:command', command))
ipcMain.on('deep-link:request', () => {
  if (lastDeepLink) sendToRenderer('deep-link', lastDeepLink)
})
