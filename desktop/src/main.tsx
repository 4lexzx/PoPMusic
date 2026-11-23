import { createRoot } from 'react-dom/client'
import App from './App'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { PlayerProvider } from '@/features/player/PlayerProvider'
import { withAlbumArt } from '@/shared/api/albumArt'
import { useLibraryStore } from '@/shared/store/libraryStore'
import type { Track } from '@/shared/types/track'
import { watchAppearance } from '@/shared/store/settingsStore'
import { initWasm } from '@/wasm'
import './index.css'

watchAppearance()

/** Sustituye miniaturas de vídeo guardadas por carátulas de álbum reales. */
async function migrateAlbumArt(): Promise<void> {
  const state = useLibraryStore.getState()
  const fix = async (track: Track): Promise<Track> => ({
    ...track,
    thumbnail: await withAlbumArt(track),
  })
  const [favorites, history] = await Promise.all([
    Promise.all(state.favorites.map(fix)),
    Promise.all(state.history.map(fix)),
  ])
  useLibraryStore.setState({ favorites, history })
}

void migrateAlbumArt()

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
