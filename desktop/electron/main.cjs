const { app, BrowserWindow, Menu, Tray, globalShortcut, ipcMain, nativeImage, shell } = require('electron')
const { pathToFileURL } = require('node:url')
const path = require('node:path')

const DEV_URL = process.env.VITE_DEV_SERVER_URL || 'http://localhost:5174'
const isDev = !app.isPackaged

let mainWindow = null
let overlayWindow = null
let tray = null
let lastTrack = { title: 'PoP Music', artist: '', thumbnail: '', playing: false }
let lastDeepLink = null

const iconPath = path.join(__dirname, '..', 'icons', 'icon.ico')

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

  if (isDev) {
    mainWindow.loadURL(DEV_URL)
  } else {
    mainWindow.loadURL(pathToFileURL(path.join(__dirname, '..', 'dist', 'index.html')).href)
  }

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

  const url = isDev ? `${DEV_URL}/#/overlay` : `${pathToFileURL(path.join(__dirname, '..', 'dist', 'index.html')).href}#/overlay`
  overlayWindow.loadURL(url)
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
  globalShortcut.register('MediaPlayPause', () => sendToRenderer('player:command', 'play-pause'))
  globalShortcut.register('MediaTrackNext', () => sendToRenderer('player:command', 'next'))
  globalShortcut.register('MediaTrackPrevious', () => sendToRenderer('player:command', 'prev'))
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

  app.whenReady().then(() => {
    if (process.platform === 'win32') app.setAsDefaultProtocolClient('popmusic')
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

ipcMain.on('player:command', (_event, command) => sendToRenderer('player:command', command))
ipcMain.on('deep-link:request', () => {
  if (lastDeepLink) sendToRenderer('deep-link', lastDeepLink)
})
