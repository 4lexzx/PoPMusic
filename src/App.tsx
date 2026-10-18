import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { HomePage } from '@/pages/HomePage'
import { LibraryPage } from '@/pages/LibraryPage'
import { NowPlayingPage } from '@/pages/NowPlayingPage'
import { PlaylistPage } from '@/pages/PlaylistPage'
import { RecognizePage } from '@/pages/RecognizePage'
import { SearchPage } from '@/pages/SearchPage'
import { SettingsPage } from '@/pages/SettingsPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/recognize" element={<RecognizePage />} />
          <Route path="/library" element={<LibraryPage />} />
          <Route path="/now-playing" element={<NowPlayingPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/playlist/:id" element={<PlaylistPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
