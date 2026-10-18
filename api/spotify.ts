import { handleSpotify } from '../server/spotify'

/**
 * Serverless Function de Vercel.
 *
 * Ruta pública: GET /api/spotify?action=playlist&id=<playlistId>
 *
 * Mantiene el Client Secret de Spotify en el servidor y resuelve el flujo
 * Client Credentials (que no puede ejecutarse desde el navegador).
 */

interface MinimalRequest {
  method?: string
  query?: Record<string, string | string[]>
}

interface MinimalResponse {
  statusCode: number
  setHeader: (name: string, value: string) => void
  end: (body?: string) => void
}

export default async function handler(req: MinimalRequest, res: MinimalResponse): Promise<void> {
  const result = await handleSpotify(req.query ?? {})
  res.statusCode = result.status
  res.setHeader('Content-Type', 'application/json; charset=utf-8')
  res.setHeader('Cache-Control', 'no-store')
  res.end(JSON.stringify(result.body))
}
