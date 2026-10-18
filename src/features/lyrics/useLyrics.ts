import { useEffect, useState } from 'react'
import { fetchLyrics } from '@/shared/api/lrclib'
import type { Lyrics } from '@/shared/types/lyrics'
import type { Track } from '@/shared/types/track'

export type LyricsStatus = 'idle' | 'loading' | 'ready' | 'empty' | 'error'

/** Carga letras desde LRCLIB cuando cambia la pista actual. */
export function useLyrics(track: Track | null): { lyrics: Lyrics | null; status: LyricsStatus } {
  const [lyrics, setLyrics] = useState<Lyrics | null>(null)
  const [status, setStatus] = useState<LyricsStatus>('idle')

  const trackId = track?.id ?? ''
  const trackSource = track?.source ?? ''
  const title = track?.title ?? ''
  const artist = track?.artist ?? ''
  const duration = track?.duration ?? 0

  useEffect(() => {
    if (!title) {
      setLyrics(null)
      setStatus('idle')
      return
    }

    let cancelled = false
    setStatus('loading')
    setLyrics(null)

    fetchLyrics({ title, artist, duration })
      .then((result) => {
        if (cancelled) return
        if (result) {
          setLyrics(result)
          setStatus('ready')
        } else {
          setStatus('empty')
        }
      })
      .catch(() => {
        if (!cancelled) setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [trackId, trackSource, title, artist, duration])

  return { lyrics, status }
}
