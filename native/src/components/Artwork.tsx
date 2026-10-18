import { useState } from 'react'
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native'
import { Image } from 'expo-image'
import { MusicIcon } from './Icons'
import { useThemeColors } from '@/shared/theme/theme'

interface ArtworkProps {
  src?: string
  className?: string
  style?: StyleProp<ViewStyle>
  iconSize?: number
  radius?: number
}

/** Portada con degradado de reserva si la imagen falla o no existe. */
export function Artwork({ src, className = '', style, iconSize = 24, radius = 12 }: ArtworkProps) {
  const colors = useThemeColors()
  const [failed, setFailed] = useState(false)
  const showImage = Boolean(src) && !failed

  return (
    <View
      className={`overflow-hidden ${className}`}
      style={[{ borderRadius: radius, backgroundColor: colors.surface2 }, style]}
    >
      {showImage ? (
        <Image
          source={{ uri: src }}
          style={StyleSheet.absoluteFill}
          contentFit="cover"
          transition={150}
          onError={() => setFailed(true)}
        />
      ) : (
        <View className="flex-1 items-center justify-center" style={{ backgroundColor: colors.surface3 }}>
          <MusicIcon size={iconSize} color={colors.faint} />
        </View>
      )}
    </View>
  )
}
