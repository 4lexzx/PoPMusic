const { YtDlp } = require('ytdlp-nodejs')

let ytdlp = null
function getYtdlp() {
  if (!ytdlp) ytdlp = new YtDlp()
  return ytdlp
}

/** Devuelve la mejor URL de audio HTTPS para una canción. */
async function resolveAudio(artist, title) {
  const y = getYtdlp()
  const query = `ytsearch1:${artist} ${title} official audio`.trim()
  const search = await y.getInfoAsync(query)
  const entry = Array.isArray(search.entries) ? search.entries[0] : search
  if (!entry) return null

  // La entrada de búsqueda solo trae metadatos: hace falta pedir el vídeo.
  const info = entry.formats ? entry : await y.getInfoAsync(entry.url)
  const formats = (info.formats || []).filter(
    (f) =>
      f.url &&
      f.protocol === 'https' &&
      String(f.vcodec || '') === 'none' &&
      String(f.acodec || '') !== 'none',
  )
  if (!formats.length) return null

  formats.sort((a, b) => (b.tbr || 0) - (a.tbr || 0))
  return formats[0].url
}

module.exports = { resolveAudio }
