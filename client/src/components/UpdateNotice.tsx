import { useCallback, useEffect, useState } from 'react'
import { ArrowRight, LoaderCircle } from '../icons'

interface UpdateInfo {
  available: boolean
  version?: string
  notes?: string
  page?: string
  asset?: { name: string; url: string; size: number }
}

const SKIP_KEY = 'popmusic-client-skip-update'
const CHECK_EVERY = 6 * 3600 * 1000

/** Comprueba si hay versión nueva (al abrir y cada 6 h, o a mano desde Ajustes). */
export function useUpdateCheck() {
  const [info, setInfo] = useState<UpdateInfo | null>(null)
  const check = useCallback(async (manual = false) => {
    const res = await window.popApi?.updateCheck?.()
    if (!res) return null
    if (res.available && (manual || localStorage.getItem(SKIP_KEY) !== res.version)) setInfo(res)
    return res
  }, [])
  useEffect(() => {
    if (!window.popApi?.updateCheck) return
    const first = setTimeout(() => void check(), 6000)
    const every = setInterval(() => void check(), CHECK_EVERY)
    const onManual = () => void check(true)
    window.addEventListener('pop-check-update', onManual)
    return () => { clearTimeout(first); clearInterval(every); window.removeEventListener('pop-check-update', onManual) }
  }, [check])
  return { info, dismiss: () => setInfo(null) }
}

/** Aviso de "Nueva actualización disponible", con descarga e instalación. */
export default function UpdateNotice({ info, onClose }: { info: UpdateInfo; onClose: () => void }) {
  const [state, setState] = useState<'idle' | 'downloading' | 'installing' | 'error'>('idle')
  const [pct, setPct] = useState(0)
  const [closing, setClosing] = useState(false)
  const close = () => { setClosing(true); setTimeout(onClose, 160) }

  useEffect(() => window.popApi?.onUpdateProgress?.(({ done, total }) => setPct(total ? Math.round((done / total) * 100) : 0)), [])

  const skip = () => { if (info.version) localStorage.setItem(SKIP_KEY, info.version); close() }
  const update = async () => {
    if (!info.asset) return
    setState('downloading')
    try {
      const file = await window.popApi!.updateDownload!(info.asset.url, info.asset.size)
      setState('installing')
      window.popApi!.updateInstall!(file)
    } catch {
      setState('error')
    }
  }

  return (
    <div className={`modalLayer ${closing ? 'closing' : ''}`}>
      <div className="dialog updateCard" role="dialog" aria-label="Nueva actualización">
        <header>
          <div>
            <h2>Nueva actualización disponible</h2>
            <p>
              Una nueva versión de PoP Music (v{info.version}) está lista para descargar.{' '}
              {info.page && <a onClick={() => window.open(info.page, '_blank')}>Registro de cambios <ArrowRight size={12} /></a>}
            </p>
          </div>
        </header>
        {state === 'downloading' && (
          <div className="importProgress">
            <div className="bar"><i style={{ width: `${Math.max(3, pct)}%` }} /></div>
            <span>Descargando… {pct}%</span>
          </div>
        )}
        {state === 'installing' && <div className="importProgress"><span><LoaderCircle className="spin" size={14} /> Instalando… la app se volverá a abrir sola.</span></div>}
        {state === 'error' && <div className="dialogError">No se pudo descargar la actualización. Inténtalo más tarde.</div>}
        <div className="updateActions">
          <button className="btnGlass" onClick={skip} disabled={state === 'downloading' || state === 'installing'}>Omitir</button>
          <button className="btnPrimary" onClick={update} disabled={state === 'downloading' || state === 'installing'}>
            {state === 'downloading' || state === 'installing' ? <LoaderCircle className="spin" size={15} /> : null}Actualizar
          </button>
        </div>
      </div>
    </div>
  )
}
