import { motion, useReducedMotion } from 'framer-motion'
import type { ArtworkColors } from '@/shared/utils/color'

interface ReactiveBackdropProps {
  colors: ArtworkColors
  image?: string
  dim?: number
}

/** Fondo reactivo: manchas de color de la portada flotando sobre el negro. */
export function ReactiveBackdrop({ colors, image, dim = 0.55 }: ReactiveBackdropProps) {
  const reduceMotion = useReducedMotion()
  const float = (x: number, y: number, duration: number) =>
    reduceMotion
      ? {}
      : {
          animate: { x, y, scale: [1, 1.15, 1] },
          transition: {
            duration,
            repeat: Infinity,
            repeatType: 'mirror' as const,
            ease: 'easeInOut' as const,
          },
        }

  return (
    <div className="reactive-backdrop" aria-hidden="true">
      <motion.div
        className="reactive-backdrop__blob"
        style={{ background: colors.primary, left: '-12%', top: '-20%' }}
        {...float(60, 40, 14)}
      />
      <motion.div
        className="reactive-backdrop__blob reactive-backdrop__blob--small"
        style={{ background: colors.secondary, right: '-10%', bottom: '-18%' }}
        {...float(-70, -30, 18)}
      />
      {colors.palette[2] ? (
        <motion.div
          className="reactive-backdrop__blob reactive-backdrop__blob--tiny"
          style={{ background: colors.palette[2], left: '45%', top: '55%' }}
          {...float(40, -50, 22)}
        />
      ) : null}
      {image ? (
        <div
          className="reactive-backdrop__art"
          style={{ backgroundImage: `url(${image})` }}
        />
      ) : null}
      <div className="reactive-backdrop__veil" style={{ opacity: dim }} />
      <div className="reactive-backdrop__grain" />
    </div>
  )
}
