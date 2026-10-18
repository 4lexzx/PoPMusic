import { Text, View, type StyleProp, type ViewStyle } from 'react-native'
import { WaveformIcon } from './Icons'
import { useThemeColors } from '@/shared/theme/theme'

interface LogoProps {
  size?: number
  withWordmark?: boolean
  style?: StyleProp<ViewStyle>
}

/** Marca de PoP Music: glifo de onda con acento de color + wordmark. */
export function Logo({ size = 22, withWordmark = true, style }: LogoProps) {
  const colors = useThemeColors()
  return (
    <View className="flex-row items-center gap-2" style={style}>
      <View
        className="items-center justify-center"
        style={{
          width: size + 12,
          height: size + 12,
          borderRadius: 12,
          backgroundColor: colors.accent + '26',
        }}
      >
        <WaveformIcon size={size} strokeWidth={2.2} color={colors.accent} />
      </View>
      {withWordmark ? (
        <Text className="text-lg font-semibold tracking-tight" style={{ color: colors.ink }}>
          PoP
        </Text>
      ) : null}
    </View>
  )
}
