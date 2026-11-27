const { contextBridge, ipcRenderer } = require('electron')

const listen = (channel) => (cb) => {
  const handler = (_e, payload) => cb(payload)
  ipcRenderer.on(channel, handler)
  return () => ipcRenderer.removeListener(channel, handler)
}

contextBridge.exposeInMainWorld('popApi', {
  resolveAudio: (artist, title, fresh) =>
    ipcRenderer.invoke('resolve-audio', artist, title, { fresh: !!fresh, priority: 'play' }),
  prefetchAudio: (artist, title, priority) => ipcRenderer.send('prefetch-audio', artist, title, priority),
  debug: (msg) => ipcRenderer.send('debug', msg),
  deezer: (apiPath) => ipcRenderer.invoke('deezer', apiPath),
  netease: (apiPath) => ipcRenderer.invoke('netease', apiPath),
  cache: (action) => ipcRenderer.invoke('cache', action),
  windowAction: (action) => ipcRenderer.send('window', action),
  onWindowState: listen('window-state'),
  getBounds: () => ipcRenderer.invoke('get-bounds'),
  setBounds: (b) => ipcRenderer.send('set-bounds', b),
  // Superposición en segundo plano
  sendNowPlaying: (state) => ipcRenderer.send('now-playing', state),
  overlayConfig: (config) => ipcRenderer.send('overlay-config', config),
  overlayCommand: (cmd) => ipcRenderer.send('overlay-command', cmd),
  overlayReady: () => ipcRenderer.send('overlay-ready'),
  onOverlayState: listen('overlay-state'),
  onOverlayVisible: listen('overlay-visible'),
  onRemoteCommand: listen('remote-command'),
})
