import type { Settings } from '../types'

interface Props {
  settings: Settings
  onChange: (next: Settings) => void
  onClearLibrary: () => void
}

const COUNTRIES = [
  { code: 'us', name: 'Estados Unidos' },
  { code: 'mx', name: 'México' },
  { code: 'es', name: 'España' },
  { code: 'pe', name: 'Perú' },
  { code: 'co', name: 'Colombia' },
  { code: 'ar', name: 'Argentina' },
  { code: 'cl', name: 'Chile' },
]

export default function SettingsView({ settings, onChange, onClearLibrary }: Props) {
  const set = <K extends keyof Settings>(key: K, value: Settings[K]) =>
    onChange({ ...settings, [key]: value })

  return (
    <div className="settingsView">
      <h1>Ajustes</h1>

      <section>
        <h2>Reproducción</h2>
        <div className="settingRow">
          <div><strong>Volumen</strong><small>Se aplica al reproductor</small></div>
          <input
            type="range" min="0" max="1" step="0.01"
            value={settings.volume}
            onChange={(e) => set('volume', Number(e.target.value))}
          />
          <span>{Math.round(settings.volume * 100)}%</span>
        </div>
        <div className="settingRow">
          <div><strong>Reproducción automática</strong><small>Sigue con la cola al terminar</small></div>
          <button
            className={`switch ${settings.autoplay ? 'on' : ''}`}
            onClick={() => set('autoplay', !settings.autoplay)}
          />
        </div>
        <div className="settingRow">
          <div><strong>Transición</strong><small>Fundido entre canciones</small></div>
          <input
            type="range" min="0" max="8" step="1"
            value={settings.crossfade}
            onChange={(e) => set('crossfade', Number(e.target.value))}
          />
          <span>{settings.crossfade} s</span>
        </div>
      </section>

      <section>
        <h2>Catálogo</h2>
        <div className="settingRow">
          <div><strong>País</strong><small>Chart de Descubre y resultados</small></div>
          <select value={settings.country} onChange={(e) => set('country', e.target.value)}>
            {COUNTRIES.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
          </select>
        </div>
      </section>

      <section>
        <h2>Biblioteca</h2>
        <div className="settingRow">
          <div><strong>Borrar datos locales</strong><small>Favoritos, recientes y playlists</small></div>
          <button className="dangerBtn" onClick={onClearLibrary}>Borrar</button>
        </div>
      </section>

      <section>
        <h2>Acerca de</h2>
        <p className="aboutText">
          PoP Music · cliente propio. Música sin videos, con carátulas de álbum reales.
        </p>
      </section>
    </div>
  )
}
