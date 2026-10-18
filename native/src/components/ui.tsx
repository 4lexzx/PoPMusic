import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import {
  Animated,
  Modal,
  PanResponder,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  type StyleProp,
  type ViewStyle,
} from 'react-native'
import { CloseIcon, SpinnerIcon } from '@/components/Icons'
import { useThemeColors } from '@/shared/theme/theme'
import { clamp } from '@/shared/utils/time'

/** En web había un Portal; en nativo los overlays usan Modal (sin necesidad de él). */
export function Portal({ children }: { children: ReactNode }) {
  return <>{children}</>
}

/* ==========================================================================
   Button
   ========================================================================== */
type ButtonVariant = 'primary' | 'ghost' | 'subtle' | 'danger'
type ButtonSize = 'sm' | 'md' | 'lg'

interface ButtonProps {
  variant?: ButtonVariant
  size?: ButtonSize
  fullWidth?: boolean
  loading?: boolean
  icon?: ReactNode
  onPress?: () => void
  disabled?: boolean
  style?: StyleProp<ViewStyle>
  children?: ReactNode
}

const BUTTON_HEIGHT: Record<ButtonSize, number> = { sm: 36, md: 44, lg: 52 }

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth,
  loading,
  icon,
  onPress,
  disabled,
  style,
  children,
}: ButtonProps) {
  const colors = useThemeColors()
  const isDisabled = disabled || loading

  const background =
    variant === 'primary'
      ? colors.primary
      : variant === 'subtle'
        ? colors.overlay2
        : variant === 'danger'
          ? colors.accent + '26'
          : 'transparent'
  const textColor =
    variant === 'primary'
      ? colors.primaryInk
      : variant === 'danger'
        ? colors.accent
        : colors.ink

  return (
    <Pressable
      onPress={isDisabled ? undefined : onPress}
      disabled={isDisabled}
      className="flex-row items-center justify-center gap-2 rounded-2xl"
      style={[
        {
          height: BUTTON_HEIGHT[size],
          paddingHorizontal: size === 'sm' ? 14 : 18,
          backgroundColor: background,
          opacity: isDisabled ? 0.4 : 1,
        },
        fullWidth ? { alignSelf: 'stretch' } : null,
        style,
      ]}
    >
      {loading ? (
        <Spinner size={18} />
      ) : (
        icon
      )}
      {typeof children === 'string' ? (
        <Text style={{ color: textColor, fontWeight: '600', fontSize: size === 'lg' ? 16 : 14 }}>
          {children}
        </Text>
      ) : (
        children
      )}
    </Pressable>
  )
}

/* ==========================================================================
   IconButton
   ========================================================================== */
interface IconButtonProps {
  label: string
  active?: boolean
  size?: 'sm' | 'md' | 'lg'
  onPress?: () => void
  disabled?: boolean
  style?: StyleProp<ViewStyle>
  children: ReactNode
}

export function IconButton({
  label,
  active,
  size = 'md',
  onPress,
  disabled,
  style,
  children,
}: IconButtonProps) {
  const colors = useThemeColors()
  const dimension = size === 'sm' ? 36 : size === 'lg' ? 56 : 44
  return (
    <Pressable
      accessibilityLabel={label}
      onPress={disabled ? undefined : onPress}
      disabled={disabled}
      className="items-center justify-center rounded-full"
      style={[
        {
          width: dimension,
          height: dimension,
          backgroundColor: active ? colors.accent + '26' : 'transparent',
          opacity: disabled ? 0.4 : 1,
        },
        style,
      ]}
    >
      {children}
    </Pressable>
  )
}

/* ==========================================================================
   Reveal (aparición al montar)
   ========================================================================== */
export function Reveal({
  children,
  delay = 0,
  style,
}: {
  children: ReactNode
  delay?: number
  style?: StyleProp<ViewStyle>
}) {
  const anim = useRef(new Animated.Value(0)).current
  useEffect(() => {
    Animated.timing(anim, {
      toValue: 1,
      duration: 500,
      delay,
      useNativeDriver: true,
    }).start()
  }, [anim, delay])

  return (
    <Animated.View
      style={[
        {
          opacity: anim,
          transform: [{ translateY: anim.interpolate({ inputRange: [0, 1], outputRange: [14, 0] }) }],
        },
        style,
      ]}
    >
      {children}
    </Animated.View>
  )
}

