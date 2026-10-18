import { create } from 'zustand'
import { persist } from 'zustand/middleware'

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
  css: string
}

/** Fondos predefinidos (se aplican como capa fija tras la interfaz). */
export const WALLPAPERS: WallpaperPreset[] = [
  {
    id: 'aurora',
    label: 'Aurora',
    css: 'radial-gradient(120% 90% at 15% 10%, #3a1156 0%, transparent 55%), radial-gradient(120% 100% at 90% 20%, #0b4f6c 0%, transparent 55%), linear-gradient(160deg, #0a0a12, #05050a)',
  },
  {
    id: 'ember',
    label: 'Brasa',
    css: 'radial-gradient(110% 90% at 20% 15%, #5a1414 0%, transparent 55%), radial-gradient(120% 100% at 85% 30%, #7a3b06 0%, transparent 50%), linear-gradient(160deg, #120a0a, #060404)',
  },
  {
    id: 'ocean',
    label: 'Océano',
    css: 'radial-gradient(120% 90% at 20% 10%, #0b3a5b 0%, transparent 55%), radial-gradient(120% 100% at 90% 25%, #0d5c7a 0%, transparent 50%), linear-gradient(160deg, #060c12, #03060a)',
  },
  {
    id: 'forest',
    label: 'Bosque',
    css: 'radial-gradient(120% 90% at 15% 10%, #0f3d2b 0%, transparent 55%), radial-gradient(120% 100% at 90% 25%, #1c5a2a 0%, transparent 50%), linear-gradient(160deg, #060f0b, #030705)',
  },
  {
    id: 'midnight',
    label: 'Medianoche',
    css: 'radial-gradient(120% 90% at 50% -10%, #1b2340 0%, transparent 60%), linear-gradient(180deg, #080a12, #030409)',
  },
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
  setGlass: (partial: Partial<Pick<SettingsState, 'glassOpacity' | 'glassBlur' | 'glassDim'>>) => void
  setWallpaper: (css: string | null) => void
  setWallpaperImage: (dataUrl: string | null) => void
  setWallpaperTuning: (
    partial: Partial<Pick<SettingsState, 'wallpaperOpacity' | 'wallpaperBlur' | 'wallpaperDim'>>,
  ) => void
  setPlayback: (partial: Partial<Pick<SettingsState, 'autoplayNext' | 'restoreLastTrack' | 'crossfade'>>) => void
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
  glassOpacity: 0.82,
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
      version: 4,
      migrate: (persisted, version) => {
        const state = (persisted ?? {}) as Partial<SettingsState>
        const next = { ...state }
        if (version < 3) next.accent = 'graphite'
        if (version < 4) next.glassOpacity = APPEARANCE_DEFAULTS.glassOpacity
        return next
      },
    },
  ),
)

const DARK = {
  surface: '13,13,15',
  bg: '0,0,0',
  elev: '10,10,11',
}

const LIGHT = {
  surface: '255,255,255',
  bg: '242,242,245',
  elev: '255,255,255',
}

function resolveTheme(mode: ThemeMode): 'dark' | 'light' {
  if (mode === 'system') {
    const prefersLight =
      typeof window !== 'undefined' &&
      window.matchMedia?.('(prefers-color-scheme: light)').matches
    return prefersLight ? 'light' : 'dark'
  }
  return mode
}

/** Aplica acento, tema, cristal y fondo de pantalla como variables CSS globales. */
export function applyAppearance(): void {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  const state = useSettingsStore.getState()

  const accent = ACCENTS.find((item) => item.id === state.accent) ?? ACCENTS[0]
  root.style.setProperty('--color-accent', accent.color)
  root.style.setProperty('--color-accent-soft', accent.soft)
  root.style.setProperty('--color-accent-ink', accent.ink)

  const palette = resolveTheme(state.theme) === 'light' ? LIGHT : DARK
  root.dataset.theme = resolveTheme(state.theme)
  root.style.setProperty('--surface-rgb', palette.surface)
  root.style.setProperty('--bg-rgb', palette.bg)
  root.style.setProperty('--elev-rgb', palette.elev)

  const opacity = Math.min(1, Math.max(0.2, state.glassOpacity))
  const dim = Math.min(0.6, Math.max(0, state.glassDim))
  root.style.setProperty('--glass-alpha', opacity.toFixed(3))
  root.style.setProperty('--glass-blur', `${state.glassBlur}px`)
  root.style.setProperty('--glass-dim', dim.toFixed(3))

  root.style.setProperty('--wallpaper-blur', `${state.wallpaperBlur}px`)
  root.style.setProperty('--wallpaper-opacity', state.wallpaperOpacity.toFixed(3))
  root.style.setProperty('--wallpaper-dim', state.wallpaperDim.toFixed(3))
}

/** Suscribe el store a los cambios de apariencia y los aplica al DOM. */
export function watchAppearance(): () => void {
  applyAppearance()
  const media =
    typeof window !== 'undefined' ? window.matchMedia?.('(prefers-color-scheme: light)') : null
  const onScheme = () => applyAppearance()
  media?.addEventListener?.('change', onScheme)
  const unsubscribe = useSettingsStore.subscribe(applyAppearance)
  return () => {
    media?.removeEventListener?.('change', onScheme)
    unsubscribe()
  }
}
