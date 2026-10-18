import type { Playlist, Track } from '@/shared/types/track'

/**
 * Catálogo de muestra para la pantalla de bienvenida (cuando aún no hay nada
 * guardado). Las pistas no traen audio: al reproducir se resuelve su canción
 * completa en YouTube.
 */
export const demoTracks: Track[] = [
  {
    id: 'dQw4w9WgXcQ',
    videoId: 'dQw4w9WgXcQ',
    title: 'Never Gonna Give You Up',
    artist: 'Rick Astley',
    thumbnail: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
    duration: 213,
    source: 'youtube',
  },
  {
    id: 'djV11Xbc914',
    videoId: 'djV11Xbc914',
    title: 'Take On Me',
    artist: 'a-ha',
    thumbnail: 'https://i.ytimg.com/vi/djV11Xbc914/hqdefault.jpg',
    duration: 225,
    source: 'youtube',
  },
  {
    id: 'fJ9rUzIMcZQ',
    videoId: 'fJ9rUzIMcZQ',
    title: 'Bohemian Rhapsody',
    artist: 'Queen',
    thumbnail: 'https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg',
    duration: 355,
    source: 'youtube',
  },
  {
    id: '4NRXx6U8ABQ',
    videoId: '4NRXx6U8ABQ',
    title: 'Blinding Lights',
    artist: 'The Weeknd',
    thumbnail: 'https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg',
    duration: 262,
    source: 'youtube',
  },
  {
    id: 'TUVcZfQe-Kw',
    videoId: 'TUVcZfQe-Kw',
    title: 'Levitating',
    artist: 'Dua Lipa',
    thumbnail: 'https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg',
    duration: 203,
    source: 'youtube',
  },
  {
    id: 'JGwWNGJdvx8',
    videoId: 'JGwWNGJdvx8',
    title: 'Shape of You',
    artist: 'Ed Sheeran',
    thumbnail: 'https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg',
    duration: 264,
    source: 'youtube',
  },
  {
    id: 'FTQbiNvZqaY',
    videoId: 'FTQbiNvZqaY',
    title: 'Africa',
    artist: 'Toto',
    thumbnail: 'https://i.ytimg.com/vi/FTQbiNvZqaY/hqdefault.jpg',
    duration: 275,
    source: 'youtube',
  },
  {
    id: 'yKNxeF4KMsY',
    videoId: 'yKNxeF4KMsY',
    title: 'Yellow',
    artist: 'Coldplay',
    thumbnail: 'https://i.ytimg.com/vi/yKNxeF4KMsY/hqdefault.jpg',
    duration: 268,
    source: 'youtube',
  },
]

export const demoPlaylist: Playlist = {
  id: 'demo-clasicos',
  name: 'Clásicos para empezar',
  description: 'Selección de muestra para empezar a escuchar al instante.',
  thumbnail: demoTracks[0].thumbnail,
  source: 'local',
  tracks: demoTracks,
  importedAt: 0,
}

/** Filtra los temas demo por una consulta de búsqueda local. */
export function searchDemo(query: string): Track[] {
  const normalized = query.trim().toLowerCase()
  if (!normalized) return demoTracks
  return demoTracks.filter(
    (track) =>
      track.title.toLowerCase().includes(normalized) ||
      track.artist.toLowerCase().includes(normalized),
  )
}
