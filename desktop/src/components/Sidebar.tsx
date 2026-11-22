import { NavLink } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  DiscIcon,
  HomeIcon,
  LibraryIcon,
  MicIcon,
  SearchIcon,
  SettingsIcon,
  WaveformIcon,
} from '@/components/Icons'
import { usePlayerStore } from '@/shared/store/playerStore'

const ITEMS = [
  { to: '/', label: 'Inicio', icon: HomeIcon, end: true },
  { to: '/search', label: 'Buscar', icon: SearchIcon },
  { to: '/recognize', label: 'Reconocer', icon: MicIcon },
  { to: '/library', label: 'Biblioteca', icon: LibraryIcon },
]

/** Barra lateral estilo reproductor de escritorio. */
export function Sidebar() {
  const hasTrack = usePlayerStore((state) => state.queue.length > 0)

  return (
    <aside className="sidebar">
      <nav className="sidebar__nav">
        {ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end} className="sidebar__item">
            {({ isActive }) => (
              <>
                {isActive ? (
                  <motion.span
                    layoutId="sidebar-active"
                    className="sidebar__active"
                    transition={{ type: 'spring', stiffness: 480, damping: 38 }}
                  />
                ) : null}
                <Icon size={20} className="sidebar__icon" />
                <span>{label}</span>
              </>
            )}
          </NavLink>
        ))}

        {hasTrack ? (
          <NavLink to="/now-playing" className="sidebar__item">
            {({ isActive }) => (
              <>
                {isActive ? (
                  <motion.span layoutId="sidebar-active" className="sidebar__active" />
                ) : null}
                <WaveformIcon size={20} className="sidebar__icon" />
                <span>Reproduciendo</span>
              </>
            )}
          </NavLink>
        ) : null}
      </nav>

      <div className="sidebar__bottom">
        <NavLink to="/settings" className="sidebar__item">
          {({ isActive }) => (
            <>
              {isActive ? (
                <motion.span layoutId="sidebar-active" className="sidebar__active" />
              ) : null}
              <SettingsIcon size={20} className="sidebar__icon" />
              <span>Ajustes</span>
            </>
          )}
        </NavLink>
        <div className="sidebar__hint">
          <DiscIcon size={15} />
          <span>PoP Music para Windows</span>
        </div>
      </div>
    </aside>
  )
}
