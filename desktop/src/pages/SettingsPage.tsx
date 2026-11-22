import { useRef, useState, type ReactNode } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import {
  Check,
  ChevronRight,
  ExternalLink,
  Image as ImageIcon,
  Info,
  Layers,
  Music2,
  Palette,
  PictureInPicture2,
  Play,
  RotateCcw,
  Shuffle,
  SlidersHorizontal,
  Timer,
  Volume2,
} from 'lucide-react'
import { Slider } from '@/components/ui'
import {
  ACCENTS,
  WALLPAPERS,
  useSettingsStore,
  type ThemeMode,
} from '@/shared/store/settingsStore'
import { useUiStore } from '@/shared/store/uiStore'
import { fileToDownscaledDataUrl } from '@/shared/utils/image'

const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1]

type Tab = 'appearance' | 'playback' | 'overlay' | 'about'
type FondoMode = 'none' | 'gradient' | 'image'

const TABS: Array<{ id: Tab; label: string; icon: ReactNode }> = [
  { id: 'appearance', label: 'Apariencia', icon: <Palette size={18} /> },
  { id: 'playback', label: 'Reproducción', icon: <Play size={18} /> },
  { id: 'overlay', label: 'Reproductor flotante', icon: <PictureInPicture2 size={18} /> },
  { id: 'about', label: 'Acerca de', icon: <Info size={18} /> },
]

/* ---------------------------- Bloques de UI ---------------------------- */

function Section({ label, children }: { label: string; children: ReactNode }) {
  return (
    <section className="mb-8">
      <h2 className="mb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-[#8c8780]">
        {label}
      </h2>
      <div className="divide-y divide-white/5 overflow-hidden rounded-[20px] bg-[#121212]">
        {children}
      </div>
    </section>
  )
}

function Row({
  icon,
  title,
  description,
  control,
}: {
  icon?: ReactNode
  title: string
  description?: string
  control: ReactNode
}) {
  return (
    <div className="flex min-h-[52px] items-center gap-3 px-4 py-2">
      {icon ? (
        <span className="grid w-[18px] shrink-0 place-items-center text-[#8c8780]">{icon}</span>
      ) : null}
      <div className="min-w-0 flex-1">
        <p className="text-[14px] font-medium leading-tight text-[#f5f3f0]">{title}</p>
        {description ? (
          <p className="mt-0.5 text-[12px] leading-tight text-[#8c8780]">{description}</p>
        ) : null}
      </div>
      <div className="shrink-0">{control}</div>
    </div>
  )
}

function SliderRow({
  icon,
  title,
  description,
  value,
  min,
  max,
  format,
  onChange,
}: {
  icon?: ReactNode
  title: string
  description?: string
  value: number
  min: number
  max: number
  format: (value: number) => string
  onChange: (value: number) => void
}) {
  const ratio = max === min ? 0 : (value - min) / (max - min)
  return (
    <div className="px-4 py-3">
      <div className="flex items-center gap-3">
        {icon ? (
          <span className="grid w-[18px] shrink-0 place-items-center text-[#8c8780]">{icon}</span>
        ) : null}
        <div className="min-w-0 flex-1">
          <p className="text-[14px] font-medium leading-tight text-[#f5f3f0]">{title}</p>
          {description ? (
            <p className="mt-0.5 text-[12px] leading-tight text-[#8c8780]">{description}</p>
          ) : null}
        </div>
        <span className="shrink-0 text-[12px] tabular-nums text-[#8c8780]">{format(value)}</span>
      </div>
      <Slider
        value={ratio}
        ariaLabel={title}
        onChange={(next) => onChange(min + next * (max - min))}
        trackClassName="h-1.5 bg-white/10"
        fillClassName="bg-[#f5f3f0]"
        thumbClassName="bg-white"
      />
    </div>
  )
}

