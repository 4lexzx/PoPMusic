import { Innertube } from 'youtubei.js'

let clientPromise = null

function getClient() {
  if (!clientPromise) {
    clientPromise = Innertube.create({ retrieve_player: true })
  }
  return clientPromise
}

/** Resuelve una URL de audio reproducible a partir de artista y título. */
export async function resolveAudioUrl(artist, title) {
  const yt = await getClient()
  const query = `${artist} ${title} official audio`.trim()
  const results = await yt.search(query, { type: 'video' })
  const video = results?.videos?.[0]
  if (!video?.id) return null

  const info = await yt.getInfo(video.id)
  const sd = info?.streaming_data || info?.streamingData || {}
  const formats =
    sd.adaptive_formats || sd.adaptiveFormats || sd.formats || []
  const audio = formats.find((f) =>
    String(f.mime_type || f.mimeType || f.type || '').startsWith('audio/'),
  )
  if (!audio) return null
  if (audio.url) return audio.url
  if (typeof audio.decipher === 'function') {
    return audio.decipher(yt.session?.player) ?? null
  }
  return null
}
