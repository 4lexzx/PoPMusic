import { useEffect, useState } from 'react'
import {
  DEFAULT_COLORS,
  extractColorsFromImage,
  type ArtworkColors,
} from '@/shared/utils/color'
import { useSettingsStore } from '@/shared/store/settingsStore'

/** Extrae la paleta dominante de la portada y expone un fallback estable. */
export function useArtworkColors(url: string | undefined): ArtworkColors {
  const [colors, setColors] = useState<ArtworkColors>(DEFAULT_COLORS)

  useEffect(() => {
    if (!url) {
      setColors(DEFAULT_COLORS)
      return
    }

    let cancelled = false
    extractColorsFromImage(url)
      .then((result) => {
        if (!cancelled) setColors(result)
      })
      .catch(() => {
        if (!cancelled) setColors(DEFAULT_COLORS)
      })

    return () => {
      cancelled = true
    }
  }, [url])

  return colors
}

interface DynamicBackgroundProps {
  image?: string
  colors: ArtworkColors
  intensity?: 'soft' | 'medium' | 'strong'
}

/** Fondo reactivo: glow radial del color de la portada sobre negro + viñeta. */
export function DynamicBackground({ image, colors, intensity = 'medium' }: DynamicBackgroundProps) {
  const enabled = useSettingsStore((state) => state.dynamicBackground)
  const opacity = intensity === 'strong' ? 0.7 : intensity === 'soft' ? 0.32 : 0.48

  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden bg-bg">
      {enabled ? (
        <>
          <div
            className="glow-a absolute -left-1/4 -top-1/4 h-[80%] w-[80%] rounded-full blur-[110px]"
            style={{ background: colors.primary, opacity }}
          />
          <div
            className="glow-b absolute -bottom-1/4 -right-1/4 h-[80%] w-[80%] rounded-full blur-[130px]"
            style={{ background: colors.secondary, opacity: opacity * 0.9 }}
          />
          {image ? (
            <div
              className="absolute inset-0 scale-150 opacity-[0.18] blur-3xl"
              style={{
                backgroundImage: `url(${image})`,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
              }}
            />
          ) : null}
          <div className="absolute inset-0 bg-gradient-to-b from-bg/30 via-bg/70 to-bg" />
        </>
      ) : (
        <div className="absolute inset-0 bg-gradient-to-b from-surface/50 to-bg" />
      )}
    </div>
  )
}
