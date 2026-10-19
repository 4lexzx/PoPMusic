import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { resolveAudiusStream } from '@/shared/api/audius'
import { resolveYouTubeVideoId } from '@/shared/api/youtube'
import { useLibraryStore } from '@/shared/store/libraryStore'
import type { RepeatMode, Track } from '@/shared/types/track'
import { audioEngine } from '@/features/player/audioEngine'
import { YT_STATE } from '@/features/player/youtubeController'

interface PlayerState {
  queue: Track[]
  currentIndex: number
  isPlaying: boolean
  isBuffering: boolean
  isLoadingTrack: boolean
  position: number
  duration: number
  volume: number
  muted: boolean
  shuffle: boolean
  repeat: RepeatMode

  playTrack: (track: Track, queue?: Track[]) => Promise<void>
  playQueue: (tracks: Track[], index: number) => Promise<void>
  jumpTo: (index: number) => Promise<void>
  toggle: () => void
  next: (auto?: boolean) => void
  prev: () => void
  seek: (seconds: number) => void
  setVolume: (volume: number) => void
  toggleMute: () => void
  toggleShuffle: () => void
  cycleRepeat: () => void
  enqueue: (track: Track) => void
  playNext: (track: Track) => void
  removeFromQueue: (index: number) => void
  clearQueue: () => void

  applyPlayerState: (state: number) => void
  setPosition: (position: number) => void
  setDuration: (duration: number) => void
}

async function resolveVideoId(track: Track): Promise<string | null> {
  if (track.videoId) return track.videoId
  if (track.source === 'youtube') return track.id
  try {
    return await resolveYouTubeVideoId(track.title, track.artist)
  } catch {
    return null
  }
}

async function loadAndPlay(track: Track): Promise<void> {
  usePlayerStore.setState({
    isLoadingTrack: true,
    position: 0,
    duration: track.duration || 0,
  })

  // 1) Pistas de Audius: audio directo que continúa en segundo plano.
  if (track.source === 'audius' && track.stream) {
    audioEngine.loadUrl(track.stream, true)
    usePlayerStore.setState({ isLoadingTrack: false })
    useLibraryStore.getState().addToHistory(track)
    return
  }

  // 2) Otras pistas: si existe una versión clara en Audius, se usa para el 2º plano.
  try {
    const stream = await resolveAudiusStream(track.title, track.artist)
    if (stream) {
      audioEngine.loadUrl(stream, true)
      usePlayerStore.setState({ isLoadingTrack: false })
      useLibraryStore.getState().addToHistory(track)
      return
    }
  } catch {
    // Sin emparejamiento: se reproduce con YouTube.
  }

  // 3) YouTube IFrame (no continúa en segundo plano en móvil).
  const videoId = await resolveVideoId(track)

  if (!videoId) {
    usePlayerStore.setState({ isLoadingTrack: false, isPlaying: false })
    return
  }

  usePlayerStore.setState((state) => ({
    isLoadingTrack: false,
    queue: state.queue.map((item) =>
      item.id === track.id && item.source === track.source ? { ...item, videoId } : item,
    ),
  }))

  audioEngine.loadVideo(videoId, true)
  useLibraryStore.getState().addToHistory({ ...track, videoId })
}

