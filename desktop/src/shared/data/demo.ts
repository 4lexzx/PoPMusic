import type { Playlist, Track } from '@/shared/types/track'

/**
 * Datos de muestra usados en modo demo (sin VITE_YT_API_KEY).
 * Los videoId corresponden a vídeos públicos de YouTube: el reproductor
 * IFrame funciona sin necesidad de la Data API.
 */
export const demoTracks: Track[] = [
  {
    id: 'dQw4w9WgXcQ',
    videoId: 'dQw4w9WgXcQ',
    title: 'Never Gonna Give You Up',
    artist: 'Rick Astley',
    thumbnail: 'https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/ce/6d/5b/ce6d5b48-8c36-b990-3b9c-81862fadb459/0859381157694.jpg/600x600bb.jpg',
    duration: 213,
    source: 'youtube',
  },
  {
    id: 'djV11Xbc914',
    videoId: 'djV11Xbc914',
    title: 'Take On Me',
    artist: 'a-ha',
    thumbnail: 'https://is1-ssl.mzstatic.com/image/thumb/Music/c6/e1/c8/mzi.ixgzfcmc.jpg/600x600bb.jpg',
    duration: 225,
    source: 'youtube',
  },
  {
    id: 'fJ9rUzIMcZQ',
    videoId: 'fJ9rUzIMcZQ',
    title: 'Bohemian Rhapsody',
    artist: 'Queen',
    thumbnail: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/8b/0a/ea/8b0aea60-6f4a-195b-5958-cdf459c2333b/602527644271.jpg/600x600bb.jpg',
    duration: 355,
    source: 'youtube',
  },
  {
    id: '4NRXx6U8ABQ',
    videoId: '4NRXx6U8ABQ',
    title: 'Blinding Lights',
    artist: 'The Weeknd',
    thumbnail: 'https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/2b/b9/fe/2bb9fef5-d7f3-8345-25a9-db0e79fde4e4/20UMGIM11048.rgb.jpg/600x600bb.jpg',
    duration: 262,
    source: 'youtube',
  },
  {
    id: 'TUVcZfQe-Kw',
    videoId: 'TUVcZfQe-Kw',
    title: 'Levitating',
    artist: 'Dua Lipa',
    thumbnail: 'https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/6c/11/d6/6c11d681-aa3a-d59e-4c2e-f77e181026ab/190295092665.jpg/600x600bb.jpg',
    duration: 203,
    source: 'youtube',
  },
  {
    id: 'JGwWNGJdvx8',
    videoId: 'JGwWNGJdvx8',
    title: 'Shape of You',
    artist: 'Ed Sheeran',
    thumbnail: 'https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/15/e6/e8/15e6e8a4-4190-6a8b-86c3-ab4a51b88288/190295851286.jpg/600x600bb.jpg',
    duration: 264,
    source: 'youtube',
  },
  {
    id: 'FTQbiNvZqaY',
    videoId: 'FTQbiNvZqaY',
    title: 'Africa',
    artist: 'Toto',
    thumbnail: 'https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/69/ce/d2/69ced240-07a7-2a04-bbab-2afbacf30809/074643772822.jpg/600x600bb.jpg',
    duration: 275,
    source: 'youtube',
  },
  {
    id: 'yKNxeF4KMsY',
    videoId: 'yKNxeF4KMsY',
    title: 'Yellow',
    artist: 'Coldplay',
    thumbnail: 'https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/f5/93/8c/f5938c49-964c-31d1-4b33-78b634f71fb7/190295978075.jpg/600x600bb.jpg',
    duration: 268,
    source: 'youtube',
  },
]

export const demoPlaylist: Playlist = {
  id: 'demo-clasicos',
  name: 'Clásicos para empezar',
  description: 'Selección de muestra para probar la app sin configurar API keys.',
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