/* ==========================================================================
   Indicador de reproducción (barras animadas)
   ========================================================================== */
export function PlayingBars({ color, size = 14 }: { color?: string; size?: number }) {
  const colors = useThemeColors()
  const bars = [0, 1, 2, 3]
  const values = useRef(bars.map(() => new Animated.Value(0.4))).current

  useEffect(() => {
    const animations = values.map((value, index) =>
      Animated.loop(
        Animated.sequence([
          Animated.timing(value, {
            toValue: 1,
            duration: 450,
            delay: index * 120,
            useNativeDriver: true,
          }),
          Animated.timing(value, {
            toValue: 0.4,
            duration: 450,
            useNativeDriver: true,
          }),
        ]),
      ),
    )
    animations.forEach((animation) => animation.start())
    return () => animations.forEach((animation) => animation.stop())
  }, [values])

  return (
    <View className="flex-row items-end gap-[2px]" style={{ height: size }}>
      {bars.map((bar) => (
        <Animated.View
          key={bar}
          style={{
            width: 3,
            height: '100%',
            borderRadius: 999,
            backgroundColor: color ?? colors.accent,
            transform: [{ scaleY: values[bar] }],
          }}
        />
      ))}
    </View>
  )
}

/* ==========================================================================
   Toggle (interruptor)
   ========================================================================== */
export function Toggle({
  checked,
  onChange,
  label,
}: {
  checked: boolean
  onChange: (value: boolean) => void
  label?: string
}) {
  const colors = useThemeColors()
  const anim = useRef(new Animated.Value(checked ? 1 : 0)).current

  useEffect(() => {
    Animated.timing(anim, {
      toValue: checked ? 1 : 0,
      duration: 200,
      useNativeDriver: false,
    }).start()
  }, [anim, checked])

  return (
    <Pressable
      accessibilityRole="switch"
      accessibilityLabel={label}
      accessibilityState={{ checked }}
      onPress={() => onChange(!checked)}
      style={{
        width: 48,
        height: 28,
        borderRadius: 999,
        backgroundColor: checked ? colors.accent : colors.overlay3,
        padding: 4,
      }}
    >
      <Animated.View
        style={{
          width: 20,
          height: 20,
          borderRadius: 999,
          backgroundColor: checked ? colors.accentInk : '#ffffff',
          transform: [
            { translateX: anim.interpolate({ inputRange: [0, 1], outputRange: [0, 20] }) },
          ],
        }}
      />
    </Pressable>
  )
}

/* ==========================================================================
   Slider
   ========================================================================== */
interface SliderProps {
  value: number
  onChange: (value: number) => void
  onCommit?: (value: number) => void
  ariaLabel?: string
  trackColor?: string
  fillColor?: string
  thumbColor?: string
  showThumb?: boolean
  trackHeight?: number
}

