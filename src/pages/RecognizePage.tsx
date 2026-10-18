import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Artwork } from '@/components/Artwork'
import { HeartFilledIcon, HeartIcon, MicIcon, PlayIcon, SearchIcon } from '@/components/Icons'
import { Button, IconButton, Spinner } from '@/components/ui'
import { recognizeAudio, type RecognizedSong } from '@/shared/api/recognize'
import { searchTracks } from '@/shared/api/youtube'
import { HAS_RECOGNITION } from '@/shared/config'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useUiStore } from '@/shared/store/uiStore'
import type { Track } from '@/shared/types/track'

type Phase = 'idle' | 'listening' | 'analyzing' | 'found' | 'notfound' | 'error'

const SAMPLE_MS = 7000

/** Elige el mejor mimeType soportado por el navegador. */
function pickMimeType(): string | undefined {
  const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/mp4']
  if (typeof MediaRecorder === 'undefined') return undefined
  return candidates.find((type) => MediaRecorder.isTypeSupported(type))
}

export function RecognizePage() {
  const navigate = useNavigate()
  const [phase, setPhase] = useState<Phase>('idle')
  const [song, setSong] = useState<RecognizedSong | null>(null)
  const [elapsed, setElapsed] = useState(0)

  const notify = useUiStore((state) => state.notify)
  const playTrack = usePlayerStore((state) => state.playTrack)
  const favorites = useLibraryStore((state) => state.favorites)
  const toggleFavorite = useLibraryStore((state) => state.toggleFavorite)

  const recorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])
  const streamRef = useRef<MediaStream | null>(null)
  const timerRef = useRef<number | null>(null)

  const stopStream = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    if (timerRef.current) {
      window.clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  useEffect(() => () => stopStream(), [stopStream])

  const analyze = useCallback(
    async (blob: Blob) => {
      setPhase('analyzing')
      try {
        const result = await recognizeAudio(blob)
        if (result && result.title) {
          setSong(result)
          setPhase('found')
        } else {
          setPhase('notfound')
        }
      } catch {
        setPhase('error')
      }
    },
    [],
  )

  const startListening = useCallback(async () => {
    if (!HAS_RECOGNITION) {
      notify('Falta configurar VITE_AUDD_API_KEY para reconocer música.', 'error')
      return
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      notify('Tu navegador no permite acceder al micrófono.', 'error')
      return
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream
      const mimeType = pickMimeType()
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined)
      chunksRef.current = []

      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) chunksRef.current.push(event.data)
      }
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: mimeType ?? 'audio/webm' })
        stopStream()
        void analyze(blob)
      }

      recorder.start()
      recorderRef.current = recorder
      setSong(null)
      setElapsed(0)
      setPhase('listening')

      const started = Date.now()
      timerRef.current = window.setInterval(() => {
        setElapsed(Date.now() - started)
      }, 100)

      window.setTimeout(() => {
        if (recorder.state !== 'inactive') recorder.stop()
      }, SAMPLE_MS)
    } catch {
      notify('No se pudo acceder al micrófono.', 'error')
      setPhase('idle')
    }
  }, [analyze, notify, stopStream])

  const stopEarly = useCallback(() => {
    const recorder = recorderRef.current
    if (recorder && recorder.state !== 'inactive') recorder.stop()
  }, [])

  const playRecognized = useCallback(async () => {
    if (!song) return
    setPhase('analyzing')
    try {
      const results = await searchTracks(`${song.title} ${song.artist}`, 10)
      if (results.length === 0) {
        notify('No se encontró en YouTube.', 'error')
        setPhase('found')
        return
      }
      await playTrack(results[0], results)
    } catch {
      notify('No se pudo reproducir la canción.', 'error')
      setPhase('found')
    }
  }, [song, searchTracks, playTrack, notify])

  const favoriteRecognized = useCallback(async () => {
    if (!song) return
    const results = await searchTracks(`${song.title} ${song.artist}`, 5)
    const track: Track =
      results[0] ??
      ({
        id: `recognized-${song.title}-${song.artist}`,
        title: song.title,
        artist: song.artist,
        thumbnail: song.cover ?? '',
        duration: 0,
        source: 'youtube',
      } as Track)
    toggleFavorite(track)
  }, [song, toggleFavorite])

  const progress = Math.min(1, elapsed / SAMPLE_MS)
  const isFavorite = song
    ? favorites.some(
        (item) =>
          item.title.toLowerCase() === song.title.toLowerCase() &&
          item.artist.toLowerCase() === song.artist.toLowerCase(),
      )
    : false

  return (
    <div className="flex min-h-[calc(100dvh-8rem)] flex-col px-6 pb-6 pt-6">
      <header className="text-center">
        <h1 className="text-[1.6rem] font-semibold tracking-tight">Reconocer música</h1>
        <p className="mt-1 text-sm text-faint">
          Acerca el micrófono a la música y déjame escuchar unos segundos.
        </p>
      </header>

      <div className="relative flex flex-1 flex-col items-center justify-center">
        <div className="relative grid place-items-center">
          {phase === 'listening' ? (
            <>
              <span className="pulse-ring" />
              <span className="pulse-ring pulse-ring--slow" />
            </>
          ) : null}

          <button
            type="button"
            onClick={phase === 'listening' ? stopEarly : startListening}
            disabled={phase === 'analyzing'}
            aria-label={phase === 'listening' ? 'Detener' : 'Escuchar'}
            className={`relative grid h-32 w-32 place-items-center rounded-full transition-transform active:scale-95 disabled:opacity-60 ${
              phase === 'listening'
                ? 'bg-accent text-[color:var(--color-accent-ink)]'
                : 'glass-panel text-ink'
            }`}
          >
            {phase === 'analyzing' ? (
              <Spinner size={40} />
            ) : (
              <MicIcon size={46} />
            )}
          </button>

          {phase === 'listening' ? (
            <svg className="absolute h-40 w-40 -rotate-90" viewBox="0 0 100 100" aria-hidden="true">
              <circle cx="50" cy="50" r="48" fill="none" stroke="var(--overlay-3)" strokeWidth="2" />
              <circle
                cx="50"
                cy="50"
                r="48"
                fill="none"
                stroke="var(--color-accent)"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeDasharray={2 * Math.PI * 48}
                strokeDashoffset={2 * Math.PI * 48 * (1 - progress)}
                style={{ transition: 'stroke-dashoffset 0.1s linear' }}
              />
            </svg>
          ) : null}
        </div>

        <p className="mt-8 h-6 text-sm font-medium text-muted">
          {phase === 'idle' && 'Toca para escuchar'}
          {phase === 'listening' && 'Escuchando…'}
          {phase === 'analyzing' && 'Analizando…'}
          {phase === 'notfound' && 'No reconocí la canción. Inténtalo de nuevo.'}
          {phase === 'error' && 'Hubo un problema al analizar el audio.'}
        </p>
      </div>

      {phase === 'found' && song ? (
        <div className="animate-pop glass-panel mt-2 flex items-center gap-3.5 rounded-2xl p-3.5">
          <Artwork src={song.cover} className="h-16 w-16 shrink-0" rounded="rounded-xl" iconSize={26} />
          <div className="min-w-0 flex-1">
            <p className="truncate font-semibold">{song.title}</p>
            <p className="truncate text-sm text-faint">
              {song.artist}
              {song.album ? ` · ${song.album}` : ''}
            </p>
            <div className="mt-2.5 flex items-center gap-2">
              <Button size="sm" icon={<PlayIcon size={16} />} onClick={() => void playRecognized()}>
                Reproducir
              </Button>
              <IconButton
                label={isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}
                size="sm"
                active={isFavorite}
                onClick={() => void favoriteRecognized()}
              >
                {isFavorite ? <HeartFilledIcon size={18} /> : <HeartIcon size={18} />}
              </IconButton>
              <IconButton
                label="Buscar en YouTube"
                size="sm"
                onClick={() =>
                  navigate(`/search?q=${encodeURIComponent(`${song.title} ${song.artist}`)}`)
                }
              >
                <SearchIcon size={18} />
              </IconButton>
            </div>
          </div>
        </div>
      ) : null}

      {!HAS_RECOGNITION ? (
        <div className="mt-3 rounded-2xl border border-accent/25 bg-accent/8 p-3.5 text-xs leading-relaxed text-muted">
          <span className="font-semibold text-ink">Falta la clave de reconocimiento.</span> Crea una
          cuenta gratuita en AudD y añade <code className="text-accent-soft">VITE_AUDD_API_KEY</code>{' '}
          en tu archivo <code>.env</code> para activar esta función.
        </div>
      ) : null}
    </div>
  )
}
