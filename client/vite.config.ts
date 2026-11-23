import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  // Rutas relativas: necesario para que el EXE (file://) cargue los assets.
  base: './',
  server: { port: 5180 },
})