export const usePlayerStore = create<PlayerState>()(
  persist(
    (set, get) => ({
      queue: [],
      currentIndex: 0,
      isPlaying: false,
      isBuffering: false,
      isLoadingTrack: false,
      position: 0,
      duration: 0,
      volume: 85,
      muted: false,
      shuffle: false,
      repeat: 'off',

      playTrack: async (track, queue) => {
        const list = queue && queue.length > 0 ? queue : [track]
        const found = list.findIndex(
          (item) => item.id === track.id && item.source === track.source,
        )
        const index = found >= 0 ? found : 0
        set({ queue: list, currentIndex: index, isPlaying: true, isLoadingTrack: true })
        await loadAndPlay(list[index])
      },

      playQueue: async (tracks, index) => {
        if (tracks.length === 0) return
        const safeIndex = Math.max(0, Math.min(index, tracks.length - 1))
        set({
          queue: tracks,
          currentIndex: safeIndex,
          isPlaying: true,
          isLoadingTrack: true,
        })
        await loadAndPlay(tracks[safeIndex])
      },

      jumpTo: async (index) => {
        const { queue } = get()
        if (index < 0 || index >= queue.length) return
        set({ currentIndex: index, isPlaying: true, isLoadingTrack: true })
        await loadAndPlay(queue[index])
      },

      toggle: () => {
        const { isPlaying, queue } = get()
        if (queue.length === 0) return
        if (isPlaying) {
          audioEngine.pause()
          set({ isPlaying: false })
        } else {
          audioEngine.play()
          set({ isPlaying: true })
        }
      },

      next: (auto = false) => {
        const { queue, currentIndex, shuffle, repeat } = get()
        if (queue.length === 0) return

        if (auto && repeat === 'one') {
          audioEngine.seek(0)
          audioEngine.play()
          set({ position: 0 })
          return
        }

        let target: number
        if (shuffle && queue.length > 1) {
          do {
            target = Math.floor(Math.random() * queue.length)
          } while (target === currentIndex)
        } else {
          target = currentIndex + 1
          if (target >= queue.length) {
            if (!auto || repeat === 'all') {
              target = 0
            } else {
              set({ isPlaying: false, position: 0 })
              return
            }
          }
        }
        void get().jumpTo(target)
      },

      prev: () => {
        const { queue, currentIndex, position } = get()
        if (queue.length === 0) return
        if (position > 3) {
          audioEngine.seek(0)
          set({ position: 0 })
          return
        }
        const target = currentIndex - 1 < 0 ? queue.length - 1 : currentIndex - 1
        void get().jumpTo(target)
      },

      seek: (seconds) => {
        audioEngine.seek(seconds)
        set({ position: seconds })
      },

      setVolume: (volume) => {
        const clamped = Math.max(0, Math.min(100, Math.round(volume)))
        audioEngine.setVolume(clamped)
        if (clamped > 0) audioEngine.unmute()
        set({ volume: clamped, muted: clamped === 0 })
      },

      toggleMute: () => {
        const { muted, volume } = get()
        if (muted) {
          audioEngine.setVolume(volume)
          audioEngine.unmute()
          set({ muted: false })
        } else {
          audioEngine.mute()
          set({ muted: true })
        }
      },

      toggleShuffle: () => set((state) => ({ shuffle: !state.shuffle })),

      cycleRepeat: () =>
        set((state) => {
          const order: RepeatMode[] = ['off', 'all', 'one']
          const nextMode = order[(order.indexOf(state.repeat) + 1) % order.length]
          return { repeat: nextMode }
        }),

      enqueue: (track) => set((state) => ({ queue: [...state.queue, track] })),

      playNext: (track) => {
        const { queue, currentIndex } = get()
        const copy = [...queue]
        copy.splice(currentIndex + 1, 0, track)
        set({ queue: copy })
      },

      removeFromQueue: (index) =>
        set((state) => {
          if (index < 0 || index >= state.queue.length) return state
          const queue = state.queue.filter((_, itemIndex) => itemIndex !== index)
          let currentIndex = state.currentIndex
          if (index < currentIndex) currentIndex -= 1
          if (currentIndex >= queue.length) currentIndex = Math.max(0, queue.length - 1)
          return { queue, currentIndex }
        }),

      clearQueue: () =>
        set({ queue: [], currentIndex: 0, isPlaying: false, position: 0, duration: 0 }),

      applyPlayerState: (playerState) => {
        if (playerState === YT_STATE.PLAYING) {
          set({ isPlaying: true, isBuffering: false })
        } else if (playerState === YT_STATE.PAUSED) {
          set({ isPlaying: false, isBuffering: false })
        } else if (playerState === YT_STATE.BUFFERING) {
          set({ isBuffering: true })
        } else if (playerState === YT_STATE.ENDED) {
          get().next(true)
        } else if (playerState === YT_STATE.CUED) {
          set({ isBuffering: false })
        }
      },

      setPosition: (position) => set({ position }),

      setDuration: (duration) => set({ duration }),
    }),
    {
      name: 'liner-player-v1',
      version: 1,
      partialize: (state) => ({
        queue: state.queue,
        currentIndex: state.currentIndex,
        volume: state.volume,
        muted: state.muted,
        shuffle: state.shuffle,
        repeat: state.repeat,
      }),
    },
  ),
)

/** Pista que se está reproduciendo (o va a reproducirse). */
export function selectCurrentTrack(state: PlayerState): Track | null {
  return state.queue[state.currentIndex] ?? null
}
