const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('popDesktop', {
  isElectron: true,
  minimize: () => ipcRenderer.send('window:minimize'),
  maximize: () => ipcRenderer.send('window:maximize'),
  close: () => ipcRenderer.send('window:close'),
  toggleOverlay: () => ipcRenderer.send('overlay:toggle'),
  hideOverlay: () => ipcRenderer.send('overlay:hide'),
  sendTrack: (track) => ipcRenderer.send('track:update', track),
  sendCommand: (command) => ipcRenderer.send('player:command', command),
  requestDeepLink: () => ipcRenderer.send('deep-link:request'),
  youtubeFetch: (options) => ipcRenderer.invoke('youtube:fetch', options),
  onMaximized: (callback) => {
    ipcRenderer.on('window:maximized', (_event, value) => callback(value))
    return () => ipcRenderer.removeAllListeners('window:maximized')
  },
  onCommand: (callback) => {
    ipcRenderer.on('player:command', (_event, command) => callback(command))
    return () => ipcRenderer.removeAllListeners('player:command')
  },
  onTrack: (callback) => {
    ipcRenderer.on('overlay:track', (_event, track) => callback(track))
    return () => ipcRenderer.removeAllListeners('overlay:track')
  },
  onDeepLink: (callback) => {
    ipcRenderer.on('deep-link', (_event, url) => callback(url))
    return () => ipcRenderer.removeAllListeners('deep-link')
  },
})
