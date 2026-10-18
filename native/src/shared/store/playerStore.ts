import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { audioService, YT_STATE } from '@/features/player/audioService'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { zustandStorage } from '@/shared/store/storage'
import type { RepeatMode, Track } from '@/shared/types/track'

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

async function loadAndPlay(track: Track): Promise<void> {
  usePlayerStore.setState({
    isLoadingTrack: true,
    position: 0,
    duration: track.duration || 0,
  })

  const resolved = await audioService.load(track, true)

  usePlayerStore.setState((state) => ({
    isLoadingTrack: false,
    isPlaying: Boolean(resolved),
    queue: state.queue.map((item) =>
      item.id === track.id && item.source === track.source
        ? {
            ...item,
            streamUrl: resolved?.url ?? item.streamUrl,
            preview: resolved?.preview ?? item.preview,
            duration: resolved?.duration || item.duration,
          }
        : item,
    ),
  }))

  if (resolved) useLibraryStore.getState().addToHistory(track)
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
        const found = list.findIndex((item) => item.id === track.id && item.source === track.source)
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
          audioService.pause()
          set({ isPlaying: false })
        } else {
          audioService.play()
          set({ isPlaying: true })
        }
      },

      next: (auto = false) => {
        const { queue, currentIndex, shuffle, repeat } = get()
        if (queue.length === 0) return

        if (auto && repeat === 'one') {
          audioService.seek(0)
          audioService.play()
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
          audioService.seek(0)
          set({ position: 0 })
          return
        }
        const target = currentIndex - 1 < 0 ? queue.length - 1 : currentIndex - 1
        void get().jumpTo(target)
      },

      seek: (seconds) => {
        audioService.seek(seconds)
        set({ position: seconds })
      },

      setVolume: (volume) => {
        const clamped = Math.max(0, Math.min(100, Math.round(volume)))
        audioService.setVolume(clamped)
        if (clamped > 0) audioService.unmute()
        set({ volume: clamped, muted: clamped === 0 })
      },

      toggleMute: () => {
        const { muted, volume } = get()
        if (muted) {
          audioService.setVolume(volume)
          audioService.unmute()
          set({ muted: false })
        } else {
          audioService.mute()
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
      storage: zustandStorage,
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
