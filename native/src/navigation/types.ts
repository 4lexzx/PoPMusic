import type { NavigatorScreenParams } from '@react-navigation/native'

export type RootTabParamList = {
  Home: undefined
  Search: { q?: string } | undefined
  Recognize: undefined
  Library: undefined
}

export type RootStackParamList = {
  Tabs: NavigatorScreenParams<RootTabParamList> | undefined
  NowPlaying: undefined
  Playlist: { id: string }
  Settings: undefined
}

export const TAB_LABELS: Record<keyof RootTabParamList, string> = {
  Home: 'Inicio',
  Search: 'Buscar',
  Recognize: 'Reconocer',
  Library: 'Biblioteca',
}

export const SOURCE_LABELS: Record<string, string> = {
  spotify: 'Spotify',
  youtube: 'YouTube',
  local: 'Local',
}
