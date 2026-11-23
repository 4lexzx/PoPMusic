const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('popApi', {
  resolveAudio: (artist, title) =>
    ipcRenderer.invoke('resolve-audio', artist, title),
  debug: (msg) => ipcRenderer.send('debug', msg),
})
