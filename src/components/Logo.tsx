import { WaveformIcon } from './Icons'

interface LogoProps {
  size?: number
  className?: string
  withWordmark?: boolean
}

/** Marca de PoP Music: glifo de onda con acento de color + wordmark. */
export function Logo({ size = 22, className = '', withWordmark = true }: LogoProps) {
  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <span
        className="grid place-items-center rounded-xl bg-accent/15 text-accent"
        style={{ width: size + 12, height: size + 12 }}
      >
        <WaveformIcon size={size} strokeWidth={2.2} />
      </span>
      {withWordmark ? (
        <span className="text-lg font-semibold tracking-tight">PoP</span>
      ) : null}
    </span>
  )
}
