const { app, BrowserWindow, ipcMain, shell } = require('electron')
const path = require('node:path')
const fs = require('node:fs')
const { resolveAudio } = require('./audio.cjs')

function debug(msg) {
  try {
    const dir = app.getPath('userData')
    fs.appendFileSync(path.join(dir, 'audio-debug.log'), `${new Date().toISOString()} ${msg}\n`)
  } catch {}
  console.log('[PoP Music]', msg)
}

const DEV_URL = 'http://localhost:5180'

let win = null

function createWindow() {
  win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    backgroundColor: '#f2f2f4',
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

  win.webContents.on('did-fail-load', (_e, code, desc, url) => {
    debug(`did-fail-load ${code} ${desc} ${url}`)
  })
  win.webContents.on('render-process-gone', (_e, details) => {
    debug(`render-process-gone ${JSON.stringify(details)}`)
  })
  win.webContents.on('console-message', (_e, level, message) => {
    if (level >= 2) debug(`consola(${level}): ${message}`)
  })

  if (process.env.NODE_ENV === 'development') {
    win.loadURL(DEV_URL)
  } else {
    win.loadFile(path.join(__dirname, '..', 'dist', 'index.html'))
  }

  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })
}

app.whenReady().then(() => {
  ipcMain.on('debug', (_event, msg) => debug(`renderer: ${msg}`))

  ipcMain.handle('resolve-audio', async (_event, artist, title) => {
    debug(`resolve-audio: ${artist} - ${title}`)
    try {
      const url = await resolveAudio(artist, title)
      debug(url ? `URL obtenida (${url.length} chars)` : 'sin URL')
      return url
    } catch (error) {
      debug(`ERROR ${error && error.message ? error.message : String(error)}`)
      return null
    }
  })

  createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