function Switch({
  checked,
  onChange,
  label,
}: {
  checked: boolean
  onChange: (value: boolean) => void
  label: string
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={`relative h-[26px] w-[44px] shrink-0 rounded-full transition-colors duration-200 ${
        checked ? 'bg-[#f5f3f0]' : 'bg-white/10'
      }`}
      style={{ transitionTimingFunction: 'cubic-bezier(0.22,1,0.36,1)' }}
    >
      <span
        className={`absolute top-[3px] h-5 w-5 rounded-full shadow transition-transform duration-200 ${
          checked ? 'translate-x-[21px] bg-[#0c0b0a]' : 'translate-x-[3px] bg-[#8c8780]'
        }`}
        style={{ transitionTimingFunction: 'cubic-bezier(0.22,1,0.36,1)' }}
      />
    </button>
  )
}

function Segmented<T extends string>({
  value,
  onChange,
  options,
}: {
  value: T
  onChange: (value: T) => void
  options: Array<{ id: T; label: string }>
}) {
  return (
    <div className="flex rounded-full bg-[#0c0b0a] p-1">
      {options.map((option) => {
        const active = option.id === value
        return (
          <button
            key={option.id}
            type="button"
            onClick={() => onChange(option.id)}
            className={`whitespace-nowrap rounded-full px-3 py-1.5 text-[12px] font-medium transition-all ${
              active
                ? 'bg-[#2a2a2a] text-[#f5f3f0] shadow-sm'
                : 'text-[#8c8780] hover:text-[#f5f3f0]'
            }`}
            style={{ transitionTimingFunction: 'cubic-bezier(0.22,1,0.36,1)' }}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}

function Kbd({ children }: { children: ReactNode }) {
  return (
    <kbd className="rounded-md border border-white/10 bg-[#0c0b0a] px-1.5 py-0.5 text-[11px] text-[#f5f3f0]">
      {children}
    </kbd>
  )
}

function PageTitle({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <header className="mb-7">
      <h1 className="text-[24px] font-semibold tracking-tight text-[#f5f3f0]">{title}</h1>
      <p className="mt-1 text-[13px] text-[#8c8780]">{subtitle}</p>
    </header>
  )
}

/* ------------------------------ Página ------------------------------ */

export function SettingsPage() {
  const [tab, setTab] = useState<Tab>('appearance')

  const {
    accent,
    theme,
    wallpaper,
    wallpaperImage,
    glassOpacity,
    glassBlur,
    glassDim,
    autoplayNext,
    restoreLastTrack,
    crossfade,
    setAccent,
    setTheme,
    setWallpaper,
    setWallpaperImage,
    setGlass,
    setPlayback,
  } = useSettingsStore()

  const notify = useUiStore((state) => state.notify)
  const fileRef = useRef<HTMLInputElement>(null)

  /* Ajustes sin store real: estados locales (no se persisten). */
  const [volume, setVolume] = useState(0.8)
  const [shuffle, setShuffle] = useState(false)
  const [quality, setQuality] = useState('auto')
  const [skipSilence, setSkipSilence] = useState(1)
  const [crossfadeSeconds, setCrossfadeSeconds] = useState(crossfade ? 3 : 0)
  const [overlayVisible, setOverlayVisible] = useState(false)

  const [fondoMode, setFondoMode] = useState<FondoMode>(
    wallpaperImage ? 'image' : wallpaper ? 'gradient' : 'none',
  )

  const changeFondo = (mode: FondoMode) => {
    if (mode === 'none') {
      setWallpaper(null)
      setWallpaperImage(null)
    } else if (mode === 'gradient') {
      setWallpaperImage(null)
      if (!wallpaper) setWallpaper(WALLPAPERS[0].css)
    }
    setFondoMode(mode)
  }

  const handleUpload = async (file: File | undefined) => {
    if (!file) return
    try {
      const dataUrl = await fileToDownscaledDataUrl(file)
      setWallpaperImage(dataUrl)
      setWallpaper(null)
      setFondoMode('image')
      notify('Fondo actualizado')
    } catch {
      notify('No se pudo cargar la imagen.', 'error')
    }
  }

  return (
    <div className="flex h-full min-h-0 gap-8 bg-[#0c0b0a] px-8 py-7">
      {/* Barra lateral de pestañas */}
      <aside className="w-[180px] shrink-0">
        <h1 className="mb-6 px-3 text-[22px] font-semibold tracking-tight text-[#f5f3f0]">
          Ajustes
        </h1>
        <nav className="flex flex-col gap-1">
          {TABS.map((item) => {
            const active = tab === item.id
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setTab(item.id)}
                className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-left text-[13px] font-medium transition-colors ${
                  active
                    ? 'bg-[#121212] text-[#f5f3f0]'
                    : 'text-[#8c8780] hover:text-[#f5f3f0]'
                }`}
                style={{ transitionTimingFunction: 'cubic-bezier(0.22,1,0.36,1)' }}
              >
                <span className={active ? 'text-[#f5f3f0]' : 'text-[#8c8780]'}>{item.icon}</span>
                <span className="min-w-0 flex-1">{item.label}</span>
                {active ? <span className="h-1.5 w-1.5 rounded-full bg-[#f5f3f0]" /> : null}
              </button>
            )
          })}
        </nav>
      </aside>

      {/* Contenido */}
      <main className="min-h-0 flex-1 overflow-y-auto pb-16 pr-1">
        <div className="mx-auto max-w-[760px]">
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={tab}
              initial={{ opacity: 0, y: 6, filter: 'blur(4px)' }}
              animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
              exit={{ opacity: 0, y: -6, filter: 'blur(4px)' }}
              transition={{ duration: 0.2, ease: EASE }}
            >
              {tab === 'appearance' ? (
                <>
                  <PageTitle title="Apariencia" subtitle="Personaliza cómo se ve PoP Music." />

                  <Section label="Tema">
                    <Row
                      icon={<Palette size={18} />}
                      title="Modo"
                      description="Elige la apariencia general."
                      control={
                        <Segmented<ThemeMode>
                          value={theme}
                          onChange={setTheme}
                          options={[
                            { id: 'dark', label: 'Oscuro' },
                            { id: 'light', label: 'Claro' },
                            { id: 'system', label: 'Sistema' },
                          ]}
                        />
                      }
                    />
                  </Section>

                  <Section label="Color de acento">
                    <div className="px-4 py-3.5">
                      <p className="mb-3 text-[14px] font-medium text-[#f5f3f0]">Color</p>
                      <div className="flex flex-wrap gap-3">
                        {ACCENTS.map((option) => {
                          const active = option.id === accent
                          return (
                            <button
                              key={option.id}
                              type="button"
                              aria-label={option.label}
                              title={option.label}
                              onClick={() => setAccent(option.id)}
                              className={`grid h-8 w-8 place-items-center rounded-full transition-transform ${
                                active ? 'scale-105' : 'hover:scale-105'
                              }`}
                              style={{
                                background: option.color,
                                boxShadow: active
                                  ? `0 0 0 2px #0c0b0a, 0 0 0 4px ${option.color}`
                                  : undefined,
                              }}
                            >
                              {active ? (
                                <Check size={15} strokeWidth={3} style={{ color: option.ink }} />
                              ) : null}
                            </button>
                          )
                        })}
                      </div>
                    </div>
                  </Section>

                  <Section label="Fondo">
                    <Row
                      icon={<ImageIcon size={18} />}
                      title="Estilo de fondo"
                      description="Ninguno, degradado o imagen."
                      control={
                        <Segmented<FondoMode>
                          value={fondoMode}
                          onChange={changeFondo}
                          options={[
                            { id: 'none', label: 'Ninguno' },
                            { id: 'gradient', label: 'Degradado' },
                            { id: 'image', label: 'Imagen' },
                          ]}
                        />
                      }
                    />
                    {fondoMode === 'gradient' ? (
                      <div className="px-4 pb-4">
                        <div className="grid grid-cols-5 gap-2">
                          {WALLPAPERS.map((preset) => {
                            const active = wallpaper === preset.css && !wallpaperImage
                            return (
                              <button
                                key={preset.id}
                                type="button"
                                title={preset.label}
                                aria-label={preset.label}
                                onClick={() => {
                                  setWallpaperImage(null)
                                  setWallpaper(preset.css)
                                }}
                                className={`h-14 rounded-xl transition-all ${
                                  active
                                    ? 'ring-2 ring-[#f5f3f0]'
                                    : 'opacity-80 hover:opacity-100'
                                }`}
                                style={{ background: preset.css }}
                              />
                            )
                          })}
                        </div>
                      </div>
                    ) : null}
                    {fondoMode === 'image' ? (
                      <div className="px-4 pb-4">
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
                          <div className="flex items-center gap-3">
                            <div
                              className="h-14 w-24 rounded-xl bg-cover bg-center"
                              style={{ backgroundImage: `url(${wallpaperImage})` }}
                            />
                            <div className="flex gap-2">
                              <button
                                type="button"
                                onClick={() => fileRef.current?.click()}
                                className="rounded-lg bg-white/10 px-3 py-1.5 text-[12px] font-medium text-[#f5f3f0] transition-colors hover:bg-white/15"
                              >
                                Cambiar
                              </button>
                              <button
                                type="button"
                                onClick={() => {
                                  setWallpaperImage(null)
                                  setFondoMode('none')
                                }}
                                className="rounded-lg px-3 py-1.5 text-[12px] font-medium text-[#8c8780] transition-colors hover:text-[#f5f3f0]"
                              >
                                Quitar
                              </button>
                            </div>
                          </div>
                        ) : (
                          <button
                            type="button"
                            onClick={() => fileRef.current?.click()}
                            className="flex items-center gap-2 rounded-xl border border-dashed border-white/15 px-4 py-3 text-[13px] text-[#8c8780] transition-colors hover:border-white/30 hover:text-[#f5f3f0]"
                          >
                            <ImageIcon size={16} /> Elegir una imagen
                          </button>
                        )}
                      </div>
                    ) : null}
                  </Section>

                  <Section label="Cristal">
                    <SliderRow
                      icon={<Layers size={18} />}
                      title="Opacidad"
                      value={glassOpacity}
                      min={0.2}
                      max={1}
                      format={(value) => `${Math.round(value * 100)}%`}
                      onChange={(value) => setGlass({ glassOpacity: Math.round(value * 100) / 100 })}
                    />
                    <SliderRow
                      title="Desenfoque"
                      value={glassBlur}
                      min={0}
                      max={40}
                      format={(value) => `${Math.round(value)} px`}
                      onChange={(value) => setGlass({ glassBlur: Math.round(value) })}
                    />
                    <SliderRow
                      title="Oscurecimiento"
                      value={glassDim}
                      min={0}
                      max={0.6}
                      format={(value) => `${Math.round(value * 100)}%`}
                      onChange={(value) => setGlass({ glassDim: Math.round(value * 100) / 100 })}
                    />
                  </Section>
                </>
              ) : null}

              {tab === 'playback' ? (
                <>
                  <PageTitle title="Reproducción" subtitle="Controla cómo suena y avanza la música." />

                  <Section label="Reproducción">
                    <Row
                      icon={<Play size={18} />}
                      title="Reproducción automática"
                      description="Continúa con la siguiente canción."
                      control={
                        <Switch
                          checked={autoplayNext}
                          onChange={(value) => setPlayback({ autoplayNext: value })}
                          label="Reproducción automática"
                        />
                      }
                    />
                    <Row
                      icon={<Shuffle size={18} />}
                      title="Aleatorio"
                      description="Reproduce las canciones en orden aleatorio."
                      control={
                        <Switch
                          checked={shuffle}
                          onChange={setShuffle}
                          label="Aleatorio"
                        />
                      }
                    />
                    <Row
                      icon={<RotateCcw size={18} />}
                      title="Recordar última canción"
                      description="Restaura la cola al volver a abrir."
                      control={
                        <Switch
                          checked={restoreLastTrack}
                          onChange={(value) => setPlayback({ restoreLastTrack: value })}
                          label="Recordar última canción"
                        />
                      }
                    />
                  </Section>

                  <Section label="Audio">
                    <SliderRow
                      icon={<Volume2 size={18} />}
                      title="Volumen"
                      value={volume}
                      min={0}
                      max={1}
                      format={(value) => `${Math.round(value * 100)}%`}
                      onChange={setVolume}
                    />
                    <SliderRow
                      icon={<Timer size={18} />}
                      title="Saltar silencios"
                      description="Recorta los silencios entre canciones."
                      value={skipSilence}
                      min={0}
                      max={5}
                      format={(value) => (value === 0 ? 'Desactivado' : `${value.toFixed(1)} s`)}
                      onChange={setSkipSilence}
                    />
                    <SliderRow
                      icon={<SlidersHorizontal size={18} />}
                      title="Crossfade"
                      description="Fundido entre una canción y la siguiente."
                      value={crossfadeSeconds}
                      min={0}
                      max={10}
                      format={(value) => (value === 0 ? 'Desactivado' : `${Math.round(value)} s`)}
                      onChange={(value) => {
                        setCrossfadeSeconds(value)
                        setPlayback({ crossfade: value > 0 })
                      }}
                    />
                  </Section>

                  <Section label="Calidad">
                    <Row
                      icon={<Music2 size={18} />}
                      title="Calidad de audio"
                      description="Mayor calidad utiliza más datos."
                      control={
                        <Segmented
                          value={quality}
                          onChange={setQuality}
                          options={[
                            { id: 'auto', label: 'Auto' },
                            { id: 'low', label: 'Baja' },
                            { id: 'high', label: 'Alta' },
                          ]}
                        />
                      }
                    />
                  </Section>
                </>
              ) : null}

              {tab === 'overlay' ? (
                <>
                  <PageTitle
                    title="Reproductor flotante"
                    subtitle="Mantén el reproductor visible mientras usas otras aplicaciones."
                  />

                  <Section label="Ventana flotante">
                    <Row
                      icon={<PictureInPicture2 size={18} />}
                      title="Mostrar ventana flotante"
                      description="Una pequeña ventana sobre el resto de aplicaciones."
                      control={
                        <Switch
                          checked={overlayVisible}
                          onChange={(value) => {
                            setOverlayVisible(value)
                            window.popDesktop?.toggleOverlay()
                          }}
                          label="Mostrar ventana flotante"
                        />
                      }
                    />
                    <div className="px-4 py-3.5">
                      <p className="text-[13px] leading-relaxed text-[#8c8780]">
                        Usa <Kbd>Ctrl</Kbd> + <Kbd>Alt</Kbd> + <Kbd>P</Kbd> para mostrar u ocultar la
                        ventana, y <Kbd>←</Kbd> / <Kbd>→</Kbd> para cambiar de canción.
                      </p>
                    </div>
                  </Section>
                </>
              ) : null}

              {tab === 'about' ? (
                <>
                  <PageTitle title="Acerca de" subtitle="Información sobre PoP Music." />

                  <Section label="Aplicación">
                    <div className="px-4 py-4">
                      <p className="text-[16px] font-semibold text-[#f5f3f0]">PoP Music</p>
                      <p className="mt-1 text-[12px] text-[#8c8780]">Versión 1.0.0</p>
                      <p className="mt-3 text-[13px] leading-relaxed text-[#8c8780]">
                        Reproductor de música con letras sincronizadas. Sin anuncios.
                      </p>
                    </div>
                  </Section>

                  <Section label="Enlaces">
                    {['Sitio web', 'Privacidad', 'Términos'].map((label) => (
                      <a
                        key={label}
                        href="#"
                        onClick={(event) => event.preventDefault()}
                        className="flex min-h-[52px] items-center gap-3 px-4 py-2 transition-colors hover:bg-white/[0.03]"
                      >
                        <span className="grid w-[18px] shrink-0 place-items-center text-[#8c8780]">
                          <ExternalLink size={18} />
                        </span>
                        <span className="min-w-0 flex-1 text-[14px] font-medium text-[#f5f3f0]">
                          {label}
                        </span>
                        <ChevronRight size={16} className="shrink-0 text-[#8c8780]" />
                      </a>
                    ))}
                  </Section>
                </>
              ) : null}
            </motion.div>
          </AnimatePresence>
        </div>
      </main>
    </div>
  )
}
