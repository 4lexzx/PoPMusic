import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Search, X, Play, Keyboard, PictureInPicture, Headphones, MicVocal, Palette, Paintbrush, HardDrive, Info, Check, Pipette,
} from '../icons'
import type { ArtQuality, LyricsEffect, Settings, ThemeMode } from '../types'
import { EFFECTS } from './FullPlayer'
import { ACCENTS, hexToRgb, resolvedMode } from '../theme'
import { RECENT_KEY } from '../search'
import { Logo } from './Logo'

interface Props {
  settings: Settings
  onChange: (next: Settings) => void
  onClose: () => void
  onClearLibrary: () => void
  onClearRecent: () => void
  stats: { favorites: number; recent: number; playlists: number }
}

interface Row { title: string; hint?: string; control: ReactNode; keywords?: string; block?: boolean }
interface SectionDef { id: string; group: string; label: string; icon: ReactNode; desc: string; rows: Row[] }

const COUNTRIES = [
  { code: 'us', name: 'Estados Unidos' }, { code: 'mx', name: 'México' }, { code: 'es', name: 'España' },
  { code: 'pe', name: 'Perú' }, { code: 'co', name: 'Colombia' }, { code: 'ar', name: 'Argentina' },
  { code: 'cl', name: 'Chile' }, { code: 'ec', name: 'Ecuador' }, { code: 'gb', name: 'Reino Unido' },
  { code: 'br', name: 'Brasil' }, { code: 'fr', name: 'Francia' }, { code: 'de', name: 'Alemania' },
]

function Switch({ on, onToggle }: { on: boolean; onToggle: () => void }) {
  return <button role="switch" aria-checked={on} className={`switch ${on ? 'on' : ''}`} onClick={onToggle}><i /></button>
}

function Segment<T extends string | number>({ value, options, onChange }: {
  value: T; options: [T, string][]; onChange: (v: T) => void
}) {
  return (
    <div className="segGroup labeled">
      {options.map(([v, l]) => <button key={String(v)} className={value === v ? 'on' : ''} onClick={() => onChange(v)}>{l}</button>)}
    </div>
  )
}

function Slider({ value, min, max, step, onChange, label }: {
  value: number; min: number; max: number; step: number; onChange: (v: number) => void; label: string
}) {
  return (
    <div className="setSlider">
      <input type="range" min={min} max={max} step={step} value={value}
        style={{ '--v': `${((value - min) / (max - min)) * 100}%` } as React.CSSProperties}
        onChange={(e) => onChange(Number(e.target.value))} />
      <span>{label}</span>
    </div>
  )
}

/** Miniatura de la interfaz para elegir tema. */
function ThemePreview({ mode }: { mode: 'light' | 'dark' | 'split' }) {
  const pane = (m: 'light' | 'dark') => (
    <div className={`tpPane ${m}`}>
      <div className="tpSide"><i className="dots" /><i /><i /><i /><b /></div>
      <div className="tpMain"><i className="bar" /><div className="tpGrid"><i /><i /><i /><i /></div></div>
    </div>
  )
  return <div className="themePreview">{mode === 'split' ? <>{pane('light')}{pane('dark')}</> : pane(mode)}</div>
}

