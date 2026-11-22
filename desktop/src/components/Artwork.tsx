import { useState } from 'react'
import { MusicIcon } from './Icons'

interface ArtworkProps {
  src?: string
  alt?: string
  className?: string
  iconSize?: number
  rounded?: string
}

/** Portada con degradado de reserva si la imagen falla o no existe. */
export function Artwork({
  src,
  alt = '',
  className = '',
  iconSize = 24,
  rounded = 'rounded-xl',
}: ArtworkProps) {
  const [failed, setFailed] = useState(false)
  const showImage = src && !failed

  return (
    <div
      className={`relative overflow-hidden bg-surface-2 ${rounded} ${className}`}
      aria-hidden={!alt}
    >
      {showImage ? (
        <img
          src={src}
          alt={alt}
          loading="eager"
          decoding="async"
          referrerPolicy="no-referrer"
          onError={() => setFailed(true)}
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
