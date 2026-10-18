import { useState, type ReactNode } from 'react'
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native'
import * as ImagePicker from 'expo-image-picker'
import { LinearGradient } from 'expo-linear-gradient'
import {
  BookIcon,
  CheckIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  CloseIcon,
  GridIcon,
  HelpIcon,
  ImageIcon,
  InfoIcon,
  MoonIcon,
  PlayIcon,
  PlusIcon,
  ServerIcon,
  SparklesIcon,
  SunIcon,
  TrashIcon,
  VolumeIcon,
} from '@/components/Icons'
import { Logo } from '@/components/Logo'
import { Button, Segmented, Slider, Toggle } from '@/components/ui'
import {
  ACCENTS,
  GENRES,
  WALLPAPERS,
  useSettingsStore,
  type ThemeMode,
} from '@/shared/store/settingsStore'
import { useLibraryStore } from '@/shared/store/libraryStore'
import { useUiStore } from '@/shared/store/uiStore'
import { useThemeColors } from '@/shared/theme/theme'

type View_ =
  | 'root'
  | 'playback'
  | 'algorithm'
  | 'appearance'
  | 'customization'
  | 'audio'
  | 'storage'
  | 'about'
  | 'docs'
  | 'support'

const VIEW_TITLES: Record<Exclude<View_, 'root'>, string> = {
  playback: 'Reproducción',
  algorithm: 'Mi algoritmo',
  appearance: 'Apariencia',
  customization: 'Personalización',
  audio: 'Audio',
  storage: 'Caché y almacenamiento',
  about: 'Acerca de',
  docs: 'Documentación',
  support: 'Soporte',
}

const SETTINGS_SEARCH: Array<{ view: Exclude<View_, 'root'>; label: string }> = [
  { view: 'playback', label: 'Reproducción automática' },
  { view: 'algorithm', label: 'Mi algoritmo, gustos y artistas' },
  { view: 'appearance', label: 'Tema claro u oscuro' },
  { view: 'appearance', label: 'Color de énfasis' },
  { view: 'customization', label: 'Fondo de pantalla' },
  { view: 'audio', label: 'Efecto cristal' },
  { view: 'storage', label: 'Borrar datos' },
  { view: 'about', label: 'Versión' },
]

function Card({ children }: { children: ReactNode }) {
  const colors = useThemeColors()
  return (
    <View
      className="rounded-2xl p-3.5"
      style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
    >
      {children}
    </View>
  )
}

function SwitchRow({
  title,
  description,
  checked,
  onChange,
}: {
  title: string
  description?: string
  checked: boolean
  onChange: (v: boolean) => void
}) {
  const colors = useThemeColors()
  return (
    <Card>
      <View className="flex-row items-center justify-between gap-4">
        <View className="min-w-0 flex-1">
          <Text className="text-sm font-medium" style={{ color: colors.ink }}>
            {title}
          </Text>
          {description ? (
            <Text className="mt-0.5 text-xs" style={{ color: colors.faint }}>
              {description}
            </Text>
          ) : null}
        </View>
        <Toggle checked={checked} onChange={onChange} label={title} />
      </View>
    </Card>
  )
}

function RangeRow({
  label,
  value,
  min,
  max,
  format,
  onChange,
}: {
  label: string
  value: number
  min: number
  max: number
  format: (value: number) => string
  onChange: (value: number) => void
}) {
  const colors = useThemeColors()
  const ratio = max === min ? 0 : (value - min) / (max - min)
  return (
    <View className="py-1.5">
      <View className="flex-row items-center justify-between">
        <Text className="text-sm" style={{ color: colors.muted }}>
          {label}
        </Text>
        <Text className="text-sm" style={{ color: colors.faint }}>
          {format(value)}
        </Text>
      </View>
      <Slider
        value={ratio}
        trackColor={colors.overlay3}
        fillColor={colors.accent}
        onChange={(next) => onChange(min + next * (max - min))}
      />
    </View>
  )
}

