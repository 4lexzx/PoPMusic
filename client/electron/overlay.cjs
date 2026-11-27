/**
 * Superposición en segundo plano: una píldora flotante, siempre encima, con
 * la canción que suena y sus controles. Aparece al minimizar la app (si está
 * activado en Ajustes → Superposición) o con Alt+Shift+O.
 */
const { BrowserWindow, ipcMain, screen } = require('electron')

const SIZE = { width: 360, height: 64 }
const MARGIN = 18

let deps = null
let pill = null
let config = { enabled: true, onMinimize: true, anchor: 'center' }
let state = null // última canción/estado enviados por la ventana principal
let manual = false // la mostró el usuario con el atajo

function init(d) { deps = d }

function position() {
  const area = screen.getPrimaryDisplay().workArea
  const x = config.anchor === 'left' ? area.x + MARGIN
    : config.anchor === 'right' ? area.x + area.width - SIZE.width - MARGIN
      : area.x + Math.round((area.width - SIZE.width) / 2)
  return { x, y: area.y + MARGIN, ...SIZE }
}

function ensure() {
  if (pill && !pill.isDestroyed()) return pill
  pill = new BrowserWindow({
    ...position(),
    frame: false,
    transparent: true,
    backgroundColor: '#00000000',
    resizable: false,
    movable: true,
    minimizable: false,
    maximizable: false,
    skipTaskbar: true,
    alwaysOnTop: true,
    hasShadow: false,
    show: false,
    focusable: true,
    webPreferences: { preload: deps.preload, contextIsolation: true, nodeIntegration: false, backgroundThrottling: false },
  })
  pill.setAlwaysOnTop(true, 'screen-saver')
  pill.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen: true })
  deps.loadUi(pill, 'overlay')
  pill.webContents.once('did-finish-load', () => state && pill.webContents.send('overlay-state', state))
  pill.on('closed', () => { pill = null })
  return pill
}

function show() {
  if (!config.enabled || !state?.track) return
  const w = ensure()
  w.setBounds(position())
  w.showInactive()
  w.webContents.send('overlay-visible', true)
}

function hide() {
  manual = false
  if (!pill || pill.isDestroyed() || !pill.isVisible()) return
  pill.webContents.send('overlay-visible', false)
  setTimeout(() => pill && !pill.isDestroyed() && pill.hide(), 140)
}

function toggle() {
  if (pill && !pill.isDestroyed() && pill.isVisible()) hide()
  else { manual = true; show() }
}

function onMainMinimized() { if (config.onMinimize) show() }
function onMainRestored() { if (!manual) hide() }
function destroy() { if (pill && !pill.isDestroyed()) pill.destroy(); pill = null }

function registerIpc() {
  // La ventana principal informa de lo que suena.
  ipcMain.on('now-playing', (_e, s) => {
    state = s
    if (pill && !pill.isDestroyed()) pill.webContents.send('overlay-state', s)
    if (!s?.track) hide()
  })
  // La píldora ya escucha: se le manda lo que está sonando.
  ipcMain.on('overlay-ready', (e) => { if (state) e.sender.send('overlay-state', state) })
  ipcMain.on('overlay-config', (_e, c) => {
    config = { ...config, ...c }
    if (!config.enabled) hide()
    else if (pill && !pill.isDestroyed() && pill.isVisible()) pill.setBounds(position())
  })
  // Botones de la píldora → ventana principal.
  ipcMain.on('overlay-command', (_e, cmd) => {
    const main = deps?.getMain()
    if (!main || main.isDestroyed()) return
    if (cmd === 'open') {
      if (main.isMinimized()) main.restore()
      main.show()
      main.focus()
      hide()
      return
    }
    if (cmd === 'close') { hide(); return }
    main.webContents.send('remote-command', cmd)
  })
}

module.exports = { init, registerIpc, onMainMinimized, onMainRestored, toggle, destroy }
