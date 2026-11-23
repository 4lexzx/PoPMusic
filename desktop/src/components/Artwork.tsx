import { useEffect, useState } from 'react'
import { fallbackThumbnail } from '@/shared/utils/thumbnails'
import { MusicIcon } from './Icons'

interface ArtworkProps {
  src?: string
  alt?: string
  className?: string
  iconSize?: number
  rounded?: string
}

/** Portada limpia: usa maxres y baja a hq solo si la imagen falla. */
export function Artwork({
  src,
  alt = '',
  className = '',
  iconSize = 24,
  rounded = 'rounded-xl',
}: ArtworkProps) {
  const [source, setSource] = useState(src ?? '')
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    setSource(src ?? '')
    setFailed(false)
  }, [src])

  const handleError = () => {
    const fallback = fallbackThumbnail(source)
    if (fallback) {
      setSource(fallback)
      return
    }
    setFailed(true)
  }

  const handleLoad = (event: React.SyntheticEvent<HTMLImageElement>) => {
    const image = event.currentTarget
    // YouTube devuelve una imagen gris de reserva cuando no hay HD:
    // se detecta por su tamaño y se baja a una miniatura válida.
    if (
      (source.includes('hq720') || source.includes('maxresdefault')) &&
      image.naturalWidth < 600
    ) {
      setSource(source.replace(/(hq720|maxresdefault)/, 'mqdefault'))
    }
  }

  const showImage = source && !failed

  return (
    <div
      className={`relative overflow-hidden bg-surface-2 ${rounded} ${className}`}
      aria-hidden={!alt}
    >
      {showImage ? (
        <img
          src={source}
          alt={alt}
          loading="eager"
          decoding="async"
          referrerPolicy="no-referrer"
          onError={handleError}
          onLoad={handleLoad}
          className="h-full w-full object-cover"
        />
      ) : (
        <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-surface-3 via-surface-2 to-bg text-faint">
          <MusicIcon size={iconSize} />
        </div>
      )}
    </div>
  )
}
