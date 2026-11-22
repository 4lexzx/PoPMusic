import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'
import { Sidebar } from '@/components/Sidebar'
import { TitleBar } from '@/components/TitleBar'
import { DesktopPlayerBar } from '@/features/player/DesktopPlayerBar'
import { QueueSheet } from '@/features/player/QueueSheet'
import { usePlayerStore } from '@/shared/store/playerStore'
import { useSettingsStore } from '@/shared/store/settingsStore'
import { useUiStore } from '@/shared/store/uiStore'

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
    <motion.button
      type="button"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      onClick={dismiss}
      className={`desktop-toast ${message.tone === 'error' ? 'desktop-toast--error' : ''}`}
    >
      {message.text}
    </motion.button>
  )
}

/** Estructura de la aplicación de escritorio: barra de título, lateral y reproductor. */
export function Layout() {
  const location = useLocation()
  const reduceMotion = useReducedMotion()
  const wallpaper = useSettingsStore((state) => state.wallpaper)
  const wallpaperImage = useSettingsStore((state) => state.wallpaperImage)
  const isFullscreenPlayer = location.pathname === '/now-playing'
  const hasTrack = usePlayerStore((state) => state.queue.length > 0)

  return (
    <div className="desktop-shell">
      <WallpaperLayer css={wallpaper} image={wallpaperImage} />
      <TitleBar />
      <div className="desktop-body">
        <Sidebar />
        <main className="desktop-main">
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={location.pathname}
              initial={reduceMotion ? { opacity: 0 } : { opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              exit={reduceMotion ? { opacity: 0 } : { opacity: 0, y: -12 }}
              transition={{ duration: reduceMotion ? 0.12 : 0.3, ease: [0.22, 1, 0.36, 1] }}
              className={`desktop-page ${isFullscreenPlayer ? 'desktop-page--player' : ''}`}
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
      {!isFullscreenPlayer && hasTrack ? <DesktopPlayerBar /> : null}
      <QueueSheet />
      <MessageToast />
    </div>
  )
}
