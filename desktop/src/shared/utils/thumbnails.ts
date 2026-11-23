/** Carátulas en HD con degradación ordenada si YouTube no tiene la variante. */

export function hdThumbnail(videoId: string): string {
  return `https://i.ytimg.com/vi/${videoId}/hq720.jpg`
}

/** Sustituye la variante de baja calidad por una HD manteniendo el id. */
export function upgradeThumbnail(url: string | undefined, videoId?: string): string {
  if (videoId) return hdThumbnail(videoId)
  if (!url) return ''
  return url.replace(/\/(mqdefault|hqdefault|sddefault|maxresdefault|default)\.jpg/, '/hq720.jpg')
}

/** Siguiente variante a probar cuando una imagen falla. */
export function fallbackThumbnail(url: string): string | null {
  if (url.includes('hq720')) return url.replace('hq720', 'maxresdefault')
  if (url.includes('maxresdefault')) return url.replace('maxresdefault', 'mqdefault')
  if (url.includes('mqdefault')) return url.replace('mqdefault', 'hqdefault')
  return null
}
