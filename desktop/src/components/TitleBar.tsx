import { useEffect, useState } from 'react'
import { CloseIcon, MinusIcon, PlusIcon } from '@/components/Icons'

/** Barra de título sin marca: el logo solo vive en el rail lateral. */
export function TitleBar() {
  const [maximized, setMaximized] = useState(false)

  useEffect(() => {
    return window.popDesktop?.onMaximized(setMaximized)
  }, [])

  return (
    <header className="titlebar">
      <div className="titlebar__spacer" />
      <div className="titlebar__controls">
        <button
          type="button"
          aria-label="Minimizar"
          onClick={() => window.popDesktop?.minimize()}
          className="titlebar__button"
        >
          <MinusIcon size={16} />
        </button>
        <button
          type="button"
          aria-label={maximized ? 'Restaurar' : 'Maximizar'}
          onClick={() => window.popDesktop?.maximize()}
          className="titlebar__button"
        >
          {maximized ? <MinusIcon size={14} /> : <PlusIcon size={14} />}
        </button>
        <button
          type="button"
          aria-label="Cerrar"
          onClick={() => window.popDesktop?.close()}
          className="titlebar__button titlebar__button--close"
        >
          <CloseIcon size={15} />
        </button>
      </div>
    </header>
  )
}
