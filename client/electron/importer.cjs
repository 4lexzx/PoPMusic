/**
 * Importar listas desde un enlace (proceso principal: sin CORS y con yt-dlp).
 *
 * Devuelve siempre la misma forma, venga de donde venga:
 *   { source, title, cover, items: [{ title, artist, durationMs, cover }] }
 * La interfaz después busca cada canción en el catálogo para tener carátula HD.
 *
 *  - Spotify: página pública de "embed" (playlist, álbum o canción).
 *  - YouTube / YouTube Music / SoundCloud: yt-dlp en modo lista (--flat-playlist).
 *  - Apple Music: datos que trae la propia página pública.
 *  - Deezer: los enlaces cortos (link.deezer.com) se resuelven aquí; el resto
 *    lo lee la interfaz con la API del catálogo.
 */
const { execFile } = require('node:child_process')

const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130 Safari/537.36'

function detect(raw) {
  let url
  try { url = new URL(raw.trim()) } catch { return null }
  const host = url.hostname.toLowerCase().replace(/^www\./, '')
  const is = (d) => host === d || host.endsWith(`.${d}`)
  if (is('youtube.com') || host === 'youtu.be') return { kind: 'youtube', url }
  if (is('soundcloud.com') || host === 'on.soundcloud.com') return { kind: 'soundcloud', url }
  if (is('spotify.com') || host === 'spotify.link') return { kind: 'spotify', url }
  if (is('deezer.com') || host === 'deezer.page.link') return { kind: 'deezer', url }
  if (is('music.apple.com')) return { kind: 'apple', url }
  return null
}

async function getHtml(url) {
  const res = await fetch(url, { headers: { 'User-Agent': UA, 'Accept-Language': 'es,en;q=0.8' }, redirect: 'follow' })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return { html: await res.text(), finalUrl: res.url }
}

// ---------- Spotify ----------
async function spotify(url) {
  let target = url
  if (url.hostname === 'spotify.link') target = new URL((await getHtml(url.href)).finalUrl)
  const m = /\/(playlist|album|track)\/([A-Za-z0-9]+)/.exec(target.pathname)
  if (!m) throw new Error('Enlace de Spotify no reconocido')
  const { html } = await getHtml(`https://open.spotify.com/embed/${m[1]}/${m[2]}`)
  const json = /<script id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/.exec(html)
  if (!json) throw new Error('Spotify no devolvió la lista')
  const entity = JSON.parse(json[1])?.props?.pageProps?.state?.data?.entity
  if (!entity) throw new Error('Spotify no devolvió la lista')
  const cover = entity.coverArt?.sources?.sort((a, b) => (b.width || 0) - (a.width || 0))[0]?.url
    || entity.visualIdentity?.image?.sort?.((a, b) => (b.maxWidth || 0) - (a.maxWidth || 0))[0]?.url || ''
  const list = entity.trackList?.length ? entity.trackList
    : [{ title: entity.name || entity.title, subtitle: (entity.artists || []).map((a) => a.name).join(', '), duration: entity.duration }]
  return {
    source: 'spotify',
    title: entity.name || entity.title || 'Lista de Spotify',
    cover,
    items: list.map((t) => ({
      title: t.title,
      artist: String(t.subtitle || '').replace(/ /g, ' ').split(/,\s*/)[0].trim(),
      durationMs: t.duration || 0,
    })),
  }
}

// ---------- YouTube / SoundCloud (yt-dlp) ----------
function ytFlat(bin, url) {
  return new Promise((resolve, reject) => {
    execFile(bin, ['--flat-playlist', '-J', '--no-warnings', url], { timeout: 120000, windowsHide: true, maxBuffer: 64 << 20 }, (err, stdout) => {
      if (err) return reject(new Error('No se pudo leer la lista (¿es pública?)'))
      try { resolve(JSON.parse(String(stdout))) } catch { reject(new Error('Respuesta no válida')) }
    })
  })
}

