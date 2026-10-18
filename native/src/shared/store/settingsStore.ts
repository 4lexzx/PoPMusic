import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { zustandStorage } from '@/shared/store/storage'

export type ThemeMode = 'dark' | 'light' | 'system'

export interface AccentOption {
  id: string
  label: string
  color: string
  soft: string
  /** Color del texto/icono cuando va encima del acento. */
  ink: string
}

export const ACCENTS: AccentOption[] = [
  { id: 'graphite', label: 'Grafito', color: '#d7d7de', soft: '#ececf2', ink: '#0a0a0d' },
  { id: 'red', label: 'Rojo', color: '#ff4d4d', soft: '#ff8080', ink: '#1a0505' },
  { id: 'violet', label: 'Violeta', color: '#8b7cff', soft: '#b3a8ff', ink: '#0d0820' },
  { id: 'blue', label: 'Azul', color: '#4d8cff', soft: '#8fb4ff', ink: '#04101f' },
  { id: 'cyan', label: 'Cian', color: '#22c9d6', soft: '#7ee6ee', ink: '#04201f' },
  { id: 'green', label: 'Verde', color: '#3ddc84', soft: '#8ff0bb', ink: '#04220f' },
  { id: 'amber', label: 'Ámbar', color: '#ffb020', soft: '#ffd27a', ink: '#1f1402' },
  { id: 'pink', label: 'Rosa', color: '#ff5c9d', soft: '#ff9cc4', ink: '#20040f' },
]

export interface WallpaperPreset {
  id: string
  label: string
  /** Colores del degradado (de arriba-izquierda a abajo-derecha). */
  colors: string[]
  /** Posiciones de cada color (0..1). Si se omite, se reparten a partes iguales. */
  locations?: number[]
}

/** Fondos predefinidos (se aplican como capa tras la interfaz). */
export const WALLPAPERS: WallpaperPreset[] = [
  { id: 'aurora', label: 'Aurora', colors: ['#3a1156', '#0b4f6c', '#0a0a12', '#05050a'] },
  { id: 'ember', label: 'Brasa', colors: ['#5a1414', '#7a3b06', '#120a0a', '#060404'] },
  { id: 'ocean', label: 'Océano', colors: ['#0b3a5b', '#0d5c7a', '#060c12', '#03060a'] },
  { id: 'forest', label: 'Bosque', colors: ['#0f3d2b', '#1c5a2a', '#060f0b', '#030705'] },
  { id: 'midnight', label: 'Medianoche', colors: ['#1b2340', '#080a12', '#030409'] },
]

interface SettingsState {
  accent: string
  theme: ThemeMode
  dynamicBackground: boolean

  /* Efecto cristal */
  glassOpacity: number
  glassBlur: number
  glassDim: number

  /* Fondo de pantalla */
  wallpaper: string | null
  wallpaperImage: string | null
  wallpaperOpacity: number
  wallpaperBlur: number
  wallpaperDim: number

  /* Reproducción */
  autoplayNext: boolean
  restoreLastTrack: boolean
  crossfade: boolean

  /* Mi algoritmo (personalización de recomendaciones) */
  genres: string[]
  artists: string[]
  discovery: number

  setAccent: (id: string) => void
  setTheme: (theme: ThemeMode) => void
  setDynamicBackground: (value: boolean) => void
  setGlass: (
    partial: Partial<Pick<SettingsState, 'glassOpacity' | 'glassBlur' | 'glassDim'>>,
  ) => void
  setWallpaper: (id: string | null) => void
  setWallpaperImage: (uri: string | null) => void
  setWallpaperTuning: (
    partial: Partial<Pick<SettingsState, 'wallpaperOpacity' | 'wallpaperBlur' | 'wallpaperDim'>>,
  ) => void
  setPlayback: (
    partial: Partial<Pick<SettingsState, 'autoplayNext' | 'restoreLastTrack' | 'crossfade'>>,
  ) => void
  resetAppearance: () => void

