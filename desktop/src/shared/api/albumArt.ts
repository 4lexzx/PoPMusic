/** Carátulas de álbum reales (iTunes), no fotogramas de vídeo. */

const cache = new Map<string, string | null>()

export async function resolveAlbumArt(title: string, artist: string): Promise<string | null> {
  const key = `${artist}|${title}`.toLowerCase().trim()
  if (cache.has(key)) return cache.get(key) ?? null

  try {
    const url = `https://itunes.apple.com/search?term=${encodeURIComponent(`${artist} ${title}`)}&entity=song&limit=1&country=US`
    const response = await fetch(url)
    const data = (await response.json()) as {
      results?: Array<{ artworkUrl100?: string }>
    }
    const art = data.results?.[0]?.artworkUrl100?.replace('100x100bb', '600x600bb') ?? null
    cache.set(key, art)
    return art
  } catch {
    cache.set(key, null)
    return null
  }
}

/** Sustituye la miniatura de vídeo por la carátula del álbum cuando existe. */
export async function withAlbumArt(
  track: { title: string; artist: string; thumbnail: string },
): Promise<string> {
  return (await resolveAlbumArt(track.title, track.artist)) || track.thumbnail
}
