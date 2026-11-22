import { NavLink } from 'react-router-dom'
import { motion } from 'framer-motion'
import { HomeIcon, LibraryIcon, SearchIcon, SettingsIcon } from '@/components/Icons'

const ITEMS = [
  { to: '/', label: 'Inicio', icon: HomeIcon, end: true },
  { to: '/search', label: 'Buscar', icon: SearchIcon },
  { to: '/library', label: 'Biblioteca', icon: LibraryIcon },
]

/** Rail de iconos estilo Liner: primordia el contenido, la Chrome desaparece. */
export function Sidebar() {
  return (
    <aside className="rail">
      <div className="rail__brand" title="PoP Music">
        <img src="/logo-glyph.png" alt="PoP Music" className="rail__logo" />
      </div>

      <nav className="rail__nav">
        {ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end} title={label} className="rail__item">
            {({ isActive }) => (
              <>
                {isActive ? (
                  <motion.span
                    layoutId="rail-active"
                    className="rail__active"
                    transition={{ type: 'spring', stiffness: 320, damping: 22 }}
                  />
                ) : null}
                <Icon size={20} strokeWidth={isActive ? 2.1 : 1.7} />
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="rail__bottom">
        <NavLink to="/settings" title="Ajustes" className="rail__item">
          {({ isActive }) => (
            <>
              {isActive ? (
                <motion.span layoutId="rail-active" className="rail__active" />
              ) : null}
              <SettingsIcon size={20} strokeWidth={isActive ? 2.1 : 1.7} />
            </>
          )}
        </NavLink>
      </div>
    </aside>
  )
}