/** "Artista - Canción (Official Video)" → { artist, title } limpios. */
function cleanVideoTitle(rawTitle, channel) {
  let title = String(rawTitle || '')
    // Cualquier paréntesis o corchete de vídeo: "( Video Lyric )", "[Official Audio]", "(Performance Video)", "(En Vivo)"…
    .replace(/\s*[([][^)\]]*\b(official|oficial|video|v[ií]deo|audio|lyrics?|letra|visuali[sz]er|performance|en vivo|live|hd|4k|mv|m\/v|remaster(ed)?)\b[^)\]]*[)\]]/gi, '')
    .replace(/\s*\|.*$/, '')
    .replace(/\s*\/\/.*$/, '')
    .replace(/\s*(official (music )?video|video oficial|lyrics?|letra)\s*$/i, '')
    .replace(/\s+/g, ' ')
    .trim()
  let artist = String(channel || '').replace(/\s*-\s*topic$/i, '').replace(/vevo$/i, '').replace(/\s*(official|oficial)$/i, '').trim()
  const dash = title.split(/\s+[-–—]\s+/)
  if (dash.length >= 2) {
    const norm = (x) => x.toLowerCase().replace(/[^a-z0-9]/g, '')
    const ch = norm(artist)
    // "Canción - Artista": si el canal coincide con la parte derecha, van al revés.
    const reversed = ch && norm(dash[dash.length - 1]).includes(ch) && !norm(dash[0]).includes(ch)
    if (reversed) { title = dash.slice(0, -1).join(' - ').trim(); artist = dash[dash.length - 1].trim() }
    else { artist = dash[0].trim(); title = dash.slice(1).join(' - ').trim() }
  }
  return { artist, title }
}

async function ytdlpList(bin, url, source) {
  const data = await ytFlat(bin, url.href)
  const entries = Array.isArray(data.entries) ? data.entries : [data]
  return {
    source,
    title: data.title || (source === 'soundcloud' ? 'Lista de SoundCloud' : 'Lista de YouTube'),
    cover: data.thumbnails?.slice(-1)[0]?.url || '',
    items: entries.filter(Boolean).map((e) => {
      const clean = source === 'soundcloud'
        ? { artist: e.uploader || '', title: e.title || '' }
        : cleanVideoTitle(e.title, e.channel || e.uploader)
      return {
        ...clean,
        durationMs: e.duration ? Math.round(e.duration * 1000) : 0,
        cover: e.thumbnails?.slice(-1)[0]?.url || '',
      }
    }).filter((x) => x.title),
  }
}

// ---------- Apple Music ----------
async function apple(url) {
  const { html } = await getHtml(url.href)
  const m = /id="serialized-server-data">([\s\S]*?)<\/script>/.exec(html)
  if (!m) throw new Error('Apple Music no devolvió la lista')
  const data = JSON.parse(m[1])
  const items = []
  const seen = new Set()
  const walk = (o) => {
    if (!o || typeof o !== 'object') return
    if (Array.isArray(o)) { o.forEach(walk); return }
    if (typeof o.title === 'string' && typeof o.artistName === 'string' && typeof o.duration === 'number') {
      const key = `${o.title}|${o.artistName}`
      if (!seen.has(key)) {
        seen.add(key)
        const art = o.artwork?.dictionary?.url || o.artwork?.url || ''
        items.push({ title: o.title, artist: o.artistName, durationMs: o.duration, cover: art.replace('{w}', '600').replace('{h}', '600').replace('{f}', 'jpg') })
      }
    }
    Object.values(o).forEach(walk)
  }
  walk(data)
  const title = (/<meta property="og:title" content="([^"]+)"/.exec(html) || [])[1] || 'Lista de Apple Music'
  const cover = (/<meta property="og:image" content="([^"]+)"/.exec(html) || [])[1] || ''
  return { source: 'apple', title: title.replace(/\s+on Apple Music$/i, '').replace(/\s+en Apple Music$/i, ''), cover, items }
}

async function deezer(url) {
  // Enlaces cortos: se sigue la redirección hasta deezer.com/.../playlist/ID
  let target = url
  if (!/\/(playlist|album|track)\/\d+/.test(url.pathname)) target = new URL((await getHtml(url.href)).finalUrl)
  const m = /\/(playlist|album|track)\/(\d+)/.exec(target.pathname)
  if (!m) throw new Error('Enlace de Deezer no reconocido')
  return { source: 'deezer', kind: m[1], id: m[2], title: '', cover: '', items: [] }
}

/**
 * @param {string} link
 * @param {() => string | null} ytdlpBinary
 */
async function importLink(link, ytdlpBinary) {
  const found = detect(link)
  if (!found) throw new Error('Pega un enlace de Spotify, YouTube, YouTube Music, SoundCloud, Apple Music o Deezer')
  if (found.kind === 'spotify') return spotify(found.url)
  if (found.kind === 'apple') return apple(found.url)
  if (found.kind === 'deezer') return deezer(found.url)
  const bin = ytdlpBinary()
  if (!bin) throw new Error('yt-dlp no está disponible')
  return ytdlpList(bin, found.url, found.kind)
}

module.exports = { importLink, detect }
