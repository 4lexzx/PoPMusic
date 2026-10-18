import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from 'node:url'
import { handleSpotify } from './server/spotify'

/**
 * Middleware de desarrollo: emula la Serverless Function de Vercel (/api/spotify)
 * para poder importar playlists de Spotify con `npm run dev` sin necesidad de `vercel dev`.
 */
function spotifyDevApi(): Plugin {
  return {
    name: 'spotify-dev-api',
    configureServer(server) {
      server.middlewares.use('/api/spotify', async (req, res) => {
        try {
          const url = new URL(req.url ?? '', 'http://localhost')
          const query: Record<string, string> = {}
          url.searchParams.forEach((value, key) => {
            query[key] = value
          })
          const result = await handleSpotify(query)
          res.statusCode = result.status
          res.setHeader('Content-Type', 'application/json; charset=utf-8')
          res.end(JSON.stringify(result.body))
        } catch (error) {
          res.statusCode = 500
          res.setHeader('Content-Type', 'application/json; charset=utf-8')
          res.end(JSON.stringify({ error: String(error) }))
        }
      })
    },
  }
}

export default defineConfig({
  plugins: [react(), tailwindcss(), spotifyDevApi()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 5173,
  },
  build: {
    target: 'es2020',
    sourcemap: false,
  },
})
