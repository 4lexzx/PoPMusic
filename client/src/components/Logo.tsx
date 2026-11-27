import glyph from '../assets/logo-glyph.png'
import wordmark from '../assets/logo-wordmark.png'

const RATIO = { glyph: 558 / 256, wordmark: 586 / 200 }

/**
 * Logo de PoP Music. Se pinta con una máscara para que tome el color de
 * énfasis (o el del texto) y se vea igual de bien en tema claro y oscuro.
 */
export function Logo({ full = false, height = 24, className = '', color }: {
  full?: boolean; height?: number; className?: string; color?: string
}) {
  const src = full ? wordmark : glyph
  const w = Math.round(height * (full ? RATIO.wordmark : RATIO.glyph))
  return (
    <span
      role="img"
      aria-label="PoP Music"
      className={`logo ${className}`}
      style={{
        width: w,
        height,
        background: color || 'var(--logo, var(--accent))',
        WebkitMaskImage: `url(${src})`,
        maskImage: `url(${src})`,
      }}
    />
  )
}
