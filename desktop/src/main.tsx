import { createRoot } from 'react-dom/client'
import App from './App'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { PlayerProvider } from '@/features/player/PlayerProvider'
import { watchAppearance } from '@/shared/store/settingsStore'
import { initWasm } from '@/wasm'
import './index.css'

watchAppearance()

const container = document.getElementById('root')

if (container) {
  initWasm().finally(() => {
    createRoot(container).render(
      <AuthProvider>
        <PlayerProvider>
          <App />
        </PlayerProvider>
      </AuthProvider>,
    )
  })
}
