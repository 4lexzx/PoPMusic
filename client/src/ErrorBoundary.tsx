import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  error: Error | null
}

/** Evita que un fallo deje la ventana en blanco: muestra un mensaje y permite recargar. */
export default class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[PoP Music] error de interfaz', error, info.componentStack)
  }

  render() {
    if (!this.state.error) return this.props.children
    return (
      <div className="crash">
        <h1>Algo falló</h1>
        <p>{this.state.error.message}</p>
        <button onClick={() => window.location.reload()}>Reiniciar</button>
      </div>
    )
  }
}
