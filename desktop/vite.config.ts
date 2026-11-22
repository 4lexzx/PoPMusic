import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from 'node:url'

/** La app se abre desde file:// en Electron, por eso las rutas son relativas. */
export default defineConfig({
  base: './',
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5174,
    strictPort: false,
  },
  build: {
    target: 'es2020',
    sourcemap: false,
    outDir: 'dist',
  },
})
