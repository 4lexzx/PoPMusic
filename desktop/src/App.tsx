import { HashRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { AuthPage } from '@/pages/AuthPage'
import { HomePage } from '@/pages/HomePage'
import { LibraryPage } from '@/pages/LibraryPage'
import { NowPlayingPage } from '@/pages/NowPlayingPage'
import { OverlayPage } from '@/pages/OverlayPage'
import { PlaylistPage } from '@/pages/PlaylistPage'
import { RecognizePage } from '@/pages/RecognizePage'
import { SearchPage } from '@/pages/SearchPage'
import { SettingsPage } from '@/pages/SettingsPage'

export default function App() {
  return (
    <HashRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/recognize" element={<RecognizePage />} />
          <Route path="/library" element={<LibraryPage />} />
          <Route path="/now-playing" element={<NowPlayingPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/auth" element={<AuthPage />} />
          <Route path="/playlist/:id" element={<PlaylistPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
        <Route path="/overlay" element={<OverlayPage />} />
      </Routes>
    </HashRouter>
  )
}
