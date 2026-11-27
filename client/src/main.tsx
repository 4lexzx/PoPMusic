import React from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import Overlay from './components/Overlay'
import ErrorBoundary from './ErrorBoundary'
import './styles.css'

// La superposición en segundo plano carga esta misma página con #overlay.
const isOverlay = window.location.hash === '#overlay'

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      {isOverlay ? <Overlay /> : <App />}
    </ErrorBoundary>
  </React.StrictMode>,
)
