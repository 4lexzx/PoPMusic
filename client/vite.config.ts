import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  // Rutas relativas: necesario para que el EXE (file://) cargue los assets.
  base: './',
  server: {
    port: 5180,
    // En el navegador (sin Electron) el catálogo de Deezer pasa por este proxy.
    proxy: {
      '/dz': { target: 'https://api.deezer.com', changeOrigin: true, rewrite: (p) => p.replace(/^\/dz/, '') },
      '/ne': {
        target: 'https://music.163.com', changeOrigin: true, rewrite: (p) => p.replace(/^\/ne/, ''),
        headers: { Referer: 'https://music.163.com/' },
      },
    },
  },
})
