import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const MAX_SEARCHES = 12

export type MessageTone = 'info' | 'error'

interface UiState {
  searchHistory: string[]
  addSearch: (query: string) => void
  clearSearchHistory: () => void

  queueOpen: boolean
  setQueueOpen: (open: boolean) => void

  message: { id: number; text: string; tone: MessageTone } | null
  notify: (text: string, tone?: MessageTone) => void
  dismissMessage: () => void
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      searchHistory: [],
      addSearch: (query) =>
        set((state) => {
          const trimmed = query.trim()
          if (!trimmed) return state
          return {
            searchHistory: [
              trimmed,
              ...state.searchHistory.filter((item) => item.toLowerCase() !== trimmed.toLowerCase()),
            ].slice(0, MAX_SEARCHES),
          }
        }),
      clearSearchHistory: () => set({ searchHistory: [] }),

      queueOpen: false,
      setQueueOpen: (queueOpen) => set({ queueOpen }),

      message: null,
      notify: (text, tone = 'info') => set({ message: { id: Date.now(), text, tone } }),
      dismissMessage: () => set({ message: null }),
    }),
    {
      name: 'liner-ui-v1',
      version: 1,
      partialize: (state) => ({ searchHistory: state.searchHistory }),
    },
  ),
)