function formatBytes(n: number): string {
  if (n < 1024 * 1024) return `${Math.round(n / 1024)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

export default function SettingsModal({ settings, onChange, onClose, onClearLibrary, onClearRecent, stats }: Props) {
  const set = <K extends keyof Settings>(key: K, value: Settings[K]) => onChange({ ...settings, [key]: value })
  const [active, setActive] = useState('playback')
  const [query, setQuery] = useState('')
  const [closing, setClosing] = useState(false)
  const [devices, setDevices] = useState<MediaDeviceInfo[]>([])
  const [cacheSize, setCacheSize] = useState<number | null>(null)
  const panel = useRef<HTMLDivElement>(null)
  const close = () => { setClosing(true); setTimeout(onClose, 180) }

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  })

  useEffect(() => {
    navigator.mediaDevices?.enumerateDevices?.()
      .then((list) => setDevices(list.filter((d) => d.kind === 'audiooutput' && d.deviceId !== 'default' && d.deviceId !== 'communications')))
      .catch(() => {})
    window.popApi?.cache?.('size').then(setCacheSize).catch(() => {})
  }, [])

  const custom = settings.accent && !ACCENTS.some((a) => a.value === settings.accent)

  const sections: SectionDef[] = useMemo(() => [
    {
      id: 'playback', group: 'Preferencias', label: 'Reproducción', icon: <Play size={17} />,
      desc: 'Comportamiento de la cola, acciones de clic y transiciones.',
      rows: [
        { title: 'Reproducir canciones similares automáticamente', hint: 'Cuando la cola se acaba, sigue con música parecida sin parar.', keywords: 'radio autoplay infinito',
          control: <Switch on={settings.autoRadio} onToggle={() => set('autoRadio', !settings.autoRadio)} /> },
        { title: 'Doble clic en una canción', hint: 'Qué pasa al hacer doble clic en una canción o tarjeta.',
          control: <Segment value={settings.doubleClick} onChange={(v) => set('doubleClick', v)} options={[['play', 'Reproducir al instante'], ['queue', 'Añadir a la cola']]} /> },
        { title: 'Al abrir la aplicación', hint: 'Retoma la última cola o empieza sin nada.', keywords: 'inicio arranque',
          control: <Segment value={settings.startup} onChange={(v) => set('startup', v)} options={[['resume', 'Reanudar última cola'], ['empty', 'Iniciar vacío']]} /> },
        { title: 'Seguir con la cola', hint: 'Al terminar una canción pasa a la siguiente.', keywords: 'autoplay',
          control: <Switch on={settings.autoplay} onToggle={() => set('autoplay', !settings.autoplay)} /> },
        { title: 'Transición entre canciones', hint: 'Fundido de salida al final de cada canción.', keywords: 'crossfade fundido',
          control: <Slider value={settings.crossfade} min={0} max={8} step={1} onChange={(v) => set('crossfade', v)} label={settings.crossfade ? `${settings.crossfade} s` : 'No'} /> },
        { title: 'País del catálogo', hint: 'Top del inicio y resultados.', keywords: 'region',
          control: (
            <select className="select" value={settings.country} onChange={(e) => set('country', e.target.value)}>
              {COUNTRIES.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
            </select>
          ) },
      ],
    },
    {
      id: 'audio', group: 'Preferencias', label: 'Audio', icon: <Headphones size={17} />,
      desc: 'Volumen y dispositivo de salida.',
      rows: [
        { title: 'Volumen', control: <Slider value={settings.volume} min={0} max={1} step={0.01} onChange={(v) => set('volume', v)} label={`${Math.round(settings.volume * 100)}%`} /> },
        { title: 'Dispositivo de salida', hint: 'Por dónde suena la música.', keywords: 'altavoz auriculares salida',
          control: (
            <select className="select" value={settings.outputDevice} onChange={(e) => set('outputDevice', e.target.value)}>
              <option value="">Predeterminado del sistema</option>
              {devices.map((d, i) => <option key={d.deviceId} value={d.deviceId}>{d.label || `Salida ${i + 1}`}</option>)}
            </select>
          ) },
      ],
    },
    {
      id: 'overlay', group: 'Preferencias', label: 'Superposición', icon: <PictureInPicture size={17} />,
      desc: 'Una píldora flotante con la canción y sus controles, encima de cualquier ventana.',
      rows: [
        { title: 'Activar superposición', hint: 'Se muestra y oculta también con Alt+Shift+O.', keywords: 'overlay flotante segundo plano',
          control: <Switch on={settings.overlay} onToggle={() => set('overlay', !settings.overlay)} /> },
        { title: 'Mostrar al minimizar', hint: 'Aparece sola cuando minimizas la app y algo está sonando.',
          control: <Switch on={settings.overlayOnMinimize} onToggle={() => set('overlayOnMinimize', !settings.overlayOnMinimize)} /> },
        { title: 'Posición', hint: 'Parte superior de la pantalla.',
          control: <Segment value={settings.overlayAnchor} onChange={(v) => set('overlayAnchor', v)} options={[['left', 'Izquierda'], ['center', 'Centro'], ['right', 'Derecha']]} /> },
      ],
    },
    {
      id: 'lyrics', group: 'Preferencias', label: 'Letras', icon: <MicVocal size={17} />,
      desc: 'Cómo se ve la letra sincronizada.',
      rows: [
        { title: 'Efecto', hint: 'Cómo se anima la letra mientras suena (también desde el botón de la pantalla de letras).', keywords: 'karaoke animacion palabra',
          control: (
            <select className="select" value={settings.lyricsEffect || 'karaoke'} onChange={(e) => set('lyricsEffect', e.target.value as LyricsEffect)}>
              {EFFECTS.map((fx) => <option key={fx.id} value={fx.id}>{fx.name}</option>)}
            </select>
          ) },
        { title: 'Tamaño del texto', control: <Segment value={settings.lyricsSize} onChange={(v) => set('lyricsSize', v)} options={[['m', 'Mediano'], ['l', 'Grande'], ['xl', 'Enorme']]} /> },
        { title: 'Desenfocar líneas lejanas', hint: 'Resalta la línea que está sonando.',
          control: <Switch on={settings.lyricsBlur} onToggle={() => set('lyricsBlur', !settings.lyricsBlur)} /> },
        { title: 'Sincronización', hint: 'Adelanta o atrasa la letra si no coincide con el audio.',
          control: <Slider value={settings.lyricsOffset} min={-3000} max={3000} step={250} onChange={(v) => set('lyricsOffset', v)} label={`${settings.lyricsOffset > 0 ? '+' : ''}${(settings.lyricsOffset / 1000).toFixed(2)} s`} /> },
      ],
    },
    {
      id: 'shortcuts', group: 'Preferencias', label: 'Atajos', icon: <Keyboard size={17} />,
      desc: 'Teclas para controlar la música sin el ratón.',
      rows: [
        { title: 'Reproducir / pausar', control: <kbd>Espacio</kbd> },
        { title: 'Siguiente canción', control: <span className="kbds"><kbd>Ctrl</kbd><kbd>→</kbd></span> },
        { title: 'Canción anterior', control: <span className="kbds"><kbd>Ctrl</kbd><kbd>←</kbd></span> },
        { title: 'Buscar', control: <span className="kbds"><kbd>Ctrl</kbd><kbd>K</kbd> o <kbd>/</kbd></span> },
        { title: 'Letras a pantalla completa', control: <kbd>L</kbd> },
        { title: 'Atrás', control: <span className="kbds"><kbd>Alt</kbd><kbd>←</kbd></span> },
      ],
    },
    {
      id: 'appearance', group: 'Preferencias', label: 'Apariencia', icon: <Palette size={17} />,
      desc: 'Tema y color de énfasis. Los textos se ajustan solos para leerse siempre bien.',
      rows: [
        { title: 'Tema', hint: 'Elige la apariencia general de la aplicación.', block: true, keywords: 'claro oscuro sistema',
          control: (
            <div className="themeCards">
              {([['system', 'Sistema', 'split'], ['light', 'Claro', 'light'], ['dark', 'Oscuro', 'dark']] as [ThemeMode, string, 'split' | 'light' | 'dark'][]).map(([v, l, m]) => (
                <button key={v} className={`themeCard ${settings.theme === v ? 'on' : ''}`} onClick={() => set('theme', v)}>
                  <ThemePreview mode={m} />
                  {settings.theme === v && <span className="themeCheck"><Check size={13} strokeWidth={3} /></span>}
                  <span>{l}</span>
                </button>
              ))}
            </div>
          ) },
        { title: 'Color de énfasis', hint: 'Para el logo, botones principales e indicadores.', block: true, keywords: 'color acento',
          control: (
            <div className="swatches">
              {ACCENTS.map((a) => (
                <button key={a.name} className={`swatch ${settings.accent === a.value ? 'on' : ''}`} onClick={() => set('accent', a.value)}>
                  <Logo height={15} color={a.value || 'var(--solid)'} />
                  {a.name}
                </button>
              ))}
              <label className={`swatch ${custom ? 'on' : ''}`} title="Elige cualquier color">
                <Pipette size={15} style={custom ? { color: settings.accent } : undefined} />
                Personalizado
                <input type="color" value={hexToRgb(settings.accent) ? settings.accent : '#8a5cff'} onChange={(e) => set('accent', e.target.value)} />
              </label>
            </div>
          ) },
      ],
    },
    {
      id: 'custom', group: 'Preferencias', label: 'Personalización', icon: <Paintbrush size={17} />,
      desc: 'Fondo, carátulas y movimiento.',
      rows: [
        { title: 'Color de la portada en el fondo', hint: 'Cuánto tiñe la carátula que suena el fondo de la app.', keywords: 'tinte',
          control: <Slider value={settings.tint} min={0} max={1} step={0.05} onChange={(v) => set('tint', v)} label={`${Math.round(settings.tint * 100)}%`} /> },
        { title: 'Calidad de las carátulas', hint: 'Más resolución se ve más nítida pero usa más datos.', keywords: 'hd imagen',
          control: <Segment<ArtQuality> value={settings.artQuality} onChange={(v) => set('artQuality', v)} options={[[600, 'Normal'], [1000, 'HD'], [1400, 'Máxima']]} /> },
        { title: 'Reducir animaciones', hint: 'Quita movimientos y desenfoques animados.',
          control: <Switch on={settings.reduceMotion} onToggle={() => set('reduceMotion', !settings.reduceMotion)} /> },
      ],
    },
    {
      id: 'storage', group: 'Aplicación', label: 'Caché y almacenamiento', icon: <HardDrive size={17} />,
      desc: 'Datos guardados en este equipo.',
      rows: [
        { title: 'Caché de imágenes y audio', hint: cacheSize === null ? 'Solo en la app de escritorio.' : `Ocupa ${formatBytes(cacheSize)}.`,
          control: (
            <button className="btnGlass" disabled={cacheSize === null} onClick={async () => {
              await window.popApi?.cache?.('clear'); setCacheSize(0)
            }}>Vaciar</button>
          ) },
        { title: 'Historial de escucha', hint: `${stats.recent} canciones. Se usa para tus recomendaciones.`,
          control: <button className="btnGlass" onClick={onClearRecent}>Borrar</button> },
        { title: 'Búsquedas recientes', control: <button className="btnGlass" onClick={() => localStorage.removeItem(RECENT_KEY)}>Borrar</button> },
        { title: 'Biblioteca local', hint: `${stats.favorites} me gusta · ${stats.playlists} playlists`,
          control: (
            <button className="btnGlass danger" onClick={() => {
              if (window.confirm('¿Borrar toda la biblioteca local? No se puede deshacer.')) onClearLibrary()
            }}>Borrar todo</button>
          ) },
      ],
    },
    {
      id: 'about', group: 'Aplicación', label: 'Acerca de', icon: <Info size={17} />,
      desc: 'PoP Music para PC.',
      rows: [
        { title: '', block: true, control: (
          <div className="aboutCard">
            <Logo full height={46} />
            <div>
              <strong>PoP Music para PC · v0.3.0</strong>
              <small>Música sin anuncios, letras sincronizadas y carátulas en HD.</small>
              <small>Catálogo: Deezer e iTunes · Letras: LrcLib · Audio: yt-dlp</small>
            </div>
          </div>
        ) },
      ],
    },
  // eslint-disable-next-line react-hooks/exhaustive-deps
  ], [settings, devices, cacheSize, stats, custom])

  const q = query.trim().toLowerCase()
  const matches = q
    ? sections.map((s) => ({ ...s, rows: s.rows.filter((r) => `${r.title} ${r.hint || ''} ${r.keywords || ''} ${s.label}`.toLowerCase().includes(q)) }))
      .filter((s) => s.rows.length)
    : sections.filter((s) => s.id === active)

  const groups = [...new Set(sections.map((s) => s.group))]

  return (
    <div className={`modalLayer ${closing ? 'closing' : ''}`} onMouseDown={(e) => { if (e.target === e.currentTarget) close() }}>
      <div className="settingsModal" role="dialog" aria-label="Ajustes">
        <nav className="setNav">
          <label className="miniSearch setSearch">
            <Search size={14} />
            <input autoFocus value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Buscar ajustes..." />
            {query && <button className="iconBtn" onClick={() => setQuery('')}><X size={13} /></button>}
          </label>
          {groups.map((g) => (
            <div key={g} className="setGroup">
              <small>{g}</small>
              {sections.filter((s) => s.group === g).map((s) => (
                <button key={s.id} className={!q && active === s.id ? 'on' : ''} onClick={() => { setQuery(''); setActive(s.id); panel.current?.scrollTo({ top: 0 }) }}>
                  {s.icon}{s.label}
                </button>
              ))}
            </div>
          ))}
          <div className="setFooter">
            <Logo height={14} />
            <span>Tema {resolvedMode(settings.theme) === 'light' ? 'claro' : 'oscuro'}</span>
          </div>
        </nav>

        <div className="setBody" ref={panel}>
          <button className="iconBtn setClose" onClick={close} title="Cerrar (Esc)"><X size={18} /></button>
          {matches.length === 0 && <div className="emptyState"><p>Ningún ajuste coincide con “{query}”.</p></div>}
          {matches.map((s) => (
            <section key={s.id} className="setPage">
              <h2>{s.label}</h2>
              <p className="setDesc">{s.desc}</p>
              {s.rows.map((r, i) => (
                <div key={`${s.id}-${i}`} className={`setRow ${r.block ? 'block' : ''}`}>
                  {r.title && <div className="setText"><strong>{r.title}</strong>{r.hint && <small>{r.hint}</small>}</div>}
                  <div className="setControl">{r.control}</div>
                </div>
              ))}
            </section>
          ))}
        </div>
      </div>
    </div>
  )
}
