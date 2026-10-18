import { useColorScheme } from 'react-native'
import {
  findAccent,
  useSettingsStore,
  type ThemeMode,
} from '@/shared/store/settingsStore'

export interface ThemeColors {
  mode: 'dark' | 'light'

  accent: string
  accentSoft: string
  accentInk: string

  bg: string
  bgElev: string
  surface: string
  surface2: string
  surface3: string
  line: string
  lineStrong: string

  ink: string
  muted: string
  faint: string

  primary: string
  primaryInk: string

  danger: string

  /** Superposiciones semánticas (se adaptan al tema). */
  overlay1: string
  overlay2: string
  overlay3: string
  hair: string
  hairStrong: string
}

const DARK: Omit<ThemeColors, 'accent' | 'accentSoft' | 'accentInk' | 'mode'> = {
  bg: '#000000',
  bgElev: '#0a0a0b',
  surface: '#0d0d0f',
  surface2: '#17171a',
  surface3: '#232327',
  line: '#1d1d21',
  lineStrong: '#2a2a30',
  ink: '#ffffff',
  muted: '#b4b4bb',
  faint: '#74747c',
  primary: '#f4f4f5',
  primaryInk: '#000000',
  danger: '#ff4d4d',
  overlay1: 'rgba(255,255,255,0.06)',
  overlay2: 'rgba(255,255,255,0.10)',
  overlay3: 'rgba(255,255,255,0.16)',
  hair: 'rgba(255,255,255,0.08)',
  hairStrong: 'rgba(255,255,255,0.14)',
}

const LIGHT: Omit<ThemeColors, 'accent' | 'accentSoft' | 'accentInk' | 'mode'> = {
  bg: '#f2f2f5',
  bgElev: '#ffffff',
  surface: '#ffffff',
  surface2: '#ffffff',
  surface3: '#e9e9ee',
  line: '#e2e2e8',
  lineStrong: '#cfcfd8',
  ink: '#0b0b0d',
  muted: '#55555d',
  faint: '#8c8c95',
  primary: '#101013',
  primaryInk: '#ffffff',
  danger: '#ff4d4d',
  overlay1: 'rgba(15,15,22,0.05)',
  overlay2: 'rgba(15,15,22,0.08)',
  overlay3: 'rgba(15,15,22,0.13)',
  hair: 'rgba(15,15,22,0.10)',
  hairStrong: 'rgba(15,15,22,0.18)',
}

/** Resuelve el tema efectivo ('dark'/'light') a partir del modo configurado. */
export function resolveThemeMode(mode: ThemeMode, scheme: 'dark' | 'light' | null): 'dark' | 'light' {
  if (mode === 'system') return scheme === 'light' ? 'light' : 'dark'
  return mode
}

/** Construye la paleta completa para un acento y un tema dados. */
export function buildColors(accentId: string, mode: 'dark' | 'light'): ThemeColors {
  const accent = findAccent(accentId)
  const base = mode === 'light' ? LIGHT : DARK
  return {
    mode,
    accent: accent.color,
    accentSoft: accent.soft,
    accentInk: accent.ink,
    ...base,
  }
}

/** Hook con la paleta reactiva al acento y al tema configurados. */
export function useThemeColors(): ThemeColors {
  const scheme = useColorScheme()
  const accentId = useSettingsStore((state) => state.accent)
  const theme = useSettingsStore((state) => state.theme)
  return buildColors(accentId, resolveThemeMode(theme, scheme))
}
