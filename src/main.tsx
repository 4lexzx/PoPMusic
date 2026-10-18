import { createRoot } from 'react-dom/client'
import App from './App'
import { PlayerProvider } from '@/features/player/PlayerProvider'
import { watchAppearance } from '@/shared/store/settingsStore'
import './index.css'

watchAppearance()

const container = document.getElementById('root')

if (container) {
  createRoot(container).render(
    <PlayerProvider>
      <App />
    </PlayerProvider>,
  )
}