export function Slider({
  value,
  onChange,
  onCommit,
  trackColor,
  fillColor,
  thumbColor,
  showThumb = true,
  trackHeight = 6,
}: SliderProps) {
  const colors = useThemeColors()
  const [width, setWidth] = useState(0)
  const widthRef = useRef(0)
  const [dragging, setDragging] = useState(false)
  const valueRef = useRef(value)
  valueRef.current = value

  const responder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: () => true,
        onPanResponderGrant: (event) => {
          setDragging(true)
          if (widthRef.current > 0) {
            onChange(clamp(event.nativeEvent.locationX / widthRef.current, 0, 1))
          }
        },
        onPanResponderMove: (event, gesture) => {
          if (widthRef.current > 0) {
            onChange(clamp((gesture.moveX - gesture.x0 + gesture.dx) / widthRef.current, 0, 1))
          }
        },
        onPanResponderRelease: () => {
          setDragging(false)
          onCommit?.(valueRef.current)
        },
      }),
    [onChange, onCommit],
  )

  const percent = clamp(value, 0, 1)

  return (
    <View
      className="justify-center"
      style={{ height: 32 }}
      onLayout={(event) => {
        const w = event.nativeEvent.layout.width
        widthRef.current = w
        setWidth(w)
      }}
      {...responder.panHandlers}
    >
      <View
        style={{
          height: trackHeight,
          borderRadius: 999,
          backgroundColor: trackColor ?? colors.overlay3,
          overflow: 'hidden',
        }}
      >
        <View
          style={{
            height: '100%',
            width: `${percent * 100}%`,
            borderRadius: 999,
            backgroundColor: fillColor ?? colors.ink,
          }}
        />
      </View>
      {showThumb && width > 0 ? (
        <View
          style={{
            position: 'absolute',
            left: Math.max(0, Math.min(width - 14, percent * width - 7)),
            width: dragging ? 18 : 14,
            height: dragging ? 18 : 14,
            borderRadius: 999,
            backgroundColor: thumbColor ?? '#ffffff',
          }}
        />
      ) : null}
    </View>
  )
}

/* ==========================================================================
   Skeleton / Spinner
   ========================================================================== */
export function Skeleton({ className = '', style }: { className?: string; style?: StyleProp<ViewStyle> }) {
  const colors = useThemeColors()
  const anim = useRef(new Animated.Value(0.5)).current
  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(anim, { toValue: 1, duration: 900, useNativeDriver: true }),
        Animated.timing(anim, { toValue: 0.5, duration: 900, useNativeDriver: true }),
      ]),
    )
    loop.start()
    return () => loop.stop()
  }, [anim])
  return (
    <Animated.View
      className={`rounded-lg ${className}`}
      style={[{ backgroundColor: colors.overlay2, opacity: anim }, style]}
    />
  )
}

export function Spinner({ size = 22, color }: { size?: number; color?: string }) {
  const colors = useThemeColors()
  const anim = useRef(new Animated.Value(0)).current
  useEffect(() => {
    const loop = Animated.loop(
      Animated.timing(anim, { toValue: 1, duration: 900, useNativeDriver: true }),
    )
    loop.start()
    return () => loop.stop()
  }, [anim])

  const rotate = anim.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] })
  return (
    <Animated.View style={{ width: size, height: size, transform: [{ rotate }] }}>
      <SpinnerIcon size={size} color={color ?? colors.muted} />
    </Animated.View>
  )
}

/* ==========================================================================
   Sheet (bottom sheet)
   ========================================================================== */
interface SheetProps {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
}

export function Sheet({ open, onClose, title, children }: SheetProps) {
  const colors = useThemeColors()
  return (
    <Modal visible={open} transparent animationType="slide" onRequestClose={onClose}>
      <View className="flex-1 justify-end">
        <Pressable
          className="absolute inset-0 bg-black/70"
          onPress={onClose}
          accessibilityLabel="Cerrar"
        />
        <View
          className="max-h-[85%] overflow-hidden rounded-t-[1.75rem] border-t"
          style={{ backgroundColor: colors.bgElev, borderColor: colors.hairStrong }}
        >
          <View className="items-center px-4 pb-2 pt-3">
            <View className="h-1.5 w-10 rounded-full" style={{ backgroundColor: colors.overlay3 }} />
            <View className="mt-3 w-full flex-row items-center justify-between">
              {title ? (
                <Text className="text-base font-semibold tracking-tight" style={{ color: colors.ink }}>
                  {title}
                </Text>
              ) : (
                <View />
              )}
              <IconButton label="Cerrar" size="sm" onPress={onClose}>
                <CloseIcon size={18} color={colors.muted} />
              </IconButton>
            </View>
          </View>
          <ScrollView
            className="px-4"
            contentContainerStyle={{ paddingBottom: 24 }}
            showsVerticalScrollIndicator={false}
          >
            {children}
          </ScrollView>
        </View>
      </View>
    </Modal>
  )
}