function Row({
  icon,
  label,
  hint,
  onPress,
}: {
  icon: ReactNode
  label: string
  hint?: string
  onPress: () => void
}) {
  const colors = useThemeColors()
  return (
    <Pressable onPress={onPress} className="flex-row items-center gap-3.5 rounded-xl px-3 py-3">
      <View className="h-8 w-8 items-center justify-center">{icon}</View>
      <Text numberOfLines={1} className="min-w-0 flex-1 text-sm font-medium" style={{ color: colors.ink }}>
        {label}
      </Text>
      {hint ? (
        <Text className="text-xs" style={{ color: colors.faint }}>
          {hint}
        </Text>
      ) : null}
      <ChevronRightIcon size={16} color={colors.faint} />
    </Pressable>
  )
}

function Group({ title, children }: { title: string; children: ReactNode }) {
  const colors = useThemeColors()
  return (
    <View className="mt-5">
      <Text className="mb-1.5 px-3 text-xs font-semibold uppercase tracking-wider" style={{ color: colors.faint }}>
        {title}
      </Text>
      <View className="overflow-hidden rounded-2xl" style={{ borderWidth: 1, borderColor: colors.hair }}>
        {children}
      </View>
    </View>
  )
}

export function SettingsPage() {
  const colors = useThemeColors()
  const [view, setView] = useState<View_>('root')
  const [filter, setFilter] = useState('')
  const [artistInput, setArtistInput] = useState('')

  const settings = useSettingsStore()
  const {
    accent,
    theme,
    dynamicBackground,
    glassOpacity,
    glassBlur,
    glassDim,
    wallpaper,
    wallpaperImage,
    wallpaperOpacity,
    wallpaperBlur,
    wallpaperDim,
    autoplayNext,
    restoreLastTrack,
    crossfade,
    genres,
    artists,
    discovery,
    setAccent,
    setTheme,
    setDynamicBackground,
    setGlass,
    setWallpaper,
    setWallpaperImage,
    setWallpaperTuning,
    setPlayback,
    resetAppearance,
    toggleGenre,
    addArtist,
    removeArtist,
    setDiscovery,
    resetAlgorithm,
  } = settings

  const clearSearchHistory = useUiStore((state) => state.clearSearchHistory)
  const notify = useUiStore((state) => state.notify)
  const clearHistory = useLibraryStore((state) => state.clearHistory)

  const handleUpload = async () => {
    try {
      const permission = await ImagePicker.requestMediaLibraryPermissionsAsync()
      if (!permission.granted) {
        notify('Necesitamos permiso para acceder a tus fotos.', 'error')
        return
      }
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        quality: 0.8,
      })
      if (result.canceled || !result.assets[0]) return
      setWallpaperImage(result.assets[0].uri)
      if (wallpaper) setWallpaper(null)
      notify('Fondo actualizado')
    } catch {
      notify('No se pudo cargar la imagen.', 'error')
    }
  }

  const inputStyle = {
    height: 44,
    borderWidth: 1,
    borderColor: colors.hair,
    backgroundColor: colors.overlay1,
    borderRadius: 12,
    paddingHorizontal: 14,
    color: colors.ink,
    fontSize: 14,
  } as const

  if (view === 'root') {
    const term = filter.trim().toLowerCase()
    const matches = term ? SETTINGS_SEARCH.filter((item) => item.label.toLowerCase().includes(term)) : []

    return (
      <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
        <View className="px-3 pb-3 pt-3">
          <Text className="px-1 text-[1.6rem] font-semibold tracking-tight" style={{ color: colors.ink }}>
            Ajustes
          </Text>
          <View className="mt-3 flex-row items-center gap-2 px-3.5" style={[inputStyle, { paddingHorizontal: 14 }]}>
            <InfoIcon size={17} color={colors.faint} />
            <TextInput
              value={filter}
              onChangeText={setFilter}
              placeholder="Buscar ajustes…"
              placeholderTextColor={colors.faint}
              className="h-11 flex-1 text-sm"
              style={{ color: colors.ink }}
            />
          </View>
        </View>

        <View className="px-5 pb-6">
          {term ? (
            matches.length > 0 ? (
              <View className="overflow-hidden rounded-2xl" style={{ borderWidth: 1, borderColor: colors.hair }}>
                {matches.map((item, index) => (
                  <Pressable
                    key={`${item.view}-${item.label}`}
                    onPress={() => {
                      setFilter('')
                      setView(item.view)
                    }}
                    className="flex-row items-center gap-3 px-3.5 py-3"
                    style={{ borderTopWidth: index > 0 ? 1 : 0, borderTopColor: colors.hair }}
                  >
                    <Text numberOfLines={1} className="min-w-0 flex-1 text-sm" style={{ color: colors.ink }}>
                      {item.label}
                    </Text>
                    <ChevronRightIcon size={16} color={colors.faint} />
                  </Pressable>
                ))}
              </View>
            ) : (
              <Text className="px-1 py-6 text-center text-sm" style={{ color: colors.faint }}>
                Sin resultados.
              </Text>
            )
          ) : (
            <>
              <Group title="Preferencias">
                <Row icon={<PlayIcon size={19} color={colors.muted} />} label="Reproducción" onPress={() => setView('playback')} />
                <Row
                  icon={<SparklesIcon size={19} color={colors.muted} />}
                  label="Mi algoritmo"
                  onPress={() => setView('algorithm')}
                />
                <Row
                  icon={<ImageIcon size={19} color={colors.muted} />}
                  label="Personalización"
                  onPress={() => setView('customization')}
                />
                <Row icon={<VolumeIcon size={19} color={colors.muted} />} label="Audio" onPress={() => setView('audio')} />
                <Row
                  icon={<GridIcon size={19} color={colors.muted} />}
                  label="Apariencia"
                  onPress={() => setView('appearance')}
                />
              </Group>

              <Group title="Aplicación">
                <Row
                  icon={<ServerIcon size={19} color={colors.muted} />}
                  label="Caché y almacenamiento"
                  onPress={() => setView('storage')}
                />
                <Row icon={<InfoIcon size={19} color={colors.muted} />} label="Acerca de" onPress={() => setView('about')} />
              </Group>

              <Group title="Ayuda">
                <Row icon={<BookIcon size={19} color={colors.muted} />} label="Documentación" onPress={() => setView('docs')} />
                <Row icon={<HelpIcon size={19} color={colors.muted} />} label="Soporte" onPress={() => setView('support')} />
              </Group>
            </>
          )}
        </View>
      </ScrollView>
    )
  }

  return (
    <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
      <View className="flex-row items-center gap-2 px-3 pb-2 pt-3">
        <Pressable
          accessibilityLabel="Volver"
          onPress={() => setView('root')}
          className="h-10 w-10 items-center justify-center rounded-full"
        >
          <ChevronLeftIcon size={24} color={colors.ink} />
        </Pressable>
        <Text className="text-[1.15rem] font-semibold tracking-tight" style={{ color: colors.ink }}>
          {VIEW_TITLES[view]}
        </Text>
      </View>

      <View className="gap-3 px-5 pb-6">
        {view === 'playback' ? (
          <>
            <SwitchRow
              title="Reproducción automática"
              description="Continúa con la siguiente canción."
              checked={autoplayNext}
              onChange={(v) => setPlayback({ autoplayNext: v })}
            />
            <SwitchRow
              title="Recordar última canción"
              description="Restaura la cola al volver a abrir la app."
              checked={restoreLastTrack}
              onChange={(v) => setPlayback({ restoreLastTrack: v })}
            />
            <SwitchRow
              title="Transición suave"
              description="Fundido breve entre canciones."
              checked={crossfade}
              onChange={(v) => setPlayback({ crossfade: v })}
            />
          </>
        ) : null}

        {view === 'algorithm' ? (
          <>
            <Card>
              <Text className="text-sm font-medium" style={{ color: colors.ink }}>
                Géneros que te gustan
              </Text>
              <Text className="mt-0.5 text-xs" style={{ color: colors.faint }}>
                Elige uno o varios para afinar tus recomendaciones.
              </Text>
              <View className="mt-3 flex-row flex-wrap gap-2">
                {GENRES.map((genre) => {
                  const active = genres.includes(genre)
                  return (
                    <Pressable
                      key={genre}
                      onPress={() => toggleGenre(genre)}
                      className="rounded-full px-3.5 py-1.5"
                      style={{ backgroundColor: active ? colors.accent : colors.overlay2 }}
                    >
                      <Text className="text-sm font-medium" style={{ color: active ? colors.accentInk : colors.muted }}>
                        {genre}
                      </Text>
                    </Pressable>
                  )
                })}
              </View>
            </Card>

            <Card>
              <Text className="text-sm font-medium" style={{ color: colors.ink }}>
                Artistas favoritos
              </Text>
              <Text className="mt-0.5 text-xs" style={{ color: colors.faint }}>
                Añade artistas para priorizarlos en las sugerencias.
              </Text>
              <View className="mt-3 flex-row items-center gap-2">
                <TextInput
                  value={artistInput}
                  onChangeText={setArtistInput}
                  onSubmitEditing={() => {
                    addArtist(artistInput)
                    setArtistInput('')
                  }}
                  placeholder="Nombre del artista…"
                  placeholderTextColor={colors.faint}
                  style={[inputStyle, { flex: 1, minWidth: 0 }]}
                />
                <Button
                  size="sm"
                  icon={<PlusIcon size={16} color={colors.primaryInk} />}
                  disabled={!artistInput.trim()}
                  onPress={() => {
                    addArtist(artistInput)
                    setArtistInput('')
                  }}
                >
                  Añadir
                </Button>
              </View>
              {artists.length > 0 ? (
                <View className="mt-3 flex-row flex-wrap gap-2">
                  {artists.map((artist) => (
                    <View
                      key={artist}
                      className="flex-row items-center gap-1.5 rounded-full py-1 pl-3 pr-1.5"
                      style={{ backgroundColor: colors.overlay2 }}
                    >
                      <Text className="text-sm" style={{ color: colors.ink }}>
                        {artist}
                      </Text>
                      <Pressable
                        accessibilityLabel={`Quitar ${artist}`}
                        onPress={() => removeArtist(artist)}
                        className="h-5 w-5 items-center justify-center rounded-full"
                      >
                        <CloseIcon size={14} color={colors.faint} />
                      </Pressable>
                    </View>
                  ))}
                </View>
              ) : (
                <Text className="mt-3 text-xs" style={{ color: colors.faint }}>
                  Aún no has añadido artistas.
                </Text>
              )}
            </Card>

            <Card>
              <View className="flex-row items-center justify-between">
                <Text className="text-sm" style={{ color: colors.muted }}>
                  Nivel de descubrimiento
                </Text>
                <Text className="text-sm" style={{ color: colors.faint }}>
                  {Math.round(discovery * 100)}%
                </Text>
              </View>
              <Slider
                value={discovery}
                trackColor={colors.overlay3}
                fillColor={colors.accent}
                onChange={setDiscovery}
              />
              <View className="flex-row justify-between">
                <Text className="text-[11px]" style={{ color: colors.faint }}>
                  Lo que ya conozco
                </Text>
                <Text className="text-[11px]" style={{ color: colors.faint }}>
                  Novedades
                </Text>
              </View>
            </Card>

            <Button
              variant="subtle"
              fullWidth
              icon={<TrashIcon size={17} color={colors.ink} />}
              onPress={() => {
                resetAlgorithm()
                notify('Algoritmo restablecido')
              }}
            >
              Restablecer algoritmo
            </Button>
          </>
        ) : null}

        {view === 'appearance' ? (
          <>
            <Card>
              <Text className="mb-2 text-sm font-medium" style={{ color: colors.ink }}>
                Tema
              </Text>
              <Segmented
                value={theme}
                onChange={(id) => setTheme(id as ThemeMode)}
                tabs={[
                  { id: 'system', label: 'Sistema' },
                  { id: 'light', label: 'Claro' },
                  { id: 'dark', label: 'Oscuro' },
                ]}
              />
              <View className="mt-2 flex-row items-center gap-1.5">
                {theme === 'light' ? (
                  <SunIcon size={14} color={colors.faint} />
                ) : (
                  <MoonIcon size={14} color={colors.faint} />
                )}
                <Text className="text-xs" style={{ color: colors.faint }}>
                  {theme === 'system'
                    ? 'Sigue el tema del sistema.'
                    : theme === 'light'
                      ? 'Interfaz clara.'
                      : 'Interfaz oscura (recomendado).'}
                </Text>
              </View>
            </Card>

            <Card>
              <Text className="mb-3 text-sm font-medium" style={{ color: colors.ink }}>
                Color de énfasis
              </Text>
              <View className="flex-row flex-wrap gap-3">
                {ACCENTS.map((option) => {
                  const active = option.id === accent
                  return (
                    <Pressable
                      key={option.id}
                      accessibilityLabel={option.label}
                      onPress={() => setAccent(option.id)}
                      className="h-10 w-10 items-center justify-center rounded-full"
                      style={{
                        backgroundColor: option.color,
                        borderWidth: active ? 3 : 0,
                        borderColor: colors.bg,
                      }}
                    >
                      {active ? <CheckIcon size={16} color={option.ink} /> : null}
                    </Pressable>
                  )
                })}
              </View>
            </Card>

            <SwitchRow
              title="Fondo dinámico"
              description="Tiñe las pantallas con los colores de la portada."
              checked={dynamicBackground}
              onChange={setDynamicBackground}
            />
          </>
        ) : null}

        {view === 'audio' ? (
          <Card>
            <RangeRow
              label="Opacidad del cristal"
              value={glassOpacity}
              min={0.2}
              max={1}
              format={(v) => `${Math.round(v * 100)}%`}
              onChange={(v) => setGlass({ glassOpacity: Math.round(v * 100) / 100 })}
            />
            <RangeRow
              label="Desenfoque"
              value={glassBlur}
              min={0}
              max={40}
              format={(v) => `${Math.round(v)}px`}
              onChange={(v) => setGlass({ glassBlur: Math.round(v) })}
            />
            <RangeRow
              label="Oscurecimiento"
              value={glassDim}
              min={0}
              max={0.6}
              format={(v) => `${Math.round(v * 100)}%`}
              onChange={(v) => setGlass({ glassDim: Math.round(v * 100) / 100 })}
            />
          </Card>
        ) : null}

        {view === 'customization' ? (
          <>
            <Card>
              <Text className="mb-2 text-sm font-medium" style={{ color: colors.ink }}>
                Tu imagen
              </Text>
              {wallpaperImage ? (
                <View className="gap-3">
                  <View className="h-40 w-full overflow-hidden rounded-xl">
                    <LinearGradient colors={[colors.overlay2, colors.overlay3]} style={{ flex: 1 }}>
                      <View style={{ flex: 1 }} />
                    </LinearGradient>
                  </View>
                  <View className="flex-row gap-2">
                    <Button size="sm" variant="subtle" onPress={() => void handleUpload()}>
                      Cambiar imagen
                    </Button>
                    <Button
                      size="sm"
                      variant="danger"
                      icon={<TrashIcon size={16} color={colors.accent} />}
                      onPress={() => setWallpaperImage(null)}
                    >
                      Quitar
                    </Button>
                  </View>
                </View>
              ) : (
                <Button
                  size="sm"
                  variant="subtle"
                  icon={<ImageIcon size={17} color={colors.ink} />}
                  onPress={() => void handleUpload()}
                >
                  Subir una imagen
                </Button>
              )}
            </Card>

            <Card>
              <Text className="mb-2 text-sm font-medium" style={{ color: colors.ink }}>
                Degradados
              </Text>
              <View className="flex-row flex-wrap gap-2">
                <Pressable
                  onPress={() => setWallpaper(null)}
                  className="items-center justify-center rounded-xl"
                  style={{
                    width: '31%',
                    aspectRatio: 3 / 4,
                    backgroundColor: colors.overlay2,
                    borderWidth: wallpaper === null && !wallpaperImage ? 2 : 1,
                    borderColor: wallpaper === null && !wallpaperImage ? colors.accent : colors.hair,
                  }}
                >
                  <Text className="text-[0.7rem]" style={{ color: wallpaper === null && !wallpaperImage ? colors.ink : colors.faint }}>
                    Ninguno
                  </Text>
                </Pressable>
                {WALLPAPERS.map((preset) => {
                  const active = wallpaper === preset.id && !wallpaperImage
                  return (
                    <Pressable
                      key={preset.id}
                      onPress={() => {
                        setWallpaperImage(null)
                        setWallpaper(preset.id)
                      }}
                      className="overflow-hidden rounded-xl"
                      style={{
                        width: '31%',
                        aspectRatio: 3 / 4,
                        borderWidth: active ? 2 : 0,
                        borderColor: colors.accent,
                      }}
                    >
                      <LinearGradient
                        colors={preset.colors as [string, string, ...string[]]}
                        style={{ flex: 1, alignItems: 'center', justifyContent: 'flex-end', padding: 6 }}
                      >
                        <View className="rounded-full px-2 py-0.5" style={{ backgroundColor: 'rgba(0,0,0,0.4)' }}>
                          <Text className="text-[0.7rem] font-medium" style={{ color: '#ffffff' }}>
                            {preset.label}
                          </Text>
                        </View>
                      </LinearGradient>
                    </Pressable>
                  )
                })}
              </View>
            </Card>

            <Card>
              <RangeRow
                label="Transparencia"
                value={wallpaperOpacity}
                min={0.15}
                max={1}
                format={(v) => `${Math.round(v * 100)}%`}
                onChange={(v) => setWallpaperTuning({ wallpaperOpacity: Math.round(v * 100) / 100 })}
              />
              <RangeRow
                label="Desenfoque"
                value={wallpaperBlur}
                min={0}
                max={40}
                format={(v) => `${Math.round(v)}px`}
                onChange={(v) => setWallpaperTuning({ wallpaperBlur: Math.round(v) })}
              />
              <RangeRow
                label="Oscurecimiento"
                value={wallpaperDim}
                min={0}
                max={0.85}
                format={(v) => `${Math.round(v * 100)}%`}
                onChange={(v) => setWallpaperTuning({ wallpaperDim: Math.round(v * 100) / 100 })}
              />
            </Card>
          </>
        ) : null}

        {view === 'storage' ? (
          <>
            <Pressable
              onPress={() => {
                clearSearchHistory()
                notify('Búsquedas borradas')
              }}
              className="flex-row items-center gap-3 rounded-2xl px-3.5 py-3"
              style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
            >
              <TrashIcon size={18} color={colors.muted} />
              <Text className="text-sm font-medium" style={{ color: colors.ink }}>
                Borrar búsquedas recientes
              </Text>
            </Pressable>
            <Pressable
              onPress={() => {
                clearHistory()
                notify('Historial borrado')
              }}
              className="flex-row items-center gap-3 rounded-2xl px-3.5 py-3"
              style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
            >
              <TrashIcon size={18} color={colors.muted} />
              <Text className="text-sm font-medium" style={{ color: colors.ink }}>
                Borrar historial de reproducción
              </Text>
            </Pressable>
            <Pressable
              onPress={() => {
                useLibraryStore.setState({ playlists: [], favorites: [], history: [] })
                notify('Biblioteca vaciada')
              }}
              className="flex-row items-center gap-3 rounded-2xl px-3.5 py-3"
              style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
            >
              <TrashIcon size={18} color={colors.accent} />
              <Text className="text-sm font-medium" style={{ color: colors.accent }}>
                Vaciar toda la biblioteca
              </Text>
            </Pressable>
            <Pressable
              onPress={() => {
                resetAppearance()
                notify('Apariencia restablecida')
              }}
              className="flex-row items-center gap-3 rounded-2xl px-3.5 py-3"
              style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
            >
              <GridIcon size={18} color={colors.accent} />
              <Text className="text-sm font-medium" style={{ color: colors.accent }}>
                Restablecer apariencia
              </Text>
            </Pressable>
          </>
        ) : null}

        {view === 'about' ? (
          <>
            <View
              className="flex-row items-center gap-3 rounded-2xl p-4"
              style={{ backgroundColor: colors.overlay1, borderWidth: 1, borderColor: colors.hair }}
            >
              <Logo size={20} withWordmark={false} />
              <View className="min-w-0">
                <Text className="text-sm font-semibold" style={{ color: colors.ink }}>
                  PoP Music
                </Text>
                <Text className="text-xs" style={{ color: colors.faint }}>
                  Versión 1.0.0
                </Text>
              </View>
            </View>
            <Text className="px-1 text-xs leading-relaxed" style={{ color: colors.faint }}>
              Reproductor de música mobile-first con letras sincronizadas. Integra YouTube, LRCLIB, Spotify y
              reconocimiento de audio.
            </Text>
          </>
        ) : null}

        {view === 'docs' ? (
          <Text className="px-1 text-sm leading-relaxed" style={{ color: colors.muted }}>
            PoP Music reproduce música desde YouTube, muestra letras sincronizadas con LRCLIB y permite importar
            playlists de Spotify. Usa el buscador para encontrar canciones, o el micrófono para reconocer la música
            que suena a tu alrededor.
          </Text>
        ) : null}

        {view === 'support' ? (
          <Text className="px-1 text-sm leading-relaxed" style={{ color: colors.muted }}>
            ¿Necesitas ayuda? Escríbenos y te responderemos lo antes posible.
          </Text>
        ) : null}
      </View>
    </ScrollView>
  )
}
