import { useEffect } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { HomeIcon, LibraryIcon, MicIcon, SearchIcon } from '@/components/Icons'
import { MiniPlayer } from '@/features/player/MiniPlayer'
import { QueueSheet } from '@/features/player/QueueSheet'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useSettingsStore } from '@/shared/store/settingsStore'
import { useUiStore } from '@/shared/store/uiStore'

interface NavItem {
  to: string
  label: string
  icon: typeof HomeIcon
  end?: boolean
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Inicio', icon: HomeIcon, end: true },
  { to: '/search', label: 'Buscar', icon: SearchIcon },
  { to: '/recognize', label: 'Reconocer', icon: MicIcon },
  { to: '/library', label: 'Biblioteca', icon: LibraryIcon },
]

function BottomNav() {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 pb-[env(safe-area-inset-bottom)]">
      <div className="mx-auto max-w-md px-3 pb-2">
        <div className="glass-panel flex h-[4.25rem] items-stretch rounded-[1.5rem] p-1.5">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className="flex flex-1 flex-col items-center justify-center"
              >
                {({ isActive }) => (
                  <span
                    className={`flex h-full w-full flex-col items-center justify-center gap-1 rounded-2xl text-[10px] font-medium tracking-wide transition-all ${
                      isActive
                        ? 'bg-accent/18 text-accent ring-1 ring-inset ring-accent/30'
                        : 'text-faint'
                    }`}
                  >
                    <Icon
                      size={22}
                      strokeWidth={isActive ? 2.3 : 1.8}
                      className={isActive ? 'drop-shadow-[0_0_10px_currentColor]' : ''}
                    />
                    <span>{item.label}</span>
                  </span>
                )}
              </NavLink>
            )
          })}
        </div>
      </div>
    </nav>
  )
}

function WallpaperLayer({ css, image }: { css: string | null; image: string | null }) {
  if (!css && !image) return null
  return (
    <div className="wallpaper-layer" aria-hidden="true">
      {image ? (
        <div className="wallpaper-layer__image" style={{ backgroundImage: `url(${image})` }} />
      ) : (
        <div className="wallpaper-layer__image" style={{ background: css ?? undefined }} />
      )}
      <div className="wallpaper-layer__dim" />
    </div>
  )
}

function MessageToast() {
  const message = useUiStore((state) => state.message)
  const dismiss = useUiStore((state) => state.dismissMessage)

  useEffect(() => {
    if (!message) return
    const timer = window.setTimeout(dismiss, 4500)
    return () => window.clearTimeout(timer)
  }, [message, dismiss])

  if (!message) return null

  return (
    <div
      className="pointer-events-none fixed inset-x-0 z-[70] flex justify-center px-4"
      style={{ bottom: 'calc(5.75rem + env(safe-area-inset-bottom))' }}
    >
      <button
        type="button"
        onClick={dismiss}
        className={`animate-fade-in pointer-events-auto max-w-md rounded-2xl px-4 py-3 text-left text-sm font-medium shadow-2xl shadow-black/60 ${
          message.tone === 'error' ? 'bg-accent text-white' : 'glass-panel text-ink'
        }`}
      >
        {message.text}
      </button>
    </div>
  )
}

/** Estructura principal: contenido, mini reproductor y navegación inferior. */
export function Layout() {
  const location = useLocation()
  const hasTrack = usePlayerStore((state) => state.queue.length > 0)
  const wallpaper = useSettingsStore((state) => state.wallpaper)
  const wallpaperImage = useSettingsStore((state) => state.wallpaperImage)
  const isNowPlaying = location.pathname === '/now-playing'

  const paddingBottom = isNowPlaying
    ? ''
    : hasTrack
      ? 'pb-[calc(9.75rem+env(safe-area-inset-bottom))]'
      : 'pb-[calc(6rem+env(safe-area-inset-bottom))]'

  const hasWallpaper = Boolean(wallpaper || wallpaperImage)

  return (
    <>
      <WallpaperLayer css={wallpaper} image={wallpaperImage} />
      <div
        className={`relative z-10 mx-auto flex min-h-dvh w-full max-w-md flex-col ${
          hasWallpaper ? '' : 'bg-bg'
        }`}
      >
        <div className="relative z-10 flex flex-1 flex-col">
          <main key={location.pathname} className={`animate-page-in flex-1 ${paddingBottom}`}>
            <Outlet />
          </main>
        </div>

        {!isNowPlaying && hasTrack ? <MiniPlayer /> : null}
        {!isNowPlaying ? <BottomNav /> : null}
        <QueueSheet />
        <MessageToast />
      </div>
    </>
  )
}
