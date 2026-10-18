import { useRef, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
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
import { fileToDownscaledDataUrl } from '@/shared/utils/image'

type View =
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

const VIEW_TITLES: Record<Exclude<View, 'root'>, string> = {
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

const SETTINGS_SEARCH: Array<{ view: Exclude<View, 'root'>; label: string }> = [
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
  return <div className="rounded-2xl hairline surface-1 p-3.5">{children}</div>
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
  return (
    <Card>
      <div className="flex items-center justify-between gap-4">
        <div className="min-w-0">
          <p className="text-sm font-medium">{title}</p>
          {description ? <p className="mt-0.5 text-xs text-faint">{description}</p> : null}
        </div>
        <Toggle checked={checked} onChange={onChange} label={title} />
      </div>
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
  const ratio = max === min ? 0 : (value - min) / (max - min)
  return (
    <div className="py-1.5">
      <div className="flex items-center justify-between text-sm">
        <span className="text-muted">{label}</span>
        <span className="tabular-nums text-faint">{format(value)}</span>
      </div>
      <Slider
        value={ratio}
        ariaLabel={label}
        onChange={(next) => onChange(min + next * (max - min))}
        trackClassName="h-1.5 bg-[color:var(--overlay-3)]"
        fillClassName="bg-accent"
      />
    </div>
  )
}

function Row({
  icon,
  label,
  hint,
  onClick,
}: {
  icon: ReactNode
  label: string
  hint?: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center gap-3.5 rounded-xl px-3 py-3 text-left tap"
    >
      <span className="grid h-8 w-8 shrink-0 place-items-center text-muted">{icon}</span>
      <span className="min-w-0 flex-1 truncate text-sm font-medium">{label}</span>
      {hint ? <span className="shrink-0 text-xs text-faint">{hint}</span> : null}
      <ChevronRightIcon size={16} className="shrink-0 text-faint" />
    </button>
  )
}

function Group({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-5">
      <h2 className="mb-1.5 px-3 text-xs font-semibold uppercase tracking-wider text-faint">
        {title}
      </h2>
      <div className="overflow-hidden rounded-2xl hairline">{children}</div>
    </section>
  )
}

export function SettingsPage() {
  const navigate = useNavigate()
  const [view, setView] = useState<View>('root')
  const [filter, setFilter] = useState('')

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
  } = useSettingsStore()

  const [artistInput, setArtistInput] = useState('')

  const clearSearchHistory = useUiStore((state) => state.clearSearchHistory)
  const notify = useUiStore((state) => state.notify)
  const clearHistory = useLibraryStore((state) => state.clearHistory)
  const fileRef = useRef<HTMLInputElement>(null)

  const handleUpload = async (file: File | undefined) => {
    if (!file) return
    try {
      const dataUrl = await fileToDownscaledDataUrl(file)
      setWallpaperImage(dataUrl)
      if (wallpaper) setWallpaper(null)
      notify('Fondo actualizado')
    } catch {
      notify('No se pudo cargar la imagen.', 'error')
    }
  }

  const back = () => setView('root')

  if (view === 'root') {
    const term = filter.trim().toLowerCase()
    const matches = term ? SETTINGS_SEARCH.filter((item) => item.label.toLowerCase().includes(term)) : []

    return (
      <div>
        <header className="sticky top-0 z-20 px-3 pb-3 pt-[calc(0.75rem+env(safe-area-inset-top))] backdrop-blur-xl">
          <h1 className="px-1 text-[1.6rem] font-semibold tracking-tight">Ajustes</h1>
          <div className="mt-3 flex h-11 items-center gap-2 rounded-2xl hairline surface-2 px-3.5">
            <InfoIcon size={17} className="text-faint" />
            <input
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder="Buscar ajustes…"
              className="h-11 flex-1 bg-transparent text-sm outline-none placeholder:text-faint"
            />
          </div>
        </header>

        <div className="px-5 pb-6">
          {term ? (
            matches.length > 0 ? (
              <div className="overflow-hidden rounded-2xl hairline">
                {matches.map((item, index) => (
                  <button
                    key={`${item.view}-${item.label}`}
                    type="button"
                    onClick={() => {
                      setFilter('')
                      setView(item.view)
                    }}
                    className={`flex w-full items-center gap-3 px-3.5 py-3 text-left text-sm tap ${
                      index > 0 ? 'border-t border-[color:var(--hair)]' : ''
                    }`}
                  >
                    <span className="min-w-0 flex-1 truncate">{item.label}</span>
                    <ChevronRightIcon size={16} className="shrink-0 text-faint" />
                  </button>
                ))}
              </div>
            ) : (
              <p className="px-1 py-6 text-center text-sm text-faint">Sin resultados.</p>
            )
          ) : (
            <>
              <Group title="Preferencias">
                <Row icon={<PlayIcon size={19} />} label="Reproducción" onClick={() => setView('playback')} />
                <Row
                  icon={<SparklesIcon size={19} />}
                  label="Mi algoritmo"
                  onClick={() => setView('algorithm')}
                />
                <Row
                  icon={<ImageIcon size={19} />}
                  label="Personalización"
                  onClick={() => setView('customization')}
                />
                <Row icon={<VolumeIcon size={19} />} label="Audio" onClick={() => setView('audio')} />
                <Row
                  icon={<GridIcon size={19} />}
                  label="Apariencia"
                  onClick={() => setView('appearance')}
                />
              </Group>

              <Group title="Aplicación">
                <Row
                  icon={<ServerIcon size={19} />}
                  label="Caché y almacenamiento"
                  onClick={() => setView('storage')}
                />
                <Row icon={<InfoIcon size={19} />} label="Acerca de" onClick={() => setView('about')} />
              </Group>

              <Group title="Ayuda">
                <Row icon={<BookIcon size={19} />} label="Documentación" onClick={() => setView('docs')} />
                <Row icon={<HelpIcon size={19} />} label="Soporte" onClick={() => setView('support')} />
              </Group>
            </>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="animate-page-in">
      <header className="sticky top-0 z-20 flex items-center gap-2 px-3 pb-2 pt-[calc(0.75rem+env(safe-area-inset-top))] backdrop-blur-xl">
        <button
          type="button"
          aria-label="Volver"
          onClick={back}
          className="grid h-10 w-10 place-items-center rounded-full text-ink tap"
        >
          <ChevronLeftIcon size={24} />
        </button>
        <h1 className="text-[1.15rem] font-semibold tracking-tight">{VIEW_TITLES[view]}</h1>
      </header>

      <div className="space-y-3 px-5 pb-6">
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
              <p className="text-sm font-medium">Géneros que te gustan</p>
              <p className="mt-0.5 text-xs text-faint">
                Elige uno o varios para afinar tus recomendaciones.
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                {GENRES.map((genre) => {
                  const active = genres.includes(genre)
                  return (
                    <button
                      key={genre}
                      type="button"
                      onClick={() => toggleGenre(genre)}
                      className={`rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors ${
                        active
                          ? 'bg-accent text-[color:var(--color-accent-ink)]'
                          : 'surface-2 text-muted tap'
                      }`}
                    >
                      {genre}
                    </button>
                  )
                })}
              </div>
            </Card>

            <Card>
              <p className="text-sm font-medium">Artistas favoritos</p>
              <p className="mt-0.5 text-xs text-faint">
                Añade artistas para priorizarlos en las sugerencias.
              </p>
              <div className="mt-3 flex items-center gap-2">
                <input
                  value={artistInput}
                  onChange={(event) => setArtistInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault()
                      addArtist(artistInput)
                      setArtistInput('')
                    }
                  }}
                  placeholder="Nombre del artista…"
                  className="hairline surface-1 h-11 min-w-0 flex-1 rounded-xl px-3.5 text-sm outline-none focus:border-accent/50"
                />
                <Button
                  size="sm"
                  icon={<PlusIcon size={16} />}
                  disabled={!artistInput.trim()}
                  onClick={() => {
                    addArtist(artistInput)
                    setArtistInput('')
                  }}
                >
                  Añadir
                </Button>
              </div>
              {artists.length > 0 ? (
                <div className="mt-3 flex flex-wrap gap-2">
                  {artists.map((artist) => (
                    <span
                      key={artist}
                      className="flex items-center gap-1.5 rounded-full surface-2 py-1 pl-3 pr-1.5 text-sm"
                    >
                      {artist}
                      <button
                        type="button"
                        aria-label={`Quitar ${artist}`}
                        onClick={() => removeArtist(artist)}
                        className="grid h-5 w-5 place-items-center rounded-full text-faint"
                      >
                        <CloseIcon size={14} />
                      </button>
                    </span>
                  ))}
                </div>
              ) : (
                <p className="mt-3 text-xs text-faint">Aún no has añadido artistas.</p>
              )}
            </Card>

            <Card>
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted">Nivel de descubrimiento</span>
                <span className="tabular-nums text-faint">{Math.round(discovery * 100)}%</span>
              </div>
              <Slider
                value={discovery}
                ariaLabel="Nivel de descubrimiento"
                onChange={setDiscovery}
                trackClassName="h-1.5 bg-[color:var(--overlay-3)]"
                fillClassName="bg-accent"
              />
              <div className="flex justify-between text-[11px] text-faint">
                <span>Lo que ya conozco</span>
                <span>Novedades</span>
              </div>
            </Card>

            <Button
              variant="subtle"
              fullWidth
              icon={<TrashIcon size={17} />}
              onClick={() => {
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
              <p className="mb-2 text-sm font-medium">Tema</p>
              <Segmented
                value={theme}
                onChange={(id) => setTheme(id as ThemeMode)}
                tabs={[
                  { id: 'system', label: 'Sistema' },
                  { id: 'light', label: 'Claro' },
                  { id: 'dark', label: 'Oscuro' },
                ]}
              />
              <div className="mt-2 flex items-center gap-1.5 text-xs text-faint">
                {theme === 'light' ? <SunIcon size={14} /> : <MoonIcon size={14} />}
                <span>
                  {theme === 'system'
                    ? 'Sigue el tema del sistema.'
                    : theme === 'light'
                      ? 'Interfaz clara.'
                      : 'Interfaz oscura (recomendado).'}
                </span>
              </div>
            </Card>

            <Card>
              <p className="mb-3 text-sm font-medium">Color de énfasis</p>
              <div className="flex flex-wrap gap-3">
                {ACCENTS.map((option) => {
                  const active = option.id === accent
                  return (
                    <button
                      key={option.id}
                      type="button"
                      aria-label={option.label}
                      onClick={() => setAccent(option.id)}
                      className="relative grid h-10 w-10 place-items-center rounded-full transition-transform active:scale-95"
                      style={{
                        background: option.color,
                        boxShadow: active
                          ? `0 0 0 2px var(--color-bg), 0 0 0 4px ${option.color}`
                          : undefined,
                      }}
                    >
                      {active ? <CheckIcon size={16} style={{ color: option.ink }} /> : null}
                    </button>
                  )
                })}
              </div>
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
              <p className="mb-2 text-sm font-medium">Tu imagen</p>
              <input
                ref={fileRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(event) => {
                  void handleUpload(event.target.files?.[0])
                  event.target.value = ''
                }}
              />
              {wallpaperImage ? (
                <div className="space-y-3">
                  <div
                    className="h-40 w-full rounded-xl bg-cover bg-center"
                    style={{ backgroundImage: `url(${wallpaperImage})` }}
                  />
                  <div className="flex gap-2">
                    <Button size="sm" variant="subtle" onClick={() => fileRef.current?.click()}>
                      Cambiar imagen
                    </Button>
                    <Button
                      size="sm"
                      variant="danger"
                      icon={<TrashIcon size={16} />}
                      onClick={() => setWallpaperImage(null)}
                    >
                      Quitar
                    </Button>
                  </div>
                </div>
              ) : (
                <Button
                  size="sm"
                  variant="subtle"
                  icon={<ImageIcon size={17} />}
                  onClick={() => fileRef.current?.click()}
                >
                  Subir una imagen
                </Button>
              )}
            </Card>

            <Card>
              <p className="mb-2 text-sm font-medium">Degradados</p>
              <div className="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  onClick={() => setWallpaper(null)}
                  className={`relative flex aspect-[3/4] items-center justify-center rounded-xl text-[0.7rem] transition-all ${
                    wallpaper === null && !wallpaperImage
                      ? 'ring-2 ring-accent text-ink'
                      : 'hairline surface-2 text-faint'
                  }`}
                >
                  Ninguno
                </button>
                {WALLPAPERS.map((preset) => {
                  const active = wallpaper === preset.css && !wallpaperImage
                  return (
                    <button
                      key={preset.id}
                      type="button"
                      onClick={() => {
                        setWallpaperImage(null)
                        setWallpaper(preset.css)
                      }}
                      className={`relative flex aspect-[3/4] items-end justify-center rounded-xl p-1.5 text-[0.7rem] font-medium text-white transition-all ${
                        active ? 'ring-2 ring-accent' : ''
                      }`}
                      style={{ background: preset.css }}
                    >
                      <span className="rounded-full bg-black/40 px-2 py-0.5 backdrop-blur">
                        {preset.label}
                      </span>
                    </button>
                  )
                })}
              </div>
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
            <button
              type="button"
              onClick={() => {
                clearSearchHistory()
                notify('Búsquedas borradas')
              }}
              className="flex w-full items-center gap-3 rounded-2xl hairline surface-1 px-3.5 py-3 text-left text-sm font-medium tap"
            >
              <TrashIcon size={18} className="text-muted" />
              Borrar búsquedas recientes
            </button>
            <button
              type="button"
              onClick={() => {
                clearHistory()
                notify('Historial borrado')
              }}
              className="flex w-full items-center gap-3 rounded-2xl hairline surface-1 px-3.5 py-3 text-left text-sm font-medium tap"
            >
              <TrashIcon size={18} className="text-muted" />
              Borrar historial de reproducción
            </button>
            <button
              type="button"
              onClick={() => {
                useLibraryStore.setState({ playlists: [], favorites: [], history: [] })
                notify('Biblioteca vaciada')
              }}
              className="flex w-full items-center gap-3 rounded-2xl hairline surface-1 px-3.5 py-3 text-left text-sm font-medium text-accent tap"
            >
              <TrashIcon size={18} />
              Vaciar toda la biblioteca
            </button>
            <button
              type="button"
              onClick={() => {
                resetAppearance()
                notify('Apariencia restablecida')
              }}
              className="flex w-full items-center gap-3 rounded-2xl hairline surface-1 px-3.5 py-3 text-left text-sm font-medium text-accent tap"
            >
              <GridIcon size={18} />
              Restablecer apariencia
            </button>
          </>
        ) : null}

        {view === 'about' ? (
          <>
            <div className="flex items-center gap-3 rounded-2xl hairline surface-1 p-4">
              <Logo size={20} withWordmark={false} />
              <div className="min-w-0">
                <p className="text-sm font-semibold">PoP Music</p>
                <p className="text-xs text-faint">Versión 1.0.0</p>
              </div>
            </div>
            <p className="px-1 text-xs leading-relaxed text-faint">
              Reproductor de música mobile-first con letras sincronizadas. Integra YouTube, LRCLIB,
              Spotify y reconocimiento de audio.
            </p>
          </>
        ) : null}

        {view === 'docs' ? (
          <p className="px-1 text-sm leading-relaxed text-muted">
            PoP Music reproduce música desde YouTube, muestra letras sincronizadas con LRCLIB y
            permite importar playlists de Spotify. Usa el buscador para encontrar canciones, o el
            micrófono para reconocer la música que suena a tu alrededor.
          </p>
        ) : null}

        {view === 'support' ? (
          <p className="px-1 text-sm leading-relaxed text-muted">
            ¿Necesitas ayuda? Escríbenos y te responderemos lo antes posible.
          </p>
        ) : null}
      </div>
    </div>
  )
}