  toggleGenre: (genre: string) => void
  addArtist: (name: string) => void
  removeArtist: (name: string) => void
  setDiscovery: (value: number) => void
  resetAlgorithm: () => void
}

/** Géneros disponibles para «Mi algoritmo». */
export const GENRES = [
  'Pop',
  'Rock',
  'Reggaetón',
  'Latino',
  'Electrónica',
  'Hip-Hop',
  'R&B',
  'Indie',
  'Jazz',
  'Clásica',
  'Metal',
  'Country',
  'Trap',
  'Salsa',
  'Bachata',
  'K-Pop',
]

const APPEARANCE_DEFAULTS = {
  glassOpacity: 0.62,
  glassBlur: 24,
  glassDim: 0,
  wallpaper: null,
  wallpaperImage: null,
  wallpaperOpacity: 1,
  wallpaperBlur: 0,
  wallpaperDim: 0.35,
} as const

const ALGORITHM_DEFAULTS = {
  genres: [] as string[],
  artists: [] as string[],
  discovery: 0.5,
} as const

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      accent: 'graphite',
      theme: 'dark',
      dynamicBackground: true,
      glassOpacity: APPEARANCE_DEFAULTS.glassOpacity,
      glassBlur: APPEARANCE_DEFAULTS.glassBlur,
      glassDim: APPEARANCE_DEFAULTS.glassDim,
      wallpaper: APPEARANCE_DEFAULTS.wallpaper,
      wallpaperImage: APPEARANCE_DEFAULTS.wallpaperImage,
      wallpaperOpacity: APPEARANCE_DEFAULTS.wallpaperOpacity,
      wallpaperBlur: APPEARANCE_DEFAULTS.wallpaperBlur,
      wallpaperDim: APPEARANCE_DEFAULTS.wallpaperDim,
      autoplayNext: true,
      restoreLastTrack: true,
      crossfade: false,

      genres: ALGORITHM_DEFAULTS.genres,
      artists: ALGORITHM_DEFAULTS.artists,
      discovery: ALGORITHM_DEFAULTS.discovery,

      setAccent: (accent) => set({ accent }),
      setTheme: (theme) => set({ theme }),
      setDynamicBackground: (dynamicBackground) => set({ dynamicBackground }),
      setGlass: (partial) => set(partial),
      setWallpaper: (wallpaper) => set({ wallpaper }),
      setWallpaperImage: (wallpaperImage) => set({ wallpaperImage }),
      setWallpaperTuning: (partial) => set(partial),
      setPlayback: (partial) => set(partial),
      resetAppearance: () => set({ ...APPEARANCE_DEFAULTS }),

      toggleGenre: (genre) =>
        set((state) => ({
          genres: state.genres.includes(genre)
            ? state.genres.filter((item) => item !== genre)
            : [...state.genres, genre],
        })),
      addArtist: (name) =>
        set((state) => {
          const clean = name.trim()
          if (!clean || state.artists.some((item) => item.toLowerCase() === clean.toLowerCase())) {
            return state
          }
          return { artists: [...state.artists, clean] }
        }),
      removeArtist: (name) =>
        set((state) => ({ artists: state.artists.filter((item) => item !== name) })),
      setDiscovery: (discovery) => set({ discovery }),
      resetAlgorithm: () => set({ ...ALGORITHM_DEFAULTS }),
    }),
    {
      name: 'liner-settings-v1',
      storage: zustandStorage,
      version: 3,
      migrate: (persisted, version) => {
        const state = (persisted ?? {}) as Partial<SettingsState>
        if (version < 3) return { ...state, accent: 'graphite' }
        return state
      },
    },
  ),
)

/** Devuelve el preset de fondo por id. */
export function findWallpaper(id: string | null): WallpaperPreset | null {
  if (!id) return null
  return WALLPAPERS.find((item) => item.id === id) ?? null
}

/** Devuelve la opción de acento activa. */
export function findAccent(id: string): AccentOption {
  return ACCENTS.find((item) => item.id === id) ?? ACCENTS[0]
}
