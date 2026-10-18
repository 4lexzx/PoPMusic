import { useEffect, useState } from 'react'
import { StyleSheet, View } from 'react-native'
import { LinearGradient } from 'expo-linear-gradient'
import { Image } from 'expo-image'
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

/** Fondo reactivo: glow del color de la portada sobre negro + viñeta. */
export function DynamicBackground({ image, colors, intensity = 'medium' }: DynamicBackgroundProps) {
  const enabled = useSettingsStore((state) => state.dynamicBackground)
  const opacity = intensity === 'strong' ? 0.7 : intensity === 'soft' ? 0.32 : 0.48

  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      <View style={{ flex: 1, backgroundColor: '#000' }} />
      {enabled ? (
        <>
          <View
            style={{
              position: 'absolute',
              left: '-25%',
              top: '-25%',
              height: '80%',
              width: '80%',
              borderRadius: 9999,
              backgroundColor: colors.primary,
              opacity: opacity * 0.5,
            }}
          />
          <View
            style={{
              position: 'absolute',
              right: '-25%',
              bottom: '-25%',
              height: '80%',
              width: '80%',
              borderRadius: 9999,
              backgroundColor: colors.secondary,
              opacity: opacity * 0.45,
            }}
          />
          {image ? (
            <Image
              source={{ uri: image }}
              style={[StyleSheet.absoluteFill, { opacity: 0.18 }]}
              contentFit="cover"
              blurRadius={40}
            />
          ) : null}
          <LinearGradient
            colors={['rgba(0,0,0,0.3)', 'rgba(0,0,0,0.7)', 'rgba(0,0,0,1)']}
            style={StyleSheet.absoluteFill}
          />
        </>
      ) : (
        <LinearGradient
          colors={['rgba(13,13,15,0.5)', 'rgba(0,0,0,1)']}
          style={StyleSheet.absoluteFill}
        />
      )}
    </View>
  )
}
