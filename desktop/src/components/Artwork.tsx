import { useEffect, useState } from 'react'
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
    if (source.includes('mqdefault')) {
      setSource(source.replace('mqdefault', 'hqdefault'))
      return
    }
    setFailed(true)
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
