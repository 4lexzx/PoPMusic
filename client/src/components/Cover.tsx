import { useState } from 'react'
import { Heart, Music2 } from '../icons'
import { hd } from '../api'
import { useArtQuality } from '../ui'

interface Props {
  src?: string
  size?: number
  className?: string
  alt?: string
  /** Pide la carátula en alta resolución (por defecto sí). */
  sharp?: boolean
}

/** Carátula con fundido al cargar y respaldo si falla. */
export function Cover({ src, size, className = '', alt = '', sharp = true }: Props) {
  const quality = useArtQuality()
  const [loaded, setLoaded] = useState(false)
  const [failed, setFailed] = useState(false)
  // Las miniaturas pequeñas no necesitan 1400 px: se limita al doble del tamaño visible.
  const px = size ? Math.min(quality, Math.max(200, Math.round(size * 2 / 100) * 100)) : quality
  const url = sharp ? hd(src, px) : src
  const style = size ? { width: size, height: size } : undefined

  if (!src || failed) {
    return <div className={`cover cover-empty ${className}`} style={style}><Music2 size={(size || 120) / 3.2} /></div>
  }
  return (
    <div className={`cover ${loaded ? 'is-loaded' : ''} ${className}`} style={style}>
      <img
        src={url}
        alt={alt}
        loading="lazy"
        decoding="async"
        draggable={false}
        onLoad={() => setLoaded(true)}
        onError={() => setFailed(true)}
      />
    </div>
  )
}

/** Portada de 4 carátulas para playlists con varias canciones. */
export function Collage({ covers, size }: { covers: string[]; size?: number }) {
  const unique = [...new Set(covers.filter(Boolean))]
  if (unique.length < 4) return <Cover src={unique[0]} size={size} />
  return (
    <div className="cover collage" style={size ? { width: size, height: size } : undefined}>
      {unique.slice(0, 4).map((c) => <Cover key={c} src={c} size={size ? size / 2 : 160} />)}
    </div>
  )
}

/** Portada generada para "Me gusta": degradado con brillo y un corazón. */
export function LikedCover({ size }: { size?: number }) {
  return (
    <div className="cover likedCover" style={size ? { width: size, height: size } : undefined}>
      <span className="glow g1" /><span className="glow g2" /><span className="glow g3" />
      <Heart size={(size || 160) * 0.34} fill="currentColor" strokeWidth={0} />
    </div>
  )
}