/* ==========================================================================
   Pestillas tipo píldora
   ========================================================================== */
interface TabsProps<T extends string> {
  tabs: Array<{ id: T; label: string; count?: number }>
  value: T
  onChange: (id: T) => void
}

export function PillTabs<T extends string>({ tabs, value, onChange }: TabsProps<T>) {
  const colors = useThemeColors()
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
      {tabs.map((tab) => {
        const active = tab.id === value
        return (
          <Pressable
            key={tab.id}
            onPress={() => onChange(tab.id)}
            style={{
              borderRadius: 999,
              paddingHorizontal: 14,
              paddingVertical: 6,
              backgroundColor: active ? colors.accent : colors.overlay1,
              flexDirection: 'row',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <Text
              style={{ color: active ? colors.accentInk : colors.muted, fontWeight: '600', fontSize: 14 }}
            >
              {tab.label}
            </Text>
            {tab.count !== undefined && tab.count > 0 ? (
              <Text style={{ color: active ? colors.accentInk : colors.muted, fontSize: 12 }}>
                {tab.count}
              </Text>
            ) : null}
          </Pressable>
        )
      })}
    </ScrollView>
  )
}

/* ==========================================================================
   Control segmentado
   ========================================================================== */
export function Segmented<T extends string>({
  tabs,
  value,
  onChange,
  style,
}: TabsProps<T> & { style?: StyleProp<ViewStyle> }) {
  const colors = useThemeColors()
  return (
    <View
      className="flex-row items-center gap-1 rounded-2xl border p-1"
      style={[{ borderColor: colors.hair, backgroundColor: colors.overlay1 }, style]}
    >
      {tabs.map((tab) => {
        const active = tab.id === value
        return (
          <Pressable
            key={tab.id}
            onPress={() => onChange(tab.id)}
            className="flex-1 flex-row items-center justify-center gap-1.5 rounded-xl px-2.5 py-2"
            style={{ backgroundColor: active ? colors.accent : 'transparent' }}
          >
            <Text
              style={{
                color: active ? colors.accentInk : colors.faint,
                fontWeight: '600',
                fontSize: 13,
              }}
            >
              {tab.label}
            </Text>
            {tab.count !== undefined && tab.count > 0 ? (
              <Text style={{ color: active ? colors.accentInk : colors.faint, fontSize: 11 }}>
                {tab.count}
              </Text>
            ) : null}
          </Pressable>
        )
      })}
    </View>
  )
}

/* ==========================================================================
   Diálogo centrado
   ========================================================================== */
interface DialogProps {
  open: boolean
  onClose: () => void
  children: ReactNode
}

export function Dialog({ open, onClose, children }: DialogProps) {
  const colors = useThemeColors()
  return (
    <Modal visible={open} transparent animationType="fade" onRequestClose={onClose}>
      <View className="flex-1 items-center justify-center p-4">
        <Pressable className="absolute inset-0 bg-black/70" onPress={onClose} accessibilityLabel="Cerrar" />
        <View
          className="w-full max-w-sm rounded-2xl border p-5"
          style={{ backgroundColor: colors.bgElev, borderColor: colors.hair }}
        >
          {children}
        </View>
      </View>
    </Modal>
  )
}

/* ==========================================================================
   Encabezado de pantalla
   ========================================================================== */
export function ScreenHeader({
  title,
  subtitle,
  action,
}: {
  title: string
  subtitle?: string
  action?: ReactNode
}) {
  const colors = useThemeColors()
  return (
    <View className="flex-row items-end justify-between gap-3 px-5 pb-4 pt-5">
      <View className="min-w-0 flex-1">
        <Text
          numberOfLines={1}
          className="text-[1.7rem] font-semibold tracking-tight"
          style={{ color: colors.ink }}
        >
          {title}
        </Text>
        {subtitle ? (
          <Text numberOfLines={1} className="mt-1 text-sm" style={{ color: colors.faint }}>
            {subtitle}
          </Text>
        ) : null}
      </View>
      {action}
    </View>
  )
}

export const hairline = StyleSheet.hairlineWidth
